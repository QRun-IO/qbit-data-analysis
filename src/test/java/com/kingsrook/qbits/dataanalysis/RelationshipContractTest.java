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

import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.engine.FieldAnalysisAccess;
import com.kingsrook.qbits.dataanalysis.result.CompareRequest;
import com.kingsrook.qbits.dataanalysis.result.CorrelationRequest;
import com.kingsrook.qbits.dataanalysis.result.CrosstabRequest;
import com.kingsrook.qbits.dataanalysis.result.Drill;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.ScatterRequest;
import com.kingsrook.qbits.dataanalysis.result.ShiftRequest;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/*******************************************************************************
 ** Identical relationship contracts inherited by memory, H2 and PostgreSQL.
 *******************************************************************************/
abstract class RelationshipContractTest extends PivotContractTest
{
   /*******************************************************************************
    ** Verify compare numeric and categorical.
    *******************************************************************************/
   @Test
   void compareNumericAndCategorical() throws Exception
   {
      var request = new CompareRequest().withTableName("orders").withFields(List.of("amount"))
         .withFilterA(DrillFilters.condition("id", QCriteriaOperator.LESS_THAN_OR_EQUALS, 6))
         .withFilterB(DrillFilters.condition("id", QCriteriaOperator.GREATER_THAN, 6)).withLabelA("Early").withLabelB("Late");
      var result = service.compare(request);
      var field = result.getFields().getFirst();
      var value = field.getValues().getFirst();
      assertThat(value.getSideA().getLabel()).isEqualTo("Early");
      assertNumber(value.getSideA().getMetrics(), "count", 6);
      assertNumber(value.getSideA().getMetrics(), "mean", 50.0 / 6);
      assertNumber(value.getSideA().getMetrics(), "median", 10);
      assertNumber(value.getSideB().getMetrics(), "count", 5);
      assertNumber(value.getSideB().getMetrics(), "mean", 224);
      assertNumber(value.getSideB().getMetrics(), "median", 40);
      assertThat(((Number) value.getDelta().getValue()).doubleValue()).isCloseTo(224 - 50.0 / 6, within(1e-9));
      // scipy.stats.ttest_ind_from_stats(50/6, sqrt(136.66666666666666), 6, 224, sqrt(188530), 5, equal_var=False)
      assertThat(field.getSignificance().getPValue()).isCloseTo(.3290396212758404, within(1e-10));
      assertThat(field.getChart().getKind().name()).isEqualTo("BOX");
      assertThat(field.getChart().getSeries()).hasSize(2).allSatisfy(s -> assertThat(s.getPoints()).hasSize(5));
      relationshipDrills(result);
      var categorical = service.compare(new CompareRequest().withTableName("orders").withFields(List.of("status")));
      var category = categorical.getFields().getFirst();
      assertThat(category.getValues()).hasSize(2);
      assertThat(category.getSignificance().getPValue()).isEqualTo(1);
      assertThat(category.getSignificance().getDf()).isEqualTo(1);
      assertThat(category.getChart().getKind().name()).isEqualTo("BAR");
      for(var v : category.getValues())
      {
         assertNumber(v.getSideA().getMetrics(), "count", 6);
         assertNumber(v.getSideB().getMetrics(), "share", .5);
         assertThat(v.getSignificance().getApplicable()).isTrue();
      }
      relationshipDrills(categorical);
      var small = service.compare(request.withFields(List.of("status")));
      assertThat(small.getFields().getFirst().getValues()).singleElement().satisfies(v -> assertThat(v.getLabel()).isEqualTo("Other"));
      assertThat(small.getFields().getFirst().getSignificance().getApplicable()).isFalse();
      relationshipDrills(small);
   }

