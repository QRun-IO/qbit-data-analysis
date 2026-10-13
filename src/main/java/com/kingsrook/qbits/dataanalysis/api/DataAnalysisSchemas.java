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
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dataanalysis.result.ChartKind;
import com.kingsrook.qbits.dataanalysis.result.ValueType;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.QueryFilter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;

/*******************************************************************************
 ** Versioned transport models isolated from the optional middleware-free engine.
 *******************************************************************************/
public class DataAnalysisSchemas
{
   /*******************************************************************************
    ** Core's Schema model lacks the standard OpenAPI required property.
    *******************************************************************************/
   public static class RequiredSchema extends Schema
   {
      private final List<String> required;

      /*******************************************************************************
       ** Declare the fields the endpoint requires at runtime.
       *******************************************************************************/
      public RequiredSchema(List<String> required)
      {
         this.required = required;
      }

      /*******************************************************************************
       ** Standard JSON Schema required property serialized by core's generator.
       *******************************************************************************/
      public List<String> getRequired()
      {
         return (required);
      }
   }

   /*******************************************************************************
    ** Shared component reference, including the core QueryFilter contract.
    *******************************************************************************/
   private static Schema ref(String name)
   {
      return (new Schema().withRef("#/components/schemas/" + name));
   }

   /*******************************************************************************
    ** Complete standalone extension component catalog.
    *******************************************************************************/
   public static Map<String, Schema> components()
   {
      Map<String, Schema> result = new LinkedHashMap<>();
      result.put("QueryFilter", new QueryFilter().toSchema());
      result.put("FilterCriteria", new com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.FilterCriteria().toSchema());
      result.put("OrderBy", new com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.OrderBy().toSchema());
      result.put("DataAnalysisAxisV1", new DataAnalysisAxisV1().toSchema());
      result.put("DataAnalysisCandidateKeyV1", new DataAnalysisCandidateKeyV1().toSchema());
      result.put("DataAnalysisChartV1", new DataAnalysisChartV1().toSchema());
      result.put("DataAnalysisColumnProfileV1", new DataAnalysisColumnProfileV1().toSchema());
      result.put("DataAnalysisColumnProfileRequestV1", new DataAnalysisColumnProfileRequestV1().toSchema());
      result.put("DataAnalysisDateProfileV1", new DataAnalysisDateProfileV1().toSchema());
      result.put("DataAnalysisDistinctValuesPageV1", new DataAnalysisDistinctValuesPageV1().toSchema());
      result.put("DataAnalysisDistinctValuesRequestV1", new DataAnalysisDistinctValuesRequestV1().toSchema());
      result.put("DataAnalysisDrillV1", new DataAnalysisDrillV1().toSchema());
      result.put("DataAnalysisDuplicatesRequestV1", new DataAnalysisDuplicatesRequestV1().toSchema());
      result.put("DataAnalysisDuplicatesResultV1", new DataAnalysisDuplicatesResultV1().toSchema());
      result.put("DataAnalysisFieldSummaryV1", new DataAnalysisFieldSummaryV1().toSchema());
      result.put("DataAnalysisMetricV1", new DataAnalysisMetricV1().toSchema());
      result.put("DataAnalysisNumericProfileV1", new DataAnalysisNumericProfileV1().toSchema());
      result.put("DataAnalysisPointV1", new DataAnalysisPointV1().toSchema());
      result.put("DataAnalysisRunStatusV1", new DataAnalysisRunStatusV1().toSchema());
      result.put("DataAnalysisSeriesV1", new DataAnalysisSeriesV1().toSchema());
      result.put("DataAnalysisTableProfileV1", new DataAnalysisTableProfileV1().toSchema());
      result.put("DataAnalysisTableProfileRequestV1", new DataAnalysisTableProfileRequestV1().toSchema());
      result.put("DataAnalysisTextProfileV1", new DataAnalysisTextProfileV1().toSchema());
      result.put("DataAnalysisValueCountV1", new DataAnalysisValueCountV1().toSchema());
      result.put("DataAnalysisCapabilitiesV1", new DataAnalysisCapabilitiesV1().toSchema());
      result.put("DataAnalysisCapabilityFieldV1", new DataAnalysisCapabilityFieldV1().toSchema());
      result.put("DataAnalysisLimitsV1", new DataAnalysisLimitsV1().toSchema());
      result.put("DataAnalysisStartRunRequestV1", new DataAnalysisStartRunRequestV1().toSchema());
      result.put("DataAnalysisRunStartedV1", new DataAnalysisRunStartedV1().toSchema());
      return (result);
   }

