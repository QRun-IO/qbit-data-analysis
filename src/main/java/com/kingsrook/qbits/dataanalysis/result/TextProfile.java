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
 ** Typed TextProfile contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TextProfile implements Serializable
{
   private List<Metric> metrics = new java.util.ArrayList<>();
   private List<ValueCount> histogram = List.of();
   private List<ValueCount> patterns = List.of();
   private Long sampleRows;



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
   public TextProfile withMetrics(List<Metric> metrics)
   {
      this.metrics = metrics;
      return (this);
   }



   /*******************************************************************************
    ** Getter for histogram.
    *******************************************************************************/
   public List<ValueCount> getHistogram()
   {
      return (histogram);
   }



   /*******************************************************************************
    ** Fluent setter for histogram.
    *******************************************************************************/
   public TextProfile withHistogram(List<ValueCount> histogram)
   {
      this.histogram = histogram;
      return (this);
   }



   /*******************************************************************************
    ** Getter for patterns.
    *******************************************************************************/
   public List<ValueCount> getPatterns()
   {
      return (patterns);
   }



   /*******************************************************************************
    ** Fluent setter for patterns.
    *******************************************************************************/
   public TextProfile withPatterns(List<ValueCount> patterns)
   {
      this.patterns = patterns;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sampleRows.
    *******************************************************************************/
   public Long getSampleRows()
   {
      return (sampleRows);
   }



   /*******************************************************************************
    ** Fluent setter for sampleRows.
    *******************************************************************************/
   public TextProfile withSampleRows(Long sampleRows)
   {
      this.sampleRows = sampleRows;
      return (this);
   }
}
