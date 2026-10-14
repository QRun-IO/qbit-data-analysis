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
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qbits.dataanalysis.saved.SavedDefinition;
import com.kingsrook.qbits.dataanalysis.saved.SqlHandoff;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SavedSqlH2Test extends EngineFixture
{
   @Override
   protected RDBMSBackendMetaData database()
   {
      return (new RDBMSBackendMetaData().withVendor("h2").withUsername("sa").withPassword("")
         .withJdbcUrl("jdbc:h2:mem:saved_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE"));
   }

   @Test
   void actualMainAggregateHasSecurityFiltersAndWorkbenchPermission() throws Exception
   {
      instance.addApp(new QAppMetaData().withName("sqlWorkbench"));
      QContext.init(instance, SavedAnalysesTest.viewer("alice", 1));
      var definition = SavedDefinition.parse("PIVOT", "orders", Map.of("rows", List.of(Map.of("field", "status")), "values", List.of(Map.of("operator", "COUNT"))));
      assertThat(SqlHandoff.execute(definition, service).available()).isFalse();
      QContext.getQSession().withPermission("sql_workbench.hasAccess");
      var result = SqlHandoff.execute(definition, service);
      assertThat(result.available()).as(result.note()).isTrue();
      assertThat(result.sql().toLowerCase()).contains("select", "count(", "group by", "customer", "status");
      assertThat(result.note()).contains("Record security", "for reading");
   }

   @Test
   void profileMainStatementIsTheMetricBatchAndCaptureDoesNotMutateMetadata() throws Exception
   {
      instance.addApp(new QAppMetaData().withName("sqlWorkbench"));
      QContext.init(instance, SavedAnalysesTest.viewer("alice", 1).withPermission("sql_workbench.hasAccess"));
      var before = java.util.Set.copyOf(instance.getTable("orders").getEnabledCapabilities());
      var result = SqlHandoff.execute(SavedDefinition.parse("COLUMN_PROFILE", "orders", Map.of("fieldName", "amount")), service);
      assertThat(result.available()).as(result.note()).isTrue();
      assertThat(result.sql().toLowerCase()).contains("select", "amount", "customer").doesNotContain("group by");
      assertThat(instance.getTable("orders").getEnabledCapabilities()).containsExactlyInAnyOrderElementsOf(before);
      // A second request captures its own SQL rather than stale thread-local state.
      var next = SqlHandoff.execute(SavedDefinition.parse("CORRELATION", "orders", Map.of("fields", List.of("amount", "quantity"))), service);
      assertThat(next.available()).as(next.note()).isTrue();
      assertThat(next.sql().toLowerCase()).contains("corr(");
   }
}
