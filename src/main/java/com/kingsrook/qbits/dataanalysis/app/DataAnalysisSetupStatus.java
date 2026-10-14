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

package com.kingsrook.qbits.dataanalysis.app;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitConfig;
import com.kingsrook.qbits.dataanalysis.DataAnalysisRuntime;
import com.kingsrook.qbits.dataanalysis.engine.EffectiveConfig;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.processes.TableConfigDefaultsProcess;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;


/*******************************************************************************
 ** The Setup card on the Analyses home: each configuration or content table,
 ** whether it is set up, and the next step. Reads settings through the same
 ** resolver the analysis runtime uses (code defaults, then rows), so the card
 ** never reports something the runtime would not do. Counts run as the viewer.
 *******************************************************************************/
public final class DataAnalysisSetupStatus
{
   /*******************************************************************************
    ** OK: set up. DEFAULT: working on built-in defaults (nothing required).
    ** OPTIONAL: a feature that stays off until someone uses it. UNAVAILABLE: the
    ** viewer cannot read the table.
    *******************************************************************************/
   public enum State
   {
      OK, DEFAULT, OPTIONAL, UNAVAILABLE
   }



   /*******************************************************************************
    ** One line of the card.
    *******************************************************************************/
   public record Item(String label, String tableName, State state, String detail, String nextStep)
   {
   }



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DataAnalysisSetupStatus()
   {
   }



   /*******************************************************************************
    ** The card's lines, in the order a person would set things up.
    *******************************************************************************/
   public static List<Item> load() throws QException
   {
      DataAnalysisQBitConfig config = DataAnalysisRuntime.service().getConfigResolver().getDefaults();
      List<Item>             items  = new ArrayList<>();

      ///////////////////////
      // analysis settings //
      ///////////////////////
      List<String> analyzable = TableConfigDefaultsProcess.analyzableTables(QContext.getQInstance(), config);
      if(!DataAnalysisHome.canRead(DataAnalysisTables.TABLE_CONFIG))
      {
         items.add(new Item("Analysis settings", DataAnalysisTables.TABLE_CONFIG, State.UNAVAILABLE, "Unavailable: READ permission required.", null));
      }
      else
      {
         int rows = DataAnalysisHome.count(DataAnalysisTables.TABLE_CONFIG, new QQueryFilter());
         String defaults = "max rows " + config.getDefaultMaxRows() + ", sample " + config.getDefaultSampleRows() + " above " + config.getDefaultSampleThresholdRows()
            + ", smallest group " + config.getDefaultMinGroupSize() + ", results reused " + config.getDefaultCacheTtlMinutes() + " min";
         if(rows == 0)
         {
            items.add(new Item("Analysis settings", DataAnalysisTables.TABLE_CONFIG, State.DEFAULT,
               "All " + analyzable.size() + " analyzable tables are on, with the defaults (" + defaults + ").",
               "Run Create rows for analyzable tables to see and edit them, or add a row to change one table."));
         }
         else
         {
            items.add(new Item("Analysis settings", DataAnalysisTables.TABLE_CONFIG, rows >= analyzable.size() ? State.OK : State.DEFAULT,
               rows + " settings row" + (rows == 1 ? "" : "s") + " for " + analyzable.size() + " analyzable tables; tables without a row use the defaults (" + defaults + ").",
               rows >= analyzable.size() ? null : "Run Create rows for analyzable tables to add rows for the rest."));
         }
      }

      /////////////////////////////////////////////////////////////////////
      // result cache: the resolved value, exactly what caching will use //
      /////////////////////////////////////////////////////////////////////
      long cached = analyzable.stream().filter(table -> ttl(table) == 0).count(); // tables with caching off
      items.add(!analyzable.isEmpty() && cached == analyzable.size()
         ? new Item("Result cache", DataAnalysisTables.RUN, State.DEFAULT, "Off: every exact run computes fresh.", "Set cache minutes on Analysis Settings to reuse results.")
         : new Item("Result cache", DataAnalysisTables.RUN, State.OK, "Exact results are reused for " + config.getDefaultCacheTtlMinutes()
            + " minutes unless a table's settings row says otherwise" + (cached > 0 && config.getDefaultCacheTtlMinutes() > 0 ? " (" + cached + " tables have caching off)" : "") + ".", null));

      ///////////////////////////////////////////////////
      // saved analyses, watches, schedules: optional  //
      ///////////////////////////////////////////////////
      items.add(optional("Saved analyses", DataAnalysisTables.SAVED, "No saved analyses yet.", "Save an analysis from the Analyze screen, then star or share it."));
      String scheduler = config.getSchedulerName() == null ? " No scheduler is configured, so watches only run with Evaluate now." : "";
      items.add(optional("Watches", DataAnalysisTables.WATCH, "No watches yet." + scheduler, "Add a watch on a saved analysis to be alerted to spikes, drops and new categories."));
      String schedules = config.getSchedulerName() == null ? " No scheduler is configured, so schedules do not run." : "";
      items.add(optional("Schedules", DataAnalysisTables.SCHEDULE, "No schedules yet." + schedules, "Add a schedule on a saved analysis to send it by email."));
      return (items);
   }



   /*******************************************************************************
    ** One optional feature: set up once it has rows.
    *******************************************************************************/
   private static Item optional(String label, String table, String emptyDetail, String nextStep) throws QException
   {
      if(!DataAnalysisHome.canRead(table))
      {
         return (new Item(label, table, State.UNAVAILABLE, "Unavailable: READ permission required.", null));
      }
      int rows = DataAnalysisHome.count(table, new QQueryFilter());
      return (rows == 0 ? new Item(label, table, State.OPTIONAL, emptyDetail, nextStep) : new Item(label, table, State.OK, rows + " in use.", null));
   }



   /*******************************************************************************
    ** The effective cache minutes for a table, as the run cache resolves them.
    *******************************************************************************/
   private static int ttl(String table)
   {
      try
      {
         EffectiveConfig config = DataAnalysisRuntime.service().getConfigResolver().resolve(table);
         return (config.getCacheTtlMinutes());
      }
      catch(QException e)
      {
         return (-1);
      }
   }

}
