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

package com.kingsrook.qbits.dataanalysis.schedule;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitConfig;
import com.kingsrook.qbits.dataanalysis.DataAnalysisService;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.permissions.DataAnalysisPermissions;
import com.kingsrook.qbits.dataanalysis.saved.AnalysisExport;
import com.kingsrook.qbits.dataanalysis.saved.SavedAnalyses;
import com.kingsrook.qbits.dataanalysis.saved.SavedDefinition;
import com.kingsrook.qbits.dataanalysis.saved.SavedResult;
import com.kingsrook.qbits.dataanalysis.watch.WatchRecords;
import com.kingsrook.qqq.backend.core.actions.messaging.SendMessageAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.alerts.AlertRecordRef;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Attachment;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Content;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Party;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailContentRole;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;

/*******************************************************************************
 ** Scheduled work uses owner and recipient sessions; maintenance only inventories rows.
 *******************************************************************************/
public class ScheduleRunner
{
   public static final String ALERT_TYPE = "data.analysis.schedule.failed";
   private final DataAnalysisQBitConfig config;
   private final DataAnalysisService service;

   /*******************************************************************************
    ** One instance serializes manual and automatic delivery on this application node.
    *******************************************************************************/
   public ScheduleRunner(DataAnalysisQBitConfig config, DataAnalysisService service)
   {
      this.config = config;
      this.service = service;
   }

   /*******************************************************************************
    ** Enumerate enabled due rows with a maintenance session restricted to metadata.
    *******************************************************************************/
   public synchronized Integer runDue(Instant now) throws QException
   {
      QSession maintenance = AnalysisSessions.maintenance(config, () ->
      {
         QSession legacy = new QSession().withUser(new QUser().withIdReference("data-analysis-scheduler"))
            .withPermissions("data_analysis.saved.read", "data_analysis.saved.edit")
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
         var filter = DrillFilters.and(DrillFilters.condition("enabled", QCriteriaOperator.EQUALS, true),
            DrillFilters.condition("next_run_at", QCriteriaOperator.LESS_THAN_OR_EQUALS, now));
         for(QRecord row : SavedAnalyses.query(DataAnalysisTables.SCHEDULE, filter))
         {
            sendNow(row.getValue("id"), now);
            count[0]++;
         }
      });
      return (count[0]);
   }

   /*******************************************************************************
    ** Manual delivery is authorized before source work; all attempts record their outcome.
    *******************************************************************************/
   public synchronized String sendNow(Serializable id, Instant now) throws QException
   {
      UpdateInput authorization = new UpdateInput(DataAnalysisTables.SCHEDULE);
      authorization.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      if(!PermissionsHelper.hasTablePermission(authorization, DataAnalysisTables.SCHEDULE, TablePermissionSubType.EDIT))
      {
         throw new QPermissionDeniedException("Schedule editing is unavailable.");
      }
      QRecord schedule = WatchRecords.get(DataAnalysisTables.SCHEDULE, id);
      String status;
      boolean failed = false;
      try
      {
         status = deliver(schedule);
      }
      catch(Exception e)
      {
         failed = true;
         status = "Failed: " + (e instanceof QException ? e.getMessage() : "Delivery unavailable; check the host configuration.");
      }
      Instant next = null;
      try
      {
         next = ScheduleTiming.next(schedule, now);
      }
      catch(QException e)
      {
         failed = true;
         status += "; " + e.getMessage();
      }
      QRecord patch = new QRecord().withValue("id", id).withValue("last_run_at", now).withValue("last_status", status).withValue("next_run_at", next);
      UpdateInput input = new UpdateInput(DataAnalysisTables.SCHEDULE).withRecords(List.of(patch));
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      QRecord stored = new UpdateAction().execute(input).getRecords().getFirst();
      if(stored.getErrors() != null && !stored.getErrors().isEmpty())
      {
         throw new QException("Schedule status could not be stored.");
      }
      if(failed)
      {
         QAlerts.raise(new AlertSignal(ALERT_TYPE).withDedupKey("data_analysis_schedule:" + id).withSeverity(AlertSeverity.ERROR)
            .withTitle("Analysis schedule failed").withSummary(status).withRecordRef(AlertRecordRef.of(DataAnalysisTables.SCHEDULE, id)));
      }
      return (status);
   }

   /*******************************************************************************
    ** The host resolves identities; an unavailable identity never becomes a system user.
    *******************************************************************************/
   private QSession session(String id) throws QException
   {
      QSession session;
      try
      {
         session = AnalysisSessions.forUser(config, id);
      }
      catch(Exception e)
      {
         throw new QException("User session unavailable.");
      }
      if(session == null || session.getUser() == null || !Objects.equals(id, session.getUser().getIdReference())
         || session.getSecurityKeyValues() == null || !session.getSecurityKeyValues().getOrDefault(DataAnalysisTables.USER_KEY, List.of()).contains(id))
      {
         throw new QException("User session unavailable.");
      }
      return (session);
   }

