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

package com.kingsrook.qbits.dataanalysis.model;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** On INSERT only, an owner/user field left empty gets the inserting user's id,
 ** so a UI (which does not know the user's id) can create a saved analysis,
 ** star or watch. Unlike core DynamicDefaultValueBehavior.USER_ID it never
 ** applies on UPDATE, so an administrator's partial edit cannot take ownership.
 ** Naming another owner is still refused by the table customizers and locks.
 *******************************************************************************/
public enum InsertingUserBehavior implements FieldBehavior<InsertingUserBehavior>
{
   INSERTING_USER,
   NONE;



   /*******************************************************************************
    ** No behavior unless asked for.
    *******************************************************************************/
   @Override
   public InsertingUserBehavior getDefault()
   {
      return (NONE);
   }



   /*******************************************************************************
    ** Fill empty values with the session user's id on insert.
    *******************************************************************************/
   @Override
   public void apply(ValueBehaviorApplier.Action action, List<QRecord> recordList, QInstance instance, QTableMetaData table, QFieldMetaData field)
   {
      if(this == NONE || action != ValueBehaviorApplier.Action.INSERT)
      {
         return;
      }
      String userId = QContext.getQSession() == null || QContext.getQSession().getUser() == null ? null : QContext.getQSession().getUser().getIdReference();
      if(!StringUtils.hasContent(userId))
      {
         return;
      }
      for(QRecord record : CollectionUtils.nonNullList(recordList))
      {
         if(!StringUtils.hasContent(record.getValueString(field.getName())))
         {
            record.setValue(field.getName(), userId);
         }
      }
   }
}
