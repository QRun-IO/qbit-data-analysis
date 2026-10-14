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
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;

/*******************************************************************************
 ** Explicit version-one contracts for compare and relationships.
 *******************************************************************************/
public class RelationshipSchemas
{
   /*******************************************************************************
    ** Register every referenced response and request component.
    *******************************************************************************/
   public static Map<String, Schema> components()
   {
      Map<String, Schema> result = new LinkedHashMap<>();
      result.put("DataAnalysisCompareRequestV1", new DataAnalysisCompareRequestV1().toSchema());
      result.put("DataAnalysisShiftRequestV1", new DataAnalysisShiftRequestV1().toSchema());
      result.put("DataAnalysisCorrelationRequestV1", new DataAnalysisCorrelationRequestV1().toSchema());
      result.put("DataAnalysisScatterRequestV1", new DataAnalysisScatterRequestV1().toSchema());
      result.put("DataAnalysisCrosstabRequestV1", new DataAnalysisCrosstabRequestV1().toSchema());
      result.put("DataAnalysisSignificanceV1", new DataAnalysisSignificanceV1().toSchema());
      result.put("DataAnalysisComparisonSideV1", new DataAnalysisComparisonSideV1().toSchema());
      result.put("DataAnalysisComparisonValueV1", new DataAnalysisComparisonValueV1().toSchema());
      result.put("DataAnalysisFieldComparisonV1", new DataAnalysisFieldComparisonV1().toSchema());
      result.put("DataAnalysisCompareV1", new DataAnalysisCompareV1().toSchema());
      result.put("DataAnalysisShiftBucketV1", new DataAnalysisShiftBucketV1().toSchema());
      result.put("DataAnalysisShiftV1", new DataAnalysisShiftV1().toSchema());
      result.put("DataAnalysisCorrelationCellV1", new DataAnalysisCorrelationCellV1().toSchema());
      result.put("DataAnalysisCorrelationV1", new DataAnalysisCorrelationV1().toSchema());
      result.put("DataAnalysisTrendLineV1", new DataAnalysisTrendLineV1().toSchema());
      result.put("DataAnalysisScatterV1", new DataAnalysisScatterV1().toSchema());
      result.put("DataAnalysisCrosstabCellV1", new DataAnalysisCrosstabCellV1().toSchema());
      result.put("DataAnalysisCrosstabV1", new DataAnalysisCrosstabV1().toSchema());
      return (result);
   }

   /*******************************************************************************
    ** Reuse the common metric, filter and chart contracts.
    *******************************************************************************/
   private static Schema ref(String name)
   {
      return (new Schema().withRef("#/components/schemas/" + name));
   }

