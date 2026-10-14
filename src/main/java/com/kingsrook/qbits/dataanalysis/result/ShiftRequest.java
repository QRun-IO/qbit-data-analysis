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
 ** Typed ShiftRequest contract for comparison and relationship analysis.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class ShiftRequest implements AnalysisRequest
{
   private String tableName;
   private QQueryFilter filter;
   private Boolean exact = true;
   private String field;
   private String dateField;
   private String bucket = "MONTH";



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
   public ShiftRequest withTableName(String tableName)
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
   public ShiftRequest withFilter(QQueryFilter filter)
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
   public ShiftRequest withExact(Boolean exact)
   {
      this.exact = exact;
      return (this);
   }



   /*******************************************************************************
    ** Getter for field.
    *******************************************************************************/
   public String getField()
   {
      return (field);
   }



   /*******************************************************************************
    ** Fluent setter for field.
    *******************************************************************************/
   public ShiftRequest withField(String field)
   {
      this.field = field;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dateField.
    *******************************************************************************/
   public String getDateField()
   {
      return (dateField);
   }



   /*******************************************************************************
    ** Fluent setter for dateField.
    *******************************************************************************/
   public ShiftRequest withDateField(String dateField)
   {
      this.dateField = dateField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bucket.
    *******************************************************************************/
   public String getBucket()
   {
      return (bucket);
   }



   /*******************************************************************************
    ** Fluent setter for bucket.
    *******************************************************************************/
   public ShiftRequest withBucket(String bucket)
   {
      this.bucket = bucket;
      return (this);
   }
}
