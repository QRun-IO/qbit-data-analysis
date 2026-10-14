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

package com.kingsrook.qbits.dataanalysis.saved;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qbits.dataanalysis.DataAnalysisService;
import com.kingsrook.qbits.dataanalysis.result.AnalysisRequest;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfileRequest;
import com.kingsrook.qbits.dataanalysis.result.CompareRequest;
import com.kingsrook.qbits.dataanalysis.result.CorrelationRequest;
import com.kingsrook.qbits.dataanalysis.result.CrosstabRequest;
import com.kingsrook.qbits.dataanalysis.result.PivotRequest;
import com.kingsrook.qbits.dataanalysis.result.ShiftRequest;
import com.kingsrook.qbits.dataanalysis.result.TableProfileRequest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;

/*******************************************************************************
 ** One parser for persisted definitions, exports, schedules and SQL requests.
 *******************************************************************************/
public record SavedDefinition(String kind, AnalysisRequest request)
{
   private static final Map<String, Class<? extends AnalysisRequest>> TYPES = Map.of(
      "COLUMN_PROFILE", ColumnProfileRequest.class, "TABLE_PROFILE", TableProfileRequest.class,
      "PIVOT", PivotRequest.class, "COMPARE", CompareRequest.class, "CORRELATION", CorrelationRequest.class,
      "CROSSTAB", CrosstabRequest.class, "SHIFT", ShiftRequest.class);
   private static final Set<String> FIELD_KEYS = Set.of("fieldName", "field", "dateField", "rowField", "columnField");
   public static final String FIELD_WARNING = "Some fields are not available to you";

   /*******************************************************************************
    ** The definition owns the typed filter; legacy filter_json remains supported.
    *******************************************************************************/
   public static SavedDefinition parse(QRecord row) throws QException
   {
      Map<String, Object> body = object(row.getValueString("definition_json"));
      if(body.get("filter") == null && row.getValueString("filter_json") != null)
      {
         body.put("filter", object(row.getValueString("filter_json")));
      }
      return (parse(row.getValueString("kind"), row.getValueString("table_name"), body));
   }

   /*******************************************************************************
    ** Reject malformed JSON with a plain, stable message.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public static Map<String, Object> object(String json) throws QBadRequestException
   {
      try
      {
         Object value = JsonUtils.toObject(json, Object.class);
         if(value instanceof Map<?, ?> map)
         {
            return (new LinkedHashMap<>((Map<String, Object>) map));
         }
      }
      catch(Exception expected)
      {
         // Normalize parser diagnostics; never return raw saved JSON in errors.
      }
      throw new QBadRequestException("definition_json must be a JSON object.");
   }

   /*******************************************************************************
    ** Validate metadata existence before saving, without querying source records.
    *******************************************************************************/
   public static SavedDefinition parse(String kind, String table, Map<String, Object> body) throws QException
   {
      if(!TYPES.containsKey(kind == null ? "" : kind))
      {
         throw new QBadRequestException("Choose a supported analysis kind.");
      }
      if(table == null || QContext.getQInstance().getTable(table) == null)
      {
         throw new QBadRequestException("Choose an existing analysis table.");
      }
      if(body.get("tableName") != null && !table.equals(body.get("tableName")))
      {
         throw new QBadRequestException("The definition table must match table_name.");
      }
      Map<String, Object> copy = new LinkedHashMap<>(body);
      copy.put("tableName", table);
      try
      {
         validateJson(copy);
         AnalysisRequest request = JsonUtils.toObject(JsonUtils.toJson(copy), TYPES.get(kind));
         requireShape(request);
         for(String field : fields(copy))
         {
            String[] parts = field.split("\\.", 2);
            var metadata = QContext.getQInstance().getTable(parts.length == 2 ? parts[0] : table);
            if(metadata == null || metadata.getField(parts.length == 2 ? parts[1] : field) == null)
            {
               throw new IllegalArgumentException("A definition field does not exist.");
            }
         }
         return (new SavedDefinition(kind, request));
      }
      catch(Exception e)
      {
         throw new QBadRequestException("Invalid analysis definition: " + (e instanceof IllegalArgumentException ? e.getMessage() : "check the request fields and types."));
      }
   }

   /*******************************************************************************
    ** Reject coercible scalar types and malformed predicates before Jackson conversion.
    *******************************************************************************/
   private static void validateJson(Object value)
   {
      if(value instanceof Map<?, ?> map)
      {
         for(var entry : map.entrySet())
         {
            String name = String.valueOf(entry.getKey());
            Object item = entry.getValue();
            if(item == null)
            {
               continue;
            }
            if((FIELD_KEYS.contains(name) || Set.of("tableName", "bucket", "operator").contains(name)) && !(item instanceof String))
            {
               throw new IllegalArgumentException("Field names and operators must be strings.");
            }
            if(name.equals("fields") && (!(item instanceof List<?> list) || list.stream().anyMatch(v -> !(v instanceof String))))
            {
               throw new IllegalArgumentException("fields must be an array of field names.");
            }
            if(Set.of("filter", "filterA", "filterB").contains(name) && !(item instanceof Map<?, ?>))
            {
               throw new IllegalArgumentException("Filters must be query filter objects.");
            }
            if(name.equals("criteria") && (!(item instanceof List<?> list) || list.stream().anyMatch(v -> !(v instanceof Map<?, ?> c)
               || !(c.get("fieldName") instanceof String) || !(c.get("operator") instanceof String))))
            {
               throw new IllegalArgumentException("Filter criteria require a field and operator.");
            }
            if(Set.of("exact", "showTotals").contains(name) && !(item instanceof Boolean))
            {
               throw new IllegalArgumentException("Flags must be booleans.");
            }
            validateJson(item);
         }
      }
      else if(value instanceof List<?> list)
      {
         list.forEach(SavedDefinition::validateJson);
      }
   }

