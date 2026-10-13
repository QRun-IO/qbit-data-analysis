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
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.dataanalysis.result.Axis;
import com.kingsrook.qbits.dataanalysis.result.Chart;
import com.kingsrook.qbits.dataanalysis.result.ChartKind;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.Point;
import com.kingsrook.qbits.dataanalysis.result.Series;
import com.kingsrook.qbits.dataanalysis.result.ValueCount;
import com.kingsrook.qbits.dataanalysis.result.ValueType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;

/*******************************************************************************
 ** Shared semantic result construction; profilers contain no rendering concerns.
 *******************************************************************************/
public final class ProfileSupport
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private ProfileSupport()
   {
   }



   /*******************************************************************************
    ** Empty populations have zero share, never NaN.
    *******************************************************************************/
   public static BigDecimal share(long count, long rows)
   {
      return (rows == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(count).divide(BigDecimal.valueOf(rows), MathContext.DECIMAL128));
   }



   /*******************************************************************************
    ** Every ordinary count carries its exact membership predicate.
    *******************************************************************************/
   public static Metric count(String key, long count, String table, QQueryFilter filter, QQueryFilter condition)
   {
      return (metric(key, count, ValueType.COUNT).withDrill(DrillFilters.drill(table, filter, condition)));
   }



   /*******************************************************************************
    ** Scalars have semantic types and locale-neutral formatted values.
    *******************************************************************************/
   public static Metric metric(String key, Serializable value, ValueType type)
   {
      return (new Metric().withKey(key).withLabel(key).withValue(value).withValueType(type).withFormatted(value == null ? null : value.toString()));
   }



   /*******************************************************************************
    ** The population row total is exact even when other metrics are estimates.
    *******************************************************************************/
   public static void markEstimates(List<Metric> metrics)
   {
      if(metrics != null)
      {
         metrics.stream().filter(m -> !"rows".equals(m.getKey())).forEach(m ->
         {
            m.withEstimate(true);
            if(m.getFormatted() != null)
            {
               m.withFormatted("≈ " + m.getFormatted());
            }
         });
      }
   }

   /*******************************************************************************
    ** PVS translations run as the current user; drill values always remain ids.
    *******************************************************************************/
   public static List<ValueCount> values(String table, QQueryFilter filter, QFieldMetaData field, List<AnalysisBackend.GroupCount> groups, long total) throws QException
   {
      List<ValueCount> result = new ArrayList<>();
      for(AnalysisBackend.GroupCount group : groups)
      {
         String label = group.label();
         if(field != null && field.getPossibleValueSourceName() != null && group.value() != null)
         {
            String translated = AnalysisLabels.translate(field, group.value());
            if(translated != null)
            {
               label = translated;
            }
         }
         result.add(new ValueCount().withValue(group.value()).withLabel(label).withCount(group.count()).withShare(group.share() == null ? share(group.count(), total) : group.share())
            .withDrill(DrillFilters.drill(table, filter, group.condition())));
      }
      return (result);
   }



   /*******************************************************************************
    ** Charts are built only from privacy-guarded groups.
    *******************************************************************************/
   public static Chart chart(ChartKind kind, ChartKind alternative, String field, List<ValueCount> values)
   {
      Series series = new Series().withName("Count");
      for(ValueCount value : values)
      {
         series.getPoints().add(new Point().withX(value.getValue()).withY(BigDecimal.valueOf(value.getCount()))
            .withLabel(value.getLabel()).withDrill(value.getDrill()));
      }
      Chart chart = new Chart().withKind(kind).withAlternatives(List.of(alternative))
         .withX(new Axis().withField(field).withLabel(field).withValueType(ValueType.TEXT))
         .withY(new Axis().withLabel("Count").withValueType(ValueType.COUNT)).withSeries(new ArrayList<>(List.of(series)));
      if(kind == ChartKind.TIME_SERIES)
      {
         Series average = new Series().withName("7-bucket moving average");
         for(int i = 0; i < values.size(); i++)
         {
            long sum = 0;
            for(int j = Math.max(0, i - 6); j <= i; j++)
            {
               sum += values.get(j).getCount();
            }
            average.getPoints().add(new Point().withX(values.get(i).getValue()).withLabel(values.get(i).getLabel())
               .withY(BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(Math.min(7, i + 1)), MathContext.DECIMAL128)));
         }
         chart.getSeries().add(average);
      }
      return (chart);
   }



   /*******************************************************************************
    ** Convert instants in the session zone; DATE values are already local dates.
    *******************************************************************************/
   public static java.time.LocalDate day(Serializable value, java.time.ZoneId zone)
   {
      return (value instanceof java.time.LocalDate date ? date : ((java.time.Instant) value).atZone(zone).toLocalDate());
   }
}
