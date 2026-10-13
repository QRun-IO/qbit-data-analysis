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
 ** Typed DateProfile contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DateProfile implements Serializable
{
   private List<Metric> metrics = new java.util.ArrayList<>();
   private String unit;
   private List<ValueCount> buckets = List.of();
   private List<ValueCount> gaps = List.of();
   private List<ValueCount> dayOfWeek = List.of();
   private List<ValueCount> hourOfDay = List.of();



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
   public DateProfile withMetrics(List<Metric> metrics)
   {
      this.metrics = metrics;
      return (this);
   }



   /*******************************************************************************
    ** Getter for unit.
    *******************************************************************************/
   public String getUnit()
   {
      return (unit);
   }



   /*******************************************************************************
    ** Fluent setter for unit.
    *******************************************************************************/
   public DateProfile withUnit(String unit)
   {
      this.unit = unit;
      return (this);
   }



   /*******************************************************************************
    ** Getter for buckets.
    *******************************************************************************/
   public List<ValueCount> getBuckets()
   {
      return (buckets);
   }



   /*******************************************************************************
    ** Fluent setter for buckets.
    *******************************************************************************/
   public DateProfile withBuckets(List<ValueCount> buckets)
   {
      this.buckets = buckets;
      return (this);
   }



   /*******************************************************************************
    ** Getter for gaps.
    *******************************************************************************/
   public List<ValueCount> getGaps()
   {
      return (gaps);
   }



   /*******************************************************************************
    ** Fluent setter for gaps.
    *******************************************************************************/
   public DateProfile withGaps(List<ValueCount> gaps)
   {
      this.gaps = gaps;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dayOfWeek.
    *******************************************************************************/
   public List<ValueCount> getDayOfWeek()
   {
      return (dayOfWeek);
   }



   /*******************************************************************************
    ** Fluent setter for dayOfWeek.
    *******************************************************************************/
   public DateProfile withDayOfWeek(List<ValueCount> dayOfWeek)
   {
      this.dayOfWeek = dayOfWeek;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hourOfDay.
    *******************************************************************************/
   public List<ValueCount> getHourOfDay()
   {
      return (hourOfDay);
   }



   /*******************************************************************************
    ** Fluent setter for hourOfDay.
    *******************************************************************************/
   public DateProfile withHourOfDay(List<ValueCount> hourOfDay)
   {
      this.hourOfDay = hourOfDay;
      return (this);
   }
}
