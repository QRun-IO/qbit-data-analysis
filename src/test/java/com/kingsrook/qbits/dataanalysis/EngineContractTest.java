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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisBackend;
import com.kingsrook.qbits.dataanalysis.engine.ColumnProfiler;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.engine.StandInAnalysisBackend;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfile;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfileRequest;
import com.kingsrook.qbits.dataanalysis.result.DistinctValuesRequest;
import com.kingsrook.qbits.dataanalysis.result.Drill;
import com.kingsrook.qbits.dataanalysis.result.DuplicatesRequest;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.RunStatus;
import com.kingsrook.qbits.dataanalysis.result.TableProfileRequest;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/*******************************************************************************
 ** Shared backend acceptance tests, including a generic drill equivalence walker.
 *******************************************************************************/
abstract class EngineContractTest extends RelationshipContractTest
{
   /*******************************************************************************
    ** Every emitted key across all profile kinds has a label, on every backend.
    *******************************************************************************/
   @Test
   void presentationAcrossProfileKinds() throws Exception
   {
      java.util.Set<String> keys = new java.util.HashSet<>();
      for(String field : List.of("amount", "quantity", "note", "paid", "order_date", "created"))
      {
         ColumnProfile profile = service.columnProfile(column(field));
         java.util.List<Metric> metrics = new java.util.ArrayList<>(profile.getCounts());
         if(profile.getNumeric() != null)
         {
            metrics.addAll(profile.getNumeric().getMetrics());
         }
         if(profile.getText() != null)
         {
            metrics.addAll(profile.getText().getMetrics());
         }
         if(profile.getDates() != null)
         {
            metrics.addAll(profile.getDates().getMetrics());
            assertThat(profile.getChart().getX().getLabel()).endsWith(" (day)");
            assertThat(profile.getChart().getSeries().getFirst().getPoints().getFirst().getX().toString()).isEqualTo("2026-01-01");
         }
         if(profile.getBooleans() != null)
         {
            metrics.addAll(profile.getBooleans());
         }
         for(Metric metric : metrics)
         {
            keys.add(metric.getKey());
            assertThat(metric.getLabel()).isEqualTo(com.kingsrook.qbits.dataanalysis.engine.MetricLabels.label(metric.getKey(), metric.getValueType()));
            assertThat(metric.getFormatted()).isNotBlank();
         }
      }
      assertThat(keys).containsExactlyInAnyOrder("rows", "nonNull", "nulls", "blanks", "distinct", "uniqueness", "min", "max", "mean", "sum", "stddev",
         "p1", "p5", "p25", "p50", "p75", "p95", "p99", "zeros", "negatives", "outliers", "minLength", "maxLength", "meanLength", "true", "false", "range");
      ColumnProfile amount = service.columnProfile(column("amount"));
      assertThat(metric(amount.getNumeric().getMetrics(), "sum").getFormatted()).isEqualTo("$1,170.00");
      assertThat(amount.getNumeric().getHistogram()).allSatisfy(v -> assertThat(v.getLabel()).contains("$"));
      Metric uniqueness = metric(amount.getCounts(), "uniqueness");
      assertThat(uniqueness.getValueType().name()).isEqualTo("PERCENT");
      assertThat(uniqueness.getValue().toString()).isEqualTo("0.727273");
      assertThat(uniqueness.getFormatted()).isEqualTo("72.7%");
      assertThat(metric(service.columnProfile(column("created")).getDates().getMetrics(), "min").getFormatted()).isEqualTo("2026-01-01 06:00");
      assertThat(service.columnProfile(column("note")).getWarnings()).isEmpty();
      var table = service.tableProfile(new TableProfileRequest().withTableName("orders").withFields(List.of("note")));
      assertThat(table.getRowCount().getLabel()).isEqualTo("Rows");
      assertThat(table.getFields().getFirst().getNulls().getLabel()).isEqualTo("Nulls");
      assertThat(table.getFields().getFirst().getBlanks().getLabel()).isEqualTo("Blanks");
   }

