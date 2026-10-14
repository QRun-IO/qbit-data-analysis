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
 ** Typed TrendLine contract for comparison and relationship analysis.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class TrendLine implements Serializable
{
   private Double slope;
   private Double intercept;
   private Double r;



   /*******************************************************************************
    ** Getter for slope.
    *******************************************************************************/
   public Double getSlope()
   {
      return (slope);
   }



   /*******************************************************************************
    ** Fluent setter for slope.
    *******************************************************************************/
   public TrendLine withSlope(Double slope)
   {
      this.slope = slope;
      return (this);
   }



   /*******************************************************************************
    ** Getter for intercept.
    *******************************************************************************/
   public Double getIntercept()
   {
      return (intercept);
   }



   /*******************************************************************************
    ** Fluent setter for intercept.
    *******************************************************************************/
   public TrendLine withIntercept(Double intercept)
   {
      this.intercept = intercept;
      return (this);
   }



   /*******************************************************************************
    ** Getter for r.
    *******************************************************************************/
   public Double getR()
   {
      return (r);
   }



   /*******************************************************************************
    ** Fluent setter for r.
    *******************************************************************************/
   public TrendLine withR(Double r)
   {
      this.r = r;
      return (this);
   }
}
