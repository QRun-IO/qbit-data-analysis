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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.PivotHeader;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;

/*******************************************************************************
 ** Export already-authorized typed results using core CSV and XLSX streamers.
 *******************************************************************************/
public final class AnalysisExport
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private AnalysisExport()
   {
   }

   /*******************************************************************************
    ** A result table, also reused by scheduled HTML summaries.
    *******************************************************************************/
   public record ResultTable(List<String> columns, List<List<String>> rows)
   {
   }

   /*******************************************************************************
    ** Download bytes and metadata; filenames are fixed rather than user supplied.
    *******************************************************************************/
   public record File(byte[] bytes, String mimeType, String filename)
   {
   }

   /*******************************************************************************
    ** Accept only the two documented export formats.
    *******************************************************************************/
   public static ReportFormat format(String value) throws QBadRequestException
   {
      if("csv".equalsIgnoreCase(value))
      {
         return (ReportFormat.CSV);
      }
      if("xlsx".equalsIgnoreCase(value))
      {
         return (ReportFormat.XLSX);
      }
      throw new QBadRequestException("format must be csv or xlsx.");
   }

   /*******************************************************************************
    ** Core's report streamers accept computed records without a source-table query.
    *******************************************************************************/
   public static File file(SavedResult result, String format) throws QException
   {
      ReportFormat type = format(format);
      ResultTable table = table(result);
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      var destination = new ReportDestination().withReportFormat(type).withReportOutputStream(bytes);
      var view = new QReportView().withName("analysis").withLabel("Analysis").withType(ReportType.TABLE);
      List<QFieldMetaData> fields = new ArrayList<>();
      for(int i = 0; i < table.columns().size(); i++)
      {
         fields.add(new QFieldMetaData("c" + i, QFieldType.STRING).withLabel(table.columns().get(i)));
         view.withColumn(new QReportField("c" + i).withLabel(table.columns().get(i)).withType(QFieldType.STRING));
      }
      var streamer = type.newReportStreamer();
      streamer.preRun(destination, List.of(view));
      streamer.start(new ExportInput().withReportDestination(destination), fields, "Analysis", view);
      List<QRecord> records = new ArrayList<>();
      for(List<String> row : table.rows())
      {
         QRecord record = new QRecord();
         for(int i = 0; i < row.size(); i++)
         {
            record.withValue("c" + i, row.get(i));
         }
         records.add(record);
      }
      for(String warning : result.warnings())
      {
         records.add(new QRecord().withValue("c0", "Warning: " + warning));
      }
      streamer.addRecords(records);
      streamer.finish();
      return (new File(bytes.toByteArray(), type.getMimeType(), "analysis." + type.getExtension()));
   }

   /*******************************************************************************
    ** Preserve formatted values and suppressed cells; never export underlying raw data.
    *******************************************************************************/
   public static ResultTable table(SavedResult result)
   {
      List<String> columns = new ArrayList<>(List.of("Section", "Metric", "Value"));
      List<List<String>> rows = new ArrayList<>();
      if(result.columnProfile() != null)
      {
         var p = result.columnProfile();
         metrics(rows, "Counts", p.getCounts());
         if(p.getNumeric() != null)
         {
            metrics(rows, "Numeric", p.getNumeric().getMetrics());
         }
         if(p.getText() != null)
         {
            metrics(rows, "Text", p.getText().getMetrics());
         }
         if(p.getDates() != null)
         {
            metrics(rows, "Dates", p.getDates().getMetrics());
         }
         if(p.getBooleans() != null)
         {
            metrics(rows, "Boolean", p.getBooleans());
         }
         p.getTopValues().forEach(v -> rows.add(row("Top values", v.getLabel(), v.getCount())));
      }
      else if(result.tableProfile() != null)
      {
         var p = result.tableProfile();
         metrics(rows, "Table", List.of(p.getRowCount()));
         for(var field : p.getFields())
         {
            rows.add(row(field.getLabel(), "Completeness", field.getCompleteness()));
            rows.add(row(field.getLabel(), "Distinct", field.getDistinct()));
            rows.add(row(field.getLabel(), "Uniqueness", field.getUniqueness()));
         }
      }
      else if(result.pivot() != null)
      {
         var p = result.pivot();
         columns = new ArrayList<>(List.of("Row"));
         for(var headers : p.getColumnHeaders())
         {
            for(var measure : p.getMeasures())
            {
               columns.add(label(headers) + " / " + measure.getLabel());
            }
         }
         for(int r = 0; r < p.getRowHeaders().size(); r++)
         {
            List<String> row = new ArrayList<>();
            row.add(label(p.getRowHeaders().get(r)));
            for(int c = 0; c < p.getColumnHeaders().size(); c++)
            {
               final int ri = r;
               final int ci = c;
               var cell = p.getCells().stream().filter(x -> x.getRowIndex() == ri && x.getColumnIndex() == ci).findFirst();
               for(int m = 0; m < p.getMeasures().size(); m++)
               {
                  row.add(cell.isPresent() ? value(cell.get().getValues().get(m)) : "");
               }
            }
            rows.add(row);
         }
      }
      else if(result.compare() != null)
      {
         columns = List.of("Field", "Value", "Metric", "A", "B", "Delta", "Percent delta");
         for(var field : result.compare().getFields())
         {
            for(var comparison : field.getValues())
            {
               for(var metric : comparison.getSideA().getMetrics())
               {
                  var b = comparison.getSideB().getMetrics().stream().filter(m -> Objects.equals(m.getKey(), metric.getKey())).findFirst().orElse(null);
                  rows.add(row(field.getField(), comparison.getLabel(), metric.getLabel(), value(metric), value(b), value(comparison.getDelta()), value(comparison.getPercentDelta())));
               }
            }
         }
      }
      else if(result.correlation() != null)
      {
         columns = List.of("X", "Y", "Paired rows", "Correlation");
         result.correlation().getCells().forEach(c -> rows.add(row(c.getXField(), c.getYField(), c.getN(), c.getR())));
      }
      else if(result.crosstab() != null)
      {
         columns = List.of("Row", "Column", "Count", "Row share", "Column share");
         var p = result.crosstab();
         p.getCells().forEach(c -> rows.add(row(p.getRows().get(c.getRowIndex()).getLabel(), p.getColumns().get(c.getColumnIndex()).getLabel(),
            value(c.getCount()), value(c.getRowShare()), value(c.getColumnShare()))));
      }
      else if(result.shift() != null)
      {
         for(var bucket : result.shift().getBuckets())
         {
            metrics(rows, bucket.getLabel(), bucket.getMetrics());
         }
         if(result.shift().getPsi() != null)
         {
            metrics(rows, "Shift", List.of(result.shift().getPsi()));
         }
      }
      return (new ResultTable(columns, rows));
   }

   /*******************************************************************************
    ** Headline metrics shared by standard chart widgets and email summaries.
    *******************************************************************************/
   public static List<Metric> headlines(SavedResult result)
   {
      List<Metric> metrics = new ArrayList<>();
      if(result.columnProfile() != null)
      {
         metrics.addAll(result.columnProfile().getCounts());
         if(result.columnProfile().getNumeric() != null)
         {
            metrics.addAll(result.columnProfile().getNumeric().getMetrics().stream()
               .filter(m -> List.of("mean", "median", "min", "max").contains(m.getKey())).toList());
         }
      }
      else if(result.tableProfile() != null)
      {
         metrics.add(result.tableProfile().getRowCount());
      }
      else if(result.pivot() != null)
      {
         metrics.addAll(result.pivot().getGrandTotals());
      }
      else if(result.compare() != null)
      {
         result.compare().getFields().stream().limit(4).forEach(f -> f.getValues().stream().limit(1).forEach(v -> metrics.add(v.getDelta())));
      }
      else if(result.shift() != null)
      {
         metrics.add(result.shift().getPsi());
      }
      else
      {
         int count = result.correlation() != null ? result.correlation().getFields().size() : result.crosstab() != null ? result.crosstab().getCells().size() : 0;
         if(result.correlation() != null || result.crosstab() != null)
         {
            metrics.add(new Metric().withKey("visible").withLabel(result.correlation() != null ? "Numeric fields" : "Visible combinations")
               .withValue(count).withValueType(com.kingsrook.qbits.dataanalysis.result.ValueType.COUNT).withFormatted(String.valueOf(count)));
         }
      }
      return (metrics.stream().filter(Objects::nonNull).toList());
   }

   /*******************************************************************************
    ** No nulls in report cells; unknown and suppressed are distinct from zero.
    *******************************************************************************/
   private static List<String> row(Object... values)
   {
      return (java.util.Arrays.stream(values).map(v -> v == null ? "" : String.valueOf(v)).toList());
   }

   /*******************************************************************************
    ** Render a tuple using the engine's already-authorized display labels.
    *******************************************************************************/
   private static String label(List<PivotHeader> headers)
   {
      return (headers.stream().map(PivotHeader::getLabel).collect(java.util.stream.Collectors.joining(" / ")));
   }

   /*******************************************************************************
    ** Metrics always use their semantic formatted value.
    *******************************************************************************/
   public static String value(Metric metric)
   {
      return (metric == null ? "" : Boolean.TRUE.equals(metric.getSuppressed()) ? "Suppressed" : Objects.toString(metric.getFormatted(), ""));
   }

   /*******************************************************************************
    ** Profile metrics become one row per headline statistic.
    *******************************************************************************/
   private static void metrics(List<List<String>> rows, String section, List<Metric> metrics)
   {
      metrics.forEach(m -> rows.add(row(section, m.getLabel(), value(m))));
   }
}
