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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dataanalysis.result.ChartKind;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfile;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfileRequest;
import com.kingsrook.qbits.dataanalysis.result.DateProfile;
import com.kingsrook.qbits.dataanalysis.result.NumericProfile;
import com.kingsrook.qbits.dataanalysis.result.TextProfile;
import com.kingsrook.qbits.dataanalysis.result.ValueCount;
import com.kingsrook.qbits.dataanalysis.result.ValueType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;

/*******************************************************************************
 ** Column profiles depend only on primitive backend operations and typed results.
 *******************************************************************************/
public class ColumnProfiler
{
   private final AnalysisBackend backend;

   /*******************************************************************************
    ** The backend can be replaced when core's new operators ship.
    *******************************************************************************/
   public ColumnProfiler(AnalysisBackend backend)
   {
      this.backend = backend;
   }



   /*******************************************************************************
    ** Produce a guarded profile for a validated field and selected population.
    *******************************************************************************/
   public ColumnProfile profile(ColumnProfileRequest request, QFieldMetaData field, AnalysisBackend.Scope scope, EffectiveConfig config) throws QException
   {
      String table = request.getTableName();
      String name = field.getName();
      QQueryFilter filter = scope.filter();
      long rows = backend.count(table, filter);
      AnalysisBackend.FieldCounts counts = backend.fieldCounts(table, filter, List.of(name)).get(name);
      ColumnProfile result = new ColumnProfile().withTableName(table).withFieldName(name).withFieldLabel(field.getLabel()).withFieldType(field.getType().name())
         .withFilter(DrillFilters.restoreTypes(request.getTableName(), request.getFilter())).withEstimate(scope.estimate()).withSampleRows(scope.estimate() ? rows : 0L)
         .withRowsScanned(scope.rowsScanned()).withTruncated(scope.truncated()).withAsOf(Instant.now());
      result.getCounts().add(ProfileSupport.count("rows", rows, table, filter, null));
      result.getCounts().add(ProfileSupport.count("nonNull", counts.nonNull(), table, filter, DrillFilters.nonNull(name, field.getType().isStringLike())));
      result.getCounts().add(ProfileSupport.count("nulls", counts.nulls(), table, filter, DrillFilters.nulls(name, field.getType().isStringLike())));
      if(field.getType().isStringLike())
      {
         result.getCounts().add(ProfileSupport.count("blanks", counts.blanks(), table, filter, counts.blankFilter()));
      }
      result.getCounts().add(ProfileSupport.count("distinct", counts.distinct(), table, filter, counts.distinctFilter()));
      result.getCounts().add(ProfileSupport.metric("uniqueness", ProfileSupport.share(counts.distinct(), counts.nonNull()), ValueType.PERCENT));
      result.withTopValues(ProfileSupport.values(table, filter, field, backend.topValues(table, filter, name, 20, 0, null), rows));
      QFieldType type = field.getType();
      if(type == QFieldType.INTEGER || type == QFieldType.LONG || type == QFieldType.DECIMAL)
      {
         numeric(result, filter, rows);
      }
      else if(type == QFieldType.DATE || type == QFieldType.DATE_TIME)
      {
         dates(result, filter, rows, type);
      }
      else if(type.isStringLike())
      {
         AnalysisBackend.LengthSummary summary = backend.lengthSummary(table, filter, name);
         TextProfile text = new TextProfile().withHistogram(ProfileSupport.values(table, filter, null, summary.histogram(), rows))
            .withPatterns(ProfileSupport.values(table, filter, null, backend.patterns(table, filter, name, 10_000), Math.min(rows, 10_000)))
            .withSampleRows(Math.min(rows, 10_000));
         summary.metrics().forEach((key, value) -> text.getMetrics().add(ProfileSupport.metric(key, value, ValueType.DECIMAL)));
         result.withText(text);
         result.getWarnings().add("Patterns use the first " + Math.min(rows, 10_000) + " rows by primary key.");
      }
      else if(type == QFieldType.BOOLEAN)
      {
         result.withBooleans(new java.util.ArrayList<>());
         for(boolean value : List.of(true, false))
         {
            QQueryFilter condition = DrillFilters.value(name, value, false);
            result.getBooleans().add(ProfileSupport.count(Boolean.toString(value), backend.count(table, DrillFilters.and(filter, condition)), table, filter, condition));
         }
         result.getBooleans().add(ProfileSupport.count("nulls", counts.nulls(), table, filter, DrillFilters.nulls(name, false)));
      }
      new MinGroupSizeGuard(config.getMinGroupSize()).column(result, rows);
      if(field.getPossibleValueSourceName() != null || result.getNumeric() == null && result.getDates() == null)
      {
         result.withChart(ProfileSupport.chart(ChartKind.BAR, ChartKind.PARETO, name, result.getTopValues()));
      }
      else if(result.getNumeric() != null)
      {
         result.withChart(ProfileSupport.chart(ChartKind.HISTOGRAM, ChartKind.BOX, name, result.getNumeric().getHistogram()));
      }
      else
      {
         result.withChart(ProfileSupport.chart(ChartKind.TIME_SERIES, ChartKind.BAR, name, result.getDates().getBuckets()));
      }
      if(rows == 0)
      {
         result.getWarnings().add("No readable records match this analysis scope.");
      }
      else if(counts.nonNull() == 0)
      {
         result.getWarnings().add("All values in this analysis scope are null.");
      }
      if(scope.estimate())
      {
         result.getWarnings().add("Estimate: first " + rows + " rows by primary key; not a random sample.");
      }
      if(scope.truncated())
      {
         result.getWarnings().add("Partial result: max_rows reached; counts and drills cover only the selected prefix.");
      }
      return (result);
   }



