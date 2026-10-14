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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.kingsrook.qbits.dataanalysis.result.Chart;
import com.kingsrook.qbits.dataanalysis.result.ChartKind;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.Series;
import com.kingsrook.qbits.dataanalysis.result.Significance;
import com.kingsrook.qbits.dataanalysis.result.ValueType;
import com.kingsrook.qbits.dataanalysis.stats.Statistics;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;

/*******************************************************************************
 ** Shared core execution and semantic output for request-local relationships.
 *******************************************************************************/
public class RelationshipSupport extends StandInAnalysisBackend
{
   protected final AnalysisSecurity security;
   protected final String table;

   /*******************************************************************************
    ** A bucket retains exact membership; privacy buckets intentionally lack drills.
    *******************************************************************************/
   protected record Category(Serializable value, String label, QQueryFilter condition, boolean privateGroup)
   {
   }

   /*******************************************************************************
    ** Bind the caller's already-authorized table and current policy.
    *******************************************************************************/
   public RelationshipSupport(AnalysisSecurity security, EffectiveConfig config, String table)
   {
      super(config);
      this.security = security;
      this.table = table;
   }

   /*******************************************************************************
    ** Core owns pushdown and bounded fallback. Never label a partial test exact.
    *******************************************************************************/
   @Override
   protected List<AggregateResult> aggregate(AggregateInput input) throws QException
   {
      input.withFilter(DrillFilters.restoreTypes(table, input.getFilter())).withAllowStreamedFallback(true).withStreamedRowLimit(config.getMaxRows());
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      var output = new AggregateAction().execute(input);
      if(Boolean.TRUE.equals(output.getTruncated()))
      {
         throw new QBadRequestException("Analysis exceeds the streamed row limit; narrow the filter.");
      }
      return (output.getResults());
   }

   /*******************************************************************************
    ** All named fields, including PVS numerics, must pass the FULL resolver first.
    *******************************************************************************/
   protected QFieldMetaData full(String name) throws QException
   {
      return (security.field(table, name));
   }

   /*******************************************************************************
    ** PVS identifiers are categorical even when their storage type is numeric.
    *******************************************************************************/
   protected boolean numeric(QFieldMetaData field)
   {
      return (field.getPossibleValueSourceName() == null && List.of(QFieldType.INTEGER, QFieldType.LONG, QFieldType.DECIMAL).contains(field.getType()));
   }

   /*******************************************************************************
    ** Reject strings and categorical identifiers before computing relationships.
    *******************************************************************************/
   protected void requireNumeric(String name) throws QException
   {
      if(!numeric(full(name)))
      {
         throw new QBadRequestException("Choose numeric analysis fields.");
      }
   }

   /*******************************************************************************
    ** Typed aggregate keys must match keys returned by all backend implementations.
    *******************************************************************************/
   protected Aggregate measure(String field, AggregateOperator operator)
   {
      Aggregate aggregate = new Aggregate(field, operator);
      return (aggregate.withFieldType(com.kingsrook.qqq.backend.core.utils.aggregates.AggregateAnalysis.resultType(aggregate, field(table, field).getType())));
   }

   /*******************************************************************************
    ** Scalar summaries have one result; empty populations have null numeric values.
    *******************************************************************************/
   protected AggregateResult summarize(QQueryFilter filter, List<Aggregate> measures) throws QException
   {
      AggregateInput input = new AggregateInput(table).withFilter(DrillFilters.copy(filter));
      measures.forEach(input::withAggregate);
      List<AggregateResult> results = aggregate(input);
      return (results.isEmpty() ? new AggregateResult() : results.getFirst());
   }

   /*******************************************************************************
    ** Exact row count uses the core count action with the caller's record locks.
    *******************************************************************************/
   protected long size(QQueryFilter filter) throws QException
   {
      return (count(table, filter));
   }

