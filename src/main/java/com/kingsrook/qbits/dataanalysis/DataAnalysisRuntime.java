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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;

/*******************************************************************************
 ** Instance-scoped service holder for UI API endpoints and host integrations.
 *******************************************************************************/
public class DataAnalysisRuntime implements QSupplementalInstanceMetaData
{
   private final transient DataAnalysisService service;
   private final transient com.kingsrook.qbits.dataanalysis.schedule.ScheduleRunner schedules;
   private final transient com.kingsrook.qbits.dataanalysis.saved.SavedAnalyses saved;
   private final transient com.kingsrook.qbits.dataanalysis.watch.WatchEvaluator watches;

   /*******************************************************************************
    ** Create a runtime for one configured instance.
    *******************************************************************************/
   public DataAnalysisRuntime(DataAnalysisQBitConfig config)
   {
      service = new DataAnalysisService(config);
      schedules = new com.kingsrook.qbits.dataanalysis.schedule.ScheduleRunner(config, service);
      saved = new com.kingsrook.qbits.dataanalysis.saved.SavedAnalyses(service);
      watches = new com.kingsrook.qbits.dataanalysis.watch.WatchEvaluator(config, service);
   }



   /*******************************************************************************
    ** Stable supplemental metadata name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("dataAnalysisRuntime");
   }



   /*******************************************************************************
    ** Runtime objects never appear in metadata JSON.
    *******************************************************************************/
   @JsonIgnore
   public DataAnalysisService getService()
   {
      return (service);
   }



   /*******************************************************************************
    ** Locate the service belonging to the current QInstance.
    *******************************************************************************/
   public static DataAnalysisService service()
   {
      return (((DataAnalysisRuntime) QContext.getQInstance().getSupplementalMetaData("dataAnalysisRuntime")).getService());
   }

   /*******************************************************************************
    ** Shared evaluator serializes manual and scheduled watch transitions.
    *******************************************************************************/
   public static com.kingsrook.qbits.dataanalysis.watch.WatchEvaluator watches()
   {
      return (((DataAnalysisRuntime) QContext.getQInstance().getSupplementalMetaData("dataAnalysisRuntime")).watches);
   }

   /*******************************************************************************
    ** Saved operations share the instance's engine and serialize star toggles.
    *******************************************************************************/
   public static com.kingsrook.qbits.dataanalysis.saved.SavedAnalyses saved()
   {
      return (((DataAnalysisRuntime) QContext.getQInstance().getSupplementalMetaData("dataAnalysisRuntime")).saved);
   }

   /*******************************************************************************
    ** Shared schedule runner for manual and automatic delivery.
    *******************************************************************************/
   public static com.kingsrook.qbits.dataanalysis.schedule.ScheduleRunner schedules()
   {
      return (((DataAnalysisRuntime) QContext.getQInstance().getSupplementalMetaData("dataAnalysisRuntime")).schedules);
   }
}
