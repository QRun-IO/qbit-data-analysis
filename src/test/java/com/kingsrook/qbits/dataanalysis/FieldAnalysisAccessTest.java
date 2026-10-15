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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qbits.dataanalysis.engine.CoreTierFieldAccessResolver;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.engine.FieldAnalysisAccess;
import com.kingsrook.qbits.dataanalysis.engine.FieldAnalysisAccessResolver;
import com.kingsrook.qbits.dataanalysis.engine.FieldSecurityLockFieldAccessResolver;
import com.kingsrook.qbits.dataanalysis.result.DistinctValuesRequest;
import com.kingsrook.qbits.dataanalysis.result.ValueCount;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfile;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfileRequest;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.PivotCell;
import com.kingsrook.qbits.dataanalysis.result.PivotDimension;
import com.kingsrook.qbits.dataanalysis.result.PivotMeasure;
import com.kingsrook.qbits.dataanalysis.result.PivotRequest;
import com.kingsrook.qbits.dataanalysis.result.PivotResult;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/*******************************************************************************
 ** Exercise reflection without shadowing a future real core package on test classpaths.
 *******************************************************************************/
class FieldAnalysisAccessTest extends EngineFixture
{
   /*******************************************************************************
    ** Test helper for field-access behavior.
    *******************************************************************************/
   public static class TierApi
   {
      static String tier = "FULL";
      static boolean fail;
      /*******************************************************************************
       ** Verify or support the pivot and field-access contract.
       *******************************************************************************/
      public static String getTier(QTableMetaData table, String field)
      {
         if(fail)
         {
            throw new IllegalStateException("unavailable");
         }
         return (tier);
      }
      /*******************************************************************************
       ** Verify or support the pivot and field-access contract.
       *******************************************************************************/
      public static String getAccessFingerprint()
      {
         if(fail)
         {
            throw new IllegalStateException("unavailable");
         }
         return ("policy-v1");
      }
   }

