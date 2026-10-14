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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.processes.ScheduleActions;
import com.kingsrook.qbits.dataanalysis.saved.AnalysisExport;
import com.kingsrook.qbits.dataanalysis.saved.SavedDefinition;
import com.kingsrook.qbits.dataanalysis.schedule.ScheduleRunner;
import com.kingsrook.qbits.dataanalysis.schedule.ScheduleTiming;
import com.kingsrook.qbits.dataanalysis.watch.WatchRecords;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.QMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.messaging.MessagingProviderInterface;
import com.kingsrook.qqq.backend.core.modules.messaging.QMessagingProviderDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchedulesTest extends EngineFixture
{
   static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");
   private ScheduleRunner runner;

   public static class Mail implements MessagingProviderInterface
   {
      static final List<SendMessageInput> sent = new ArrayList<>();
      static boolean fail;

      @Override
      public String getType()
      {
         return ("analysis-test-mail");
      }

      @Override
      public SendMessageOutput sendMessage(SendMessageInput input) throws QException
      {
         if(fail)
         {
            throw new QException("Test mail failure");
         }
         sent.add(input);
         return (new SendMessageOutput());
      }
   }

   @BeforeEach
   void deliverySetup() throws Exception
   {
      Mail.sent.clear();
      Mail.fail = false;
      WatchesTest.Sink.raised.clear();
      QMessagingProviderDispatcher.registerMessagingProvider(new Mail());
      instance.addMessagingProvider(new QMessagingProviderMetaData().withName("mail").withType("analysis-test-mail"));
      AlertsInstanceMetaData.ofOrWithNew(instance).withSink(new QCodeReference(WatchesTest.Sink.class));
      config.withMessagingProviderName("mail").withFromAddress("analysis@example.test").withRecipientAddressSupplier(id -> id + "@example.test")
         .withOwnerSessionSupplier(id ->
         {
            if(id.equals("missing"))
            {
               return (null);
            }
            QSession user = SavedAnalysesTest.viewer(id, id.equals("alice") ? 1 : 2);
            if(id.equals("denied"))
            {
               user.getPermissions().remove("orders.analyze");
            }
            return (user);
         });
      runner = new ScheduleRunner(config, service);
   }

   QRecord schedule(String kind, Map<String, Object> definition, String format) throws Exception
   {
      var saved = TestSupport.insert(DataAnalysisTables.SAVED, SavedAnalysesTest.definition("alice", false, kind, definition).withValue("name", "<Orders>"));
      var schedule = TestSupport.insert(DataAnalysisTables.SCHEDULE, new QRecord().withValue("saved_id", saved.getValue("id"))
         .withValue("frequency", "DAILY").withValue("time", "09:00").withValue("time_zone", "America/Chicago")
         .withValue("recipients", "[\"alice\",\"bob\",\"denied\",\"missing\"]").withValue("format", format).withValue("enabled", true));
      new UpdateAction().execute(new UpdateInput(DataAnalysisTables.SCHEDULE).withRecords(List.of(new QRecord().withValue("id", schedule.getValue("id")).withValue("next_run_at", NOW))));
      return (schedule);
   }

   @Test
   void calendarWeeklyMonthlyDstAndCron() throws Exception
   {
      QRecord row = new QRecord().withValue("frequency", "DAILY").withValue("time", "09:00").withValue("time_zone", "America/Chicago");
      assertThat(ScheduleTiming.next(row, NOW)).isEqualTo(Instant.parse("2026-10-13T14:00:00Z"));
      row.withValue("frequency", "WEEKLY").withValue("day", 1);
      assertThat(ScheduleTiming.next(row, NOW)).isEqualTo(Instant.parse("2026-10-19T14:00:00Z"));
      row.withValue("frequency", "MONTHLY").withValue("day", 31);
      assertThat(ScheduleTiming.next(row, Instant.parse("2026-02-01T00:00:00Z"))).isEqualTo(Instant.parse("2026-02-28T15:00:00Z"));
      row.withValue("frequency", "DAILY").withValue("time", "02:30");
      assertThat(ScheduleTiming.next(row, Instant.parse("2026-03-08T06:00:00Z"))).isEqualTo(Instant.parse("2026-03-08T08:30:00Z"));
      row.withValue("time", "01:30");
      assertThat(ScheduleTiming.next(row, Instant.parse("2026-11-01T07:00:00Z"))).isEqualTo(Instant.parse("2026-11-02T07:30:00Z"));
      row.withValue("cron", "0 0 9 ? * MON");
      assertThat(ScheduleTiming.next(row, NOW)).isEqualTo(Instant.parse("2026-10-19T14:00:00Z"));
      row.withValue("cron", "bad cron");
      assertThatThrownBy(() -> ScheduleTiming.next(row, NOW)).hasMessageContaining("Invalid schedule");
      row.withValue("cron", null).withValue("time_zone", "Mars/Time");
      assertThatThrownBy(() -> ScheduleTiming.next(row, NOW)).hasMessageContaining("Invalid schedule");
      row.withValue("time_zone", "UTC").withValue("frequency", "MONTHLY").withValue("day", 32);
      assertThatThrownBy(() -> ScheduleTiming.next(row, NOW)).hasMessageContaining("Invalid schedule");
   }

   @Test
   void dueRunsAsOwnerAndRecipientRowsAreRecheckedWithAttachments() throws Exception
   {
      var schedule = schedule("PIVOT", Map.of("rows", List.of(Map.of("field", "status")), "values", List.of(Map.of("operator", "COUNT"))), "CSV");
      assertThat(runner.runDue(NOW.minusSeconds(1))).isZero();
      assertThat(runner.runDue(NOW)).isEqualTo(1);
      assertThat(Mail.sent).hasSize(2);
      assertThat(Mail.sent).extracting(m -> m.getTo().getAddress()).containsExactly("alice@example.test", "bob@example.test");
      String ownerCsv = new String(Mail.sent.get(0).getAttachmentList().getFirst().getContents(), StandardCharsets.UTF_8);
      String viewerCsv = new String(Mail.sent.get(1).getAttachmentList().getFirst().getContents(), StandardCharsets.UTF_8);
      assertThat(ownerCsv).contains("4");
      assertThat(viewerCsv).contains("2").doesNotContain("4");
      assertThat(Mail.sent.getFirst().getContentList().getFirst().getBody()).contains("&lt;Orders&gt;", "<table>").doesNotContain("<Orders>");
      var stored = WatchRecords.get(DataAnalysisTables.SCHEDULE, schedule.getValue("id"));
      assertThat(stored.getValueString("last_status")).contains("Sent 2", "denied", "missing");
      assertThat(stored.getValueInstant("last_run_at")).isEqualTo(NOW);
      assertThat(stored.getValueInstant("next_run_at")).isAfter(NOW);
      assertThat(runner.runDue(NOW)).isZero();
      assertThat(WatchesTest.Sink.raised).isEmpty();
   }

   @Test
   void providerFailureAndMissingOwnerRaiseVisibleAlerts() throws Exception
   {
      var schedule = schedule("COLUMN_PROFILE", Map.of("fieldName", "amount"), "XLSX");
      config.withMessagingProviderName(null);
      assertThat(runner.sendNow(schedule.getValue("id"), NOW)).contains("No messaging provider configured");
      assertThat(WatchesTest.Sink.raised).hasSize(1);
      assertThat(WatchesTest.Sink.raised.getFirst().getAlertTypeName()).isEqualTo(ScheduleRunner.ALERT_TYPE);
      config.withMessagingProviderName("mail").withOwnerSessionSupplier(id -> null);
      assertThat(runner.sendNow(schedule.getValue("id"), NOW)).contains("User session unavailable");
   }

   @Test
   void xlsxDeliveryFieldDenialAndManualReviewResults() throws Exception
   {
      var schedule = schedule("COLUMN_PROFILE", Map.of("fieldName", "amount"), "XLSX");
      assertThat(runner.sendNow(schedule.getValue("id"), NOW)).contains("Sent 2");
      var attachment = Mail.sent.getFirst().getAttachmentList().getFirst();
      assertThat(attachment.getName()).isEqualTo("analysis.xlsx");
      try(var workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(attachment.getContents())))
      {
         assertThat(workbook.getSheetAt(0).getLastRowNum()).isGreaterThan(5);
      }
      Mail.fail = true;
      assertThat(runner.sendNow(schedule.getValue("id"), NOW)).contains("delivery failed for: alice, bob");
      assertThat(instance.getProcess(ScheduleActions.SCHEDULED).getIsHidden()).isTrue();
      assertThat(instance.getProcess(ScheduleActions.SEND).getTableName()).isEqualTo(DataAnalysisTables.SCHEDULE);
      var input = new RunBackendStepInput().withProcessName(ScheduleActions.SEND).withRecords(List.of(schedule));
      var output = new RunBackendStepOutput();
      var review = new ScheduleActions.Review();
      review.runOnePage(input, output);
      assertThat(review.getProcessSummary(output, false).toString()).contains("authorized recipients");
      // Runtime has no messaging config: Result reports that real failure rather than claiming success.
      var result = new ScheduleActions.Result();
      result.runOnePage(input, new RunBackendStepOutput());
      assertThat(result.getProcessSummary(output, true).toString()).contains("No messaging provider");
      QContext.init(instance, SavedAnalysesTest.viewer("bob", 2));
      assertThatThrownBy(() -> runner.sendNow(schedule.getValue("id"), NOW)).isInstanceOf(QException.class);
   }

   @Test
   void recipientMustHaveFullFieldAccessAndHtmlFormatHasNoAttachment() throws Exception
   {
      var schedule = schedule("TABLE_PROFILE", Map.of("fields", List.of("amount", "status")), "HTML_SUMMARY");
      config.withFieldAnalysisAccessResolver(() -> (table, field) ->
         field.getName().equals("amount") && QContext.getQSession().getUser().getIdReference().equals("bob")
            ? com.kingsrook.qbits.dataanalysis.engine.FieldAnalysisAccess.NONE : com.kingsrook.qbits.dataanalysis.engine.FieldAnalysisAccess.FULL);
      var restricted = new ScheduleRunner(config, new DataAnalysisService(config));
      assertThat(restricted.sendNow(schedule.getValue("id"), NOW)).contains("Sent 1", "bob", "denied", "missing");
      assertThat(Mail.sent).hasSize(1);
      assertThat(Mail.sent.getFirst().getAttachmentList()).isNullOrEmpty();
      var output = new RunBackendStepOutput();
      new ScheduleActions.Scheduled().run(new RunBackendStepInput(), output);
      assertThat(output.getValue("count")).isNotNull();
   }

   @Test
   void stockSmtpAttachmentLimitationFailsBeforeSending() throws Exception
   {
      var schedule = schedule("COLUMN_PROFILE", Map.of("fieldName", "amount"), "CSV");
      var smtp = new com.kingsrook.qqq.backend.core.model.metadata.messaging.email.EmailMessagingProviderMetaData();
      smtp.withName("smtp");
      instance.addMessagingProvider(smtp);
      config.withMessagingProviderName("smtp");
      assertThat(runner.sendNow(schedule.getValue("id"), NOW)).contains("does not support attachments");
      assertThat(Mail.sent).isEmpty();
      assertThat(WatchesTest.Sink.raised).hasSize(1);
   }
}
