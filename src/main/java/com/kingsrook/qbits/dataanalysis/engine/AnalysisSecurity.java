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

import java.io.Serializable;
import java.util.List;
import com.kingsrook.qbits.dataanalysis.permissions.DataAnalysisPermissions;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;

/*******************************************************************************
 ** Fail closed before any source-table computation, including cache retrieval.
 *******************************************************************************/
public class AnalysisSecurity
{
   private final EffectiveConfigResolver configs;
   private final FieldAnalysisAccessResolver fieldAccess;

   /*******************************************************************************
    ** Use the runtime's configuration policy.
    *******************************************************************************/
   public AnalysisSecurity(EffectiveConfigResolver configs)
   {
      this(configs, CoreTierFieldAccessResolver.defaultResolver());
   }



   /*******************************************************************************
    ** Bind the host field policy.
    *******************************************************************************/
   public AnalysisSecurity(EffectiveConfigResolver configs, FieldAnalysisAccessResolver fieldAccess)
   {
      this.configs = configs;
      this.fieldAccess = fieldAccess;
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
      return (table.getFields().values().stream().filter(f -> fieldAccess.resolve(table, f) == FieldAnalysisAccess.FULL)
         .filter(f -> c.getAnalyzableFields().isEmpty() || c.getAnalyzableFields().contains(f.getName()))
         .filter(f -> !c.getExcludedFields().contains(f.getName())).toList());
   }



   /*******************************************************************************
    ** A common denial does not disclose a hidden field's existence or values.
    *******************************************************************************/
   public QFieldMetaData field(String table, String field) throws QException
   {
      return (analyzableFields(table).stream().filter(f -> f.getName().equals(field)).findFirst()
         .orElseThrow(() -> new QBadRequestException("Unknown analysis field.")));
   }

   /*******************************************************************************
    ** Core fingerprint plus the sorted FULL-field list, hashed by the run identity.
    *******************************************************************************/
   public String accessFingerprint(String table) throws QException
   {
      return (fieldAccess.getAccessFingerprint() + ":" + JsonUtils.toJson(analyzableFields(table).stream().map(QFieldMetaData::getName).sorted().toList()));
   }

   /*******************************************************************************
    ** Check a joined field without requiring the related table analyze permission.
    *******************************************************************************/
   public QFieldMetaData joinedField(String tableName, String fieldName) throws QException
   {
      QTableMetaData table = joinedTable(tableName);
      QFieldMetaData field = table.getFields().get(fieldName);
      if(field == null || fieldAccess.resolve(table, field) != FieldAnalysisAccess.FULL)
      {
         throw new QBadRequestException("Unknown analysis field.");
      }
      return (field);
   }

   /*******************************************************************************
    ** Display labels use the same policy as the selected pivot field.
    *******************************************************************************/
   public String label(QFieldMetaData field, Serializable value) throws QException
   {
      return (AnalysisLabels.translate(field, value, fieldAccess));
   }

   /*******************************************************************************
    ** Each intermediate exposed-join table requires READ, without analyze rights.
    *******************************************************************************/
   public QTableMetaData joinedTable(String tableName) throws QException
   {
      QueryInput input = new QueryInput(tableName);
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      if(QContext.getQInstance().getTable(tableName) == null || !PermissionsHelper.hasTablePermission(input, tableName, TablePermissionSubType.READ))
      {
         throw new QBadRequestException("Unknown analysis field.");
      }
      return (TableMetaDataPersonalizerAction.execute(input, true));
   }
}
