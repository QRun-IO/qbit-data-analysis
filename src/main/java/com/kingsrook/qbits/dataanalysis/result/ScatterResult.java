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
 ** Typed ScatterResult contract for comparison and relationship analysis.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class ScatterResult implements Serializable
{
   private Long n;
   private Integer sampleSize;
   private String sampling;
   private List<Point> points = new java.util.ArrayList<>();
   private TrendLine trend;
   private Chart chart;



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
   public ScatterResult withN(Long n)
   {
      this.n = n;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sampleSize.
    *******************************************************************************/
   public Integer getSampleSize()
   {
      return (sampleSize);
   }



   /*******************************************************************************
    ** Fluent setter for sampleSize.
    *******************************************************************************/
   public ScatterResult withSampleSize(Integer sampleSize)
   {
      this.sampleSize = sampleSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sampling.
    *******************************************************************************/
   public String getSampling()
   {
      return (sampling);
   }



   /*******************************************************************************
    ** Fluent setter for sampling.
    *******************************************************************************/
   public ScatterResult withSampling(String sampling)
   {
      this.sampling = sampling;
      return (this);
   }



   /*******************************************************************************
    ** Getter for points.
    *******************************************************************************/
   public List<Point> getPoints()
   {
      return (points);
   }



   /*******************************************************************************
    ** Fluent setter for points.
    *******************************************************************************/
   public ScatterResult withPoints(List<Point> points)
   {
      this.points = points;
      return (this);
   }



   /*******************************************************************************
    ** Getter for trend.
    *******************************************************************************/
   public TrendLine getTrend()
   {
      return (trend);
   }



   /*******************************************************************************
    ** Fluent setter for trend.
    *******************************************************************************/
   public ScatterResult withTrend(TrendLine trend)
   {
      this.trend = trend;
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
   public ScatterResult withChart(Chart chart)
   {
      this.chart = chart;
      return (this);
   }
}
