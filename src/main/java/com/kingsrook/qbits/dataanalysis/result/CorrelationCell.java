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
 ** Typed CorrelationCell contract for comparison and relationship analysis.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class CorrelationCell implements Serializable
{
   private String xField;
   private String yField;
   private Long n;
   private Double r;
   private ScatterRequest scatter;



   /*******************************************************************************
    ** Getter for xField.
    *******************************************************************************/
   public String getXField()
   {
      return (xField);
   }



   /*******************************************************************************
    ** Fluent setter for xField.
    *******************************************************************************/
   public CorrelationCell withXField(String xField)
   {
      this.xField = xField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for yField.
    *******************************************************************************/
   public String getYField()
   {
      return (yField);
   }



   /*******************************************************************************
    ** Fluent setter for yField.
    *******************************************************************************/
   public CorrelationCell withYField(String yField)
   {
      this.yField = yField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for n.
    *******************************************************************************/
   public Long getN()
   {
      return (n);
   }



   /*******************************************************************************
    ** Fluent setter for n.
    *******************************************************************************/
   public CorrelationCell withN(Long n)
   {
      this.n = n;
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
   public CorrelationCell withR(Double r)
   {
      this.r = r;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scatter.
    *******************************************************************************/
   public ScatterRequest getScatter()
   {
      return (scatter);
   }



   /*******************************************************************************
    ** Fluent setter for scatter.
    *******************************************************************************/
   public CorrelationCell withScatter(ScatterRequest scatter)
   {
      this.scatter = scatter;
      return (this);
   }
}
