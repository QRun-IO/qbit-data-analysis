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
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;

/*******************************************************************************
 ** Typed CrosstabRequest contract for comparison and relationship analysis.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class CrosstabRequest implements AnalysisRequest
{
   private String tableName;
   private QQueryFilter filter;
   private Boolean exact = true;
   private String rowField;
   private String columnField;
   private Integer limit = 20;



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
   public CrosstabRequest withTableName(String tableName)
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
   public CrosstabRequest withFilter(QQueryFilter filter)
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
   public CrosstabRequest withExact(Boolean exact)
   {
      this.exact = exact;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rowField.
    *******************************************************************************/
   public String getRowField()
   {
      return (rowField);
   }



   /*******************************************************************************
    ** Fluent setter for rowField.
    *******************************************************************************/
   public CrosstabRequest withRowField(String rowField)
   {
      this.rowField = rowField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for columnField.
    *******************************************************************************/
   public String getColumnField()
   {
      return (columnField);
   }



   /*******************************************************************************
    ** Fluent setter for columnField.
    *******************************************************************************/
   public CrosstabRequest withColumnField(String columnField)
   {
      this.columnField = columnField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for limit.
    *******************************************************************************/
   public Integer getLimit()
   {
      return (limit);
   }



   /*******************************************************************************
    ** Fluent setter for limit.
    *******************************************************************************/
   public CrosstabRequest withLimit(Integer limit)
   {
      this.limit = limit;
      return (this);
   }
}