   /*******************************************************************************
    ** Required selections must not turn an invalid draft into a different query.
    *******************************************************************************/
   private static void requireShape(AnalysisRequest request)
   {
      boolean valid = switch(request)
      {
         case ColumnProfileRequest r -> r.getFieldName() != null;
         case TableProfileRequest r -> r.getFields() == null || !r.getFields().isEmpty();
         case CompareRequest r -> r.getFields() != null && !r.getFields().isEmpty();
         case CorrelationRequest r -> r.getFields() != null && r.getFields().size() >= 2;
         case CrosstabRequest r -> r.getRowField() != null && r.getColumnField() != null;
         case ShiftRequest r -> r.getField() != null && r.getDateField() != null;
         case PivotRequest r -> r.getValues() != null && !r.getValues().isEmpty()
            && r.getValues().stream().allMatch(v -> v.getOperator() != null && (v.getOperator().name().equals("COUNT") || v.getField() != null));
         default -> false;
      };
      if(!valid)
      {
         throw new IllegalArgumentException("Choose the fields and measures required by this analysis kind.");
      }
   }

   /*******************************************************************************
    ** Include selected and filter fields, including both compare populations.
    *******************************************************************************/
   public static Set<String> fields(Object value)
   {
      Set<String> names = new LinkedHashSet<>();
      if(value instanceof Map<?, ?> map)
      {
         for(var entry : map.entrySet())
         {
            if(FIELD_KEYS.contains(entry.getKey()) && entry.getValue() instanceof String name)
            {
               names.add(name);
            }
            else if("fields".equals(entry.getKey()) && entry.getValue() instanceof List<?> list)
            {
               list.forEach(name -> names.add((String) name));
            }
            else
            {
               names.addAll(fields(entry.getValue()));
            }
         }
      }
      else if(value instanceof List<?> list)
      {
         list.forEach(item -> names.addAll(fields(item)));
      }
      return (names);
   }

   /*******************************************************************************
    ** A fresh tree makes field reduction request-local and cannot change storage.
    *******************************************************************************/
   public Map<String, Object> body() throws QException
   {
      return (object(JsonUtils.toJson(request)));
   }

   /*******************************************************************************
    ** Filter rights are not weakened: an unavailable predicate prevents execution.
    ** Required denied axes return an empty typed envelope rather than change meaning.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public SavedResult run(DataAnalysisService service, boolean strict) throws QException
   {
      service.capabilities(request.getTableName());
      Map<String, Object> body = body();
      Set<String> denied = new LinkedHashSet<>();
      for(String field : fields(body))
      {
         if(!service.isAvailableField(request.getTableName(), field))
         {
            denied.add(field);
         }
      }
      if(strict && !denied.isEmpty())
      {
         throw new QBadRequestException(FIELD_WARNING);
      }
      List<String> warnings = denied.isEmpty() ? List.of() : List.of(FIELD_WARNING);
      for(String filter : List.of("filter", "filterA", "filterB"))
      {
         if(fields(body.get(filter)).stream().anyMatch(denied::contains))
         {
            return (SavedResult.of(kind, null, warnings));
         }
      }
      if(body.get("fields") instanceof List<?> list)
      {
         body.put("fields", list.stream().filter(f -> !denied.contains(f)).toList());
      }
      for(String axis : List.of("rows", "columns", "values"))
      {
         if(body.get(axis) instanceof List<?> list)
         {
            body.put(axis, list.stream().filter(item -> fields(item).stream().noneMatch(denied::contains)).toList());
         }
      }
      if(FIELD_KEYS.stream().anyMatch(key -> denied.contains(body.get(key))))
      {
         return (SavedResult.of(kind, null, warnings));
      }
      AnalysisRequest reduced;
      try
      {
         reduced = JsonUtils.toObject(JsonUtils.toJson(body), TYPES.get(kind));
         requireShape(reduced);
      }
      catch(Exception e)
      {
         return (SavedResult.of(kind, null, warnings));
      }
      Object result = switch(reduced)
      {
         case ColumnProfileRequest r ->
         {
            var backend = service.backend(r.getTableName());
            yield service.compute(r, backend, backend.scope(r.getTableName(), r.getFilter(), Boolean.TRUE.equals(r.getExact())));
         }
         case TableProfileRequest r ->
         {
            var backend = service.backend(r.getTableName());
            yield service.compute(r, backend, backend.scope(r.getTableName(), r.getFilter(), Boolean.TRUE.equals(r.getExact())));
         }
         case PivotRequest r -> service.pivot(r);
         case CompareRequest r -> service.compare(r);
         case CorrelationRequest r -> service.correlation(r);
         case CrosstabRequest r -> service.crosstab(r);
         case ShiftRequest r -> service.shift(r);
         default -> throw new QBadRequestException("Unsupported analysis kind.");
      };
      return (SavedResult.of(kind, result, warnings));
   }
}