   /*******************************************************************************
    ** Decimal moments retain ten significant digits; drills retain exact membership.
    *******************************************************************************/
   @Test
   void numericProfile() throws Exception
   {
      ColumnProfile result = service.columnProfile(column("amount"));
      assertMetric(result.getCounts(), "rows", "12");
      assertMetric(result.getCounts(), "nonNull", "11");
      assertMetric(result.getCounts(), "nulls", "1");
      assertMetric(result.getCounts(), "distinct", "8");
      Map<String, String> expected = Map.ofEntries(Map.entry("min", "-10"), Map.entry("max", "1000"), Map.entry("sum", "1170"),
         Map.entry("mean", "106.36363636363636"), Map.entry("p1", "-9"), Map.entry("p5", "-5"), Map.entry("p25", "5"),
         Map.entry("p50", "20"), Map.entry("p75", "35"), Map.entry("p95", "525"), Map.entry("p99", "905"),
         Map.entry("zeros", "2"), Map.entry("negatives", "1"), Map.entry("outliers", "1"));
      for(var entry : expected.entrySet())
      {
         assertMetric(result.getNumeric().getMetrics(), entry.getKey(), new BigDecimal(entry.getValue()).round(new java.math.MathContext(10)).toPlainString());
      }
      double[] values = {-10, 0, 0, 10, 10, 20, 20, 30, 40, 50, 1000};
      double mean = 1170.0 / 11;
      double variance = java.util.Arrays.stream(values).map(v -> Math.pow(v - mean, 2)).sum() / 10;
      assertMetric(result.getNumeric().getMetrics(), "stddev", BigDecimal.valueOf(Math.sqrt(variance)).round(new java.math.MathContext(10)).toPlainString());
      assertThat(result.getNumeric().getHistogram()).hasSize(22);
      assertThat(result.getNumeric().getHistogram().stream().mapToLong(v -> v.getCount()).sum()).isEqualTo(11);
      assertThat(result.getNumeric().getOutliers()).singleElement().satisfies(v -> assertThat(new BigDecimal(v.getValue().toString())).isEqualByComparingTo("1000"));
      assertThat(result.getChart().getKind().name()).isEqualTo("HISTOGRAM");
      assertDrills(result);
      assertDrills(service.columnProfile(column("quantity")));
   }

   /*******************************************************************************
    ** Text nulls, whitespace, lengths, shapes, enum labels and FK labels.
    *******************************************************************************/
   @Test
   void textAndLabels() throws Exception
   {
      ColumnProfile text = service.columnProfile(column("note"));
      assertMetric(text.getCounts(), "nulls", "1");
      assertMetric(text.getCounts(), "blanks", "4");
      assertMetric(text.getCounts(), "distinct", "10");
      assertMetric(text.getText().getMetrics(), "minLength", "0");
      assertMetric(text.getText().getMetrics(), "maxLength", "5");
      assertMetric(text.getText().getMetrics(), "meanLength", Double.toString(28.0 / 11));
      assertThat(text.getText().getPatterns()).anySatisfy(v ->
      {
         assertThat(v.getLabel()).isEqualTo("A-9");
         assertThat(v.getCount()).isEqualTo(3);
      });
      assertDrills(text);
      ColumnProfile customer = service.columnProfile(column("customer"));
      assertThat(customer.getTopValues()).extracting(v -> v.getLabel()).containsExactly("Alice Co", "Bob Co");
      assertThat(customer.getChart().getKind().name()).isEqualTo("BAR");
      assertDrills(customer);
      ColumnProfile status = service.columnProfile(column("status"));
      assertThat(status.getTopValues()).extracting(v -> v.getLabel()).containsExactlyInAnyOrder("New order", "Paid order");
      assertDrills(status);
   }

   /*******************************************************************************
    ** Date gaps, timezone buckets, moving average and boolean counts.
    *******************************************************************************/
   @Test
   void datesAndBooleans() throws Exception
   {
      ColumnProfile date = service.columnProfile(column("order_date"));
      assertThat(date.getDates().getUnit()).isEqualTo("DAY");
      assertThat(date.getDates().getBuckets()).extracting(v -> v.getCount()).containsExactly(4L, 0L, 8L);
      assertThat(date.getDates().getGaps()).hasSize(1);
      assertThat(date.getChart().getSeries()).hasSize(2);
      assertMetric(date.getDates().getMetrics(), "range", "2");
      assertDrills(date);
      ColumnProfile instant = service.columnProfile(column("created"));
      assertThat(instant.getDates().getHourOfDay()).extracting(v -> v.getValue().toString()).containsExactlyInAnyOrder("6", "12");
      assertDrills(instant);
      ColumnProfile bool = service.columnProfile(column("paid"));
      assertMetric(bool.getBooleans(), "true", "6");
      assertMetric(bool.getBooleans(), "false", "5");
      assertMetric(bool.getBooleans(), "nulls", "1");
      assertDrills(bool);
   }

