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

import java.util.Set;
import com.kingsrook.qbits.dataanalysis.result.CandidateKey;
import com.kingsrook.qbits.dataanalysis.result.Chart;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfile;
import com.kingsrook.qbits.dataanalysis.result.DateProfile;
import com.kingsrook.qbits.dataanalysis.result.DistinctValuesPage;
import com.kingsrook.qbits.dataanalysis.result.Drill;
import com.kingsrook.qbits.dataanalysis.result.DuplicatesResult;
import com.kingsrook.qbits.dataanalysis.result.FieldSummary;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.NumericProfile;
import com.kingsrook.qbits.dataanalysis.result.Point;
import com.kingsrook.qbits.dataanalysis.result.Series;
import com.kingsrook.qbits.dataanalysis.result.TableProfile;
import com.kingsrook.qbits.dataanalysis.result.TextProfile;
import com.kingsrook.qbits.dataanalysis.result.ValueCount;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;

/*******************************************************************************
 ** Final defense for profile-generated drills, including representative primary
 ** keys and prefix bounds. User-authored predicates remain intact; unsafe generated
 ** drills are disabled rather than broadened by removing their conditions.
 *******************************************************************************/
public class AnalysisDrillGuard
{
   private final Set<String> fields;
   private final String original;

   /*******************************************************************************
    ** Resolve FULL fields once per completed result.
    *******************************************************************************/
   public AnalysisDrillGuard(Set<String> fields, QQueryFilter original)
   {
      this.fields = fields;
      this.original = JsonUtils.toJson(DrillFilters.copy(original));
   }

   /*******************************************************************************
    ** Visit only typed result containers, never the caller's query.
    *******************************************************************************/
   public void apply(Object value)
   {
      if(value instanceof Iterable<?> list)
      {
         list.forEach(this::apply);
      }
      else if(value instanceof Metric metric)
      {
         metric.withDrill(permitted(metric.getDrill()));
      }
      else if(value instanceof ValueCount count)
      {
         count.withDrill(permitted(count.getDrill()));
      }
      else if(value instanceof Point point)
      {
         point.withDrill(permitted(point.getDrill()));
      }
      else if(value instanceof CandidateKey key)
      {
         key.withDrill(permitted(key.getDrill()));
      }
      else if(value instanceof ColumnProfile profile)
      {
         apply(profile.getCounts());
         apply(profile.getTopValues());
         apply(profile.getBooleans());
         apply(profile.getNumeric());
         apply(profile.getDates());
         apply(profile.getText());
         apply(profile.getChart());
      }
      else if(value instanceof NumericProfile profile)
      {
         apply(profile.getMetrics());
         apply(profile.getHistogram());
         apply(profile.getOutliers());
      }
      else if(value instanceof DateProfile profile)
      {
         apply(profile.getMetrics());
         apply(profile.getBuckets());
         apply(profile.getGaps());
         apply(profile.getDayOfWeek());
         apply(profile.getHourOfDay());
      }
      else if(value instanceof TextProfile profile)
      {
         apply(profile.getMetrics());
         apply(profile.getHistogram());
         apply(profile.getPatterns());
      }
      else if(value instanceof TableProfile profile)
      {
         apply(profile.getRowCount());
         apply(profile.getFields());
         apply(profile.getCandidateKeys());
         apply(profile.getHeatmap());
      }
      else if(value instanceof FieldSummary field)
      {
         apply(field.getNulls());
         apply(field.getBlanks());
      }
      else if(value instanceof Chart chart)
      {
         apply(chart.getSeries());
      }
      else if(value instanceof Series series)
      {
         apply(series.getPoints());
      }
      else if(value instanceof DistinctValuesPage page)
      {
         apply(page.getValues());
      }
      else if(value instanceof DuplicatesResult result)
      {
         apply(result.getGroups());
      }
   }

   /*******************************************************************************
    ** Preserve count equivalence by removing the entire unsafe drill.
    *******************************************************************************/
   private Drill permitted(Drill drill)
   {
      return (drill == null || safe(drill.getFilter()) ? drill : null);
   }

   /*******************************************************************************
    ** An exact copy of a user subtree is exempt from analysis field restrictions.
    *******************************************************************************/
   private boolean safe(QQueryFilter filter)
   {
      if(original.equals(JsonUtils.toJson(DrillFilters.copy(filter))))
      {
         return (true);
      }
      return (filter.getCriteria().stream().allMatch(c -> fields.contains(c.getFieldName())
         && (c.getOtherFieldName() == null || fields.contains(c.getOtherFieldName()))
         && (c.getFieldFunction() == null || fields.contains(c.getFieldFunction().getFieldName())))
         && filter.getSubFilters().stream().allMatch(this::safe));
   }
}
