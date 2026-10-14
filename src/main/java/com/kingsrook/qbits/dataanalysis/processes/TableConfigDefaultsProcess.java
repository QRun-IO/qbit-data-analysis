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

package com.kingsrook.qbits.dataanalysis.processes;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitConfig;
import com.kingsrook.qbits.dataanalysis.DataAnalysisRuntime;
import com.kingsrook.qbits.dataanalysis.engine.EffectiveConfig;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.permissions.UseTablePermissionCustomPermissionChecker;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractExtractStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;


/*******************************************************************************
 ** "Create rows for analyzable tables": a table action on Analysis Settings
 ** that writes one row per analyzable table that has none yet, holding the
 ** settings that already apply to it (code defaults for that table, then the
 ** global defaults). It changes no behavior on its own: it makes the defaults
 ** visible and editable. Existing rows are never touched. Standard streamed
 ** ETL: Review lists what will be added, Result what was added.
 *******************************************************************************/
public final class TableConfigDefaultsProcess
{
   public static final String NAME = "data_analysis_table_config_create_rows";



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private TableConfigDefaultsProcess()
   {
   }



   /*******************************************************************************
    ** The process: a table-level action (no records need to be selected). Only
    ** people who may insert Analysis Settings see it; the steps enforce the table
    ** permissions again.
    *******************************************************************************/
   public static QProcessMetaData process()
   {
      Map<String, Serializable> values = new HashMap<>();
      values.put(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, DataAnalysisTables.TABLE_CONFIG);
      values.put(StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, DataAnalysisTables.TABLE_CONFIG);
      values.put(StreamedETLWithFrontendProcess.FIELD_SUPPORTS_FULL_VALIDATION, false);
      values.put(StreamedETLWithFrontendProcess.FIELD_PREVIEW_MESSAGE,
         "These rows will be added: one per analyzable table that has none, holding the settings that already apply. Existing rows are not changed.");
      QProcessMetaData process = StreamedETLWithFrontendProcess.defineProcessMetaData(Extract.class, Transform.class, Load.class, values);
      process.setName(NAME);
      process.setLabel("Create rows for analyzable tables");
      process.setIcon(new QIcon("playlist_add"));
      process.setTableName(DataAnalysisTables.TABLE_CONFIG);
      process.setMinInputRecords(0);
      process.setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED)
         .withCustomPermissionChecker(UseTablePermissionCustomPermissionChecker.build(DataAnalysisTables.TABLE_CONFIG, TablePermissionSubType.INSERT)));
      return (process);
   }



   /*******************************************************************************
    ** The tables analysis may be used on: everything but the qbit's own tables,
    ** limited by the includeTables and excludeTables configuration. The same test
    ** the "analyze" table permission uses.
    *******************************************************************************/
   public static List<String> analyzableTables(QInstance instance, DataAnalysisQBitConfig config)
   {
      return (instance.getTables().values().stream().map(QTableMetaData::getName)
         .filter(name -> !name.startsWith(DataAnalysisQBitConfig.TABLE_NAME_PREFIX))
         .filter(name -> config.getIncludeTables().isEmpty() || config.getIncludeTables().contains(name))
         .filter(name -> !config.getExcludeTables().contains(name)).sorted().toList());
   }



   /*******************************************************************************
    ** The rows that are missing: one per analyzable table without a row, with
    ** the effective settings (the same values EffectiveConfigResolver produces).
    *******************************************************************************/
   public static List<QRecord> missingRows(QInstance instance, DataAnalysisQBitConfig config) throws QException
   {
      Set<String> existing = existingTables();
      DataAnalysisRuntime.service().getConfigResolver().invalidate();
      List<QRecord> rows = new ArrayList<>();
      for(String table : analyzableTables(instance, config))
      {
         if(!existing.contains(table))
         {
            EffectiveConfig c = DataAnalysisRuntime.service().getConfigResolver().resolve(table);
            rows.add(new QRecord().withValue("table_name", table).withValue("enabled", c.getEnabled()).withValue("max_rows", c.getMaxRows())
               .withValue("sample_threshold_rows", c.getSampleThresholdRows()).withValue("sample_rows", c.getSampleRows())
               .withValue("min_group_size", c.getMinGroupSize()).withValue("cache_ttl_minutes", c.getCacheTtlMinutes())
               .withValue("analyzable_fields", c.getAnalyzableFields().isEmpty() ? null : String.join(",", new TreeSet<>(c.getAnalyzableFields())))
               .withValue("excluded_fields", c.getExcludedFields().isEmpty() ? null : String.join(",", new TreeSet<>(c.getExcludedFields()))));
         }
      }
      return (rows);
   }



   /*******************************************************************************
    ** Table names that already have a settings row (read as the signed-in user).
    *******************************************************************************/
   private static Set<String> existingTables() throws QException
   {
      QueryInput input = new QueryInput(DataAnalysisTables.TABLE_CONFIG).withFilter(new QQueryFilter());
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      Set<String> existing = new HashSet<>();
      new QueryAction().execute(input).getRecords().forEach(row -> existing.add(row.getValueString("table_name")));
      return (existing);
   }



   /*******************************************************************************
    ** Extract: the missing rows, computed (no query of the selected records).
    *******************************************************************************/
   public static class Extract extends AbstractExtractStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Integer doCount(RunBackendStepInput input) throws QException
      {
         return (missingRows(QContext.getQInstance(), DataAnalysisRuntime.service().getConfigResolver().getDefaults()).size());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         for(QRecord row : missingRows(QContext.getQInstance(), DataAnalysisRuntime.service().getConfigResolver().getDefaults()))
         {
            getRecordPipe().addRecord(row);
         }
      }
   }



   /*******************************************************************************
    ** Transform: lists what will be added for the review screen.
    *******************************************************************************/
   public static class Transform extends AbstractTransformStep implements ProcessSummaryProviderInterface
   {
      private final ProcessSummaryLine added = line(Status.OK, "table will get a settings row", "tables will get a settings row",
         "table got a settings row", "tables got a settings row");



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         for(QRecord record : input.getRecords())
         {
            added.incrementCount();
            output.getRecords().add(record);
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         added.addSelfToListIfAnyCount(lines);
         if(lines.isEmpty())
         {
            lines.add(new ProcessSummaryLine(Status.INFO, null, "Every analyzable table already has a settings row."));
         }
         return (lines);
      }
   }



   /*******************************************************************************
    ** Load: insert through the standard action with the user's own permissions.
    *******************************************************************************/
   public static class Load extends AbstractLoadStep implements ProcessSummaryProviderInterface
   {
      private final ProcessSummaryLine created = line(Status.OK, "row was added", "rows were added", "row was added", "rows were added");



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         InsertInput insert = new InsertInput(DataAnalysisTables.TABLE_CONFIG).withRecords(input.getRecords());
         insert.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         for(QRecord record : new InsertAction().execute(insert).getRecords())
         {
            if(record.getErrors() != null && !record.getErrors().isEmpty())
            {
               throw (new QException("Could not add analysis settings: " + record.getErrors()));
            }
            created.incrementCount();
            output.getRecords().add(record);
         }
         DataAnalysisRuntime.service().getConfigResolver().invalidate();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         created.addSelfToListIfAnyCount(lines);
         return (lines);
      }
   }



   /*******************************************************************************
    ** A summary line with its four messages.
    *******************************************************************************/
   private static ProcessSummaryLine line(Status status, String singularFuture, String pluralFuture, String singularPast, String pluralPast)
   {
      ProcessSummaryLine line = new ProcessSummaryLine(status, 0, null);
      line.setSingularFutureMessage(singularFuture);
      line.setPluralFutureMessage(pluralFuture);
      line.setSingularPastMessage(singularPast);
      line.setPluralPastMessage(pluralPast);
      return (line);
   }

}
