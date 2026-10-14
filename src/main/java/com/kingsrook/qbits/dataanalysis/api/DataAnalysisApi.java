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

import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dataanalysis.DataAnalysisRuntime;
import com.kingsrook.qbits.dataanalysis.DataAnalysisService;
import com.kingsrook.qbits.dataanalysis.engine.CoreAnalysisBackend;
import com.kingsrook.qbits.dataanalysis.result.AnalysisRequest;
import com.kingsrook.qbits.dataanalysis.result.ChartKind;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfileRequest;
import com.kingsrook.qbits.dataanalysis.result.DistinctValuesRequest;
import com.kingsrook.qbits.dataanalysis.result.DuplicatesRequest;
import com.kingsrook.qbits.dataanalysis.result.PivotRequest;
import com.kingsrook.qbits.dataanalysis.result.TableProfileRequest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.AbstractJsonEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtension;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtensionsMetaData;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;

/*******************************************************************************
 ** Session-bound profile operations registered only with optional Javalin present.
 *******************************************************************************/
public class DataAnalysisApi
{
   public static final String PREFIX = "/qbit/data-analysis";

   /*******************************************************************************
    ** Publish the extension through the core spec registry.
    *******************************************************************************/
   public static void register(QInstance instance)
   {
      MiddlewareSpecExtensionsMetaData.ofOrWithNew(instance).withExtension(extension());
   }

   /*******************************************************************************
    ** The same descriptor drives routes, OpenAPI and assistant tool discovery.
    *******************************************************************************/
   public static MiddlewareSpecExtension extension()
   {
      return (new MiddlewareSpecExtension().withName("data-analysis").withSemanticVersion("1.1.0")
         .withDescription("Profiles, pivots, distinct values, duplicate groups and exact analysis runs as the signed-in user.")
         .withSpec(CapabilitiesSpec.class).withSpec(ColumnProfileSpec.class).withSpec(DistinctValuesSpec.class)
         .withSpec(PivotSpec.class).withSpec(TableProfileSpec.class).withSpec(DuplicatesSpec.class).withSpec(StartRunSpec.class).withSpec(GetRunSpec.class));
   }

   /*******************************************************************************
    ** Common schema catalog and path parameters for the seven JSON specs.
    *******************************************************************************/
   public abstract static class ProfileSpec extends AbstractJsonEndpointSpec
   {
      private final String path;
      private final HttpMethod method;
      private final String summary;
      private final String description;
      private final String request;
      private final String response;

      /*******************************************************************************
       ** Bind immutable operation metadata.
       *******************************************************************************/
      protected ProfileSpec(String path, HttpMethod method, String summary, String description, String request, String response)
      {
         this.path = path;
         this.method = method;
         this.summary = summary;
         this.description = description;
         this.request = request;
         this.response = response;
      }

      /*******************************************************************************
       ** Assistant-facing operation purpose.
       *******************************************************************************/
      @Override
      public BasicOperation defineBasicOperation()
      {
         return (new BasicOperation().withPath(PREFIX + path).withHttpMethod(method).withShortSummary(summary).withLongDescription(description));
      }

      /*******************************************************************************
       ** Required path parameter, when present.
       *******************************************************************************/
      @Override
      public List<Parameter> defineRequestParameters()
      {
         String name = path.contains("{table}") ? "table" : path.contains("{runId}") ? "runId" : null;
         return (name == null ? List.of() : List.of(new Parameter().withName(name).withIn(In.PATH).withRequired(true)
            .withDescription(name.equals("table") ? "QQQ table name to analyze." : "Owner-scoped analysis run id.").withSchema(new Schema().withType(Type.STRING))));
      }

      /*******************************************************************************
       ** Versioned request schema.
       *******************************************************************************/
      @Override
      protected Schema defineRequestBodySchema()
      {
         return (request == null ? null : new Schema().withRef("#/components/schemas/DataAnalysis" + request + "V1"));
      }

      /*******************************************************************************
       ** Versioned response schema.
       *******************************************************************************/
      @Override
      protected String getResponseSchemaName()
      {
         return ("DataAnalysis" + response + "V1");
      }