   /*******************************************************************************
    ** CompareRequest version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisCompareRequestV1 implements ToSchema
   {
      @OpenAPIDescription("Population A filter; null selects the whole permitted table.")
      public QQueryFilter filterA;

      @OpenAPIDescription("Population B filter; null selects the whole permitted table.")
      public QQueryFilter filterB;

      @OpenAPIDescription("Display name of population A; default A.")
      public String labelA;

      @OpenAPIDescription("Display name of population B; default B.")
      public String labelB;

      @OpenAPIDescription("Distinct FULL fields to analyze; correlation requires 2 to 12 numeric fields.")
      public List<String> fields;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("filterA", ref("QueryFilter").withDescription("Population A filter; null selects the whole permitted table.").withNullable(true));
         properties.put("filterB", ref("QueryFilter").withDescription("Population B filter; null selects the whole permitted table.").withNullable(true));
         properties.put("labelA", new Schema().withType(Type.STRING).withDescription("Display name of population A; default A."));
         properties.put("labelB", new Schema().withType(Type.STRING).withDescription("Display name of population B; default B."));
         properties.put("fields", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Distinct FULL fields to analyze; correlation requires 2 to 12 numeric fields."));
         return (new DataAnalysisSchemas.RequiredSchema(List.of("fields")).withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** ShiftRequest version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisShiftRequestV1 implements ToSchema
   {
      @OpenAPIDescription("Current-user query filter; record locks and core field rules apply.")
      public QQueryFilter filter;

      @OpenAPIDescription("FULL field whose distribution is compared over time.")
      public String field;

      @OpenAPIDescription("FULL DATE or DATE_TIME field for session-timezone calendar buckets.")
      public String dateField;

      @OpenAPIDescription("Calendar unit: HOUR, DAY, WEEK, MONTH, QUARTER or YEAR; default MONTH.")
      public String bucket;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("filter", ref("QueryFilter").withDescription("Current-user query filter; record locks and core field rules apply.").withNullable(true));
         properties.put("field", new Schema().withType(Type.STRING).withDescription("FULL field whose distribution is compared over time."));
         properties.put("dateField", new Schema().withType(Type.STRING).withDescription("FULL DATE or DATE_TIME field for session-timezone calendar buckets."));
         properties.put("bucket", new Schema().withType(Type.STRING).withDescription("Calendar unit: HOUR, DAY, WEEK, MONTH, QUARTER or YEAR; default MONTH."));
         return (new DataAnalysisSchemas.RequiredSchema(List.of("field", "dateField")).withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** CorrelationRequest version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisCorrelationRequestV1 implements ToSchema
   {
      @OpenAPIDescription("Current-user query filter; record locks and core field rules apply.")
      public QQueryFilter filter;

      @OpenAPIDescription("Distinct FULL fields to analyze; correlation requires 2 to 12 numeric fields.")
      public List<String> fields;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("filter", ref("QueryFilter").withDescription("Current-user query filter; record locks and core field rules apply.").withNullable(true));
         properties.put("fields", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Distinct FULL fields to analyze; correlation requires 2 to 12 numeric fields."));
         return (new DataAnalysisSchemas.RequiredSchema(List.of("fields")).withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** ScatterRequest version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisScatterRequestV1 implements ToSchema
   {
      @OpenAPIDescription("Table for the scatter link; route parameter takes precedence.")
      public String tableName;

      @OpenAPIDescription("Compatibility flag; scatter always returns the bounded sample.")
      public Boolean exact;
      @OpenAPIDescription("Current-user query filter; record locks and core field rules apply.")
      public QQueryFilter filter;

      @OpenAPIDescription("FULL numeric field on the x axis.")
      public String xField;

      @OpenAPIDescription("FULL numeric field on the y axis.")
      public String yField;

      @OpenAPIDescription("Maximum sample points (1 to 2000, default 2000) or kept crosstab categories per axis (1 to 20, default 20).")
      public Integer limit;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("tableName", new Schema().withType(Type.STRING).withDescription("Table for the scatter link; route parameter takes precedence.").withNullable(true));
         properties.put("exact", new Schema().withType(Type.BOOLEAN).withDescription("Compatibility flag; scatter always returns the bounded sample.").withNullable(true));
         properties.put("filter", ref("QueryFilter").withDescription("Current-user query filter; record locks and core field rules apply.").withNullable(true));
         properties.put("xField", new Schema().withType(Type.STRING).withDescription("FULL numeric field on the x axis."));
         properties.put("yField", new Schema().withType(Type.STRING).withDescription("FULL numeric field on the y axis."));
         properties.put("limit", new Schema().withType(Type.INTEGER).withDescription("Maximum sample points (1 to 2000, default 2000) or kept crosstab categories per axis (1 to 20, default 20)."));
         return (new DataAnalysisSchemas.RequiredSchema(List.of("xField", "yField")).withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** CrosstabRequest version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisCrosstabRequestV1 implements ToSchema
   {
      @OpenAPIDescription("Current-user query filter; record locks and core field rules apply.")
      public QQueryFilter filter;

      @OpenAPIDescription("FULL field defining crosstab rows.")
      public String rowField;

      @OpenAPIDescription("FULL field defining crosstab columns.")
      public String columnField;

      @OpenAPIDescription("Maximum sample points (1 to 2000, default 2000) or kept crosstab categories per axis (1 to 20, default 20).")
      public Integer limit;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("filter", ref("QueryFilter").withDescription("Current-user query filter; record locks and core field rules apply.").withNullable(true));
         properties.put("rowField", new Schema().withType(Type.STRING).withDescription("FULL field defining crosstab rows."));
         properties.put("columnField", new Schema().withType(Type.STRING).withDescription("FULL field defining crosstab columns."));
         properties.put("limit", new Schema().withType(Type.INTEGER).withDescription("Maximum sample points (1 to 2000, default 2000) or kept crosstab categories per axis (1 to 20, default 20)."));
         return (new DataAnalysisSchemas.RequiredSchema(List.of("rowField", "columnField")).withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** Significance version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisSignificanceV1 implements ToSchema
   {
      @OpenAPIDescription("Test.")
      public String test;

      @OpenAPIDescription("Statistic.")
      public Double statistic;

      @OpenAPIDescription("Degrees of freedom; null when undefined.")
      public Double df;

      @OpenAPIDescription("Two-sided p-value; p >= 0.05 is never significant.")
      public Double pValue;

      @OpenAPIDescription("Whether all test validity and privacy requirements hold.")
      public Boolean applicable;

      @OpenAPIDescription("Plain-language result or reason the statistic is not applicable.")
      public String sentence;

      @OpenAPIDescription("Cramer association strength computed on the pooled contingency table; null when invalid.")
      public Double cramersV;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("test", new Schema().withType(Type.STRING).withDescription("Test.").withNullable(true));
         properties.put("statistic", new Schema().withType(Type.NUMBER).withDescription("Statistic.").withNullable(true));
         properties.put("df", new Schema().withType(Type.NUMBER).withDescription("Degrees of freedom; null when undefined.").withNullable(true));
         properties.put("pValue", new Schema().withType(Type.NUMBER).withDescription("Two-sided p-value; p >= 0.05 is never significant.").withNullable(true));
         properties.put("applicable", new Schema().withType(Type.BOOLEAN).withDescription("Whether all test validity and privacy requirements hold.").withNullable(true));
         properties.put("sentence", new Schema().withType(Type.STRING).withDescription("Plain-language result or reason the statistic is not applicable.").withNullable(true));
         properties.put("cramersV", new Schema().withType(Type.NUMBER).withDescription("Cramer association strength computed on the pooled contingency table; null when invalid.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** ComparisonSide version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisComparisonSideV1 implements ToSchema
   {
      @OpenAPIDescription("Label.")
      public String label;

      @OpenAPIDescription("Typed metrics with exact population drills unless privacy suppresses them.")
      public List<DataAnalysisSchemas.DataAnalysisMetricV1> metrics;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("label", new Schema().withType(Type.STRING).withDescription("Label.").withNullable(true));
         properties.put("metrics", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisMetricV1")).withDescription("Typed metrics with exact population drills unless privacy suppresses them.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** ComparisonValue version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisComparisonValueV1 implements ToSchema
   {
      @OpenAPIDescription("Value.")
      public Serializable value;

      @OpenAPIDescription("Label.")
      public String label;

      @OpenAPIDescription("Side a.")
      public DataAnalysisComparisonSideV1 sideA;

      @OpenAPIDescription("Side b.")
      public DataAnalysisComparisonSideV1 sideB;

      @OpenAPIDescription("B minus A, in original units (share differences are fractions).")
      public DataAnalysisSchemas.DataAnalysisMetricV1 delta;

      @OpenAPIDescription("(B minus A) divided by absolute A, a fraction; null at a zero baseline.")
      public DataAnalysisSchemas.DataAnalysisMetricV1 percentDelta;

      @OpenAPIDescription("Test assumptions, two-sided p-value, degrees of freedom and plain-language conclusion.")
      public DataAnalysisSignificanceV1 significance;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("value", new Schema().withDescription("Value.").withNullable(true));
         properties.put("label", new Schema().withType(Type.STRING).withDescription("Label.").withNullable(true));
         properties.put("sideA", ref("DataAnalysisComparisonSideV1").withDescription("Side a.").withNullable(true));
         properties.put("sideB", ref("DataAnalysisComparisonSideV1").withDescription("Side b.").withNullable(true));
         properties.put("delta", ref("DataAnalysisMetricV1").withDescription("B minus A, in original units (share differences are fractions).").withNullable(true));
         properties.put("percentDelta", ref("DataAnalysisMetricV1").withDescription("(B minus A) divided by absolute A, a fraction; null at a zero baseline.").withNullable(true));
         properties.put("significance", ref("DataAnalysisSignificanceV1").withDescription("Test assumptions, two-sided p-value, degrees of freedom and plain-language conclusion.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** FieldComparison version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisFieldComparisonV1 implements ToSchema
   {
      @OpenAPIDescription("FULL field whose distribution is compared over time.")
      public String field;

      @OpenAPIDescription("Values.")
      public List<DataAnalysisComparisonValueV1> values;

      @OpenAPIDescription("Test assumptions, two-sided p-value, degrees of freedom and plain-language conclusion.")
      public DataAnalysisSignificanceV1 significance;

      @OpenAPIDescription("Semantic series; category comparisons are grouped and category time series are stacked.")
      public DataAnalysisSchemas.DataAnalysisChartV1 chart;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("field", new Schema().withType(Type.STRING).withDescription("FULL field whose distribution is compared over time.").withNullable(true));
         properties.put("values", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisComparisonValueV1")).withDescription("Values.").withNullable(true));
         properties.put("significance", ref("DataAnalysisSignificanceV1").withDescription("Test assumptions, two-sided p-value, degrees of freedom and plain-language conclusion.").withNullable(true));
         properties.put("chart", ref("DataAnalysisChartV1").withDescription("Semantic series; category comparisons are grouped and category time series are stacked.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** CompareResult version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisCompareV1 implements ToSchema
   {
      @OpenAPIDescription("Distinct FULL fields to analyze; correlation requires 2 to 12 numeric fields.")
      public List<DataAnalysisFieldComparisonV1> fields;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("fields", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisFieldComparisonV1")).withDescription("Distinct FULL fields to analyze; correlation requires 2 to 12 numeric fields.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** ShiftBucket version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisShiftBucketV1 implements ToSchema
   {
      @OpenAPIDescription("Value.")
      public Serializable value;

      @OpenAPIDescription("Label.")
      public String label;

      @OpenAPIDescription("True when the calendar bucket end has passed in the session timezone.")
      public Boolean complete;

      @OpenAPIDescription("Typed metrics with exact population drills unless privacy suppresses them.")
      public List<DataAnalysisSchemas.DataAnalysisMetricV1> metrics;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("value", new Schema().withDescription("Value.").withNullable(true));
         properties.put("label", new Schema().withType(Type.STRING).withDescription("Label.").withNullable(true));
         properties.put("complete", new Schema().withType(Type.BOOLEAN).withDescription("True when the calendar bucket end has passed in the session timezone.").withNullable(true));
         properties.put("metrics", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisMetricV1")).withDescription("Typed metrics with exact population drills unless privacy suppresses them.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** ShiftResult version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisShiftV1 implements ToSchema
   {
      @OpenAPIDescription("Buckets.")
      public List<DataAnalysisShiftBucketV1> buckets;

      @OpenAPIDescription("Population stability index, null when inapplicable; zero shares are floored at 0.000001.")
      public DataAnalysisSchemas.DataAnalysisMetricV1 psi;

      @OpenAPIDescription("Plain-language result or reason the statistic is not applicable.")
      public String sentence;

      @OpenAPIDescription("Semantic series; category comparisons are grouped and category time series are stacked.")
      public DataAnalysisSchemas.DataAnalysisChartV1 chart;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("buckets", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisShiftBucketV1")).withDescription("Buckets.").withNullable(true));
         properties.put("psi", ref("DataAnalysisMetricV1").withDescription("Population stability index, null when inapplicable; zero shares are floored at 0.000001.").withNullable(true));
         properties.put("sentence", new Schema().withType(Type.STRING).withDescription("Plain-language result or reason the statistic is not applicable.").withNullable(true));
         properties.put("chart", ref("DataAnalysisChartV1").withDescription("Semantic series; category comparisons are grouped and category time series are stacked.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** CorrelationCell version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisCorrelationCellV1 implements ToSchema
   {
      @OpenAPIDescription("FULL numeric field on the x axis.")
      public String xField;

      @OpenAPIDescription("FULL numeric field on the y axis.")
      public String yField;

      @OpenAPIDescription("Pairwise non-null population count; null when privacy suppresses it.")
      public Long n;

      @OpenAPIDescription("Pearson correlation; null for insufficient data, zero variance or privacy suppression.")
      public Double r;

      @OpenAPIDescription("POST the included parameters to the table scatter endpoint.")
      public DataAnalysisScatterRequestV1 scatter;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("xField", new Schema().withType(Type.STRING).withDescription("FULL numeric field on the x axis.").withNullable(true));
         properties.put("yField", new Schema().withType(Type.STRING).withDescription("FULL numeric field on the y axis.").withNullable(true));
         properties.put("n", new Schema().withType(Type.INTEGER).withDescription("Pairwise non-null population count; null when privacy suppresses it.").withNullable(true));
         properties.put("r", new Schema().withType(Type.NUMBER).withDescription("Pearson correlation; null for insufficient data, zero variance or privacy suppression.").withNullable(true));
         properties.put("scatter", ref("DataAnalysisScatterRequestV1").withDescription("POST the included parameters to the table scatter endpoint.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** CorrelationResult version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisCorrelationV1 implements ToSchema
   {
      @OpenAPIDescription("Distinct FULL fields to analyze; correlation requires 2 to 12 numeric fields.")
      public List<String> fields;

      @OpenAPIDescription("Cells.")
      public List<DataAnalysisCorrelationCellV1> cells;

      @OpenAPIDescription("Semantic series; category comparisons are grouped and category time series are stacked.")
      public DataAnalysisSchemas.DataAnalysisChartV1 chart;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("fields", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Distinct FULL fields to analyze; correlation requires 2 to 12 numeric fields.").withNullable(true));
         properties.put("cells", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisCorrelationCellV1")).withDescription("Cells.").withNullable(true));
         properties.put("chart", ref("DataAnalysisChartV1").withDescription("Semantic series; category comparisons are grouped and category time series are stacked.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** TrendLine version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisTrendLineV1 implements ToSchema
   {
      @OpenAPIDescription("Slope.")
      public Double slope;

      @OpenAPIDescription("Intercept.")
      public Double intercept;

      @OpenAPIDescription("Pearson correlation; null for insufficient data, zero variance or privacy suppression.")
      public Double r;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("slope", new Schema().withType(Type.NUMBER).withDescription("Slope.").withNullable(true));
         properties.put("intercept", new Schema().withType(Type.NUMBER).withDescription("Intercept.").withNullable(true));
         properties.put("r", new Schema().withType(Type.NUMBER).withDescription("Pearson correlation; null for insufficient data, zero variance or privacy suppression.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** ScatterResult version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisScatterV1 implements ToSchema
   {
      @OpenAPIDescription("Pairwise non-null population count; null when privacy suppresses it.")
      public Long n;

      @OpenAPIDescription("Number of displayed paired records.")
      public Integer sampleSize;

      @OpenAPIDescription("Sampling method; core query currently uses a deterministic primary-key prefix.")
      public String sampling;

      @OpenAPIDescription("Points.")
      public List<DataAnalysisSchemas.DataAnalysisPointV1> points;

      @OpenAPIDescription("Least-squares slope, intercept and r fitted to the displayed sample.")
      public DataAnalysisTrendLineV1 trend;

      @OpenAPIDescription("Semantic series; category comparisons are grouped and category time series are stacked.")
      public DataAnalysisSchemas.DataAnalysisChartV1 chart;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("n", new Schema().withType(Type.INTEGER).withDescription("Pairwise non-null population count; null when privacy suppresses it.").withNullable(true));
         properties.put("sampleSize", new Schema().withType(Type.INTEGER).withDescription("Number of displayed paired records.").withNullable(true));
         properties.put("sampling", new Schema().withType(Type.STRING).withDescription("Sampling method; core query currently uses a deterministic primary-key prefix.").withNullable(true));
         properties.put("points", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisPointV1")).withDescription("Points.").withNullable(true));
         properties.put("trend", ref("DataAnalysisTrendLineV1").withDescription("Least-squares slope, intercept and r fitted to the displayed sample.").withNullable(true));
         properties.put("chart", ref("DataAnalysisChartV1").withDescription("Semantic series; category comparisons are grouped and category time series are stacked.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** CrosstabCell version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisCrosstabCellV1 implements ToSchema
   {
      @OpenAPIDescription("Row index.")
      public Integer rowIndex;

      @OpenAPIDescription("Column index.")
      public Integer columnIndex;

      @OpenAPIDescription("Cell count with its exact drill, or a suppressed metric.")
      public DataAnalysisSchemas.DataAnalysisMetricV1 count;

      @OpenAPIDescription("Cell count divided by row total; suppressed if it could disclose a hidden cell.")
      public DataAnalysisSchemas.DataAnalysisMetricV1 rowShare;

      @OpenAPIDescription("Cell count divided by column total; suppressed if it could disclose a hidden cell.")
      public DataAnalysisSchemas.DataAnalysisMetricV1 columnShare;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("rowIndex", new Schema().withType(Type.INTEGER).withDescription("Row index.").withNullable(true));
         properties.put("columnIndex", new Schema().withType(Type.INTEGER).withDescription("Column index.").withNullable(true));
         properties.put("count", ref("DataAnalysisMetricV1").withDescription("Cell count with its exact drill, or a suppressed metric.").withNullable(true));
         properties.put("rowShare", ref("DataAnalysisMetricV1").withDescription("Cell count divided by row total; suppressed if it could disclose a hidden cell.").withNullable(true));
         properties.put("columnShare", ref("DataAnalysisMetricV1").withDescription("Cell count divided by column total; suppressed if it could disclose a hidden cell.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }

   /*******************************************************************************
    ** CrosstabResult version-one transport model.
    *******************************************************************************/
   public static class DataAnalysisCrosstabV1 implements ToSchema
   {
      @OpenAPIDescription("Rows.")
      public List<DataAnalysisSchemas.DataAnalysisPivotHeaderV1> rows;

      @OpenAPIDescription("Columns.")
      public List<DataAnalysisSchemas.DataAnalysisPivotHeaderV1> columns;

      @OpenAPIDescription("Cells.")
      public List<DataAnalysisCrosstabCellV1> cells;

      @OpenAPIDescription("Test assumptions, two-sided p-value, degrees of freedom and plain-language conclusion.")
      public DataAnalysisSignificanceV1 significance;

      /*******************************************************************************
       ** Explicit references preserve the standalone OpenAPI fragment.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         Map<String, Schema> properties = new LinkedHashMap<>();
         properties.put("rows", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisPivotHeaderV1")).withDescription("Rows.").withNullable(true));
         properties.put("columns", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisPivotHeaderV1")).withDescription("Columns.").withNullable(true));
         properties.put("cells", new Schema().withType(Type.ARRAY).withItems(ref("DataAnalysisCrosstabCellV1")).withDescription("Cells.").withNullable(true));
         properties.put("significance", ref("DataAnalysisSignificanceV1").withDescription("Test assumptions, two-sided p-value, degrees of freedom and plain-language conclusion.").withNullable(true));
         return (new Schema().withType(Type.OBJECT).withProperties(properties));
      }
   }
}
