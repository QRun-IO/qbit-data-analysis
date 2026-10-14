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

package com.kingsrook.qbits.dataanalysis.api;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;

/*******************************************************************************
 ** Standalone watch preview contracts referencing the existing chart and drill.
 *******************************************************************************/
public class WatchSchemas
{
   /*******************************************************************************
    ** Register every referenced version-one watch response component.
    *******************************************************************************/
   public static Map<String, Schema> components()
   {
      return (Map.of("DataAnalysisWatchPreviewV1", new DataAnalysisWatchPreviewV1().toSchema(),
         "DataAnalysisWatchBucketV1", new DataAnalysisWatchBucketV1().toSchema(),
         "DataAnalysisWatchVerdictV1", new DataAnalysisWatchVerdictV1().toSchema()));
   }

   /*******************************************************************************
    ** One complete bucket; suppressed series never expose categories or drills.
    *******************************************************************************/
   public static class DataAnalysisWatchBucketV1 implements ToSchema
   {
      public Serializable start;
      public Double value;
      public List<String> categories;
      public DataAnalysisSchemas.DataAnalysisDrillV1 drill;

      /*******************************************************************************
       ** Explicit nullable types and component references.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("start", new Schema().withType(Type.STRING).withDescription("Calendar date or hour-start instant in the user's time zone."));
         properties.put("value", new Schema().withType(Type.NUMBER).withNullable(true));
         properties.put("categories", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)));
         properties.put("drill", new Schema().withRef("#/components/schemas/DataAnalysisDrillV1").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** Unevaluated means no state change; baseline bounds describe the annotation.
    *******************************************************************************/
   public static class DataAnalysisWatchVerdictV1 implements ToSchema
   {
      public Boolean evaluated;
      public Boolean firing;
      public Double value;
      public Double baseline;
      public Double baselineLow;
      public Double baselineHigh;
      public String message;
      public DataAnalysisSchemas.DataAnalysisDrillV1 drill;

      /*******************************************************************************
       ** Carry numeric baseline bounds as well as the plain-English explanation.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("evaluated", new Schema().withType(Type.BOOLEAN).withDescription("False for insufficient history or privacy suppression; must not resolve an alert."));
         properties.put("firing", new Schema().withType(Type.BOOLEAN));
         for(String field : List.of("value", "baseline", "baselineLow", "baselineHigh"))
         {
            properties.put(field, new Schema().withType(Type.NUMBER).withNullable(true));
         }
         properties.put("message", new Schema().withType(Type.STRING));
         properties.put("drill", new Schema().withRef("#/components/schemas/DataAnalysisDrillV1").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** Complete preview, using the same semantic chart as other analysis endpoints.
    *******************************************************************************/
   public static class DataAnalysisWatchPreviewV1 implements ToSchema
   {
      public List<DataAnalysisWatchBucketV1> series;
      public DataAnalysisSchemas.DataAnalysisChartV1 chart;
      public DataAnalysisWatchVerdictV1 verdict;

      /*******************************************************************************
       ** Stable references let clients vendor this fragment without middleware.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("series", new Schema().withType(Type.ARRAY).withItems(new Schema().withRef("#/components/schemas/DataAnalysisWatchBucketV1")));
         properties.put("chart", new Schema().withRef("#/components/schemas/DataAnalysisChartV1").withDescription("TIME_SERIES with baseline band annotations."));
         properties.put("verdict", new Schema().withRef("#/components/schemas/DataAnalysisWatchVerdictV1"));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }
}
