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


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dataanalysis.engine.EffectiveConfigResolver;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisFieldPossibleValueProvider;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueSearchFilterUseCase;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Dependent possible values through the same search inputs used by form requests.
 *******************************************************************************/
class DataAnalysisPossibleValuesTest
{
   /*******************************************************************************
    ** Release the request context.
    *******************************************************************************/
   @AfterEach
   void cleanup()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** The watch table selection filters saved analyses without widening owner locks.
    *******************************************************************************/
   @Test
   void watchSavedAnalysisDependsOnTable() throws Exception
   {
      QInstance instance = TestSupport.instance();
      QRecord orders = TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("alice", false));
      QRecord customers = TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("alice", false).withValue("table_name", "customers"));
      TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("bob", true));
      QContext.init(instance, TestSupport.user("alice").withPermissions("data_analysis.saved.read", "data_analysis.watch.edit"));
      QFieldMetaData field = instance.getTable(DataAnalysisTables.WATCH).getField("saved_id");
      assertThat(search(field, Map.of("table_name", "orders"))).extracting(value -> (Object) value.getId()).containsExactly(orders.getValueInteger("id"));
      assertThat(search(field, Map.of("table_name", "customers"))).extracting(value -> (Object) value.getId()).containsExactly(customers.getValueInteger("id"));
      assertThat(search(field, Map.of())).isEmpty();
      assertThat(search(field, Map.of("table_name", "missing"))).isEmpty();
   }



   /*******************************************************************************
    ** All four table references use the core source, including host-owned sources.
    *******************************************************************************/
   @Test
   void tableNamesUseCoreTablesSource() throws Exception
   {
      QInstance instance = TestSupport.instance();
      instance.addTable(new QTableMetaData().withName("orders").withLabel("Orders"));
      for(String tableName : List.of(DataAnalysisTables.TABLE_CONFIG, DataAnalysisTables.SAVED, DataAnalysisTables.WATCH, DataAnalysisTables.RUN))
      {
         QFieldMetaData field = instance.getTable(tableName).getField("table_name");
         assertThat(field.getPossibleValueSourceName()).isEqualTo("tables");
         assertThat(search(field, Map.of())).extracting(value -> value.getId().toString()).containsExactlyInAnyOrderElementsOf(instance.getTables().keySet());
      }
      var hostSource = instance.getPossibleValueSource("tables").withLabel("Host tables");
      QInstance host = new QInstance();
      host.addBackend(instance.getBackend("memory"));
      host.addPossibleValueSource(hostSource);
      new DataAnalysisQBitProducer().withQBitConfig(new DataAnalysisQBitConfig().withBackendName("memory")).produce(host);
      assertThat(host.getPossibleValueSource("tables")).isSameAs(hostSource);
      assertThat(host.getPossibleValueSource("tables").getLabel()).isEqualTo("Host tables");
   }



   /*******************************************************************************
    ** Both CSV settings offer only selected-table fields and retain CSV parsing.
    *******************************************************************************/
   @Test
   void fieldPickersDependOnTableAndKeepCsvStorage() throws Exception
   {
      QInstance instance = TestSupport.instance();
      instance.addTable(new QTableMetaData().withName("orders").withLabel("Orders")
         .withField(new QFieldMetaData("amount", QFieldType.DECIMAL).withLabel("Order total"))
         .withField(new QFieldMetaData("status", QFieldType.STRING)));
      instance.addTable(new QTableMetaData().withName("customers").withLabel("Customers")
         .withField(new QFieldMetaData("email", QFieldType.STRING).withLabel("Email")));
      for(String fieldName : List.of("analyzable_fields", "excluded_fields"))
      {
         QFieldMetaData field = instance.getTable(DataAnalysisTables.TABLE_CONFIG).getField(fieldName);
         assertThat(field.getType()).isEqualTo(QFieldType.TEXT);
         assertThat(instance.getPossibleValueSource(field.getPossibleValueSourceName()).getType()).isEqualTo(QPossibleValueSourceType.CUSTOM);
         assertThat(search(field, Map.of("table_name", "orders"))).extracting(value -> (Object) value.getId()).containsExactly("amount", "status");
         assertThat(search(field, Map.of("table_name", "orders"))).extracting(QPossibleValue::getLabel).containsExactly("Order total (amount)", "status (status)");
         assertThat(search(field, Map.of("table_name", "customers"))).extracting(value -> (Object) value.getId()).containsExactly("email");
         assertThat(search(field, Map.of())).isEmpty();
         assertThat(search(field, Map.of("table_name", ""))).isEmpty();
         assertThat(search(field, Map.of("table_name", "missing"))).isEmpty();
         assertThat(search(field, null)).isEmpty();
      }
      SearchPossibleValueSourceInput input = new SearchPossibleValueSourceInput().withPossibleValueSourceName(DataAnalysisFieldPossibleValueProvider.NAME)
         .withOtherValues(Map.of("table_name", "orders")).withSearchTerm("TOTAL");
      assertThat(new SearchPossibleValueSourceAction().execute(input).getResults()).extracting(value -> (Object) value.getId()).containsExactly("amount");
      input.withSearchTerm("AMOUNT");
      assertThat(new SearchPossibleValueSourceAction().execute(input).getResults()).extracting(value -> (Object) value.getId()).containsExactly("amount");
      input.withIdList(List.of("status", "email"));
      assertThat(new SearchPossibleValueSourceAction().execute(input).getResults()).extracting(value -> (Object) value.getId()).containsExactly("status");
      DataAnalysisFieldPossibleValueProvider provider = new DataAnalysisFieldPossibleValueProvider();
      assertThat(provider.getPossibleValue(null)).isNull();
      assertThat(provider.getPossibleValue("amount,status").getId()).isEqualTo("amount,status");
      TestSupport.insert(DataAnalysisTables.TABLE_CONFIG, new QRecord().withValue("table_name", "orders")
         .withValue("analyzable_fields", "amount, status").withValue("excluded_fields", "status, amount"));
      var config = new EffectiveConfigResolver(new DataAnalysisQBitConfig().withBackendName("memory")).resolve("orders");
      assertThat(config.getAnalyzableFields()).containsExactlyInAnyOrder("amount", "status");
      assertThat(config.getExcludedFields()).containsExactlyInAnyOrder("amount", "status");
   }



   /*******************************************************************************
    ** Mirror TablePossibleValuesSpecV1: clone and interpret the field filter as FORM.
    *******************************************************************************/
   private List<QPossibleValue<?>> search(QFieldMetaData field, Map<String, Serializable> otherValues) throws Exception
   {
      SearchPossibleValueSourceInput input = new SearchPossibleValueSourceInput()
         .withPossibleValueSourceName(field.getPossibleValueSourceName()).withOtherValues(otherValues);
      if(field.getPossibleValueSourceFilter() != null)
      {
         var filter = field.getPossibleValueSourceFilter().clone();
         filter.interpretValues(Map.of("input", otherValues), PossibleValueSearchFilterUseCase.FORM);
         input.withDefaultQueryFilter(filter);
      }
      return (new SearchPossibleValueSourceAction().execute(input).getResults());
   }
}
