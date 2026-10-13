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

package com.kingsrook.qbits.dataanalysis.run;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.dataanalysis.DataAnalysisService;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisBackend;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.engine.EffectiveConfig;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.result.AnalysisRequest;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfile;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfileRequest;
import com.kingsrook.qbits.dataanalysis.result.RunStatus;
import com.kingsrook.qbits.dataanalysis.result.TableProfile;
import com.kingsrook.qbits.dataanalysis.result.TableProfileRequest;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;

/*******************************************************************************
 ** Persistent owner-scoped runs. Results are guarded before being serialized.
 ** Cache identity includes session policy, effective config and data version.
 *******************************************************************************/
public class AnalysisRunService
{
   private static final QLogger LOG = QLogger.getLogger(AnalysisRunService.class);
   private final DataAnalysisService service;
   private final Executor executor;

   /*******************************************************************************
    ** Use a host-managed executor without inheriting a caller transaction.
    *******************************************************************************/
   public AnalysisRunService(DataAnalysisService service, Executor executor)
   {
      this.service = service;
      this.executor = executor;
   }



   /*******************************************************************************
    ** Estimate is transient; exact results are persisted and reused.
    *******************************************************************************/
   public ColumnProfile columnProfile(ColumnProfileRequest request) throws QException
   {
      service.validate(request);
      AnalysisBackend backend = service.backend(request.getTableName());
      AnalysisBackend.Scope scope = backend.scope(request.getTableName(), request.getFilter(), Boolean.TRUE.equals(request.getExact()));
      if(scope.estimate())
      {
         return (service.compute(request, backend, scope).withDataVersion(DataVersion.of(service.backend(request.getTableName()), request.getTableName(), request.getFilter())));
      }
      return (exact(request).getColumnProfile());
   }



   /*******************************************************************************
    ** Table profiles share the same lifecycle and cache isolation.
    *******************************************************************************/
   public TableProfile tableProfile(TableProfileRequest request) throws QException
   {
      service.validate(request);
      AnalysisBackend backend = service.backend(request.getTableName());
      AnalysisBackend.Scope scope = backend.scope(request.getTableName(), request.getFilter(), Boolean.TRUE.equals(request.getExact()));
      if(scope.estimate())
      {
         return (service.compute(request, backend, scope).withDataVersion(DataVersion.of(service.backend(request.getTableName()), request.getTableName(), request.getFilter())));
      }
      return (exact(request).getTableProfile());
   }



   /*******************************************************************************
    ** Complete synchronously for already-small populations and explicit exact calls.
    *******************************************************************************/
   private RunStatus exact(AnalysisRequest request) throws QException
   {
      AnalysisRequest snapshot = snapshot(request);
      QRecord cached = cached(snapshot);
      if(cached != null)
      {
         return (decode(cached));
      }
      QRecord run = insert(snapshot);
      execute(snapshot, run);
      RunStatus result = getRun(run.getValueString("id"));
      if(!"COMPLETE".equals(result.getStatus()))
      {
         throw new QException("Analysis failed; inspect the run status and retry.");
      }
      return (result);
   }



   /*******************************************************************************
    ** Snapshot both request and session before dispatching work. No ambient
    ** transaction or privileged action stack is copied into the worker.
    *******************************************************************************/
   public String startExactRun(AnalysisRequest request) throws QException
   {
      service.validate(request);
      AnalysisRequest snapshot = snapshot(request);
      service.validate(snapshot);
      QRecord cached = cached(snapshot);
      if(cached != null)
      {
         return (cached.getValueString("id"));
      }
      QRecord run = insert(snapshot);
      CapturedContext context;
      try
      {
         context = new CapturedContext(QContext.getQInstance(), QContext.getQSession().clone());
      }
      catch(CloneNotSupportedException e)
      {
         fail(run);
         throw new QException("Could not capture the analysis session.", e);
      }
      try
      {
         executor.execute(() -> QContext.withTemporaryContext(context, () -> execute(snapshot, run)));
      }
      catch(RuntimeException e)
      {
         fail(run);
         throw new QException("The analysis executor could not accept this run.", e);
      }
      return (run.getValueString("id"));
   }



