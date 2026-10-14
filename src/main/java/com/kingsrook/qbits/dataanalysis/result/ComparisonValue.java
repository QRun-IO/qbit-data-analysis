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
 ** Typed ComparisonValue contract for comparison and relationship analysis.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class ComparisonValue implements Serializable
{
   private Serializable value;
   private String label;
   private ComparisonSide sideA;
   private ComparisonSide sideB;
   private Metric delta;
   private Metric percentDelta;
   private Significance significance;



   /*******************************************************************************
    ** Getter for value.
    *******************************************************************************/
   public Serializable getValue()
   {
      return (value);
   }



   /*******************************************************************************
    ** Fluent setter for value.
    *******************************************************************************/
   public ComparisonValue withValue(Serializable value)
   {
      this.value = value;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label.
    *******************************************************************************/
   public String getLabel()
   {
      return (label);
   }



   /*******************************************************************************
    ** Fluent setter for label.
    *******************************************************************************/
   public ComparisonValue withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sideA.
    *******************************************************************************/
   public ComparisonSide getSideA()
   {
      return (sideA);
   }



   /*******************************************************************************
    ** Fluent setter for sideA.
    *******************************************************************************/
   public ComparisonValue withSideA(ComparisonSide sideA)
   {
      this.sideA = sideA;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sideB.
    *******************************************************************************/
   public ComparisonSide getSideB()
   {
      return (sideB);
   }



   /*******************************************************************************
    ** Fluent setter for sideB.
    *******************************************************************************/
   public ComparisonValue withSideB(ComparisonSide sideB)
   {
      this.sideB = sideB;
      return (this);
   }



   /*******************************************************************************
    ** Getter for delta.
    *******************************************************************************/
   public Metric getDelta()
   {
      return (delta);
   }



   /*******************************************************************************
    ** Fluent setter for delta.
    *******************************************************************************/
   public ComparisonValue withDelta(Metric delta)
   {
      this.delta = delta;
      return (this);
   }



   /*******************************************************************************
    ** Getter for percentDelta.
    *******************************************************************************/
   public Metric getPercentDelta()
   {
      return (percentDelta);
   }



   /*******************************************************************************
    ** Fluent setter for percentDelta.
    *******************************************************************************/
   public ComparisonValue withPercentDelta(Metric percentDelta)
   {
      this.percentDelta = percentDelta;
      return (this);
   }



   /*******************************************************************************
    ** Getter for significance.
    *******************************************************************************/
   public Significance getSignificance()
   {
      return (significance);
   }



   /*******************************************************************************
    ** Fluent setter for significance.
    *******************************************************************************/
   public ComparisonValue withSignificance(Significance significance)
   {
      this.significance = significance;
      return (this);
   }
}
