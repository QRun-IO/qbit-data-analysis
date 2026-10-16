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

package com.kingsrook.qbits.dataanalysis.watch;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitConfig;
import com.kingsrook.qbits.dataanalysis.DataAnalysisService;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.permissions.DataAnalysisPermissions;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.alerts.AlertLink;
import com.kingsrook.qqq.backend.core.alerts.AlertRecordRef;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;

/*******************************************************************************
 ** Transition watches through the core alerts SPI, computing exclusively as owner.
 *******************************************************************************/
public class WatchEvaluator
{
   public static final String ALERT_TYPE = "data.analysis.anomaly";
   private final DataAnalysisQBitConfig config;
   private final DataAnalysisService service;

   /*******************************************************************************
    ** One evaluator per instance serializes scheduled and manual transitions.
    *******************************************************************************/
   public WatchEvaluator(DataAnalysisQBitConfig config, DataAnalysisService service)
   {
      this.config = config;
      this.service = service;
   }

   /*******************************************************************************
    ** Preview uses the viewer's rights, never the saved owner's cached data.
    *******************************************************************************/
   public WatchPreview preview(Serializable id, Instant now) throws QException
   {
      return (service.watchPreview(WatchRecords.get(DataAnalysisTables.WATCH, id), now));
   }

   /*******************************************************************************
    ** Manual evaluation first requires the caller's read and edit rights.
    *******************************************************************************/
   public synchronized WatchVerdict evaluate(Serializable id, Instant now) throws QException
   {
      requireEdit();
      QRecord row = WatchRecords.get(DataAnalysisTables.WATCH, id);
      WatchVerdict verdict = asOwner(row, now);
      store(row, verdict, now);
      return (verdict);
   }

   /*******************************************************************************
    ** Only watch inventory/status use maintenance grants, never source data.
    *******************************************************************************/
   public synchronized Integer evaluateEnabled(Instant now) throws QException
   {
      QSession maintenance = com.kingsrook.qbits.dataanalysis.schedule.AnalysisSessions.maintenance(config, () ->
      {
         QSession legacy = new QSession().withUser(new QUser().withIdReference("data-analysis-scheduler"))
            .withPermissions("data_analysis.watch.read", "data_analysis.watch.edit")
            .withSecurityKeyValue(DataAnalysisPermissions.ADMIN, true);
         if(QContext.getQSession().getBackendVariants() != null)
         {
            legacy.setBackendVariants(new java.util.HashMap<>(QContext.getQSession().getBackendVariants()));
         }
         return (legacy);
      });
      int[] count = {0};
      QContext.withTemporaryContext(new CapturedContext(QContext.getQInstance(), maintenance), () ->
      {
         QueryInput input = new QueryInput(DataAnalysisTables.WATCH).withShouldFetchHeavyFields(true)
            .withFilter(DrillFilters.condition("enabled", QCriteriaOperator.EQUALS, true));
         input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         for(QRecord row : new QueryAction().execute(input).getRecords())
         {
            WatchVerdict verdict = evaluate(row.getValue("id"), now);
            if(verdict.evaluated())
            {
               count[0]++;
            }
         }
      });
      return (count[0]);
   }

