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

package com.kingsrook.qbits.dataanalysis.run;

import java.io.Serializable;
import java.util.Locale;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisBackend;
import com.kingsrook.qqq.backend.core.actions.permissions.FieldSecurityLockHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;

/*******************************************************************************
 ** Cheap filtered data version; deliberately not a transactional snapshot guarantee.
 *******************************************************************************/
public final class DataVersion
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DataVersion()
   {
   }



   /*******************************************************************************
    ** Never bypass a lock on a modification field to construct a cache key.
    *******************************************************************************/
   public static String of(AnalysisBackend backend, String table, QQueryFilter filter) throws QException
   {
      QTableMetaData metadata = QContext.getQInstance().getTable(table);
      String versionField = metadata.getFields().values().stream().filter(FieldSecurityLockHelper::isFieldAccessible)
         .filter(f -> isModificationField(f.getName())).map(QFieldMetaData::getName).findFirst().orElse(metadata.getPrimaryKeyField());
      Serializable maximum = backend.extrema(table, filter, versionField).get("max");
      return (backend.count(table, filter) + ":" + versionField + ":" + maximum);
   }



   /*******************************************************************************
    ** Conventional host modification timestamps, in camel or snake case.
    *******************************************************************************/
   private static boolean isModificationField(String field)
   {
      String name = field.replace("_", "").toLowerCase(Locale.ROOT);
      return (name.equals("modifydate") || name.equals("modifiedat") || name.equals("updatedat") || name.equals("modifieddate"));
   }
}