   /*******************************************************************************
    ** Test helper for field-access behavior.
    *******************************************************************************/
   public static class TierOnlyApi
   {
      /*******************************************************************************
       ** Verify or support the pivot and field-access contract.
       *******************************************************************************/
      public static String getTier(QTableMetaData table, String field)
      {
         return ("FULL");
      }
   }

   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   @Test
   void legacyFallbackAndReflectiveTierMapping() throws Exception
   {
      var table = instance.getTable("orders");
      var fallback = new FieldSecurityLockFieldAccessResolver();
      assertThat(fallback.resolve(table, table.getField("amount"))).isEqualTo(FieldAnalysisAccess.FULL);
      assertThat(fallback.resolve(table, table.getField("secret"))).isEqualTo(FieldAnalysisAccess.NONE);
      // RC.7 supplies the core tier API; verify discovery and effective access.
      var defaultResolver = CoreTierFieldAccessResolver.defaultResolver();
      assertThat(defaultResolver).isInstanceOf(CoreTierFieldAccessResolver.class);
      assertThat(defaultResolver.resolve(table, table.getField("amount"))).isEqualTo(FieldAnalysisAccess.FULL);
      assertThat(defaultResolver.resolve(table, table.getField("secret"))).isEqualTo(FieldAnalysisAccess.NONE);
      var resolver = new CoreTierFieldAccessResolver(TierApi.class);
      try
      {
         for(String tier : List.of("FULL", "REVEALABLE", "MASKED", "HIDDEN", "UNKNOWN"))
         {
            TierApi.tier = tier;
            assertThat(resolver.resolve(table, table.getField("amount"))).isEqualTo(tier.equals("FULL") ? FieldAnalysisAccess.FULL : tier.equals("REVEALABLE") ? FieldAnalysisAccess.EXACT_MATCH_ONLY : FieldAnalysisAccess.NONE);
         }
         assertThat(resolver.getAccessFingerprint()).isEqualTo("policy-v1");
         TierApi.fail = true;
         assertThat(resolver.resolve(table, table.getField("amount"))).isEqualTo(FieldAnalysisAccess.NONE);
         assertThatThrownBy(resolver::getAccessFingerprint).isInstanceOf(IllegalStateException.class);
         assertThat(new CoreTierFieldAccessResolver(TierOnlyApi.class).getAccessFingerprint()).isEmpty();
      }
      finally
      {
         TierApi.fail = false;
         TierApi.tier = "FULL";
      }
   }

   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   @Test
   void customPolicyAndUserFilterPassThrough() throws Exception
   {
      config.withFieldAnalysisAccessResolver(() -> (table, field) -> field.getName().equals("note") ? FieldAnalysisAccess.NONE
         : field.getName().equals("paid") ? FieldAnalysisAccess.EXACT_MATCH_ONLY : new FieldSecurityLockFieldAccessResolver().resolve(table, field));
      service = new DataAnalysisService(config, Runnable::run);
      assertThat(service.capabilities("orders").getFields()).doesNotContain("note", "paid", "secret");
      for(String field : List.of("note", "paid"))
      {
         assertThatThrownBy(() -> service.columnProfile(new ColumnProfileRequest().withTableName("orders").withFieldName(field)))
            .isInstanceOf(com.kingsrook.qqq.backend.core.exceptions.QBadRequestException.class);
         assertThatThrownBy(() -> service.pivot(new PivotRequest().withTableName("orders").withRows(List.of(new PivotDimension().withField(field)))
            .withValues(List.of(new PivotMeasure().withOperator(PivotMeasure.Operator.COUNT)))))
            .isInstanceOf(com.kingsrook.qqq.backend.core.exceptions.QBadRequestException.class);
         assertThatThrownBy(() -> service.pivot(new PivotRequest().withTableName("orders").withRows(List.of(new PivotDimension().withField("status")))
            .withValues(List.of(new PivotMeasure().withOperator(PivotMeasure.Operator.MIN).withField(field)))))
            .isInstanceOf(com.kingsrook.qqq.backend.core.exceptions.QBadRequestException.class);
      }
      var request = new PivotRequest().withTableName("orders").withRows(List.of(new PivotDimension().withField("status")))
         .withValues(List.of(new PivotMeasure().withOperator(PivotMeasure.Operator.COUNT)));
      assertThat(JsonUtils.toJson(service.pivot(request))).doesNotContain("\"fieldName\":\"note\"", "\"fieldName\":\"paid\"");
      var ownFilter = DrillFilters.condition("paid", QCriteriaOperator.EQUALS, true);
      String before = JsonUtils.toJson(ownFilter);
      PivotResult result = service.pivot(request.withFilter(ownFilter));
      assertThat(JsonUtils.toJson(ownFilter)).isEqualTo(before);
      assertThat(((Number)result.getGrandTotals().getFirst().getValue()).intValue()).isEqualTo(6);
      for(PivotCell cell : result.getCells())
      {
         var drill = cell.getValues().getFirst().getDrill().getFilter();
         assertThat(JsonUtils.toJson(drill.getSubFilters().getFirst())).isEqualTo(before);
         assertThat(JsonUtils.toJson(drill.getSubFilters().get(1))).doesNotContain("paid", "note");
      }
   }

   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   @Test
   void cacheMissesOnFieldTierAndCoreFingerprintChanges() throws Exception
   {
      AtomicReference<FieldAnalysisAccess> tier = new AtomicReference<>(FieldAnalysisAccess.FULL);
      AtomicReference<String> fingerprint = new AtomicReference<>("first");
      config.withFieldAnalysisAccessResolver(() -> new FieldAnalysisAccessResolver()
      {
         /*******************************************************************************
          ** Verify or support the pivot and field-access contract.
          *******************************************************************************/
         @Override
         public FieldAnalysisAccess resolve(QTableMetaData table, QFieldMetaData field)
         {
            return (field.getName().equals("quantity") ? tier.get() : new FieldSecurityLockFieldAccessResolver().resolve(table, field));
         }
         /*******************************************************************************
          ** Verify or support the pivot and field-access contract.
          *******************************************************************************/
         @Override
         public String getAccessFingerprint()
         {
            return (fingerprint.get());
         }
      });
      service = new DataAnalysisService(config, Runnable::run);
      ColumnProfileRequest request = new ColumnProfileRequest().withTableName("orders").withFieldName("amount").withExact(true);
      String first = service.startExactRun(request);
      assertThat(service.startExactRun(request)).isEqualTo(first);
      tier.set(FieldAnalysisAccess.EXACT_MATCH_ONLY);
      String second = service.startExactRun(request);
      assertThat(second).isNotEqualTo(first);
      assertThatThrownBy(() -> service.getRun(first)).isInstanceOf(com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException.class);
      fingerprint.set("second");
      assertThat(service.startExactRun(request)).isNotEqualTo(second);
   }
   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   @Test
   void generatedRepresentativeDrillsCannotUseRestrictedPrimaryKey() throws Exception
   {
      config.withFieldAnalysisAccessResolver(() -> (table, field) -> field.getName().equals("id") ? FieldAnalysisAccess.NONE
         : new FieldSecurityLockFieldAccessResolver().resolve(table, field));
      service = new DataAnalysisService(config, Runnable::run);
      ColumnProfile profile = service.columnProfile(new ColumnProfileRequest().withTableName("orders").withFieldName("status").withExact(true));
      Metric distinct = profile.getCounts().stream().filter(m -> m.getKey().equals("distinct")).findFirst().orElseThrow();
      assertThat(distinct.getDrill()).isNull();
      assertThat(profile.getCounts().getFirst().getDrill()).isNotNull();
      var ownFilter = DrillFilters.condition("id", QCriteriaOperator.EQUALS, 1);
      profile = service.columnProfile(new ColumnProfileRequest().withTableName("orders").withFieldName("status").withFilter(ownFilter).withExact(true));
      assertThat(profile.getCounts().getFirst().getDrill()).isNotNull();
      assertThat(JsonUtils.toJson(profile.getCounts().getFirst().getDrill())).contains("id");
   }
   /*******************************************************************************
    ** Related label tiers must neither leak labels nor reuse a stale cache entry.
    *******************************************************************************/
   @Test
   void relatedLabelTierChangesInvalidateCache() throws Exception
   {
      AtomicReference<FieldAnalysisAccess> tier = new AtomicReference<>(FieldAnalysisAccess.FULL);
      config.withFieldAnalysisAccessResolver(() -> (table, field) -> table.getName().equals("customer") && field.getName().equals("name")
         ? tier.get() : new FieldSecurityLockFieldAccessResolver().resolve(table, field));
      service = new DataAnalysisService(config, Runnable::run);
      ColumnProfileRequest request = new ColumnProfileRequest().withTableName("orders").withFieldName("customer").withExact(true);
      String first = service.startExactRun(request);
      assertThat(service.columnProfile(request).getTopValues()).extracting(ValueCount::getLabel).contains("Alice Co");
      tier.set(FieldAnalysisAccess.NONE);
      assertThat(service.startExactRun(request)).isNotEqualTo(first);
      assertThat(service.columnProfile(request).getTopValues()).extracting(ValueCount::getLabel).containsExactly("1", "2");
      assertThat(service.distinctValues(new DistinctValuesRequest().withTableName("orders").withFieldName("customer")).getValues()).extracting(ValueCount::getLabel).containsExactly("1", "2");
   }
}