      /*******************************************************************************
       ** Include all referenced transport models and the shared core filter.
       *******************************************************************************/
      @Override
      public Map<String, Schema> defineComponentSchemas()
      {
         return (this instanceof CapabilitiesSpec ? DataAnalysisSchemas.components() : Map.of());
      }
   }

   /*******************************************************************************
    ** Existing table lookup before invoking permission-sensitive operations.
    *******************************************************************************/
   private static QTableMetaData table(String name) throws QNotFoundException
   {
      QTableMetaData table = QContext.getQInstance().getTable(name);
      if(table == null)
      {
         throw new QNotFoundException("Unknown analysis table.");
      }
      return (table);
   }

   /*******************************************************************************
    ** Parse one JSON object with strict scalar/list types and core filter JSON.
    *******************************************************************************/
   private static AnalysisRequest request(String operation, String table, Map<String, Object> body) throws Exception
   {
      table(table);
      DataAnalysisService service = DataAnalysisRuntime.service();
      List<String> readable = service.capabilities(table).getFields();
      try
      {
         for(String name : List.of("fieldName", "search"))
         {
            if(body.get(name) != null && !(body.get(name) instanceof String))
            {
               throw new IllegalArgumentException(name + " must be a string.");
            }
         }
         if(body.get("exact") != null && !(body.get("exact") instanceof Boolean))
         {
            throw new IllegalArgumentException("exact must be a boolean.");
         }
         QQueryFilter filter = body.get("filter") == null ? null : JsonUtils.toObject(JsonUtils.toJson(body.get("filter")), QQueryFilter.class);
         validateFilter(filter);
         List<String> fields = null;
         if(body.get("fields") != null)
         {
            if(!(body.get("fields") instanceof List<?> values) || values.stream().anyMatch(v -> !(v instanceof String)))
            {
               throw new IllegalArgumentException("fields must be an array of field names.");
            }
            fields = ((List<?>) body.get("fields")).stream().map(String.class::cast).toList();
            if(fields.isEmpty() || !readable.containsAll(fields))
            {
               throw new IllegalArgumentException("Choose readable analysis fields.");
            }
         }
         String field = (String) body.get("fieldName");
         if((operation.equals("COLUMN_PROFILE") || operation.equals("DISTINCT_VALUES")) && !readable.contains(field))
         {
            throw new IllegalArgumentException("Choose a readable analysis field.");
         }
         boolean exact = Boolean.TRUE.equals(body.get("exact"));
         return (switch(operation)
         {
            case "COLUMN_PROFILE" -> new ColumnProfileRequest().withTableName(table).withFilter(filter).withFieldName(field).withExact(exact);
            case "TABLE_PROFILE" -> new TableProfileRequest().withTableName(table).withFilter(filter).withFields(fields);
            case "DISTINCT_VALUES" -> new DistinctValuesRequest().withTableName(table).withFilter(filter).withFieldName(field)
               .withPage(integer(body, "page", 0, 0, 1_000_000)).withPageSize(integer(body, "pageSize", 100, 1, 500)).withSearch((String) body.get("search"));
            case "DUPLICATES" -> new DuplicatesRequest().withTableName(table).withFilter(filter)
               .withFields(java.util.Objects.requireNonNull(fields, "Choose duplicate fields.")).withLimit(integer(body, "limit", 50, 1, 500));
            default -> throw new IllegalArgumentException("operation must be COLUMN_PROFILE or TABLE_PROFILE.");
         });
      }
      catch(Exception e)
      {
         throw new QBadRequestException("Invalid analysis request: " + e.getMessage());
      }
   }

   /*******************************************************************************
    ** Bad filter fields fail before any aggregate or cached result is consulted.
    *******************************************************************************/
   private static void validateFilter(QQueryFilter filter)
   {
      if(filter == null)
      {
         return;
      }
      for(var criterion : filter.getCriteria())
      {
         if(criterion.getOperator() == null || criterion.getFieldName() == null)
         {
            throw new IllegalArgumentException("Choose readable filter fields and a valid operator.");
         }
      }
      filter.getSubFilters().forEach(sub -> validateFilter(sub));
   }

