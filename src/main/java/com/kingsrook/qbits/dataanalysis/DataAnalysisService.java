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

import java.util.List;
import java.util.concurrent.Executor;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisBackend;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisSecurity;
import com.kingsrook.qbits.dataanalysis.engine.ColumnProfiler;
import com.kingsrook.qbits.dataanalysis.engine.DistinctValues;
import com.kingsrook.qbits.dataanalysis.engine.DuplicateFinder;
import com.kingsrook.qbits.dataanalysis.engine.EffectiveConfigResolver;
import com.kingsrook.qbits.dataanalysis.engine.StandInAnalysisBackend;
import com.kingsrook.qbits.dataanalysis.engine.TableProfiler;
import com.kingsrook.qbits.dataanalysis.result.AnalysisRequest;
import com.kingsrook.qbits.dataanalysis.result.Capabilities;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfile;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfileRequest;
import com.kingsrook.qbits.dataanalysis.result.DistinctValuesPage;
import com.kingsrook.qbits.dataanalysis.result.DistinctValuesRequest;
import com.kingsrook.qbits.dataanalysis.result.DuplicatesRequest;
import com.kingsrook.qbits.dataanalysis.result.DuplicatesResult;
import com.kingsrook.qbits.dataanalysis.result.RunStatus;
import com.kingsrook.qbits.dataanalysis.result.TableProfile;
import com.kingsrook.qbits.dataanalysis.result.TableProfileRequest;
import com.kingsrook.qbits.dataanalysis.run.AnalysisRunService;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;

/*******************************************************************************
 ** Single host facade for analysis; all entry points use the calling QContext.
 *******************************************************************************/
public class DataAnalysisService
{
   private final EffectiveConfigResolver configs;
   private final AnalysisSecurity security;
   private final AnalysisRunService runs;

   /*******************************************************************************
    ** Default asynchronous jobs use the common executor with captured context.
    *******************************************************************************/
   public DataAnalysisService(DataAnalysisQBitConfig config)
   {
      this(config, java.util.concurrent.ForkJoinPool.commonPool());
   }



   /*******************************************************************************
    ** Hosts may supply their managed executor.
    *******************************************************************************/
   public DataAnalysisService(DataAnalysisQBitConfig config, Executor executor)
   {
      configs = new EffectiveConfigResolver(config);
      security = new AnalysisSecurity(configs);
      runs = new AnalysisRunService(this, executor);
   }



   /*******************************************************************************
    ** Expose only readable, configured fields and detached effective settings.
    *******************************************************************************/
   public Capabilities capabilities(String table) throws QException
   {
      return (new Capabilities().withTableName(table).withFields(security.analyzableFields(table).stream().map(QFieldMetaData::getName).toList())
         .withConfig(configs.resolve(table)).withBackend("RC.5 stand-in; deterministic primary-key prefix sampling"));
   }



   /*******************************************************************************
    ** Estimate above the threshold; otherwise reuse or persist an exact run.
    *******************************************************************************/
   public ColumnProfile columnProfile(ColumnProfileRequest request) throws QException
   {
      validate(request);
      return (runs.columnProfile(request));
   }



   /*******************************************************************************
    ** Table profiles share the exact-run cache lifecycle.
    *******************************************************************************/
   public TableProfile tableProfile(TableProfileRequest request) throws QException
   {
      validate(request);
      return (runs.tableProfile(request));
   }



   /*******************************************************************************
    ** Page guarded distinct values as the current user.
    *******************************************************************************/
   public DistinctValuesPage distinctValues(DistinctValuesRequest request) throws QException
   {
      validate(request);
      return (new DistinctValues(backend(request.getTableName())).page(request, security.field(request.getTableName(), request.getFieldName()), configs.resolve(request.getTableName())));
   }



   /*******************************************************************************
    ** Find guarded duplicate groups using only authorized fields.
    *******************************************************************************/
   public DuplicatesResult duplicates(DuplicatesRequest request) throws QException
   {
      validate(request);
      return (new DuplicateFinder(backend(request.getTableName())).find(request, configs.resolve(request.getTableName())));
   }



   /*******************************************************************************
    ** Start either supported exact profile kind and return the persistent run id.
    *******************************************************************************/
   public String startExactRun(AnalysisRequest request) throws QException
   {
      validate(request);
      return (runs.startExactRun(request));
   }



   /*******************************************************************************
    ** Owner-locked status plus a typed completed result.
    *******************************************************************************/
   public RunStatus getRun(String id) throws QException
   {
      return (runs.getRun(id));
   }



   /*******************************************************************************
    ** Settings editors can force an immediate runtime-policy refresh.
    *******************************************************************************/
   public EffectiveConfigResolver getConfigResolver()
   {
      return (configs);
   }



   /*******************************************************************************
    ** Fresh per-request backend, ready for a future core-backed implementation.
    *******************************************************************************/
   public AnalysisBackend backend(String table) throws QException
   {
      return (new StandInAnalysisBackend(configs.resolve(table)));
   }



   /*******************************************************************************
    ** Validate every entry point before counting source data or consulting cache.
    *******************************************************************************/
   public void validate(AnalysisRequest request) throws QException
   {
      if(request == null || request.getTableName() == null)
      {
         throw new QException("An analysis table is required.");
      }
      security.table(request.getTableName());
      if(request instanceof ColumnProfileRequest column)
      {
         security.field(request.getTableName(), column.getFieldName());
      }
      else if(request instanceof DistinctValuesRequest distinct)
      {
         security.field(request.getTableName(), distinct.getFieldName());
      }
      else if(request instanceof DuplicatesRequest duplicates)
      {
         if(duplicates.getFields() == null || duplicates.getFields().isEmpty())
         {
            throw new QException("Choose at least one duplicate field.");
         }
         for(String field : duplicates.getFields())
         {
            security.field(request.getTableName(), field);
         }
      }
      else if(request instanceof TableProfileRequest table && table.getFields() != null)
      {
         for(String field : table.getFields())
         {
            security.field(request.getTableName(), field);
         }
      }
   }



   /*******************************************************************************
    ** The run layer supplies the scope; privacy is applied by the profiler.
    *******************************************************************************/
   public ColumnProfile compute(ColumnProfileRequest request, AnalysisBackend backend, AnalysisBackend.Scope scope) throws QException
   {
      validate(request);
      return (new ColumnProfiler(backend).profile(request, security.field(request.getTableName(), request.getFieldName()), scope, configs.resolve(request.getTableName())));
   }



   /*******************************************************************************
    ** Resolve requested fields against personalized metadata on every execution.
    *******************************************************************************/
   public TableProfile compute(TableProfileRequest request, AnalysisBackend backend, AnalysisBackend.Scope scope) throws QException
   {
      validate(request);
      List<QFieldMetaData> fields = security.analyzableFields(request.getTableName()).stream()
         .filter(f -> request.getFields() == null || request.getFields().contains(f.getName())).toList();
      return (new TableProfiler(backend).profile(request, fields, scope));
   }
}
