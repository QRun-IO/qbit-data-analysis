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
import java.util.ArrayList;
import com.kingsrook.qbits.dataanalysis.result.Drill;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.PivotCell;
import com.kingsrook.qbits.dataanalysis.result.PivotDimension;
import com.kingsrook.qbits.dataanalysis.result.PivotHeader;
import com.kingsrook.qbits.dataanalysis.result.PivotMeasure;
import com.kingsrook.qbits.dataanalysis.result.PivotNumericBins;
import com.kingsrook.qbits.dataanalysis.result.PivotRequest;
import com.kingsrook.qbits.dataanalysis.result.PivotResult;
import com.kingsrook.qbits.dataanalysis.result.ValueType;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucketUnit;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/*******************************************************************************
 ** Pivot contract inherited by every existing backend test configuration.
 *******************************************************************************/
abstract class PivotContractTest extends EngineFixture
{
   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   protected PivotDimension dimension(String field)
   {
      return (new PivotDimension().withField(field));
   }

   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   protected PivotMeasure measure(PivotMeasure.Operator operator, String field)
   {
      return (new PivotMeasure().withOperator(operator).withField(field));
   }

   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   protected PivotRequest pivot(List<String> rows, List<String> columns)
   {
      return (new PivotRequest().withTableName("orders").withRows(rows.stream().map(this::dimension).toList())
         .withColumns(columns.stream().map(this::dimension).toList()).withValues(List.of(measure(PivotMeasure.Operator.COUNT, null))));
   }

   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   private void equalNumber(Metric metric, String value)
   {
      assertThat(new BigDecimal(metric.getValue().toString())).isEqualByComparingTo(value);
   }

   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   protected void countDrills(PivotResult result) throws Exception
   {
      List<List<Metric>> groups = new ArrayList<>(result.getRowTotals());
      groups.addAll(result.getColumnTotals());
      groups.add(result.getGrandTotals());
      result.getCells().forEach(c -> groups.add(c.getValues()));
      for(List<Metric> metrics : groups)
      {
         for(Metric metric : metrics)
         {
            if(metric.getKey().equals("value0") && !metric.getSuppressed())
            {
               assertThat(metric.getDrill()).isNotNull();
               Drill drill = JsonUtils.toObject(JsonUtils.toJson(metric.getDrill()), Drill.class);
               CountInput input = new CountInput(drill.getTableName()).withFilter(drill.getFilter());
               input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
               assertThat(new CountAction().execute(input).getCount()).as(JsonUtils.toJson(drill)).isEqualTo(((Number) metric.getValue()).intValue());
               QueryInput query = new QueryInput(drill.getTableName()).withFilter(drill.getFilter());
               query.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
               assertThat(new QueryAction().execute(query).getRecords()).as(JsonUtils.toJson(drill)).hasSize(((Number) metric.getValue()).intValue());
            }
         }
      }
   }

   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   @Test
   void pivotShapesAndTotals() throws Exception
   {
      for(PivotRequest request : List.of(pivot(List.of("status"), List.of("customer")), pivot(List.of("status", "paid"), List.of("customer")),
         pivot(List.of("status"), List.of("customer", "paid"))))
      {
         PivotResult result = service.pivot(request);
         equalNumber(result.getGrandTotals().getFirst(), "12");
         assertThat(result.getCells().stream().mapToLong(c -> ((Number)c.getValues().getFirst().getValue()).longValue()).sum()).isEqualTo(12);
         countDrills(result);
      }
      PivotResult result = service.pivot(pivot(List.of("status"), List.of("customer")));
      assertThat(result.getCells()).hasSize(4);
      assertThat(result.getCells()).allSatisfy(c -> assertThat(((Number)c.getValues().getFirst().getValue()).longValue()).isIn(2L, 4L));
      assertThat(result.getRowTotals()).allSatisfy(m -> equalNumber(m.getFirst(), "6"));
      result = service.pivot(pivot(List.of("status"), List.of()).withShowTotals(false));
      assertThat(result.getGrandTotals()).isEmpty();
      assertThat(result.getRowTotals()).isEmpty();
      assertThat(result.getColumnHeaders()).containsExactly(List.of());
   }

   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   @Test
   void pivotOtherAndNonAdditiveMeasures() throws Exception
   {
      PivotRequest request = pivot(List.of("quantity"), List.of("status")).withLimitGroups(1).withValues(List.of(
         measure(PivotMeasure.Operator.COUNT, null), measure(PivotMeasure.Operator.SUM, "amount"), measure(PivotMeasure.Operator.AVG, "amount"),
         measure(PivotMeasure.Operator.COUNT_DISTINCT, "amount"), measure(PivotMeasure.Operator.PERCENTILE, "amount").withPercentile(new BigDecimal("0.5"))));
      PivotResult result = service.pivot(request);
      assertThat(result.getTruncatedRows()).isTrue();
      assertThat(result.getTruncatedColumns()).isTrue();
      assertThat(result.getRowHeaders().stream().flatMap(List::stream).map(PivotHeader::getLabel)).anyMatch(l -> l.equals("Other (5 groups)"));
      equalNumber(result.getGrandTotals().get(1), "1170");
      equalNumber(result.getGrandTotals().get(3), "8");
      equalNumber(result.getGrandTotals().get(4), "20");
      countDrills(result);
      for(PivotCell cell : result.getCells())
      {
         var input = new com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput("orders").withFilter(cell.getValues().getFirst().getDrill().getFilter());
         input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         var numbers = new com.kingsrook.qqq.backend.core.actions.tables.QueryAction().execute(input).getRecords().stream().map(r -> r.getValueBigDecimal("amount"))
            .filter(java.util.Objects::nonNull).sorted().toList();
         BigDecimal sum = numbers.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
         equalNumber(cell.getValues().get(1), sum.toString());
         assertThat(new BigDecimal(cell.getValues().get(2).getValue().toString()).doubleValue()).isCloseTo(sum.doubleValue() / numbers.size(), within(0.000000001));
         equalNumber(cell.getValues().get(3), Long.toString(numbers.stream().map(BigDecimal::stripTrailingZeros).distinct().count()));
         BigDecimal median = numbers.size() % 2 == 1 ? numbers.get(numbers.size() / 2) : numbers.get(numbers.size() / 2 - 1).add(numbers.get(numbers.size() / 2)).divide(BigDecimal.valueOf(2));
         equalNumber(cell.getValues().get(4), median.toString());
      }
      for(String field : List.of("paid", "note", "amount"))
      {
         countDrills(service.pivot(pivot(List.of(field), List.of()).withLimitGroups(1)));
      }
   }

   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   @Test
   void pivotBuckets() throws Exception
   {
      for(DateBucketUnit unit : DateBucketUnit.values())
      {
         PivotRequest request = pivot(List.of("created"), List.of("status")).withRows(List.of(dimension("created").withDateBucket(unit)));
         PivotResult result = service.pivot(request);
         equalNumber(result.getGrandTotals().getFirst(), "12");
         countDrills(result);
      }
      PivotResult dates = service.pivot(pivot(List.of("order_date"), List.of("status")).withRows(List.of(dimension("order_date").withDateBucket(DateBucketUnit.DAY))));
      assertThat(dates.getCells()).hasSize(4);
      countDrills(dates);
      for(PivotNumericBins bins : List.of(new PivotNumericBins().withCount(4).withMin(BigDecimal.ZERO).withMax(BigDecimal.valueOf(40)), new PivotNumericBins().withCount(10)))
      {
         countDrills(service.pivot(pivot(List.of("amount"), List.of("status")).withRows(List.of(dimension("amount").withNumericBins(bins)))));
      }
   }

   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   @Test
   void pivotPrivacyAndUserFilter() throws Exception
   {
      config.withDefaultMinGroupSize(3);
      service.getConfigResolver().invalidate();
      PivotResult result = service.pivot(pivot(List.of("status"), List.of("customer")).withValues(List.of(measure(PivotMeasure.Operator.COUNT, null), measure(PivotMeasure.Operator.SUM, "amount"))));
      assertThat(result.getCells().stream().filter(c -> c.getValues().getFirst().getSuppressed())).hasSize(2);
      result.getCells().stream().filter(c -> c.getValues().getFirst().getSuppressed()).forEach(c -> assertThat(c.getValues()).allSatisfy(m ->
      {
         assertThat(m.getValue()).isNull();
         assertThat(m.getDrill()).isNull();
      }));
      assertThat(service.pivot(pivot(List.of("quantity"), List.of())).getRowHeaders()).isEmpty();
      config.withDefaultMinGroupSize(0);
      service.getConfigResolver().invalidate();
      var filter = DrillFilters.condition("customer", QCriteriaOperator.EQUALS, 1);
      String before = JsonUtils.toJson(filter);
      result = service.pivot(pivot(List.of("status"), List.of()).withFilter(filter));
      equalNumber(result.getGrandTotals().getFirst(), "8");
      assertThat(JsonUtils.toJson(filter)).isEqualTo(before);
      countDrills(result);
   }

   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   @Test
   void pivotJoinedDimensionAndPermissions() throws Exception
   {
      instance.addJoin(new QJoinMetaData().withName("ordersCustomer").withLeftTable("orders").withRightTable("customer")
         .withType(JoinType.MANY_TO_ONE).withJoinOn(new JoinOn("customer", "id")));
      instance.getTable("orders").withExposedJoin(new ExposedJoin().withJoinTable("customer").withJoinPath(List.of("ordersCustomer")));
      PivotResult result = service.pivot(pivot(List.of("customer.name"), List.of("status")));
      assertThat(result.getRowHeaders().stream().flatMap(List::stream).map(PivotHeader::getValue)).containsExactlyInAnyOrder("Alice Co", "Bob Co");
      equalNumber(result.getGrandTotals().getFirst(), "12");
      assertThat(result.getWarnings()).isEmpty();
      assertThat(result.getCells()).allSatisfy(c -> assertThat(JsonUtils.toJson(c.getValues().getFirst().getDrill())).contains("customer.name"));
      countDrills(result);
      countDrills(service.pivot(pivot(List.of("customer.name"), List.of("status")).withLimitGroups(1)
         .withFilter(DrillFilters.condition("paid", QCriteriaOperator.EQUALS, true))));
      instance.getJoins().put("ordersCustomer", instance.getJoin("ordersCustomer").flip());
      countDrills(service.pivot(pivot(List.of("customer.name"), List.of("status"))));
      QContext.getQSession().getPermissions().remove("customer.read");
      assertThatThrownBy(() -> service.pivot(pivot(List.of("customer.name"), List.of()))).isInstanceOf(com.kingsrook.qqq.backend.core.exceptions.QBadRequestException.class);
   }

