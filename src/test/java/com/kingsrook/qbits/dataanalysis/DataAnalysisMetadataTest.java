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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qbits.dataanalysis.app.DataAnalysisHome;
import com.kingsrook.qbits.dataanalysis.metadata.DataAnalysisMetaDataProducer;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfigValidationException;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Metadata, configuration and generated schema contracts.
 *******************************************************************************/
class DataAnalysisMetadataTest
{
   /*******************************************************************************
    ** Release thread-local context between tests.
    *******************************************************************************/
   @AfterEach
   void cleanup()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Full production, validation, navigation, locks, enums and permission catalog.
    *******************************************************************************/
   @Test
   void producerAndMetadata() throws Exception
   {
      QInstance instance = TestSupport.instance();
      new QInstanceValidator().validate(instance);
      assertThat(instance.getTables()).containsOnlyKeys(DataAnalysisTables.ALL);
      assertThat(instance.getPossibleValueSources().keySet().stream().filter(name -> name.startsWith("data_analysis_"))).hasSize(16);
      assertThat(instance.getJoins()).hasSize(3);
      assertThat(instance.getWidgets()).hasSize(9).containsKey(DataAnalysisHome.HOME_WIDGET);
      assertThat(instance.getApp("dataAnalysis").getSections().getFirst().getProcesses()).isNullOrEmpty();
      assertThat(instance.getApp("dataAnalysis").getLabel()).isEqualTo("Analyses");
      assertThat(instance.getApp("dataAnalysis").getSections().getFirst().getTables()).containsExactlyElementsOf(DataAnalysisTables.NAV_TABLES);
      assertThat(instance.getTable(DataAnalysisTables.SAVED).getAuditRules().getAuditLevel()).isEqualTo(AuditLevel.FIELD);
      assertThat(instance.getTable(DataAnalysisTables.TABLE_CONFIG).getAuditRules().getAuditLevel()).isEqualTo(AuditLevel.FIELD);
      assertThat(instance.getTable(DataAnalysisTables.TABLE_CONFIG).getUniqueKeys().getFirst().getFieldNames()).containsExactly("table_name");
      assertThat(instance.getTable(DataAnalysisTables.SAVED_STAR).getUniqueKeys().getFirst().getFieldNames()).containsExactly("saved_id", "user_id");
      for(String name : List.of(DataAnalysisTables.SAVED, DataAnalysisTables.SAVED_STAR, DataAnalysisTables.SCHEDULE))
      {
         assertThat(instance.getTable(name).getPermissionRules().getPermissionBaseName()).isEqualTo("data_analysis.saved");
      }
      assertThat(instance.getTable(DataAnalysisTables.WATCH).getPermissionRules().getPermissionBaseName()).isEqualTo("data_analysis.watch");
      assertThat(instance.getTable(DataAnalysisTables.RUN).getPermissionRules().getPermissionBaseName()).isEqualTo("data_analysis.run");
      assertThat(instance.getTable(DataAnalysisTables.TABLE_CONFIG).getPermissionRules().getPermissionBaseName()).isEqualTo("data_analysis.settings");
      for(String name : List.of(DataAnalysisTables.SAVED, DataAnalysisTables.SAVED_STAR, DataAnalysisTables.RUN))
      {
         RecordSecurityLock lock = instance.getTable(name).getRecordSecurityLocks().getFirst();
         assertThat(lock.getSecurityKeyType()).isEqualTo("data_analysis_user");
         assertThat(lock.getFieldName()).isEqualTo(name.equals(DataAnalysisTables.SAVED) ? "owner_user_id" : "user_id");
         assertThat(lock.getNullValueBehavior()).isEqualTo(RecordSecurityLock.NullValueBehavior.DENY);
         assertThat(lock.getLockScope()).isEqualTo(name.equals(DataAnalysisTables.SAVED) ? RecordSecurityLock.LockScope.WRITE : RecordSecurityLock.LockScope.READ_AND_WRITE);
      }
      assertThat(instance.getSecurityKeyType("data_analysis_user").getAllAccessKeyName()).isEqualTo("data_analysis.admin");
      assertThat(PermissionsHelper.getAllAvailablePermissionNames(instance)).contains("data_analysis.settings.read", "data_analysis.settings.edit", "data_analysis.run.read", "data_analysis.run.delete", "data_analysis.saved.read", "data_analysis.saved.insert",
         "data_analysis.saved.edit", "data_analysis.saved.delete", "data_analysis.watch.read", "data_analysis.watch.insert", "data_analysis.watch.edit", "data_analysis.watch.delete")
         .doesNotContain("data_analysis.admin.read");
   }



