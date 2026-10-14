/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qbits.dataanalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.dataanalysis.result.Drill;
import com.kingsrook.qbits.dataanalysis.watch.WatchBucket;
import com.kingsrook.qbits.dataanalysis.watch.WatchDefinition;
import com.kingsrook.qbits.dataanalysis.watch.WatchDetectors;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*******************************************************************************
 ** Hand-computed detector cases isolate statistical behavior from the database.
 *******************************************************************************/
class WatchDetectorsTest
{
   static QRecord definition(String detector, String metric, String parameters)
   {
      return (new QRecord().withValue("table_name", "orders").withValue("owner_user_id", "alice").withValue("detector", detector)
         .withValue("metric_json", "{\"operator\":\"" + metric + "\",\"field\":\"amount\"}").withValue("parameters_json", parameters)
         .withValue("date_field", "created").withValue("bucket", "DAY").withValue("severity", "WARNING").withValue("enabled", true));
   }

   private WatchDefinition parsed(String detector, String metric, String parameters) throws Exception
   {
      return (WatchDefinition.parse(definition(detector, metric, parameters)));
   }

   private List<WatchBucket> series(Double... values)
   {
      List<WatchBucket> result = new ArrayList<>();
      for(Double value : values)
      {
         result.add(new WatchBucket("day-" + result.size(), value, Set.of(), new Drill().withTableName("orders")));
      }
      return (result);
   }

   @Test
   void robustZScoreHasDirectionAndStrictThreshold() throws Exception
   {
      var spike = parsed("SPIKE", "COUNT", "{\"k\":3}");
      assertThat(WatchDetectors.evaluate(spike, series(9.0, 10.0, 11.0, 20.0), "Orders per day").firing()).isTrue();
      assertThat(WatchDetectors.evaluate(spike, series(9.0, 10.0, 11.0, 1.0), "Orders").firing()).isFalse();
      var drop = parsed("DROP", "COUNT", "{\"k\":3}");
      var verdict = WatchDetectors.evaluate(drop, series(40.0, 45.0, 55.0, 12.0), "Orders per day");
      assertThat(verdict.firing()).isTrue();
      assertThat(verdict.baseline()).isEqualTo(45.0);
      assertThat(verdict.message()).contains("Orders per day dropped to 12", "usual 40");
      assertThat(verdict.drill().getTableName()).isEqualTo("orders");
      assertThat(WatchDetectors.evaluate(drop, series(40.0, 45.0, 55.0, 100.0), "Orders").firing()).isFalse();
      var exact = parsed("SPIKE", "COUNT", "{\"k\":3,\"threshold\":0.6745}");
      assertThat(WatchDetectors.evaluate(exact, series(9.0, 10.0, 11.0, 11.0), "Orders").firing()).isFalse();
   }

   @Test
   void zeroMadUsesRelativeChangeAndHandlesZeroAndNegativeBaselines() throws Exception
   {
      var spike = parsed("SPIKE", "COUNT", "{\"k\":2}");
      for(double x : new double[] {10, 15})
      {
         assertThat(WatchDetectors.evaluate(spike, series(10.0, 10.0, x), "Orders").firing()).isFalse();
      }
      assertThat(WatchDetectors.evaluate(spike, series(10.0, 10.0, 16.0), "Orders").firing()).isTrue();
      assertThat(WatchDetectors.evaluate(spike, series(0.0, 0.0, 0.0), "Orders").firing()).isFalse();
      assertThat(WatchDetectors.evaluate(spike, series(0.0, 0.0, 1.0), "Orders").firing()).isTrue();
      assertThat(WatchDetectors.evaluate(spike, series(-10.0, -10.0, -4.0), "Balance").firing()).isTrue();
      var drop = parsed("DROP", "COUNT", "{\"k\":2}");
      assertThat(WatchDetectors.evaluate(drop, series(10.0, 10.0, 4.0), "Orders").firing()).isTrue();
      assertThat(WatchDetectors.evaluate(drop, series(0.0, 0.0, -1.0), "Balance").firing()).isTrue();
   }

   @Test
   void rangeIncludesBothBoundsAndNeedsOnlyLatestValue() throws Exception
   {
      var range = parsed("OUT_OF_RANGE", "SUM", "{\"min\":10,\"max\":20}");
      assertThat(WatchDetectors.evaluate(range, series(10.0), "Amount").firing()).isFalse();
      assertThat(WatchDetectors.evaluate(range, series(20.0), "Amount").firing()).isFalse();
      assertThat(WatchDetectors.evaluate(range, series(9.0), "Amount").firing()).isTrue();
      assertThat(WatchDetectors.evaluate(range, series(21.0), "Amount").firing()).isTrue();
   }

