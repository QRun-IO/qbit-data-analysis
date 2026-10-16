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


import java.io.Serializable;
import java.util.HashMap;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.schedule.AnalysisSessions;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule;
import com.kingsrook.qqq.backend.core.serviceidentity.QDelegatedSession;
import com.kingsrook.qqq.backend.core.serviceidentity.QServicePrincipalMetaData;
import com.kingsrook.qqq.backend.core.serviceidentity.ServiceIdentities;
import com.kingsrook.qqq.backend.core.serviceidentity.ServiceIdentitiesMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Owner decision 2026-10-16: a schedule or watch run for its creator (or a
 ** recipient) through the data-analysis principal sees exactly what that user
 ** can see - their permissions and record-lock keys, not the principal's.
 *******************************************************************************/
class AnalysisSessionsTest extends EngineFixture
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void runsForTheCreatorWithExactlyTheCreatorsAccess() throws Exception
   {
      instance.withInstanceDefaultAuthentication(new com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData().withName("mock")
         .withType(com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType.MOCK));
      instance.getAuthentication().setCustomizer(new QCodeReference(CreatorCustomizer.class));
      ServiceIdentitiesMetaData.declare(instance, new QServicePrincipalMetaData().withName("data-analysis").withMayActOnBehalfOfUsers(true)
         .withLocalPermissions("data_analysis.saved.read"));
      DataAnalysisQBitConfig creatorConfig = new DataAnalysisQBitConfig().withServicePrincipal("data-analysis");

      QSession own     = new MockAuthenticationModule().createAutomatedSessionForUser(instance, "alice");
      QSession forUser = AnalysisSessions.forUser(creatorConfig, "alice");
      assertThat(forUser).isInstanceOf(QDelegatedSession.class);
      assertThat(forUser.getPermissions()).isEqualTo(own.getPermissions()).contains("orders.analyze");
      assertThat(new HashMap<>(forUser.getSecurityKeyValues())).isEqualTo(new HashMap<>(own.getSecurityKeyValues()));
      assertThat(forUser.getSecurityKeyValues().get(DataAnalysisTables.USER_KEY)).containsExactly("alice");
      assertThat(ServiceIdentities.attributionName(forUser)).endsWith("via service:data-analysis");
   }



   /*******************************************************************************
    ** The creator's own grants and owner key, as the host customizer sets them.
    *******************************************************************************/
   public static class CreatorCustomizer implements QAuthenticationModuleCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void customizeAutomatedSessionForUser(QInstance qInstance, QSession session, Serializable userId)
      {
         session.withPermissions("orders.read", "orders.analyze", "data_analysis.saved.read", "data_analysis.saved.edit");
         session.withSecurityKeyValue(DataAnalysisTables.USER_KEY, String.valueOf(userId));
      }
   }
}
