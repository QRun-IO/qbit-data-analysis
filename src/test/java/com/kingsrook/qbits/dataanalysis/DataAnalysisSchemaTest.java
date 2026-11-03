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


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Execute generated saved-analysis DDL on H2 in PostgreSQL mode, including SQL's three-valued
 ** CHECK semantics. These direct writes intentionally bypass SavedCustomizer.
 *******************************************************************************/
class DataAnalysisSchemaTest
{
   /*******************************************************************************
    ** A null read owner grants shared reads, so only an explicit true shared flag
    ** may omit it. Null and omitted flags must fail closed like false.
    *******************************************************************************/
   @Test
   void savedAnalysisRequiresReadOwnerUnlessExplicitlyShared() throws Exception
   {
      try(Connection connection = DriverManager.getConnection("jdbc:h2:mem:saved_schema_" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE");
         var statement = connection.createStatement())
      {
         for(String sql : DataAnalysisSchema.postgresScript(new DataAnalysisQBitConfig().withBackendName("database")).split(";"))
         {
            if(sql.stripLeading().startsWith("CREATE TABLE IF NOT EXISTS data_analysis_saved ("))
            {
               statement.execute(sql);
            }
         }

         assertThatThrownBy(() -> statement.executeUpdate("INSERT INTO data_analysis_saved (name, table_name, kind, definition_json, owner_user_id) VALUES ('omitted flag', 'test_table', 'PIVOT', '{}', 'alice')"))
            .isInstanceOf(SQLException.class).hasMessageContaining("ck_data_analysis_saved_read_owner");
         assertThatThrownBy(() -> insert(connection, null, null)).isInstanceOf(SQLException.class).hasMessageContaining("ck_data_analysis_saved_read_owner");
         assertThatThrownBy(() -> insert(connection, false, null)).isInstanceOf(SQLException.class).hasMessageContaining("ck_data_analysis_saved_read_owner");

         insert(connection, true, null);
         insert(connection, false, "alice");
         insert(connection, null, "alice");
         try(var rows = statement.executeQuery("SELECT COUNT(*) FROM data_analysis_saved"))
         {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isEqualTo(3);
         }
      }
   }



   /*******************************************************************************
    ** Nullable values are bound explicitly so no Java boolean conversion can hide
    ** the SQL null case that caused the original constraint to admit private rows.
    *******************************************************************************/
   private static void insert(Connection connection, Boolean shared, String readOwner) throws SQLException
   {
      try(var statement = connection.prepareStatement("INSERT INTO data_analysis_saved (name, table_name, kind, definition_json, owner_user_id, shared, read_owner_user_id) VALUES ('schema test', 'test_table', 'PIVOT', '{}', 'alice', ?, ?)"))
      {
         statement.setObject(1, shared);
         statement.setString(2, readOwner);
         statement.executeUpdate();
      }
   }
}
