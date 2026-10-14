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

/*******************************************************************************
 ** Typed ShiftBucket contract for comparison and relationship analysis.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class ShiftBucket implements Serializable
{
   private Serializable value;
   private String label;
   private Boolean complete = false;
   private List<Metric> metrics = new java.util.ArrayList<>();



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
   public ShiftBucket withValue(Serializable value)
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
   public ShiftBucket withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for complete.
    *******************************************************************************/
   public Boolean getComplete()
   {
      return (complete);
   }



   /*******************************************************************************
    ** Fluent setter for complete.
    *******************************************************************************/
   public ShiftBucket withComplete(Boolean complete)
   {
      this.complete = complete;
      return (this);
   }



   /*******************************************************************************
    ** Getter for metrics.
    *******************************************************************************/
   public List<Metric> getMetrics()
   {
      return (metrics);
   }



   /*******************************************************************************
    ** Fluent setter for metrics.
    *******************************************************************************/
   public ShiftBucket withMetrics(List<Metric> metrics)
   {
      this.metrics = metrics;
      return (this);
   }
}
