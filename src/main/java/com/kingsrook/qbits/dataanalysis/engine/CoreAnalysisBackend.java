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
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateCapability;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucketUnit;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.NumericBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;

/*******************************************************************************
 ** Capability-directed aggregates; streamed primitives retain portable drills.
 *******************************************************************************/
public class CoreAnalysisBackend extends StandInAnalysisBackend
{
   private BigDecimal samplePercent;
   private QQueryFilter populationFilter;
   private long populationRows;
   private long aggregateRows;
   private BigDecimal lastCountShare;

   /*******************************************************************************
    ** Bind the same bounded configuration as the streamed implementation.
    *******************************************************************************/
   public CoreAnalysisBackend(EffectiveConfig config)
   {
      super(config);
   }

   /*******************************************************************************
    ** Ask the actual backend implementation, including dialect-specific support.
    *******************************************************************************/
   public static Set<AggregateCapability> capabilities(String table) throws QException
   {
      AggregateInput input = new AggregateInput(table);
      return (new QBackendModuleDispatcher().getQBackendModule(input.getBackend()).getAggregateInterface().getSupportedCapabilities(input));
   }

   /*******************************************************************************
    ** Sampling belongs to each aggregate, never to a materialized key filter.
    *******************************************************************************/
   @Override
   public Scope scope(String table, QQueryFilter filter, boolean exact) throws QException
   {
      samplePercent = null;
      long total = super.count(table, filter);
      if(exact || total <= config.getSampleThresholdRows() || total <= config.getSampleRows() || !capabilities(table).contains(AggregateCapability.SAMPLING))
      {
         return (super.scope(table, filter, exact));
      }
      estimating = true;
      populationRows = total;
      populationFilter = DrillFilters.copy(filter);
      samplePercent = BigDecimal.valueOf(config.getSampleRows()).multiply(BigDecimal.valueOf(100))
         .divide(BigDecimal.valueOf(total), MathContext.DECIMAL128).min(BigDecimal.valueOf(100));
      long seen = sampledCount(table, populationFilter);
      return (new Scope(DrillFilters.copy(filter), total, seen, false, true, samplePercent));
   }

   /*******************************************************************************
    ** Count the sampled call's actual rows rather than assuming the requested size.
    *******************************************************************************/
   private long sampledCount(String table, QQueryFilter filter) throws QException
   {
      if(samplePercent.signum() == 0)
      {
         return (0);
      }
      List<AggregateResult> result = super.aggregate(new AggregateInput(table).withFilter(DrillFilters.copy(filter))
         .withAggregate(countOf(primaryKey(table), AggregateOperator.COUNT)).withSamplePercent(samplePercent));
      return (result.isEmpty() ? 0 : number(result.getFirst(), primaryKey(table), AggregateOperator.COUNT).longValue());
   }

   /*******************************************************************************
    ** Unpaginated groups supply their own denominator, including null groups.
    ** Limited groups use a separate sampled population count for this call.
    *******************************************************************************/
   @Override
   protected List<AggregateResult> aggregate(AggregateInput input) throws QException
   {
      if(samplePercent == null)
      {
         return (super.aggregate(input));
      }
      if(samplePercent.signum() == 0)
      {
         aggregateRows = 0;
         return (List.of());
      }
      Aggregate count = countOf(primaryKey(input.getTableName()), AggregateOperator.COUNT);
      if(input.getAggregates() == null || !input.getAggregates().contains(count))
      {
         input.withAggregate(count);
      }
      input.withSamplePercent(samplePercent);
      boolean fullPopulation = JsonUtils.toJson(DrillFilters.copy(input.getFilter())).equals(JsonUtils.toJson(populationFilter));
      List<AggregateResult> result = super.aggregate(input);
      aggregateRows = fullPopulation && input.getLimit() == null
         ? result.stream().mapToLong(r -> number(r, primaryKey(input.getTableName()), AggregateOperator.COUNT).longValue()).sum()
         : sampledCount(input.getTableName(), populationFilter);
      return (result);
   }

