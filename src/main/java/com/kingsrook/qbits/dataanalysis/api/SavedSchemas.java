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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;

/*******************************************************************************
 ** Versioned saved envelopes reuse all seven existing typed request/result schemas.
 *******************************************************************************/
public final class SavedSchemas
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private SavedSchemas()
   {
   }

   /*******************************************************************************
    ** JSON Schema union supported by the standard OpenAPI serializer.
    *******************************************************************************/
   public static class RequestUnion extends Schema
   {
      /*******************************************************************************
       ** All supported typed analysis requests, selected by the sibling kind.
       *******************************************************************************/
      public List<Schema> getAnyOf()
      {
         return (List.of("ColumnProfile", "TableProfile", "Pivot", "Compare", "Correlation", "Crosstab", "Shift").stream()
            .map(name -> ref(name + "Request")).toList());
      }
   }

   /*******************************************************************************
    ** Every new endpoint's request and response is discoverable by clients.
    *******************************************************************************/
   public static Map<String, Schema> components()
   {
      Map<String, Schema> result = new LinkedHashMap<>();
      Map<String, Schema> fields = new LinkedHashMap<>();
      fields.put("kind", new Schema().withType(Type.STRING).withEnumValues(List.of("COLUMN_PROFILE", "TABLE_PROFILE", "PIVOT", "COMPARE", "CORRELATION", "CROSSTAB", "SHIFT")));
      fields.put("tableName", new Schema().withType(Type.STRING).withDescription("Source QQQ table name; required for ad hoc export, supplied by the path for SQL."));
      fields.put("request", new RequestUnion().withDescription("Typed request matching kind. Field names, filters and dimensions use the existing analysis request contracts."));
      fields.put("format", new Schema().withType(Type.STRING).withEnumValues(List.of("csv", "xlsx")).withDescription("Required for export; ignored for SQL."));
      result.put("DataAnalysisSavedRequestV1", new DataAnalysisSchemas.RequiredSchema(List.of("kind", "request")).withType(Type.OBJECT).withProperties(fields));
      Map<String, Schema> response = new LinkedHashMap<>();
      response.put("kind", fields.get("kind"));
      response.put("warnings", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)));
      new java.util.TreeMap<>(Map.of("columnProfile", "ColumnProfile", "tableProfile", "TableProfile", "pivot", "Pivot", "compare", "Compare",
         "correlation", "Correlation", "crosstab", "Crosstab", "shift", "Shift")).forEach((key, value) -> response.put(key, ref(value).withNullable(true)));
      result.put("DataAnalysisSavedResultV1", new Schema().withType(Type.OBJECT).withProperties(response));
      Map<String, Schema> entry = new LinkedHashMap<>();
      entry.put("id", new Schema().withType(Type.INTEGER));
      for(String name : List.of("name", "kind", "tableName", "tableLabel", "owner", "updated"))
      {
         entry.put(name, new Schema().withType(Type.STRING));
      }
      entry.put("updated", new Schema().withType(Type.STRING).withFormat("date-time").withNullable(true));
      entry.put("shared", new Schema().withType(Type.BOOLEAN));
      entry.put("starred", new Schema().withType(Type.BOOLEAN));
      result.put("DataAnalysisSavedEntryV1", new Schema().withType(Type.OBJECT).withProperties(entry));
      result.put("DataAnalysisSavedListV1", new Schema().withType(Type.OBJECT).withProperties(new java.util.TreeMap<>(Map.of("analyses", new Schema().withType(Type.ARRAY).withItems(ref("SavedEntry"))))));
      result.put("DataAnalysisStarV1", new Schema().withType(Type.OBJECT).withProperties(new java.util.TreeMap<>(Map.of("starred", new Schema().withType(Type.BOOLEAN)))));
      result.put("DataAnalysisSqlV1", new Schema().withType(Type.OBJECT).withProperties(new java.util.TreeMap<>(Map.of("available", new Schema().withType(Type.BOOLEAN),
         "sql", new Schema().withType(Type.STRING).withNullable(true), "note", new Schema().withType(Type.STRING)))));
      result.put("DataAnalysisFileV1", new Schema().withType(Type.STRING).withFormat("binary"));
      return (result);
   }

   /*******************************************************************************
    ** Reuse the extension's stable schema prefix.
    *******************************************************************************/
   private static Schema ref(String name)
   {
      return (new Schema().withRef("#/components/schemas/DataAnalysis" + name + "V1"));
   }
}
