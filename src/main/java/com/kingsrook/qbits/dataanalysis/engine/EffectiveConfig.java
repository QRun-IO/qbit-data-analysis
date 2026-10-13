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

package com.kingsrook.qbits.dataanalysis.engine;


import java.io.Serializable;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonAutoDetect;


/*******************************************************************************
 ** Typed EffectiveConfig contract; independent of any presentation framework.
 *******************************************************************************/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EffectiveConfig implements Serializable
{
   private Boolean enabled = true;
   private Integer maxRows;
   private Integer sampleThresholdRows;
   private Integer sampleRows;
   private Integer minGroupSize;
   private Integer cacheTtlMinutes;
   private Set<String> analyzableFields = Set.of();
   private Set<String> excludedFields = Set.of();



   /*******************************************************************************
    ** Getter for enabled.
    *******************************************************************************/
   public Boolean getEnabled()
   {
      return (enabled);
   }



   /*******************************************************************************
    ** Fluent setter for enabled.
    *******************************************************************************/
   public EffectiveConfig withEnabled(Boolean enabled)
   {
      this.enabled = enabled;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxRows.
    *******************************************************************************/
   public Integer getMaxRows()
   {
      return (maxRows);
   }



   /*******************************************************************************
    ** Fluent setter for maxRows.
    *******************************************************************************/
   public EffectiveConfig withMaxRows(Integer maxRows)
   {
      this.maxRows = maxRows;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sampleThresholdRows.
    *******************************************************************************/
   public Integer getSampleThresholdRows()
   {
      return (sampleThresholdRows);
   }



   /*******************************************************************************
    ** Fluent setter for sampleThresholdRows.
    *******************************************************************************/
   public EffectiveConfig withSampleThresholdRows(Integer sampleThresholdRows)
   {
      this.sampleThresholdRows = sampleThresholdRows;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sampleRows.
    *******************************************************************************/
   public Integer getSampleRows()
   {
      return (sampleRows);
   }



   /*******************************************************************************
    ** Fluent setter for sampleRows.
    *******************************************************************************/
   public EffectiveConfig withSampleRows(Integer sampleRows)
   {
      this.sampleRows = sampleRows;
      return (this);
   }



   /*******************************************************************************
    ** Getter for minGroupSize.
    *******************************************************************************/
   public Integer getMinGroupSize()
   {
      return (minGroupSize);
   }



   /*******************************************************************************
    ** Fluent setter for minGroupSize.
    *******************************************************************************/
   public EffectiveConfig withMinGroupSize(Integer minGroupSize)
   {
      this.minGroupSize = minGroupSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for cacheTtlMinutes.
    *******************************************************************************/
   public Integer getCacheTtlMinutes()
   {
      return (cacheTtlMinutes);
   }



   /*******************************************************************************
    ** Fluent setter for cacheTtlMinutes.
    *******************************************************************************/
   public EffectiveConfig withCacheTtlMinutes(Integer cacheTtlMinutes)
   {
      this.cacheTtlMinutes = cacheTtlMinutes;
      return (this);
   }



   /*******************************************************************************
    ** Getter for analyzableFields.
    *******************************************************************************/
   public Set<String> getAnalyzableFields()
   {
      return (analyzableFields);
   }



   /*******************************************************************************
    ** Fluent setter for analyzableFields.
    *******************************************************************************/
   public EffectiveConfig withAnalyzableFields(Set<String> analyzableFields)
   {
      this.analyzableFields = analyzableFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for excludedFields.
    *******************************************************************************/
   public Set<String> getExcludedFields()
   {
      return (excludedFields);
   }



   /*******************************************************************************
    ** Fluent setter for excludedFields.
    *******************************************************************************/
   public EffectiveConfig withExcludedFields(Set<String> excludedFields)
   {
      this.excludedFields = excludedFields;
      return (this);
   }
}