   /*******************************************************************************
    ** Numeric moments, twenty interior bins, exterior bins and Tukey outliers.
    *******************************************************************************/
   private void numeric(ColumnProfile result, QQueryFilter filter, long rows) throws QException
   {
      String table = result.getTableName();
      String field = result.getFieldName();
      AnalysisBackend.NumericSummary summary = backend.numericSummary(table, filter, field,
         List.of("0.01", "0.05", "0.25", "0.50", "0.75", "0.95", "0.99").stream().map(BigDecimal::new).toList());
      NumericProfile numeric = new NumericProfile();
      summary.metrics().forEach((key, value) -> numeric.getMetrics().add(ProfileSupport.metric(key, value, ValueType.DECIMAL)));
      for(AnalysisBackend.GroupCount count : summary.counts())
      {
         numeric.getMetrics().add(ProfileSupport.count(count.label(), count.count(), table, filter, count.condition()));
      }
      numeric.getMetrics().add(ProfileSupport.count("outliers", summary.outlierCount(), table, filter, summary.outlierFilter()));
      numeric.withHistogram(ProfileSupport.values(table, filter, null,
         backend.numericHistogram(table, filter, field, summary.metrics().get("p1"), summary.metrics().get("p99"), 20), rows));
      numeric.withOutliers(ProfileSupport.values(table, filter, null, summary.outliers(), rows));
      result.withNumeric(numeric);
   }



   /*******************************************************************************
    ** Date extrema, range, calendar volume, gaps and cyclic time patterns.
    *******************************************************************************/
   private void dates(ColumnProfile result, QQueryFilter filter, long rows, QFieldType type) throws QException
   {
      String table = result.getTableName();
      String field = result.getFieldName();
      ZoneId zone = ValueUtils.getSessionOrInstanceZoneId();
      Map<String, Serializable> extrema = backend.extrema(table, filter, field);
      DateProfile dates = new DateProfile();
      ValueType valueType = type == QFieldType.DATE ? ValueType.DATE : ValueType.DATE_TIME;
      extrema.forEach((key, value) -> dates.getMetrics().add(ProfileSupport.metric(key, value, valueType)));
      if(extrema.get("min") != null && extrema.get("max") != null)
      {
         LocalDate first = ProfileSupport.day(extrema.get("min"), zone);
         LocalDate last = ProfileSupport.day(extrema.get("max"), zone);
         AnalysisBackend.DateUnit unit = autoBucket(first, last);
         dates.withUnit(unit.name());
         dates.getMetrics().add(ProfileSupport.metric("range", ChronoUnit.DAYS.between(first, last), ValueType.DURATION));
         List<ValueCount> buckets = ProfileSupport.values(table, filter, null, backend.dateBuckets(table, filter, field, unit, zone), rows);
         dates.withBuckets(buckets).withGaps(buckets.stream().filter(b -> b.getCount() == 0).toList());
         if(type == QFieldType.DATE_TIME)
         {
            dates.withDayOfWeek(ProfileSupport.values(table, filter, null, backend.dateBuckets(table, filter, field, AnalysisBackend.DateUnit.DAY_OF_WEEK, zone), rows))
               .withHourOfDay(ProfileSupport.values(table, filter, null, backend.dateBuckets(table, filter, field, AnalysisBackend.DateUnit.HOUR_OF_DAY, zone), rows));
         }
      }
      result.withDates(dates);
   }



   /*******************************************************************************
    ** Inclusive calendar boundaries: 92 days, two years and ten years.
    *******************************************************************************/
   public static AnalysisBackend.DateUnit autoBucket(LocalDate first, LocalDate last)
   {
      if(ChronoUnit.DAYS.between(first, last) <= 92)
      {
         return (AnalysisBackend.DateUnit.DAY);
      }
      if(!last.isAfter(first.plusYears(2)))
      {
         return (AnalysisBackend.DateUnit.WEEK);
      }
      return (!last.isAfter(first.plusYears(10)) ? AnalysisBackend.DateUnit.MONTH : AnalysisBackend.DateUnit.YEAR);
   }
}
