/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qbits.dataanalysis.schedule;


import java.util.function.Supplier;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitConfig;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.serviceidentity.ServiceIdentities;


/*******************************************************************************
 ** Sessions for scheduled analysis work. With the host's data-analysis service
 ** principal declared (QQQ service identities): inventory runs as the
 ** principal, and owner or recipient runs go on their behalf through it (their
 ** current access intersected with the principal's). Without it: the previous
 ** local sessions (single-node default unchanged). Never a system session.
 *******************************************************************************/
public final class AnalysisSessions
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private AnalysisSessions()
   {
   }



   /*******************************************************************************
    ** The session for schedule/watch inventory and status updates.
    *******************************************************************************/
   public static QSession maintenance(DataAnalysisQBitConfig config, Supplier<QSession> legacy) throws QException
   {
      if(usesPrincipal(config))
      {
         return (ServiceIdentities.session(QContext.getQInstance(), config.getServicePrincipal()));
      }
      return (legacy.get());
   }



   /*******************************************************************************
    ** The session for running as a user (owner or recipient).
    *******************************************************************************/
   public static QSession forUser(DataAnalysisQBitConfig config, String userId) throws QException
   {
      if(config.getOwnerSessionSupplier() != null)
      {
         return (config.getOwnerSessionSupplier().apply(userId));
      }
      if(usesPrincipal(config))
      {
         return (ServiceIdentities.sessionOnBehalfOf(QContext.getQInstance(), config.getServicePrincipal(), userId));
      }
      return (new QAuthenticationModuleDispatcher().getQModule(QContext.getQInstance().getAuthentication()).createAutomatedSessionForUser(QContext.getQInstance(), userId));
   }



   /*******************************************************************************
    ** Whether the host declared the configured principal.
    *******************************************************************************/
   private static boolean usesPrincipal(DataAnalysisQBitConfig config)
   {
      return (config.getServicePrincipal() != null && ServiceIdentities.isDeclared(QContext.getQInstance(), config.getServicePrincipal()));
   }
}
