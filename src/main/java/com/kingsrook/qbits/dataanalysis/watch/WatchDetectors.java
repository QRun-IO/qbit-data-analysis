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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisFormatting;

/*******************************************************************************
 ** Pure detectors over complete buckets, with no context, queries or side effects.
 *******************************************************************************/
public final class WatchDetectors
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private WatchDetectors()
   {
   }

   /*******************************************************************************
    ** Compare the latest value to the preceding k complete buckets.
    *******************************************************************************/
   public static WatchVerdict evaluate(WatchDefinition definition, List<WatchBucket> series, String label)
   {
      int k = definition.k();
      if(series.isEmpty() || !"OUT_OF_RANGE".equals(definition.detector()) && series.size() < k + 1)
      {
         return (WatchVerdict.skipped("Not enough history."));
      }
      WatchBucket latest = series.getLast();
      List<WatchBucket> history = series.subList(Math.max(0, series.size() - k - 1), series.size() - 1);
      if("NEW_CATEGORY".equals(definition.detector()))
      {
         Set<String> previous = new HashSet<>();
         history.forEach(bucket -> previous.addAll(bucket.categories()));
         long added = latest.categories().stream().filter(value -> !previous.contains(value)).count();
         return (new WatchVerdict(true, added > 0, (double) latest.categories().size(), (double) previous.size(), null, null,
            label + (added > 0 ? " has " + added + " new " + (added == 1 ? "category." : "categories.") : " has no new categories."), latest.drill()));
      }
      if(latest.value() == null || !Double.isFinite(latest.value()) || !"OUT_OF_RANGE".equals(definition.detector()) && history.stream().anyMatch(bucket -> bucket.value() == null || !Double.isFinite(bucket.value())))
      {
         return (WatchVerdict.skipped("Not enough history: a bucket has no numeric value."));
      }
      double x = latest.value();
      double[] values = history.stream().filter(bucket -> bucket.value() != null && Double.isFinite(bucket.value())).mapToDouble(WatchBucket::value).sorted().toArray();
      Double baseline = values.length == 0 ? null : median(values);
      Double low = values.length == 0 ? null : values[0];
      Double high = values.length == 0 ? null : values[values.length - 1];
      boolean firing;
      switch(definition.detector())
      {
         case "SPIKE", "DROP" ->
         {
            double mad = median(java.util.Arrays.stream(values).map(value -> Math.abs(value - baseline)).sorted().toArray());
            double change = x - baseline;
            double score = mad == 0 ? baseline == 0 ? Math.signum(change) * (change == 0 ? 0 : Double.POSITIVE_INFINITY)
               : change / Math.abs(baseline) : 0.6745 * change / mad;
            double threshold = mad == 0 ? definition.relativeChange() : definition.threshold();
            firing = "SPIKE".equals(definition.detector()) ? score > threshold : score < -threshold;
         }
         case "OUT_OF_RANGE" ->
         {
            low = definition.min();
            high = definition.max();
            firing = x < low || x > high;
         }
         case "NULL_RATE_CREEP" -> firing = x - baseline > definition.delta() || rising(series, definition.n());
         default -> throw new IllegalArgumentException("Unsupported numeric detector.");
      }
      String verb = firing ? "DROP".equals(definition.detector()) ? " dropped to " : "SPIKE".equals(definition.detector())
         || "NULL_RATE_CREEP".equals(definition.detector()) ? " rose to " : " is out of range at " : " is ";
      String band = low == null ? "" : " (" + ("OUT_OF_RANGE".equals(definition.detector()) ? "expected " : "usual ") + format(low) + "–" + format(high) + ")";
      return (new WatchVerdict(true, firing, x, baseline, low, high, label + verb + format(x) + band + ".", latest.drill()));
   }

   /*******************************************************************************
    ** n consecutive rises require n+1 values, including the latest bucket.
    *******************************************************************************/
   private static boolean rising(List<WatchBucket> series, int n)
   {
      if(series.size() <= n)
      {
         return (false);
      }
      for(int i = series.size() - n; i < series.size(); i++)
      {
         if(series.get(i).value() <= series.get(i - 1).value())
         {
            return (false);
         }
      }
      return (true);
   }

   /*******************************************************************************
    ** Median of a sorted non-empty array, including even-sized baselines.
    *******************************************************************************/
   private static double median(double[] values)
   {
      int middle = values.length / 2;
      return (values.length % 2 == 0 ? values[middle - 1] / 2 + values[middle] / 2 : values[middle]);
   }

   /*******************************************************************************
    ** Compact English numeric labels with no scientific notation.
    *******************************************************************************/
   private static String format(double value)
   {
      return (AnalysisFormatting.number(value, 3));
   }
}
