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

import java.time.Instant;
import java.util.ArrayList;
import com.kingsrook.qbits.dataanalysis.DataAnalysisRuntime;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;

/*******************************************************************************
 ** Ordinary streamed ETL Review/Result steps; no custom forms or navigation.
 *******************************************************************************/
public final class WatchActions
{
   public static final String EVALUATE = "data_analysis_watch_evaluate";
   public static final String ENABLE = "data_analysis_watch_enable";
   public static final String DISABLE = "data_analysis_watch_disable";
   public static final String SCHEDULED = "data_analysis_evaluate_watches";

   /*******************************************************************************
    ** Steps only.
    *******************************************************************************/
   private WatchActions()
   {
   }

   /*******************************************************************************
    ** Preview selected records without evaluating or mutating them.
    *******************************************************************************/
   public static class Review extends AbstractTransformStep implements ProcessSummaryProviderInterface
   {
      private final ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();

      /*******************************************************************************
       ** Extract runs as caller; enforce EDIT before promising any action.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         if(!PermissionsHelper.hasTablePermission(new UpdateInput(DataAnalysisTables.WATCH), DataAnalysisTables.WATCH, TablePermissionSubType.EDIT))
         {
            throw new QPermissionDeniedException("Watch editing is unavailable.");
         }
         String verb = verb(input.getProcessName());
         for(QRecord record : input.getRecords())
         {
            lines.add(new ProcessSummaryLine(Status.INFO, null, "Watch " + record.getValueString("id") + " (" + record.getValueString("detector") + ") will be " + verb + "."));
            output.addRecord(record);
         }
      }

      /*******************************************************************************
       ** Standard summary lines are rendered by the core Review screen.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean result)
      {
         return (result ? new ArrayList<>() : lines);
      }
   }

   /*******************************************************************************
    ** Apply selected records and report exact evaluated/firing/OK/skipped counts.
    *******************************************************************************/
   public static class Result extends AbstractLoadStep implements ProcessSummaryProviderInterface
   {
      private int done;
      private int firing;
      private int ok;
      private String process;
      private final ArrayList<ProcessSummaryLineInterface> failures = new ArrayList<>();

      /*******************************************************************************
       ** Re-read and authorize each id when executing after Review.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         process = input.getProcessName();
         verb(process);
         for(QRecord record : input.getRecords())
         {
            try
            {
               if(EVALUATE.equals(process))
               {
                  var verdict = DataAnalysisRuntime.watches().evaluate(record.getValue("id"), Instant.now());
                  if(!verdict.evaluated())
                  {
                     failures.add(new ProcessSummaryLine(Status.WARNING, null, "Watch " + record.getValueString("id") + " skipped: " + verdict.message()));
                     continue;
                  }
                  firing += verdict.firing() ? 1 : 0;
                  ok += verdict.firing() ? 0 : 1;
               }
               else
               {
                  DataAnalysisRuntime.watches().setEnabled(record.getValue("id"), ENABLE.equals(process));
               }
               done++;
               output.addRecord(record);
            }
            catch(QException e)
            {
               failures.add(new ProcessSummaryLine(Status.ERROR, null, "Watch " + record.getValueString("id") + " failed: " + e.getMessage()));
            }
         }
      }

      /*******************************************************************************
       ** Counts span all streamed pages, so the Result screen is a true total.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean result)
      {
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         String message = done + (done == 1 ? " watch " : " watches ") + (EVALUATE.equals(process) ? "evaluated: " + firing + " firing, " + ok + " OK"
            : ENABLE.equals(process) ? "enabled" : "disabled");
         lines.add(new ProcessSummaryLine(Status.OK, null, message));
         lines.addAll(failures);
         return (lines);
      }
   }

   /*******************************************************************************
    ** Scheduler invokes a hidden backend-only process every fifteen minutes.
    *******************************************************************************/
   public static class Scheduled implements BackendStep
   {
      /*******************************************************************************
       ** Store the real count of successfully evaluated enabled watches.
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         output.addValue("count", DataAnalysisRuntime.watches().evaluateEnabled(Instant.now()));
      }
   }

   /*******************************************************************************
    ** Reject unknown processes instead of silently choosing a mutation.
    *******************************************************************************/
   private static String verb(String process) throws QException
   {
      return (switch(process)
      {
         case EVALUATE -> "evaluated";
         case ENABLE -> "enabled";
         case DISABLE -> "disabled";
         default -> throw new QException("Unknown watch action.");
      });
   }
}
