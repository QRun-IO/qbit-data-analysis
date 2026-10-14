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

package com.kingsrook.qbits.dataanalysis.metadata;

import java.util.List;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitConfig;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.processes.ScheduleActions;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitComponentMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;

/*******************************************************************************
 ** Watch record actions and one hidden scheduled process, never app children.
 *******************************************************************************/
public class ScheduleProcesses extends QBitComponentMetaDataProducer<MetaDataProducerMultiOutput, DataAnalysisQBitConfig>
{
   /*******************************************************************************
    ** Build after the watch table has been registered.
    *******************************************************************************/
   @Override
   public int getSortOrder()
   {
      return (200);
   }

   /*******************************************************************************
    ** Component scanning also encounters unconfigured producers.
    *******************************************************************************/
   @Override
   public boolean isEnabled()
   {
      return (getQBitConfig() != null);
   }

   /*******************************************************************************
    ** Ordinary core actions provide selected records, Review, and Result screens.
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance instance)
   {
      MetaDataProducerMultiOutput output = new MetaDataProducerMultiOutput();
      for(String name : List.of(ScheduleActions.SEND))
      {
         String label = "Send now";
         QProcessMetaData process = StreamedETLWithFrontendProcess.processMetaDataBuilder()
            .withName(name).withLabel(label).withIcon(new QIcon("notifications_active"))
            .withTableName(DataAnalysisTables.SCHEDULE).withSourceTable(DataAnalysisTables.SCHEDULE).withDestinationTable(DataAnalysisTables.SCHEDULE)
            .withExtractStepClass(ExtractViaQueryStep.class).withTransformStepClass(ScheduleActions.Review.class).withLoadStepClass(ScheduleActions.Result.class)
            .withSupportsFullValidation(true).withDoFullValidation(true).withMinInputRecords(1)
            .withPreviewMessage("Review the selected schedules.").getProcessMetaData();
         // Table READ/EDIT and owner locks are enforced by extraction and both steps.
         process.withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED));
         output.add(process);
      }
      QProcessMetaData scheduled = new QProcessMetaData().withName(ScheduleActions.SCHEDULED).withLabel("Run analysis schedules").withIsHidden(true)
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION).withPermissionBaseName(ScheduleActions.SCHEDULED))
         .withStep(new QBackendStepMetaData().withName("run").withCode(new QCodeReference(ScheduleActions.Scheduled.class)));
      if(getQBitConfig().getSchedulerName() != null)
      {
         scheduled.withSchedule(new QScheduleMetaData().withSchedulerName(getQBitConfig().getSchedulerName()).withRepeatSeconds(300).withInitialDelaySeconds(300));
      }
      output.add(scheduled);
      return (output);
   }
}