   /*******************************************************************************
    ** Reversing a many-to-one join multiplies records and disables every drill.
    *******************************************************************************/
   @Test
   void pivotToManyDimension() throws Exception
   {
      instance.addJoin(new QJoinMetaData().withName("ordersCustomer").withLeftTable("orders").withRightTable("customer")
         .withType(JoinType.MANY_TO_ONE).withJoinOn(new JoinOn("customer", "id")));
      instance.getTable("customer").withExposedJoin(new ExposedJoin().withJoinTable("orders").withJoinPath(List.of("ordersCustomer")));
      QContext.getQSession().getPermissions().add("customer.analyze");
      for(int direction = 0; direction < 2; direction++)
      {
         PivotResult result = service.pivot(pivot(List.of("orders.status"), List.of()).withTableName("customer"));
         equalNumber(result.getGrandTotals().getFirst(), "12");
         assertThat(result.getWarnings()).containsExactly("Counts include one row per related " + instance.getTable("orders").getLabel()
            + "; drill-through is not available for this dimension.");
         assertThat(result.getCells()).allSatisfy(c -> assertThat(c.getValues()).allSatisfy(m -> assertThat(m.getDrill()).isNull()));
         assertThat(result.getRowTotals()).allSatisfy(metrics -> assertThat(metrics).allSatisfy(m -> assertThat(m.getDrill()).isNull()));
         assertThat(result.getColumnTotals()).allSatisfy(metrics -> assertThat(metrics).allSatisfy(m -> assertThat(m.getDrill()).isNull()));
         assertThat(result.getGrandTotals()).allSatisfy(m -> assertThat(m.getDrill()).isNull());
         instance.getJoins().put("ordersCustomer", instance.getJoin("ordersCustomer").flip());
      }
   }

