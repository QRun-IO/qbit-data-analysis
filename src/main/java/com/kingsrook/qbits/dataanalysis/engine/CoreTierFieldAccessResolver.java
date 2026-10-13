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

import java.lang.reflect.Method;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;

/*******************************************************************************
 ** Optional core bridge: replace with direct calls once QQQ ships the fieldaccess package.
 ** Class and method discovery happen once; invocation failures fail closed.
 *******************************************************************************/
public class CoreTierFieldAccessResolver implements FieldAnalysisAccessResolver
{
   private static final CoreTierFieldAccessResolver DEFAULT = discover();
   private final Method tier;
   private final Method fingerprint;

   /*******************************************************************************
    ** Bind discovered static methods once.
    *******************************************************************************/
   public CoreTierFieldAccessResolver(Class<?> helper) throws ReflectiveOperationException
   {
      tier = helper.getMethod("getTier", QTableMetaData.class, String.class);
      Method optional;
      try
      {
         optional = helper.getMethod("getAccessFingerprint");
      }
      catch(NoSuchMethodException e)
      {
         optional = null;
      }
      fingerprint = optional;
   }

   /*******************************************************************************
    ** Resolve the optional class once per class loader.
    *******************************************************************************/
   private static CoreTierFieldAccessResolver discover()
   {
      try
      {
         return (new CoreTierFieldAccessResolver(Class.forName("com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper")));
      }
      catch(ClassNotFoundException e)
      {
         return (null);
      }
      catch(ReflectiveOperationException e)
      {
         throw new ExceptionInInitializerError(e);
      }
   }

   /*******************************************************************************
    ** Use legacy locks only when the core helper class is absent.
    *******************************************************************************/
   public static FieldAnalysisAccessResolver defaultResolver()
   {
      return (DEFAULT == null ? new FieldSecurityLockFieldAccessResolver() : DEFAULT);
   }

   /*******************************************************************************
    ** Unknown tiers and invocation failures never grant analysis rights.
    *******************************************************************************/
   @Override
   public FieldAnalysisAccess resolve(QTableMetaData table, QFieldMetaData field)
   {
      try
      {
         return (switch(String.valueOf(tier.invoke(null, table, field.getName())))
         {
            case "FULL" -> FieldAnalysisAccess.FULL;
            case "REVEALABLE" -> FieldAnalysisAccess.EXACT_MATCH_ONLY;
            default -> FieldAnalysisAccess.NONE;
         });
      }
      catch(ReflectiveOperationException e)
      {
         return (FieldAnalysisAccess.NONE);
      }
   }

   /*******************************************************************************
    ** A broken fingerprint prevents cache reuse rather than returning stale data.
    *******************************************************************************/
   @Override
   public String getAccessFingerprint()
   {
      try
      {
         return (fingerprint == null ? "" : (String) fingerprint.invoke(null));
      }
      catch(ReflectiveOperationException e)
      {
         throw new IllegalStateException("Could not resolve field access fingerprint.", e);
      }
   }
}
