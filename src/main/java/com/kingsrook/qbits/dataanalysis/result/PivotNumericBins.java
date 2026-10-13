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
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

/*******************************************************************************
 ** Typed PivotNumericBins contract.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PivotNumericBins implements Serializable
{
   private Integer count = 20;
   private BigDecimal min;
   private BigDecimal max;


   /*******************************************************************************
    ** Getter for count.
    *******************************************************************************/
   public Integer getCount()
   {
      return (count);
   }

   /*******************************************************************************
    ** Fluent setter for count.
    *******************************************************************************/
   public PivotNumericBins withCount(Integer count)
   {
      this.count = count;
      return (this);
   }

   /*******************************************************************************
    ** Getter for min.
    *******************************************************************************/
   public BigDecimal getMin()
   {
      return (min);
   }

   /*******************************************************************************
    ** Fluent setter for min.
    *******************************************************************************/
   public PivotNumericBins withMin(BigDecimal min)
   {
      this.min = min;
      return (this);
   }

   /*******************************************************************************
    ** Getter for max.
    *******************************************************************************/
   public BigDecimal getMax()
   {
      return (max);
   }

   /*******************************************************************************
    ** Fluent setter for max.
    *******************************************************************************/
   public PivotNumericBins withMax(BigDecimal max)
   {
      this.max = max;
      return (this);
   }
}
