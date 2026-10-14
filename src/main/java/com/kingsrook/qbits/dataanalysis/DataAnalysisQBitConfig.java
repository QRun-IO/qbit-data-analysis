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


import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import com.kingsrook.qbits.dataanalysis.engine.CoreTierFieldAccessResolver;
import com.kingsrook.qbits.dataanalysis.engine.FieldAnalysisAccessResolver;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;


/*******************************************************************************
 ** Code configuration for Data Analysis; null table overrides inherit defaults.
 *******************************************************************************/
public class DataAnalysisQBitConfig implements QBitConfig
{
   private Supplier<FieldAnalysisAccessResolver> fieldAnalysisAccessResolver = CoreTierFieldAccessResolver::defaultResolver;
   private transient java.util.function.Function<String, com.kingsrook.qqq.backend.core.model.session.QSession> ownerSessionSupplier;
   private String backendName;
   private String schedulerName;
   private String appName = "dataAnalysis";
   private String appLabel = "Analyses";
   private String parentAppName;
   private Integer defaultMaxRows = 1_000_000;
   private Integer defaultSampleThresholdRows = 1_000_000;
   private Integer defaultSampleRows = 100_000;
   private Integer defaultMinGroupSize = 0;
   private Integer defaultCacheTtlMinutes = 60;
   private Set<String> includeTables = new LinkedHashSet<>();
   private Set<String> excludeTables = new LinkedHashSet<>();
   private Map<String, TableAnalysisDefaults> tableDefaults = new LinkedHashMap<>();



   public static final String TABLE_NAME_PREFIX = "data_analysis_";
   public static final String PERMISSION_BASE_NAME = "data_analysis";



   /*******************************************************************************
    ** Validate the backend and numeric defaults, collecting all errors.
    *******************************************************************************/
   @Override
   public void validate(QInstance instance, List<String> errors)
   {
      if(backendName == null || instance.getBackend(backendName) == null)
      {
         errors.add("backendName must name an existing backend");
      }
      if(defaultMaxRows == null || defaultMaxRows < 0)
      {
         errors.add("defaultMaxRows must be non-negative");
      }
      if(defaultSampleThresholdRows == null || defaultSampleThresholdRows < 0)
      {
         errors.add("defaultSampleThresholdRows must be non-negative");
      }
      if(defaultSampleRows == null || defaultSampleRows < 0)
      {
         errors.add("defaultSampleRows must be non-negative");
      }
      if(defaultMinGroupSize == null || defaultMinGroupSize < 0)
      {
         errors.add("defaultMinGroupSize must be non-negative");
      }
      if(defaultCacheTtlMinutes == null || defaultCacheTtlMinutes < 0)
      {
         errors.add("defaultCacheTtlMinutes must be non-negative");
      }
      if(tableDefaults == null)
      {
         errors.add("tableDefaults must not be null");
         return;
      }
      for(Map.Entry<String, TableAnalysisDefaults> entry : tableDefaults.entrySet())
      {
         if(entry.getValue() == null)
         {
            errors.add("tableDefaults[" + entry.getKey() + "] must not be null");
         }
         else
         {
            entry.getValue().validate("tableDefaults[" + entry.getKey() + "].", errors);
         }
      }
   }



   /*******************************************************************************
    ** The backend connection used by the qbit tables.
    *******************************************************************************/
   @Override
   public String getDefaultBackendNameForTables()
   {
      return (backendName);
   }



   /*******************************************************************************
    ** Getter for backendName.
    *******************************************************************************/
   public String getBackendName()
   {
      return (backendName);
   }



   /*******************************************************************************
    ** Fluent setter for backendName.
    *******************************************************************************/
   public DataAnalysisQBitConfig withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for schedulerName.
    *******************************************************************************/
   public String getSchedulerName()
   {
      return (schedulerName);
   }