   /*******************************************************************************
    ** An unavailable or mismatched host session must never fall back to system.
    *******************************************************************************/
   private WatchVerdict asOwner(QRecord row, Instant now)
   {
      String owner = row.getValueString("owner_user_id");
      QSession session;
      try
      {
         session = com.kingsrook.qbits.dataanalysis.schedule.AnalysisSessions.forUser(config, owner);
      }
      catch(Exception e)
      {
         return (WatchVerdict.skipped("Owner session unavailable"));
      }
      if(owner == null || session == null || session.getUser() == null || !Objects.equals(owner, session.getUser().getIdReference())
         || session.getSecurityKeyValues() == null || !session.getSecurityKeyValues().getOrDefault(DataAnalysisTables.USER_KEY, List.of()).contains(owner))
      {
         return (WatchVerdict.skipped("Owner session unavailable"));
      }
      WatchVerdict[] verdict = new WatchVerdict[1];
      try
      {
         QContext.withTemporaryContext(new CapturedContext(QContext.getQInstance(), session), () ->
         {
            QRecord owned = WatchRecords.get(DataAnalysisTables.WATCH, row.getValue("id"));
            verdict[0] = service.watchPreview(owned, now).verdict();
         });
         return (verdict[0]);
      }
      catch(QPermissionDeniedException e)
      {
         return (WatchVerdict.skipped("Owner lacks READ or Analyze access to the watched table."));
      }
      catch(com.kingsrook.qqq.backend.core.exceptions.QBadRequestException e)
      {
         return (WatchVerdict.skipped("Watch evaluation unavailable: " + e.getMessage()));
      }
      catch(Exception e)
      {
         return (WatchVerdict.skipped("Watch evaluation unavailable; check the saved analysis and source configuration."));
      }
   }

   /*******************************************************************************
    ** Persist first; only successful evaluated transitions notify the alerts SPI.
    *******************************************************************************/
   private void store(QRecord row, WatchVerdict verdict, Instant now) throws QException
   {
      String key = "data_analysis_watch:" + row.getValue("id");
      boolean wasFiring = "FIRING".equals(row.getValueString("state"));
      QRecord patch = new QRecord().withValue("id", row.getValue("id")).withValue("last_evaluated_at", now)
         .withValue("last_value", verdict.value()).withValue("last_message", verdict.message());
      if(verdict.evaluated())
      {
         patch.withValue("state", verdict.firing() ? "FIRING" : "OK").withValue("alert_key", key);
      }
      update(patch);
      if(!verdict.evaluated() || wasFiring == verdict.firing())
      {
         return;
      }
      if(verdict.firing())
      {
         AlertSignal signal = new AlertSignal(ALERT_TYPE).withDedupKey(key).withSeverity(AlertSeverity.valueOf(row.getValueString("severity")))
            .withTitle("Data anomaly").withSummary(verdict.message()).withRecordRef(AlertRecordRef.of(DataAnalysisTables.WATCH, row.getValue("id")));
         if(verdict.drill() != null)
         {
            signal.withLink(AlertLink.toTable(verdict.drill().getTableName(), verdict.drill().getFilter(), "View matching records"));
         }
         QAlerts.raise(signal);
      }
      else
      {
         QAlerts.resolve(ALERT_TYPE, key, verdict.message());
      }
   }

   /*******************************************************************************
    ** Enable and disable selected owner-locked rows through standard core updates.
    *******************************************************************************/
   public void setEnabled(Serializable id, Boolean enabled) throws QException
   {
      requireEdit();
      WatchRecords.get(DataAnalysisTables.WATCH, id);
      update(new QRecord().withValue("id", id).withValue("enabled", enabled));
   }

   /*******************************************************************************
    ** Never count an unsuccessful core update as an evaluated watch.
    *******************************************************************************/
   private static void update(QRecord record) throws QException
   {
      UpdateInput input = new UpdateInput(DataAnalysisTables.WATCH).withRecords(List.of(record));
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      QRecord stored = new UpdateAction().execute(input).getRecords().getFirst();
      if(stored.getErrors() != null && !stored.getErrors().isEmpty())
      {
         throw new QException("Watch could not be updated: " + stored.getErrors().getFirst().getMessage());
      }
   }

   /*******************************************************************************
    ** Public mutation entry points enforce table permission before source queries.
    *******************************************************************************/
   private static void requireEdit() throws QPermissionDeniedException
   {
      if(!PermissionsHelper.hasTablePermission(new UpdateInput(DataAnalysisTables.WATCH), DataAnalysisTables.WATCH, TablePermissionSubType.EDIT))
      {
         throw new QPermissionDeniedException("Watch editing is unavailable.");
      }
   }
}
