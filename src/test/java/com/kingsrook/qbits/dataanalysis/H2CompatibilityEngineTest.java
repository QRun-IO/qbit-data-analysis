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

import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;

/*******************************************************************************
 ** Verify native bucket SQL in H2's ordinal-grouping compatibility mode.
 *******************************************************************************/
class H2CompatibilityEngineTest extends H2EngineTest
{
   /*******************************************************************************
    ** H2 supports core's GROUP BY positions in PostgreSQL compatibility mode.
    *******************************************************************************/
   @Override
   protected RDBMSBackendMetaData database()
   {
      RDBMSBackendMetaData database = super.database();
      return (database.withJdbcUrl(database.getJdbcUrl() + ";MODE=PostgreSQL"));
   }
}