   /*******************************************************************************
    ** Only completed, fresh, owner-and-policy-matching rows qualify as cache hits.
    *******************************************************************************/
   private QRecord cached(AnalysisRequest request) throws QException
   {
      service.validate(request);
      EffectiveConfig config = service.getConfigResolver().resolve(request.getTableName());
      if(config.getCacheTtlMinutes() == 0)
      {
         return (null);
      }
      QQueryFilter filter = DrillFilters.condition("user_id", QCriteriaOperator.EQUALS, userId())
         .withCriteria("request_hash", QCriteriaOperator.EQUALS, hash(request))
         .withCriteria("data_version", QCriteriaOperator.EQUALS, DataVersion.of(service.backend(request.getTableName()), request.getTableName(), request.getFilter()))
         .withCriteria("status", QCriteriaOperator.EQUALS, "COMPLETE")
         .withCriteria("finished_at", QCriteriaOperator.GREATER_THAN, Instant.now().minus(config.getCacheTtlMinutes(), ChronoUnit.MINUTES))
         .withCriteria("finished_at", QCriteriaOperator.LESS_THAN_OR_EQUALS, Instant.now())
         .withOrderBy(new QFilterOrderBy("id", false)).withLimit(1);
      List<QRecord> rows = query(filter);
      return (rows.isEmpty() ? null : rows.getFirst());
   }



   /*******************************************************************************
    ** Run inserts and updates are ordinary user actions under owner locks.
    *******************************************************************************/
   private QRecord insert(AnalysisRequest request) throws QException
   {
      QRecord run = new QRecord().withValue("table_name", request.getTableName()).withValue("operation", operation(request))
         .withValue("request_hash", hash(request)).withValue("filter_hash", digest(JsonUtils.toJson(DrillFilters.copy(request.getFilter()))))
         .withValue("data_version", DataVersion.of(service.backend(request.getTableName()), request.getTableName(), request.getFilter()))
         .withValue("status", "RUNNING").withValue("started_at", Instant.now()).withValue("user_id", userId());
      InsertInput input = new InsertInput(DataAnalysisTables.RUN).withRecords(List.of(run));
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      QRecord saved = new InsertAction().execute(input).getRecords().getFirst();
      check(saved);
      return (saved);
   }



   /*******************************************************************************
    ** Failure text is intentionally generic; backend errors may contain user data.
    *******************************************************************************/
   private void execute(AnalysisRequest request, QRecord run)
   {
      try
      {
         service.validate(request);
         AnalysisBackend backend = service.backend(request.getTableName());
         AnalysisBackend.Scope scope = backend.scope(request.getTableName(), request.getFilter(), true);
         Serializable result;
         if(request instanceof ColumnProfileRequest column)
         {
            result = service.compute(column, backend, scope).withDataVersion(run.getValueString("data_version"));
         }
         else
         {
            result = service.compute((TableProfileRequest) request, backend, scope).withDataVersion(run.getValueString("data_version"));
         }
         if(!hash(request).equals(run.getValueString("request_hash")))
         {
            throw new QException("Analysis policy changed during the run.");
         }
         update(new QRecord().withValue("id", run.getValue("id")).withValue("status", "COMPLETE").withValue("finished_at", Instant.now())
            .withValue("rows_scanned", scope.rowsScanned()).withValue("sampled", false).withValue("result_json", JsonUtils.toJson(result)));
      }
      catch(Exception e)
      {
         LOG.warn("Analysis execution failed; recording a safe failure status.", e);
         fail(run);
      }
   }



   /*******************************************************************************
    ** Persist safe failure status; persistence failures are logged separately.
    *******************************************************************************/
   private void fail(QRecord run)
   {
      try
      {
         update(new QRecord().withValue("id", run.getValue("id")).withValue("status", "FAILED").withValue("finished_at", Instant.now())
            .withValue("result_json", null).withValue("error", "Analysis could not be completed. Check permissions and settings, then retry."));
      }
      catch(QException e)
      {
         LOG.warn("Could not persist analysis failure status.", e);
      }
   }



