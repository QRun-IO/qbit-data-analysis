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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dataanalysis.result.ChartKind;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.Point;
import com.kingsrook.qbits.dataanalysis.result.Series;
import com.kingsrook.qbits.dataanalysis.result.ShiftBucket;
import com.kingsrook.qbits.dataanalysis.result.ShiftRequest;
import com.kingsrook.qbits.dataanalysis.result.ShiftResult;
import com.kingsrook.qbits.dataanalysis.result.ValueType;
import com.kingsrook.qbits.dataanalysis.stats.Statistics;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucketUnit;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;

/*******************************************************************************
 ** Distribution drift with stable categories or fixed baseline quartile bins.
 *******************************************************************************/
public class ShiftExplorer extends ComparisonExplorer
{
   /*******************************************************************************
    ** Bind the same core and privacy policy as compare.
    *******************************************************************************/
   public ShiftExplorer(AnalysisSecurity security, EffectiveConfig config, String table)
   {
      super(security, config, table);
   }

   /*******************************************************************************
    ** One calendar bucket with exact boundaries and whether its end has passed.
    *******************************************************************************/
   private record Bucket(Serializable start, QQueryFilter condition, boolean complete)
   {
   }

   /*******************************************************************************
    ** First and last complete nonempty buckets define the descriptive PSI.
    *******************************************************************************/
   public ShiftResult shift(ShiftRequest request) throws QException
   {
      var field = full(request.getField());
      var date = full(request.getDateField());
      DateBucketUnit unit;
      try
      {
         unit = DateBucketUnit.valueOf(request.getBucket());
      }
      catch(RuntimeException e)
      {
         throw new QBadRequestException("Choose a calendar bucket.");
      }
      if(!List.of(QFieldType.DATE, QFieldType.DATE_TIME).contains(date.getType()) || unit == DateBucketUnit.DAY_OF_WEEK
         || unit == DateBucketUnit.HOUR_OF_DAY || unit == DateBucketUnit.HOUR && date.getType() != QFieldType.DATE_TIME)
      {
         throw new QBadRequestException("Choose a date field and a calendar bucket from HOUR to YEAR.");
      }
      GroupBy group = new GroupBy(date).withBucket(new DateBucket(unit));
      var aggregate = measure(primaryKey(table), AggregateOperator.COUNT);
      var grouped = aggregate(new AggregateInput(table).withFilter(DrillFilters.copy(request.getFilter())).withGroupBy(group).withAggregate(aggregate));
      List<Bucket> buckets = grouped.stream().map(r -> r.getGroupByValues().get(group)).filter(java.util.Objects::nonNull)
         .sorted(Comparator.comparing(Object::toString)).map(start -> bucket(request.getDateField(), start, unit, date.getType())).toList();
      if(buckets.size() > 2000)
      {
         throw new QBadRequestException("Choose a larger time bucket; at most 2000 buckets are supported.");
      }
      ShiftResult result = new ShiftResult();
      List<Series> series = new ArrayList<>();
      List<double[]> complete = new ArrayList<>();
      boolean numeric = numeric(field);
      List<Category> categories = numeric ? List.of() : categories(request.getField(), request.getFilter(), 8);
      Map<String, BigDecimal> baseline = null;
      boolean privateData = false;
      if(numeric)
      {
         for(String name : List.of("p25", "median", "p75"))
         {
            series.add(new Series().withName(name));
         }
      }
      else
      {
         for(Category category : categories)
         {
            series.add(new Series().withName(category.label()));
         }
      }
      for(Bucket bucket : buckets)
      {
         QQueryFilter filter = DrillFilters.and(request.getFilter(), bucket.condition());
         long n = size(filter);
         ShiftBucket output = new ShiftBucket().withValue(bucket.start()).withLabel(bucket.start().toString()).withComplete(bucket.complete());
         double[] counts;
         if(numeric)
         {
            Map<String, BigDecimal> moments = moments(request.getField(), filter);
            long nonNull = moments.get("count").longValue();
            if(baseline == null && bucket.complete())
            {
               baseline = moments;
            }
            output.withMetrics(numericSide(request.getField(), output.getLabel(), filter, moments).getMetrics());
            counts = baseline == null || baseline.get("count").signum() == 0 ? new double[0] : numericCounts(request.getField(), filter, baseline);
            privateData |= nonNull < config.getMinGroupSize();
            for(int i = 0; i < series.size(); i++)
            {
               String key = series.get(i).getName();
               Metric metric = output.getMetrics().stream().filter(m -> m.getKey().equals(key)).findFirst().orElseThrow();
               series.get(i).getPoints().add(new Point().withX(bucket.start()).withY(decimal(metric.getValue())).withDrill(metric.getDrill()));
            }
         }
         else
         {
            counts = new double[categories.size()];
            for(int i = 0; i < categories.size(); i++)
            {
               Category category = categories.get(i);
               long count = size(DrillFilters.and(filter, category.condition()));
               counts[i] = count;
               Metric countMetric = metric("category" + i + "Count", count, ValueType.COUNT, filter, category.condition()).withLabel(category.label());
               Metric share = metric("category" + i + "Share", ProfileSupport.share(count, n), ValueType.PERCENT, filter, category.condition()).withLabel(category.label());
               List<Metric> metrics = List.of(countMetric, share);
               guard(metrics, count);
               if(category.privateGroup())
               {
                  metrics.forEach(m -> m.withDrill(null));
               }
               privateData |= category.privateGroup() || count < config.getMinGroupSize();
               output.getMetrics().addAll(metrics);
               series.get(i).getPoints().add(new Point().withX(bucket.start()).withY(decimal(share.getValue())).withLabel(category.label()).withDrill(share.getDrill()));
            }
         }
         result.getBuckets().add(output);
         if(bucket.complete())
         {
            complete.add(counts);
         }
      }
      Double psi = !privateData && complete.size() >= 2 && java.util.Arrays.stream(complete.getFirst()).sum() >= 2
         && java.util.Arrays.stream(complete.getLast()).sum() >= 2 ? Statistics.psi(complete.getFirst(), complete.getLast()) : null;
      result.withPsi(ProfileSupport.metric("psi", psi, ValueType.DECIMAL));
      result.withSentence(psi == null ? "Not applicable: " + (privateData ? "privacy guard" : "two complete buckets with at least two values are required")
         : "The first and last complete buckets have a population stability index of " + BigDecimal.valueOf(psi).setScale(3, java.math.RoundingMode.HALF_UP) + ".");
      return (result.withChart(chart(ChartKind.TIME_SERIES, series, numeric ? "Numeric quartiles; PSI uses fixed first-complete-bucket quartile boundaries."
         : "Stacked category shares. PSI floors zero shares at 0.000001; it is not a significance test.").withStacked(!numeric)));
   }

