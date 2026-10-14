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
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;

/*******************************************************************************
 ** Typed CompareRequest contract for comparison and relationship analysis.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class CompareRequest implements AnalysisRequest
{
   private String tableName;
   private QQueryFilter filter;
   private Boolean exact = true;
   private QQueryFilter filterA;
   private QQueryFilter filterB;
   private String labelA = "A";
   private String labelB = "B";
   private List<String> fields;



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
   public CompareRequest withTableName(String tableName)
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
   public CompareRequest withFilter(QQueryFilter filter)
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
   public CompareRequest withExact(Boolean exact)
   {
      this.exact = exact;
      return (this);
   }



   /*******************************************************************************
    ** Getter for filterA.
    *******************************************************************************/
   public QQueryFilter getFilterA()
   {
      return (filterA);
   }



   /*******************************************************************************
    ** Fluent setter for filterA.
    *******************************************************************************/
   public CompareRequest withFilterA(QQueryFilter filterA)
   {
      this.filterA = filterA;
      return (this);
   }



   /*******************************************************************************
    ** Getter for filterB.
    *******************************************************************************/
   public QQueryFilter getFilterB()
   {
      return (filterB);
   }



   /*******************************************************************************
    ** Fluent setter for filterB.
    *******************************************************************************/
   public CompareRequest withFilterB(QQueryFilter filterB)
   {
      this.filterB = filterB;
      return (this);
   }



   /*******************************************************************************
    ** Getter for labelA.
    *******************************************************************************/
   public String getLabelA()
   {
      return (labelA);
   }



   /*******************************************************************************
    ** Fluent setter for labelA.
    *******************************************************************************/
   public CompareRequest withLabelA(String labelA)
   {
      this.labelA = labelA;
      return (this);
   }



   /*******************************************************************************
    ** Getter for labelB.
    *******************************************************************************/
   public String getLabelB()
   {
      return (labelB);
   }



   /*******************************************************************************
    ** Fluent setter for labelB.
    *******************************************************************************/
   public CompareRequest withLabelB(String labelB)
   {
      this.labelB = labelB;
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
   public CompareRequest withFields(List<String> fields)
   {
      this.fields = fields;
      return (this);
   }
}
