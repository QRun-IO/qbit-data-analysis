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

package com.kingsrook.qbits.dataanalysis.watch;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisSecurity;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.engine.EffectiveConfig;
import com.kingsrook.qbits.dataanalysis.engine.RelationshipSupport;
import com.kingsrook.qbits.dataanalysis.result.Axis;
import com.kingsrook.qbits.dataanalysis.result.Chart;
import com.kingsrook.qbits.dataanalysis.result.ChartKind;
import com.kingsrook.qbits.dataanalysis.result.Point;
import com.kingsrook.qbits.dataanalysis.result.Series;
import com.kingsrook.qbits.dataanalysis.result.ValueType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucketUnit;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;

/*******************************************************************************
 ** Exact core date-bucket aggregates under the active user's FULL-field policy.
 *******************************************************************************/
public class WatchSeries extends RelationshipSupport
{
   /*******************************************************************************
    ** Bind request-local policy and core streamed limits.
    *******************************************************************************/
   public WatchSeries(AnalysisSecurity security, EffectiveConfig config, String table)
   {
      super(security, config, table);
   }

   /*******************************************************************************
    ** Evaluate only complete buckets; reject partial or privacy-suppressed data.
    *******************************************************************************/
   public WatchPreview preview(WatchDefinition definition, QQueryFilter filter, Instant now) throws QException
   {
      var date = full(definition.dateField());
      WatchDefinition.require(date.getType() == QFieldType.DATE || date.getType() == QFieldType.DATE_TIME, "Choose a DATE or DATE_TIME field.");
      WatchDefinition.require(!"HOUR".equals(definition.bucket()) || date.getType() == QFieldType.DATE_TIME, "HOUR requires a DATE_TIME field.");
      if(definition.field() != null)
      {
         full(definition.field());
      }
      if(List.of("SUM", "AVG").contains(definition.metric()))
      {
         requireNumeric(definition.field());
      }
      validateFilter(filter);
      ZonedDateTime end = boundary(now, definition.bucket());
      ZonedDateTime start = move(end, definition.bucket(), -definition.k() - 1);
      QQueryFilter window = DrillFilters.drill(table, filter, range(definition.dateField(), date.getType(), start, end)).getFilter();
      GroupBy dateGroup = new GroupBy(date).withBucket(new DateBucket(DateBucketUnit.valueOf(definition.bucket())));
      Aggregate count = measure(primaryKey(table), AggregateOperator.COUNT);
      Aggregate metric = switch(definition.metric())
      {
         case "SUM" -> measure(definition.field(), AggregateOperator.SUM);
         case "AVG" -> measure(definition.field(), AggregateOperator.AVG);
         case "NULL_SHARE" -> measure(definition.field(), AggregateOperator.COUNT_NULL);
         default -> count;
      };
      Aggregate nonNull = List.of("SUM", "AVG").contains(definition.metric()) ? measure(definition.field(), AggregateOperator.COUNT) : count;
      AggregateInput input = new AggregateInput(table).withFilter(window).withGroupBy(dateGroup).withAggregate(count);
      if(metric != count)
      {
         input.withAggregate(metric);
      }
      if(nonNull != count)
      {
         input.withAggregate(nonNull);
      }
      GroupBy category = "CATEGORY_SET".equals(definition.metric()) ? new GroupBy(full(definition.field())) : null;
      if(category != null)
      {
         input.withGroupBy(category);
      }
      Map<String, List<AggregateResult>> grouped = new LinkedHashMap<>();
      for(AggregateResult row : aggregate(input))
      {
         grouped.computeIfAbsent(key(row.getGroupByValues().get(dateGroup), definition.bucket()), ignored -> new ArrayList<>()).add(row);
      }
      List<WatchBucket> buckets = new ArrayList<>();
      boolean seen = false;
      for(ZonedDateTime at = start; at.isBefore(end); at = move(at, definition.bucket(), 1))
      {
         Serializable value = "HOUR".equals(definition.bucket()) ? at.toInstant() : at.toLocalDate();
         List<AggregateResult> rows = grouped.getOrDefault(key(value, definition.bucket()), List.of());
         seen |= !rows.isEmpty();
         if(!seen)
         {
            continue;
         }
         long size = rows.stream().mapToLong(row -> ((Number) row.getAggregateValues().get(count)).longValue()).sum();
         if(size < config.getMinGroupSize())
         {
            return (skipped("Privacy guard: a bucket is below the minimum group size."));
         }
         Set<String> categories = new LinkedHashSet<>();
         if(category != null)
         {
            for(AggregateResult row : rows)
            {
               if(((Number) row.getAggregateValues().get(count)).longValue() < config.getMinGroupSize())
               {
                  return (skipped("Privacy guard: a category is below the minimum group size."));
               }
               Serializable member = row.getGroupByValues().get(category);
               if(member != null)
               {
                  categories.add(member.toString());
               }
            }
         }
         if(nonNull != count && !rows.isEmpty() && ((Number) rows.getFirst().getAggregateValues().get(nonNull)).longValue() < config.getMinGroupSize())
         {
            return (skipped("Privacy guard: too few non-null values."));
         }
         Object raw = rows.isEmpty() ? null : rows.getFirst().getAggregateValues().get(metric);
         if("NULL_SHARE".equals(definition.metric()) && raw != null)
         {
            long nulls = ((Number) raw).longValue();
            if(nulls > 0 && nulls < config.getMinGroupSize() || size - nulls > 0 && size - nulls < config.getMinGroupSize())
            {
               return (skipped("Privacy guard: too few null or non-null values."));
            }
         }
         Double number = switch(definition.metric())
         {
            case "COUNT" -> (double) size;
            case "CATEGORY_SET" -> (double) categories.size();
            case "NULL_SHARE" -> size == 0 ? null : ((Number) raw).doubleValue() / size;
            case "SUM" -> rows.isEmpty() ? 0.0 : raw == null ? null : ((Number) raw).doubleValue();
            default -> raw == null ? null : ((Number) raw).doubleValue();
         };
         buckets.add(new WatchBucket(value, number, categories, DrillFilters.drill(table, filter,
            range(definition.dateField(), date.getType(), at, move(at, definition.bucket(), 1)))));
      }
      String label = switch(definition.metric())
      {
         case "COUNT" -> security.table(table).getLabel();
         case "NULL_SHARE" -> "Null share of " + full(definition.field()).getLabel();
         case "CATEGORY_SET" -> full(definition.field()).getLabel();
         default -> ("SUM".equals(definition.metric()) ? "Sum of " : "Average ") + full(definition.field()).getLabel();
      };
      label += " per " + definition.bucket().toLowerCase(java.util.Locale.ROOT);
      WatchVerdict verdict = WatchDetectors.evaluate(definition, buckets, label);
      List<Point> points = buckets.stream().map(bucket -> new Point().withX(bucket.start())
         .withY(bucket.value() == null ? null : BigDecimal.valueOf(bucket.value())).withDrill(bucket.drill())).toList();
      List<String> annotations = new ArrayList<>();
      if(verdict.baselineLow() != null)
      {
         annotations.add("Baseline band: " + verdict.baselineLow() + " to " + verdict.baselineHigh() + ".");
      }
      annotations.add(verdict.message());
      Chart chart = new Chart().withKind(ChartKind.TIME_SERIES)
         .withX(new Axis().withField(definition.dateField()).withLabel(date.getLabel()).withValueType(date.getType() == QFieldType.DATE ? ValueType.DATE : ValueType.DATE_TIME))
         .withY(new Axis().withLabel(label).withValueType("NULL_SHARE".equals(definition.metric()) ? ValueType.PERCENT : ValueType.DECIMAL))
         .withSeries(List.of(new Series().withName(label).withPoints(points))).withAnnotations(annotations);
      return (new WatchPreview(buckets, chart, verdict));
   }