   /*******************************************************************************
    ** Reject fractional, null, overflowing and out-of-range numeric parameters.
    *******************************************************************************/
   private static int integer(Map<String, Object> body, String name, int fallback, int min, int max)
   {
      if(!body.containsKey(name))
      {
         return (fallback);
      }
      Object value = body.get(name);
      if(!(value instanceof Number number) || number.doubleValue() != number.intValue() || number.intValue() < min || number.intValue() > max)
      {
         throw new IllegalArgumentException(name + " must be an integer from " + min + " to " + max + ".");
      }
      return (number.intValue());
   }

   /*******************************************************************************
    ** Transport conversion avoids middleware dependencies in engine results.
    *******************************************************************************/
   private static <T> T transport(Object value, Class<T> type) throws Exception
   {
      return (JsonUtils.toObject(JsonUtils.toJson(value), type));
   }

   /*******************************************************************************
    ** Check analysis availability.
    *******************************************************************************/
   public static class CapabilitiesSpec extends ProfileSpec
   {
      /*******************************************************************************
       ** Public constructor used by the core extension registry.
       *******************************************************************************/
      public CapabilitiesSpec()
      {
         super("/tables/{table}/capabilities", HttpMethod.GET, "Check analysis availability",
            "Discover whether the current user can analyze this table, readable fields, chart defaults, limits and backend capabilities.", null, "Capabilities");
      }

      /*******************************************************************************
       ** Execute in the request session established by core middleware.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (capabilities(input.getPathParam("table")));
      }
   }

   /*******************************************************************************
    ** Profile one column.
    *******************************************************************************/
   public static class ColumnProfileSpec extends ProfileSpec
   {
      /*******************************************************************************
       ** Public constructor used by the core extension registry.
       *******************************************************************************/
      public ColumnProfileSpec()
      {
         super("/tables/{table}/column-profile", HttpMethod.POST, "Profile one column",
            "Profile one column of a table under a filter: counts, nulls, distinct values, top values, numeric statistics and histogram, date patterns; every number has a drill-through filter unless privacy suppresses it.", "ColumnProfileRequest", "ColumnProfile");
      }

      /*******************************************************************************
       ** Execute in the request session established by core middleware.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (transport(DataAnalysisRuntime.service().columnProfile((ColumnProfileRequest) request("COLUMN_PROFILE", input.getPathParam("table"), input.getBody())), DataAnalysisSchemas.DataAnalysisColumnProfileV1.class));
      }
   }

   /*******************************************************************************
    ** List distinct column values.
    *******************************************************************************/
   public static class DistinctValuesSpec extends ProfileSpec
   {
      /*******************************************************************************
       ** Public constructor used by the core extension registry.
       *******************************************************************************/
      public DistinctValuesSpec()
      {
         super("/tables/{table}/distinct-values", HttpMethod.POST, "List distinct column values",
            "Page through distinct stored values with display labels, counts and drill-through filters; search narrows stored values.", "DistinctValuesRequest", "DistinctValuesPage");
      }

      /*******************************************************************************
       ** Execute in the request session established by core middleware.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (transport(DataAnalysisRuntime.service().distinctValues((DistinctValuesRequest) request("DISTINCT_VALUES", input.getPathParam("table"), input.getBody())), DataAnalysisSchemas.DataAnalysisDistinctValuesPageV1.class));
      }
   }

   /*******************************************************************************
    ** Profile table completeness and keys.
    *******************************************************************************/
   public static class TableProfileSpec extends ProfileSpec
   {
      /*******************************************************************************
       ** Public constructor used by the core extension registry.
       *******************************************************************************/
      public TableProfileSpec()
      {
         super("/tables/{table}/table-profile", HttpMethod.POST, "Profile table completeness and keys",
            "Profile readable fields under a filter for completeness, uniqueness, constant or empty columns and candidate keys.", "TableProfileRequest", "TableProfile");
      }

      /*******************************************************************************
       ** Execute in the request session established by core middleware.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (transport(DataAnalysisRuntime.service().tableProfile((TableProfileRequest) request("TABLE_PROFILE", input.getPathParam("table"), input.getBody())), DataAnalysisSchemas.DataAnalysisTableProfileV1.class));
      }
   }

   /*******************************************************************************
    ** Find duplicate groups.
    *******************************************************************************/
   public static class DuplicatesSpec extends ProfileSpec
   {
      /*******************************************************************************
       ** Public constructor used by the core extension registry.
       *******************************************************************************/
      public DuplicatesSpec()
      {
         super("/tables/{table}/duplicates", HttpMethod.POST, "Find duplicate groups",
            "Group matching records by the selected readable fields and return repeated tuples with counts and drill-through filters.", "DuplicatesRequest", "DuplicatesResult");
      }

