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

package com.kingsrook.qbits.dataanalysis.watch;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.permissions.DataAnalysisPermissions;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;

/*******************************************************************************
 ** Validate standard table CRUD, including partial updates and owner changes.
 *******************************************************************************/
public class WatchCustomizer implements TableCustomizerInterface
{
   private static final Set<String> CONFIGURATION = Set.of("saved_id", "table_name", "filter_json", "metric_json", "date_field", "bucket",
      "detector", "parameters_json", "severity", "owner_user_id", "enabled");

   /*******************************************************************************
    ** Validate every inserted watch, including disabled drafts.
    *******************************************************************************/
   @Override
   public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean preview) throws QException
   {
      for(QRecord record : records)
      {
         validate(record, record);
      }
      return (records);
   }

   /*******************************************************************************
    ** Merge partial patches with the stored row before checking detector parameters.
    *******************************************************************************/
   @Override
   public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean preview, Optional<List<QRecord>> oldRecords) throws QException
   {
      for(QRecord record : records)
      {
         if(record.getValues().keySet().stream().noneMatch(CONFIGURATION::contains))
         {
            continue;
         }
         QRecord old = WatchRecords.get(DataAnalysisTables.WATCH, record.getValue("id"));
         QRecord merged = new QRecord(old);
         merged.getValues().putAll(record.getValues());
         validate(record, merged);
      }
      return (records);
   }

   /*******************************************************************************
    ** Record-level messages are shown by normal core insert/update screens.
    *******************************************************************************/
   private void validate(QRecord output, QRecord merged)
   {
      try
      {
         String owner = merged.getValueString("owner_user_id");
         var session = QContext.getQSession();
         WatchDefinition.require(owner != null && !owner.isBlank(), "owner_user_id is required.");
         WatchDefinition.require(session != null && (session.hasPermission(DataAnalysisPermissions.ADMIN)
            || session.getUser() != null && owner.equals(session.getUser().getIdReference())), "Only an administrator may assign another watch owner.");
         WatchDefinition.parse(WatchRecords.resolve(merged));
      }
      catch(QException e)
      {
         output.addError(new BadInputStatusMessage(e.getMessage()));
      }
   }
}