   /*******************************************************************************
    ** No suppressed values, categories or drill predicates leave this computation.
    *******************************************************************************/
   public static WatchPreview skipped(String message)
   {
      return (new WatchPreview(List.of(), new Chart().withKind(ChartKind.TIME_SERIES).withAnnotations(List.of(message)), WatchVerdict.skipped(message)));
   }

   /*******************************************************************************
    ** User-authored watch filters must also use FULL fields only.
    *******************************************************************************/
   private void validateFilter(QQueryFilter filter) throws QException
   {
      if(filter == null)
      {
         return;
      }
      for(var criterion : filter.getCriteria())
      {
         if(criterion.getFieldName() != null)
         {
            full(criterion.getFieldName());
         }
         if(criterion.getFieldFunction() != null)
         {
            full(criterion.getFieldFunction().getFieldName());
         }
      }
      for(QQueryFilter child : filter.getSubFilters())
      {
         validateFilter(child);
      }
   }

   /*******************************************************************************
    ** Calendar boundaries follow the owner's time zone, including DST changes.
    *******************************************************************************/
   private static ZonedDateTime boundary(Instant now, String bucket)
   {
      ZonedDateTime local = now.atZone(ValueUtils.getSessionOrInstanceZoneId());
      return (switch(bucket)
      {
         case "HOUR" -> local.truncatedTo(ChronoUnit.HOURS);
         case "WEEK" -> local.toLocalDate().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay(local.getZone());
         case "MONTH" -> local.toLocalDate().withDayOfMonth(1).atStartOfDay(local.getZone());
         default -> local.toLocalDate().atStartOfDay(local.getZone());
      });
   }

   /*******************************************************************************
    ** Advance on the calendar, rather than treating every day as 24 hours.
    *******************************************************************************/
   private static ZonedDateTime move(ZonedDateTime at, String bucket, int count)
   {
      return (switch(bucket)
      {
         case "HOUR" -> at.plusHours(count);
         case "WEEK" -> at.plusWeeks(count);
         case "MONTH" -> at.plusMonths(count);
         default -> at.plusDays(count);
      });
   }

   /*******************************************************************************
    ** Keep DATE filters as dates and DATE_TIME filters as instants.
    *******************************************************************************/
   private static QQueryFilter range(String field, QFieldType type, ZonedDateTime start, ZonedDateTime end)
   {
      return (DrillFilters.range(field, type == QFieldType.DATE ? start.toLocalDate() : start.toInstant(),
         type == QFieldType.DATE ? end.toLocalDate() : end.toInstant(), false));
   }

   /*******************************************************************************
    ** Normalize core date-bucket keys independently of JDBC value representation.
    *******************************************************************************/
   private static String key(Serializable value, String bucket)
   {
      return ("HOUR".equals(bucket) ? ValueUtils.getValueAsInstant(value).toString() : ValueUtils.getValueAsLocalDate(value).toString());
   }
}