      /*******************************************************************************
       ** Execute in the request session established by core middleware.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (transport(DataAnalysisRuntime.service().duplicates((DuplicatesRequest) request("DUPLICATES", input.getPathParam("table"), input.getBody())), DataAnalysisSchemas.DataAnalysisDuplicatesResultV1.class));
      }
   }

   /*******************************************************************************
    ** Start an exact profile run.
    *******************************************************************************/
   public static class StartRunSpec extends ProfileSpec
   {
      /*******************************************************************************
       ** Public constructor used by the core extension registry.
       *******************************************************************************/
      public StartRunSpec()
      {
         super("/runs", HttpMethod.POST, "Start an exact profile run",
            "Start an owner-scoped exact column or table profile. Poll the returned runId until COMPLETE or FAILED; results honor the current session permissions.", "StartRunRequest", "RunStarted");
      }

      /*******************************************************************************
       ** Execute in the request session established by core middleware.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         Map<String, Object> body = input.getBody();
         if(body == null || !(body.get("tableName") instanceof String name) || !(body.get("operation") instanceof String operation)
            || !List.of("COLUMN_PROFILE", "TABLE_PROFILE").contains(operation) || !(body.get("request") instanceof Map<?, ?>))
         {
            throw new QBadRequestException("Supply operation, tableName and a profile request object.");
         }
         Map<String, Object> parameters = JsonUtils.toObject(JsonUtils.toJson(body.get("request")), java.util.LinkedHashMap.class);
         String id = DataAnalysisRuntime.service().startExactRun(request(operation, name, parameters));
         DataAnalysisSchemas.DataAnalysisRunStartedV1 result = new DataAnalysisSchemas.DataAnalysisRunStartedV1();
         result.runId = id;
         result.status = DataAnalysisRuntime.service().getRun(id).getStatus();
         return (result);
      }
   }

   /*******************************************************************************
    ** Get an exact profile run.
    *******************************************************************************/
   public static class GetRunSpec extends ProfileSpec
   {
      /*******************************************************************************
       ** Public constructor used by the core extension registry.
       *******************************************************************************/
      public GetRunSpec()
      {
         super("/runs/{runId}", HttpMethod.GET, "Get an exact profile run",
            "Read status, timestamps and the typed columnProfile or tableProfile result of an owner-scoped run. Completed results are rechecked against current permissions.", null, "RunStatus");
      }

      /*******************************************************************************
       ** Execute in the request session established by core middleware.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (transport(DataAnalysisRuntime.service().getRun(input.getPathParam("runId")), DataAnalysisSchemas.DataAnalysisRunStatusV1.class));
      }
   }

   /*******************************************************************************
    ** Permission probing is a successful response with no disclosed field list.
    *******************************************************************************/
   private static DataAnalysisSchemas.DataAnalysisCapabilitiesV1 capabilities(String name) throws Exception
   {
      QTableMetaData table = table(name);
      DataAnalysisSchemas.DataAnalysisCapabilitiesV1 result = new DataAnalysisSchemas.DataAnalysisCapabilitiesV1();
      result.tableName = name;
      result.tableLabel = table.getLabel();
      result.canAnalyze = false;
      result.fields = List.of();
      result.backendCapabilities = List.of();
      result.sqlWorkbenchAvailable = QContext.getQInstance().getApp("sqlWorkbench") != null
         || QContext.getQInstance().getTables().keySet().stream().anyMatch(t -> t.startsWith("sql_workbench_"));
      try
      {
         var capabilities = DataAnalysisRuntime.service().capabilities(name);
         result.canAnalyze = true;
         result.backendCapabilities = CoreAnalysisBackend.capabilities(name).stream().map(Enum::name).sorted().toList();
         result.fields = capabilities.getFields().stream().map(f -> capabilityField(table.getField(f))).toList();
         var config = capabilities.getConfig();
         result.limits = new DataAnalysisSchemas.DataAnalysisLimitsV1();
         result.limits.maxRows = config.getMaxRows();
         result.limits.sampleThresholdRows = config.getSampleThresholdRows();
         result.limits.sampleRows = config.getSampleRows();
         result.limits.minGroupSize = config.getMinGroupSize();
      }
      catch(QPermissionDeniedException expected)
      {
         // A UI capability probe deliberately reports denial without an error.
      }
      return (result);
   }

