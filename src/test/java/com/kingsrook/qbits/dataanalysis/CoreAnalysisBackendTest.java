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

import java.util.Set;
import com.kingsrook.qbits.dataanalysis.engine.CoreAnalysisBackend;
import com.kingsrook.qbits.dataanalysis.engine.StandInAnalysisBackend;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfileRequest;
import com.kingsrook.qqq.backend.core.actions.interfaces.AggregateInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateCapability;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryAggregateAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** Native sample membership and capability-directed fallback behavior.
 *******************************************************************************/
class CoreAnalysisBackendTest extends EngineFixture
{
   /*******************************************************************************
    ** Restore the global dispatcher after a capability-specific fixture.
    *******************************************************************************/
   @AfterEach
   void restoreModule()
   {
      QBackendModuleDispatcher.registerBackendModule(new MemoryBackendModule());
   }

   /*******************************************************************************
    ** Deterministic memory sampling scales every-k-th counts to the population.
    *******************************************************************************/
   @Test
   void sampledPopulation() throws Exception
   {
      config.withDefaultSampleThresholdRows(5).withDefaultSampleRows(4);
      assertThat(service.backend("orders")).isInstanceOf(CoreAnalysisBackend.class);
      var profile = service.columnProfile(new ColumnProfileRequest().withTableName("orders").withFieldName("amount"));
      assertThat(profile.getEstimate()).isTrue();
      assertThat(profile.getSampleRows()).isEqualTo(4);
      assertThat(profile.getNumeric().getMetrics().stream().filter(m -> m.getKey().equals("sum")).findFirst().orElseThrow().getValue().toString()).isEqualTo("1030");
      var counts = profile.getCounts().stream().collect(java.util.stream.Collectors.toMap(m -> m.getKey(), m -> m));
      assertThat(counts.get("rows").getValue()).isEqualTo(12L);
      assertThat(counts.get("rows").getEstimate()).isFalse();
      assertThat(counts.get("nonNull").getValue()).isEqualTo(Math.round(4.0 * 12 / 4));
      assertThat(profile.getTopValues()).allSatisfy(v ->
      {
         assertThat(v.getCount()).isEqualTo(Math.round(1.0 * 12 / 4));
         assertThat(v.getShare()).isEqualByComparingTo("0.25");
      });
      assertThat(com.kingsrook.qqq.backend.core.utils.JsonUtils.toJson(profile)).doesNotContain("\"operator\":\"IN\"");
      new MemoryEngineTest().assertDrills(profile);
      config.withDefaultMaxRows(0);
      service.getConfigResolver().invalidate();
      var empty = service.columnProfile(new ColumnProfileRequest().withTableName("orders").withFieldName("amount"));
      assertThat(empty.getEstimate()).isTrue();
      assertThat(empty.getSampleRows()).isEqualTo(4);
      assertThat(empty.getTruncated()).isFalse();
   }

   /*******************************************************************************
    ** Rounding counts must not alter raw shares, and drills retain the user filter.
    *******************************************************************************/
   @Test
   void filteredSampleRatios() throws Exception
   {
      config.withDefaultSampleThresholdRows(2).withDefaultSampleRows(4);
      var filter = com.kingsrook.qbits.dataanalysis.engine.DrillFilters.condition("id",
         com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.GREATER_THAN, 1);
      var profile = service.columnProfile(new ColumnProfileRequest().withTableName("orders").withFieldName("amount").withFilter(filter));
      // The filtered ids 2..12, sampled every third row, yield 2, 5, 8, 11.
      var sampledAmounts = java.util.Arrays.asList(0, 20, 40, null);
      long nonNull = sampledAmounts.stream().filter(java.util.Objects::nonNull).count();
      var counts = profile.getCounts().stream().collect(java.util.stream.Collectors.toMap(m -> m.getKey(), m -> m));
      assertThat(profile.getSampleRows()).isEqualTo(sampledAmounts.size());
      assertThat(counts.get("rows").getValue()).isEqualTo(11L);
      assertThat(counts.get("nonNull").getValue()).isEqualTo(Math.round(nonNull * 11.0 / sampledAmounts.size()));
      assertThat(counts.get("nulls").getValue()).isEqualTo(Math.round((sampledAmounts.size() - nonNull) * 11.0 / sampledAmounts.size()));
      assertThat(profile.getTopValues()).hasSize(4).allSatisfy(v ->
      {
         assertThat(v.getCount()).isEqualTo(Math.round(11.0 / sampledAmounts.size()));
         assertThat(v.getShare()).isEqualByComparingTo("0.25");
      });
      var exact = new StandInAnalysisBackend(service.getConfigResolver().resolve("orders"));
      assertThat(exact.count("orders", counts.get("rows").getDrill().getFilter())).isEqualTo(11);
      assertThat(exact.count("orders", counts.get("nonNull").getDrill().getFilter())).isEqualTo(10);
      var twenty = profile.getTopValues().stream().filter(v -> v.getValue() != null && v.getValue().toString().equals("20")).findFirst().orElseThrow();
      assertThat(exact.count("orders", twenty.getDrill().getFilter())).isEqualTo(2);
   }

