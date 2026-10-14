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

import com.kingsrook.qqq.backend.core.actions.permissions.FieldSecurityLockHelper;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;

/*******************************************************************************
 ** Legacy lock adapter. The core tier API (qbit-secured-data) will map REVEALABLE
 ** to EXACT_MATCH_ONLY, MASKED/HIDDEN without filter rights to NONE, and
 ** MASKED/HIDDEN with filter rights to FULL.
 *******************************************************************************/
public class FieldSecurityLockFieldAccessResolver implements FieldAnalysisAccessResolver
{
   /*******************************************************************************
    ** Keep the legacy lock enforcement when core tiers are not installed.
    *******************************************************************************/
   @Override
   public FieldAnalysisAccess resolve(QTableMetaData table, QFieldMetaData field)
   {
      return (FieldSecurityLockHelper.isFieldAccessible(field) ? FieldAnalysisAccess.FULL : FieldAnalysisAccess.NONE);
   }
}
