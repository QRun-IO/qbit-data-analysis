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

package com.kingsrook.qbits.dataanalysis.permissions;


import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Data Analysis permissions through the core table feature registry.
 *******************************************************************************/
public final class DataAnalysisPermissions
{
   public static final String ADMIN = "data_analysis.admin";
   public static final String SAVED_READ = "data_analysis.saved.read";



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DataAnalysisPermissions()
   {
   }



   /*******************************************************************************
    ** Stand-in, replace with core TableFeaturePermissionsMetaData (DATA-ANALYSIS.md §3.6).
    ** Replicates the package-private PermissionsHelper effective-base-name logic.
    *******************************************************************************/
   public static String analyzePermissionName(QTableMetaData table)
   {
      QPermissionRules rules = PermissionsHelper.getEffectivePermissionRules(table, QContext.getQInstance());
      String base = rules != null && StringUtils.hasContent(rules.getPermissionBaseName()) ? rules.getPermissionBaseName() : table.getName();
      return (base + ".analyze");
   }



   /*******************************************************************************
    ** Analysis always needs its explicit permission; READ follows instance enforcement.
    *******************************************************************************/
   public static boolean canAnalyze(QTableMetaData table)
   {
      boolean read = !Boolean.TRUE.equals(QContext.getQInstance().getEnforcePermissionsInCoreActions())
         || PermissionsHelper.hasTablePermission(new QueryInput(table.getName()), table.getName(), TablePermissionSubType.READ);
      return (read && PermissionsHelper.hasTableFeaturePermission(QContext.getQInstance(), QContext.getQSession(), table, "analyze"));
   }
}
