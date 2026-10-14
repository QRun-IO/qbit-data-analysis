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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dataanalysis.app.SavedAnalysisWidget;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.saved.AnalysisExport;
import com.kingsrook.qbits.dataanalysis.saved.SavedAnalyses;
import com.kingsrook.qbits.dataanalysis.saved.SavedDefinition;
import com.kingsrook.qbits.dataanalysis.saved.SqlHandoff;
import com.kingsrook.qbits.dataanalysis.watch.WatchRecords;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChartData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SavedAnalysesTest extends EngineFixture
{
   static QSession viewer(String id, int customer)
   {
      return (TestSupport.user(id).withPermissions("data_analysis.saved.read", "data_analysis.saved.insert", "data_analysis.saved.edit", "data_analysis.saved.delete",
         "orders.read", "orders.analyze", "customer.read").withSecurityKeyValue("customer_scope", customer));
   }

   static QRecord definition(String owner, boolean shared, String kind, Map<String, Object> body)
   {
      return (TestSupport.saved(owner, shared).withValue("table_name", "orders").withValue("kind", kind).withValue("definition_json", JsonUtils.toJson(body)));
   }

   @Test
   void sharedReadOwnerWriteAndFieldReduction() throws Exception
   {
      var saved = TestSupport.insert(DataAnalysisTables.SAVED, definition("alice", true, "TABLE_PROFILE", Map.of("fields", List.of("amount", "secret"))));
      var privateRow = TestSupport.insert(DataAnalysisTables.SAVED, definition("alice", false, "COLUMN_PROFILE", Map.of("fieldName", "amount")));
      QContext.init(instance, viewer("bob", 2));
      var api = new SavedAnalyses(service);
      assertThat(api.list()).extracting(SavedAnalyses.Entry::id).contains(saved.getValue("id")).doesNotContain(privateRow.getValue("id"));
      var result = api.run(saved.getValue("id"));
      assertThat(result.kind()).isEqualTo("TABLE_PROFILE");
      assertThat(result.warnings()).containsExactly(SavedDefinition.FIELD_WARNING);
      assertThat(result.tableProfile().getFields()).extracting(f -> f.getField()).containsExactly("amount");
      assertThat(((Number) result.tableProfile().getRowCount().getValue()).intValue()).isEqualTo(4);
      assertThatThrownBy(() -> api.run(privateRow.getValue("id"))).isInstanceOf(QException.class);
      var patch = new QRecord().withValue("id", saved.getValue("id")).withValue("name", "Stolen").withValue("read_owner_user_id", "bob");
      assertThat(new UpdateAction().execute(new UpdateInput(DataAnalysisTables.SAVED).withRecords(List.of(patch))).getRecords().getFirst().getErrors()).isNotEmpty();
      var deleted = new com.kingsrook.qqq.backend.core.actions.tables.DeleteAction().execute(
         new com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput(DataAnalysisTables.SAVED).withPrimaryKeys(List.of(saved.getValue("id"))));
      assertThat(deleted.getDeletedRecordCount()).isZero();
      QContext.init(instance, viewer("alice", 1));
      var updated = new UpdateAction().execute(new UpdateInput(DataAnalysisTables.SAVED).withRecords(List.of(new QRecord().withValue("id", saved.getValue("id")).withValue("shared", false)))).getRecords().getFirst();
      assertThat(updated.getErrors()).isNullOrEmpty();
      QContext.init(instance, viewer("bob", 2));
      assertThat(api.list()).isEmpty();
   }

   /*******************************************************************************
    ** A UI insert without an owner belongs to the inserting user; naming someone
    ** else is still refused for a non-administrator.
    *******************************************************************************/
   @Test
   void insertWithoutOwnerBelongsToTheCurrentUser() throws Exception
   {
      QContext.init(instance, viewer("bob", 2));
      QRecord row = definition("bob", false, "COLUMN_PROFILE", Map.of("fieldName", "amount"));
      row.removeValue("owner_user_id");
      QRecord inserted = new InsertAction().execute(new InsertInput(DataAnalysisTables.SAVED).withRecords(List.of(row))).getRecords().getFirst();
      assertThat(inserted.getErrors()).isEmpty();
      assertThat(inserted.getValueString("owner_user_id")).isEqualTo("bob");
      QRecord forged = definition("alice", false, "COLUMN_PROFILE", Map.of("fieldName", "amount"));
      assertThat(new InsertAction().execute(new InsertInput(DataAnalysisTables.SAVED).withRecords(List.of(forged))).getRecords().getFirst().getErrors()).isNotEmpty();
   }

   @Test
   void starsAreIdempotentAndPersonal() throws Exception
   {
      var saved = TestSupport.insert(DataAnalysisTables.SAVED, definition("alice", true, "COLUMN_PROFILE", Map.of("fieldName", "amount")));
      QContext.init(instance, viewer("bob", 2));
      var api = new SavedAnalyses(service);
      assertThat(api.star(saved.getValue("id"), true)).isTrue();
      api.star(saved.getValue("id"), true);
      assertThat(api.list().getFirst().starred()).isTrue();
      QContext.init(instance, viewer("alice", 1));
      assertThat(api.list().getFirst().starred()).isFalse();
      QContext.init(instance, viewer("bob", 2));
      assertThat(api.star(saved.getValue("id"), false)).isFalse();
      api.star(saved.getValue("id"), false);
      assertThat(api.list().getFirst().starred()).isFalse();
   }

   @Test
   void malformedDefinitionsAndPartialEditsReturnPlainErrors() throws Exception
   {
      for(String json : List.of("not json", "[]", "{}", "{\"fieldName\":\"missing\"}", "{\"tableName\":\"other\",\"fieldName\":\"amount\"}"))
      {
         QRecord row = definition("admin", false, "COLUMN_PROFILE", Map.of()).withValue("definition_json", json);
         var inserted = new InsertAction().execute(new InsertInput(DataAnalysisTables.SAVED).withRecords(List.of(row))).getRecords().getFirst();
         assertThat(inserted.getErrors()).isNotEmpty();
      }
      assertThatThrownBy(() -> SavedDefinition.parse("UNKNOWN", "orders", Map.of())).hasMessageContaining("kind");
      assertThatThrownBy(() -> SavedDefinition.parse("TABLE_PROFILE", "unknown", Map.of())).hasMessageContaining("table");
      var saved = TestSupport.insert(DataAnalysisTables.SAVED, definition("admin", false, "COLUMN_PROFILE", Map.of("fieldName", "amount")));
      var invalid = new QRecord().withValue("id", saved.getValue("id")).withValue("definition_json", "{\"fieldName\":\"nope\"}");
      assertThat(new UpdateAction().execute(new UpdateInput(DataAnalysisTables.SAVED).withRecords(List.of(invalid))).getRecords().getFirst().getErrors()).isNotEmpty();
      assertThat(WatchRecords.get(DataAnalysisTables.SAVED, saved.getValue("id")).getValueString("definition_json")).contains("amount");
   }

   @Test
   void allKindsRunAndExportRealTables() throws Exception
   {
      Map<String, Map<String, Object>> definitions = Map.of(
         "COLUMN_PROFILE", Map.of("fieldName", "amount"),
         "TABLE_PROFILE", Map.of("fields", List.of("amount", "status")),
         "PIVOT", Map.of("rows", List.of(Map.of("field", "status")), "values", List.of(Map.of("operator", "COUNT"))),
         "COMPARE", Map.of("fields", List.of("amount", "status")),
         "CORRELATION", Map.of("fields", List.of("amount", "quantity")),
         "CROSSTAB", Map.of("rowField", "status", "columnField", "paid"),
         "SHIFT", Map.of("field", "amount", "dateField", "created", "bucket", "DAY"));
      for(var entry : definitions.entrySet())
      {
         var saved = TestSupport.insert(DataAnalysisTables.SAVED, definition("alice", true, entry.getKey(), entry.getValue()));
         QContext.init(instance, viewer("alice", 1));
         var result = new SavedAnalyses(service).run(saved.getValue("id"));
         assertThat(result.kind()).isEqualTo(entry.getKey());
         assertThat(AnalysisExport.table(result).rows()).as(entry.getKey()).isNotEmpty();
         var csv = AnalysisExport.file(result, "csv");
         assertThat(new String(csv.bytes(), StandardCharsets.UTF_8)).contains(AnalysisExport.table(result).columns().getFirst());
         var xlsx = AnalysisExport.file(result, "xlsx");
         try(var workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsx.bytes())))
         {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(AnalysisExport.table(result).rows().size());
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo(AnalysisExport.table(result).columns().getFirst());
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo(AnalysisExport.table(result).rows().getFirst().getFirst());
         }
         var widget = SavedAnalysisWidget.widget().withDefaultValue("savedId", saved.getValue("id"));
         var chart = (ChartData) new SavedAnalysisWidget().render(new RenderWidgetInput().withWidgetMetaData(widget)).getWidgetData();
         assertThat(chart.getChartData().getDatasets()).as(entry.getKey()).isNotEmpty();
         assertThat(chart.getFooterHTML()).contains("View saved analysis");
         QContext.init(instance, session());
      }
      assertThatThrownBy(() -> AnalysisExport.format("pdf")).hasMessageContaining("csv or xlsx");
   }

   @Test
   void unavailableRequiredFieldsAndFiltersNeverBroadenResults() throws Exception
   {
      QContext.init(instance, viewer("alice", 1));
      for(var definition : List.of(
         SavedDefinition.parse("COLUMN_PROFILE", "orders", Map.of("fieldName", "secret")),
         SavedDefinition.parse("CORRELATION", "orders", Map.of("fields", List.of("amount", "secret"))),
         SavedDefinition.parse("TABLE_PROFILE", "orders", Map.of("fields", List.of("amount"), "filter", Map.of("criteria", List.of(Map.of("fieldName", "secret", "operator", "EQUALS", "values", List.of("x"))))))))
      {
         var result = definition.run(service, false);
         assertThat(result.warnings()).contains(SavedDefinition.FIELD_WARNING);
         assertThat(AnalysisExport.table(result).rows()).isEmpty();
         assertThatThrownBy(() -> definition.run(service, true)).hasMessageContaining(SavedDefinition.FIELD_WARNING);
      }
   }

   @Test
   void savedWidgetUsesViewerAndMemoryHasNoSql() throws Exception
   {
      var saved = TestSupport.insert(DataAnalysisTables.SAVED, definition("alice", true, "COLUMN_PROFILE", Map.of("fieldName", "amount")));
      QContext.init(instance, viewer("bob", 2));
      var empty = (ChartData) new SavedAnalysisWidget().render(new RenderWidgetInput().withWidgetMetaData(SavedAnalysisWidget.widget())).getWidgetData();
      assertThat(empty.getDescription()).contains("Choose a saved analysis");
      var metadata = SavedAnalysisWidget.widget().withDefaultValue("savedId", saved.getValue("id"));
      var data = (ChartData) new SavedAnalysisWidget().render(new RenderWidgetInput().withWidgetMetaData(metadata)).getWidgetData();
      assertThat(data.getChartData().getLabels()).isNotEmpty();
      assertThat(data.getDescription()).contains("4");
      assertThat(data.getFooterHTML()).contains("/" + saved.getValue("id"));
      var definition = SavedDefinition.parse("COLUMN_PROFILE", "orders", Map.of("fieldName", "amount"));
      assertThat(SqlHandoff.execute(definition, service).available()).isFalse();
      instance.addApp(new QAppMetaData().withName("sqlWorkbench"));
      QContext.getQSession().withPermission("sql_workbench.hasAccess");
      assertThat(SqlHandoff.execute(definition, service).note()).contains("non-RDBMS");
   }
}