   /*******************************************************************************
    ** Completeness, field and pair keys and duplicate tuples.
    *******************************************************************************/
   @Test
   void tableAndDuplicates() throws Exception
   {
      var table = service.tableProfile(new TableProfileRequest().withTableName("orders"));
      assertThat(table.getFields()).noneMatch(f -> f.getField().equals("secret"));
      assertThat(table.getFields()).anySatisfy(f ->
      {
         assertThat(f.getField()).isEqualTo("note");
         assertThat(f.getCompleteness().doubleValue()).isEqualTo(0.666667);
      });
      assertThat(table.getCandidateKeys()).anyMatch(k -> k.getFields().equals(List.of("id")));
      var pairs = service.tableProfile(new TableProfileRequest().withTableName("orders").withFields(List.of("status", "quantity")));
      assertThat(pairs.getCandidateKeys()).anyMatch(k -> Set.copyOf(k.getFields()).equals(Set.of("status", "quantity")));
      assertDrills(table);
      assertDrills(pairs);
      var duplicates = service.duplicates(new DuplicatesRequest().withTableName("orders").withFields(List.of("amount")));
      assertThat(duplicates.getGroups()).extracting(v -> v.getCount()).containsExactly(2L, 2L, 2L);
      assertDrills(duplicates);
      assertDrills(service.duplicates(new DuplicatesRequest().withTableName("orders").withFields(List.of("status", "customer"))));
   }

   /*******************************************************************************
    ** Paging and search retain exact memberships and enforce page size.
    *******************************************************************************/
   @Test
   void distinctPages() throws Exception
   {
      var first = service.distinctValues(new DistinctValuesRequest().withTableName("orders").withFieldName("note").withPageSize(2));
      var second = service.distinctValues(new DistinctValuesRequest().withTableName("orders").withFieldName("note").withPageSize(2).withPage(1));
      assertThat(first.getHasMore()).isTrue();
      assertThat(first.getValues()).hasSize(2);
      assertThat(second.getValues()).hasSize(2);
      assertThat(first.getValues().getFirst().getValue()).isNotEqualTo(second.getValues().getFirst().getValue());
      assertDrills(first);
      assertDrills(second);
      var search = service.distinctValues(new DistinctValuesRequest().withTableName("orders").withFieldName("note").withSearch("AA"));
      assertThat(search.getValues()).hasSize(1);
      assertDrills(search);
      assertThatThrownBy(() -> service.distinctValues(new DistinctValuesRequest().withTableName("orders").withFieldName("note").withPageSize(501))).isInstanceOf(QException.class);
   }

   /*******************************************************************************
    ** Missing READ/analyze, locked fields and exclusions fail before computation.
    *******************************************************************************/
   @Test
   void permissionChecks() throws Exception
   {
      assertThat(service.capabilities("orders").getFields()).doesNotContain("secret");
      assertThatThrownBy(() -> service.columnProfile(column("secret"))).isInstanceOf(com.kingsrook.qqq.backend.core.exceptions.QBadRequestException.class);
      QContext.init(instance, TestSupport.user("alice").withPermission("orders.analyze"));
      assertThatThrownBy(() -> service.columnProfile(column("amount"))).isInstanceOf(QPermissionDeniedException.class);
      QContext.init(instance, TestSupport.user("alice").withPermission("orders.read"));
      assertThatThrownBy(() -> service.columnProfile(column("amount"))).isInstanceOf(QPermissionDeniedException.class);
      QContext.init(instance, session());
      config.withTableDefaults(Map.of("orders", new TableAnalysisDefaults().withExcludedFields(Set.of("amount"))));
      service.getConfigResolver().invalidate();
      assertThat(service.capabilities("orders").getFields()).doesNotContain("amount");
      assertThatThrownBy(() -> service.columnProfile(column("amount"))).isInstanceOf(com.kingsrook.qqq.backend.core.exceptions.QBadRequestException.class);
      assertThatThrownBy(() -> service.capabilities("missing")).isInstanceOf(QPermissionDeniedException.class);
   }