   /*******************************************************************************
    ** Count and scalar drills preserve the caller's original OR predicates.
    *******************************************************************************/
   protected Metric metric(String key, Serializable value, ValueType type, QQueryFilter filter, QQueryFilter condition)
   {
      return (ProfileSupport.metric(key.startsWith("category") ? type == ValueType.COUNT ? "count" : "share" : key, value, type).withKey(key).withDrill(DrillFilters.drill(table, filter, condition)));
   }

   /*******************************************************************************
    ** Apply the stricter cell guard to remove both values and membership drills.
    *******************************************************************************/
   protected void guard(List<Metric> metrics, long n)
   {
      new MinGroupSizeGuard(config.getMinGroupSize()).pivotMetrics(metrics, n);
   }

   /*******************************************************************************
    ** Group identities are typed; null and empty string remain distinct.
    *******************************************************************************/
   protected List<Category> categories(String field, QQueryFilter filter, int limit) throws QException
   {
      QFieldMetaData metadata = full(field);
      GroupBy group = new GroupBy(metadata);
      Aggregate count = measure(primaryKey(table), AggregateOperator.COUNT);
      List<AggregateResult> groups = new ArrayList<>(aggregate(new AggregateInput(table).withFilter(DrillFilters.copy(filter)).withGroupBy(group).withAggregate(count)));
      groups.sort(Comparator.<AggregateResult>comparingLong(r -> ((Number) r.getAggregateValues().get(count)).longValue()).reversed()
         .thenComparing(r -> String.valueOf(r.getGroupByValues().get(group))));
      List<Category> result = new ArrayList<>();
      List<Category> other = new ArrayList<>();
      boolean privacy = false;
      for(AggregateResult row : groups)
      {
         Serializable value = row.getGroupByValues().get(group);
         long n = ((Number) row.getAggregateValues().get(count)).longValue();
         boolean hidden = n < config.getMinGroupSize();
         Category category = new Category(value, hidden ? "Other" : value == null ? "(blank)" : security.label(metadata, value),
            DrillFilters.value(field, value, metadata.getType() == QFieldType.STRING), hidden);
         if(hidden || result.size() >= limit)
         {
            other.add(category);
            privacy |= hidden;
         }
         else
         {
            result.add(category);
         }
      }
      if(!other.isEmpty())
      {
         result.add(merge(other, privacy));
      }
      return (result);
   }

   /*******************************************************************************
    ** An explicit union avoids null-sensitive NOT IN semantics for Other.
    *******************************************************************************/
   protected Category merge(List<Category> categories, boolean privacy)
   {
      QQueryFilter condition = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR);
      categories.forEach(c -> condition.withSubFilter(c.condition()));
      return (new Category(null, "Other", condition, privacy));
   }

   /*******************************************************************************
    ** Keep the math package free of QQQ and JSON dependencies.
    *******************************************************************************/
   protected Significance significance(Statistics.Result result)
   {
      return (new Significance().withTest(result.test()).withStatistic(result.statistic()).withDf(result.df()).withPValue(result.pValue())
         .withApplicable(result.applicable()).withSentence(result.sentence()).withCramersV(result.cramersV()));
   }

   /*******************************************************************************
    ** Scalar conversion preserves null instead of leaking non-finite JSON numbers.
    *******************************************************************************/
   protected static BigDecimal decimal(Object value)
   {
      return (value == null ? null : new BigDecimal(value.toString()));
   }

   /*******************************************************************************
    ** Shared chart shape with a deliberate grouping/stacking annotation.
    *******************************************************************************/
   protected Chart chart(ChartKind kind, List<Series> series, String annotation)
   {
      return (new Chart().withKind(kind).withSeries(series).withAnnotations(List.of(annotation)));
   }

   /*******************************************************************************
    ** Correlation and regression exclude rows missing either numeric coordinate.
    *******************************************************************************/
   protected QQueryFilter paired(QQueryFilter filter, String x, String y)
   {
      return (DrillFilters.and(filter, DrillFilters.condition(x, QCriteriaOperator.IS_NOT_BLANK).withCriteria(y, QCriteriaOperator.IS_NOT_BLANK)));
   }
}