   /*******************************************************************************
    ** Population rows remain exact; conditional counters estimate their membership.
    *******************************************************************************/
   @Override
   public long count(String table, QQueryFilter filter) throws QException
   {
      if(samplePercent == null)
      {
         return (super.count(table, filter));
      }
      if(JsonUtils.toJson(DrillFilters.copy(filter)).equals(JsonUtils.toJson(populationFilter)))
      {
         lastCountShare = ProfileSupport.share(populationRows, populationRows);
         return (populationRows);
      }
      long value = sampledCount(table, filter);
      aggregateRows = sampledCount(table, populationFilter);
      lastCountShare = ProfileSupport.share(value, aggregateRows);
      return (aggregateCount(value));
   }

   /*******************************************************************************
    ** Round count estimates only after normalizing by observed sample rows.
    *******************************************************************************/
   @Override
   protected long aggregateCount(long value)
   {
      return (samplePercent == null ? value : aggregateRows == 0 ? 0 : BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(populationRows))
         .divide(BigDecimal.valueOf(aggregateRows), 0, RoundingMode.HALF_UP).longValueExact());
   }

   /*******************************************************************************
    ** Keep ratios in sample units, independent of count rounding.
    *******************************************************************************/
   @Override
   protected BigDecimal aggregateShare(long value)
   {
      return (samplePercent == null ? null : ProfileSupport.share(value, aggregateRows));
   }

   /*******************************************************************************
    ** Conditional groups retain the ratio from their sampled count call.
    *******************************************************************************/
   @Override
   protected BigDecimal countShare()
   {
      return (samplePercent == null ? null : lastCountShare);
   }

   /*******************************************************************************
    ** Streamed fallbacks also observe native sampling, without binding sampled ids.
    *******************************************************************************/
   @Override
   protected List<QRecord> rows(String table, QQueryFilter filter, Set<String> fields, int limit) throws QException
   {
      if(samplePercent == null)
      {
         return (super.rows(table, filter, fields, limit));
      }
      if(limit == 0 || config.getMaxRows() == 0)
      {
         return (List.of());
      }
      AggregateInput input = new AggregateInput(table).withFilter(DrillFilters.copy(filter)).withGroupBy(new GroupBy(field(table, primaryKey(table))))
         .withLimit(Math.min(limit, config.getMaxRows()));
      fields.stream().filter(f -> !f.equals(primaryKey(table))).forEach(f -> input.withGroupBy(new GroupBy(field(table, f))));
      List<QRecord> result = new ArrayList<>();
      for(AggregateResult row : aggregate(input))
      {
         QRecord record = new QRecord();
         row.getGroupByValues().forEach((key, value) -> record.setValue(key.getFieldName(), value));
         result.add(record);
      }
      return (result);
   }

   /*******************************************************************************
    ** Every field batch shares a COUNT denominator from the same sampled call.
    *******************************************************************************/
   @Override
   public Map<String, FieldCounts> fieldCounts(String table, QQueryFilter filter, List<String> fields) throws QException
   {
      if(samplePercent == null)
      {
         return (super.fieldCounts(table, filter, fields));
      }
      Map<String, FieldCounts> result = new LinkedHashMap<>();
      for(int start = 0; start < fields.size(); start += 20)
      {
         List<String> batch = fields.subList(start, Math.min(start + 20, fields.size()));
         AggregateInput input = new AggregateInput(table).withFilter(DrillFilters.copy(filter));
         for(String field : batch)
         {
            input.withAggregate(countOf(field, AggregateOperator.COUNT)).withAggregate(countOf(field, AggregateOperator.COUNT_DISTINCT));
         }
         fieldCountAggregates(input, batch);
         List<String> stringFields = batch.stream().filter(f -> field(table, f).getType().isStringLike()).toList();
         List<QRecord> records = stringFields.isEmpty() ? List.of() : rows(table, filter, Set.copyOf(stringFields), config.getMaxRows());
         List<AggregateResult> results = aggregate(input);
         long seen = aggregateRows;
         for(String field : batch)
         {
            boolean string = field(table, field).getType().isStringLike();
            long nonNull = results.isEmpty() ? 0 : number(results.getFirst(), field, AggregateOperator.COUNT).longValue();
            long distinct = results.isEmpty() ? 0 : number(results.getFirst(), field, AggregateOperator.COUNT_DISTINCT).longValue();
            long blanks = string && capabilities(table).contains(AggregateCapability.NULL_BLANK_COUNTS) && !results.isEmpty()
               ? number(results.getFirst(), field, AggregateOperator.COUNT_BLANK).longValue() : seen - nonNull;
            QQueryFilter blankFilter = DrillFilters.nulls(field, string);
            if(string)
            {
               blankFilter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withSubFilter(blankFilter);
               for(String blank : records.stream().map(r -> r.getValueString(field)).filter(Objects::nonNull).filter(String::isBlank).distinct().toList())
               {
                  blankFilter.withSubFilter(DrillFilters.condition(field, QCriteriaOperator.EQUALS, blank));
               }
            }
            long blankCount = aggregateCount(blanks);
            BigDecimal completeness = ProfileSupport.share(seen - blanks, seen);
            if(string && !capabilities(table).contains(AggregateCapability.NULL_BLANK_COUNTS))
            {
               blankCount = count(table, DrillFilters.and(filter, blankFilter));
               completeness = BigDecimal.ONE.subtract(countShare());
               aggregateRows = seen;
            }
            result.put(field, new FieldCounts(aggregateCount(nonNull), aggregateCount(seen - nonNull), blankCount, aggregateCount(distinct),
               blankFilter, DrillFilters.nonNull(field, string), ProfileSupport.share(distinct, nonNull), completeness, distinct == 1));
         }
      }
      return (result);
   }

   /*******************************************************************************
    ** Add supported counters to the existing twenty-field aggregate batch.
    *******************************************************************************/
   @Override
   protected void fieldCountAggregates(AggregateInput input, List<String> fields) throws QException
   {
      if(capabilities(input.getTableName()).contains(AggregateCapability.NULL_BLANK_COUNTS))
      {
         for(String name : fields)
         {
            input.withAggregate(new Aggregate(name, AggregateOperator.COUNT_NULL));
            if(field(input.getTableName(), name).getType().isStringLike())
            {
               input.withAggregate(new Aggregate(name, AggregateOperator.COUNT_BLANK));
            }
         }
      }
   }

   /*******************************************************************************
    ** Read native counters, falling back to the portable predicate counter.
    *******************************************************************************/
   @Override
   protected long fieldCount(String table, QQueryFilter filter, String field, AggregateOperator operator,
      List<AggregateResult> results, QQueryFilter condition) throws QException
   {
      if(capabilities(table).contains(AggregateCapability.NULL_BLANK_COUNTS))
      {
         return (results.isEmpty() ? 0 : number(results.getFirst(), field, operator).longValue());
      }
      return (super.fieldCount(table, filter, field, operator, results, condition));
   }

   /*******************************************************************************
    ** Push each supported statistic, streaming only unavailable operators.
    *******************************************************************************/
   @Override
   protected Map<String, BigDecimal> numericMetrics(String table, QQueryFilter filter, String field, List<BigDecimal> percentiles, List<BigDecimal> values) throws QException
   {
      Set<AggregateCapability> supported = capabilities(table);
      if(!supported.containsAll(Set.of(AggregateCapability.STDDEV, AggregateCapability.PERCENTILE)))
      {
         return (partialNumericMetrics(table, filter, field, percentiles, values, supported));
      }
      return (nativeNumericMetrics(table, filter, field, percentiles, supported));
   }

   /*******************************************************************************
    ** Existing streamed arithmetic supplies only unavailable metrics.
    *******************************************************************************/
   private Map<String, BigDecimal> partialNumericMetrics(String table, QQueryFilter filter, String field, List<BigDecimal> percentiles,
      List<BigDecimal> values, Set<AggregateCapability> supported) throws QException
   {
      Map<String, BigDecimal> metrics = super.numericMetrics(table, filter, field, percentiles, values);
      metrics.putAll(nativeNumericMetrics(table, filter, field, percentiles, supported));
      return (metrics);
   }

   /*******************************************************************************
    ** One aggregate call computes basic moments and all supported extensions.
    *******************************************************************************/
   private Map<String, BigDecimal> nativeNumericMetrics(String table, QQueryFilter filter, String field, List<BigDecimal> percentiles,
      Set<AggregateCapability> supported) throws QException
   {
      Map<String, BigDecimal> metrics = new LinkedHashMap<>();
      AggregateInput input = new AggregateInput(table).withFilter(DrillFilters.copy(filter));
      Map<AggregateOperator, String> names = Map.of(AggregateOperator.MIN, "min", AggregateOperator.MAX, "max", AggregateOperator.AVG, "mean", AggregateOperator.SUM, "sum", AggregateOperator.STDDEV_SAMP, "stddev");
      for(AggregateOperator op : names.keySet())
      {
         if(op != AggregateOperator.STDDEV_SAMP || supported.contains(AggregateCapability.STDDEV))
         {
            input.withAggregate(new Aggregate(field, op));
         }
      }
      if(supported.contains(AggregateCapability.PERCENTILE))
      {
         percentiles.forEach(p -> input.withAggregate(new Aggregate(field, AggregateOperator.PERCENTILE_CONT).withPercentile(p)));
      }
      for(AggregateResult row : aggregate(input))
      {
         row.getAggregateValues().entrySet().stream().filter(e -> e.getKey().getOperator() != AggregateOperator.COUNT).forEach(entry ->
         {
            Aggregate key = entry.getKey();
            Serializable value = entry.getValue();
            metrics.put(key.getOperator() == AggregateOperator.PERCENTILE_CONT
               ? "p" + key.getPercentile().multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() : names.get(key.getOperator()),
               value == null ? null : new BigDecimal(value.toString()));
         });
      }
      return (metrics);
   }

   /*******************************************************************************
    ** Convert native bin numbers into identical half-open drill predicates.
    *******************************************************************************/
   @Override
   public List<GroupCount> numericHistogram(String table, QQueryFilter filter, String field, BigDecimal low, BigDecimal high, int bins) throws QException
   {
      if(low == null || high == null || low.compareTo(high) >= 0 || !capabilities(table).contains(AggregateCapability.NUMERIC_BUCKET))
      {
         return (super.numericHistogram(table, filter, field, low, high, bins));
      }
      NiceBins nice = NiceBins.covering(low, high);
      low = nice.low();
      high = nice.high();
      bins = nice.count();
      GroupBy group = new GroupBy(field(table, field)).withBucket(new NumericBucket(low, high, bins));
      Map<Serializable, Long> counts = bucketCounts(table, filter, group);
      List<BigDecimal> values = !estimating && isMemory(table) ? rows(table, filter, Set.of(field), config.getMaxRows()).stream().map(r -> r.getValue(field))
         .filter(Objects::nonNull).map(v -> new BigDecimal(v.toString())).toList() : List.of();
      List<GroupCount> result = new ArrayList<>();
      result.add(new GroupCount(AnalysisFormatting.interval(field(table, field), null, low), AnalysisFormatting.interval(field(table, field), null, low), aggregateCount(counts.getOrDefault(0, 0L)), numericRange(table, field, values, null, low, false), aggregateShare(counts.getOrDefault(0, 0L))));
      BigDecimal width = high.subtract(low).divide(BigDecimal.valueOf(bins), MathContext.DECIMAL128);
      for(int i = 0; i < bins; i++)
      {
         BigDecimal start = low.add(width.multiply(BigDecimal.valueOf(i)));
         BigDecimal end = i == bins - 1 ? high : low.add(width.multiply(BigDecimal.valueOf(i + 1)));
         String label = AnalysisFormatting.interval(field(table, field), start, end);
         result.add(new GroupCount(label, label, aggregateCount(counts.getOrDefault(i + 1, 0L)), numericRange(table, field, values, start, end, i == bins - 1), aggregateShare(counts.getOrDefault(i + 1, 0L))));
      }
      result.add(new GroupCount(AnalysisFormatting.interval(field(table, field), high, null), AnalysisFormatting.interval(field(table, field), high, null), aggregateCount(counts.getOrDefault(bins + 1, 0L)),
         DrillFilters.and(DrillFilters.condition(field, QCriteriaOperator.IS_NOT_BLANK), DrillFilters.condition(field, QCriteriaOperator.GREATER_THAN, high)), aggregateShare(counts.getOrDefault(bins + 1, 0L))));
      return (result);
   }

   /*******************************************************************************
    ** Native dates use the session zone; fill calendar gaps with zero counts.
    *******************************************************************************/
   @Override
   public List<GroupCount> dateBuckets(String table, QQueryFilter filter, String field, DateUnit unit, ZoneId zone) throws QException
   {
      if(unit == DateUnit.HOUR || unit == DateUnit.QUARTER)
      {
         return (super.dateBuckets(table, filter, field, unit, zone));
      }
      GroupBy group = new GroupBy(field(table, field)).withBucket(new DateBucket(DateBucketUnit.valueOf(unit.name())));
      AggregateInput probe = new AggregateInput(table).withGroupBy(group);
      if(!new QBackendModuleDispatcher().getQBackendModule(probe.getBackend()).getAggregateInterface().getSupportedCapabilities(probe).contains(AggregateCapability.DATE_BUCKET))
      {
         return (super.dateBuckets(table, filter, field, unit, zone));
      }
      Map<Serializable, Long> counts = bucketCounts(table, filter, group);
      if(unit == DateUnit.DAY_OF_WEEK || unit == DateUnit.HOUR_OF_DAY)
      {
         long seen = aggregateRows;
         List<GroupCount> conditions = super.dateBuckets(table, filter, field, unit, zone);
         aggregateRows = seen;
         return (conditions.stream()
            .map(g -> new GroupCount(g.value(), g.label(), aggregateCount(counts.getOrDefault(g.value(), 0L)), g.condition(), aggregateShare(counts.getOrDefault(g.value(), 0L)))).toList());
      }
      List<LocalDate> dates = counts.keySet().stream().filter(Objects::nonNull).map(v -> (LocalDate) v).sorted().toList();
      List<GroupCount> result = new ArrayList<>();
      if(!dates.isEmpty())
      {
         LocalDate start = dates.getFirst();
         while(!start.isAfter(dates.getLast()))
         {
            LocalDate end = next(start, unit);
            String label = AnalysisFormatting.dateBucket(start, unit, zone);
            result.add(new GroupCount(start, label, aggregateCount(counts.getOrDefault(start, 0L)),
               DrillFilters.dateRange(field, start, end, field(table, field).getType() == QFieldType.DATE_TIME, zone), aggregateShare(counts.getOrDefault(start, 0L))));
            start = end;
         }
      }
      return (result);
   }

   /*******************************************************************************
    ** Length moments are native; the derived length histogram remains streamed.
    *******************************************************************************/
   @Override
   public LengthSummary lengthSummary(String table, QQueryFilter filter, String field) throws QException
   {
      LengthSummary streamed = super.lengthSummary(table, filter, field);
      if(capabilities(table).contains(AggregateCapability.STRING_LENGTH))
      {
         AggregateInput input = new AggregateInput(table).withFilter(DrillFilters.copy(filter));
         Map<AggregateOperator, String> names = Map.of(AggregateOperator.MIN_LENGTH, "minLength", AggregateOperator.MAX_LENGTH, "maxLength", AggregateOperator.AVG_LENGTH, "meanLength");
         names.keySet().forEach(op -> input.withAggregate(new Aggregate(field, op)));
         for(AggregateResult row : aggregate(input))
         {
            row.getAggregateValues().forEach((key, value) ->
            {
               if(value != null && names.containsKey(key.getOperator()))
               {
                  streamed.metrics().put(names.get(key.getOperator()), new BigDecimal(value.toString()));
               }
            });
         }
      }
      return (streamed);
   }

   /*******************************************************************************
    ** Bucket result keys retain core's typed date or integer values.
    *******************************************************************************/
   private Map<Serializable, Long> bucketCounts(String table, QQueryFilter filter, GroupBy group) throws QException
   {
      Map<Serializable, Long> result = new LinkedHashMap<>();
      AggregateInput input = new AggregateInput(table).withFilter(DrillFilters.copy(filter)).withGroupBy(group)
         .withAggregate(countOf(primaryKey(table), AggregateOperator.COUNT));
      for(AggregateResult row : aggregate(input))
      {
         result.put(row.getGroupByValues().get(group), number(row, primaryKey(table), AggregateOperator.COUNT).longValue());
      }
      return (result);
   }
}