   /*******************************************************************************
    ** Custom app names, parent linkage and qbit identity.
    *******************************************************************************/
   @Test
   void producerConfiguration() throws Exception
   {
      QInstance instance = new QInstance();
      instance.addBackend(new com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData().withName("memory")
         .withBackendType(com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule.class));
      instance.addApp(new QAppMetaData().withName("admin").withLabel("Admin"));
      DataAnalysisQBitConfig config = new DataAnalysisQBitConfig().withBackendName("memory").withAppName("customAnalysis").withAppLabel("Insights").withParentAppName("admin");
      DataAnalysisQBitProducer producer = new DataAnalysisQBitProducer().withQBitConfig(config).withNamespace("analysis");
      assertThat(producer.isEnabled()).isTrue();
      assertThat(producer.getNamespace()).isEqualTo("analysis");
      assertThat(producer.getQBitConfig()).isSameAs(config);
      assertThat(producer.getQBitMetaData().getVersion()).isEqualTo("0.1.0");
      assertThat(producer.getQBitMetaData().getGroupId()).isEqualTo("com.kingsrook.qbits");
      assertThat(producer.getQBitMetaData().getArtifactId()).isEqualTo("qbit-data-analysis");
      assertThat(producer.getPackageNameForFindingMetaDataProducers()).endsWith(".metadata");
      producer.produce(instance);
      assertThat(instance.getApp("customAnalysis").getParentAppName()).isEqualTo("admin");
      assertThat(instance.getApp("admin").getChildren()).contains(instance.getApp("customAnalysis"));
      assertThat(new DataAnalysisQBitProducer().isEnabled()).isFalse();
      assertThat(new DataAnalysisMetaDataProducer().isEnabled()).isFalse();
      assertThatThrownBy(() -> new DataAnalysisQBitProducer().withQBitConfig(new DataAnalysisQBitConfig()).produce(instance))
         .isInstanceOf(QBitConfigValidationException.class);
   }



   /*******************************************************************************
    ** Defaults and meaningful code-level overrides remain available to later phases.
    *******************************************************************************/
   @Test
   void configDefaultsAndOverrides() throws Exception
   {
      QInstance instance = TestSupport.instance();
      DataAnalysisQBitConfig config = new DataAnalysisQBitConfig().withBackendName("memory");
      assertThat(config.getDefaultMaxRows()).isEqualTo(1_000_000);
      assertThat(config.getDefaultSampleThresholdRows()).isEqualTo(1_000_000);
      assertThat(config.getDefaultSampleRows()).isEqualTo(100_000);
      assertThat(config.getDefaultMinGroupSize()).isZero();
      assertThat(config.getDefaultCacheTtlMinutes()).isEqualTo(60);
      assertThat(config.getIncludeTables()).isEmpty();
      assertThat(config.getExcludeTables()).isEmpty();
      assertThat(config.getTableDefaults()).isEmpty();
      assertThat(config.getDefaultBackendNameForTables()).isEqualTo("memory");
      TableAnalysisDefaults defaults = new TableAnalysisDefaults().withEnabled(true).withMaxRows(200).withSampleThresholdRows(100)
         .withSampleRows(50).withMinGroupSize(5).withCacheTtlMinutes(10).withAnalyzableFields(Set.of("amount")).withExcludedFields(Set.of("secret"));
      config.withSchedulerName("scheduler").withIncludeTables(Set.of("orders")).withExcludeTables(Set.of("users")).withTableDefaults(Map.of("orders", defaults));
      config.validate(instance);
      assertThat(config.getSchedulerName()).isEqualTo("scheduler");
      assertThat(config.getTableDefaults().get("orders")).isSameAs(defaults);
      assertThat(config.getIncludeTables()).containsExactly("orders");
      assertThat(config.getExcludeTables()).containsExactly("users");
      assertThat(defaults.getEnabled()).isTrue();
      assertThat(defaults.getMaxRows()).isEqualTo(200);
      assertThat(defaults.getSampleThresholdRows()).isEqualTo(100);
      assertThat(defaults.getSampleRows()).isEqualTo(50);
      assertThat(defaults.getMinGroupSize()).isEqualTo(5);
      assertThat(defaults.getCacheTtlMinutes()).isEqualTo(10);
      assertThat(defaults.getAnalyzableFields()).containsExactly("amount");
      assertThat(defaults.getExcludedFields()).containsExactly("secret");
   }