   /*******************************************************************************
    ** Verify correlation matches independent java pearson.
    *******************************************************************************/
   @Test
   void correlationMatchesIndependentJavaPearson() throws Exception
   {
      var result = service.correlation(new CorrelationRequest().withTableName("orders").withFields(List.of("amount", "quantity", "id")));
      assertThat(result.getCells()).hasSize(9);
      double[] x = {-10, 0, 10, 10, 20, 20, 30, 40, 50, 1000, 0};
      double[] y = {1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6};
      double meanX = java.util.Arrays.stream(x).average().orElseThrow();
      double meanY = java.util.Arrays.stream(y).average().orElseThrow();
      double xx = 0, yy = 0, xy = 0;
      for(int i = 0; i < x.length; i++)
      {
         xx += Math.pow(x[i] - meanX, 2);
         yy += Math.pow(y[i] - meanY, 2);
         xy += (x[i] - meanX) * (y[i] - meanY);
      }
      var cell = result.getCells().stream().filter(c -> c.getXField().equals("amount") && c.getYField().equals("quantity")).findFirst().orElseThrow();
      assertThat(cell.getN()).isEqualTo(11);
      assertThat(cell.getR()).isCloseTo(xy / Math.sqrt(xx * yy), within(1e-10));
      assertThat(service.scatter(cell.getScatter()).getSampleSize()).isEqualTo(11);
      assertThat(result.getChart().getKind().name()).isEqualTo("HEATMAP");
      for(var c : result.getCells())
      {
         if(c.getXField().equals(c.getYField()))
         {
            assertThat(c.getR()).isCloseTo(1, within(1e-12));
         }
      }
   }

   /*******************************************************************************
    ** Verify scatter limits trend and record drills.
    *******************************************************************************/
   @Test
   void scatterLimitsTrendAndRecordDrills() throws Exception
   {
      var result = service.scatter(new ScatterRequest().withTableName("orders").withXField("id").withYField("amount").withLimit(3));
      assertThat(result.getN()).isEqualTo(11);
      assertThat(result.getSampleSize()).isEqualTo(3);
      assertThat(result.getPoints()).extracting(p -> p.getX().toString()).containsExactly("1", "2", "3");
      assertThat(result.getTrend().getSlope()).isCloseTo(10, within(1e-12));
      assertThat(result.getTrend().getIntercept()).isCloseTo(-20, within(1e-12));
      assertThat(result.getTrend().getR()).isCloseTo(1, within(1e-12));
      assertThat(result.getChart().getSeries()).hasSize(2);
      for(var point : result.getPoints())
      {
         assertThat(drillCount(point.getDrill())).isEqualTo(1);
         assertThat(point.getDrill().getFilter().getSubFilters().getLast().getCriteria()).singleElement().satisfies(c ->
         {
            assertThat(c.getFieldName()).isEqualTo("id");
            assertThat(c.getOperator()).isEqualTo(QCriteriaOperator.EQUALS);
         });
      }
      assertThatThrownBy(() -> service.scatter(new ScatterRequest().withTableName("orders").withXField("id").withYField("amount").withLimit(2001))).isInstanceOf(QBadRequestException.class);
      assertThat(service.scatter(new ScatterRequest().withTableName("orders").withXField("id").withYField("amount").withLimit(1)).getTrend()).isNull();
      var constant = service.scatter(new ScatterRequest().withTableName("orders").withXField("id").withYField("quantity").withLimit(2));
      assertThat(constant.getTrend().getSlope()).isZero();
      assertThat(constant.getTrend().getIntercept()).isEqualTo(1);
      assertThat(constant.getTrend().getR()).isNull();
      assertThat(service.scatter(new ScatterRequest().withTableName("orders").withXField("quantity").withYField("id").withLimit(2)).getTrend()).isNull();
   }

   /*******************************************************************************
    ** Verify crosstab counts shares chi square and other.
    *******************************************************************************/
   @Test
   void crosstabCountsSharesChiSquareAndOther() throws Exception
   {
      var request = new CrosstabRequest().withTableName("orders").withRowField("status").withColumnField("customer");
      var result = service.crosstab(request);
      assertThat(result.getCells()).hasSize(4);
      assertThat(result.getCells()).extracting(c -> ((Number) c.getCount().getValue()).longValue()).containsExactlyInAnyOrder(4L, 4L, 2L, 2L);
      assertThat(result.getCells()).allSatisfy(c -> assertThat(((Number) c.getColumnShare().getValue()).doubleValue()).isEqualTo(.5));
      assertThat(result.getSignificance().getApplicable()).isFalse();
      relationshipDrills(result);
      relationshipDrills(service.crosstab(request.withLimit(1)));
      for(int i = 13; i <= 132; i++)
      {
         TestSupport.insert("orders", new QRecord().withValue("id", i).withValue("customer", i < 73 ? 1 : 2).withValue("status", i % 3 == 0 ? "NEW" : "PAID"));
      }
      var larger = service.crosstab(request.withLimit(20));
      assertThat(larger.getSignificance().getApplicable()).isTrue();
      assertThat(larger.getSignificance().getCramersV()).isNotNull();
      // scipy.stats.chi2_contingency([[24,22],[44,42]], correction=False)
      assertThat(larger.getSignificance().getStatistic()).isCloseTo(.012267293166002618, within(1e-12));
      assertThat(larger.getSignificance().getPValue()).isCloseTo(.9118083997104913, within(1e-12));
      relationshipDrills(larger);
   }

