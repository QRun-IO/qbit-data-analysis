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

/*******************************************************************************
 ** Typed PivotMeasureInfo contract.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PivotMeasureInfo implements Serializable
{
   private String key;
   private String label;
   private ValueType valueType;


   /*******************************************************************************
    ** Getter for key.
    *******************************************************************************/
   public String getKey()
   {
      return (key);
   }

   /*******************************************************************************
    ** Fluent setter for key.
    *******************************************************************************/
   public PivotMeasureInfo withKey(String key)
   {
      this.key = key;
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
   public PivotMeasureInfo withLabel(String label)
   {
      this.label = label;
      return (this);
   }

   /*******************************************************************************
    ** Getter for valueType.
    *******************************************************************************/
   public ValueType getValueType()
   {
      return (valueType);
   }

   /*******************************************************************************
    ** Fluent setter for valueType.
    *******************************************************************************/
   public PivotMeasureInfo withValueType(ValueType valueType)
   {
      this.valueType = valueType;
      return (this);
   }
}
