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

package com.kingsrook.qbits.dataanalysis.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dataanalysis.result.ChartKind;
import com.kingsrook.qbits.dataanalysis.result.CompareRequest;
import com.kingsrook.qbits.dataanalysis.result.CompareResult;
import com.kingsrook.qbits.dataanalysis.result.ComparisonSide;
import com.kingsrook.qbits.dataanalysis.result.ComparisonValue;
import com.kingsrook.qbits.dataanalysis.result.FieldComparison;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.Point;
import com.kingsrook.qbits.dataanalysis.result.Series;
import com.kingsrook.qbits.dataanalysis.result.ValueType;
import com.kingsrook.qbits.dataanalysis.stats.Statistics;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;

/*******************************************************************************
 ** Two populations share category boundaries and exact producing predicates.
 *******************************************************************************/
public class ComparisonExplorer extends RelationshipSupport
{
   /*******************************************************************************
    ** Bind request-local policy without modifying session metadata.
    *******************************************************************************/
   public ComparisonExplorer(AnalysisSecurity security, EffectiveConfig config, String table)
   {
      super(security, config, table);
   }

   /*******************************************************************************
    ** Resolve every field before issuing source queries.
    *******************************************************************************/
   public CompareResult compare(CompareRequest request) throws QException
   {
      if(request.getFields() == null || request.getFields().isEmpty() || request.getFields().size() > 100
         || request.getFields().stream().distinct().count() != request.getFields().size())
      {
         throw new QBadRequestException("Choose 1 to 100 distinct analysis fields.");
      }
      for(String field : request.getFields())
      {
         full(field);
      }
      CompareResult result = new CompareResult();
      for(String field : request.getFields())
      {
         result.getFields().add(numeric(full(field)) ? numericComparison(request, field) : categoryComparison(request, field));
      }
      return (result);
   }

   /*******************************************************************************
    ** All moments and quartiles are computed together by core, including VAR_SAMP.
    *******************************************************************************/
   protected Map<String, BigDecimal> moments(String field, QQueryFilter filter) throws QException
   {
      Map<String, Aggregate> measures = new LinkedHashMap<>();
      measures.put("count", measure(field, AggregateOperator.COUNT));
      measures.put("mean", measure(field, AggregateOperator.AVG));
      measures.put("variance", measure(field, AggregateOperator.VAR_SAMP));
      measures.put("min", measure(field, AggregateOperator.MIN));
      measures.put("max", measure(field, AggregateOperator.MAX));
      for(int p : List.of(25, 50, 75))
      {
         measures.put("p" + p, measure(field, AggregateOperator.PERCENTILE_CONT).withPercentile(BigDecimal.valueOf(p, 2)));
      }
      var row = summarize(filter, new ArrayList<>(measures.values()));
      Map<String, BigDecimal> result = new LinkedHashMap<>();
      measures.forEach((name, aggregate) -> result.put(name, decimal(row.getAggregateValues().get(aggregate))));
      result.putIfAbsent("count", BigDecimal.ZERO);
      return (result);
   }

   /*******************************************************************************
    ** Two BOX series retain all five summary values; mean and median get drills.
    *******************************************************************************/
   private FieldComparison numericComparison(CompareRequest request, String field) throws QException
   {
      Map<String, BigDecimal> a = moments(field, request.getFilterA());
      Map<String, BigDecimal> b = moments(field, request.getFilterB());
      long nA = a.get("count").longValue();
      long nB = b.get("count").longValue();
      ComparisonSide sideA = numericSide(field, request.getLabelA(), request.getFilterA(), a);
      ComparisonSide sideB = numericSide(field, request.getLabelB(), request.getFilterB(), b);
      ComparisonValue value = new ComparisonValue().withSideA(sideA).withSideB(sideB).withLabel(field);
      boolean hidden = nA < config.getMinGroupSize() || nB < config.getMinGroupSize();
      if(!hidden)
      {
         deltas(value, a.get("mean"), b.get("mean"));
      }
      Statistics.Result test = hidden ? Statistics.unavailable("Welch's t-test", "privacy guard")
         : Statistics.welch(nA, number(a, "mean"), number(a, "variance"), nB, number(b, "mean"), number(b, "variance"));
      return (new FieldComparison().withField(field).withValues(List.of(value)).withSignificance(significance(test))
         .withChart(chart(ChartKind.BOX, List.of(box(sideA), box(sideB)), "Two box summaries; delta is B minus A.")));
   }

   /*******************************************************************************
    ** Null moments remain invalid test inputs rather than becoming zero variance.
    *******************************************************************************/
   private static double number(Map<String, BigDecimal> values, String key)
   {
      return (values.get(key) == null ? Double.NaN : values.get(key).doubleValue());
   }

   /*******************************************************************************
    ** Count means non-null numeric records; every summary drills to that set.
    *******************************************************************************/
   protected ComparisonSide numericSide(String field, String label, QQueryFilter filter, Map<String, BigDecimal> values)
   {
      ComparisonSide result = new ComparisonSide().withLabel(label);
      for(String key : List.of("count", "mean", "min", "p25", "p50", "p75", "max"))
      {
         String name = key.equals("p50") ? "median" : key;
         result.getMetrics().add(metric(name, values.get(key), key.equals("count") ? ValueType.COUNT : ValueType.DECIMAL, filter, DrillFilters.nonNull(field, false)));
      }
      guard(result.getMetrics(), values.get("count").longValue());
      return (result);
   }

