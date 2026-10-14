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
 ** Typed CorrelationResult contract for comparison and relationship analysis.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class CorrelationResult implements Serializable
{
   private List<String> fields;
   private List<CorrelationCell> cells = new java.util.ArrayList<>();
   private Chart chart;



   /*******************************************************************************
    ** Getter for fields.
    *******************************************************************************/
   public List<String> getFields()
   {
      return (fields);
   }



   /*******************************************************************************
    ** Fluent setter for fields.
    *******************************************************************************/
   public CorrelationResult withFields(List<String> fields)
   {
      this.fields = fields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for cells.
    *******************************************************************************/
   public List<CorrelationCell> getCells()
   {
      return (cells);
   }



   /*******************************************************************************
    ** Fluent setter for cells.
    *******************************************************************************/
   public CorrelationResult withCells(List<CorrelationCell> cells)
   {
      this.cells = cells;
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
   public CorrelationResult withChart(Chart chart)
   {
      this.chart = chart;
      return (this);
   }
}