   /*******************************************************************************
    ** Report every invalid number and absent backend instead of failing at runtime.
    *******************************************************************************/
   @Test
   void invalidConfig() throws Exception
   {
      QInstance instance = TestSupport.instance();
      DataAnalysisQBitConfig config = new DataAnalysisQBitConfig().withBackendName("missing").withDefaultMaxRows(-1).withDefaultSampleThresholdRows(-1)
         .withDefaultSampleRows(-1).withDefaultMinGroupSize(-1).withDefaultCacheTtlMinutes(-1);
      List<String> errors = new ArrayList<>();
      config.validate(instance, errors);
      assertThat(errors).hasSize(6);
      TableAnalysisDefaults defaults = new TableAnalysisDefaults().withMaxRows(-1).withSampleThresholdRows(-1).withSampleRows(-1).withMinGroupSize(-1).withCacheTtlMinutes(-1);
      config.withTableDefaults(Map.of("orders", defaults)).withBackendName("memory").withDefaultMaxRows(0).withDefaultSampleThresholdRows(0)
         .withDefaultSampleRows(0).withDefaultMinGroupSize(0).withDefaultCacheTtlMinutes(0);
      errors.clear();
      config.validate(instance, errors);
      assertThat(errors).hasSize(5).allMatch(error -> error.startsWith("tableDefaults[orders]"));
      config.withTableDefaults(Map.of("orders", new TableAnalysisDefaults()));
      config.validate(instance);
   }



   /*******************************************************************************
    ** DDL includes every field, inferred backend column, SQL type and unique key.
    *******************************************************************************/
   @Test
   void schema()
   {
      String ddl = DataAnalysisSchema.postgresScript(new DataAnalysisQBitConfig().withBackendName("database"));
      for(String name : DataAnalysisTables.ALL)
      {
         assertThat(ddl).contains("CREATE TABLE IF NOT EXISTS " + name + " (");
      }
      assertThat(ddl).contains("id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY", "create_date TIMESTAMP WITHOUT TIME ZONE",
         "modify_date TIMESTAMP WITHOUT TIME ZONE", "rows_scanned BIGINT", "result_json TEXT", "shared BOOLEAN", "day INTEGER",
         "owner_user_id VARCHAR(250) NOT NULL", "date_field VARCHAR(250) NOT NULL", "last_value NUMERIC", "last_message TEXT", "CREATE UNIQUE INDEX IF NOT EXISTS uq_data_analysis_table_config_1 ON data_analysis_table_config (table_name)",
         "CREATE UNIQUE INDEX IF NOT EXISTS uq_data_analysis_saved_star_1 ON data_analysis_saved_star (saved_id, user_id)",
         "CHECK (shared = TRUE OR read_owner_user_id IS NOT NULL)");
      assertThat(ddl).endsWith(";\n");
   }
}
