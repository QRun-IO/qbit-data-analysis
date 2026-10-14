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

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;

/*******************************************************************************
 ** Validated watch configuration; JSON accepts only documented, typed parameters.
 *******************************************************************************/
public record WatchDefinition(String detector, String metric, String field, String dateField, String bucket,
   Integer k, Double threshold, Double relativeChange, Double min, Double max, Double delta, Integer n)
{
   /*******************************************************************************
    ** Parse a definition after resolving an optional saved analysis.
    *******************************************************************************/
   public static WatchDefinition parse(QRecord row) throws QBadRequestException
   {
      String detector = row.getValueString("detector");
      require(List.of("SPIKE", "DROP", "OUT_OF_RANGE", "NEW_CATEGORY", "NULL_RATE_CREEP").contains(detector == null ? "" : detector), "Choose a supported detector.");
      Map<String, Object> metric = object(row.getValueString("metric_json"), "metric_json");
      require(metric.keySet().stream().allMatch(Set.of("operator", "field")::contains), "metric_json accepts operator and field only.");
      String operator = string(metric.get("operator"));
      String field = string(metric.get("field"));
      require(List.of("COUNT", "SUM", "AVG", "NULL_SHARE", "CATEGORY_SET").contains(operator == null ? "" : operator), "Metric operator must be COUNT, SUM, AVG, NULL_SHARE or CATEGORY_SET.");
      require("COUNT".equals(operator) || field != null && !field.isBlank(), "This metric requires a field.");
      require("NEW_CATEGORY".equals(detector) == "CATEGORY_SET".equals(operator), "NEW_CATEGORY requires CATEGORY_SET; other detectors require a numeric metric.");
      require(!"NULL_RATE_CREEP".equals(detector) || "NULL_SHARE".equals(operator), "NULL_RATE_CREEP requires NULL_SHARE.");
      String date = row.getValueString("date_field");
      String bucket = row.getValueString("bucket");
      require(date != null && !date.isBlank(), "Choose a date field.");
      require(List.of("HOUR", "DAY", "WEEK", "MONTH").contains(bucket == null ? "" : bucket), "Bucket must be HOUR, DAY, WEEK or MONTH.");
      try
      {
         AlertSeverity.valueOf(row.getValueString("severity"));
      }
      catch(RuntimeException e)
      {
         throw new QBadRequestException("Choose a valid alert severity.");
      }
      Map<String, Object> parameters = object(row.getValueString("parameters_json"), "parameters_json");
      Set<String> allowed = switch(detector)
      {
         case "SPIKE", "DROP" -> Set.of("k", "threshold", "relativeChange");
         case "OUT_OF_RANGE" -> Set.of("k", "min", "max");
         case "NULL_RATE_CREEP" -> Set.of("k", "delta", "n");
         default -> Set.of("k");
      };
      require(allowed.containsAll(parameters.keySet()), "Unsupported parameter for " + detector + ". Allowed: " + String.join(", ", allowed.stream().sorted().toList()) + ".");
      int k = integer(parameters, "k", 14, 1, 1000);
      int n = integer(parameters, "n", 3, 1, 1000);
      double threshold = number(parameters, "threshold", 3.5);
      double relative = number(parameters, "relativeChange", 0.5);
      double delta = number(parameters, "delta", 0.05);
      require(threshold > 0, "threshold must be positive.");
      require(relative >= 0, "relativeChange must be non-negative (0.5 means 50%).");
      require(delta >= 0 && delta <= 1, "delta must be between 0 and 1.");
      Double min = parameters.containsKey("min") ? number(parameters, "min", 0) : null;
      Double max = parameters.containsKey("max") ? number(parameters, "max", 0) : null;
      require(!"OUT_OF_RANGE".equals(detector) || min != null && max != null && min <= max, "OUT_OF_RANGE requires min and max with min <= max.");
      return (new WatchDefinition(detector, operator, field, date, bucket, k, threshold, relative, min, max, delta, n));
   }

   /*******************************************************************************
    ** Strict JSON objects keep malformed definitions out of standard CRUD.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public static Map<String, Object> object(String json, String name) throws QBadRequestException
   {
      try
      {
         Object value = JsonUtils.toObject(json == null || json.isBlank() ? "{}" : json, Object.class);
         if(value instanceof Map<?, ?> map)
         {
            return ((Map<String, Object>) map);
         }
      }
      catch(Exception expected)
      {
         // Report a stable validation error, without echoing stored JSON.
      }
      throw new QBadRequestException(name + " must be a JSON object.");
   }

   /*******************************************************************************
    ** Numeric values must be finite JSON numbers, never coerced strings.
    *******************************************************************************/
   private static double number(Map<String, Object> values, String key, double fallback) throws QBadRequestException
   {
      if(!values.containsKey(key))
      {
         return (fallback);
      }
      require(values.get(key) instanceof Number, key + " must be a number.");
      double value = ((Number) values.get(key)).doubleValue();
      require(Double.isFinite(value), key + " must be finite.");
      return (value);
   }

   /*******************************************************************************
    ** Bound work and reject fractional window sizes.
    *******************************************************************************/
   private static int integer(Map<String, Object> values, String key, int fallback, int min, int max) throws QBadRequestException
   {
      double value = number(values, key, fallback);
      require(value == Math.rint(value) && value >= min && value <= max, key + " must be an integer from " + min + " to " + max + ".");
      return ((int) value);
   }

   /*******************************************************************************
    ** Optional strings still reject incompatible scalar types.
    *******************************************************************************/
   private static String string(Object value) throws QBadRequestException
   {
      require(value == null || value instanceof String, "Metric operator and field must be strings.");
      return ((String) value);
   }

   /*******************************************************************************
    ** Clear errors shared by table validation and preview.
    *******************************************************************************/
   public static void require(boolean valid, String message) throws QBadRequestException
   {
      if(!valid)
      {
         throw new QBadRequestException(message);
      }
   }
}
