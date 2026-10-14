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

package com.kingsrook.qbits.dataanalysis.saved;

import java.util.List;
import java.util.Optional;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.permissions.DataAnalysisPermissions;
import com.kingsrook.qbits.dataanalysis.watch.WatchRecords;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;

/*******************************************************************************
 ** Validate all core CRUD and maintain the read-lock projection atomically.
 *******************************************************************************/
public class SavedCustomizer implements TableCustomizerInterface
{
   /*******************************************************************************
    ** Inserts cannot forge an owner or the derived shared-read key.
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
    ** Merge partial edits so changing shared or owner also changes the read lock.
    *******************************************************************************/
   @Override
   public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean preview, Optional<List<QRecord>> oldRecords) throws QException
   {
      for(QRecord patch : records)
      {
         QRecord merged = new QRecord(WatchRecords.get(DataAnalysisTables.SAVED, patch.getValue("id")));
         merged.getValues().putAll(patch.getValues());
         validate(patch, merged);
      }
      return (records);
   }

   /*******************************************************************************
    ** Report a plain validation message on the standard record editor.
    *******************************************************************************/
   private void validate(QRecord output, QRecord merged)
   {
      try
      {
         String owner = merged.getValueString("owner_user_id");
         var session = QContext.getQSession();
         if(owner == null || owner.isBlank() || session == null || (!(session.getSecurityKeyValues() != null && session.getSecurityKeyValues().getOrDefault(DataAnalysisPermissions.ADMIN, List.of()).contains(true))
            && (session.getUser() == null || !owner.equals(session.getUser().getIdReference()))))
         {
            throw new QException("Only an administrator may assign another analysis owner.");
         }
         SavedDefinition.parse(merged);
         output.withValue("read_owner_user_id", Boolean.TRUE.equals(merged.getValueBoolean("shared")) ? null : owner);
      }
      catch(QException e)
      {
         output.addError(new BadInputStatusMessage(e.getMessage()));
      }
   }
}
