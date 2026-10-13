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

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.FieldSecurityLockHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.values.QPossibleValueTranslator;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;

/*******************************************************************************
 ** Translate through core while enforcing access to every referenced label.
 ** RC.5's direct table-PVS translation bypasses permissions, so table values are
 ** read with ENFORCE and handed to the translator as an ephemeral enum source.
 ** No shared metadata or session is mutated.
 *******************************************************************************/
public final class AnalysisLabels
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private AnalysisLabels()
   {
   }



   /*******************************************************************************
    ** Missing or denied labels fall back to the already-readable source id.
    *******************************************************************************/
   public static String translate(QFieldMetaData field, Serializable value) throws QException
   {
      return (translate(field, value, new LinkedHashSet<>()));
   }



   /*******************************************************************************
    ** Resolve nested PVS label fields without invoking core's trusted lookup path.
    *******************************************************************************/
   private static String translate(QFieldMetaData field, Serializable value, Set<String> visited) throws QException
   {
      if(value == null)
      {
         return (null);
      }
      String fallback = value.toString();
      QPossibleValueSource source = QContext.getQInstance().getPossibleValueSource(field.getPossibleValueSourceName());
      if(source == null || visited.size() >= 8 || !visited.add(source.getName()))
      {
         return (fallback);
      }
      try
      {
         QPossibleValueTranslator translator = new QPossibleValueTranslator();
         if(source.getType() != QPossibleValueSourceType.TABLE)
         {
            return (translator.translatePossibleValue(field, value));
         }
         QueryInput input = new QueryInput(source.getTableName());
         input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         QTableMetaData table = TableMetaDataPersonalizerAction.execute(input, true);
         QTableMetaData original = QContext.getQInstance().getTable(source.getTableName());
         List<String> labelFields = original.getRecordLabelFields() == null ? List.of() : original.getRecordLabelFields();
         for(String labelField : labelFields)
         {
            if(!table.getFields().containsKey(labelField) || !FieldSecurityLockHelper.isFieldAccessible(table.getField(labelField)))
            {
               return (fallback);
            }
         }
         String id = source.getOverrideIdField() == null ? table.getPrimaryKeyField() : source.getOverrideIdField();
         Set<String> selection = new LinkedHashSet<>(labelFields);
         selection.add(id);
         input.withFilter(DrillFilters.condition(id, QCriteriaOperator.EQUALS, value).withLimit(1)).withFieldNamesToInclude(selection)
            .withShouldFetchHeavyFields(true).withShouldGenerateDisplayValues(true);
         List<QRecord> records = new QueryAction().execute(input).getRecords();
         if(records.isEmpty())
         {
            return (fallback);
         }
         QRecord record = records.getFirst();
         for(String labelField : labelFields)
         {
            QFieldMetaData labelMetadata = table.getField(labelField);
            if(labelMetadata.getPossibleValueSourceName() != null)
            {
               record.withDisplayValue(labelField, translate(labelMetadata, record.getValue(labelField), visited));
            }
         }
         String label = QValueFormatter.formatRecordLabel(table, record);
         QPossibleValueSource permitted = new QPossibleValueSource().withName(source.getName()).withType(QPossibleValueSourceType.ENUM)
            .withValueFormat(source.getValueFormat()).withValueFields(source.getValueFields()).withEnumValues(List.of(new QPossibleValue<>(value, label)));
         return (translator.buildTranslatedPossibleValueList(permitted, List.of(value)).getFirst().getLabel());
      }
      catch(QPermissionDeniedException e)
      {
         return (fallback);
      }
      finally
      {
         visited.remove(source.getName());
      }
   }



   /*******************************************************************************
    ** Cache identity includes personalized field and related-label policies, so a
    ** host changing field/record locks cannot release older translated labels.
    *******************************************************************************/
   public static java.util.Map<String, Object> policy(String tableName) throws QException
   {
      java.util.Map<String, Object> result = new java.util.TreeMap<>();
      java.util.ArrayDeque<String> pending = new java.util.ArrayDeque<>();
      pending.add(tableName);
      while(!pending.isEmpty())
      {
         String name = pending.removeFirst();
         if(result.containsKey(name))
         {
            continue;
         }
         QueryInput input = new QueryInput(name);
         input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         QTableMetaData table = TableMetaDataPersonalizerAction.execute(input, true);
         java.util.Map<String, Object> entry = new java.util.TreeMap<>();
         entry.put("fields", table.getFields());
         entry.put("locks", table.getRecordSecurityLocks());
         entry.put("labelFields", table.getRecordLabelFields());
         entry.put("labelFormat", table.getRecordLabelFormat());
         entry.put("permissions", table.getPermissionRules());
         result.put(name, entry);
         for(QFieldMetaData field : table.getFields().values())
         {
            QPossibleValueSource source = QContext.getQInstance().getPossibleValueSource(field.getPossibleValueSourceName());
            if(source != null && source.getType() == QPossibleValueSourceType.TABLE)
            {
               pending.add(source.getTableName());
            }
         }
      }
      return (result);
   }
}