   /*******************************************************************************
    ** Empty samples keep the population drill; inspecting every row is exact.
    *******************************************************************************/
   @Test
   void samplingBoundaries() throws Exception
   {
      config.withDefaultSampleThresholdRows(2).withDefaultSampleRows(0);
      var empty = service.columnProfile(new ColumnProfileRequest().withTableName("orders").withFieldName("amount"));
      assertThat(empty.getSampleRows()).isZero();
      assertThat(empty.getCounts().getFirst().getValue()).isEqualTo(12L);
      assertThat(empty.getCounts().get(1).getValue()).isEqualTo(0L);
      config.withDefaultSampleRows(100);
      service.getConfigResolver().invalidate();
      var scope = service.backend("orders").scope("orders", null, false);
      assertThat(scope.samplePercent()).isNull();
      assertThat(scope.estimate()).isFalse();
      assertThat(scope.rowsScanned()).isEqualTo(12);
   }

   /*******************************************************************************
    ** A non-random estimate has one boundary bind and population-wide drills.
    *******************************************************************************/
   @Test
   void prefixBoundary() throws Exception
   {
      capabilities(Set.of());
      config.withDefaultSampleThresholdRows(2).withDefaultSampleRows(4);
      var filter = com.kingsrook.qbits.dataanalysis.engine.DrillFilters.condition("id",
         com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.GREATER_THAN, 2);
      var backend = service.backend("orders");
      var scope = backend.scope("orders", filter, false);
      assertThat(scope.filter().getSubFilters().get(1).getCriteria()).singleElement().satisfies(c ->
      {
         assertThat(c.getFieldName()).isEqualTo("id");
         assertThat(c.getOperator()).isEqualTo(com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.LESS_THAN_OR_EQUALS);
         assertThat(c.getValues()).containsExactly(6);
      });
      var profile = service.columnProfile(new ColumnProfileRequest().withTableName("orders").withFieldName("quantity").withFilter(filter));
      assertThat(profile.getCounts().getFirst().getValue()).isEqualTo(10L);
      assertThat(profile.getCounts().stream().filter(m -> m.getKey().equals("nonNull")).findFirst().orElseThrow().getValue()).isEqualTo(4L);
      assertThat(profile.getWarnings()).contains("Estimate from the first 4 rows by id");
      var drill = profile.getCounts().getFirst().getDrill();
      assertThat(new StandInAnalysisBackend(service.getConfigResolver().resolve("orders")).count("orders", drill.getFilter())).isEqualTo(10);
      assertThat(com.kingsrook.qqq.backend.core.utils.JsonUtils.toJson(profile)).doesNotContain("\"operator\":\"IN\"");
   }

   /*******************************************************************************
    ** Empty capabilities select the stand-in; partial support uses core plus fallback.
    *******************************************************************************/
   @Test
   void fallbackSelection() throws Exception
   {
      capabilities(Set.of());
      assertThat(service.backend("orders")).isExactlyInstanceOf(StandInAnalysisBackend.class);
      capabilities(Set.of(AggregateCapability.STDDEV));
      assertThat(service.backend("orders")).isInstanceOf(CoreAnalysisBackend.class);
      var profile = service.columnProfile(new ColumnProfileRequest().withTableName("orders").withFieldName("amount"));
      assertThat(profile.getNumeric().getMetrics().stream().filter(m -> m.getKey().equals("p50")).findFirst().orElseThrow().getValue().toString()).isEqualTo("20.0");
      new MemoryEngineTest().assertDrills(profile);
   }

   /*******************************************************************************
    ** Advertise selected capabilities while retaining the fixture's actual actions.
    *******************************************************************************/
   private void capabilities(Set<AggregateCapability> supported)
   {
      LimitedMemoryBackend.supported = supported;
      QBackendModuleDispatcher.registerBackendModule(new LimitedMemoryBackend());
   }

   /*******************************************************************************
    ** Public no-argument backend class required by the module dispatcher.
    *******************************************************************************/
   public static class LimitedMemoryBackend extends MemoryBackendModule
   {
      private static Set<AggregateCapability> supported = Set.of();

      /*******************************************************************************
       ** Retain real memory execution with a deliberately reduced feature set.
       *******************************************************************************/
      @Override
      public AggregateInterface getAggregateInterface()
      {
         return (new MemoryAggregateAction()
         {
            @Override
            public Set<AggregateCapability> getSupportedCapabilities(AggregateInput input)
            {
               return (supported);
            }
         });
      }
   }
}
