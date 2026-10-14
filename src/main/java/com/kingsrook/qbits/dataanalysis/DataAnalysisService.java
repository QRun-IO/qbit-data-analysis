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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisBackend;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisDrillGuard;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisLabels;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisSecurity;
import com.kingsrook.qbits.dataanalysis.engine.ColumnProfiler;
import com.kingsrook.qbits.dataanalysis.engine.ComparisonExplorer;
import com.kingsrook.qbits.dataanalysis.engine.CoreAnalysisBackend;
import com.kingsrook.qbits.dataanalysis.engine.DistinctValues;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.engine.DuplicateFinder;
import com.kingsrook.qbits.dataanalysis.engine.EffectiveConfigResolver;
import com.kingsrook.qbits.dataanalysis.engine.FieldAnalysisAccessResolver;
import com.kingsrook.qbits.dataanalysis.engine.PivotExplorer;
import com.kingsrook.qbits.dataanalysis.engine.RelationshipExplorer;
import com.kingsrook.qbits.dataanalysis.engine.ShiftExplorer;
import com.kingsrook.qbits.dataanalysis.engine.StandInAnalysisBackend;
import com.kingsrook.qbits.dataanalysis.engine.TableProfiler;
import com.kingsrook.qbits.dataanalysis.result.AnalysisRequest;
import com.kingsrook.qbits.dataanalysis.result.Capabilities;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfile;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfileRequest;
import com.kingsrook.qbits.dataanalysis.result.CompareRequest;
import com.kingsrook.qbits.dataanalysis.result.CompareResult;
import com.kingsrook.qbits.dataanalysis.result.CorrelationRequest;
import com.kingsrook.qbits.dataanalysis.result.CorrelationResult;
import com.kingsrook.qbits.dataanalysis.result.CrosstabRequest;
import com.kingsrook.qbits.dataanalysis.result.CrosstabResult;
import com.kingsrook.qbits.dataanalysis.result.DistinctValuesPage;
import com.kingsrook.qbits.dataanalysis.result.DistinctValuesRequest;
import com.kingsrook.qbits.dataanalysis.result.DuplicatesRequest;
import com.kingsrook.qbits.dataanalysis.result.DuplicatesResult;
import com.kingsrook.qbits.dataanalysis.result.PivotRequest;
import com.kingsrook.qbits.dataanalysis.result.PivotResult;
import com.kingsrook.qbits.dataanalysis.result.RunStatus;
import com.kingsrook.qbits.dataanalysis.result.ScatterRequest;
import com.kingsrook.qbits.dataanalysis.result.ScatterResult;
import com.kingsrook.qbits.dataanalysis.result.ShiftRequest;
import com.kingsrook.qbits.dataanalysis.result.ShiftResult;
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
   private final FieldAnalysisAccessResolver fieldAccess;
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
      fieldAccess = config.getFieldAnalysisAccessResolver().get();
      security = new AnalysisSecurity(configs, fieldAccess);
      runs = new AnalysisRunService(this, executor);
   }



   /*******************************************************************************
    ** Expose only readable, configured fields and detached effective settings.
    *******************************************************************************/
   public Capabilities capabilities(String table) throws QException
   {
      return (new Capabilities().withTableName(table).withFields(security.analyzableFields(table).stream().map(QFieldMetaData::getName).toList())
         .withConfig(configs.resolve(table)).withBackend(CoreAnalysisBackend.capabilities(table).isEmpty() ? "Streamed analysis" : "Core aggregate pushdown"));
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
      return (protect(new DistinctValues(backend(request.getTableName()), fieldAccess).page(request, security.field(request.getTableName(), request.getFieldName()), configs.resolve(request.getTableName())), request));
   }



   /*******************************************************************************
    ** Find guarded duplicate groups using only authorized fields.
    *******************************************************************************/
   public DuplicatesResult duplicates(DuplicatesRequest request) throws QException
   {
      validate(request);
      return (protect(new DuplicateFinder(backend(request.getTableName())).find(request, configs.resolve(request.getTableName())), request));
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
    ** Select core pushdown when the backend advertises analysis capabilities.
    *******************************************************************************/
   public AnalysisBackend backend(String table) throws QException
   {
      return (CoreAnalysisBackend.capabilities(table).isEmpty() ? new StandInAnalysisBackend(configs.resolve(table)) : new CoreAnalysisBackend(configs.resolve(table)));
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
      return (protect(new ColumnProfiler(backend, fieldAccess).profile(request, security.field(request.getTableName(), request.getFieldName()), scope, configs.resolve(request.getTableName())), request));
   }



   /*******************************************************************************
    ** Resolve requested fields against personalized metadata on every execution.
    *******************************************************************************/
   public TableProfile compute(TableProfileRequest request, AnalysisBackend backend, AnalysisBackend.Scope scope) throws QException
   {
      validate(request);
      List<QFieldMetaData> fields = security.analyzableFields(request.getTableName()).stream()
         .filter(f -> request.getFields() == null || request.getFields().contains(f.getName())).toList();
      return (protect(new TableProfiler(backend).profile(request, fields, scope), request));
   }

   /*******************************************************************************
    ** Field policy identity is re-evaluated before cache reads and writes.
    *******************************************************************************/
   public String fieldAccessFingerprint(String table) throws QException
   {
      return (security.accessFingerprint(table));
   }

   /*******************************************************************************
    ** Execute a pivot as the current user through core aggregates.
    *******************************************************************************/
   public PivotResult pivot(PivotRequest request) throws QException
   {
      validate(request);
      return (new PivotExplorer(security, configs.resolve(request.getTableName())).execute(request));
   }

   /*******************************************************************************
    ** Generated primary-key drills also obey the configured field-access seam.
    *******************************************************************************/
   private <T> T protect(T result, AnalysisRequest request) throws QException
   {
      new AnalysisDrillGuard(Set.copyOf(capabilities(request.getTableName()).getFields()),
         DrillFilters.restoreTypes(request.getTableName(), request.getFilter())).apply(result);
      return (result);
   }

   /*******************************************************************************
    ** Include resolved tiers of related label fields in cache identity.
    *******************************************************************************/
   public Map<String, Object> fieldAccessPolicy(String table) throws QException
   {
      return (AnalysisLabels.policy(table, fieldAccess));
   }

   /*******************************************************************************
    ** Execute compare under the current user's FULL-field and record policy.
    *******************************************************************************/
   public CompareResult compare(CompareRequest request) throws QException
   {
      validate(request);
      return (new ComparisonExplorer(security, configs.resolve(request.getTableName()), request.getTableName()).compare(request));
   }

   /*******************************************************************************
    ** Execute correlation under the current user's FULL-field and record policy.
    *******************************************************************************/
   public CorrelationResult correlation(CorrelationRequest request) throws QException
   {
      validate(request);
      return (new RelationshipExplorer(security, configs.resolve(request.getTableName()), request.getTableName()).correlation(request));
   }

   /*******************************************************************************
    ** Execute crosstab under the current user's FULL-field and record policy.
    *******************************************************************************/
   public CrosstabResult crosstab(CrosstabRequest request) throws QException
   {
      validate(request);
      return (new RelationshipExplorer(security, configs.resolve(request.getTableName()), request.getTableName()).crosstab(request));
   }

   /*******************************************************************************
    ** Execute scatter under the current user's FULL-field and record policy.
    *******************************************************************************/
   public ScatterResult scatter(ScatterRequest request) throws QException
   {
      validate(request);
      return (new RelationshipExplorer(security, configs.resolve(request.getTableName()), request.getTableName()).scatter(request));
   }

   /*******************************************************************************
    ** Execute shift under the current user's FULL-field and record policy.
    *******************************************************************************/
   public ShiftResult shift(ShiftRequest request) throws QException
   {
      validate(request);
      return (new ShiftExplorer(security, configs.resolve(request.getTableName()), request.getTableName()).shift(request));
   }

   /*******************************************************************************
    ** Watch aggregates use the same current-user table and FULL-field policy.
    *******************************************************************************/
   public com.kingsrook.qbits.dataanalysis.watch.WatchPreview watchPreview(com.kingsrook.qqq.backend.core.model.data.QRecord row, java.time.Instant now) throws QException
   {
      var resolved = com.kingsrook.qbits.dataanalysis.watch.WatchRecords.resolve(row);
      String table = resolved.getValueString("table_name");
      security.table(table);
      var definition = com.kingsrook.qbits.dataanalysis.watch.WatchDefinition.parse(resolved);
      return (new com.kingsrook.qbits.dataanalysis.watch.WatchSeries(security, configs.resolve(table), table)
         .preview(definition, com.kingsrook.qbits.dataanalysis.watch.WatchRecords.filter(resolved), now));
   }
}
