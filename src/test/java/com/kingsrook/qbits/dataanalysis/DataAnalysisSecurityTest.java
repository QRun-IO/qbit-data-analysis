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
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.permissions.DataAnalysisPermissions;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Permission enforcement and owner isolation through real core actions.
 *******************************************************************************/
class DataAnalysisSecurityTest
{
   /*******************************************************************************
    ** Release thread-local state.
    *******************************************************************************/
   @AfterEach
   void cleanup()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Effective table permission bases and the READ plus analyze conjunction.
    *******************************************************************************/
   @Test
   void analyzeRequiresBothPermissions() throws Exception
   {
      QInstance instance = TestSupport.instance();
      QTableMetaData table = new QTableMetaData().withName("orders")
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      instance.addTable(table);
      QContext.init(instance, TestSupport.user("alice"));
      assertThat(DataAnalysisPermissions.analyzePermissionName(table)).isEqualTo("orders.analyze");
      assertThat(DataAnalysisPermissions.canAnalyze(table)).isFalse();
      QContext.getQSession().withPermission("orders.analyze");
      assertThat(DataAnalysisPermissions.canAnalyze(table)).isFalse();
      QContext.getQSession().withPermission("orders.read");
      assertThat(DataAnalysisPermissions.canAnalyze(table)).isTrue();
      table.getPermissionRules().withPermissionBaseName("sales.orders");
      assertThat(DataAnalysisPermissions.analyzePermissionName(table)).isEqualTo("sales.orders.analyze");
      assertThat(DataAnalysisPermissions.canAnalyze(table)).isFalse();
      QContext.getQSession().withPermissions("sales.orders.read", "sales.orders.analyze");
      assertThat(DataAnalysisPermissions.canAnalyze(table)).isTrue();
      QContext.init(instance, TestSupport.user("alice").withPermission("sales.orders.read"));
      assertThat(DataAnalysisPermissions.canAnalyze(table)).isFalse();
      instance.withEnforcePermissionsInCoreActions(false);
      assertThat(DataAnalysisPermissions.canAnalyze(table)).isFalse();
      QContext.init(instance, TestSupport.user("alice").withPermission("sales.orders.analyze"));
      assertThat(DataAnalysisPermissions.canAnalyze(table)).isFalse();
      table.withPermissionRules(null);
      instance.withDefaultPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS).withPermissionBaseName("defaultBase"));
      assertThat(DataAnalysisPermissions.analyzePermissionName(table)).isEqualTo("defaultBase.analyze");
      table.withPermissionRules(new QPermissionRules().withPermissionBaseName(" "));
      assertThat(DataAnalysisPermissions.analyzePermissionName(table)).isEqualTo("orders.analyze");
   }



   /*******************************************************************************
    ** The core permission catalog honors configured inclusion and exclusion.
    *******************************************************************************/
   @Test
   void analyzeCatalog() throws Exception
   {
      QInstance instance = new QInstance().withEnforcePermissionsInCoreActions(true);
      instance.addBackend(new com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData().withName("memory").withBackendType(com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule.class));
      for(String name : List.of("orders", "excluded", "outside"))
      {
         instance.addTable(new QTableMetaData().withName(name).withLabel(name).withPermissionRules(
            new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS)));
      }
      new DataAnalysisQBitProducer().withQBitConfig(new DataAnalysisQBitConfig().withBackendName("memory")
         .withIncludeTables(java.util.Set.of("orders", "excluded")).withExcludeTables(java.util.Set.of("excluded"))).produce(instance);
      assertThat(com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper.getAllAvailablePermissions(instance)).anySatisfy(permission ->
      {
         assertThat(permission.getName()).isEqualTo("orders.analyze");
         assertThat(permission.getPermissionType()).isEqualTo("Analyze");
      }).noneMatch(permission -> permission.getName().equals("excluded.analyze") || permission.getName().equals("outside.analyze")
         || permission.getName().startsWith("data_analysis") && permission.getName().endsWith(".analyze"));
   }

   /*******************************************************************************
    ** Shared saved rows are readable; private stars, schedules and runs remain owner-locked.
    *******************************************************************************/
   @Test
   void ownerIsolation() throws Exception
   {
      QInstance instance = TestSupport.instance();
      QRecord mine = TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("alice", false));
      QRecord other = TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("bob", true));
      TestSupport.insert(DataAnalysisTables.SAVED_STAR, new QRecord().withValue("saved_id", mine.getValue("id")).withValue("user_id", "alice"));
      TestSupport.insert(DataAnalysisTables.SAVED_STAR, new QRecord().withValue("saved_id", other.getValue("id")).withValue("user_id", "bob"));
      for(String owner : List.of("alice", "bob"))
      {
         TestSupport.insert(DataAnalysisTables.RUN, new QRecord().withValue("table_name", "orders").withValue("operation", "profile").withValue("user_id", owner));
      }
      TestSupport.insert(DataAnalysisTables.SCHEDULE, new QRecord().withValue("saved_id", mine.getValue("id")));
      TestSupport.insert(DataAnalysisTables.SCHEDULE, new QRecord().withValue("saved_id", other.getValue("id")));
      QContext.init(instance, TestSupport.user("alice").withPermissions("data_analysis.saved.read", "data_analysis.run.read"));
      assertThat(query(DataAnalysisTables.SAVED)).extracting(r -> r.getValueString("owner_user_id")).containsExactly("alice", "bob");
      assertThat(query(DataAnalysisTables.SAVED_STAR)).extracting(r -> r.getValueString("user_id")).containsExactly("alice");
      assertThat(query(DataAnalysisTables.RUN)).extracting(r -> r.getValueString("user_id")).containsExactly("alice");
      assertThat(query(DataAnalysisTables.SCHEDULE)).extracting(r -> r.getValue("saved_id")).containsExactly(mine.getValue("id"));
      QContext.init(instance, TestSupport.user("alice").withPermissions("data_analysis.saved.read", "data_analysis.admin"));
      assertThat(query(DataAnalysisTables.SAVED)).hasSize(2);
      QContext.getQSession().withSecurityKeyValue("data_analysis.admin", true);
      assertThat(query(DataAnalysisTables.SAVED)).hasSize(2);
   }



   /*******************************************************************************
    ** Table READ does not grant writes; settings and run writes require admin.
    *******************************************************************************/
   @Test
   void writesAndReadPermissions() throws Exception
   {
      QInstance instance = TestSupport.instance();
      QRecord saved = TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("alice", false));
      QContext.init(instance, TestSupport.user("alice").withPermissions("data_analysis.saved.read", "data_analysis.run.read"));
      assertThat(PermissionsHelper.hasTablePermission(new QueryInput(DataAnalysisTables.RUN), DataAnalysisTables.RUN, TablePermissionSubType.READ)).isTrue();
      assertThat(PermissionsHelper.hasTablePermission(new QueryInput(DataAnalysisTables.TABLE_CONFIG), DataAnalysisTables.TABLE_CONFIG, TablePermissionSubType.READ)).isFalse();
      assertThatThrownBy(() -> new InsertAction().execute(new InsertInput(DataAnalysisTables.SAVED).withRecords(List.of(TestSupport.saved("alice", false)))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new UpdateAction().execute(new UpdateInput(DataAnalysisTables.SAVED).withRecords(List.of(new QRecord().withValue("id", saved.getValue("id")).withValue("name", "Changed")))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new DeleteAction().execute(new DeleteInput(DataAnalysisTables.SAVED).withPrimaryKeys(List.of(saved.getValue("id")))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new InsertAction().execute(new InsertInput(DataAnalysisTables.RUN).withRecords(List.of(new QRecord().withValue("user_id", "alice")))))
         .isInstanceOf(QPermissionDeniedException.class);
      QContext.getQSession().withPermission("data_analysis.saved.insert");
      TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("alice", false));
      QRecord denied = new InsertAction().execute(new InsertInput(DataAnalysisTables.SAVED).withRecords(List.of(TestSupport.saved("bob", false)))).getRecords().getFirst();
      assertThat(denied.getErrors()).isNotEmpty();
   }



   /*******************************************************************************
    ** Query with explicit enforcement, as the home and future API must do.
    *******************************************************************************/
   private static List<QRecord> query(String table) throws Exception
   {
      QueryInput input = new QueryInput(table);
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new QueryAction().execute(input).getRecords());
   }
}
