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
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

/*******************************************************************************
 ** Typed PivotResult contract.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PivotResult implements Serializable
{
   private List<List<PivotHeader>> rowHeaders = new ArrayList<>();
   private List<List<PivotHeader>> columnHeaders = new ArrayList<>();
   private List<PivotMeasureInfo> measures = new ArrayList<>();
   private List<PivotCell> cells = new ArrayList<>();
   private List<List<Metric>> rowTotals = new ArrayList<>();
   private List<List<Metric>> columnTotals = new ArrayList<>();
   private List<Metric> grandTotals = new ArrayList<>();
   private Boolean truncatedRows = false;
   private Boolean truncatedColumns = false;
   private Boolean estimate = false;
   private List<String> warnings = new ArrayList<>();
   private Instant asOf;


   /*******************************************************************************
    ** Getter for rowHeaders.
    *******************************************************************************/
   public List<List<PivotHeader>> getRowHeaders()
   {
      return (rowHeaders);
   }

   /*******************************************************************************
    ** Fluent setter for rowHeaders.
    *******************************************************************************/
   public PivotResult withRowHeaders(List<List<PivotHeader>> rowHeaders)
   {
      this.rowHeaders = rowHeaders;
      return (this);
   }

   /*******************************************************************************
    ** Getter for columnHeaders.
    *******************************************************************************/
   public List<List<PivotHeader>> getColumnHeaders()
   {
      return (columnHeaders);
   }

   /*******************************************************************************
    ** Fluent setter for columnHeaders.
    *******************************************************************************/
   public PivotResult withColumnHeaders(List<List<PivotHeader>> columnHeaders)
   {
      this.columnHeaders = columnHeaders;
      return (this);
   }

   /*******************************************************************************
    ** Getter for measures.
    *******************************************************************************/
   public List<PivotMeasureInfo> getMeasures()
   {
      return (measures);
   }

   /*******************************************************************************
    ** Fluent setter for measures.
    *******************************************************************************/
   public PivotResult withMeasures(List<PivotMeasureInfo> measures)
   {
      this.measures = measures;
      return (this);
   }

   /*******************************************************************************
    ** Getter for cells.
    *******************************************************************************/
   public List<PivotCell> getCells()
   {
      return (cells);
   }

   /*******************************************************************************
    ** Fluent setter for cells.
    *******************************************************************************/
   public PivotResult withCells(List<PivotCell> cells)
   {
      this.cells = cells;
      return (this);
   }

   /*******************************************************************************
    ** Getter for rowTotals.
    *******************************************************************************/
   public List<List<Metric>> getRowTotals()
   {
      return (rowTotals);
   }

   /*******************************************************************************
    ** Fluent setter for rowTotals.
    *******************************************************************************/
   public PivotResult withRowTotals(List<List<Metric>> rowTotals)
   {
      this.rowTotals = rowTotals;
      return (this);
   }

   /*******************************************************************************
    ** Getter for columnTotals.
    *******************************************************************************/
   public List<List<Metric>> getColumnTotals()
   {
      return (columnTotals);
   }

   /*******************************************************************************
    ** Fluent setter for columnTotals.
    *******************************************************************************/
   public PivotResult withColumnTotals(List<List<Metric>> columnTotals)
   {
      this.columnTotals = columnTotals;
      return (this);
   }

   /*******************************************************************************
    ** Getter for grandTotals.
    *******************************************************************************/
   public List<Metric> getGrandTotals()
   {
      return (grandTotals);
   }

   /*******************************************************************************
    ** Fluent setter for grandTotals.
    *******************************************************************************/
   public PivotResult withGrandTotals(List<Metric> grandTotals)
   {
      this.grandTotals = grandTotals;
      return (this);
   }

   /*******************************************************************************
    ** Getter for truncatedRows.
    *******************************************************************************/
   public Boolean getTruncatedRows()
   {
      return (truncatedRows);
   }

   /*******************************************************************************
    ** Fluent setter for truncatedRows.
    *******************************************************************************/
   public PivotResult withTruncatedRows(Boolean truncatedRows)
   {
      this.truncatedRows = truncatedRows;
      return (this);
   }

   /*******************************************************************************
    ** Getter for truncatedColumns.
    *******************************************************************************/
   public Boolean getTruncatedColumns()
   {
      return (truncatedColumns);
   }

   /*******************************************************************************
    ** Fluent setter for truncatedColumns.
    *******************************************************************************/
   public PivotResult withTruncatedColumns(Boolean truncatedColumns)
   {
      this.truncatedColumns = truncatedColumns;
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
   public PivotResult withEstimate(Boolean estimate)
   {
      this.estimate = estimate;
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
   public PivotResult withWarnings(List<String> warnings)
   {
      this.warnings = warnings;
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
   public PivotResult withAsOf(Instant asOf)
   {
      this.asOf = asOf;
      return (this);
   }
}
