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

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisBackend;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfileRequest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** Real rows prove warning boundaries and date coordinates without Docker.
 *******************************************************************************/
class PresentationSamplingTest extends EngineFixture
{
   /*******************************************************************************
    ** Null prefixes do not consume the budget; warn only after 10,000 values.
    *******************************************************************************/
   @Test
   void patternWarningOnlyWhenTruncated() throws Exception
   {
      List<QRecord> records = new ArrayList<>();
      for(int i = 100; i <= 10101; i++)
      {
         records.add(new QRecord().withValue("id", i).withValue("customer", 1).withValue("note", i == 100 ? null : "AA-11"));
      }
      assertThat(new InsertAction().execute(new InsertInput("orders").withRecords(records)).getRecords()).allSatisfy(r -> assertThat(r.getErrors()).isNullOrEmpty());
      var filter = DrillFilters.condition("id", QCriteriaOperator.GREATER_THAN_OR_EQUALS, 100);
      var request = new ColumnProfileRequest().withTableName("orders").withFieldName("note").withFilter(filter).withExact(true);
      var truncated = service.columnProfile(request);
      assertThat(truncated.getText().getSampleRows()).isEqualTo(10000);
      assertThat(truncated.getWarnings()).containsExactly("Patterns are based on the first 10,000 values.");
      request.withFilter(DrillFilters.and(filter, DrillFilters.condition("id", QCriteriaOperator.LESS_THAN_OR_EQUALS, 10100)));
      var all = service.columnProfile(request);
      assertThat(all.getText().getSampleRows()).isEqualTo(10000);
      assertThat(all.getWarnings()).isEmpty();
   }



   /*******************************************************************************
    ** Empty samples are not empty populations; full scans are not estimates.
    *******************************************************************************/
   @Test
   void warningsDescribeObservedScope() throws Exception
   {
      config.withDefaultSampleThresholdRows(1).withDefaultSampleRows(100);
      var request = new ColumnProfileRequest().withTableName("orders").withFieldName("note");
      var complete = service.columnProfile(request);
      assertThat(complete.getEstimate()).isFalse();
      assertThat(complete.getWarnings()).isEmpty();
      config.withDefaultSampleRows(0);
      service.getConfigResolver().invalidate();
      var emptySample = service.columnProfile(request);
      assertThat(emptySample.getWarnings()).contains("No sample rows were observed; run exact for exact numbers.")
         .doesNotContain("No readable records match this analysis scope.", "All values in this analysis scope are null.");
      var empty = service.columnProfile(request.withExact(true).withFilter(DrillFilters.condition("id", QCriteriaOperator.EQUALS, -1)));
      assertThat(empty.getWarnings()).containsExactly("No readable records match this analysis scope.");
      var nulls = service.columnProfile(request.withFilter(DrillFilters.condition("id", QCriteriaOperator.EQUALS, 1)));
      assertThat(nulls.getWarnings()).containsExactly("All values in this analysis scope are null.");
   }



   /*******************************************************************************
    ** Hour buckets cross DST in real elapsed hours and preserve ISO instants.
    *******************************************************************************/
   @Test
   void hourlyBucketCoordinatesAndLabels() throws Exception
   {
      var backend = service.backend("orders");
      ZoneId zone = ZoneId.of("America/Chicago");
      var buckets = backend.dateBuckets("orders", null, "created", AnalysisBackend.DateUnit.HOUR, zone);
      assertThat(buckets.getFirst().value()).isEqualTo(Instant.parse("2026-01-01T12:00:00Z"));
      assertThat(buckets.getFirst().label()).isEqualTo("2026-01-01 06:00");
      assertThat(buckets.stream().mapToLong(AnalysisBackend.GroupCount::count).sum()).isEqualTo(12);
      for(var bucket : buckets)
      {
         assertThat(backend.count("orders", bucket.condition())).isEqualTo(bucket.count());
      }
      TestSupport.insert("orders", new QRecord().withValue("id", 20).withValue("customer", 1).withValue("created", Instant.parse("2026-11-01T06:30:00Z")));
      TestSupport.insert("orders", new QRecord().withValue("id", 21).withValue("customer", 1).withValue("created", Instant.parse("2026-11-01T07:30:00Z")));
      var filter = DrillFilters.condition("id", QCriteriaOperator.GREATER_THAN_OR_EQUALS, 20);
      var dst = backend.dateBuckets("orders", filter, "created", AnalysisBackend.DateUnit.HOUR, zone);
      assertThat(dst).hasSize(2).allSatisfy(b -> assertThat(b.label()).isEqualTo("2026-11-01 01:00"));
      assertThat(dst).extracting(AnalysisBackend.GroupCount::value).doesNotHaveDuplicates();
      assertThat(dst).allSatisfy(b -> assertThat(b.count()).isEqualTo(1));
   }
}