   /*******************************************************************************
    ** Record locks constrain every backend primitive and every returned drill.
    *******************************************************************************/
   @Test
   void recordLocks() throws Exception
   {
      QSession alice = TestSupport.user("alice").withPermissions("orders.read", "orders.analyze", "customer.read",
         "data_analysis.run.read", "data_analysis.run.insert", "data_analysis.run.edit").withSecurityKeyValue("customer_scope", 1);
      QContext.init(instance, alice);
      var result = service.columnProfile(column("amount"));
      assertMetric(result.getCounts(), "rows", "8");
      assertMetric(result.getNumeric().getMetrics(), "sum", "120");
      assertDrills(result);
      assertDrills(service.tableProfile(new TableProfileRequest().withTableName("orders")));
      assertDrills(service.distinctValues(new DistinctValuesRequest().withTableName("orders").withFieldName("note")));
      assertDrills(service.duplicates(new DuplicatesRequest().withTableName("orders").withFields(List.of("quantity"))));
      QContext.getQSession().withSecurityKeyValue("customer_scope", 2);
      assertMetric(service.columnProfile(column("amount")).getCounts(), "rows", "12");
   }
   /*******************************************************************************
    ** Guarded cache JSON contains no unmerged rare top values or extreme listings.
    *******************************************************************************/
   @Test
   void privacyGuard() throws Exception
   {
      config.withDefaultMinGroupSize(3);
      service.getConfigResolver().invalidate();
      ColumnProfile note = service.columnProfile(column("note"));
      assertThat(note.getTopValues()).singleElement().satisfies(v ->
      {
         assertThat(v.getLabel()).startsWith("Other (");
         assertThat(v.getCount()).isEqualTo(12);
         assertThat(v.getDrill()).isNull();
         assertThat(v.getValue()).isNull();
      });
      ColumnProfile amount = service.columnProfile(column("amount"));
      assertThat(amount.getNumeric().getOutliers()).isEmpty();
      assertThat(metric(amount.getNumeric().getMetrics(), "outliers").getDrill()).isNull();
      var tiny = service.columnProfile(column("amount").withFilter(DrillFilters.condition("id", QCriteriaOperator.EQUALS, 1)));
      for(String key : List.of("min", "max", "mean", "stddev", "p1", "p50", "p99"))
      {
         assertThat(metric(tiny.getNumeric().getMetrics(), key).getSuppressed()).isTrue();
         assertThat(metric(tiny.getNumeric().getMetrics(), key).getValue()).isNull();
         assertThat(metric(tiny.getNumeric().getMetrics(), key).getSuppressedReason()).isEqualTo("Hidden: fewer than 3 records");
      }
      var page = service.distinctValues(new DistinctValuesRequest().withTableName("orders").withFieldName("note").withPageSize(1));
      assertThat(page.getValues()).hasSize(1);
      assertThat(page.getValues().getFirst().getCount()).isEqualTo(12);
      assertThat(page.getHasMore()).isFalse();
      for(QRecord run : runs())
      {
         JsonNode json = new ObjectMapper().readTree(run.getValueString("result_json"));
         for(JsonNode group : json.path("topValues"))
         {
            if(group.path("count").asLong() < 3)
            {
               assertThat(group.path("drill").isMissingNode() || group.path("drill").isNull()).isTrue();
               assertThat(group.path("value").isMissingNode() || group.path("value").isNull()).isTrue();
            }
         }
      }
   }

   /*******************************************************************************
    ** Real asynchronous context propagation, completion, cache hit and invalidation.
    *******************************************************************************/
   @Test
   void estimatesRunsAndCache() throws Exception
   {
      config.withDefaultSampleThresholdRows(5).withDefaultSampleRows(4);
      service = newService(config, java.util.concurrent.ForkJoinPool.commonPool());
      ColumnProfile estimate = service.columnProfile(column("amount"));
      assertThat(estimate.getEstimate()).isTrue();
      assertMetric(estimate.getCounts(), "rows", "12");
      assertThat(metric(estimate.getCounts(), "rows").getEstimate()).isFalse();
      assertThat(metric(estimate.getCounts(), "nonNull").getEstimate()).isTrue();
      assertThat(metric(estimate.getCounts(), "nonNull").getFormatted()).startsWith("≈ ");
      assertDrills(estimate);
      assertThat(count(metric(estimate.getCounts(), "rows").getDrill())).isEqualTo(12);
      if(service.backend("orders") instanceof com.kingsrook.qbits.dataanalysis.engine.CoreAnalysisBackend)
      {
         assertThat(estimate.getWarnings()).anyMatch(w -> w.contains("random sample") && w.contains("scaled to the full 12 rows"));
      }
      else
      {
         assertThat(estimate.getSampleRows()).isEqualTo(4);
         assertMetric(estimate.getCounts(), "nonNull", "4");
         assertMetric(estimate.getNumeric().getMetrics(), "sum", "10");
         assertThat(estimate.getWarnings()).contains("Estimate from the first 4 rows by id");
      }
      String id = service.startExactRun(column("amount"));
      RunStatus run = awaitRun(id);
      assertThat(run.getStatus()).isEqualTo("COMPLETE");
      assertMetric(run.getColumnProfile().getCounts(), "rows", "12");
      int before = runs().size();
      assertThat(service.startExactRun(column("amount"))).isEqualTo(id);
      assertMetric(service.columnProfile(column("amount").withExact(true)).getCounts(), "rows", "12");
      assertThat(runs()).hasSize(before);
      TestSupport.insert("orders", new QRecord().withValue("id", 13).withValue("customer", 1).withValue("amount", BigDecimal.ONE));
      String next = service.startExactRun(column("amount"));
      assertThat(next).isNotEqualTo(id);
      assertThat(awaitRun(next).getStatus()).isEqualTo("COMPLETE");
      assertMetric(service.getRun(next).getColumnProfile().getCounts(), "rows", "13");
      assertThat(service.tableProfile(new TableProfileRequest().withTableName("orders")).getEstimate()).isTrue();
      String table = service.startExactRun(new TableProfileRequest().withTableName("orders").withFields(List.of("id", "amount")));
      assertThat(awaitRun(table).getTableProfile().getRowCount().getValue().toString()).isEqualTo("13");
   }

