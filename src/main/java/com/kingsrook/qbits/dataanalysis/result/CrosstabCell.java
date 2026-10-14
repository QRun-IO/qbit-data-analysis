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

/*******************************************************************************
 ** Typed CrosstabCell contract for comparison and relationship analysis.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class CrosstabCell implements Serializable
{
   private Integer rowIndex;
   private Integer columnIndex;
   private Metric count;
   private Metric rowShare;
   private Metric columnShare;



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
   public CrosstabCell withRowIndex(Integer rowIndex)
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
   public CrosstabCell withColumnIndex(Integer columnIndex)
   {
      this.columnIndex = columnIndex;
      return (this);
   }



   /*******************************************************************************
    ** Getter for count.
    *******************************************************************************/
   public Metric getCount()
   {
      return (count);
   }



   /*******************************************************************************
    ** Fluent setter for count.
    *******************************************************************************/
   public CrosstabCell withCount(Metric count)
   {
      this.count = count;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rowShare.
    *******************************************************************************/
   public Metric getRowShare()
   {
      return (rowShare);
   }



   /*******************************************************************************
    ** Fluent setter for rowShare.
    *******************************************************************************/
   public CrosstabCell withRowShare(Metric rowShare)
   {
      this.rowShare = rowShare;
      return (this);
   }



   /*******************************************************************************
    ** Getter for columnShare.
    *******************************************************************************/
   public Metric getColumnShare()
   {
      return (columnShare);
   }



   /*******************************************************************************
    ** Fluent setter for columnShare.
    *******************************************************************************/
   public CrosstabCell withColumnShare(Metric columnShare)
   {
      this.columnShare = columnShare;
      return (this);
   }
}