   /*******************************************************************************
    ** Axis version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisAxisV1 implements ToSchema
   {
      @OpenAPIDescription("Field.")
      public String field;

      @OpenAPIDescription("Label.")
      public String label;

      @OpenAPIDescription("Value type.")
      public ValueType valueType;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("field", new Schema().withType(Type.STRING).withDescription("Field.").withNullable(true));
         properties.put("label", new Schema().withType(Type.STRING).withDescription("Label.").withNullable(true));
         properties.put("valueType", new Schema().withType(Type.STRING).withEnumValues(java.util.Arrays.stream(ValueType.values()).map(Enum::name).toList()).withDescription("Value type.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** CandidateKey version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisCandidateKeyV1 implements ToSchema
   {
      @OpenAPIDescription("Readable fields to include; omit for all analyzable fields on a table profile.")
      public List<String> fields;

      @OpenAPIDescription("Row count.")
      public Long rowCount;

      @OpenAPIDescription("Open the matching records using this table and QueryFilter; null when suppressed.")
      public DataAnalysisDrillV1 drill;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("fields", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Readable fields to include; omit for all analyzable fields on a table profile.").withNullable(true));
         properties.put("rowCount", new Schema().withType(Type.INTEGER).withDescription("Row count.").withNullable(true));
         properties.put("drill", ref("DataAnalysisDrillV1").withDescription("Open the matching records using this table and QueryFilter; null when suppressed.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** Chart version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisChartV1 implements ToSchema
   {
      @OpenAPIDescription("Semantic field kind: NUMERIC, DATE, DATE_TIME, TEXT, BOOLEAN or CATEGORY.")
      public ChartKind kind;

      @OpenAPIDescription("Alternatives.")
      public List<ChartKind> alternatives;

      @OpenAPIDescription("X.")
      public DataAnalysisAxisV1 x;

      @OpenAPIDescription("Y.")
      public DataAnalysisAxisV1 y;

      @OpenAPIDescription("Series.")
      public List<DataAnalysisSeriesV1> series;

      @OpenAPIDescription("Annotations.")
      public List<String> annotations;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("kind", new Schema().withType(Type.STRING).withEnumValues(java.util.Arrays.stream(ChartKind.values()).map(Enum::name).toList()).withDescription("Semantic field kind: NUMERIC, DATE, DATE_TIME, TEXT, BOOLEAN or CATEGORY.").withNullable(true));
         properties.put("alternatives", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING).withEnumValues(java.util.Arrays.stream(ChartKind.values()).map(Enum::name).toList())).withDescription("Alternatives.").withNullable(true));
         properties.put("x", ref("DataAnalysisAxisV1").withDescription("X.").withNullable(true));
         properties.put("y", ref("DataAnalysisAxisV1").withDescription("Y.").withNullable(true));
         properties.put("series", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisSeriesV1")).withDescription("Series.").withNullable(true));
         properties.put("annotations", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Annotations.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** ColumnProfile version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisColumnProfileV1 implements ToSchema
   {
      @OpenAPIDescription("Table name.")
      public String tableName;

      @OpenAPIDescription("Core QueryFilter JSON, identical to the table query and count endpoints.")
      public QQueryFilter filter;

      @OpenAPIDescription("True when statistics describe a sample rather than the entire filtered population.")
      public Boolean estimate;

      @OpenAPIDescription("Actual number of rows in the sample, not an extrapolated count.")
      public Long sampleRows;

      @OpenAPIDescription("True when the row limit prevented a complete scan.")
      public Boolean truncated;

      @OpenAPIDescription("Number of source rows included in computation.")
      public Long rowsScanned;

      @OpenAPIDescription("As of.")
      public String asOf;

      @OpenAPIDescription("Data version.")
      public String dataVersion;

      @OpenAPIDescription("Warnings.")
      public List<String> warnings;

      @OpenAPIDescription("Readable field name to profile or enumerate.")
      public String fieldName;

      @OpenAPIDescription("Field label.")
      public String fieldLabel;

      @OpenAPIDescription("Field type.")
      public String fieldType;

      @OpenAPIDescription("Counts.")
      public List<DataAnalysisMetricV1> counts;

      @OpenAPIDescription("Numeric.")
      public DataAnalysisNumericProfileV1 numeric;

      @OpenAPIDescription("Dates.")
      public DataAnalysisDateProfileV1 dates;

      @OpenAPIDescription("Text.")
      public DataAnalysisTextProfileV1 text;

      @OpenAPIDescription("Booleans.")
      public List<DataAnalysisMetricV1> booleans;

      @OpenAPIDescription("Top values.")
      public List<DataAnalysisValueCountV1> topValues;

      @OpenAPIDescription("Chart.")
      public DataAnalysisChartV1 chart;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("tableName", new Schema().withType(Type.STRING).withDescription("Table name.").withNullable(true));
         properties.put("filter", ref("QueryFilter").withDescription("Core QueryFilter JSON, identical to the table query and count endpoints.").withNullable(true));
         properties.put("estimate", new Schema().withType(Type.BOOLEAN).withDescription("True when statistics describe a sample rather than the entire filtered population.").withNullable(true));
         properties.put("sampleRows", new Schema().withType(Type.INTEGER).withDescription("Actual number of rows in the sample, not an extrapolated count.").withNullable(true));
         properties.put("truncated", new Schema().withType(Type.BOOLEAN).withDescription("True when the row limit prevented a complete scan.").withNullable(true));
         properties.put("rowsScanned", new Schema().withType(Type.INTEGER).withDescription("Number of source rows included in computation.").withNullable(true));
         properties.put("asOf", new Schema().withType(Type.STRING).withFormat("date-time").withDescription("As of.").withNullable(true));
         properties.put("dataVersion", new Schema().withType(Type.STRING).withDescription("Data version.").withNullable(true));
         properties.put("warnings", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Warnings.").withNullable(true));
         properties.put("fieldName", new Schema().withType(Type.STRING).withDescription("Readable field name to profile or enumerate.").withNullable(true));
         properties.put("fieldLabel", new Schema().withType(Type.STRING).withDescription("Field label.").withNullable(true));
         properties.put("fieldType", new Schema().withType(Type.STRING).withDescription("Field type.").withNullable(true));
         properties.put("counts", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisMetricV1")).withDescription("Counts.").withNullable(true));
         properties.put("numeric", ref("DataAnalysisNumericProfileV1").withDescription("Numeric.").withNullable(true));
         properties.put("dates", ref("DataAnalysisDateProfileV1").withDescription("Dates.").withNullable(true));
         properties.put("text", ref("DataAnalysisTextProfileV1").withDescription("Text.").withNullable(true));
         properties.put("booleans", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisMetricV1")).withDescription("Booleans.").withNullable(true));
         properties.put("topValues", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisValueCountV1")).withDescription("Top values.").withNullable(true));
         properties.put("chart", ref("DataAnalysisChartV1").withDescription("Chart.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** ColumnProfileRequest version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisColumnProfileRequestV1 implements ToSchema
   {
      @OpenAPIDescription("Core QueryFilter JSON, identical to the table query and count endpoints.")
      public QQueryFilter filter;

      @OpenAPIDescription("Request exact computation instead of the configured estimate.")
      public Boolean exact;

      @OpenAPIDescription("Readable field name to profile or enumerate.")
      public String fieldName;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("filter", ref("QueryFilter").withDescription("Core QueryFilter JSON, identical to the table query and count endpoints."));
         properties.put("exact", new Schema().withType(Type.BOOLEAN).withDescription("Request exact computation instead of the configured estimate."));
         properties.put("fieldName", new Schema().withType(Type.STRING).withDescription("Readable field name to profile or enumerate."));
         return (new RequiredSchema(List.of("fieldName")).withType(Type.OBJECT).withProperties(properties).withAdditionalProperties(false));
      }
   }

   /*******************************************************************************
    ** DateProfile version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisDateProfileV1 implements ToSchema
   {
      @OpenAPIDescription("Metrics.")
      public List<DataAnalysisMetricV1> metrics;

      @OpenAPIDescription("Unit.")
      public String unit;

      @OpenAPIDescription("Buckets.")
      public List<DataAnalysisValueCountV1> buckets;

      @OpenAPIDescription("Gaps.")
      public List<DataAnalysisValueCountV1> gaps;

      @OpenAPIDescription("Day of week.")
      public List<DataAnalysisValueCountV1> dayOfWeek;

      @OpenAPIDescription("Hour of day.")
      public List<DataAnalysisValueCountV1> hourOfDay;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("metrics", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisMetricV1")).withDescription("Metrics.").withNullable(true));
         properties.put("unit", new Schema().withType(Type.STRING).withDescription("Unit.").withNullable(true));
         properties.put("buckets", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisValueCountV1")).withDescription("Buckets.").withNullable(true));
         properties.put("gaps", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisValueCountV1")).withDescription("Gaps.").withNullable(true));
         properties.put("dayOfWeek", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisValueCountV1")).withDescription("Day of week.").withNullable(true));
         properties.put("hourOfDay", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisValueCountV1")).withDescription("Hour of day.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** DistinctValuesPage version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisDistinctValuesPageV1 implements ToSchema
   {
      @OpenAPIDescription("Table name.")
      public String tableName;

      @OpenAPIDescription("Core QueryFilter JSON, identical to the table query and count endpoints.")
      public QQueryFilter filter;

      @OpenAPIDescription("True when statistics describe a sample rather than the entire filtered population.")
      public Boolean estimate;

      @OpenAPIDescription("Actual number of rows in the sample, not an extrapolated count.")
      public Long sampleRows;

      @OpenAPIDescription("True when the row limit prevented a complete scan.")
      public Boolean truncated;

      @OpenAPIDescription("Number of source rows included in computation.")
      public Long rowsScanned;

      @OpenAPIDescription("As of.")
      public String asOf;

      @OpenAPIDescription("Data version.")
      public String dataVersion;

      @OpenAPIDescription("Warnings.")
      public List<String> warnings;

      @OpenAPIDescription("Readable field name to profile or enumerate.")
      public String fieldName;

      @OpenAPIDescription("Zero-based page number.")
      public Integer page;

      @OpenAPIDescription("Maximum values per page, from 1 to 500.")
      public Integer pageSize;

      @OpenAPIDescription("Has more.")
      public Boolean hasMore;

      @OpenAPIDescription("Total distinct.")
      public Long totalDistinct;

      @OpenAPIDescription("Values.")
      public List<DataAnalysisValueCountV1> values;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("tableName", new Schema().withType(Type.STRING).withDescription("Table name.").withNullable(true));
         properties.put("filter", ref("QueryFilter").withDescription("Core QueryFilter JSON, identical to the table query and count endpoints.").withNullable(true));
         properties.put("estimate", new Schema().withType(Type.BOOLEAN).withDescription("True when statistics describe a sample rather than the entire filtered population.").withNullable(true));
         properties.put("sampleRows", new Schema().withType(Type.INTEGER).withDescription("Actual number of rows in the sample, not an extrapolated count.").withNullable(true));
         properties.put("truncated", new Schema().withType(Type.BOOLEAN).withDescription("True when the row limit prevented a complete scan.").withNullable(true));
         properties.put("rowsScanned", new Schema().withType(Type.INTEGER).withDescription("Number of source rows included in computation.").withNullable(true));
         properties.put("asOf", new Schema().withType(Type.STRING).withFormat("date-time").withDescription("As of.").withNullable(true));
         properties.put("dataVersion", new Schema().withType(Type.STRING).withDescription("Data version.").withNullable(true));
         properties.put("warnings", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Warnings.").withNullable(true));
         properties.put("fieldName", new Schema().withType(Type.STRING).withDescription("Readable field name to profile or enumerate.").withNullable(true));
         properties.put("page", new Schema().withType(Type.INTEGER).withDescription("Zero-based page number.").withNullable(true));
         properties.put("pageSize", new Schema().withType(Type.INTEGER).withDescription("Maximum values per page, from 1 to 500.").withNullable(true));
         properties.put("hasMore", new Schema().withType(Type.BOOLEAN).withDescription("Has more.").withNullable(true));
         properties.put("totalDistinct", new Schema().withType(Type.INTEGER).withDescription("Total distinct.").withNullable(true));
         properties.put("values", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisValueCountV1")).withDescription("Values.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** DistinctValuesRequest version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisDistinctValuesRequestV1 implements ToSchema
   {
      @OpenAPIDescription("Core QueryFilter JSON, identical to the table query and count endpoints.")
      public QQueryFilter filter;

      @OpenAPIDescription("Readable field name to profile or enumerate.")
      public String fieldName;

      @OpenAPIDescription("Zero-based page number.")
      public Integer page;

      @OpenAPIDescription("Maximum values per page, from 1 to 500.")
      public Integer pageSize;

      @OpenAPIDescription("Optional search text applied to stored values.")
      public String search;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("filter", ref("QueryFilter").withDescription("Core QueryFilter JSON, identical to the table query and count endpoints."));
         properties.put("fieldName", new Schema().withType(Type.STRING).withDescription("Readable field name to profile or enumerate."));
         properties.put("page", new Schema().withType(Type.INTEGER).withDescription("Zero-based page number."));
         properties.put("pageSize", new Schema().withType(Type.INTEGER).withDescription("Maximum values per page, from 1 to 500."));
         properties.put("search", new Schema().withType(Type.STRING).withDescription("Optional search text applied to stored values."));
         return (new RequiredSchema(List.of("fieldName")).withType(Type.OBJECT).withProperties(properties).withAdditionalProperties(false));
      }
   }

   /*******************************************************************************
    ** Drill version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisDrillV1 implements ToSchema
   {
      @OpenAPIDescription("Table name.")
      public String tableName;

      @OpenAPIDescription("Core QueryFilter JSON, identical to the table query and count endpoints.")
      public QQueryFilter filter;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("tableName", new Schema().withType(Type.STRING).withDescription("Table name.").withNullable(true));
         properties.put("filter", ref("QueryFilter").withDescription("Core QueryFilter JSON, identical to the table query and count endpoints.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** DuplicatesRequest version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisDuplicatesRequestV1 implements ToSchema
   {
      @OpenAPIDescription("Core QueryFilter JSON, identical to the table query and count endpoints.")
      public QQueryFilter filter;

      @OpenAPIDescription("Readable fields to include; omit for all analyzable fields on a table profile.")
      public List<String> fields;

      @OpenAPIDescription("Maximum duplicate groups to return, from 1 to 500; default 50.")
      public Integer limit;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("filter", ref("QueryFilter").withDescription("Core QueryFilter JSON, identical to the table query and count endpoints."));
         properties.put("fields", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Readable fields to include; omit for all analyzable fields on a table profile."));
         properties.put("limit", new Schema().withType(Type.INTEGER).withDescription("Maximum duplicate groups to return, from 1 to 500; default 50."));
         return (new RequiredSchema(List.of("fields")).withType(Type.OBJECT).withProperties(properties).withAdditionalProperties(false));
      }
   }

   /*******************************************************************************
    ** DuplicatesResult version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisDuplicatesResultV1 implements ToSchema
   {
      @OpenAPIDescription("Table name.")
      public String tableName;

      @OpenAPIDescription("Core QueryFilter JSON, identical to the table query and count endpoints.")
      public QQueryFilter filter;

      @OpenAPIDescription("True when statistics describe a sample rather than the entire filtered population.")
      public Boolean estimate;

      @OpenAPIDescription("Actual number of rows in the sample, not an extrapolated count.")
      public Long sampleRows;

      @OpenAPIDescription("True when the row limit prevented a complete scan.")
      public Boolean truncated;

      @OpenAPIDescription("Number of source rows included in computation.")
      public Long rowsScanned;

      @OpenAPIDescription("As of.")
      public String asOf;

      @OpenAPIDescription("Data version.")
      public String dataVersion;

      @OpenAPIDescription("Warnings.")
      public List<String> warnings;

      @OpenAPIDescription("Readable fields to include; omit for all analyzable fields on a table profile.")
      public List<String> fields;

      @OpenAPIDescription("Groups.")
      public List<DataAnalysisValueCountV1> groups;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("tableName", new Schema().withType(Type.STRING).withDescription("Table name.").withNullable(true));
         properties.put("filter", ref("QueryFilter").withDescription("Core QueryFilter JSON, identical to the table query and count endpoints.").withNullable(true));
         properties.put("estimate", new Schema().withType(Type.BOOLEAN).withDescription("True when statistics describe a sample rather than the entire filtered population.").withNullable(true));
         properties.put("sampleRows", new Schema().withType(Type.INTEGER).withDescription("Actual number of rows in the sample, not an extrapolated count.").withNullable(true));
         properties.put("truncated", new Schema().withType(Type.BOOLEAN).withDescription("True when the row limit prevented a complete scan.").withNullable(true));
         properties.put("rowsScanned", new Schema().withType(Type.INTEGER).withDescription("Number of source rows included in computation.").withNullable(true));
         properties.put("asOf", new Schema().withType(Type.STRING).withFormat("date-time").withDescription("As of.").withNullable(true));
         properties.put("dataVersion", new Schema().withType(Type.STRING).withDescription("Data version.").withNullable(true));
         properties.put("warnings", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Warnings.").withNullable(true));
         properties.put("fields", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Readable fields to include; omit for all analyzable fields on a table profile.").withNullable(true));
         properties.put("groups", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisValueCountV1")).withDescription("Groups.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** FieldSummary version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisFieldSummaryV1 implements ToSchema
   {
      @OpenAPIDescription("Field.")
      public String field;

      @OpenAPIDescription("Label.")
      public String label;

      @OpenAPIDescription("Type.")
      public String type;

      @OpenAPIDescription("Completeness.")
      public BigDecimal completeness;

      @OpenAPIDescription("Distinct.")
      public Long distinct;

      @OpenAPIDescription("Uniqueness.")
      public BigDecimal uniqueness;

      @OpenAPIDescription("Constant.")
      public Boolean constant;

      @OpenAPIDescription("Empty.")
      public Boolean empty;

      @OpenAPIDescription("Nulls.")
      public DataAnalysisMetricV1 nulls;

      @OpenAPIDescription("Blanks.")
      public DataAnalysisMetricV1 blanks;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("field", new Schema().withType(Type.STRING).withDescription("Field.").withNullable(true));
         properties.put("label", new Schema().withType(Type.STRING).withDescription("Label.").withNullable(true));
         properties.put("type", new Schema().withType(Type.STRING).withDescription("Type.").withNullable(true));
         properties.put("completeness", new Schema().withType(Type.NUMBER).withDescription("Completeness.").withNullable(true));
         properties.put("distinct", new Schema().withType(Type.INTEGER).withDescription("Distinct.").withNullable(true));
         properties.put("uniqueness", new Schema().withType(Type.NUMBER).withDescription("Uniqueness.").withNullable(true));
         properties.put("constant", new Schema().withType(Type.BOOLEAN).withDescription("Constant.").withNullable(true));
         properties.put("empty", new Schema().withType(Type.BOOLEAN).withDescription("Empty.").withNullable(true));
         properties.put("nulls", ref("DataAnalysisMetricV1").withDescription("Nulls.").withNullable(true));
         properties.put("blanks", ref("DataAnalysisMetricV1").withDescription("Blanks.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** Metric version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisMetricV1 implements ToSchema
   {
      @OpenAPIDescription("Key.")
      public Boolean estimate;
      public String key;

      @OpenAPIDescription("Label.")
      public String label;

      @OpenAPIDescription("Value.")
      public Serializable value;

      @OpenAPIDescription("Value type.")
      public ValueType valueType;

      @OpenAPIDescription("Formatted.")
      public String formatted;

      @OpenAPIDescription("Open the matching records using this table and QueryFilter; null when suppressed.")
      public DataAnalysisDrillV1 drill;

      @OpenAPIDescription("Suppressed.")
      public Boolean suppressed;

      @OpenAPIDescription("Suppressed reason.")
      public String suppressedReason;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("key", new Schema().withType(Type.STRING).withDescription("Key.").withNullable(true));
         properties.put("label", new Schema().withType(Type.STRING).withDescription("Label.").withNullable(true));
         properties.put("value", new Schema().withOneOf(List.of(new Schema().withType(Type.STRING), new Schema().withType(Type.NUMBER), new Schema().withType(Type.BOOLEAN), new Schema().withType(Type.OBJECT).withNullable(true))).withDescription("Value.").withNullable(true));
         properties.put("valueType", new Schema().withType(Type.STRING).withEnumValues(java.util.Arrays.stream(ValueType.values()).map(Enum::name).toList()).withDescription("Value type.").withNullable(true));
         properties.put("estimate", new Schema().withType(Type.BOOLEAN).withDescription("Whether this metric is estimated; the population rows metric remains exact."));
         properties.put("formatted", new Schema().withType(Type.STRING).withDescription("Formatted.").withNullable(true));
         properties.put("drill", ref("DataAnalysisDrillV1").withDescription("Open the matching records using this table and QueryFilter; null when suppressed.").withNullable(true));
         properties.put("suppressed", new Schema().withType(Type.BOOLEAN).withDescription("Suppressed.").withNullable(true));
         properties.put("suppressedReason", new Schema().withType(Type.STRING).withDescription("Suppressed reason.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** NumericProfile version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisNumericProfileV1 implements ToSchema
   {
      @OpenAPIDescription("Metrics.")
      public List<DataAnalysisMetricV1> metrics;

      @OpenAPIDescription("Histogram.")
      public List<DataAnalysisValueCountV1> histogram;

      @OpenAPIDescription("Outliers.")
      public List<DataAnalysisValueCountV1> outliers;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("metrics", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisMetricV1")).withDescription("Metrics.").withNullable(true));
         properties.put("histogram", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisValueCountV1")).withDescription("Histogram.").withNullable(true));
         properties.put("outliers", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisValueCountV1")).withDescription("Outliers.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** Point version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisPointV1 implements ToSchema
   {
      @OpenAPIDescription("X.")
      public Serializable x;

      @OpenAPIDescription("Y.")
      public BigDecimal y;

      @OpenAPIDescription("Label.")
      public String label;

      @OpenAPIDescription("Open the matching records using this table and QueryFilter; null when suppressed.")
      public DataAnalysisDrillV1 drill;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("x", new Schema().withOneOf(List.of(new Schema().withType(Type.STRING), new Schema().withType(Type.NUMBER), new Schema().withType(Type.BOOLEAN), new Schema().withType(Type.OBJECT).withNullable(true))).withDescription("X.").withNullable(true));
         properties.put("y", new Schema().withType(Type.NUMBER).withDescription("Y.").withNullable(true));
         properties.put("label", new Schema().withType(Type.STRING).withDescription("Label.").withNullable(true));
         properties.put("drill", ref("DataAnalysisDrillV1").withDescription("Open the matching records using this table and QueryFilter; null when suppressed.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** RunStatus version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisRunStatusV1 implements ToSchema
   {
      @OpenAPIDescription("Run id.")
      public String runId;

      @OpenAPIDescription("Status.")
      public String status;

      @OpenAPIDescription("Error.")
      public String error;

      @OpenAPIDescription("Completed column result; null for other operations or unfinished runs.")
      public DataAnalysisColumnProfileV1 columnProfile;

      @OpenAPIDescription("Completed table result; null for other operations or unfinished runs.")
      public DataAnalysisTableProfileV1 tableProfile;

      @OpenAPIDescription("Started at.")
      public String startedAt;

      @OpenAPIDescription("Finished at.")
      public String finishedAt;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("runId", new Schema().withType(Type.STRING).withDescription("Run id.").withNullable(true));
         properties.put("status", new Schema().withType(Type.STRING).withDescription("Status.").withNullable(true));
         properties.put("error", new Schema().withType(Type.STRING).withDescription("Error.").withNullable(true));
         properties.put("columnProfile", ref("DataAnalysisColumnProfileV1").withDescription("Completed column result; null for other operations or unfinished runs.").withNullable(true));
         properties.put("tableProfile", ref("DataAnalysisTableProfileV1").withDescription("Completed table result; null for other operations or unfinished runs.").withNullable(true));
         properties.put("startedAt", new Schema().withType(Type.STRING).withFormat("date-time").withDescription("Started at.").withNullable(true));
         properties.put("finishedAt", new Schema().withType(Type.STRING).withFormat("date-time").withDescription("Finished at.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** Series version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisSeriesV1 implements ToSchema
   {
      @OpenAPIDescription("Name.")
      public String name;

      @OpenAPIDescription("Points.")
      public List<DataAnalysisPointV1> points;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("name", new Schema().withType(Type.STRING).withDescription("Name.").withNullable(true));
         properties.put("points", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisPointV1")).withDescription("Points.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** TableProfile version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisTableProfileV1 implements ToSchema
   {
      @OpenAPIDescription("Table name.")
      public String tableName;

      @OpenAPIDescription("Core QueryFilter JSON, identical to the table query and count endpoints.")
      public QQueryFilter filter;

      @OpenAPIDescription("True when statistics describe a sample rather than the entire filtered population.")
      public Boolean estimate;

      @OpenAPIDescription("Actual number of rows in the sample, not an extrapolated count.")
      public Long sampleRows;

      @OpenAPIDescription("True when the row limit prevented a complete scan.")
      public Boolean truncated;

      @OpenAPIDescription("Number of source rows included in computation.")
      public Long rowsScanned;

      @OpenAPIDescription("As of.")
      public String asOf;

      @OpenAPIDescription("Data version.")
      public String dataVersion;

      @OpenAPIDescription("Warnings.")
      public List<String> warnings;

      @OpenAPIDescription("Row count.")
      public DataAnalysisMetricV1 rowCount;

      @OpenAPIDescription("Readable fields to include; omit for all analyzable fields on a table profile.")
      public List<DataAnalysisFieldSummaryV1> fields;

      @OpenAPIDescription("Heatmap.")
      public DataAnalysisChartV1 heatmap;

      @OpenAPIDescription("Candidate keys.")
      public List<DataAnalysisCandidateKeyV1> candidateKeys;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("tableName", new Schema().withType(Type.STRING).withDescription("Table name.").withNullable(true));
         properties.put("filter", ref("QueryFilter").withDescription("Core QueryFilter JSON, identical to the table query and count endpoints.").withNullable(true));
         properties.put("estimate", new Schema().withType(Type.BOOLEAN).withDescription("True when statistics describe a sample rather than the entire filtered population.").withNullable(true));
         properties.put("sampleRows", new Schema().withType(Type.INTEGER).withDescription("Actual number of rows in the sample, not an extrapolated count.").withNullable(true));
         properties.put("truncated", new Schema().withType(Type.BOOLEAN).withDescription("True when the row limit prevented a complete scan.").withNullable(true));
         properties.put("rowsScanned", new Schema().withType(Type.INTEGER).withDescription("Number of source rows included in computation.").withNullable(true));
         properties.put("asOf", new Schema().withType(Type.STRING).withFormat("date-time").withDescription("As of.").withNullable(true));
         properties.put("dataVersion", new Schema().withType(Type.STRING).withDescription("Data version.").withNullable(true));
         properties.put("warnings", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Warnings.").withNullable(true));
         properties.put("rowCount", ref("DataAnalysisMetricV1").withDescription("Row count.").withNullable(true));
         properties.put("fields", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisFieldSummaryV1")).withDescription("Readable fields to include; omit for all analyzable fields on a table profile.").withNullable(true));
         properties.put("heatmap", ref("DataAnalysisChartV1").withDescription("Heatmap.").withNullable(true));
         properties.put("candidateKeys", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisCandidateKeyV1")).withDescription("Candidate keys.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** TableProfileRequest version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisTableProfileRequestV1 implements ToSchema
   {
      @OpenAPIDescription("Core QueryFilter JSON, identical to the table query and count endpoints.")
      public QQueryFilter filter;

      @OpenAPIDescription("Readable fields to include; omit for all analyzable fields on a table profile.")
      public List<String> fields;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("filter", ref("QueryFilter").withDescription("Core QueryFilter JSON, identical to the table query and count endpoints."));
         properties.put("fields", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Readable fields to include; omit for all analyzable fields on a table profile."));
         return (new Schema().withType(Type.OBJECT).withProperties(properties).withAdditionalProperties(false));
      }
   }

   /*******************************************************************************
    ** TextProfile version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisTextProfileV1 implements ToSchema
   {
      @OpenAPIDescription("Metrics.")
      public List<DataAnalysisMetricV1> metrics;

      @OpenAPIDescription("Histogram.")
      public List<DataAnalysisValueCountV1> histogram;

      @OpenAPIDescription("Patterns.")
      public List<DataAnalysisValueCountV1> patterns;

      @OpenAPIDescription("Actual number of rows in the sample, not an extrapolated count.")
      public Long sampleRows;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("metrics", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisMetricV1")).withDescription("Metrics.").withNullable(true));
         properties.put("histogram", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisValueCountV1")).withDescription("Histogram.").withNullable(true));
         properties.put("patterns", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisValueCountV1")).withDescription("Patterns.").withNullable(true));
         properties.put("sampleRows", new Schema().withType(Type.INTEGER).withDescription("Actual number of rows in the sample, not an extrapolated count.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** ValueCount version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisValueCountV1 implements ToSchema
   {
      @OpenAPIDescription("Value.")
      public Serializable value;

      @OpenAPIDescription("Label.")
      public String label;

      @OpenAPIDescription("Count.")
      public Long count;

      @OpenAPIDescription("Share.")
      public BigDecimal share;

      @OpenAPIDescription("Open the matching records using this table and QueryFilter; null when suppressed.")
      public DataAnalysisDrillV1 drill;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("value", new Schema().withOneOf(List.of(new Schema().withType(Type.STRING), new Schema().withType(Type.NUMBER), new Schema().withType(Type.BOOLEAN), new Schema().withType(Type.OBJECT).withNullable(true))).withDescription("Value.").withNullable(true));
         properties.put("label", new Schema().withType(Type.STRING).withDescription("Label.").withNullable(true));
         properties.put("count", new Schema().withType(Type.INTEGER).withDescription("Count.").withNullable(true));
         properties.put("share", new Schema().withType(Type.NUMBER).withDescription("Share.").withNullable(true));
         properties.put("drill", ref("DataAnalysisDrillV1").withDescription("Open the matching records using this table and QueryFilter; null when suppressed.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** Capabilities version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisCapabilitiesV1 implements ToSchema
   {
      @OpenAPIDescription("Whether this session may analyze the table; false responses expose no fields.")
      public Boolean canAnalyze;

      @OpenAPIDescription("Table name.")
      public String tableName;

      @OpenAPIDescription("Table label.")
      public String tableLabel;

      @OpenAPIDescription("Readable fields to include; omit for all analyzable fields on a table profile.")
      public List<DataAnalysisCapabilityFieldV1> fields;

      @OpenAPIDescription("Limits.")
      public DataAnalysisLimitsV1 limits;

      @OpenAPIDescription("Backend capabilities.")
      public List<String> backendCapabilities;

      @OpenAPIDescription("Whether SQL Workbench metadata is installed in this instance.")
      public Boolean sqlWorkbenchAvailable;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("canAnalyze", new Schema().withType(Type.BOOLEAN).withDescription("Whether this session may analyze the table; false responses expose no fields.").withNullable(true));
         properties.put("tableName", new Schema().withType(Type.STRING).withDescription("Table name.").withNullable(true));
         properties.put("tableLabel", new Schema().withType(Type.STRING).withDescription("Table label.").withNullable(true));
         properties.put("fields", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisCapabilityFieldV1")).withDescription("Readable fields to include; omit for all analyzable fields on a table profile.").withNullable(true));
         properties.put("limits", ref("DataAnalysisLimitsV1").withDescription("Limits.").withNullable(true));
         properties.put("backendCapabilities", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Backend capabilities.").withNullable(true));
         properties.put("sqlWorkbenchAvailable", new Schema().withType(Type.BOOLEAN).withDescription("Whether SQL Workbench metadata is installed in this instance.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** CapabilityField version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisCapabilityFieldV1 implements ToSchema
   {
      @OpenAPIDescription("Name.")
      public String name;

      @OpenAPIDescription("Label.")
      public String label;

      @OpenAPIDescription("Type.")
      public String type;

      @OpenAPIDescription("Semantic field kind: NUMERIC, DATE, DATE_TIME, TEXT, BOOLEAN or CATEGORY.")
      public String kind;

      @OpenAPIDescription("Default chart kind.")
      public ChartKind defaultChartKind;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("name", new Schema().withType(Type.STRING).withDescription("Name.").withNullable(true));
         properties.put("label", new Schema().withType(Type.STRING).withDescription("Label.").withNullable(true));
         properties.put("type", new Schema().withType(Type.STRING).withDescription("Type.").withNullable(true));
         properties.put("kind", new Schema().withType(Type.STRING).withDescription("Semantic field kind: NUMERIC, DATE, DATE_TIME, TEXT, BOOLEAN or CATEGORY.").withNullable(true));
         properties.put("defaultChartKind", new Schema().withType(Type.STRING).withEnumValues(java.util.Arrays.stream(ChartKind.values()).map(Enum::name).toList()).withDescription("Default chart kind.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** Limits version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisLimitsV1 implements ToSchema
   {
      @OpenAPIDescription("Max rows.")
      public Integer maxRows;

      @OpenAPIDescription("Sample threshold rows.")
      public Integer sampleThresholdRows;

      @OpenAPIDescription("Actual number of rows in the sample, not an extrapolated count.")
      public Integer sampleRows;

      @OpenAPIDescription("Min group size.")
      public Integer minGroupSize;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("maxRows", new Schema().withType(Type.INTEGER).withDescription("Max rows.").withNullable(true));
         properties.put("sampleThresholdRows", new Schema().withType(Type.INTEGER).withDescription("Sample threshold rows.").withNullable(true));
         properties.put("sampleRows", new Schema().withType(Type.INTEGER).withDescription("Actual number of rows in the sample, not an extrapolated count.").withNullable(true));
         properties.put("minGroupSize", new Schema().withType(Type.INTEGER).withDescription("Min group size.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** StartRunRequest version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisStartRunRequestV1 implements ToSchema
   {
      @OpenAPIDescription("COLUMN_PROFILE or TABLE_PROFILE.")
      public String operation;

      @OpenAPIDescription("Table name.")
      public String tableName;

      @OpenAPIDescription("Profile parameters for the selected operation, without tableName.")
      public Object request;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("operation", new Schema().withType(Type.STRING).withEnumValues(List.of("COLUMN_PROFILE", "TABLE_PROFILE")).withDescription("COLUMN_PROFILE or TABLE_PROFILE."));
         properties.put("tableName", new Schema().withType(Type.STRING).withDescription("Table name."));
         properties.put("request", new Schema().withOneOf(List.of(ref("DataAnalysisColumnProfileRequestV1"), ref("DataAnalysisTableProfileRequestV1"))).withDescription("Profile parameters for the selected operation, without tableName."));
         return (new RequiredSchema(List.of("operation", "tableName", "request")).withType(Type.OBJECT).withProperties(properties).withAdditionalProperties(false));
      }
   }

   /*******************************************************************************
    ** RunStarted version 1 transport contract.
    *******************************************************************************/
   public static class DataAnalysisRunStartedV1 implements ToSchema
   {
      @OpenAPIDescription("Run id.")
      public String runId;

      @OpenAPIDescription("Status.")
      public String status;

      /*******************************************************************************
       ** Explicit component references keep the standalone contract reusable.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("runId", new Schema().withType(Type.STRING).withDescription("Run id.").withNullable(true));
         properties.put("status", new Schema().withType(Type.STRING).withDescription("Status.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withNullable(true).withProperties(properties));
      }
   }
}