   /*******************************************************************************
    ** Recheck source-table access and policy before releasing historical data.
    *******************************************************************************/
   public RunStatus getRun(String id) throws QException
   {
      Integer key;
      try
      {
         key = Integer.valueOf(id);
      }
      catch(RuntimeException e)
      {
         throw new QException("Invalid analysis run id.");
      }
      List<QRecord> rows = query(DrillFilters.condition("id", QCriteriaOperator.EQUALS, key).withCriteria("user_id", QCriteriaOperator.EQUALS, userId()).withLimit(1));
      if(rows.isEmpty())
      {
         throw new QPermissionDeniedException("Analysis run is unavailable.");
      }
      QRecord row = rows.getFirst();
      service.capabilities(row.getValueString("table_name"));
      RunStatus status = decode(row);
      if("COMPLETE".equals(status.getStatus()))
      {
         AnalysisRequest request = status.getColumnProfile() != null
            ? new ColumnProfileRequest().withTableName(status.getColumnProfile().getTableName()).withFieldName(status.getColumnProfile().getFieldName()).withFilter(status.getColumnProfile().getFilter()).withExact(true)
            : new TableProfileRequest().withTableName(status.getTableProfile().getTableName()).withFields(status.getTableProfile().getFields().stream().map(f -> f.getField()).toList()).withFilter(status.getTableProfile().getFilter()).withExact(true);
         service.validate(request);
         if(!hash(request).equals(row.getValueString("request_hash")))
         {
            throw new QPermissionDeniedException("Analysis policy changed; request a new run.");
         }
      }
      return (status);
   }



   /*******************************************************************************
    ** Deserialize typed payloads through core JsonUtils.
    *******************************************************************************/
   private RunStatus decode(QRecord row) throws QException
   {
      RunStatus status = new RunStatus().withRunId(row.getValueString("id")).withStatus(row.getValueString("status")).withError(row.getValueString("error"))
         .withStartedAt(row.getValueInstant("started_at")).withFinishedAt(row.getValueInstant("finished_at"));
      try
      {
         if("COMPLETE".equals(status.getStatus()))
         {
            if("COLUMN_PROFILE".equals(row.getValueString("operation")))
            {
               status.withColumnProfile(JsonUtils.toObject(row.getValueString("result_json"), ColumnProfile.class));
            }
            else
            {
               status.withTableProfile(JsonUtils.toObject(row.getValueString("result_json"), TableProfile.class));
            }
         }
      }
      catch(java.io.IOException e)
      {
         throw new QException("Stored analysis result could not be read.", e);
      }
      return (status);
   }