   /*******************************************************************************
    ** Verify shift psi and quartiles.
    *******************************************************************************/
   @Test
   void shiftPsiAndQuartiles() throws Exception
   {
      var request = new ShiftRequest().withTableName("orders").withField("status").withDateField("order_date").withBucket("DAY");
      var result = service.shift(request);
      assertThat(result.getBuckets()).hasSize(2).allSatisfy(b -> assertThat(b.getComplete()).isTrue());
      assertThat(((Number) result.getPsi().getValue()).doubleValue()).isZero();
      assertThat(result.getChart().getSeries()).hasSize(2);
      relationshipDrills(result);
      var shifted = service.shift(request.withField("customer"));
      // scipy.special.rel_entr([.5,.5], [1,1e-6]).sum() + scipy.special.rel_entr([1,1e-6], [.5,.5]).sum()
      assertThat(((Number) shifted.getPsi().getValue()).doubleValue()).isCloseTo(6.907742156618759, within(1e-10));
      var numeric = service.shift(request.withField("amount"));
      assertThat(numeric.getPsi().getValue()).isNotNull();
      assertNumber(numeric.getBuckets().getFirst().getMetrics(), "median", 5);
      relationshipDrills(numeric);
      for(String unit : List.of("WEEK", "MONTH", "QUARTER", "YEAR", "HOUR"))
      {
         var bucketed = service.shift(request.withDateField("created").withBucket(unit));
         relationshipDrills(bucketed);
      }
   }

   /*******************************************************************************
    ** Verify relationship privacy and full field security.
    *******************************************************************************/
   @Test
   void relationshipPrivacyAndFullFieldSecurity() throws Exception
   {
      config.withDefaultMinGroupSize(6);
      service.getConfigResolver().invalidate();
      var scatter = new ScatterRequest().withTableName("orders").withXField("id").withYField("amount");
      assertThat(service.scatter(scatter).getPoints()).allSatisfy(p -> assertThat(p.getDrill()).isNull());
      assertThat(service.scatter(scatter.withFilter(DrillFilters.condition("id", QCriteriaOperator.LESS_THAN, 4))).getPoints()).isEmpty();
      var comparison = service.compare(new CompareRequest().withTableName("orders").withFields(List.of("amount", "customer"))
         .withFilterA(DrillFilters.condition("id", QCriteriaOperator.LESS_THAN, 4)));
      assertThat(comparison.getFields().getFirst().getValues().getFirst().getSideA().getMetrics()).allSatisfy(m ->
      {
         assertThat(m.getValue()).isNull();
         assertThat(m.getDrill()).isNull();
      });
      assertThat(comparison.getFields().getFirst().getSignificance().getApplicable()).isFalse();
      var cross = service.crosstab(new CrosstabRequest().withTableName("orders").withRowField("status").withColumnField("customer"));
      assertThat(cross.getCells()).allSatisfy(c -> assertThat(c.getCount().getValue()).isNull());
      assertThat(cross.getSignificance().getApplicable()).isFalse();
      assertThat(service.shift(new ShiftRequest().withTableName("orders").withField("customer").withDateField("order_date").withBucket("DAY")).getPsi().getValue()).isNull();
      assertThat(service.correlation(new CorrelationRequest().withTableName("orders").withFields(List.of("id", "amount")).withFilter(scatter.getFilter())).getCells())
         .allSatisfy(c -> assertThat(c.getR()).isNull());
      for(String field : List.of("secret", "missing"))
      {
         assertThatThrownBy(() -> service.compare(new CompareRequest().withTableName("orders").withFields(List.of(field)))).isInstanceOf(QBadRequestException.class);
         assertThatThrownBy(() -> service.correlation(new CorrelationRequest().withTableName("orders").withFields(List.of("id", field)))).isInstanceOf(QBadRequestException.class);
         assertThatThrownBy(() -> service.scatter(new ScatterRequest().withTableName("orders").withXField("id").withYField(field))).isInstanceOf(QBadRequestException.class);
         assertThatThrownBy(() -> service.crosstab(new CrosstabRequest().withTableName("orders").withRowField("status").withColumnField(field))).isInstanceOf(QBadRequestException.class);
         assertThatThrownBy(() -> service.shift(new ShiftRequest().withTableName("orders").withField(field).withDateField("order_date"))).isInstanceOf(QBadRequestException.class);
      }
      QContext.init(instance, TestSupport.user("denied").withPermissions("orders.read"));
      assertThatThrownBy(() -> service.scatter(scatter)).isInstanceOf(QPermissionDeniedException.class);
   }

