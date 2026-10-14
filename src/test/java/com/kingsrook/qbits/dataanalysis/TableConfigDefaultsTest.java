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


import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qbits.dataanalysis.app.DataAnalysisHome;
import com.kingsrook.qbits.dataanalysis.app.DataAnalysisSetupStatus;
import com.kingsrook.qbits.dataanalysis.engine.EffectiveConfig;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.processes.TableConfigDefaultsProcess;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfileRequest;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** "Create rows for analyzable tables" writes the settings that already apply
 ** and changes nothing; the rows then drive the runtime (including the result
 ** cache minutes); and the Setup card reports it all. Runs on the memory backend
 ** with the instance's own runtime, exactly as a host would.
 *******************************************************************************/
class TableConfigDefaultsTest extends EngineFixture
{
   /*******************************************************************************
    ** The settings every analyzable table resolves to, as one comparable string.
    *******************************************************************************/
   private String effective(String table) throws Exception
   {
      EffectiveConfig c = DataAnalysisRuntime.service().getConfigResolver().resolve(table);
      return (c.getEnabled() + " " + c.getMaxRows() + " " + c.getSampleRows() + " " + c.getSampleThresholdRows() + " " + c.getMinGroupSize() + " "
         + c.getCacheTtlMinutes() + " " + new java.util.TreeSet<>(c.getAnalyzableFields()) + " " + new java.util.TreeSet<>(c.getExcludedFields()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private List<ProcessSummaryLine> create() throws Exception
   {
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(TableConfigDefaultsProcess.NAME);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      RunProcessOutput output = new RunProcessAction().execute(input);
      return ((List<ProcessSummaryLine>) output.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> configRows() throws Exception
   {
      QueryInput input = new QueryInput(DataAnalysisTables.TABLE_CONFIG).withFilter(new QQueryFilter());
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new QueryAction().execute(input).getRecords());
   }



   /*******************************************************************************
    ** Rows hold the effective settings (table defaults over global defaults),
    ** excluded tables get none, and every table resolves exactly as before.
    *******************************************************************************/
   @Test
   void rowsRestateTheEffectiveSettingsAndChangeNothing() throws Exception
   {
      DataAnalysisQBitConfig runtime = DataAnalysisRuntime.service().getConfigResolver().getDefaults();
      runtime.withDefaultMaxRows(500).withTableDefaults(Map.of("orders", new TableAnalysisDefaults().withMaxRows(100).withSampleRows(8).withMinGroupSize(2)
         .withAnalyzableFields(Set.of("amount", "note")).withExcludedFields(Set.of("note"))));
      DataAnalysisRuntime.service().getConfigResolver().invalidate();
      String ordersBefore   = effective("orders");
      String customerBefore = effective("customer");
      assertThat(ordersBefore).startsWith("true 100 8 ");

      List<ProcessSummaryLine> summary = create();
      assertThat(summary).isNotEmpty();
      List<QRecord> rows = configRows();
      assertThat(rows).extracting(row -> row.getValueString("table_name")).containsExactlyInAnyOrder("customer", "orders");
      QRecord orders = rows.stream().filter(row -> "orders".equals(row.getValueString("table_name"))).findFirst().orElseThrow();
      assertThat(orders.getValueInteger("max_rows")).isEqualTo(100);
      assertThat(orders.getValueInteger("sample_rows")).isEqualTo(8);
      assertThat(orders.getValueInteger("min_group_size")).isEqualTo(2);
      assertThat(orders.getValueInteger("cache_ttl_minutes")).isEqualTo(60);
      assertThat(orders.getValueString("analyzable_fields")).isEqualTo("amount,note");
      assertThat(orders.getValueString("excluded_fields")).isEqualTo("note");
      QRecord customer = rows.stream().filter(row -> "customer".equals(row.getValueString("table_name"))).findFirst().orElseThrow();
      assertThat(customer.getValueInteger("max_rows")).isEqualTo(500);
      assertThat(customer.getValueString("analyzable_fields")).isNull();
      assertThat(customer.getValueBoolean("enabled")).isTrue();

      DataAnalysisRuntime.service().getConfigResolver().invalidate();
      assertThat(effective("orders")).isEqualTo(ordersBefore);
      assertThat(effective("customer")).isEqualTo(customerBefore);
   }



   /*******************************************************************************
    ** Tables excluded by configuration get no row; a table added later gets one on
    ** the next run; existing rows are never touched and an edit drives the runtime.
    *******************************************************************************/
   @Test
   void insertOnlyExcludedTablesSkippedEditsLive() throws Exception
   {
      DataAnalysisRuntime.service().getConfigResolver().getDefaults().withExcludeTables(Set.of("customer"));
      create();
      assertThat(configRows()).extracting(row -> row.getValueString("table_name")).containsExactly("orders");

      QRecord orders = configRows().getFirst();
      UpdateInput update = new UpdateInput(DataAnalysisTables.TABLE_CONFIG).withRecords(List.of(new QRecord().withValue("id", orders.getValue("id")).withValue("max_rows", 7)));
      update.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      new UpdateAction().execute(update);
      DataAnalysisRuntime.service().getConfigResolver().invalidate();
      assertThat(effective("orders")).startsWith("true 7 ");

      instance.addTable(new QTableMetaData().withName("extra").withBackendName("memory").withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER)));
      create();
      assertThat(configRows()).extracting(row -> row.getValueString("table_name")).containsExactlyInAnyOrder("orders", "extra");
      assertThat(configRows().stream().filter(row -> "orders".equals(row.getValueString("table_name"))).findFirst().orElseThrow().getValueInteger("max_rows")).isEqualTo(7);

      assertThat(create()).singleElement().satisfies(line -> assertThat(line.getMessage()).contains("already has a settings row"));
      assertThat(configRows()).hasSize(2);
   }



   /*******************************************************************************
    ** The result cache uses the table's settings row, not only the global default:
    ** a row with more minutes keeps an aged run as a cache hit (the global 60
    ** minutes would miss it), a row with 0 turns caching off, and a row with 1
    ** minute misses a run that is two minutes old.
    *******************************************************************************/
   @Test
   void settingsRowCacheMinutesChangeCaching() throws Exception
   {
      String id = service.startExactRun(column());
      age(id, 7200);
      assertThat(service.startExactRun(column())).as("default 60 minutes: a two hour old run is not reused").isNotEqualTo(id);

      TestSupport.insert(DataAnalysisTables.TABLE_CONFIG, new QRecord().withValue("table_name", "orders").withValue("cache_ttl_minutes", 240));
      service.getConfigResolver().invalidate();
      String fresh = service.startExactRun(column());
      age(fresh, 7200);
      assertThat(service.startExactRun(column())).as("row with 240 minutes: the two hour old run is reused").isEqualTo(fresh);

      setTtl(1);
      age(fresh, 120);
      assertThat(service.startExactRun(column())).as("row with 1 minute: a two minute old run is not reused").isNotEqualTo(fresh);

      setTtl(0);
      String zero = service.startExactRun(column());
      assertThat(service.startExactRun(column())).as("row with 0 minutes: never reused").isNotEqualTo(zero);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ColumnProfileRequest column()
   {
      return (new ColumnProfileRequest().withTableName("orders").withFieldName("amount"));
   }



   /*******************************************************************************
    ** Make a stored run look older.
    *******************************************************************************/
   private void age(String id, int seconds) throws Exception
   {
      UpdateInput update = new UpdateInput(DataAnalysisTables.RUN)
         .withRecords(List.of(new QRecord().withValue("id", Integer.valueOf(id)).withValue("finished_at", Instant.now().minusSeconds(seconds))));
      update.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      new UpdateAction().execute(update);
   }



   /*******************************************************************************
    ** Edit the orders settings row's cache minutes.
    *******************************************************************************/
   private void setTtl(int minutes) throws Exception
   {
      QRecord row = configRows().stream().filter(r -> "orders".equals(r.getValueString("table_name"))).findFirst().orElseThrow();
      UpdateInput update = new UpdateInput(DataAnalysisTables.TABLE_CONFIG).withRecords(List.of(new QRecord().withValue("id", row.getValue("id")).withValue("cache_ttl_minutes", minutes)));
      update.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      new UpdateAction().execute(update);
      service.getConfigResolver().invalidate();
   }



   /*******************************************************************************
    ** The Result cache line says what each table resolves to, including mixed
    ** settings, and a settings error is shown as needing attention, not as "on".
    *******************************************************************************/
   @Test
   void resultCacheLineIsAccurate() throws Exception
   {
      DataAnalysisRuntime.service().getConfigResolver().getDefaults().withDefaultCacheTtlMinutes(0);
      DataAnalysisRuntime.service().getConfigResolver().invalidate();
      assertThat(DataAnalysisSetupStatus.load().get(1).detail()).startsWith("Off");

      TestSupport.insert(DataAnalysisTables.TABLE_CONFIG, new QRecord().withValue("table_name", "orders").withValue("cache_ttl_minutes", 30));
      DataAnalysisRuntime.service().getConfigResolver().invalidate();
      DataAnalysisSetupStatus.Item mixed = DataAnalysisSetupStatus.load().get(1);
      assertThat(mixed.state()).isEqualTo(DataAnalysisSetupStatus.State.OK);
      assertThat(mixed.detail()).contains("default 0 min").contains("1 table: off").contains("1 table: 30 min");

      TestSupport.insert(DataAnalysisTables.TABLE_CONFIG, new QRecord().withValue("table_name", "customer").withValue("cache_ttl_minutes", -5));
      DataAnalysisRuntime.service().getConfigResolver().invalidate();
      DataAnalysisSetupStatus.Item broken = DataAnalysisSetupStatus.load().get(1);
      assertThat(broken.state()).isEqualTo(DataAnalysisSetupStatus.State.UNKNOWN);
      assertThat(broken.detail()).contains("could not be read");
   }



   /*******************************************************************************
    ** The Setup card: defaults before rows, set up after, from the runtime's own
    ** resolution; optional features say what is missing.
    *******************************************************************************/
   @Test
   void setupCard() throws Exception
   {
      List<DataAnalysisSetupStatus.Item> before = DataAnalysisSetupStatus.load();
      assertThat(before).extracting(DataAnalysisSetupStatus.Item::label).containsExactly("Analysis settings", "Result cache", "Saved analyses", "Watches", "Schedules");
      assertThat(before.get(0).state()).isEqualTo(DataAnalysisSetupStatus.State.DEFAULT);
      assertThat(before.get(0).detail()).contains("All 2 analyzable tables are on");
      assertThat(before.get(0).nextStep()).contains("Create rows for analyzable tables");
      assertThat(before.get(1).state()).isEqualTo(DataAnalysisSetupStatus.State.OK);
      assertThat(before.get(1).detail()).contains("60 minutes");
      assertThat(before.get(3).state()).isEqualTo(DataAnalysisSetupStatus.State.OPTIONAL);
      assertThat(before.get(3).detail()).contains("No scheduler is configured");

      create();
      List<DataAnalysisSetupStatus.Item> after = DataAnalysisSetupStatus.load();
      assertThat(after.get(0).state()).isEqualTo(DataAnalysisSetupStatus.State.OK);
      assertThat(after.get(0).detail()).contains("2 settings rows for 2 analyzable tables");

      TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("admin", false));
      assertThat(DataAnalysisSetupStatus.load().get(2).state()).isEqualTo(DataAnalysisSetupStatus.State.OK);

      TableData card = DataAnalysisHome.setup();
      assertThat(card.getRows()).hasSize(5);
      assertThat(card.getRows().getFirst().get("c1")).isEqualTo("Set up");
      assertThat(String.valueOf(card.getRows().getFirst().get("c0"))).contains("<a href=");
   }

}