   /*******************************************************************************
    ** Partial results retain exact prefix drills; empty populations are valid.
    *******************************************************************************/
   @Test
   void truncatedAndEmpty() throws Exception
   {
      config.withDefaultMaxRows(3);
      service.getConfigResolver().invalidate();
      ColumnProfile partial = service.columnProfile(column("amount").withExact(true));
      assertThat(partial.getTruncated()).isTrue();
      assertThat(partial.getRowsScanned()).isEqualTo(3);
      assertMetric(partial.getCounts(), "rows", "3");
      assertDrills(partial);
      var emptyFilter = DrillFilters.condition("id", QCriteriaOperator.EQUALS, -1);
      var empty = service.columnProfile(column("amount").withFilter(emptyFilter));
      assertMetric(empty.getCounts(), "rows", "0");
      assertThat(empty.getNumeric().getHistogram()).isEmpty();
      assertDrills(empty);
      assertDrills(service.columnProfile(column("created").withFilter(emptyFilter)));
      assertDrills(service.columnProfile(column("note").withFilter(emptyFilter)));
   }

   /*******************************************************************************
    ** Drill composition never modifies the request's nested OR filter.
    *******************************************************************************/
   @Test
   void originalFilterUnchanged() throws Exception
   {
      QQueryFilter filter = DrillFilters.condition("id", QCriteriaOperator.EQUALS, 1).withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withCriteria("id", QCriteriaOperator.EQUALS, 12);
      String original = JsonUtils.toJson(filter);
      ColumnProfile result = service.columnProfile(column("amount").withFilter(filter));
      assertMetric(result.getCounts(), "rows", "2");
      assertDrills(result);
      assertThat(JsonUtils.toJson(filter)).isEqualTo(original);
   }

   /*******************************************************************************
    ** Config rows override defaults and restore the session after privileged reads.
    *******************************************************************************/
   @Test
   void configResolution() throws Exception
   {
      config.withDefaultMaxRows(500).withTableDefaults(Map.of("orders", new TableAnalysisDefaults().withMaxRows(100).withSampleRows(8).withMinGroupSize(2)));
      service.getConfigResolver().invalidate();
      assertThat(service.capabilities("orders").getConfig().getMaxRows()).isEqualTo(100);
      TestSupport.insert(DataAnalysisTables.TABLE_CONFIG, new QRecord().withValue("table_name", "orders").withValue("max_rows", 50)
         .withValue("min_group_size", 3).withValue("analyzable_fields", "amount, note").withValue("excluded_fields", "note").withValue("cache_ttl_minutes", 2));
      assertThat(service.capabilities("orders").getConfig().getMaxRows()).isEqualTo(100);
      service.getConfigResolver().invalidate();
      QSession current = QContext.getQSession();
      var capabilities = service.capabilities("orders");
      assertThat(capabilities.getConfig().getMaxRows()).isEqualTo(50);
      assertThat(capabilities.getConfig().getSampleRows()).isEqualTo(8);
      assertThat(capabilities.getConfig().getMinGroupSize()).isEqualTo(3);
      assertThat(capabilities.getConfig().getCacheTtlMinutes()).isEqualTo(2);
      assertThat(capabilities.getFields()).containsExactly("amount");
      assertThat(QContext.getQSession()).isSameAs(current);
      config.withExcludeTables(Set.of("orders"));
      service.getConfigResolver().invalidate();
      assertThatThrownBy(() -> service.capabilities("orders")).isInstanceOf(QPermissionDeniedException.class);
   }

   /*******************************************************************************
    ** Changed privacy policy and another user cannot retrieve older cached data.
    *******************************************************************************/
   @Test
   void cachePolicyRechecked() throws Exception
   {
      String id = service.startExactRun(column("amount"));
      config.withDefaultMinGroupSize(3);
      service.getConfigResolver().invalidate();
      assertThatThrownBy(() -> service.getRun(id)).isInstanceOf(QPermissionDeniedException.class);
      assertThat(service.startExactRun(column("amount"))).isNotEqualTo(id);
      QContext.init(instance, TestSupport.user("bob").withPermissions("data_analysis.run.read", "orders.read", "orders.analyze"));
      assertThatThrownBy(() -> service.getRun(id)).isInstanceOf(QPermissionDeniedException.class);
   }