   /*******************************************************************************
    ** Re-run for each recipient to respect their record locks, not just table grants.
    *******************************************************************************/
   private String deliver(QRecord schedule) throws QException
   {
      if(config.getMessagingProviderName() == null || QContext.getQInstance().getMessagingProvider(config.getMessagingProviderName()) == null)
      {
         throw new QException("No messaging provider configured.");
      }
      if(config.getFromAddress() == null || config.getRecipientAddressSupplier() == null)
      {
         throw new QException("Configure fromAddress and recipientAddressSupplier for scheduled email.");
      }
      if(List.of("CSV", "XLSX").contains(Objects.toString(schedule.getValueString("format"), "")))
      {
         var provider = new com.kingsrook.qqq.backend.core.modules.messaging.QMessagingProviderDispatcher()
            .getMessagingProviderInterface(QContext.getQInstance().getMessagingProvider(config.getMessagingProviderName()));
         if(provider.getClass().equals(com.kingsrook.qqq.backend.core.model.metadata.messaging.email.EmailMessagingProvider.class))
         {
            throw new QException("Core EMAIL provider does not support attachments; configure an attachment-capable messaging provider.");
         }
      }
      QRecord saved = WatchRecords.get(DataAnalysisTables.SAVED, schedule.getValue("saved_id"));
      String owner = saved.getValueString("owner_user_id");
      SavedDefinition[] definition = new SavedDefinition[1];
      SavedResult[] ownerResult = new SavedResult[1];
      QContext.withTemporaryContext(new CapturedContext(QContext.getQInstance(), session(owner)), () ->
      {
         definition[0] = SavedDefinition.parse(WatchRecords.get(DataAnalysisTables.SAVED, saved.getValue("id")));
         if("TABLE_PROFILE".equals(definition[0].kind()) && definition[0].body().get("fields") == null)
         {
            var body = definition[0].body();
            body.put("fields", service.capabilities(definition[0].request().getTableName()).getFields());
            definition[0] = SavedDefinition.parse(definition[0].kind(), definition[0].request().getTableName(), body);
         }
         ownerResult[0] = definition[0].run(service, true);
      });
      List<String> skipped = new ArrayList<>();
      List<String> failures = new ArrayList<>();
      int sent = 0;
      for(String recipient : ScheduleCustomizer.recipients(schedule))
      {
         SavedResult[] result = new SavedResult[1];
         String address;
         try
         {
            QSession viewer = session(recipient);
            QContext.withTemporaryContext(new CapturedContext(QContext.getQInstance(), viewer), () ->
            {
               result[0] = recipient.equals(owner) ? ownerResult[0] : definition[0].run(service, true);
            });
            address = config.getRecipientAddressSupplier().apply(recipient);
            if(address == null || address.isBlank())
            {
               throw new QException("No email address.");
            }
         }
         catch(Exception e)
         {
            skipped.add(recipient);
            continue;
         }
         try
         {
            SendMessageInput message = new SendMessageInput().withMessagingProviderName(config.getMessagingProviderName())
               .withFrom(new Party().withAddress(config.getFromAddress())).withTo(new Party().withAddress(address))
               .withSubject("Analysis: " + saved.getValueString("name"))
               .withContent(new Content().withContentRole(EmailContentRole.HTML).withBody(html(saved.getValueString("name"), result[0])));
            String format = schedule.getValueString("format");
            if("CSV".equals(format) || "XLSX".equals(format))
            {
               var file = AnalysisExport.file(result[0], format);
               message.withAttachment(new Attachment().withName(file.filename()).withContents(file.bytes()));
            }
            new SendMessageAction().execute(message);
            sent++;
         }
         catch(Exception e)
         {
            failures.add(recipient);
         }
      }
      String status = "Sent " + sent + "; skipped: " + (skipped.isEmpty() ? "none" : String.join(", ", skipped));
      if(!failures.isEmpty())
      {
         throw new QException(status + "; delivery failed for: " + String.join(", ", failures));
      }
      return (status);
   }

   /*******************************************************************************
    ** Formatted result rows are escaped; pivots include only the first twenty rows.
    *******************************************************************************/
   public static String html(String name, SavedResult result)
   {
      var table = AnalysisExport.table(result);
      StringBuilder html = new StringBuilder("<h2>").append(escape(name)).append("</h2>");
      for(String warning : result.warnings())
      {
         html.append("<p>").append(escape(warning)).append("</p>");
      }
      html.append("<dl>");
      for(var metric : AnalysisExport.headlines(result))
      {
         html.append("<dt>").append(escape(metric.getLabel())).append("</dt><dd>").append(escape(AnalysisExport.value(metric))).append("</dd>");
      }
      html.append("</dl><table><thead><tr>");
      table.columns().forEach(c -> html.append("<th>").append(escape(c)).append("</th>"));
      html.append("</tr></thead><tbody>");
      for(var row : table.rows().stream().limit(20).toList())
      {
         html.append("<tr>");
         row.forEach(value -> html.append("<td>").append(escape(value)).append("</td>"));
         html.append("</tr>");
      }
      return (html.append("</tbody></table>").toString());
   }

   /*******************************************************************************
    ** Source labels and values are text, never trusted email markup.
    *******************************************************************************/
   private static String escape(String value)
   {
      return (Objects.toString(value, "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"));
   }
}
