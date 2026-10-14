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
 ** Typed Metric contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Metric implements Serializable
{
   private Boolean estimate = false;
   private String key;
   private String label;
   private Serializable value;
   private ValueType valueType;
   private String formatted;
   private Drill drill;
   private Boolean suppressed = false;
   private String suppressedReason;



   /*******************************************************************************
    ** Whether the metric describes a sampled estimate.
    *******************************************************************************/
   public Boolean getEstimate()
   {
      return (estimate);
   }

   /*******************************************************************************
    ** Mark an estimated metric independently of the exact population row total.
    *******************************************************************************/
   public Metric withEstimate(Boolean estimate)
   {
      this.estimate = estimate;
      return (this);
   }

   /*******************************************************************************
    ** Getter for key.
    *******************************************************************************/
   public String getKey()
   {
      return (key);
   }



   /*******************************************************************************
    ** Fluent setter for key.
    *******************************************************************************/
   public Metric withKey(String key)
   {
      this.key = key;
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
   public Metric withLabel(String label)
   {
      this.label = label;
      return (this);
   }



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
   public Metric withValue(Serializable value)
   {
      this.value = value;
      return (this);
   }



   /*******************************************************************************
    ** Getter for valueType.
    *******************************************************************************/
   public ValueType getValueType()
   {
      return (valueType);
   }



   /*******************************************************************************
    ** Fluent setter for valueType.
    *******************************************************************************/
   public Metric withValueType(ValueType valueType)
   {
      this.valueType = valueType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for formatted.
    *******************************************************************************/
   public String getFormatted()
   {
      return (formatted);
   }



   /*******************************************************************************
    ** Fluent setter for formatted.
    *******************************************************************************/
   public Metric withFormatted(String formatted)
   {
      this.formatted = formatted;
      return (this);
   }



   /*******************************************************************************
    ** Getter for drill.
    *******************************************************************************/
   public Drill getDrill()
   {
      return (drill);
   }



   /*******************************************************************************
    ** Fluent setter for drill.
    *******************************************************************************/
   public Metric withDrill(Drill drill)
   {
      this.drill = drill;
      return (this);
   }



   /*******************************************************************************
    ** Getter for suppressed.
    *******************************************************************************/
   public Boolean getSuppressed()
   {
      return (suppressed);
   }



   /*******************************************************************************
    ** Fluent setter for suppressed.
    *******************************************************************************/
   public Metric withSuppressed(Boolean suppressed)
   {
      this.suppressed = suppressed;
      return (this);
   }



   /*******************************************************************************
    ** Getter for suppressedReason.
    *******************************************************************************/
   public String getSuppressedReason()
   {
      return (suppressedReason);
   }



   /*******************************************************************************
    ** Fluent setter for suppressedReason.
    *******************************************************************************/
   public Metric withSuppressedReason(String suppressedReason)
   {
      this.suppressedReason = suppressedReason;
      return (this);
   }
}
