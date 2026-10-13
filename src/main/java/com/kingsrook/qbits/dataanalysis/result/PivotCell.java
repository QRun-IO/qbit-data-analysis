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
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

/*******************************************************************************
 ** Typed PivotCell contract.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PivotCell implements Serializable
{
   private Integer rowIndex;
   private Integer columnIndex;
   private List<Metric> values = new ArrayList<>();


   /*******************************************************************************
    ** Getter for rowIndex.
    *******************************************************************************/
   public Integer getRowIndex()
   {
      return (rowIndex);
   }

   /*******************************************************************************
    ** Fluent setter for rowIndex.
    *******************************************************************************/
   public PivotCell withRowIndex(Integer rowIndex)
   {
      this.rowIndex = rowIndex;
      return (this);
   }

   /*******************************************************************************
    ** Getter for columnIndex.
    *******************************************************************************/
   public Integer getColumnIndex()
   {
      return (columnIndex);
   }

   /*******************************************************************************
    ** Fluent setter for columnIndex.
    *******************************************************************************/
   public PivotCell withColumnIndex(Integer columnIndex)
   {
      this.columnIndex = columnIndex;
      return (this);
   }

   /*******************************************************************************
    ** Getter for values.
    *******************************************************************************/
   public List<Metric> getValues()
   {
      return (values);
   }

   /*******************************************************************************
    ** Fluent setter for values.
    *******************************************************************************/
   public PivotCell withValues(List<Metric> values)
   {
      this.values = values;
      return (this);
   }
}