   /*******************************************************************************
    ** Semantic type and the default visualization, with PVS/FK taking priority.
    *******************************************************************************/
   private static DataAnalysisSchemas.DataAnalysisCapabilityFieldV1 capabilityField(QFieldMetaData field)
   {
      DataAnalysisSchemas.DataAnalysisCapabilityFieldV1 result = new DataAnalysisSchemas.DataAnalysisCapabilityFieldV1();
      result.name = field.getName();
      result.label = field.getLabel();
      result.type = field.getType().name();
      result.kind = field.getPossibleValueSourceName() != null ? "CATEGORY" : switch(field.getType())
      {
         case INTEGER, LONG, DECIMAL -> "NUMERIC";
         case DATE -> "DATE";
         case DATE_TIME -> "DATE_TIME";
         case BOOLEAN -> "BOOLEAN";
         default -> "TEXT";
      };
      result.defaultChartKind = switch(result.kind)
      {
         case "NUMERIC" -> ChartKind.HISTOGRAM;
         case "DATE", "DATE_TIME" -> ChartKind.TIME_SERIES;
         default -> ChartKind.BAR;
      };
      return (result);
   }

   /*******************************************************************************
    ** Group records by row and column dimensions.
    *******************************************************************************/
   public static class PivotSpec extends ProfileSpec
   {
      /*******************************************************************************
       ** Publish assistant-friendly grouping and measure capabilities.
       *******************************************************************************/
      public PivotSpec()
      {
         super("/tables/{table}/pivot", HttpMethod.POST, "Explore a pivot table",
            "Group records by up to 3 row and 2 column fields (with date or numeric buckets) and compute counts, sums, averages, min/max, distinct counts or percentiles per cell; every cell has a drill-through filter.", "PivotRequest", "Pivot");
      }

      /*******************************************************************************
       ** Strict body validation precedes the session-bound engine.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         String tableName = input.getPathParam("table");
         table(tableName);
         DataAnalysisRuntime.service().capabilities(tableName);
         PivotRequest pivot;
         try
         {
            Map<String, Object> body = input.getBody();
            integer(body, "limitGroups", 50, 1, 200);
            if(body.containsKey("showTotals") && !(body.get("showTotals") instanceof Boolean))
            {
               throw new IllegalArgumentException("showTotals must be a boolean.");
            }
            for(String axis : List.of("rows", "columns"))
            {
               if(body.containsKey(axis))
               {
                  if(!(body.get(axis) instanceof List<?> dimensions))
                  {
                     throw new IllegalArgumentException("Dimensions must be arrays.");
                  }
                  for(Object dimension : dimensions)
                  {
                     if(!(dimension instanceof Map<?, ?> map) || !(map.get("field") instanceof String))
                     {
                        throw new IllegalArgumentException("Each dimension requires a field name.");
                     }
                     if(map.get("numericBins") instanceof Map<?, ?> bins)
                     {
                        Object count = bins.get("count");
                        if(count != null && (!(count instanceof Number number) || number.doubleValue() != number.intValue()))
                        {
                           throw new IllegalArgumentException("Bin count must be an integer.");
                        }
                     }
                  }
               }
            }
            if(!(body.get("values") instanceof List<?> values) || values.stream().anyMatch(v -> !(v instanceof Map<?, ?> m)
               || !(m.get("operator") instanceof String) || m.get("field") != null && !(m.get("field") instanceof String)))
            {
               throw new IllegalArgumentException("Measures require an operator and optional field name.");
            }
            pivot = JsonUtils.toObject(JsonUtils.toJson(body), PivotRequest.class).withTableName(tableName);
            validateFilter(pivot.getFilter());
         }
         catch(Exception e)
         {
            throw new QBadRequestException("Invalid pivot request.");
         }
         return (transport(DataAnalysisRuntime.service().pivot(pivot), DataAnalysisSchemas.DataAnalysisPivotV1.class));
      }
   }
}
