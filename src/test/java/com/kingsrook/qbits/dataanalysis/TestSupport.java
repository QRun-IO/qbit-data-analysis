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

package com.kingsrook.qbits.dataanalysis;


import java.util.List;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.permissions.DataAnalysisPermissions;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;


/*******************************************************************************
 ** Real core actions against an isolated memory backend.
 *******************************************************************************/
final class TestSupport
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private TestSupport()
   {
   }



   /*******************************************************************************
    ** Build the configured instance and establish an admin fixture session.
    *******************************************************************************/
   static QInstance instance() throws QException
   {
      MemoryRecordStore.getInstance().reset();
      QInstance instance = new QInstance().withEnforcePermissionsInCoreActions(true)
         .withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("test").withType(QAuthenticationType.FULLY_ANONYMOUS));
      instance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      new DataAnalysisQBitProducer().withQBitConfig(new DataAnalysisQBitConfig().withBackendName("memory")).produce(instance);
      new QInstanceEnricher(instance).enrich();
      QContext.init(instance, admin());
      return (instance);
   }



   /*******************************************************************************
    ** Admin permission and the separately authorized record-lock all-access key.
    *******************************************************************************/
   static QSession admin()
   {
      QSession session = user("admin").withPermission(DataAnalysisPermissions.ADMIN).withSecurityKeyValue(DataAnalysisPermissions.ADMIN, true);
      for(String base : List.of("data_analysis.saved", "data_analysis.watch", "data_analysis.settings", "data_analysis.run"))
      {
         session.withPermissions(base + ".read", base + ".insert", base + ".edit", base + ".delete");
      }
      return (session);
   }



   /*******************************************************************************
    ** The host authentication adapter supplies the user-id key.
    *******************************************************************************/
   static QSession user(String id)
   {
      return (new QSession().withUser(new QUser().withIdReference(id)).withSecurityKeyValue(DataAnalysisTables.USER_KEY, id));
   }



   /*******************************************************************************
    ** Fixture writes use normal enforced core actions, never direct store injection.
    *******************************************************************************/
   static QRecord insert(String table, QRecord record) throws QException
   {
      InsertInput input = new InsertInput(table).withRecords(List.of(record));
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      QRecord result = new InsertAction().execute(input).getRecords().getFirst();
      org.assertj.core.api.Assertions.assertThat(result.getErrors()).isNullOrEmpty();
      return (result);
   }



   /*******************************************************************************
    ** A minimally valid saved definition.
    *******************************************************************************/
   static QRecord saved(String owner, boolean shared)
   {
      return (new QRecord().withValue("name", "Profile").withValue("table_name", "orders").withValue("kind", "COLUMN_PROFILE")
         .withValue("definition_json", "{}").withValue("owner_user_id", owner).withValue("shared", shared));
   }
}
