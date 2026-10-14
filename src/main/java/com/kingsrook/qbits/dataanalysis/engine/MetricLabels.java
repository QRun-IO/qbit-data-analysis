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

import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.dataanalysis.result.ValueType;

/*******************************************************************************
 ** One vocabulary for every emitted metric, including date-specific extrema.
 *******************************************************************************/
public final class MetricLabels
{
   private static final Map<String, String> LABELS = Map.ofEntries(
      Map.entry("rows", "Rows"), Map.entry("nonNull", "Non-empty"), Map.entry("nulls", "Nulls"),
      Map.entry("blanks", "Blanks"), Map.entry("distinct", "Distinct values"), Map.entry("uniqueness", "Uniqueness"),
      Map.entry("completeness", "Completeness"), Map.entry("share", "Share"),
      Map.entry("min", "Minimum"), Map.entry("max", "Maximum"), Map.entry("mean", "Mean"),
      Map.entry("median", "Median"), Map.entry("stddev", "Standard deviation"), Map.entry("sum", "Sum"),
      Map.entry("zeros", "Zeros"), Map.entry("negatives", "Negatives"), Map.entry("outliers", "Outliers"),
      Map.entry("p1", "1st percentile"), Map.entry("p5", "5th percentile"), Map.entry("p25", "25th percentile (Q1)"),
      Map.entry("p50", "Median"), Map.entry("p75", "75th percentile (Q3)"), Map.entry("p95", "95th percentile"), Map.entry("p99", "99th percentile"),
      Map.entry("minLength", "Shortest"), Map.entry("maxLength", "Longest"), Map.entry("meanLength", "Average length"),
      Map.entry("earliest", "Earliest"), Map.entry("latest", "Latest"), Map.entry("range", "Range"),
      Map.entry("true", "True"), Map.entry("false", "False"), Map.entry("other", "Other"));

   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private MetricLabels()
   {
   }



   /*******************************************************************************
    ** Fail at construction when a new metric lacks a human label.
    *******************************************************************************/
   public static String label(String key, ValueType type)
   {
      if(type == ValueType.DATE || type == ValueType.DATE_TIME)
      {
         key = "min".equals(key) ? "earliest" : "max".equals(key) ? "latest" : key;
      }
      return (Objects.requireNonNull(LABELS.get(key), "Missing metric label: " + key));
   }
}
