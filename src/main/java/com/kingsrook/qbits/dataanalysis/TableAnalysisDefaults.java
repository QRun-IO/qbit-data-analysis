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

package com.kingsrook.qbits.dataanalysis;


import java.io.Serializable;
import java.util.List;
import java.util.Set;


/*******************************************************************************
 ** Code configuration for Data Analysis; null table overrides inherit defaults.
 *******************************************************************************/
public class TableAnalysisDefaults implements Serializable
{
   private Boolean enabled;
   private Integer maxRows;
   private Integer sampleThresholdRows;
   private Integer sampleRows;
   private Integer minGroupSize;
   private Integer cacheTtlMinutes;
   private Set<String> analyzableFields;
   private Set<String> excludedFields;



   /*******************************************************************************
    ** Reject negative overrides while allowing null to inherit.
    *******************************************************************************/
   public void validate(String prefix, List<String> errors)
   {
      if(maxRows != null && maxRows < 0)
      {
         errors.add(prefix + "maxRows must be non-negative");
      }
      if(sampleThresholdRows != null && sampleThresholdRows < 0)
      {
         errors.add(prefix + "sampleThresholdRows must be non-negative");
      }
      if(sampleRows != null && sampleRows < 0)
      {
         errors.add(prefix + "sampleRows must be non-negative");
      }
      if(minGroupSize != null && minGroupSize < 0)
      {
         errors.add(prefix + "minGroupSize must be non-negative");
      }
      if(cacheTtlMinutes != null && cacheTtlMinutes < 0)
      {
         errors.add(prefix + "cacheTtlMinutes must be non-negative");
      }
   }



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
   public TableAnalysisDefaults withEnabled(Boolean enabled)
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
   public TableAnalysisDefaults withMaxRows(Integer maxRows)
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
   public TableAnalysisDefaults withSampleThresholdRows(Integer sampleThresholdRows)
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
   public TableAnalysisDefaults withSampleRows(Integer sampleRows)
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
   public TableAnalysisDefaults withMinGroupSize(Integer minGroupSize)
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
   public TableAnalysisDefaults withCacheTtlMinutes(Integer cacheTtlMinutes)
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
   public TableAnalysisDefaults withAnalyzableFields(Set<String> analyzableFields)
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
   public TableAnalysisDefaults withExcludedFields(Set<String> excludedFields)
   {
      this.excludedFields = excludedFields;
      return (this);
   }
}
