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
 ** Typed ShiftResult contract for comparison and relationship analysis.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class ShiftResult implements Serializable
{
   private List<ShiftBucket> buckets = new java.util.ArrayList<>();
   private Metric psi;
   private String sentence;
   private Chart chart;



   /*******************************************************************************
    ** Getter for buckets.
    *******************************************************************************/
   public List<ShiftBucket> getBuckets()
   {
      return (buckets);
   }



   /*******************************************************************************
    ** Fluent setter for buckets.
    *******************************************************************************/
   public ShiftResult withBuckets(List<ShiftBucket> buckets)
   {
      this.buckets = buckets;
      return (this);
   }



   /*******************************************************************************
    ** Getter for psi.
    *******************************************************************************/
   public Metric getPsi()
   {
      return (psi);
   }



   /*******************************************************************************
    ** Fluent setter for psi.
    *******************************************************************************/
   public ShiftResult withPsi(Metric psi)
   {
      this.psi = psi;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sentence.
    *******************************************************************************/
   public String getSentence()
   {
      return (sentence);
   }



   /*******************************************************************************
    ** Fluent setter for sentence.
    *******************************************************************************/
   public ShiftResult withSentence(String sentence)
   {
      this.sentence = sentence;
      return (this);
   }



   /*******************************************************************************
    ** Getter for chart.
    *******************************************************************************/
   public Chart getChart()
   {
      return (chart);
   }



   /*******************************************************************************
    ** Fluent setter for chart.
    *******************************************************************************/
   public ShiftResult withChart(Chart chart)
   {
      this.chart = chart;
      return (this);
   }
}
