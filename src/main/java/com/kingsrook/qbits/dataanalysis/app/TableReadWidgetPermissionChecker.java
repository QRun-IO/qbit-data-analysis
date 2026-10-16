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

package com.kingsrook.qbits.dataanalysis.app;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.permissions.CustomPermissionChecker;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.InitializableViaCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceWithProperties;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** A widget is available only to sessions that can READ the table it summarizes.
 ** Contributes no catalog entry of its own: the table's READ permission is the
 ** permission.
 *******************************************************************************/
public class TableReadWidgetPermissionChecker implements CustomPermissionChecker, InitializableViaCodeReference
{
   private static final String TABLE_NAME_PROPERTY = "tableName";

   private String tableName;



   /*******************************************************************************
    ** Code reference for the given table.
    *******************************************************************************/
   public static QCodeReferenceWithProperties build(String tableName)
   {
      return (new QCodeReferenceWithProperties(TableReadWidgetPermissionChecker.class, MapBuilder.of(TABLE_NAME_PROPERTY, tableName)));
   }



   /*******************************************************************************
    ** Read the table name from the code reference properties.
    *******************************************************************************/
   @Override
   public void initialize(QCodeReference codeReference)
   {
      if(codeReference instanceof QCodeReferenceWithProperties withProperties)
      {
         Map<String, Serializable> properties = withProperties.getProperties();
         tableName = ValueUtils.getValueAsString(properties.get(TABLE_NAME_PROPERTY));
      }
   }



   /*******************************************************************************
    ** Deny unless the session may READ the table.
    *******************************************************************************/
   @Override
   public void checkPermissionsThrowing(AbstractActionInput actionInput, MetaDataWithPermissionRules metaDataWithPermissionRules) throws QPermissionDeniedException
   {
      AbstractTableActionInput tableInput = new AbstractTableActionInput();
      tableInput.setTableName(tableName);
      PermissionsHelper.checkTablePermissionThrowing(tableInput, TablePermissionSubType.READ);
   }
}
