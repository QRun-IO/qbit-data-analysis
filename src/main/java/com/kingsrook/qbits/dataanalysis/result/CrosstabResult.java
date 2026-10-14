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
 ** Typed CrosstabResult contract for comparison and relationship analysis.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class CrosstabResult implements Serializable
{
   private List<PivotHeader> rows = new java.util.ArrayList<>();
   private List<PivotHeader> columns = new java.util.ArrayList<>();
   private List<CrosstabCell> cells = new java.util.ArrayList<>();
   private Significance significance;



   /*******************************************************************************
    ** Getter for rows.
    *******************************************************************************/
   public List<PivotHeader> getRows()
   {
      return (rows);
   }



   /*******************************************************************************
    ** Fluent setter for rows.
    *******************************************************************************/
   public CrosstabResult withRows(List<PivotHeader> rows)
   {
      this.rows = rows;
      return (this);
   }



   /*******************************************************************************
    ** Getter for columns.
    *******************************************************************************/
   public List<PivotHeader> getColumns()
   {
      return (columns);
   }



   /*******************************************************************************
    ** Fluent setter for columns.
    *******************************************************************************/
   public CrosstabResult withColumns(List<PivotHeader> columns)
   {
      this.columns = columns;
      return (this);
   }



   /*******************************************************************************
    ** Getter for cells.
    *******************************************************************************/
   public List<CrosstabCell> getCells()
   {
      return (cells);
   }



   /*******************************************************************************
    ** Fluent setter for cells.
    *******************************************************************************/
   public CrosstabResult withCells(List<CrosstabCell> cells)
   {
      this.cells = cells;
      return (this);
   }



   /*******************************************************************************
    ** Getter for significance.
    *******************************************************************************/
   public Significance getSignificance()
   {
      return (significance);
   }



   /*******************************************************************************
    ** Fluent setter for significance.
    *******************************************************************************/
   public CrosstabResult withSignificance(Significance significance)
   {
      this.significance = significance;
      return (this);
   }
}
