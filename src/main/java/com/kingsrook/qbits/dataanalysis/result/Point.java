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
 ** Typed Point contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Point implements Serializable
{
   private Serializable x;
   private BigDecimal y;
   private String label;
   private Drill drill;



   /*******************************************************************************
    ** Getter for x.
    *******************************************************************************/
   public Serializable getX()
   {
      return (x);
   }



   /*******************************************************************************
    ** Fluent setter for x.
    *******************************************************************************/
   public Point withX(Serializable x)
   {
      this.x = x;
      return (this);
   }



   /*******************************************************************************
    ** Getter for y.
    *******************************************************************************/
   public BigDecimal getY()
   {
      return (y);
   }



   /*******************************************************************************
    ** Fluent setter for y.
    *******************************************************************************/
   public Point withY(BigDecimal y)
   {
      this.y = y;
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
   public Point withLabel(String label)
   {
      this.label = label;
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
   public Point withDrill(Drill drill)
   {
      this.drill = drill;
      return (this);
   }
}
