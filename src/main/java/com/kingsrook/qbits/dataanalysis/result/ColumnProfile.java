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

package com.kingsrook.qbits.dataanalysis.result;


import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;


/*******************************************************************************
 ** Typed ColumnProfile contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ColumnProfile implements Serializable
{
   private String tableName;
   private QQueryFilter filter;
   private Boolean estimate = false;
   private Long sampleRows = 0L;
   private Boolean truncated = false;
   private Long rowsScanned = 0L;
   private Instant asOf;
   private String dataVersion;
   private List<String> warnings = new java.util.ArrayList<>();
   private String fieldName;
   private String fieldLabel;
   private String fieldType;
   private List<Metric> counts = new java.util.ArrayList<>();
   private NumericProfile numeric;
   private DateProfile dates;
   private TextProfile text;
   private List<Metric> booleans;
   private List<ValueCount> topValues = List.of();
   private Chart chart;



   /*******************************************************************************
    ** Getter for tableName.
    *******************************************************************************/
   public String getTableName()
   {
      return (tableName);
   }



   /*******************************************************************************
    ** Fluent setter for tableName.
    *******************************************************************************/
   public ColumnProfile withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for filter.
    *******************************************************************************/
   public QQueryFilter getFilter()
   {
      return (filter);
   }



   /*******************************************************************************
    ** Fluent setter for filter.
    *******************************************************************************/
   public ColumnProfile withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for estimate.
    *******************************************************************************/
   public Boolean getEstimate()
   {
      return (estimate);
   }



   /*******************************************************************************
    ** Fluent setter for estimate.
    *******************************************************************************/
   public ColumnProfile withEstimate(Boolean estimate)
   {
      this.estimate = estimate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sampleRows.
    *******************************************************************************/
   public Long getSampleRows()
   {
      return (sampleRows);
   }



   /*******************************************************************************
    ** Fluent setter for sampleRows.
    *******************************************************************************/
   public ColumnProfile withSampleRows(Long sampleRows)
   {
      this.sampleRows = sampleRows;
      return (this);
   }



   /*******************************************************************************
    ** Getter for truncated.
    *******************************************************************************/
   public Boolean getTruncated()
   {
      return (truncated);
   }



   /*******************************************************************************
    ** Fluent setter for truncated.
    *******************************************************************************/
   public ColumnProfile withTruncated(Boolean truncated)
   {
      this.truncated = truncated;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rowsScanned.
    *******************************************************************************/
   public Long getRowsScanned()
   {
      return (rowsScanned);
   }



   /*******************************************************************************
    ** Fluent setter for rowsScanned.
    *******************************************************************************/
   public ColumnProfile withRowsScanned(Long rowsScanned)
   {
      this.rowsScanned = rowsScanned;
      return (this);
   }



   /*******************************************************************************
    ** Getter for asOf.
    *******************************************************************************/
   public Instant getAsOf()
   {
      return (asOf);
   }



   /*******************************************************************************
    ** Fluent setter for asOf.
    *******************************************************************************/
   public ColumnProfile withAsOf(Instant asOf)
   {
      this.asOf = asOf;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dataVersion.
    *******************************************************************************/
   public String getDataVersion()
   {
      return (dataVersion);
   }



   /*******************************************************************************
    ** Fluent setter for dataVersion.
    *******************************************************************************/
   public ColumnProfile withDataVersion(String dataVersion)
   {
      this.dataVersion = dataVersion;
      return (this);
   }



   /*******************************************************************************
    ** Getter for warnings.
    *******************************************************************************/
   public List<String> getWarnings()
   {
      return (warnings);
   }



   /*******************************************************************************
    ** Fluent setter for warnings.
    *******************************************************************************/
   public ColumnProfile withWarnings(List<String> warnings)
   {
      this.warnings = warnings;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldName.
    *******************************************************************************/
   public String getFieldName()
   {
      return (fieldName);
   }



   /*******************************************************************************
    ** Fluent setter for fieldName.
    *******************************************************************************/
   public ColumnProfile withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldLabel.
    *******************************************************************************/
   public String getFieldLabel()
   {
      return (fieldLabel);
   }



   /*******************************************************************************
    ** Fluent setter for fieldLabel.
    *******************************************************************************/
   public ColumnProfile withFieldLabel(String fieldLabel)
   {
      this.fieldLabel = fieldLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldType.
    *******************************************************************************/
   public String getFieldType()
   {
      return (fieldType);
   }



   /*******************************************************************************
    ** Fluent setter for fieldType.
    *******************************************************************************/
   public ColumnProfile withFieldType(String fieldType)
   {
      this.fieldType = fieldType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for counts.
    *******************************************************************************/
   public List<Metric> getCounts()
   {
      return (counts);
   }



   /*******************************************************************************
    ** Fluent setter for counts.
    *******************************************************************************/
   public ColumnProfile withCounts(List<Metric> counts)
   {
      this.counts = counts;
      return (this);
   }



   /*******************************************************************************
    ** Getter for numeric.
    *******************************************************************************/
   public NumericProfile getNumeric()
   {
      return (numeric);
   }



   /*******************************************************************************
    ** Fluent setter for numeric.
    *******************************************************************************/
   public ColumnProfile withNumeric(NumericProfile numeric)
   {
      this.numeric = numeric;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dates.
    *******************************************************************************/
   public DateProfile getDates()
   {
      return (dates);
   }



   /*******************************************************************************
    ** Fluent setter for dates.
    *******************************************************************************/
   public ColumnProfile withDates(DateProfile dates)
   {
      this.dates = dates;
      return (this);
   }



   /*******************************************************************************
    ** Getter for text.
    *******************************************************************************/
   public TextProfile getText()
   {
      return (text);
   }



   /*******************************************************************************
    ** Fluent setter for text.
    *******************************************************************************/
   public ColumnProfile withText(TextProfile text)
   {
      this.text = text;
      return (this);
   }



   /*******************************************************************************
    ** Getter for booleans.
    *******************************************************************************/
   public List<Metric> getBooleans()
   {
      return (booleans);
   }



   /*******************************************************************************
    ** Fluent setter for booleans.
    *******************************************************************************/
   public ColumnProfile withBooleans(List<Metric> booleans)
   {
      this.booleans = booleans;
      return (this);
   }



   /*******************************************************************************
    ** Getter for topValues.
    *******************************************************************************/
   public List<ValueCount> getTopValues()
   {
      return (topValues);
   }



   /*******************************************************************************
    ** Fluent setter for topValues.
    *******************************************************************************/
   public ColumnProfile withTopValues(List<ValueCount> topValues)
   {
      this.topValues = topValues;
      return (this);
   }



   /*******************************************************************************
    ** Getter for chart.
    *******************************************************************************/
   public Chart getChart()
   {
      return (chart);
   }



   /*******************************************************************************
    ** Fluent setter for chart.
    *******************************************************************************/
   public ColumnProfile withChart(Chart chart)
   {
      this.chart = chart;
      return (this);
   }
}
