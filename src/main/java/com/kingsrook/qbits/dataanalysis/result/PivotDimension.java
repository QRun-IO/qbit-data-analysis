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
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucketUnit;

/*******************************************************************************
 ** Typed PivotDimension contract.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PivotDimension implements Serializable
{
   private String field;
   private DateBucketUnit dateBucket;
   private PivotNumericBins numericBins;


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
   public PivotDimension withField(String field)
   {
      this.field = field;
      return (this);
   }

   /*******************************************************************************
    ** Getter for dateBucket.
    *******************************************************************************/
   public DateBucketUnit getDateBucket()
   {
      return (dateBucket);
   }

   /*******************************************************************************
    ** Fluent setter for dateBucket.
    *******************************************************************************/
   public PivotDimension withDateBucket(DateBucketUnit dateBucket)
   {
      this.dateBucket = dateBucket;
      return (this);
   }

   /*******************************************************************************
    ** Getter for numericBins.
    *******************************************************************************/
   public PivotNumericBins getNumericBins()
   {
      return (numericBins);
   }

   /*******************************************************************************
    ** Fluent setter for numericBins.
    *******************************************************************************/
   public PivotDimension withNumericBins(PivotNumericBins numericBins)
   {
      this.numericBins = numericBins;
      return (this);
   }
}
