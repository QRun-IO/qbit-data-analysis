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

package com.kingsrook.qbits.dataanalysis.schedule;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.saved.SavedDefinition;
import com.kingsrook.qbits.dataanalysis.watch.WatchRecords;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;

/*******************************************************************************
 ** Core CRUD validates recipients and computes the next occurrence on configuration edits.
 *******************************************************************************/
public class ScheduleCustomizer implements TableCustomizerInterface
{
   private static final Set<String> CONFIG = Set.of("saved_id", "cron", "frequency", "day", "time", "time_zone", "enabled", "recipients", "format");

   /*******************************************************************************
    ** Disabled empty drafts may be completed later.
    *******************************************************************************/
   @Override
   public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean preview) throws QException
   {
      for(QRecord row : records)
      {
         validate(row, row);
      }
      return (records);
   }

   /*******************************************************************************
    ** Status-only scheduler writes do not move the next occurrence twice.
    *******************************************************************************/
   @Override
   public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean preview, Optional<List<QRecord>> oldRecords) throws QException
   {
      for(QRecord patch : records)
      {
         if(patch.getValues().keySet().stream().anyMatch(CONFIG::contains))
         {
            QRecord row = new QRecord(WatchRecords.get(DataAnalysisTables.SCHEDULE, patch.getValue("id")));
            row.getValues().putAll(patch.getValues());
            validate(patch, row);
         }
      }
      return (records);
   }

   /*******************************************************************************
    ** User ids are stored as a JSON array, not an unchecked email recipient list.
    *******************************************************************************/
   public static List<String> recipients(QRecord row) throws QException
   {
      try
      {
         Object value = JsonUtils.toObject(row.getValueString("recipients"), Object.class);
         if(value instanceof List<?> list && !list.isEmpty() && list.size() <= 100
            && list.stream().allMatch(v -> v instanceof String s && !s.isBlank()))
         {
            return (list.stream().map(String.class::cast).distinct().toList());
         }
      }
      catch(Exception expected)
      {
         // Use the same plain error for JSON and shape errors.
      }
      throw new QException("Recipients must be a JSON array of 1 to 100 user ids.");
   }

   /*******************************************************************************
    ** Scheduling references a real, visible saved analysis.
    *******************************************************************************/
   private void validate(QRecord output, QRecord row)
   {
      try
      {
         SavedDefinition.parse(WatchRecords.get(DataAnalysisTables.SAVED, row.getValue("saved_id")));
         if(Boolean.TRUE.equals(row.getValueBoolean("enabled")) || row.getValueString("frequency") != null || row.getValueString("cron") != null)
         {
            recipients(row);
            output.withValue("next_run_at", ScheduleTiming.next(row, Instant.now()));
         }
      }
      catch(QException e)
      {
         output.addError(new BadInputStatusMessage(e.getMessage()));
      }
   }
}
