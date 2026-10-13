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
 ** Typed Chart contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Chart implements Serializable
{
   private ChartKind kind;
   private List<ChartKind> alternatives = List.of();
   private Axis x;
   private Axis y;
   private List<Series> series = List.of();
   private List<String> annotations = List.of();



   /*******************************************************************************
    ** Getter for kind.
    *******************************************************************************/
   public ChartKind getKind()
   {
      return (kind);
   }



   /*******************************************************************************
    ** Fluent setter for kind.
    *******************************************************************************/
   public Chart withKind(ChartKind kind)
   {
      this.kind = kind;
      return (this);
   }



   /*******************************************************************************
    ** Getter for alternatives.
    *******************************************************************************/
   public List<ChartKind> getAlternatives()
   {
      return (alternatives);
   }



   /*******************************************************************************
    ** Fluent setter for alternatives.
    *******************************************************************************/
   public Chart withAlternatives(List<ChartKind> alternatives)
   {
      this.alternatives = alternatives;
      return (this);
   }



   /*******************************************************************************
    ** Getter for x.
    *******************************************************************************/
   public Axis getX()
   {
      return (x);
   }



   /*******************************************************************************
    ** Fluent setter for x.
    *******************************************************************************/
   public Chart withX(Axis x)
   {
      this.x = x;
      return (this);
   }



   /*******************************************************************************
    ** Getter for y.
    *******************************************************************************/
   public Axis getY()
   {
      return (y);
   }



   /*******************************************************************************
    ** Fluent setter for y.
    *******************************************************************************/
   public Chart withY(Axis y)
   {
      this.y = y;
      return (this);
   }



   /*******************************************************************************
    ** Getter for series.
    *******************************************************************************/
   public List<Series> getSeries()
   {
      return (series);
   }



   /*******************************************************************************
    ** Fluent setter for series.
    *******************************************************************************/
   public Chart withSeries(List<Series> series)
   {
      this.series = series;
      return (this);
   }



   /*******************************************************************************
    ** Getter for annotations.
    *******************************************************************************/
   public List<String> getAnnotations()
   {
      return (annotations);
   }



   /*******************************************************************************
    ** Fluent setter for annotations.
    *******************************************************************************/
   public Chart withAnnotations(List<String> annotations)
   {
      this.annotations = annotations;
      return (this);
   }
}
