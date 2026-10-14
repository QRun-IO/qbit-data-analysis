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
import com.kingsrook.qbits.dataanalysis.DataAnalysisRuntime;
import com.kingsrook.qbits.dataanalysis.saved.SavedDefinition;
import com.kingsrook.qbits.dataanalysis.saved.SqlHandoff;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.openapi.model.HttpMethod;

/*******************************************************************************
 ** Saved execution, stars, visible lists and SQL discovery through the UI API.
 *******************************************************************************/
public final class SavedApi
{
   /*******************************************************************************
    ** Namespace only.
    *******************************************************************************/
   private SavedApi()
   {
   }

   /*******************************************************************************
    ** Stable integer-id errors, without probing inaccessible records.
    *******************************************************************************/
   public static Integer id(String value) throws QBadRequestException
   {
      try
      {
         return (Integer.valueOf(value));
      }
      catch(Exception e)
      {
         throw new QBadRequestException("Saved analysis id must be an integer.");
      }
   }

   /*******************************************************************************
    ** Accept a discriminated typed request; path table overrides only absent tableName.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public static SavedDefinition definition(Map<String, Object> body, String pathTable) throws QException
   {
      if(!(body.get("kind") instanceof String kind))
      {
         throw new QBadRequestException("An analysis kind is required.");
      }
      Map<String, Object> request = new LinkedHashMap<>(body);
      request.remove("kind");
      request.remove("format");
      if(body.containsKey("request"))
      {
         if(!(body.get("request") instanceof Map<?, ?> map))
         {
            throw new QBadRequestException("request must be a typed analysis object.");
         }
         request = new LinkedHashMap<>((Map<String, Object>) map);
      }
      Object table = pathTable == null ? body.getOrDefault("tableName", request.get("tableName")) : pathTable;
      if(!(table instanceof String name))
      {
         throw new QBadRequestException("tableName is required.");
      }
      return (SavedDefinition.parse(kind, name, request));
   }

   /*******************************************************************************
    ** Run the visible saved definition under the viewer's current source permissions.
    *******************************************************************************/
   public static class Run extends DataAnalysisApi.ProfileSpec
   {
      /*******************************************************************************
       ** Publish assistant-oriented semantics and field-reduction behavior.
       *******************************************************************************/
      public Run()
      {
         super("/saved/{id}/run", HttpMethod.POST, "Run a saved analysis",
            "Re-run a visible saved analysis as the signed-in viewer. Requires saved READ and source READ/Analyze. Drops unavailable fields with a warning and returns the result typed by kind; inaccessible required axes or predicates produce no result.", null, "SavedResult");
      }

      /*******************************************************************************
       ** Core locks determine visibility, not an endpoint-specific owner check.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (DataAnalysisRuntime.saved().run(id(input.getPathParam("id"))));
      }
   }

   /*******************************************************************************
    ** Visible saved metadata with this viewer's personal stars.
    *******************************************************************************/
   public static class ListSaved extends DataAnalysisApi.ProfileSpec
   {
      /*******************************************************************************
       ** A discoverable list rather than exposing the owner's cached result JSON.
       *******************************************************************************/
      public ListSaved()
      {
         super("/saved", HttpMethod.GET, "List visible saved analyses",
            "List core-visible saved analyses with name, kind, source table label, owner, shared state, this viewer's starred flag and last update.", null, "SavedList");
      }

      /*******************************************************************************
       ** The list and star lookup both use enforced core queries.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (Map.of("analyses", DataAnalysisRuntime.saved().list()));
      }
   }

   /*******************************************************************************
    ** Idempotent star PUT for the signed-in user.
    *******************************************************************************/
   public static class Star extends DataAnalysisApi.ProfileSpec
   {
      /*******************************************************************************
       ** No user-id body is accepted; stars always belong to the caller.
       *******************************************************************************/
      public Star()
      {
         super("/saved/{id}/star", HttpMethod.PUT, "Star a saved analysis",
            "Star a visible saved analysis for the signed-in user. Idempotent; uses standard saved insert permission and the personal star record lock.", null, "Star");
      }

      /*******************************************************************************
       ** Persist through core table CRUD.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (Map.of("starred", DataAnalysisRuntime.saved().star(id(input.getPathParam("id")), true)));
      }
   }

   /*******************************************************************************
    ** Idempotent star DELETE for the signed-in user.
    *******************************************************************************/
   public static class Unstar extends DataAnalysisApi.ProfileSpec
   {
      /*******************************************************************************
       ** Removal never targets another user's star.
       *******************************************************************************/
      public Unstar()
      {
         super("/saved/{id}/star", HttpMethod.DELETE, "Unstar a saved analysis",
            "Remove the signed-in user's star on a visible saved analysis. Idempotent; uses standard saved delete permission and owner locks.", null, "Star");
      }

      /*******************************************************************************
       ** Persist through core table CRUD.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (Map.of("starred", DataAnalysisRuntime.saved().star(id(input.getPathParam("id")), false)));
      }
   }

   /*******************************************************************************
    ** Actual backend aggregate SQL, captured only during this request.
    *******************************************************************************/
   public static class Sql extends DataAnalysisApi.ProfileSpec
   {
      /*******************************************************************************
       ** The assistant gets a reading aid, never an alternative execution endpoint.
       *******************************************************************************/
      public Sql()
      {
         super("/tables/{table}/sql", HttpMethod.POST, "Read an analysis aggregate SQL statement",
            "Execute a typed analysis as the viewer and capture the main RDBMS aggregate SQL from core QueryStat. Requires installed SQL Workbench and its access permission. Non-RDBMS backends report unavailable; SQL is for reading and record security remains enforced at runtime.", "SavedRequest", "Sql");
      }

      /*******************************************************************************
       ** Requests pass through the same parser and analysis authorization.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (SqlHandoff.execute(definition(input.getBody(), input.getPathParam("table")), DataAnalysisRuntime.service()));
      }
   }
}