   /*******************************************************************************
    ** Every hop must be to-one, including hops preceding a one-to-one target.
    *******************************************************************************/
   @Test
   void pivotMultiHopDimension() throws Exception
   {
      instance.addTable(new com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData().withName("region").withLabel("Region")
         .withBackendName(instance.getTable("customer").getBackendName()).withPrimaryKeyField("id")
         .withField(new com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData("id", com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType.INTEGER))
         .withField(new com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData("name", com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType.STRING)));
      if(connection != null)
      {
         try(var statement = connection.createStatement())
         {
            statement.execute("DROP TABLE IF EXISTS region");
            statement.execute("CREATE TABLE region (id INTEGER PRIMARY KEY, name VARCHAR(100))");
         }
      }
      new com.kingsrook.qqq.backend.core.instances.QInstanceEnricher(instance).enrich();
      TestSupport.insert("region", new com.kingsrook.qqq.backend.core.model.data.QRecord().withValue("id", 1).withValue("name", "East"));
      TestSupport.insert("region", new com.kingsrook.qqq.backend.core.model.data.QRecord().withValue("id", 2).withValue("name", "West"));
      instance.addJoin(new QJoinMetaData().withName("ordersCustomer").withLeftTable("orders").withRightTable("customer")
         .withType(JoinType.MANY_TO_ONE).withJoinOn(new JoinOn("customer", "id")));
      instance.addJoin(new QJoinMetaData().withName("customerRegion").withLeftTable("region").withRightTable("customer")
         .withType(JoinType.ONE_TO_ONE).withJoinOn(new JoinOn("id", "id")));
      instance.getTable("orders").withExposedJoin(new ExposedJoin().withJoinTable("region").withJoinPath(List.of("ordersCustomer", "customerRegion")));
      PivotResult result = service.pivot(pivot(List.of("region.name"), List.of("status")));
      assertThat(result.getWarnings()).containsExactly("Drill-through is not available for fields reached through more than one join (Region).");
      assertThat(result.getCells()).allSatisfy(c -> assertThat(c.getValues().getFirst().getDrill()).isNull());
      instance.getJoin("ordersCustomer").withType(JoinType.MANY_TO_MANY);
      result = service.pivot(pivot(List.of("region.name"), List.of("status")));
      assertThat(result.getWarnings()).containsExactly("Counts include one row per related Region; drill-through is not available for this dimension.");
      assertThat(result.getCells()).allSatisfy(c -> assertThat(c.getValues().getFirst().getDrill()).isNull());
   }

   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   @Test
   void pivotValidation() throws Exception
   {
      for(String field : List.of("secret", "missing", "missing.field"))
      {
         assertThatThrownBy(() -> service.pivot(pivot(List.of(field), List.of()))).isInstanceOf(com.kingsrook.qqq.backend.core.exceptions.QBadRequestException.class);
         assertThatThrownBy(() -> service.pivot(pivot(List.of("status"), List.of()).withValues(List.of(measure(PivotMeasure.Operator.MIN, field)))))
            .isInstanceOf(com.kingsrook.qqq.backend.core.exceptions.QBadRequestException.class);
      }
      for(PivotRequest bad : List.of(pivot(List.of(), List.of()), pivot(List.of("status"), List.of()).withLimitGroups(201),
         pivot(List.of("status"), List.of("status")), pivot(List.of("status"), List.of()).withRows(List.of(dimension("status").withDateBucket(DateBucketUnit.YEAR))),
         pivot(List.of("status"), List.of()).withValues(List.of(measure(PivotMeasure.Operator.PERCENTILE, "amount").withPercentile(BigDecimal.TEN)))))
      {
         assertThatThrownBy(() -> service.pivot(bad)).isInstanceOf(com.kingsrook.qqq.backend.core.exceptions.QBadRequestException.class);
      }
   }
   /*******************************************************************************
    ** Verify or support the pivot and field-access contract.
    *******************************************************************************/
   @Test
   void pivotTypedExtremesAndEmptyScope() throws Exception
   {
      PivotResult result = service.pivot(pivot(List.of("status"), List.of()).withValues(List.of(measure(PivotMeasure.Operator.COUNT, "amount"),
         measure(PivotMeasure.Operator.MIN, "order_date"), measure(PivotMeasure.Operator.MAX, "created"), measure(PivotMeasure.Operator.MIN, "amount"),
         measure(PivotMeasure.Operator.MAX, "amount"))));
      equalNumber(result.getGrandTotals().getFirst(), "12");
      equalNumber(result.getGrandTotals().get(3), "-10");
      equalNumber(result.getGrandTotals().get(4), "1000");
      assertThat(result.getGrandTotals().get(1).getValueType()).isEqualTo(ValueType.DATE);
      assertThat(result.getGrandTotals().get(2).getValueType()).isEqualTo(ValueType.DATE_TIME);
      countDrills(result);
      result = service.pivot(pivot(List.of("status"), List.of()).withFilter(DrillFilters.condition("id", QCriteriaOperator.EQUALS, -1)));
      assertThat(result.getCells()).isEmpty();
      equalNumber(result.getGrandTotals().getFirst(), "0");
      countDrills(result);
   }

