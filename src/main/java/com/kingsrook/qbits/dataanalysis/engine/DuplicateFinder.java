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

import java.time.Instant;
import com.kingsrook.qbits.dataanalysis.result.DuplicatesRequest;
import com.kingsrook.qbits.dataanalysis.result.DuplicatesResult;
import com.kingsrook.qqq.backend.core.exceptions.QException;

/*******************************************************************************
 ** Top fifty duplicate tuples with permission-filtered membership drills.
 *******************************************************************************/
public class DuplicateFinder
{
   private final AnalysisBackend backend;

   /*******************************************************************************
    ** Bind primitive operations.
    *******************************************************************************/
   public DuplicateFinder(AnalysisBackend backend)
   {
      this.backend = backend;
   }



   /*******************************************************************************
    ** Guard tuples before exposing or serializing them.
    *******************************************************************************/
   public DuplicatesResult find(DuplicatesRequest request, EffectiveConfig config) throws QException
   {
      AnalysisBackend.Scope scope = backend.scope(request.getTableName(), request.getFilter(), true);
      return (new DuplicatesResult().withTableName(request.getTableName()).withFields(request.getFields()).withFilter(DrillFilters.copy(request.getFilter()))
         .withRowsScanned(scope.rowsScanned()).withTruncated(scope.truncated()).withAsOf(Instant.now())
         .withGroups(new MinGroupSizeGuard(config.getMinGroupSize()).groups(ProfileSupport.values(request.getTableName(), scope.filter(), null,
            backend.duplicates(request.getTableName(), scope.filter(), request.getFields(), 50), scope.rowsScanned()))));
   }
}
