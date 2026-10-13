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


import java.util.List;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;


/*******************************************************************************
 ** Typed TableProfileRequest contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TableProfileRequest implements AnalysisRequest
{
   private String tableName;
   private QQueryFilter filter;
   private Boolean exact = false;
   private List<String> fields;



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
   public TableProfileRequest withTableName(String tableName)
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
   public TableProfileRequest withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for exact.
    *******************************************************************************/
   public Boolean getExact()
   {
      return (exact);
   }



   /*******************************************************************************
    ** Fluent setter for exact.
    *******************************************************************************/
   public TableProfileRequest withExact(Boolean exact)
   {
      this.exact = exact;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fields.
    *******************************************************************************/
   public List<String> getFields()
   {
      return (fields);
   }



   /*******************************************************************************
    ** Fluent setter for fields.
    *******************************************************************************/
   public TableProfileRequest withFields(List<String> fields)
   {
      this.fields = fields;
      return (this);
   }
}
