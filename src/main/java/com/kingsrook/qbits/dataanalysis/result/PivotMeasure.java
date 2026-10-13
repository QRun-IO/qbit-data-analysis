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
 ** Typed PivotMeasure contract.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PivotMeasure implements Serializable
{
   private Operator operator;
   private String field;
   private BigDecimal percentile;

   /*******************************************************************************
    ** Supported pivot measures. COUNT counts records, regardless of field nulls.
    *******************************************************************************/
   public enum Operator
   {
      COUNT, SUM, AVG, MIN, MAX, COUNT_DISTINCT, PERCENTILE
   }

   /*******************************************************************************
    ** Getter for operator.
    *******************************************************************************/
   public Operator getOperator()
   {
      return (operator);
   }

   /*******************************************************************************
    ** Fluent setter for operator.
    *******************************************************************************/
   public PivotMeasure withOperator(Operator operator)
   {
      this.operator = operator;
      return (this);
   }

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
   public PivotMeasure withField(String field)
   {
      this.field = field;
      return (this);
   }

   /*******************************************************************************
    ** Getter for percentile.
    *******************************************************************************/
   public BigDecimal getPercentile()
   {
      return (percentile);
   }

   /*******************************************************************************
    ** Fluent setter for percentile.
    *******************************************************************************/
   public PivotMeasure withPercentile(BigDecimal percentile)
   {
      this.percentile = percentile;
      return (this);
   }
}