   /*******************************************************************************
    ** Calendar boundaries use the session zone, including daylight-saving days.
    *******************************************************************************/
   private Bucket bucket(String field, Serializable value, DateBucketUnit unit, QFieldType type)
   {
      ZoneId zone = ValueUtils.getSessionOrInstanceZoneId();
      if(unit == DateBucketUnit.HOUR)
      {
         Instant start = ValueUtils.getValueAsInstant(value);
         Instant end = start.plusSeconds(3600);
         return (new Bucket(start, DrillFilters.range(field, start, end, false), !end.isAfter(Instant.now())));
      }
      LocalDate start = ValueUtils.getValueAsLocalDate(value);
      LocalDate end = next(start, DateUnit.valueOf(unit.name()));
      return (new Bucket(start, DrillFilters.dateRange(field, start, end, type == QFieldType.DATE_TIME, zone), !end.atStartOfDay(zone).toInstant().isAfter(Instant.now())));
   }

   /*******************************************************************************
    ** Both populations use identical, half-open quartile ranges with infinite tails.
    *******************************************************************************/
   private double[] numericCounts(String field, QQueryFilter filter, Map<String, BigDecimal> baseline) throws QException
   {
      BigDecimal[] edges = {null, baseline.get("p25"), baseline.get("p50"), baseline.get("p75"), null};
      double[] counts = new double[4];
      for(int i = 0; i < 4; i++)
      {
         counts[i] = size(DrillFilters.and(filter, DrillFilters.range(field, edges[i], edges[i + 1], false)));
      }
      return (counts);
   }
}