   /*******************************************************************************
    ** Calendar boundaries, weekly/monthly/yearly drills and interpolation.
    *******************************************************************************/
   @Test
   void shapeAndDateRules() throws Exception
   {
      assertThat(StandInAnalysisBackend.shape("AAA-999 QQ_88!!")).isEqualTo("A-9 A_9!!");
      LocalDate first = LocalDate.of(2024, 1, 1);
      assertThat(ColumnProfiler.autoBucket(first, first.plusDays(92))).isEqualTo(AnalysisBackend.DateUnit.DAY);
      assertThat(ColumnProfiler.autoBucket(first, first.plusDays(93))).isEqualTo(AnalysisBackend.DateUnit.WEEK);
      assertThat(ColumnProfiler.autoBucket(first, first.plusYears(2))).isEqualTo(AnalysisBackend.DateUnit.WEEK);
      assertThat(ColumnProfiler.autoBucket(first, first.plusYears(2).plusDays(1))).isEqualTo(AnalysisBackend.DateUnit.MONTH);
      assertThat(ColumnProfiler.autoBucket(first, first.plusYears(10))).isEqualTo(AnalysisBackend.DateUnit.MONTH);
      assertThat(ColumnProfiler.autoBucket(first, first.plusYears(10).plusDays(1))).isEqualTo(AnalysisBackend.DateUnit.YEAR);
      for(AnalysisBackend.DateUnit unit : List.of(AnalysisBackend.DateUnit.WEEK, AnalysisBackend.DateUnit.MONTH, AnalysisBackend.DateUnit.QUARTER, AnalysisBackend.DateUnit.YEAR))
      {
         var buckets = service.backend("orders").dateBuckets("orders", null, "order_date", unit, java.time.ZoneId.of("America/Chicago"));
         assertThat(buckets.stream().mapToLong(AnalysisBackend.GroupCount::count).sum()).isEqualTo(12);
         for(var bucket : buckets)
         {
            assertThat(count(new Drill().withTableName("orders").withFilter(bucket.condition()))).isEqualTo(bucket.count());
         }
      }
      assertThat(StandInAnalysisBackend.percentile(List.of(BigDecimal.ZERO, BigDecimal.TEN), new BigDecimal("0.25"))).isEqualByComparingTo("2.5");
   }

   /*******************************************************************************
    ** Common request builder.
    *******************************************************************************/
   protected static ColumnProfileRequest column(String field)
   {
      return (new ColumnProfileRequest().withTableName("orders").withFieldName(field));
   }

   /*******************************************************************************
    ** Select metrics by their stable semantic keys.
    *******************************************************************************/
   protected static Metric metric(List<Metric> metrics, String key)
   {
      return (metrics.stream().filter(m -> m.getKey().equals(key)).findFirst().orElseThrow());
   }

   /*******************************************************************************
    ** Scalar reference tolerance.
    *******************************************************************************/
   protected static void assertMetric(List<Metric> metrics, String key, String expected)
   {
      assertThat(new BigDecimal(metric(metrics, key).getValue().toString()).doubleValue()).as(key).isCloseTo(Double.parseDouble(expected), within(1e-9));
   }

   /*******************************************************************************
    ** Walk actual serialized payloads so cached drills receive the same assertions.
    *******************************************************************************/
   protected void assertDrills(Object result) throws Exception
   {
      walk(new ObjectMapper().readTree(JsonUtils.toJson(result)));
   }

   /*******************************************************************************
    ** Collect count metrics, group counts, chart points and candidate-key counts.
    *******************************************************************************/
   private void walk(JsonNode node) throws Exception
   {
      if(node.path("estimate").asBoolean(false))
      {
         return;
      }
      if(node.isObject())
      {
         if(node.hasNonNull("drill"))
         {
            JsonNode number = node.has("count") ? node.get("count") : node.has("rowCount") ? node.get("rowCount") : node.has("y") ? node.get("y") : node.get("value");
            assertThat(number).as(node.toString()).isNotNull();
            Drill drill = JsonUtils.toObject(node.get("drill").toString(), Drill.class);
            assertThat(count(drill)).as(node.toString()).isEqualTo(number.asLong());
         }
         var fields = node.fields();
         while(fields.hasNext())
         {
            var field = fields.next();
            if(!field.getKey().equals("drill") && !field.getKey().equals("filter"))
            {
               walk(field.getValue());
            }
         }
      }
      else if(node.isArray())
      {
         for(JsonNode child : node)
         {
            walk(child);
         }
      }
   }