   /*******************************************************************************
    ** Fluent setter for schedulerName.
    *******************************************************************************/
   public DataAnalysisQBitConfig withSchedulerName(String schedulerName)
   {
      this.schedulerName = schedulerName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for appName.
    *******************************************************************************/
   public String getAppName()
   {
      return (appName);
   }



   /*******************************************************************************
    ** Fluent setter for appName.
    *******************************************************************************/
   public DataAnalysisQBitConfig withAppName(String appName)
   {
      this.appName = appName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for appLabel.
    *******************************************************************************/
   public String getAppLabel()
   {
      return (appLabel);
   }



   /*******************************************************************************
    ** Fluent setter for appLabel.
    *******************************************************************************/
   public DataAnalysisQBitConfig withAppLabel(String appLabel)
   {
      this.appLabel = appLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for parentAppName.
    *******************************************************************************/
   public String getParentAppName()
   {
      return (parentAppName);
   }



   /*******************************************************************************
    ** Fluent setter for parentAppName.
    *******************************************************************************/
   public DataAnalysisQBitConfig withParentAppName(String parentAppName)
   {
      this.parentAppName = parentAppName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultMaxRows.
    *******************************************************************************/
   public Integer getDefaultMaxRows()
   {
      return (defaultMaxRows);
   }



   /*******************************************************************************
    ** Fluent setter for defaultMaxRows.
    *******************************************************************************/
   public DataAnalysisQBitConfig withDefaultMaxRows(Integer defaultMaxRows)
   {
      this.defaultMaxRows = defaultMaxRows;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultSampleThresholdRows.
    *******************************************************************************/
   public Integer getDefaultSampleThresholdRows()
   {
      return (defaultSampleThresholdRows);
   }



   /*******************************************************************************
    ** Fluent setter for defaultSampleThresholdRows.
    *******************************************************************************/
   public DataAnalysisQBitConfig withDefaultSampleThresholdRows(Integer defaultSampleThresholdRows)
   {
      this.defaultSampleThresholdRows = defaultSampleThresholdRows;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultSampleRows.
    *******************************************************************************/
   public Integer getDefaultSampleRows()
   {
      return (defaultSampleRows);
   }



   /*******************************************************************************
    ** Fluent setter for defaultSampleRows.
    *******************************************************************************/
   public DataAnalysisQBitConfig withDefaultSampleRows(Integer defaultSampleRows)
   {
      this.defaultSampleRows = defaultSampleRows;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultMinGroupSize.
    *******************************************************************************/
   public Integer getDefaultMinGroupSize()
   {
      return (defaultMinGroupSize);
   }



   /*******************************************************************************
    ** Fluent setter for defaultMinGroupSize.
    *******************************************************************************/
   public DataAnalysisQBitConfig withDefaultMinGroupSize(Integer defaultMinGroupSize)
   {
      this.defaultMinGroupSize = defaultMinGroupSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultCacheTtlMinutes.
    *******************************************************************************/
   public Integer getDefaultCacheTtlMinutes()
   {
      return (defaultCacheTtlMinutes);
   }



   /*******************************************************************************
    ** Fluent setter for defaultCacheTtlMinutes.
    *******************************************************************************/
   public DataAnalysisQBitConfig withDefaultCacheTtlMinutes(Integer defaultCacheTtlMinutes)
   {
      this.defaultCacheTtlMinutes = defaultCacheTtlMinutes;
      return (this);
   }



   /*******************************************************************************
    ** Getter for includeTables.
    *******************************************************************************/
   public Set<String> getIncludeTables()
   {
      return (includeTables);
   }



   /*******************************************************************************
    ** Fluent setter for includeTables.
    *******************************************************************************/
   public DataAnalysisQBitConfig withIncludeTables(Set<String> includeTables)
   {
      this.includeTables = includeTables;
      return (this);
   }



   /*******************************************************************************
    ** Getter for excludeTables.
    *******************************************************************************/
   public Set<String> getExcludeTables()
   {
      return (excludeTables);
   }



   /*******************************************************************************
    ** Fluent setter for excludeTables.
    *******************************************************************************/
   public DataAnalysisQBitConfig withExcludeTables(Set<String> excludeTables)
   {
      this.excludeTables = excludeTables;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableDefaults.
    *******************************************************************************/
   public Map<String, TableAnalysisDefaults> getTableDefaults()
   {
      return (tableDefaults);
   }



   /*******************************************************************************
    ** Fluent setter for tableDefaults.
    *******************************************************************************/
   public DataAnalysisQBitConfig withTableDefaults(Map<String, TableAnalysisDefaults> tableDefaults)
   {
      this.tableDefaults = tableDefaults;
      return (this);
   }

   /*******************************************************************************
    ** Host override for session-aware field analysis access.
    *******************************************************************************/
   public Supplier<FieldAnalysisAccessResolver> getFieldAnalysisAccessResolver()
   {
      return (fieldAnalysisAccessResolver);
   }

   /*******************************************************************************
    ** Configure an instance supplier; each service resolves one policy instance.
    *******************************************************************************/
   public DataAnalysisQBitConfig withFieldAnalysisAccessResolver(Supplier<FieldAnalysisAccessResolver> resolver)
   {
      this.fieldAnalysisAccessResolver = java.util.Objects.requireNonNull(resolver);
      return (this);
   }

   /*******************************************************************************
    ** Optional host adapter for scheduled owner permissions and record-lock keys.
    *******************************************************************************/
   public java.util.function.Function<String, com.kingsrook.qqq.backend.core.model.session.QSession> getOwnerSessionSupplier()
   {
      return (ownerSessionSupplier);
   }

   /*******************************************************************************
    ** Without an adapter, use the host authentication module's automated session.
    *******************************************************************************/
   public DataAnalysisQBitConfig withOwnerSessionSupplier(java.util.function.Function<String, com.kingsrook.qqq.backend.core.model.session.QSession> supplier)
   {
      ownerSessionSupplier = supplier;
      return (this);
   }
}