   /*******************************************************************************
    ** Verify relationship locks and resolver.
    *******************************************************************************/
   @Test
   void relationshipLocksAndResolver() throws Exception
   {
      QContext.getQSession().getSecurityKeyValues().remove("orders.all");
      QContext.getQSession().withSecurityKeyValue("customer_scope", 1);
      assertThat(service.scatter(new ScatterRequest().withTableName("orders").withXField("id").withYField("amount")).getN()).isEqualTo(8);
      var restricted = new DataAnalysisQBitConfig().withBackendName("memory").withFieldAnalysisAccessResolver(() -> (t, f) -> f.getName().equals("amount") ? FieldAnalysisAccess.NONE : FieldAnalysisAccess.FULL);
      service = newService(restricted, Runnable::run);
      assertThatThrownBy(() -> service.compare(new CompareRequest().withTableName("orders").withFields(List.of("amount")))).isInstanceOf(QBadRequestException.class);
   }

   /*******************************************************************************
    ** Verify top categories and empty populations.
    *******************************************************************************/
   @Test
   void topCategoriesAndEmptyPopulations() throws Exception
   {
      for(int i = 0; i < 300; i++)
      {
         TestSupport.insert("orders", new QRecord().withValue("id", 1000 + i).withValue("customer", 1)
            .withValue("note", "Category " + i % 15).withValue("paid", i % 2 == 0)
            .withValue("order_date", java.time.LocalDate.of(2025, 1, i < 150 ? 1 : 2)));
      }
      var filter = DrillFilters.condition("id", QCriteriaOperator.GREATER_THAN_OR_EQUALS, 1000);
      var compared = service.compare(new CompareRequest().withTableName("orders").withFields(List.of("note")).withFilterA(filter).withFilterB(filter));
      assertThat(compared.getFields().getFirst().getValues()).hasSize(11);
      assertThat(compared.getFields().getFirst().getValues().getLast().getLabel()).isEqualTo("Other");
      assertNumber(compared.getFields().getFirst().getValues().getLast().getSideA().getMetrics(), "count", 100);
      relationshipDrills(compared);
      var shift = service.shift(new ShiftRequest().withTableName("orders").withField("note").withDateField("order_date").withBucket("DAY").withFilter(filter));
      assertThat(shift.getChart().getSeries()).hasSize(9);
      assertThat(shift.getChart().getStacked()).isTrue();
      assertThat(((Number) shift.getPsi().getValue()).doubleValue()).isZero();
      relationshipDrills(shift);
      relationshipDrills(service.crosstab(new CrosstabRequest().withTableName("orders").withRowField("note").withColumnField("paid").withFilter(filter).withLimit(5)));
      var empty = DrillFilters.condition("id", QCriteriaOperator.LESS_THAN, 0);
      assertThat(service.compare(new CompareRequest().withTableName("orders").withFields(List.of("amount")).withFilterA(empty)).getFields().getFirst().getSignificance().getApplicable()).isFalse();
      assertThat(service.correlation(new CorrelationRequest().withTableName("orders").withFields(List.of("id", "amount")).withFilter(empty)).getCells()).allSatisfy(c -> assertThat(c.getR()).isNull());
      assertThat(service.crosstab(new CrosstabRequest().withTableName("orders").withRowField("note").withColumnField("paid").withFilter(empty)).getCells()).isEmpty();
      assertThat(service.shift(new ShiftRequest().withTableName("orders").withField("note").withDateField("order_date").withFilter(empty)).getPsi().getValue()).isNull();
   }