   /*******************************************************************************
    ** Only guarded summary metrics enter charts.
    *******************************************************************************/
   private Series box(ComparisonSide side)
   {
      Series series = new Series().withName(side.getLabel());
      side.getMetrics().stream().filter(m -> !List.of("count", "mean").contains(m.getKey()) && m.getValue() != null)
         .forEach(m -> series.getPoints().add(new Point().withX(m.getKey()).withY(decimal(m.getValue())).withLabel(m.getLabel()).withDrill(m.getDrill())));
      return (series);
   }

   /*******************************************************************************
    ** Top ten across the union gives both populations the same categories.
    ** Expected counts below five pool into Other before display and inference.
    *******************************************************************************/
   private FieldComparison categoryComparison(CompareRequest request, String field) throws QException
   {
      QQueryFilter union = request.getFilterA() == null || request.getFilterB() == null ? null
         : new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withSubFilter(DrillFilters.copy(request.getFilterA())).withSubFilter(DrillFilters.copy(request.getFilterB()));
      long nA = size(request.getFilterA());
      long nB = size(request.getFilterB());
      List<Category> kept = new ArrayList<>();
      List<Category> other = new ArrayList<>();
      boolean privateOther = false;
      for(Category category : categories(field, union, 10))
      {
         long a = size(DrillFilters.and(request.getFilterA(), category.condition()));
         long b = size(DrillFilters.and(request.getFilterB(), category.condition()));
         boolean hidden = category.privateGroup() || a < config.getMinGroupSize() || b < config.getMinGroupSize();
         double pooled = nA + nB == 0 ? 0 : (double) (a + b) / (nA + nB);
         if(hidden || Math.min(nA * pooled, nB * pooled) < 5 || category.label().equals("Other") && category.value() == null)
         {
            other.add(category);
            privateOther |= hidden;
         }
         else
         {
            kept.add(category);
         }
      }
      if(!other.isEmpty())
      {
         kept.add(merge(other, privateOther));
      }
      double[][] counts = new double[2][kept.size()];
      FieldComparison result = new FieldComparison().withField(field);
      Series seriesA = new Series().withName(request.getLabelA());
      Series seriesB = new Series().withName(request.getLabelB());
      for(int i = 0; i < kept.size(); i++)
      {
         Category category = kept.get(i);
         long a = size(DrillFilters.and(request.getFilterA(), category.condition()));
         long b = size(DrillFilters.and(request.getFilterB(), category.condition()));
         counts[0][i] = a;
         counts[1][i] = b;
         ComparisonSide sideA = categorySide(request.getLabelA(), request.getFilterA(), category, a, nA);
         ComparisonSide sideB = categorySide(request.getLabelB(), request.getFilterB(), category, b, nB);
         ComparisonValue value = new ComparisonValue().withValue(category.value()).withLabel(category.label()).withSideA(sideA).withSideB(sideB);
         boolean hidden = a < config.getMinGroupSize() || b < config.getMinGroupSize();
         if(!hidden)
         {
            deltas(value, ProfileSupport.share(a, nA), ProfileSupport.share(b, nB));
         }
         value.withSignificance(significance(hidden || category.privateGroup() ? Statistics.unavailable("Two-proportion z-test", "privacy guard") : Statistics.proportions(a, nA, b, nB)));
         result.getValues().add(value);
         categoryPoint(seriesA, category, sideA);
         categoryPoint(seriesB, category, sideB);
      }
      result.withSignificance(significance(privateOther ? Statistics.unavailable("Chi-square homogeneity", "privacy guard") : Statistics.chiSquare(counts, true)));
      return (result.withChart(chart(ChartKind.BAR, List.of(seriesA, seriesB), "Grouped bars; delta is B minus A. Low expected counts are merged into Other.")));
   }

   /*******************************************************************************
    ** Sensitive merged categories reveal no predicate or original value.
    *******************************************************************************/
   private ComparisonSide categorySide(String label, QQueryFilter filter, Category category, long n, long total)
   {
      ComparisonSide side = new ComparisonSide().withLabel(label).withMetrics(List.of(metric("count", n, ValueType.COUNT, filter, category.condition()),
         metric("share", ProfileSupport.share(n, total), ValueType.PERCENT, filter, category.condition())));
      if(category.privateGroup())
      {
         side.getMetrics().forEach(m -> m.withDrill(null));
      }
      guard(side.getMetrics(), n);
      return (side);
   }

   /*******************************************************************************
    ** Charts reflect only released count metrics.
    *******************************************************************************/
   private void categoryPoint(Series series, Category category, ComparisonSide side)
   {
      Metric count = side.getMetrics().getFirst();
      series.getPoints().add(new Point().withX(category.label()).withLabel(category.label()).withY(decimal(count.getValue())).withDrill(count.getDrill()));
   }

   /*******************************************************************************
    ** Percent delta is a fraction relative to |A|; zero baseline is undefined.
    *******************************************************************************/
   private void deltas(ComparisonValue value, BigDecimal a, BigDecimal b)
   {
      BigDecimal delta = a == null || b == null ? null : b.subtract(a);
      value.withDelta(ProfileSupport.metric("delta", delta, ValueType.DECIMAL));
      value.withPercentDelta(ProfileSupport.metric("percentDelta", delta == null || a.signum() == 0 ? null : delta.divide(a.abs(), MathContext.DECIMAL128), ValueType.PERCENT));
   }
}
