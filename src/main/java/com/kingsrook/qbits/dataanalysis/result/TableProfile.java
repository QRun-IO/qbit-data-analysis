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
 ** Typed TableProfile contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TableProfile implements Serializable
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
   private Metric rowCount;
   private List<FieldSummary> fields = new java.util.ArrayList<>();
   private Chart heatmap;
   private List<CandidateKey> candidateKeys = new java.util.ArrayList<>();



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
   public TableProfile withTableName(String tableName)
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
   public TableProfile withFilter(QQueryFilter filter)
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
   public TableProfile withEstimate(Boolean estimate)
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
   public TableProfile withSampleRows(Long sampleRows)
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
   public TableProfile withTruncated(Boolean truncated)
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
   public TableProfile withRowsScanned(Long rowsScanned)
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
   public TableProfile withAsOf(Instant asOf)
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
   public TableProfile withDataVersion(String dataVersion)
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
   public TableProfile withWarnings(List<String> warnings)
   {
      this.warnings = warnings;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rowCount.
    *******************************************************************************/
   public Metric getRowCount()
   {
      return (rowCount);
   }



   /*******************************************************************************
    ** Fluent setter for rowCount.
    *******************************************************************************/
   public TableProfile withRowCount(Metric rowCount)
   {
      this.rowCount = rowCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fields.
    *******************************************************************************/
   public List<FieldSummary> getFields()
   {
      return (fields);
   }



   /*******************************************************************************
    ** Fluent setter for fields.
    *******************************************************************************/
   public TableProfile withFields(List<FieldSummary> fields)
   {
      this.fields = fields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for heatmap.
    *******************************************************************************/
   public Chart getHeatmap()
   {
      return (heatmap);
   }



   /*******************************************************************************
    ** Fluent setter for heatmap.
    *******************************************************************************/
   public TableProfile withHeatmap(Chart heatmap)
   {
      this.heatmap = heatmap;
      return (this);
   }



   /*******************************************************************************
    ** Getter for candidateKeys.
    *******************************************************************************/
   public List<CandidateKey> getCandidateKeys()
   {
      return (candidateKeys);
   }



   /*******************************************************************************
    ** Fluent setter for candidateKeys.
    *******************************************************************************/
   public TableProfile withCandidateKeys(List<CandidateKey> candidateKeys)
   {
      this.candidateKeys = candidateKeys;
      return (this);
   }
}