   /*******************************************************************************
    ** Verify future buckets do not enter psi and point drills respect resolver.
    *******************************************************************************/
   @Test
   void futureBucketsDoNotEnterPsiAndPointDrillsRespectResolver() throws Exception
   {
      var future = java.time.LocalDate.now(java.time.ZoneId.of("America/Chicago")).plusYears(1);
      TestSupport.insert("orders", new QRecord().withValue("id", 99).withValue("customer", 1).withValue("order_date", future).withValue("status", "NEW"));
      var shift = service.shift(new ShiftRequest().withTableName("orders").withField("status").withDateField("order_date").withBucket("DAY"));
      assertThat(shift.getBuckets().getLast().getComplete()).isFalse();
      assertThat(((Number) shift.getPsi().getValue()).doubleValue()).isZero();
      TestSupport.insert("orders", new QRecord().withValue("id", 98).withValue("customer", 1).withValue("amount", BigDecimal.ONE)
         .withValue("order_date", java.time.LocalDate.of(2024, 1, 1)).withValue("status", "NEW"));
      for(String field : List.of("amount", "status"))
      {
         var sparseFirst = service.shift(new ShiftRequest().withTableName("orders").withField(field).withDateField("order_date").withBucket("DAY"));
         assertThat(sparseFirst.getPsi().getValue()).isNull();
         assertThat(sparseFirst.getSentence()).contains("at least two values");
      }
      var restricted = new DataAnalysisQBitConfig().withBackendName("memory").withFieldAnalysisAccessResolver(() -> (t, f) -> f.getName().equals("id") ? FieldAnalysisAccess.NONE : FieldAnalysisAccess.FULL);
      service = newService(restricted, Runnable::run);
      assertThat(service.scatter(new ScatterRequest().withTableName("orders").withXField("amount").withYField("quantity")).getPoints()).hasSize(11).allSatisfy(p -> assertThat(p.getDrill()).isNull());
      restricted.withFieldAnalysisAccessResolver(() -> (t, f) -> f.getName().equals("amount") ? FieldAnalysisAccess.EXACT_MATCH_ONLY : FieldAnalysisAccess.FULL);
      service = newService(restricted, Runnable::run);
      assertThatThrownBy(() -> service.correlation(new CorrelationRequest().withTableName("orders").withFields(List.of("amount", "quantity")))).isInstanceOf(QBadRequestException.class);
   }

   /*******************************************************************************
    ** Verify assert number.
    *******************************************************************************/
   private static void assertNumber(List<Metric> metrics, String key, double expected)
   {
      assertThat(metrics.stream().filter(m -> m.getKey().equals(key)).findFirst().orElseThrow().getValue()).isNotNull();
      assertThat(new BigDecimal(metrics.stream().filter(m -> m.getKey().equals(key)).findFirst().orElseThrow().getValue().toString()).doubleValue()).isCloseTo(expected, within(1e-9));
   }

   /*******************************************************************************
    ** Verify drill count.
    *******************************************************************************/
   private static long drillCount(Drill drill) throws Exception
   {
      CountInput input = new CountInput(drill.getTableName()).withFilter(DrillFilters.restoreTypes(drill.getTableName(), drill.getFilter()));
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new CountAction().execute(input).getCount());
   }

   /*******************************************************************************
    ** Verify relationship drills.
    *******************************************************************************/
   private void relationshipDrills(Object result) throws Exception
   {
      walkRelationship(new ObjectMapper().readTree(JsonUtils.toJson(result)));
   }

   /*******************************************************************************
    ** Verify walk relationship.
    *******************************************************************************/
   private void walkRelationship(JsonNode node) throws Exception
   {
      if(node.isObject() && node.path("valueType").asText().equals("COUNT") && node.hasNonNull("drill"))
      {
         assertThat(drillCount(JsonUtils.toObject(node.get("drill").toString(), Drill.class))).as(node.toString()).isEqualTo(node.path("value").asLong());
      }
      if(node.isContainerNode())
      {
         for(JsonNode child : node)
         {
            walkRelationship(child);
         }
      }
   }
}
