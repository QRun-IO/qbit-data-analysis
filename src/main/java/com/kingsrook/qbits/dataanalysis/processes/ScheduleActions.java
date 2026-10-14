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
 ** Standard streamed ETL Review and Result actions for scheduled analyses.
 *******************************************************************************/
public final class ScheduleActions
{
   public static final String SEND = "data_analysis_schedule_send";
   public static final String SCHEDULED = "data_analysis_run_schedules";

   /*******************************************************************************
    ** Steps only.
    *******************************************************************************/
   private ScheduleActions()
   {
   }

   /*******************************************************************************
    ** Review has no delivery side effects.
    *******************************************************************************/
   public static class Review extends AbstractTransformStep implements ProcessSummaryProviderInterface
   {
      private final ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();

      /*******************************************************************************
       ** Core extraction enforces record locks; verify the edit grant as well.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         if(!PermissionsHelper.hasTablePermission(new UpdateInput(DataAnalysisTables.SCHEDULE), DataAnalysisTables.SCHEDULE, TablePermissionSubType.EDIT))
         {
            throw new QPermissionDeniedException("Schedule editing is unavailable.");
         }
         for(QRecord record : input.getRecords())
         {
            lines.add(new ProcessSummaryLine(Status.INFO, null, "Schedule " + record.getValueString("id") + " will be sent to authorized recipients."));
            output.addRecord(record);
         }
      }

      /*******************************************************************************
       ** Standard Review summary.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean result)
      {
         return (result ? new ArrayList<>() : lines);
      }
   }

   /*******************************************************************************
    ** Every selected record contributes its actual delivery status to Result.
    *******************************************************************************/
   public static class Result extends AbstractLoadStep implements ProcessSummaryProviderInterface
   {
      private final ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();

      /*******************************************************************************
       ** Reauthorize when executing after Review.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         for(QRecord record : input.getRecords())
         {
            try
            {
               String status = DataAnalysisRuntime.schedules().sendNow(record.getValue("id"), Instant.now());
               lines.add(new ProcessSummaryLine(status.startsWith("Failed") ? Status.ERROR : Status.OK, null, status));
               output.addRecord(record);
            }
            catch(QException e)
            {
               lines.add(new ProcessSummaryLine(Status.ERROR, null, e.getMessage()));
            }
         }
      }

      /*******************************************************************************
       ** Actual results across streamed pages.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean result)
      {
         return (lines);
      }
   }

   /*******************************************************************************
    ** Hidden scheduler every five minutes.
    *******************************************************************************/
   public static class Scheduled implements BackendStep
   {
      /*******************************************************************************
       ** Count actual due schedule attempts.
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         output.addValue("count", DataAnalysisRuntime.schedules().runDue(Instant.now()));
      }
   }
}
