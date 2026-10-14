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

import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfile;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.ValueCount;

/*******************************************************************************
 ** Privacy transformation before chart construction, serialization and caching.
 *******************************************************************************/
public class MinGroupSizeGuard
{
   private final int minimum;

   /*******************************************************************************
    ** Zero disables suppression.
    *******************************************************************************/
   public MinGroupSizeGuard(int minimum)
   {
      this.minimum = minimum;
   }



   /*******************************************************************************
    ** Never carry original values or drills into a merged group.
    *******************************************************************************/
   public List<ValueCount> groups(List<ValueCount> groups)
   {
      if(minimum == 0)
      {
         return (groups);
      }
      List<ValueCount> result = new ArrayList<>();
      long count = 0;
      int hidden = 0;
      java.math.BigDecimal share = java.math.BigDecimal.ZERO;
      for(ValueCount group : groups)
      {
         if(group.getCount() < minimum)
         {
            hidden++;
            count += group.getCount();
            share = share.add(group.getShare());
         }
         else
         {
            result.add(group);
         }
      }
      if(hidden > 0)
      {
         result.add(new ValueCount().withLabel("Other (" + hidden + " groups below " + minimum + ")").withCount(count).withShare(AnalysisFormatting.ratio(share)));
      }
      return (result);
   }



   /*******************************************************************************
    ** Suppressed scalar values are removed, not just tagged as hidden.
    *******************************************************************************/
   public void metrics(List<Metric> metrics, long rows)
   {
      if(rows >= minimum)
      {
         return;
      }
      for(Metric metric : metrics)
      {
         String key = metric.getKey();
         if(key.startsWith("p") || key.startsWith("min") || key.startsWith("max") || key.startsWith("mean") || key.equals("stddev") || key.equals("range"))
         {
            metric.withValue(null).withFormatted(null).withDrill(null).withSuppressed(true)
               .withSuppressedReason("Hidden: fewer than " + minimum + " records");
         }
      }
   }



   /*******************************************************************************
    ** Remove extreme values and their drills whenever privacy is enabled.
    *******************************************************************************/
   public void column(ColumnProfile profile, long rows)
   {
      profile.withTopValues(groups(profile.getTopValues()));
      if(minimum > 0)
      {
         profile.getCounts().stream().filter(m -> m.getKey().equals("distinct")).forEach(m -> m.withDrill(null));
         if(profile.getBooleans() != null)
         {
            List<ValueCount> booleanGroups = profile.getBooleans().stream().map(m -> new ValueCount().withValue(m.getKey()).withLabel(m.getLabel())
               .withCount(((Number) m.getValue()).longValue()).withShare(ProfileSupport.share(((Number) m.getValue()).longValue(), rows)).withDrill(m.getDrill())).toList();
            profile.withBooleans(groups(booleanGroups).stream().map(v -> ProfileSupport.metric(v.getValue() == null ? "other" : v.getValue().toString(), v.getCount(),
               com.kingsrook.qbits.dataanalysis.result.ValueType.COUNT).withDrill(v.getDrill())).toList());
         }
      }
      if(profile.getNumeric() != null)
      {
         metrics(profile.getNumeric().getMetrics(), rows);
         profile.getNumeric().withHistogram(groups(profile.getNumeric().getHistogram()));
         if(minimum > 0)
         {
            profile.getNumeric().withOutliers(List.of());
            profile.getNumeric().getMetrics().stream().filter(m -> m.getKey().equals("outliers")).forEach(m -> m.withDrill(null));
         }
      }
      if(profile.getDates() != null)
      {
         metrics(profile.getDates().getMetrics(), rows);
         profile.getDates().withBuckets(groups(profile.getDates().getBuckets())).withDayOfWeek(groups(profile.getDates().getDayOfWeek()))
            .withHourOfDay(groups(profile.getDates().getHourOfDay()));
         if(minimum > 0)
         {
            profile.getDates().withGaps(List.of());
         }
      }
      if(profile.getText() != null)
      {
         metrics(profile.getText().getMetrics(), rows);
         profile.getText().withHistogram(groups(profile.getText().getHistogram())).withPatterns(groups(profile.getText().getPatterns()));
      }
   }
}
