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

import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/*******************************************************************************
 ** Actual PostgreSQL tests; skip cleanly if Docker is unavailable.
 *******************************************************************************/
class PostgresEngineTest extends EngineContractTest
{
   private static PostgreSQLContainer postgres;

   /*******************************************************************************
    ** Never substitute another database for the PostgreSQL contract.
    *******************************************************************************/
   @BeforeAll
   static void start()
   {
      if(DockerClientFactory.instance().isDockerAvailable())
      {
         postgres = new PostgreSQLContainer("postgres:17-alpine");
         postgres.start();
      }
   }

   /*******************************************************************************
    ** Release the test container.
    *******************************************************************************/
   @AfterAll
   static void stop()
   {
      if(postgres != null)
      {
         postgres.stop();
      }
   }

   /*******************************************************************************
    ** Credentials belong only to the ephemeral test container.
    *******************************************************************************/
   @Override
   protected RDBMSBackendMetaData database()
   {
      Assumptions.assumeTrue(postgres != null, "Docker unavailable; PostgreSQL contract skipped");
      return (new PostgreSQLBackendMetaData().withJdbcUrl(postgres.getJdbcUrl())
         .withUsername(postgres.getUsername()).withPassword(postgres.getPassword()));
   }
}
