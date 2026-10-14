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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;

/*******************************************************************************
 ** Watch and saved-definition reads always use the active user's core permissions.
 *******************************************************************************/
public final class WatchRecords
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private WatchRecords()
   {
   }

   /*******************************************************************************
    ** Fetch heavy JSON with record locks; missing and inaccessible ids look alike.
    *******************************************************************************/
   public static QRecord get(String table, Serializable id) throws QException
   {
      QueryInput input = new QueryInput(table).withShouldFetchHeavyFields(true)
         .withFilter(DrillFilters.condition("id", QCriteriaOperator.EQUALS, id).withLimit(1));
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      List<QRecord> records = new QueryAction().execute(input).getRecords();
      if(records.isEmpty())
      {
         throw new QNotFoundException("Watch or saved analysis is unavailable.");
      }
      return (records.getFirst());
   }

   /*******************************************************************************
    ** Saved pivots provide one measure; explicit watch metrics can also be used.
    *******************************************************************************/
   public static QRecord resolve(QRecord row) throws QException
   {
      boolean saved = row.getValue("saved_id") != null;
      WatchDefinition.require(saved != (row.getValueString("table_name") != null && !row.getValueString("table_name").isBlank()), "Choose either saved_id or table_name.");
      QRecord result = new QRecord(row);
      if(saved)
      {
         WatchDefinition.require(row.getValueString("filter_json") == null || row.getValueString("filter_json").isBlank(), "Saved watches use the saved analysis filter; omit filter_json.");
         QRecord analysis = get(DataAnalysisTables.SAVED, row.getValue("saved_id"));
         result.withValue("table_name", analysis.getValue("table_name")).withValue("filter_json", analysis.getValue("filter_json"));
         Map<String, Object> definition = WatchDefinition.object(analysis.getValueString("definition_json"), "Saved definition");
         if(result.getValueString("metric_json") == null || result.getValueString("metric_json").isBlank())
         {
            Object metric = definition.get("metric");
            if(metric == null && definition.get("values") instanceof List<?> values && values.size() == 1)
            {
               metric = values.getFirst();
            }
            WatchDefinition.require(metric != null, "Saved analysis needs one COUNT, SUM or AVG measure, or an explicit watch metric_json.");
            Map<String, Object> selected = new LinkedHashMap<>(WatchDefinition.object(JsonUtils.toJson(metric), "Saved metric"));
            if(selected.get("percentile") == null)
            {
               selected.remove("percentile");
            }
            result.withValue("metric_json", JsonUtils.toJson(selected));
         }
         if(result.getValueString("filter_json") == null && definition.get("filter") != null)
         {
            result.withValue("filter_json", JsonUtils.toJson(definition.get("filter")));
         }
      }
      filter(result);
      return (result);
   }

   /*******************************************************************************
    ** Reject invalid filter JSON rather than silently broadening the watch.
    *******************************************************************************/
   public static QQueryFilter filter(QRecord row) throws QBadRequestException
   {
      String json = row.getValueString("filter_json");
      if(json == null || json.isBlank())
      {
         return (new QQueryFilter());
      }
      WatchDefinition.object(json, "filter_json");
      try
      {
         QQueryFilter filter = JsonUtils.toObject(json, QQueryFilter.class);
         validateFilter(filter);
         return (filter);
      }
      catch(Exception e)
      {
         throw new QBadRequestException("filter_json must contain a valid core query filter.");
      }
   }

   /*******************************************************************************
    ** Invalid operators and null criteria are never interpreted as empty filters.
    *******************************************************************************/
   private static void validateFilter(QQueryFilter filter)
   {
      if(filter == null || filter.getCriteria() == null || filter.getSubFilters() == null
         || filter.getCriteria().stream().anyMatch(c -> c == null || c.getOperator() == null || c.getFieldName() == null))
      {
         throw new IllegalArgumentException("Invalid filter.");
      }
      filter.getSubFilters().forEach(WatchRecords::validateFilter);
   }
}
