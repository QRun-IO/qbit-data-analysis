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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/*******************************************************************************
 ** Identical fixture built through core actions on all three supported test backends.
 *******************************************************************************/
abstract class EngineFixture
{
   protected DataAnalysisService service;
   protected DataAnalysisQBitConfig config;
   protected QInstance instance;
   protected Connection connection;

   /*******************************************************************************
    ** Null selects memory; subclasses supply real database metadata.
    *******************************************************************************/
   protected RDBMSBackendMetaData database()
   {
      return (null);
   }

   /*******************************************************************************
    ** Build metadata and insert rows without direct memory-store injection.
    *******************************************************************************/
   @BeforeEach
   void setup() throws Exception
   {
      instance = TestSupport.instance();
      RDBMSBackendMetaData database = database();
      String backend = database == null ? "memory" : "database_" + java.util.UUID.randomUUID();
      if(database != null)
      {
         instance.addBackend(database.withName(backend));
         connection = DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword());
         try(var statement = connection.createStatement())
         {
            statement.execute("DROP TABLE IF EXISTS orders");
            statement.execute("DROP TABLE IF EXISTS customer");
            statement.execute("CREATE TABLE customer (id INTEGER PRIMARY KEY, name VARCHAR(100))");
            statement.execute("CREATE TABLE orders (id INTEGER PRIMARY KEY, customer INTEGER, status VARCHAR(20), amount DECIMAL(20,4), quantity INTEGER, order_date DATE, created TIMESTAMP, note VARCHAR(100), paid BOOLEAN, modify_date TIMESTAMP, secret VARCHAR(100))");
         }
      }
      instance.addSecurityKeyType(new QSecurityKeyType().withName("customer_scope").withAllAccessKeyName("orders.all"));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("secret_access"));
      instance.addTable(new QTableMetaData().withName("customer").withBackendName(backend).withPrimaryKeyField("id")
         .withPermissionRules(DataAnalysisTables.permissionRules("customer")).withRecordLabelFormatAndFields("%s", "name")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("name", QFieldType.STRING)));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("customer"));
      instance.addPossibleValueSource(new QPossibleValueSource().withName("statusValues").withType(QPossibleValueSourceType.ENUM)
         .withEnumValues(List.of(new QPossibleValue<>("NEW", "New order"), new QPossibleValue<>("PAID", "Paid order"))));
      instance.addTable(new QTableMetaData().withName("orders").withBackendName(backend).withPrimaryKeyField("id")
         .withPermissionRules(DataAnalysisTables.permissionRules("orders"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("customer", QFieldType.INTEGER).withPossibleValueSourceName("customer"))
         .withField(new QFieldMetaData("status", QFieldType.STRING).withPossibleValueSourceName("statusValues"))
         .withField(new QFieldMetaData("amount", QFieldType.DECIMAL)).withField(new QFieldMetaData("quantity", QFieldType.INTEGER))
         .withField(new QFieldMetaData("order_date", QFieldType.DATE)).withField(new QFieldMetaData("created", QFieldType.DATE_TIME))
         .withField(new QFieldMetaData("note", QFieldType.STRING)).withField(new QFieldMetaData("paid", QFieldType.BOOLEAN))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withBackendName("modify_date"))
         .withField(new QFieldMetaData("secret", QFieldType.STRING).withFieldSecurityLock(new FieldSecurityLock().withSecurityKeyType("secret_access")
            .withDefaultBehavior(FieldSecurityLock.Behavior.DENY).withOverrideValues(List.of(true))))
         .withRecordSecurityLock(new RecordSecurityLock().withFieldName("customer").withSecurityKeyType("customer_scope").withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY)));
      new QInstanceEnricher(instance).enrich();
      QContext.init(instance, session());
      TestSupport.insert("customer", new QRecord().withValue("id", 1).withValue("name", "Alice Co"));
      TestSupport.insert("customer", new QRecord().withValue("id", 2).withValue("name", "Bob Co"));
      Integer[] amounts = {-10, 0, 10, 10, 20, 20, 30, 40, 50, 1000, null, 0};
      String[] notes = {null, "", " ", "AA-11", "BB-22", "CC-33", "x", "yy", "z", "", "AB12", "CD34"};
      for(int i = 0; i < amounts.length; i++)
      {
         TestSupport.insert("orders", new QRecord().withValue("id", i + 1).withValue("customer", i < 8 ? 1 : 2)
            .withValue("status", i % 2 == 0 ? "NEW" : "PAID").withValue("amount", amounts[i] == null ? null : BigDecimal.valueOf(amounts[i]))
            .withValue("quantity", i / 2 + 1).withValue("order_date", LocalDate.of(2026, 1, i < 4 ? 1 : 3))
            .withValue("created", Instant.parse(i < 4 ? "2026-01-01T12:00:00Z" : "2026-01-03T18:00:00Z"))
            .withValue("note", notes[i]).withValue("paid", i == 11 ? null : i % 2 == 0).withValue("modifyDate", Instant.parse("2026-10-13T12:00:00Z")));
      }
      config = new DataAnalysisQBitConfig().withBackendName("memory");
      service = new DataAnalysisService(config, Runnable::run);
   }

   /*******************************************************************************
    ** Fixture privileges include an explicit record-lock key.
    *******************************************************************************/
   protected QSession session()
   {
      return (TestSupport.admin().withPermissions("orders.read", "orders.insert", "orders.edit", "orders.analyze", "customer.read", "customer.insert")
         .withSecurityKeyValue("orders.all", true).withValue(QSession.VALUE_KEY_USER_TIMEZONE, "America/Chicago"));
   }

   /*******************************************************************************
    ** Clear context and close SQL connections.
    *******************************************************************************/
   @AfterEach
   void cleanup() throws Exception
   {
      QContext.clear();
      if(connection != null)
      {
         connection.close();
      }
   }
}