   /*******************************************************************************
    ** Read only through enforced core actions, including heavy result JSON.
    *******************************************************************************/
   private static List<QRecord> query(QQueryFilter filter) throws QException
   {
      QueryInput input = new QueryInput(DataAnalysisTables.RUN).withFilter(filter).withShouldFetchHeavyFields(true);
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new QueryAction().execute(input).getRecords());
   }



   /*******************************************************************************
    ** Write only supplied run fields and check core record-level failures.
    *******************************************************************************/
   private static void update(QRecord record) throws QException
   {
      UpdateInput input = new UpdateInput(DataAnalysisTables.RUN).withRecords(List.of(record));
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      check(new UpdateAction().execute(input).getRecords().getFirst());
   }



   /*******************************************************************************
    ** Core can return record-level errors without throwing.
    *******************************************************************************/
   private static void check(QRecord record) throws QException
   {
      if(record.getErrors() != null && !record.getErrors().isEmpty())
      {
         throw new QException("Analysis run could not be stored.");
      }
   }



   /*******************************************************************************
    ** An authenticated identity is mandatory even on unprotected host tables.
    *******************************************************************************/
   private static String userId() throws QException
   {
      if(QContext.getQSession() == null || QContext.getQSession().getUser() == null || QContext.getQSession().getUser().getIdReference() == null)
      {
         throw new QPermissionDeniedException("Sign in to run an analysis.");
      }
      return (QContext.getQSession().getUser().getIdReference());
   }



   /*******************************************************************************
    ** Exact requests have canonical selected fields and no pagination metadata.
    *******************************************************************************/
   private AnalysisRequest snapshot(AnalysisRequest request) throws QException
   {
      if(request instanceof ColumnProfileRequest column)
      {
         return (new ColumnProfileRequest().withTableName(column.getTableName()).withFieldName(column.getFieldName()).withFilter(DrillFilters.restoreTypes(column.getTableName(), column.getFilter())).withExact(true));
      }
      if(request instanceof TableProfileRequest table)
      {
         List<String> fields = service.capabilities(table.getTableName()).getFields().stream()
            .filter(f -> table.getFields() == null || table.getFields().contains(f)).sorted().toList();
         return (new TableProfileRequest().withTableName(table.getTableName()).withFields(fields).withFilter(DrillFilters.restoreTypes(table.getTableName(), table.getFilter())).withExact(true));
      }
      throw new QException("Exact runs support column and table profiles only.");
   }



   /*******************************************************************************
    ** Stable operation identity.
    *******************************************************************************/
   private static String operation(AnalysisRequest request)
   {
      return (request instanceof ColumnProfileRequest ? "COLUMN_PROFILE" : "TABLE_PROFILE");
   }



   /*******************************************************************************
    ** Normalize JSON objects and commutative filter lists, then SHA-256. Policy
    ** values are hashed, never persisted as plaintext in the cache identity.
    *******************************************************************************/
   private String hash(AnalysisRequest request) throws QException
   {
      Map<String, Object> identity = new LinkedHashMap<>();
      identity.put("request", snapshot(request));
      identity.put("operation", operation(request));
      identity.put("user", userId());
      identity.put("permissions", QContext.getQSession().getPermissions().stream().sorted().toList());
      identity.put("keys", QContext.getQSession().getSecurityKeyValues());
      identity.put("zone", ValueUtils.getSessionOrInstanceZoneId().getId());
      identity.put("config", service.getConfigResolver().resolve(request.getTableName()));
      identity.put("fields", service.capabilities(request.getTableName()).getFields().stream().sorted().toList());
      identity.put("fieldAccess", digest(service.fieldAccessFingerprint(request.getTableName())));
      identity.put("policy", service.fieldAccessPolicy(request.getTableName()));
      identity.put("backendVariants", QContext.getQSession().getBackendVariants());
      try
      {
         JsonNode json = new ObjectMapper().readTree(JsonUtils.toJson(identity));
         return (digest(JsonUtils.toJson(normalize(json, ""))));
      }
      catch(java.io.IOException e)
      {
         throw new QException("Could not normalize analysis request.", e);
      }
   }



   /*******************************************************************************
    ** Preserve ordered parameter arrays, but sort commutative filter clauses.
    *******************************************************************************/
   private static Object normalize(JsonNode node, String key)
   {
      if(node.isObject())
      {
         Map<String, Object> result = new TreeMap<>();
         node.fields().forEachRemaining(e -> result.put(e.getKey(), normalize(e.getValue(), e.getKey())));
         return (result);
      }
      if(node.isArray())
      {
         List<Object> result = new ArrayList<>();
         node.forEach(value -> result.add(normalize(value, key)));
         if(List.of("criteria", "subFilters", "analyzableFields", "excludedFields").contains(key))
         {
            result.sort(Comparator.comparing(JsonUtils::toJson));
         }
         return (result);
      }
      return (node);
   }



   /*******************************************************************************
    ** SHA-256 is guaranteed by the Java platform.
    *******************************************************************************/
   private static String digest(String value) throws QException
   {
      try
      {
         return (HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))));
      }
      catch(java.security.NoSuchAlgorithmException e)
      {
         throw new QException("SHA-256 unavailable.", e);
      }
   }
}
