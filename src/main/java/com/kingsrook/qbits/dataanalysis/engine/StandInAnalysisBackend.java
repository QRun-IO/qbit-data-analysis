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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;

/*******************************************************************************
 ** Stand-in until QQQ core DATA-ANALYSIS.md §3 operators ship in an RC; then add
 ** CoreAnalysisBackend that pushes these down. RC.5 estimates use the first
 ** sample_rows rows by primary key, not random sampling. Streamed operations
 ** page by primary key and retain at most max_rows records.
 *******************************************************************************/
public class StandInAnalysisBackend implements AnalysisBackend
{
   private static final MathContext MATH = MathContext.DECIMAL128;
   protected final EffectiveConfig config;
   protected boolean estimating;

   /*******************************************************************************
    ** Bind one request's detached configuration.
    *******************************************************************************/
   public StandInAnalysisBackend(EffectiveConfig config)
   {
      this.config = config;
   }



   /*******************************************************************************
    ** CountAction is core's dedicated query-count operation.
    *******************************************************************************/
   @Override
   public long count(String table, QQueryFilter filter) throws QException
   {
      CountInput input = new CountInput(table).withFilter(DrillFilters.restoreTypes(table, filter));
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new CountAction().execute(input).getCount().longValue());
   }



   /*******************************************************************************
    ** A single boundary parameter bounds the prefix used for computation.
    *******************************************************************************/
   @Override
   public Scope scope(String table, QQueryFilter filter, boolean exact) throws QException
   {
      long total = count(table, filter);
      boolean estimate = !exact && total > config.getSampleThresholdRows();
      estimating = estimate;
      int limit = estimate ? Math.min(config.getSampleRows(), config.getMaxRows()) : config.getMaxRows();
      QQueryFilter scoped = DrillFilters.copy(filter);
      if(total > limit)
      {
         String pk = primaryKey(table);
         if(limit == 0)
         {
            scoped = DrillFilters.and(scoped, DrillFilters.condition(pk, QCriteriaOperator.IS_BLANK));
         }
         else
         {
            List<QRecord> boundary = query(table, scoped, Set.of(pk), limit - 1, 1);
            if(boundary.isEmpty())
            {
               throw new QException("Data changed while selecting analysis rows; retry.");
            }
            scoped = DrillFilters.and(scoped, DrillFilters.condition(pk, QCriteriaOperator.LESS_THAN_OR_EQUALS, boundary.getFirst().getValue(pk)));
         }
      }
      return (new Scope(scoped, total, Math.min(total, limit), total > limit && (!estimate || limit < config.getSampleRows()), estimate));
   }



   /*******************************************************************************
    ** Fetch only fields used by the operation, including requested heavy text.
    *******************************************************************************/
   private List<QRecord> query(String table, QQueryFilter filter, Set<String> fields, int offset, int limit) throws QException
   {
      QueryInput input = new QueryInput(table).withFilter(DrillFilters.restoreTypes(table, filter).withOrderBy(new QFilterOrderBy(primaryKey(table))).withSkip(offset).withLimit(limit))
         .withFieldNamesToInclude(fields).withShouldFetchHeavyFields(true);
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new QueryAction().execute(input).getRecords());
   }



   /*******************************************************************************
    ** Bound both page size and retained records.
    *******************************************************************************/
   protected List<QRecord> rows(String table, QQueryFilter filter, Set<String> fields, int limit) throws QException
   {
      List<QRecord> records = new ArrayList<>();
      int bound = Math.min(limit, config.getMaxRows());
      while(records.size() < bound)
      {
         int size = Math.min(1000, bound - records.size());
         List<QRecord> page = query(table, filter, fields, records.size(), size);
         records.addAll(page);
         if(page.size() < size)
         {
            break;
         }
      }
      return (records);
   }



   /*******************************************************************************
    ** Core applies table permissions, field locks and record locks.
    *******************************************************************************/
   protected List<AggregateResult> aggregate(AggregateInput input) throws QException
   {
      QQueryFilter restored = DrillFilters.restoreTypes(input.getTableName(), input.getFilter());
      restored.setOrderBys(input.getFilter().getOrderBys());
      input.setFilter(restored);
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new AggregateAction().execute(input).getResults());
   }



   /*******************************************************************************
    ** Batch supported aggregates; nulls use core criteria. The bounded scan adds
    ** whitespace blanks and representative records for cardinality drills.
    *******************************************************************************/
   @Override
   public Map<String, FieldCounts> fieldCounts(String table, QQueryFilter filter, List<String> fields) throws QException
   {
      Map<String, FieldCounts> result = new LinkedHashMap<>();
      long total = count(table, filter);
      for(int start = 0; start < fields.size(); start += 20)
      {
         List<String> batch = fields.subList(start, Math.min(start + 20, fields.size()));
         AggregateInput input = new AggregateInput(table).withFilter(DrillFilters.copy(filter));
         for(String field : batch)
         {
            input.withAggregate(countOf(field, AggregateOperator.COUNT)).withAggregate(countOf(field, AggregateOperator.COUNT_DISTINCT));
         }
         fieldCountAggregates(input, batch);
         List<AggregateResult> aggregates = aggregate(input);
         Set<String> selection = new LinkedHashSet<>(batch);
         selection.add(primaryKey(table));
         List<QRecord> records = rows(table, filter, selection, config.getMaxRows());
         for(String field : batch)
         {
            boolean string = field(table, field).getType().isStringLike();
            long nulls = fieldCount(table, filter, field, AggregateOperator.COUNT_NULL, aggregates, DrillFilters.nulls(field, string));
            Map<Serializable, Serializable> representatives = new LinkedHashMap<>();
            Set<Serializable> blanks = new LinkedHashSet<>();
            for(QRecord record : records)
            {
               Serializable value = record.getValue(field);
               if(value != null)
               {
                  representatives.putIfAbsent(value instanceof BigDecimal d ? d.stripTrailingZeros() : value, record.getValue(primaryKey(table)));
                  if(string && value.toString().isBlank())
                  {
                     blanks.add(value);
                  }
               }
            }
            QQueryFilter blankFilter = DrillFilters.nulls(field, string);
            if(!blanks.isEmpty())
            {
               blankFilter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withSubFilter(blankFilter)
                  .withSubFilter(DrillFilters.in(field, new ArrayList<>(blanks)));
            }
            long distinct = aggregates.isEmpty() ? 0 : number(aggregates.getFirst(), field, AggregateOperator.COUNT_DISTINCT).longValue();
            if(!estimating && isMemory(table) && field(table, field).getType() == QFieldType.DECIMAL)
            {
               distinct = representatives.size();
            }
            result.put(field, new FieldCounts(total - nulls, nulls, string ? fieldCount(table, filter, field, AggregateOperator.COUNT_BLANK, aggregates, blankFilter) : nulls,
               distinct, blankFilter, estimating ? DrillFilters.nonNull(field, string) : DrillFilters.in(primaryKey(table), new ArrayList<>(representatives.values()))));
         }
      }
      return (result);
   }



   /*******************************************************************************
    ** Subclasses may extend the existing batch with native null/blank counts.
    *******************************************************************************/
   protected void fieldCountAggregates(AggregateInput input, List<String> fields) throws QException
   {
      // The portable backend counts null and blank predicates separately.
   }

   /*******************************************************************************
    ** Count the exact drill predicate when native counters are unavailable.
    *******************************************************************************/
   protected long fieldCount(String table, QQueryFilter filter, String field, AggregateOperator operator,
      List<AggregateResult> results, QQueryFilter condition) throws QException
   {
      return (count(table, DrillFilters.and(filter, condition)));
   }

   /*******************************************************************************
    ** Push down group, count, ordering and pagination.
    *******************************************************************************/
   @Override
   public List<GroupCount> topValues(String table, QQueryFilter filter, String field, int limit, int offset, String search) throws QException
   {
      QQueryFilter selected = DrillFilters.copy(filter);
      if(search != null && !search.isBlank())
      {
         if(!field(table, field).getType().isStringLike())
         {
            return (groups(table, selected, List.of(field), Math.max(1, config.getMaxRows()), 0, false).stream()
               .filter(g -> g.value() != null && g.value().toString().contains(search)).skip(offset).limit(limit).toList());
         }
         selected = DrillFilters.and(selected, DrillFilters.condition(field, QCriteriaOperator.CONTAINS, search));
      }
      return (groups(table, selected, List.of(field), limit, offset, false));
   }



   /*******************************************************************************
    ** Random backends scale a count using the current aggregate's sample size.
    *******************************************************************************/
   protected long aggregateCount(long value)
   {
      return (value);
   }

   /*******************************************************************************
    ** Random backends retain the raw sample ratio before rounding counts.
    *******************************************************************************/
   protected BigDecimal aggregateShare(long value)
   {
      return (null);
   }

   /*******************************************************************************
    ** Exact and prefix groups derive shares from the profiler's row count.
    *******************************************************************************/
   protected BigDecimal countShare()
   {
      return (null);
   }

   /*******************************************************************************
    ** Tuple drills use IN for non-null values and null-only predicates otherwise.
    *******************************************************************************/
   private List<GroupCount> groups(String table, QQueryFilter filter, List<String> fields, int limit, int offset, boolean duplicates) throws QException
   {
      Aggregate count = countOf(primaryKey(table), AggregateOperator.COUNT);
      QQueryFilter ordered = DrillFilters.copy(filter).withOrderBy(new QFilterOrderByAggregate(count, false));
      AggregateInput input = new AggregateInput(table).withFilter(ordered).withAggregate(count).withLimit(Math.addExact(limit, offset));
      for(String field : fields)
      {
         input.withGroupBy(new GroupBy(field(table, field)));
         ordered.withOrderBy(new QFilterOrderByGroupBy(new GroupBy(field(table, field))));
      }
      Map<Serializable, GroupCount> merged = new LinkedHashMap<>();
      for(AggregateResult row : aggregate(input))
      {
         long rawSize = number(row, primaryKey(table), AggregateOperator.COUNT).longValue();
         long size = aggregateCount(rawSize);
         LinkedHashMap<String, Serializable> tuple = new LinkedHashMap<>();
         QQueryFilter condition = new QQueryFilter();
         for(String field : fields)
         {
            Serializable value = row.getGroupByValues().entrySet().stream().filter(e -> field.equals(e.getKey().getFieldName())).findFirst().orElseThrow().getValue();
            tuple.put(field, value instanceof BigDecimal d ? new BigDecimal(d.stripTrailingZeros().toPlainString()) : value);
            condition.withSubFilter(value == null ? DrillFilters.nulls(field, field(table, field).getType().isStringLike())
               : value instanceof BigDecimal && !estimating ? DrillFilters.in(field, List.of(value.toString()))
               : duplicates ? DrillFilters.in(field, List.of(value)) : DrillFilters.value(field, value, field(table, field).getType().isStringLike()));
         }
         Serializable value = duplicates ? tuple : tuple.get(fields.getFirst());
         GroupCount previous = merged.get(value);
         if(previous != null)
         {
            size += previous.count();
            condition = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withSubFilter(previous.condition()).withSubFilter(condition);
         }
         merged.put(value, new GroupCount(value, value == null ? "(null)" : value.toString(), size, condition, aggregateShare(rawSize)));
      }
      List<GroupCount> groups = new ArrayList<>(merged.values());
      if(isMemory(table))
      {
         groups.sort(Comparator.comparingLong(GroupCount::count).reversed().thenComparing(GroupCount::label));
      }
      return (groups.stream().filter(g -> !duplicates || g.count() > 1).skip(offset).limit(limit).toList());
   }



   /*******************************************************************************
    ** Push down basic moments; stream ordered values for variance and percentiles.
    *******************************************************************************/
   @Override
   public NumericSummary numericSummary(String table, QQueryFilter filter, String field, List<BigDecimal> percentiles) throws QException
   {
      List<BigDecimal> values = rows(table, filter, Set.of(field), config.getMaxRows()).stream().map(r -> r.getValue(field))
         .filter(Objects::nonNull).map(v -> new BigDecimal(v.toString())).sorted().toList();
      Map<String, BigDecimal> metrics = numericMetrics(table, filter, field, percentiles, values);
      if(metrics.isEmpty() || metrics.get("min") == null)
      {
         for(String key : List.of("min", "max", "mean", "stddev", "sum"))
         {
            metrics.put(key, null);
         }
         for(BigDecimal p : percentiles)
         {
            metrics.put("p" + p.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString(), null);
         }
         QQueryFilter empty = DrillFilters.condition(primaryKey(table), QCriteriaOperator.IS_BLANK);
         return (new NumericSummary(metrics, List.of(), 0, empty, List.of(new GroupCount("zeros", "zeros", 0, estimating ? DrillFilters.condition(field, QCriteriaOperator.EQUALS, BigDecimal.ZERO) : empty),
            new GroupCount("negatives", "negatives", 0, estimating ? DrillFilters.range(field, null, BigDecimal.ZERO, false) : empty))));
      }
      BigDecimal q1 = metrics.containsKey("p25") ? metrics.get("p25") : percentile(values, new BigDecimal("0.25"));
      BigDecimal q3 = metrics.containsKey("p75") ? metrics.get("p75") : percentile(values, new BigDecimal("0.75"));
      BigDecimal span = q3.subtract(q1).multiply(new BigDecimal("1.5"));
      BigDecimal low = q1.subtract(span);
      BigDecimal high = q3.add(span);
      QQueryFilter outliers = isMemory(table)
         ? new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
            .withSubFilter(numericRange(table, field, values, null, low, false))
            .withSubFilter(DrillFilters.and(DrillFilters.condition(field, QCriteriaOperator.IS_NOT_BLANK), DrillFilters.condition(field, QCriteriaOperator.GREATER_THAN, high)))
         : DrillFilters.outliers(field, low, high);
      List<BigDecimal> extremes = values.stream().filter(v -> v.compareTo(low) < 0 || v.compareTo(high) > 0).map(v -> new BigDecimal(v.stripTrailingZeros().toPlainString())).distinct()
         .sorted(Comparator.comparing((BigDecimal v) -> v.subtract(q1.add(q3).divide(BigDecimal.valueOf(2), MATH)).abs()).reversed()).limit(10).toList();
      List<GroupCount> extremeGroups = new ArrayList<>();
      for(BigDecimal value : extremes)
      {
         QQueryFilter condition = estimating ? DrillFilters.condition(field, QCriteriaOperator.EQUALS, value) : DrillFilters.in(field, equalNumbers(values, value));
         extremeGroups.add(new GroupCount(value, value.toPlainString(), count(table, DrillFilters.and(filter, condition)), condition, countShare()));
      }
      QQueryFilter zeros = estimating ? DrillFilters.condition(field, QCriteriaOperator.EQUALS, BigDecimal.ZERO) : DrillFilters.in(field, equalNumbers(values, BigDecimal.ZERO));
      QQueryFilter negatives = numericRange(table, field, values, null, BigDecimal.ZERO, false);
      return (new NumericSummary(metrics, extremeGroups, count(table, DrillFilters.and(filter, outliers)), outliers,
         List.of(bucket(table, filter, "zeros", zeros), bucket(table, filter, "negatives", negatives))));
   }



   /*******************************************************************************
    ** Portable numeric moments; core-backed subclasses replace supported work.
    *******************************************************************************/
   protected Map<String, BigDecimal> numericMetrics(String table, QQueryFilter filter, String field, List<BigDecimal> percentiles, List<BigDecimal> values) throws QException
   {
      Map<String, BigDecimal> metrics = new LinkedHashMap<>();
      if(values.isEmpty())
      {
         return (metrics);
      }
      AggregateInput input = new AggregateInput(table).withFilter(DrillFilters.copy(filter));
      for(AggregateOperator op : List.of(AggregateOperator.MIN, AggregateOperator.MAX, AggregateOperator.AVG, AggregateOperator.SUM))
      {
         input.withAggregate(new Aggregate(field, op));
      }
      List<AggregateResult> results = aggregate(input);
      AggregateResult result = results.getFirst();
      metrics.put("min", number(result, field, AggregateOperator.MIN));
      metrics.put("max", number(result, field, AggregateOperator.MAX));
      metrics.put("mean", number(result, field, AggregateOperator.AVG));
      metrics.put("sum", number(result, field, AggregateOperator.SUM));
      BigDecimal mean = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), MATH);
      BigDecimal variance = values.stream().map(v -> v.subtract(mean).pow(2)).reduce(BigDecimal.ZERO, BigDecimal::add);
      metrics.put("stddev", values.size() < 2 ? null : variance.divide(BigDecimal.valueOf(values.size() - 1), MATH).sqrt(MATH));
      for(BigDecimal p : percentiles)
      {
         metrics.put("p" + p.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString(), percentile(values, p));
      }
      return (metrics);
   }



   /*******************************************************************************
    ** PostgreSQL percentile_cont interpolation at p * (n - 1).
    *******************************************************************************/
   public static BigDecimal percentile(List<BigDecimal> sorted, BigDecimal p)
   {
      if(sorted.isEmpty())
      {
         return (null);
      }
      BigDecimal index = p.multiply(BigDecimal.valueOf(sorted.size() - 1));
      int low = index.intValue();
      int high = Math.min(low + 1, sorted.size() - 1);
      return (sorted.get(low).add(sorted.get(high).subtract(sorted.get(low)).multiply(index.subtract(BigDecimal.valueOf(low)))));
   }



   /*******************************************************************************
    ** Predicates share the exact boundary arithmetic used by drills.
    *******************************************************************************/
   @Override
   public List<GroupCount> numericHistogram(String table, QQueryFilter filter, String field, BigDecimal low, BigDecimal high, int bins) throws QException
   {
      List<GroupCount> result = new ArrayList<>();
      if(low == null || high == null)
      {
         return (result);
      }
      List<BigDecimal> values = rows(table, filter, Set.of(field), config.getMaxRows()).stream().map(r -> r.getValue(field))
         .filter(Objects::nonNull).map(v -> new BigDecimal(v.toString())).toList();
      result.add(bucket(table, filter, "Below " + low, numericRange(table, field, values, null, low, false)));
      BigDecimal width = high.subtract(low).divide(BigDecimal.valueOf(bins), MATH);
      for(int i = 0; i < bins; i++)
      {
         BigDecimal start = low.add(width.multiply(BigDecimal.valueOf(i)));
         BigDecimal end = i == bins - 1 ? high : low.add(width.multiply(BigDecimal.valueOf(i + 1)));
         result.add(bucket(table, filter, "[" + start + ", " + end + (i == bins - 1 ? "]" : ")"), numericRange(table, field, values, start, end, i == bins - 1)));
      }
      result.add(bucket(table, filter, "Above " + high, DrillFilters.and(DrillFilters.condition(field, QCriteriaOperator.IS_NOT_BLANK), DrillFilters.condition(field, QCriteriaOperator.GREATER_THAN, high))));
      return (result);
   }



   /*******************************************************************************
    ** A bucket count and its producing condition.
    *******************************************************************************/
   private GroupCount bucket(String table, QQueryFilter filter, String label, QQueryFilter condition) throws QException
   {
      return (new GroupCount(label, label, count(table, DrillFilters.and(filter, condition)), condition, countShare()));
   }



   /*******************************************************************************
    ** Fill gaps inside the observed range, retaining DST-aware boundaries.
    *******************************************************************************/
   @Override
   public List<GroupCount> dateBuckets(String table, QQueryFilter filter, String field, DateUnit unit, ZoneId zone) throws QException
   {
      List<QRecord> records = rows(table, filter, Set.of(field), config.getMaxRows());
      if(unit == DateUnit.DAY_OF_WEEK || unit == DateUnit.HOUR_OF_DAY)
      {
         return (mappedGroups(table, filter, field, records, v -> unit == DateUnit.DAY_OF_WEEK ? day(v, zone).getDayOfWeek().getValue()
            : ((Instant) v).atZone(zone).getHour(), 24));
      }
      List<LocalDate> dates = records.stream().map(r -> r.getValue(field)).filter(Objects::nonNull).map(v -> day(v, zone)).sorted().toList();
      List<GroupCount> result = new ArrayList<>();
      if(dates.isEmpty())
      {
         return (result);
      }
      LocalDate start = floor(dates.getFirst(), unit);
      LocalDate last = floor(dates.getLast(), unit);
      while(!start.isAfter(last))
      {
         LocalDate end = next(start, unit);
         result.add(bucket(table, filter, start + " / " + end, DrillFilters.dateRange(field, start, end, field(table, field).getType() == QFieldType.DATE_TIME, zone)));
         start = end;
      }
      return (result);
   }



   /*******************************************************************************
    ** Dates carry no time-zone conversion; instants do.
    *******************************************************************************/
   public static LocalDate day(Serializable value, ZoneId zone)
   {
      return (ProfileSupport.day(value, zone));
   }



   /*******************************************************************************
    ** ISO weeks begin Monday.
    *******************************************************************************/
   protected static LocalDate floor(LocalDate date, DateUnit unit)
   {
      return (switch(unit)
      {
         case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
         case MONTH -> date.withDayOfMonth(1);
         case YEAR -> date.withDayOfYear(1);
         default -> date;
      });
   }



   /*******************************************************************************
    ** Calendar arithmetic handles leap years and daylight savings.
    *******************************************************************************/
   protected static LocalDate next(LocalDate date, DateUnit unit)
   {
      return (switch(unit)
      {
         case WEEK -> date.plusWeeks(1);
         case MONTH -> date.plusMonths(1);
         case YEAR -> date.plusYears(1);
         default -> date.plusDays(1);
      });
   }



   /*******************************************************************************
    ** Preserve types in minimum and maximum aggregate values.
    *******************************************************************************/
   @Override
   public Map<String, Serializable> extrema(String table, QQueryFilter filter, String field) throws QException
   {
      AggregateInput input = new AggregateInput(table).withFilter(DrillFilters.copy(filter))
         .withAggregate(new Aggregate(field, AggregateOperator.MIN)).withAggregate(new Aggregate(field, AggregateOperator.MAX));
      Map<String, Serializable> result = new LinkedHashMap<>();
      result.put("min", null);
      result.put("max", null);
      for(AggregateResult row : aggregate(input))
      {
         row.getAggregateValues().forEach((key, value) ->
         {
            if(key.getOperator() == AggregateOperator.MIN || key.getOperator() == AggregateOperator.MAX)
            {
               result.put(key.getOperator().name().toLowerCase(java.util.Locale.ROOT), value);
            }
         });
      }
      return (result);
   }



   /*******************************************************************************
    ** Stream string lengths; bucket members retain exact value-based drills.
    *******************************************************************************/
   @Override
   public LengthSummary lengthSummary(String table, QQueryFilter filter, String field) throws QException
   {
      List<QRecord> records = rows(table, filter, Set.of(field), config.getMaxRows());
      List<Integer> lengths = records.stream().map(r -> r.getValueString(field)).filter(Objects::nonNull).map(String::length).sorted().toList();
      Map<String, BigDecimal> metrics = new LinkedHashMap<>();
      if(!lengths.isEmpty())
      {
         metrics.put("minLength", BigDecimal.valueOf(lengths.getFirst()));
         metrics.put("maxLength", BigDecimal.valueOf(lengths.getLast()));
         metrics.put("meanLength", BigDecimal.valueOf(lengths.stream().mapToLong(Integer::longValue).sum()).divide(BigDecimal.valueOf(lengths.size()), MATH));
      }
      int width = lengths.isEmpty() ? 1 : Math.max(1, (int) Math.ceil((lengths.getLast() + 1.0) / 20));
      return (new LengthSummary(metrics, mappedGroups(table, filter, field, records, v -> (v.toString().length() / width * width) + "–" + (v.toString().length() / width * width + width - 1), 20)));
   }



   /*******************************************************************************
    ** Shapes use a primary-key prefix of at most the requested sample size.
    *******************************************************************************/
   @Override
   public List<GroupCount> patterns(String table, QQueryFilter filter, String field, int sampleRows) throws QException
   {
      List<QRecord> records = rows(table, filter, Set.of(field, primaryKey(table)), sampleRows);
      QQueryFilter sampled = records.isEmpty() ? DrillFilters.in(primaryKey(table), List.of())
         : DrillFilters.condition(primaryKey(table), QCriteriaOperator.LESS_THAN_OR_EQUALS, records.getLast().getValue(primaryKey(table)));
      return (mappedGroups(table, estimating ? filter : DrillFilters.and(filter, sampled), field, records, v -> shape(v.toString()), 10));
   }



   /*******************************************************************************
    ** Collapse runs of letters and digits, preserving literal punctuation.
    *******************************************************************************/
   public static String shape(String text)
   {
      return (text.replaceAll("\\p{L}+", "A").replaceAll("\\p{N}+", "9"));
   }



   /*******************************************************************************
    ** Derived groups do not require unsupported SQL functions.
    *******************************************************************************/
   private List<GroupCount> mappedGroups(String table, QQueryFilter filter, String field, List<QRecord> rows, Function<Serializable, Serializable> mapper, int limit) throws QException
   {
      Map<Serializable, Set<Serializable>> members = new LinkedHashMap<>();
      for(QRecord row : rows)
      {
         Serializable value = row.getValue(field);
         if(value != null)
         {
            members.computeIfAbsent(mapper.apply(value), ignored -> new LinkedHashSet<>()).add(value);
         }
      }
      List<GroupCount> groups = new ArrayList<>();
      for(Map.Entry<Serializable, Set<Serializable>> entry : members.entrySet())
      {
         QQueryFilter condition = DrillFilters.in(field, new ArrayList<>(entry.getValue()));
         groups.add(new GroupCount(entry.getKey(), entry.getKey().toString(), count(table, DrillFilters.and(filter, condition)), estimating ? condition : DrillFilters.and(filter, condition), countShare()));
      }
      return (groups.stream().sorted(Comparator.comparingLong(GroupCount::count).reversed().thenComparing(GroupCount::label)).limit(limit).toList());
   }



   /*******************************************************************************
    ** Top duplicate groups are sorted before the count threshold is applied.
    *******************************************************************************/
   @Override
   public List<GroupCount> duplicates(String table, QQueryFilter filter, List<String> fields, int limit) throws QException
   {
      return (groups(table, filter, fields, limit, 0, true));
   }



   /*******************************************************************************
    ** Metadata lookup follows facade security checks.
    *******************************************************************************/
   protected static QFieldMetaData field(String table, String field)
   {
      return (QContext.getQInstance().getTable(table).getField(field));
   }



   /*******************************************************************************
    ** Deterministic paging requires an addressable primary key.
    *******************************************************************************/
   protected static String primaryKey(String table)
   {
      return (QContext.getQInstance().getTable(table).getPrimaryKeyField());
   }



   /*******************************************************************************
    ** COUNT and COUNT_DISTINCT always read as integers: RC.5's RDBMS reader
    ** otherwise reads them as the field's type (a boolean count fails).
    *******************************************************************************/
   protected static Aggregate countOf(String field, AggregateOperator op)
   {
      return (new Aggregate(field, op).withFieldType(QFieldType.INTEGER));
   }



   /*******************************************************************************
    ** The value of one aggregate of a result row (0 when absent).
    *******************************************************************************/
   protected static BigDecimal number(AggregateResult row, String field, AggregateOperator op)
   {
      Serializable value = row.getAggregateValues().entrySet().stream().filter(e -> field.equals(e.getKey().getFieldName()) && op == e.getKey().getOperator())
         .map(Map.Entry::getValue).filter(Objects::nonNull).findFirst().orElse(0);
      return (new BigDecimal(value.toString()));
   }



   /*******************************************************************************
    ** RC.5 memory equality is scale-sensitive (BigDecimal.equals), unlike SQL.
    ** Use strict comparison plus actual boundary spellings to preserve half-open
    ** membership. SQL backends use ordinary range predicates.
    *******************************************************************************/
   protected QQueryFilter numericRange(String table, String field, List<BigDecimal> values, BigDecimal low, BigDecimal high, boolean closed)
   {
      if(estimating || !isMemory(table))
      {
         return (DrillFilters.range(field, low, high, closed));
      }
      QQueryFilter result = DrillFilters.condition(field, QCriteriaOperator.IS_NOT_BLANK);
      if(low != null)
      {
         result.withSubFilter(new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
            .withSubFilter(DrillFilters.condition(field, QCriteriaOperator.GREATER_THAN, low))
            .withSubFilter(DrillFilters.in(field, equalNumbers(values, low))));
      }
      if(high != null)
      {
         result.withCriteria(field, QCriteriaOperator.LESS_THAN_OR_EQUALS, high);
         List<String> boundary = equalNumbers(values, high);
         if(!closed && !boundary.isEmpty())
         {
            result.withCriteria(field, QCriteriaOperator.NOT_IN, boundary);
         }
      }
      return (result);
   }



   /*******************************************************************************
    ** Preserve scale through JSON using string operands, accepted by core and SQL.
    *******************************************************************************/
   private static List<String> equalNumbers(List<BigDecimal> values, BigDecimal target)
   {
      return (values.stream().filter(v -> v.compareTo(target) == 0).map(BigDecimal::toString).distinct().toList());
   }



   /*******************************************************************************
    ** Isolate compatibility fixes to the RC.5 memory implementation.
    *******************************************************************************/
   protected static boolean isMemory(String table)
   {
      return ("memory".equals(QContext.getQInstance().getBackend(QContext.getQInstance().getTable(table).getBackendName()).getBackendType()));
   }
}
