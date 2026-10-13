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
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.engine.StandInAnalysisBackend;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfileRequest;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/*******************************************************************************
 ** RAND samples fluctuate around population counts without changing drill scope.
 *******************************************************************************/
class H2SamplingTest extends EngineFixture
{
   /*******************************************************************************
    ** Ordinal grouping is supported by H2's PostgreSQL compatibility mode.
    *******************************************************************************/
   @Override
   protected RDBMSBackendMetaData database()
   {
      return (new RDBMSBackendMetaData().withVendor("h2").withUsername("sa").withPassword("")
         .withJdbcUrl("jdbc:h2:mem:sampling_" + java.util.UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;MODE=PostgreSQL"));
   }

   /*******************************************************************************
    ** A large sample gives stable tolerances and includes filtered-out records.
    *******************************************************************************/
   @Test
   void randomCountsAndHistograms() throws Exception
   {
      try(var statement = connection.createStatement())
      {
         statement.execute("INSERT INTO orders (id, customer, amount) SELECT x, 1, CASE WHEN MOD(x, 4) = 0 THEN NULL ELSE MOD(x, 10) END FROM SYSTEM_RANGE(1001, 11000) AS source(x)");
      }
      config.withDefaultSampleThresholdRows(100).withDefaultSampleRows(2000);
      service.getConfigResolver().invalidate();
      var filter = DrillFilters.condition("id", QCriteriaOperator.GREATER_THAN, 1000);
      var profile = service.columnProfile(new ColumnProfileRequest().withTableName("orders").withFieldName("amount").withFilter(filter));
      assertThat(profile.getEstimate()).isTrue();
      var metrics = profile.getCounts().stream().collect(java.util.stream.Collectors.toMap(m -> m.getKey(), m -> m));
      assertThat(metrics.get("rows").getValue()).isEqualTo(10000L);
      assertThat(metrics.get("rows").getEstimate()).isFalse();
      assertThat(((Number) metrics.get("nulls").getValue()).longValue()).isCloseTo(2500L, within(500L));
      assertThat(((Number) metrics.get("nonNull").getValue()).longValue()).isCloseTo(7500L, within(500L));
      assertThat(metrics.get("nonNull").getFormatted()).startsWith("≈ ");
      assertThat(profile.getNumeric().getHistogram().stream().mapToLong(v -> v.getCount()).sum()).isCloseTo(7500L, within(700L));
      var exact = new StandInAnalysisBackend(service.getConfigResolver().resolve("orders"));
      assertThat(exact.count("orders", metrics.get("rows").getDrill().getFilter())).isEqualTo(10000);
      for(var value : profile.getTopValues())
      {
         long population = exact.count("orders", value.getDrill().getFilter());
         assertThat(value.getCount()).isCloseTo(population, within(500L));
      }
      assertThat(com.kingsrook.qqq.backend.core.utils.JsonUtils.toJson(profile)).doesNotContain("\"operator\":\"IN\"");
      var table = service.tableProfile(new com.kingsrook.qbits.dataanalysis.result.TableProfileRequest().withTableName("orders").withFilter(filter).withFields(List.of("amount")));
      assertThat(table.getRowCount().getValue()).isEqualTo(10000L);
      assertThat(table.getEstimate()).isTrue();
   }
}