   @Test
   void booleanHeadersAndHumanMeasureLabels() throws Exception
   {
      var booleans = service.pivot(pivot(List.of("paid"), List.of("status")));
      assertThat(booleans.getRowHeaders().stream().flatMap(List::stream).map(PivotHeader::getLabel)).containsExactlyInAnyOrder("Yes", "No", "(Blank)");
      assertThat(booleans.getMeasures().getFirst().getLabel()).isEqualTo("Count");
      var operators = List.of(PivotMeasure.Operator.AVG, PivotMeasure.Operator.SUM, PivotMeasure.Operator.COUNT_DISTINCT,
         PivotMeasure.Operator.MIN, PivotMeasure.Operator.MAX, PivotMeasure.Operator.PERCENTILE);
      var measures = operators.stream().map(operator -> measure(operator, "amount")).toList();
      measures.getLast().withPercentile(new BigDecimal("0.5"));
      var result = service.pivot(pivot(List.of("paid"), List.of()).withValues(measures));
      assertThat(result.getMeasures().stream().map(m -> m.getLabel())).containsExactly("Average Amount", "Sum of Amount", "Distinct Amount", "Minimum Amount", "Maximum Amount", "Median Amount");
      measures.getLast().withPercentile(new BigDecimal("0.9"));
      assertThat(service.pivot(pivot(List.of("paid"), List.of()).withValues(measures)).getMeasures().getLast().getLabel()).isEqualTo("90th percentile of Amount");
      var cross = service.crosstab(new com.kingsrook.qbits.dataanalysis.result.CrosstabRequest().withTableName("orders").withRowField("paid").withColumnField("status"));
      assertThat(cross.getRows().stream().map(PivotHeader::getLabel)).containsExactlyInAnyOrder("Yes", "No", "(Blank)");
   }
}
