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

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;

/*******************************************************************************
 ** Typed PivotRequest contract.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PivotRequest implements AnalysisRequest
{
   private String tableName;
   private QQueryFilter filter;
   private Boolean exact = true;
   private List<PivotDimension> rows = new ArrayList<>();
   private List<PivotDimension> columns = new ArrayList<>();
   private List<PivotMeasure> values = new ArrayList<>();
   private Boolean showTotals = true;
   private Integer limitGroups = 50;


   /*******************************************************************************
    ** Getter for tableName.
    *******************************************************************************/
   @Override
   public String getTableName()
   {
      return (tableName);
   }

   /*******************************************************************************
    ** Fluent setter for tableName.
    *******************************************************************************/
   public PivotRequest withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }

   /*******************************************************************************
    ** Getter for filter.
    *******************************************************************************/
   @Override
   public QQueryFilter getFilter()
   {
      return (filter);
   }

   /*******************************************************************************
    ** Fluent setter for filter.
    *******************************************************************************/
   public PivotRequest withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }

   /*******************************************************************************
    ** Getter for exact.
    *******************************************************************************/
   @Override
   public Boolean getExact()
   {
      return (exact);
   }

   /*******************************************************************************
    ** Fluent setter for exact.
    *******************************************************************************/
   public PivotRequest withExact(Boolean exact)
   {
      this.exact = exact;
      return (this);
   }

   /*******************************************************************************
    ** Getter for rows.
    *******************************************************************************/
   public List<PivotDimension> getRows()
   {
      return (rows);
   }

   /*******************************************************************************
    ** Fluent setter for rows.
    *******************************************************************************/
   public PivotRequest withRows(List<PivotDimension> rows)
   {
      this.rows = rows;
      return (this);
   }

   /*******************************************************************************
    ** Getter for columns.
    *******************************************************************************/
   public List<PivotDimension> getColumns()
   {
      return (columns);
   }

   /*******************************************************************************
    ** Fluent setter for columns.
    *******************************************************************************/
   public PivotRequest withColumns(List<PivotDimension> columns)
   {
      this.columns = columns;
      return (this);
   }

   /*******************************************************************************
    ** Getter for values.
    *******************************************************************************/
   public List<PivotMeasure> getValues()
   {
      return (values);
   }

   /*******************************************************************************
    ** Fluent setter for values.
    *******************************************************************************/
   public PivotRequest withValues(List<PivotMeasure> values)
   {
      this.values = values;
      return (this);
   }

   /*******************************************************************************
    ** Getter for showTotals.
    *******************************************************************************/
   public Boolean getShowTotals()
   {
      return (showTotals);
   }

   /*******************************************************************************
    ** Fluent setter for showTotals.
    *******************************************************************************/
   public PivotRequest withShowTotals(Boolean showTotals)
   {
      this.showTotals = showTotals;
      return (this);
   }

   /*******************************************************************************
    ** Getter for limitGroups.
    *******************************************************************************/
   public Integer getLimitGroups()
   {
      return (limitGroups);
   }

   /*******************************************************************************
    ** Fluent setter for limitGroups.
    *******************************************************************************/
   public PivotRequest withLimitGroups(Integer limitGroups)
   {
      this.limitGroups = limitGroups;
      return (this);
   }
}
