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
 ** Typed ValueCount contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ValueCount implements Serializable
{
   private Serializable value;
   private String label;
   private Long count;
   private BigDecimal share;
   private Drill drill;



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
   public ValueCount withValue(Serializable value)
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
   public ValueCount withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for count.
    *******************************************************************************/
   public Long getCount()
   {
      return (count);
   }



   /*******************************************************************************
    ** Fluent setter for count.
    *******************************************************************************/
   public ValueCount withCount(Long count)
   {
      this.count = count;
      return (this);
   }



   /*******************************************************************************
    ** Getter for share.
    *******************************************************************************/
   public BigDecimal getShare()
   {
      return (share);
   }



   /*******************************************************************************
    ** Fluent setter for share.
    *******************************************************************************/
   public ValueCount withShare(BigDecimal share)
   {
      this.share = share;
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
   public ValueCount withDrill(Drill drill)
   {
      this.drill = drill;
      return (this);
   }
}
