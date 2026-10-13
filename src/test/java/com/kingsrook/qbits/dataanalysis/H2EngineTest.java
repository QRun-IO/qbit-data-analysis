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

import java.util.UUID;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;

/*******************************************************************************
 ** H2 executes the identical fixture through the RDBMS module.
 *******************************************************************************/
class H2EngineTest extends EngineContractTest
{
   /*******************************************************************************
    ** An isolated database for each test.
    *******************************************************************************/
   @Override
   protected RDBMSBackendMetaData database()
   {
      return (new RDBMSBackendMetaData().withVendor("h2").withUsername("sa").withPassword("")
         .withJdbcUrl("jdbc:h2:mem:analysis_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE"));
   }
}
