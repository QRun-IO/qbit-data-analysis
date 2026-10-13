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

package com.kingsrook.qbits.dataanalysis.engine;

import java.util.List;
import com.kingsrook.qbits.dataanalysis.permissions.DataAnalysisPermissions;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.FieldSecurityLockHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;

/*******************************************************************************
 ** Fail closed before any source-table computation, including cache retrieval.
 *******************************************************************************/
public class AnalysisSecurity
{
   private final EffectiveConfigResolver configs;

   /*******************************************************************************
    ** Use the runtime's configuration policy.
    *******************************************************************************/
   public AnalysisSecurity(EffectiveConfigResolver configs)
   {
      this.configs = configs;
   }



   /*******************************************************************************
    ** Explicit enforcement is independent of the instance's default switch.
    *******************************************************************************/
   public QTableMetaData table(String name) throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTable(name);
      QueryInput input = new QueryInput(name);
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      if(table == null || !configs.resolve(name).getEnabled() || !DataAnalysisPermissions.canAnalyze(table)
         || !PermissionsHelper.hasTablePermission(input, name, TablePermissionSubType.READ))
      {
         throw new QPermissionDeniedException("Analysis is unavailable for this table.");
      }
      return (TableMetaDataPersonalizerAction.execute(input, true));
   }



   /*******************************************************************************
    ** Use personalized fields and locks; excluded fields never appear in capabilities.
    *******************************************************************************/
   public List<QFieldMetaData> analyzableFields(String name) throws QException
   {
      QTableMetaData table = table(name);
      EffectiveConfig c = configs.resolve(name);
      return (table.getFields().values().stream().filter(FieldSecurityLockHelper::isFieldAccessible)
         .filter(f -> c.getAnalyzableFields().isEmpty() || c.getAnalyzableFields().contains(f.getName()))
         .filter(f -> !c.getExcludedFields().contains(f.getName())).toList());
   }



   /*******************************************************************************
    ** A common denial does not disclose a hidden field's existence or values.
    *******************************************************************************/
   public QFieldMetaData field(String table, String field) throws QException
   {
      return (analyzableFields(table).stream().filter(f -> f.getName().equals(field)).findFirst()
         .orElseThrow(() -> new QPermissionDeniedException("Analysis is unavailable for this field.")));
   }
}
