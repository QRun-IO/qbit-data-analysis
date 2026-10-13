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
 ** Typed Series contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Series implements Serializable
{
   private String name;
   private List<Point> points = new java.util.ArrayList<>();



   /*******************************************************************************
    ** Getter for name.
    *******************************************************************************/
   public String getName()
   {
      return (name);
   }



   /*******************************************************************************
    ** Fluent setter for name.
    *******************************************************************************/
   public Series withName(String name)
   {
      this.name = name;
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
   public Series withPoints(List<Point> points)
   {
      this.points = points;
      return (this);
   }
}
