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
 ** Typed FieldSummary contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FieldSummary implements Serializable
{
   private String field;
   private String label;
   private String type;
   private BigDecimal completeness;
   private Long distinct;
   private BigDecimal uniqueness;
   private Boolean constant;
   private Boolean empty;
   private Metric nulls;
   private Metric blanks;



   /*******************************************************************************
    ** Getter for field.
    *******************************************************************************/
   public String getField()
   {
      return (field);
   }



   /*******************************************************************************
    ** Fluent setter for field.
    *******************************************************************************/
   public FieldSummary withField(String field)
   {
      this.field = field;
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
   public FieldSummary withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type.
    *******************************************************************************/
   public String getType()
   {
      return (type);
   }



   /*******************************************************************************
    ** Fluent setter for type.
    *******************************************************************************/
   public FieldSummary withType(String type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for completeness.
    *******************************************************************************/
   public BigDecimal getCompleteness()
   {
      return (completeness);
   }



   /*******************************************************************************
    ** Fluent setter for completeness.
    *******************************************************************************/
   public FieldSummary withCompleteness(BigDecimal completeness)
   {
      this.completeness = completeness;
      return (this);
   }



   /*******************************************************************************
    ** Getter for distinct.
    *******************************************************************************/
   public Long getDistinct()
   {
      return (distinct);
   }



   /*******************************************************************************
    ** Fluent setter for distinct.
    *******************************************************************************/
   public FieldSummary withDistinct(Long distinct)
   {
      this.distinct = distinct;
      return (this);
   }



   /*******************************************************************************
    ** Getter for uniqueness.
    *******************************************************************************/
   public BigDecimal getUniqueness()
   {
      return (uniqueness);
   }



   /*******************************************************************************
    ** Fluent setter for uniqueness.
    *******************************************************************************/
   public FieldSummary withUniqueness(BigDecimal uniqueness)
   {
      this.uniqueness = uniqueness;
      return (this);
   }



   /*******************************************************************************
    ** Getter for constant.
    *******************************************************************************/
   public Boolean getConstant()
   {
      return (constant);
   }



   /*******************************************************************************
    ** Fluent setter for constant.
    *******************************************************************************/
   public FieldSummary withConstant(Boolean constant)
   {
      this.constant = constant;
      return (this);
   }



   /*******************************************************************************
    ** Getter for empty.
    *******************************************************************************/
   public Boolean getEmpty()
   {
      return (empty);
   }



   /*******************************************************************************
    ** Fluent setter for empty.
    *******************************************************************************/
   public FieldSummary withEmpty(Boolean empty)
   {
      this.empty = empty;
      return (this);
   }



   /*******************************************************************************
    ** Getter for nulls.
    *******************************************************************************/
   public Metric getNulls()
   {
      return (nulls);
   }



   /*******************************************************************************
    ** Fluent setter for nulls.
    *******************************************************************************/
   public FieldSummary withNulls(Metric nulls)
   {
      this.nulls = nulls;
      return (this);
   }



   /*******************************************************************************
    ** Getter for blanks.
    *******************************************************************************/
   public Metric getBlanks()
   {
      return (blanks);
   }



   /*******************************************************************************
    ** Fluent setter for blanks.
    *******************************************************************************/
   public FieldSummary withBlanks(Metric blanks)
   {
      this.blanks = blanks;
      return (this);
   }
}
