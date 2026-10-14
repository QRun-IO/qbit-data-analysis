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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.dataanalysis.app.DataAnalysisHome;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.processes.WatchActions;
import com.kingsrook.qbits.dataanalysis.watch.WatchEvaluator;
import com.kingsrook.qbits.dataanalysis.watch.WatchRecords;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.alerts.AlertResolution;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.AlertSinkInterface;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*******************************************************************************
 ** Real core memory rows, owner sessions, alerts SPI and record-action summaries.
 *******************************************************************************/
class WatchesTest
{
   static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);
   static final Instant NOW = TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC);
   private QInstance instance;
   private DataAnalysisQBitConfig config;
   private WatchEvaluator evaluator;
   private final List<Integer> latestIds = new ArrayList<>();

   public static class Sink implements AlertSinkInterface
   {
      static final List<AlertSignal> raised = new ArrayList<>();
      static final List<AlertResolution> resolved = new ArrayList<>();

      @Override
      public void raise(AlertSignal signal)
      {
         raised.add(signal);
      }

      @Override
      public void resolve(AlertResolution resolution)
      {
         resolved.add(resolution);
      }

      @Override
      public void heartbeat(String key, Instant at)
      {
      }
   }

   static QSession owner(String user)
   {
      return (TestSupport.user(user).withPermissions("orders.read", "orders.analyze", "data_analysis.watch.read", "data_analysis.watch.edit", "data_analysis.saved.read")
         .withValue(QSession.VALUE_KEY_USER_TIMEZONE, "UTC"));
   }

   @BeforeEach
   void setup() throws Exception
   {
      instance = TestSupport.instance();
      instance.addTable(new QTableMetaData().withName("orders").withLabel("Orders").withBackendName("memory").withPrimaryKeyField("id")
         .withPermissionRules(DataAnalysisTables.permissionRules("orders"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("amount", QFieldType.DECIMAL))
         .withField(new QFieldMetaData("created", QFieldType.DATE_TIME)).withField(new QFieldMetaData("day", QFieldType.DATE))
         .withField(new QFieldMetaData("category", QFieldType.STRING)).withField(new QFieldMetaData("owner", QFieldType.STRING))
         .withRecordSecurityLock(new RecordSecurityLock().withFieldName("owner").withSecurityKeyType(DataAnalysisTables.USER_KEY)));
      new QInstanceEnricher(instance).enrich();
      QContext.init(instance, TestSupport.admin().withPermissions("orders.read", "orders.insert", "orders.edit", "orders.analyze")
         .withValue(QSession.VALUE_KEY_USER_TIMEZONE, "UTC"));
      int[] counts = {10, 11, 9, 60};
      for(int day = 0; day < 4; day++)
      {
         for(int i = 0; i < counts[day]; i++)
         {
            QRecord row = order(TODAY.minusDays(4 - day), "alice", i % 2 == 0 ? null : BigDecimal.TEN, day == 3 ? "new" : "old");
            if(day == 3)
            {
               latestIds.add(row.getValueInteger("id"));
            }
         }
      }
      // Another owner and the incomplete current bucket must never affect Alice.
      order(TODAY.minusDays(1), "bob", new BigDecimal("1000000"), "private");
      order(TODAY, "alice", new BigDecimal("1000000"), "incomplete");
      config = new DataAnalysisQBitConfig().withBackendName("memory").withOwnerSessionSupplier(WatchesTest::owner);
      new DataAnalysisRuntime(config).addSelfToInstance(instance);
      evaluator = DataAnalysisRuntime.watches();
      Sink.raised.clear();
      Sink.resolved.clear();
      AlertsInstanceMetaData.ofOrWithNew(instance).withSink(new QCodeReference(Sink.class));
   }

   private QRecord order(LocalDate day, String owner, BigDecimal amount, String category) throws Exception
   {
      return (TestSupport.insert("orders", new QRecord().withValue("created", day.atTime(12, 0).toInstant(ZoneOffset.UTC))
         .withValue("day", day).withValue("owner", owner).withValue("amount", amount).withValue("category", category)));
   }

   private QRecord watch(String detector, String metric, String parameters) throws Exception
   {
      return (TestSupport.insert(DataAnalysisTables.WATCH, WatchDetectorsTest.definition(detector, metric, parameters)));
   }

   private QRecord stored(QRecord watch) throws Exception
   {
      return (WatchRecords.get(DataAnalysisTables.WATCH, watch.getValue("id")));
   }

   @AfterEach
   void cleanup()
   {
      QContext.clear();
   }

   @Test
   void raisesAndResolvesOnceUsingOwnerRows() throws Exception
   {
      QRecord watch = watch("SPIKE", "COUNT", "{\"k\":3}");
      QSession caller = QContext.getQSession();
      var verdict = evaluator.evaluate(watch.getValue("id"), NOW);
      assertThat(verdict.firing()).isTrue();
      assertThat(verdict.value()).isEqualTo(60.0);
      assertThat(QContext.getQSession()).isSameAs(caller);
      assertThat(stored(watch).getValueString("state")).isEqualTo("FIRING");
      assertThat(stored(watch).getValueInstant("last_evaluated_at")).isEqualTo(NOW);
      assertThat(Sink.raised).hasSize(1);
      AlertSignal signal = Sink.raised.getFirst();
      assertThat(signal.getAlertTypeName()).isEqualTo(WatchEvaluator.ALERT_TYPE);
      assertThat(signal.getDedupKey()).isEqualTo("data_analysis_watch:" + watch.getValue("id"));
      assertThat(signal.getRecordRefs().getFirst().getTableName()).isEqualTo(DataAnalysisTables.WATCH);
      assertThat(signal.getLinks().getFirst().getTableName()).isEqualTo("orders");
      assertThat(signal.getLinks().getFirst().getUrl()).isNull();
      assertThat(signal.getLinks().getFirst().getFilter()).isNotNull();
      evaluator.evaluate(watch.getValue("id"), NOW);
      assertThat(Sink.raised).hasSize(1);
      for(int id : latestIds.subList(10, latestIds.size()))
      {
         UpdateInput update = new UpdateInput("orders").withRecords(List.of(new QRecord().withValue("id", id)
            .withValue("created", TODAY.plusDays(1).atTime(12, 0).toInstant(ZoneOffset.UTC))));
         update.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         assertThat(new UpdateAction().execute(update).getRecords().getFirst().getErrors()).isNullOrEmpty();
      }
      assertThat(evaluator.evaluate(watch.getValue("id"), NOW).firing()).isFalse();
      assertThat(stored(watch).getValueString("state")).isEqualTo("OK");
      assertThat(Sink.resolved).hasSize(1);
      assertThat(Sink.resolved.getFirst().getDedupKey()).isEqualTo(signal.getDedupKey());
      evaluator.evaluate(watch.getValue("id"), NOW);
      assertThat(Sink.resolved).hasSize(1);
   }

   @Test
   void missingOrMismatchedOwnerSessionsNeverReadAsCaller() throws Exception
   {
      QRecord watch = watch("SPIKE", "COUNT", "{\"k\":3}");
      config.withOwnerSessionSupplier(id -> null);
      assertThat(evaluator.evaluate(watch.getValue("id"), NOW).message()).isEqualTo("Owner session unavailable");
      assertThat(stored(watch).getValue("last_value")).isNull();
      config.withOwnerSessionSupplier(id -> owner("bob"));
      assertThat(evaluator.evaluate(watch.getValue("id"), NOW).message()).isEqualTo("Owner session unavailable");
      config.withOwnerSessionSupplier(null);
      assertThat(evaluator.evaluate(watch.getValue("id"), NOW).message()).isEqualTo("Owner session unavailable");
      assertThat(Sink.raised).isEmpty();
   }

   @Test
   void revokedReadOrAnalyzeSkipsWithoutResolving() throws Exception
   {
      QRecord watch = watch("SPIKE", "COUNT", "{\"k\":3}");
      evaluator.evaluate(watch.getValue("id"), NOW);
      for(String revoked : List.of("orders.read", "orders.analyze"))
      {
         config.withOwnerSessionSupplier(id ->
         {
            QSession session = owner(id);
            session.getPermissions().remove(revoked);
            return (session);
         });
         var verdict = evaluator.evaluate(watch.getValue("id"), NOW);
         assertThat(verdict.evaluated()).isFalse();
         assertThat(verdict.message()).contains("Owner lacks READ or Analyze");
         assertThat(verdict.value()).isNull();
         assertThat(verdict.drill()).isNull();
         assertThat(stored(watch).getValueString("state")).isEqualTo("FIRING");
         assertThat(stored(watch).getValue("last_value")).isNull();
      }
      assertThat(Sink.resolved).isEmpty();
      assertThat(Sink.raised).hasSize(1);
   }

   @Test
   void previewCoversAllMetricsAndPrivacy() throws Exception
   {
      QContext.setQSession(owner("alice"));
      for(String metric : List.of("COUNT", "SUM", "AVG", "NULL_SHARE", "CATEGORY_SET"))
      {
         String detector = metric.equals("CATEGORY_SET") ? "NEW_CATEGORY" : metric.equals("NULL_SHARE") ? "NULL_RATE_CREEP" : "SPIKE";
         QRecord definition = WatchDetectorsTest.definition(detector, metric, "{\"k\":3}");
         if(metric.equals("CATEGORY_SET"))
         {
            definition.withValue("metric_json", "{\"operator\":\"CATEGORY_SET\",\"field\":\"category\"}");
         }
         var preview = new DataAnalysisService(config).watchPreview(definition, NOW);
         assertThat(preview.series()).hasSize(4);
         assertThat(preview.chart().getKind().name()).isEqualTo("TIME_SERIES");
         assertThat(preview.chart().getAnnotations()).isNotEmpty();
         assertThat(preview.verdict().evaluated()).isTrue();
         if(metric.equals("CATEGORY_SET"))
         {
            assertThat(preview.verdict().firing()).isTrue();
            assertThat(preview.series().getLast().categories()).containsExactly("new");
         }
         if(metric.equals("AVG"))
         {
            assertThat(preview.verdict().value()).isEqualTo(10.0);
         }
         if(metric.equals("NULL_SHARE"))
         {
            assertThat(preview.verdict().value()).isEqualTo(0.5);
         }
      }
      var guarded = new DataAnalysisService(new DataAnalysisQBitConfig().withBackendName("memory").withDefaultMinGroupSize(12))
         .watchPreview(WatchDetectorsTest.definition("SPIKE", "COUNT", "{\"k\":3}"), NOW);
      assertThat(guarded.series()).isEmpty();
      assertThat(guarded.verdict().message()).contains("Privacy guard");
      assertThat(guarded.verdict().drill()).isNull();
   }

   @Test
   void fullFieldPolicyAndRowLimitAreEnforced() throws Exception
   {
      QContext.setQSession(owner("alice"));
      var restricted = new DataAnalysisQBitConfig().withBackendName("memory").withExcludeTables(java.util.Set.of("orders"));
      assertThatThrownBy(() -> new DataAnalysisService(restricted).watchPreview(WatchDetectorsTest.definition("SPIKE", "COUNT", "{\"k\":3}"), NOW))
         .hasMessageContaining("unavailable");
      config.withFieldAnalysisAccessResolver(() -> (table, field) -> field.getName().equals("amount")
         ? com.kingsrook.qbits.dataanalysis.engine.FieldAnalysisAccess.NONE : com.kingsrook.qbits.dataanalysis.engine.FieldAnalysisAccess.FULL);
      QRecord definition = WatchDetectorsTest.definition("SPIKE", "SUM", "{\"k\":3}");
      assertThatThrownBy(() -> new DataAnalysisService(config).watchPreview(definition, NOW)).hasMessageContaining("Unknown analysis field");
      definition.withValue("metric_json", "{\"operator\":\"COUNT\"}").withValue("filter_json", "{\"criteria\":[{\"fieldName\":\"amount\",\"operator\":\"IS_BLANK\"}]}");
      assertThatThrownBy(() -> new DataAnalysisService(config).watchPreview(definition, NOW)).hasMessageContaining("Unknown analysis field");
   }

   @Test
   void actionsHaveReviewAndResultLinesAndEnableDisable() throws Exception
   {
      QRecord spike = watch("SPIKE", "COUNT", "{\"k\":3}");
      QRecord ok = watch("OUT_OF_RANGE", "COUNT", "{\"min\":0,\"max\":100}");
      QRecord secondOk = watch("DROP", "COUNT", "{\"k\":3}");
      RunBackendStepInput input = new RunBackendStepInput().withProcessName(WatchActions.EVALUATE).withRecords(List.of(spike, ok, secondOk));
      RunBackendStepOutput output = new RunBackendStepOutput();
      WatchActions.Review review = new WatchActions.Review();
      review.runOnePage(input, output);
      assertThat(review.getProcessSummary(output, false)).hasSize(3).allSatisfy(line -> assertThat(line.toString()).contains("will be evaluated"));
      assertThat(Sink.raised).isEmpty();
      WatchActions.Result result = new WatchActions.Result();
      result.runOnePage(input, new RunBackendStepOutput());
      assertThat(result.getProcessSummary(output, true).getFirst().toString()).contains("3 watches evaluated: 1 firing, 2 OK");
      for(String action : List.of(WatchActions.DISABLE, WatchActions.ENABLE))
      {
         WatchActions.Result load = new WatchActions.Result();
         load.runOnePage(new RunBackendStepInput().withProcessName(action).withRecords(List.of(spike)), new RunBackendStepOutput());
         assertThat(stored(spike).getValueBoolean("enabled")).isEqualTo(action.equals(WatchActions.ENABLE));
         assertThat(load.getProcessSummary(output, true).getFirst().toString()).contains(action.equals(WatchActions.ENABLE) ? "1 watch enabled" : "1 watch disabled");
      }
   }

   @Test
   void scheduleEvaluatesEnabledAndHomeLinksFiringFirst() throws Exception
   {
      QRecord spike = watch("SPIKE", "COUNT", "{\"k\":3}");
      QRecord disabled = watch("DROP", "COUNT", "{\"k\":3}");
      evaluator.setEnabled(disabled.getValue("id"), false);
      assertThat(evaluator.evaluateEnabled(NOW)).isEqualTo(1);
      assertThat(stored(disabled).getValue("last_evaluated_at")).isNull();
      var data = (com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData) DataAnalysisHome.render(DataAnalysisHome.FIRING, NOW);
      assertThat(data.getRows()).hasSize(1);
      assertThat(data.getRows().getFirst().get("c0").toString()).contains("/" + spike.getValue("id") + "\"");
      assertThat(instance.getProcess(WatchActions.SCHEDULED).getIsHidden()).isTrue();
      assertThat(instance.getProcess(WatchActions.EVALUATE).getTableName()).isEqualTo(DataAnalysisTables.WATCH);
   }

   @Test
   void standardCrudValidatesPartialUpdatesAndOwnerLocks() throws Exception
   {
      QRecord watch = watch("SPIKE", "COUNT", "{\"k\":3}");
      UpdateInput input = new UpdateInput(DataAnalysisTables.WATCH).withRecords(List.of(new QRecord().withValue("id", watch.getValue("id")).withValue("parameters_json", "{\"k\":0}")));
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      assertThat(new UpdateAction().execute(input).getRecords().getFirst().getErrors()).isNotEmpty();
      assertThat(stored(watch).getValueString("parameters_json")).contains("3");
      InsertInput invalid = new InsertInput(DataAnalysisTables.WATCH).withRecord(WatchDetectorsTest.definition("OUT_OF_RANGE", "COUNT", "{}"));
      invalid.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      assertThat(new InsertAction().execute(invalid).getRecords().getFirst().getErrors()).isNotEmpty();
      QContext.setQSession(owner("bob"));
      assertThatThrownBy(() -> evaluator.preview(watch.getValue("id"), NOW)).hasMessageContaining("unavailable");
      assertThatThrownBy(() -> evaluator.evaluate(watch.getValue("id"), NOW)).hasMessageContaining("unavailable");
      QContext.setQSession(owner("alice"));
      QContext.getQSession().getPermissions().remove("data_analysis.watch.edit");
      assertThatThrownBy(() -> evaluator.evaluate(watch.getValue("id"), NOW)).hasMessageContaining("editing is unavailable");
      assertThat(Sink.raised).isEmpty();
   }

   @Test
   void savedAnalysisResolvesAsOwnerAndNoAlertsInstallationStillStoresState() throws Exception
   {
      QRecord saved = TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("alice", false)
         .withValue("kind", "PIVOT").withValue("table_name", "orders").withValue("definition_json", "{\"values\":[{\"operator\":\"COUNT\"}]}"));
      QRecord definition = WatchDetectorsTest.definition("SPIKE", "COUNT", "{\"k\":3}")
         .withValue("saved_id", saved.getValue("id")).withValue("table_name", null).withValue("metric_json", null);
      QRecord watch = TestSupport.insert(DataAnalysisTables.WATCH, definition);
      AlertsInstanceMetaData.of(instance).withSink(null);
      assertThat(evaluator.evaluate(watch.getValue("id"), NOW).firing()).isTrue();
      assertThat(stored(watch).getValueString("state")).isEqualTo("FIRING");
      assertThat(Sink.raised).isEmpty();
   }

   public static class AutomatedSessions implements com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface
   {
      @Override
      public void customizeAutomatedSessionForUser(QInstance instance, QSession session, java.io.Serializable userId)
      {
         QSession grants = owner(userId.toString());
         session.setPermissions(grants.getPermissions());
         session.setSecurityKeyValues(grants.getSecurityKeyValues());
         session.withValue(QSession.VALUE_KEY_USER_TIMEZONE, "UTC");
      }
   }

   @Test
   void hostAuthenticationModuleSuppliesTheOwnerSession() throws Exception
   {
      config.withOwnerSessionSupplier(null);
      instance.withInstanceDefaultAuthentication(new com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData()
         .withName("owner-auth").withType(com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType.MOCK).withCustomizer(new QCodeReference(AutomatedSessions.class)));
      QRecord watch = watch("SPIKE", "COUNT", "{\"k\":3}");
      assertThat(evaluator.evaluate(watch.getValue("id"), NOW).value()).isEqualTo(60.0);
      assertThat(Sink.raised).hasSize(1);
   }

   @Test
   void allCalendarUnitsExcludeCurrentBucketAndHaveExactDrills() throws Exception
   {
      for(String unit : List.of("HOUR", "DAY", "WEEK", "MONTH"))
      {
         java.time.ZonedDateTime end = NOW.atZone(ZoneOffset.UTC);
         end = switch(unit)
         {
            case "HOUR" -> end.truncatedTo(java.time.temporal.ChronoUnit.HOURS);
            case "WEEK" -> end.toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay(ZoneOffset.UTC);
            case "MONTH" -> end.toLocalDate().withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC);
            default -> end.toLocalDate().atStartOfDay(ZoneOffset.UTC);
         };
         java.time.temporal.ChronoUnit duration = switch(unit)
         {
            case "HOUR" -> java.time.temporal.ChronoUnit.HOURS;
            case "WEEK" -> java.time.temporal.ChronoUnit.WEEKS;
            case "MONTH" -> java.time.temporal.ChronoUnit.MONTHS;
            default -> java.time.temporal.ChronoUnit.DAYS;
         };
         for(int ago : new int[] {2, 1, 1, 0})
         {
            TestSupport.insert("orders", new QRecord().withValue("owner", "alice").withValue("category", unit)
               .withValue("created", end.minus(ago, duration).toInstant()).withValue("day", end.minus(ago, duration).toLocalDate()));
         }
         QRecord definition = WatchDetectorsTest.definition("SPIKE", "COUNT", "{\"k\":1}").withValue("bucket", unit)
            .withValue("date_field", unit.equals("HOUR") ? "created" : "day")
            .withValue("filter_json", com.kingsrook.qqq.backend.core.utils.JsonUtils.toJson(DrillFilters.condition("category", QCriteriaOperator.EQUALS, unit)));
         QContext.setQSession(owner("alice"));
         var preview = new DataAnalysisService(config).watchPreview(definition, NOW);
         assertThat(preview.series()).as(unit).hasSize(2);
         assertThat(preview.verdict().value()).as(unit).isEqualTo(2.0);
         assertThat(preview.verdict().firing()).as(unit).isTrue();
         var count = new com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput("orders").withFilter(preview.verdict().drill().getFilter());
         count.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         assertThat(new com.kingsrook.qqq.backend.core.actions.tables.CountAction().execute(count).getCount()).as(unit).isEqualTo(2);
         QContext.setQSession(TestSupport.admin().withPermissions("orders.insert"));
      }
   }

   @Test
   void streamedProcessRunsSelectedRecordsThroughCore() throws Exception
   {
      QRecord watch = watch("SPIKE", "COUNT", "{\"k\":3}");
      var input = new com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput().withProcessName(WatchActions.EVALUATE)
         .withFrontendStepBehavior(com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput.FrontendStepBehavior.SKIP)
         .withCallback(new com.kingsrook.qqq.backend.core.actions.processes.QProcessCallback()
         {
            @Override
            public com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter getQueryFilter()
            {
               return (DrillFilters.condition("id", QCriteriaOperator.EQUALS, watch.getValue("id")));
            }
         });
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      var output = new com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction().execute(input);
      assertThat(output.getException()).isEmpty();
      assertThat(output.getValue(com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY).toString())
         .contains("1 watch evaluated: 1 firing, 0 OK");
      assertThat(stored(watch).getValueString("state")).isEqualTo("FIRING");
   }

   @Test
   void configuredSchedulerUsesFifteenMinuteInterval() throws Exception
   {
      var configured = new QInstance();
      configured.addBackend(new com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData().withName("memory")
         .withBackendType(com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule.class));
      new DataAnalysisQBitProducer().withQBitConfig(new DataAnalysisQBitConfig().withBackendName("memory").withSchedulerName("hostScheduler")).produce(configured);
      var schedule = configured.getProcess(WatchActions.SCHEDULED).getSchedule();
      assertThat(schedule.getSchedulerName()).isEqualTo("hostScheduler");
      assertThat(schedule.getRepeatSeconds()).isEqualTo(900);
      new WatchActions.Scheduled().run(new RunBackendStepInput(), new RunBackendStepOutput());
   }
}