   @Test
   void categoriesCompareWithEntireTrailingWindow() throws Exception
   {
      var definition = parsed("NEW_CATEGORY", "CATEGORY_SET", "{\"k\":2}");
      var baseline = List.of(new WatchBucket("1", 1.0, Set.of("a"), null), new WatchBucket("2", 1.0, Set.of("b"), null));
      List<WatchBucket> values = new ArrayList<>(baseline);
      values.add(new WatchBucket("3", 2.0, Set.of("a", "b"), null));
      assertThat(WatchDetectors.evaluate(definition, values, "Status").firing()).isFalse();
      values.set(2, new WatchBucket("3", 2.0, Set.of("a", "new"), null));
      assertThat(WatchDetectors.evaluate(definition, values, "Status").message()).contains("1 new category");
      values.set(2, new WatchBucket("3", 0.0, Set.of(), null));
      assertThat(WatchDetectors.evaluate(definition, values, "Status").firing()).isFalse();
   }

   @Test
   void nullRateDetectsDeltaOrConsecutiveRises() throws Exception
   {
      var definition = parsed("NULL_RATE_CREEP", "NULL_SHARE", "{\"k\":3}");
      assertThat(WatchDetectors.evaluate(definition, series(0.1, 0.1, 0.1, 0.16), "Null share").firing()).isTrue();
      assertThat(WatchDetectors.evaluate(definition, series(0.1, 0.11, 0.12, 0.13), "Null share").firing()).isTrue();
      assertThat(WatchDetectors.evaluate(definition, series(0.1, 0.12, 0.11, 0.13), "Null share").firing()).isFalse();
      assertThat(WatchDetectors.evaluate(definition, series(0.1, 0.11, 0.11, 0.12), "Null share").firing()).isFalse();
   }

   @ParameterizedTest
   @ValueSource(strings = {"SPIKE", "DROP", "NEW_CATEGORY", "NULL_RATE_CREEP"})
   void insufficientHistoryNeverBecomesRecovery(String detector) throws Exception
   {
      String metric = detector.equals("NEW_CATEGORY") ? "CATEGORY_SET" : detector.equals("NULL_RATE_CREEP") ? "NULL_SHARE" : "COUNT";
      var result = WatchDetectors.evaluate(parsed(detector, metric, null), series(1.0), "Orders");
      assertThat(result.evaluated()).isFalse();
      assertThat(result.message()).contains("Not enough history");
      assertThat(result.value()).isNull();
      assertThat(result.drill()).isNull();
   }

   @Test
   void emptyAndMissingNumericBucketsAreNotZero() throws Exception
   {
      var range = parsed("OUT_OF_RANGE", "AVG", "{\"min\":0,\"max\":1}");
      assertThat(WatchDetectors.evaluate(range, List.of(), "Orders").evaluated()).isFalse();
      assertThat(WatchDetectors.evaluate(range, series((Double) null), "Orders").evaluated()).isFalse();
      assertThat(WatchDetectors.evaluate(range, series(Double.NaN), "Orders").evaluated()).isFalse();
   }

   @ParameterizedTest
   @ValueSource(strings = {"[]", "{", "null", "{\"k\":0}", "{\"k\":1001}", "{\"k\":1.5}", "{\"k\":\"14\"}", "{\"threshold\":0}", "{\"threshold\":null}", "{\"relativeChange\":-1}", "{\"delta\":0.1}"})
   void rejectsInvalidSpikeParameters(String json)
   {
      assertThatThrownBy(() -> parsed("SPIKE", "COUNT", json)).isInstanceOf(QBadRequestException.class);
   }

   @Test
   void validatesMetricDetectorAndRangeCompatibility() throws Exception
   {
      assertThatThrownBy(() -> parsed("SPIKE", "CATEGORY_SET", null)).hasMessageContaining("NEW_CATEGORY");
      assertThatThrownBy(() -> parsed("NEW_CATEGORY", "COUNT", null)).hasMessageContaining("CATEGORY_SET");
      assertThatThrownBy(() -> parsed("NULL_RATE_CREEP", "SUM", null)).hasMessageContaining("NULL_SHARE");
      assertThatThrownBy(() -> parsed("OUT_OF_RANGE", "COUNT", "{\"min\":2,\"max\":1}")).hasMessageContaining("min <= max");
      assertThatThrownBy(() -> parsed("OUT_OF_RANGE", "COUNT", null)).hasMessageContaining("requires min and max");
      assertThatThrownBy(() -> parsed("NULL_RATE_CREEP", "NULL_SHARE", "{\"delta\":2}")).hasMessageContaining("delta");
      assertThatThrownBy(() -> parsed("NULL_RATE_CREEP", "NULL_SHARE", "{\"n\":0}")).hasMessageContaining("n must");
      assertThat(parsed("SPIKE", "COUNT", null).k()).isEqualTo(14);
      assertThat(parsed("SPIKE", "COUNT", null).threshold()).isEqualTo(3.5);
   }
}