   /*******************************************************************************
    ** Use core count under the current user's locks.
    *******************************************************************************/
   private static long count(Drill drill) throws Exception
   {
      CountInput input = new CountInput(drill.getTableName()).withFilter(drill.getFilter());
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new CountAction().execute(input).getCount());
   }

   /*******************************************************************************
    ** Inspect persistent cache rows through enforced actions.
    *******************************************************************************/
   protected List<QRecord> runs() throws Exception
   {
      QueryInput input = new QueryInput(DataAnalysisTables.RUN).withShouldFetchHeavyFields(true);
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new QueryAction().execute(input).getRecords());
   }

   /*******************************************************************************
    ** Wait for actual asynchronous completion with a fixed deadline.
    *******************************************************************************/
   protected RunStatus awaitRun(String id) throws Exception
   {
      long deadline = System.nanoTime() + 20_000_000_000L;
      RunStatus run;
      do
      {
         run = service.getRun(id);
         if(!"RUNNING".equals(run.getStatus()))
         {
            return (run);
         }
         Thread.sleep(10);
      }
      while(System.nanoTime() < deadline);
      throw new AssertionError("Run did not finish: " + id);
   }


   /*******************************************************************************
    ** Related labels obey READ, field locks and record locks without bypasses.
    *******************************************************************************/
   @Test
   void relatedLabelSecurity() throws Exception
   {
      String original = service.startExactRun(column("customer"));
      QContext.getQSession().getPermissions().remove("customer.read");
      assertThat(service.columnProfile(column("customer")).getTopValues()).extracting(v -> v.getLabel()).containsExactly("1", "2");
      QContext.getQSession().withPermission("customer.read");
      instance.getTable("customer").getField("name").withFieldSecurityLock(new com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock()
         .withSecurityKeyType("secret_access").withDefaultBehavior(com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock.Behavior.DENY));
      assertThatThrownBy(() -> service.getRun(original)).isInstanceOf(QPermissionDeniedException.class);
      assertThat(service.columnProfile(column("customer")).getTopValues()).extracting(v -> v.getLabel()).containsExactly("1", "2");
      instance.getTable("customer").getField("name").withFieldSecurityLock(null);
      instance.getTable("customer").withRecordSecurityLock(new com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock()
         .withSecurityKeyType("customer_scope").withFieldName("id"));
      QContext.getQSession().getSecurityKeyValues().remove("orders.all");
      QContext.getQSession().withSecurityKeyValue("customer_scope", 1);
      assertThat(service.columnProfile(column("customer")).getTopValues()).extracting(v -> v.getLabel()).containsExactly("Alice Co");
   }

   /*******************************************************************************
    ** Zero/constant populations, zero limits, rejected executors and disabled cache.
    *******************************************************************************/
   @Test
   void boundaryAndFailurePaths() throws Exception
   {
      assertThat(DataAnalysisRuntime.service()).isNotNull();
      var constant = service.columnProfile(column("amount").withFilter(DrillFilters.condition("id", QCriteriaOperator.EQUALS, 2)));
      assertThat(constant.getNumeric().getHistogram().stream().mapToLong(v -> v.getCount()).sum()).isEqualTo(1);
      assertDrills(constant);
      config.withDefaultMaxRows(0).withDefaultCacheTtlMinutes(0);
      service.getConfigResolver().invalidate();
      assertMetric(service.columnProfile(column("amount")).getCounts(), "rows", "0");
      int previous = runs().size();
      service.columnProfile(column("amount"));
      assertThat(runs()).hasSize(previous + 1);
      service = newService(config, task ->
      {
         throw new java.util.concurrent.RejectedExecutionException("test");
      });
      assertThatThrownBy(() -> service.startExactRun(column("amount"))).isInstanceOf(QException.class);
      assertThat(runs()).anyMatch(r -> "FAILED".equals(r.getValueString("status")));
      assertThatThrownBy(() -> service.getRun("bad")).isInstanceOf(QException.class);
      assertThatThrownBy(() -> service.duplicates(new DuplicatesRequest().withTableName("orders"))).isInstanceOf(QException.class);
      assertThatThrownBy(() -> service.columnProfile(null)).isInstanceOf(QException.class);
      assertThatThrownBy(() -> service.startExactRun(new DistinctValuesRequest().withTableName("orders").withFieldName("note"))).isInstanceOf(QException.class);
   }

   /*******************************************************************************
    ** Decimal scales, integral boundaries and final-bin closure must be portable.
    *******************************************************************************/
   @Test
   void scaledDecimalsAndIntegerBins() throws Exception
   {
      TestSupport.insert("orders", new QRecord().withValue("id", 13).withValue("customer", 1).withValue("amount", new BigDecimal("0.00")));
      TestSupport.insert("orders", new QRecord().withValue("id", 14).withValue("customer", 1).withValue("amount", new BigDecimal("10.000")));
      var result = service.columnProfile(column("amount"));
      assertMetric(result.getCounts(), "distinct", "8");
      assertMetric(result.getNumeric().getMetrics(), "zeros", "3");
      assertThat(result.getNumeric().getHistogram().stream().mapToLong(v -> v.getCount()).sum()).isEqualTo(13);
      assertDrills(result);
      var quantity = service.columnProfile(column("quantity"));
      assertMetric(quantity.getNumeric().getMetrics(), "mean", "3.5");
      assertThat(quantity.getNumeric().getHistogram().stream().mapToLong(v -> v.getCount()).sum()).isEqualTo(12);
      assertDrills(quantity);
      var filtered = column("amount").withFilter(DrillFilters.condition("amount", QCriteriaOperator.EQUALS, new BigDecimal("0.00")));
      var selected = service.columnProfile(filtered);
      assertThat(((Number) metric(selected.getCounts(), "rows").getValue()).longValue()).isEqualTo(count(new Drill().withTableName("orders").withFilter(filtered.getFilter())));
      assertDrills(selected);
      assertDrills(service.columnProfile(filtered));
   }


   /*******************************************************************************
    ** Expired cache rows miss; reordered AND criteria share normalized identity.
    *******************************************************************************/
   @Test
   void cacheExpiryAndNormalizedFilters() throws Exception
   {
      QQueryFilter a = DrillFilters.condition("id", QCriteriaOperator.GREATER_THAN, 1).withCriteria("id", QCriteriaOperator.LESS_THAN, 8);
      QQueryFilter b = DrillFilters.condition("id", QCriteriaOperator.LESS_THAN, 8).withCriteria("id", QCriteriaOperator.GREATER_THAN, 1);
      String id = service.startExactRun(column("amount").withFilter(a));
      assertThat(service.startExactRun(column("amount").withFilter(b))).isEqualTo(id);
      var update = new com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput(DataAnalysisTables.RUN)
         .withRecords(List.of(new QRecord().withValue("id", Integer.valueOf(id)).withValue("finished_at", java.time.Instant.now().minusSeconds(7200))));
      update.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      new com.kingsrook.qqq.backend.core.actions.tables.UpdateAction().execute(update);
      assertThat(service.startExactRun(column("amount").withFilter(a))).isNotEqualTo(id);
      var backend = service.backend("customer");
      assertThat(com.kingsrook.qbits.dataanalysis.run.DataVersion.of(backend, "customer", null)).isEqualTo("2:id:2");
   }

   /*******************************************************************************
    ** Local midnight date drills span 23 hours across daylight-saving transitions.
    *******************************************************************************/
   @Test
   void daylightSavingAndSearch() throws Exception
   {
      TestSupport.insert("orders", new QRecord().withValue("id", 13).withValue("customer", 1).withValue("created", java.time.Instant.parse("2026-03-08T06:00:00Z")));
      TestSupport.insert("orders", new QRecord().withValue("id", 14).withValue("customer", 1).withValue("created", java.time.Instant.parse("2026-03-09T04:59:59Z")));
      TestSupport.insert("orders", new QRecord().withValue("id", 15).withValue("customer", 1).withValue("created", java.time.Instant.parse("2026-03-09T05:00:00Z")));
      var drill = new Drill().withTableName("orders").withFilter(DrillFilters.dateRange("created", LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 9), true, java.time.ZoneId.of("America/Chicago")));
      assertThat(count(drill)).isEqualTo(2);
      assertDrills(service.columnProfile(column("created").withFilter(DrillFilters.condition("id", QCriteriaOperator.GREATER_THAN_OR_EQUALS, 13))));
      var values = service.distinctValues(new DistinctValuesRequest().withTableName("orders").withFieldName("amount").withSearch("1000"));
      assertThat(values.getValues()).hasSize(1);
      assertThat(values.getTotalDistinct()).isEqualTo(1);
      assertDrills(values);
   }

   /*******************************************************************************
    ** Computation failures persist a safe FAILED row, with no partial result JSON.
    *******************************************************************************/
   @Test
   void executionFailureAndGuardedBooleans() throws Exception
   {
      List<Runnable> jobs = new java.util.ArrayList<>();
      service = newService(config, jobs::add);
      String id = service.startExactRun(column("amount"));
      var table = instance.getTables().remove("orders");
      jobs.getFirst().run();
      instance.addTable(table);
      assertThat(service.getRun(id).getStatus()).isEqualTo("FAILED");
      assertThat(service.getRun(id).getError()).contains("could not be completed");
      assertThat(runs().getFirst().getValue("result_json")).isNull();
      config.withDefaultMinGroupSize(3);
      service = newService(config, Runnable::run);
      var paid = service.columnProfile(column("paid"));
      assertThat(paid.getBooleans()).anySatisfy(m ->
      {
         assertThat(m.getKey()).isEqualTo("other");
         assertThat(m.getDrill()).isNull();
      });
      assertDrills(paid);
      assertThat(service.columnProfile(column("created")).getDates().getGaps()).isEmpty();
      assertThat(service.duplicates(new DuplicatesRequest().withTableName("orders").withFields(List.of("quantity"))).getGroups())
         .allMatch(v -> v.getDrill() == null);
   }
}
