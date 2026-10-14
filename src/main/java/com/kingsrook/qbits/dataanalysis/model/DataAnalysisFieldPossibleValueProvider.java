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


package com.kingsrook.qbits.dataanalysis.model;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.values.QCustomPossibleValueProvider;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Fields of the table selected in the settings form. Core has no multi-select
 ** PVS field yet; settings retain their comma-separated TEXT representation.
 *******************************************************************************/
public class DataAnalysisFieldPossibleValueProvider implements QCustomPossibleValueProvider<String>
{
   public static final String NAME = "data_analysis_fields";



   /*******************************************************************************
    ** Without form values the table is unknown; preserve names, including CSV lists.
    *******************************************************************************/
   @Override
   public QPossibleValue<String> getPossibleValue(Serializable idValue)
   {
      String name = ValueUtils.getValueAsString(idValue);
      return (name == null ? null : new QPossibleValue<>(name, name));
   }



   /*******************************************************************************
    ** Offer only fields of the selected table, honoring search text and id lookups.
    *******************************************************************************/
   @Override
   public List<QPossibleValue<String>> search(SearchPossibleValueSourceInput input)
   {
      List<QPossibleValue<String>> values = new ArrayList<>();
      QTableMetaData table = resolveTable(input.getOtherValues());
      if(table == null)
      {
         return (values);
      }
      for(QFieldMetaData field : table.getFields().values())
      {
         String label = field.getLabel() == null ? field.getName() : field.getLabel();
         QPossibleValue<String> value = new QPossibleValue<>(field.getName(), label + " (" + field.getName() + ")");
         if(CollectionUtils.nullSafeHasContents(input.getIdList()))
         {
            if(input.getIdList().stream().anyMatch(id -> value.getId().equals(ValueUtils.getValueAsString(id))))
            {
               values.add(value);
            }
         }
         else
         {
            String term = input.getSearchTerm();
            if(!StringUtils.hasContent(term) || value.getLabel().toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT)))
            {
               values.add(value);
            }
         }
      }
      return (values);
   }



   /*******************************************************************************
    ** Resolve the table from the frontend form values, never from the config table.
    *******************************************************************************/
   private QTableMetaData resolveTable(Map<String, Serializable> otherValues)
   {
      if(otherValues == null || QContext.getQInstance() == null)
      {
         return (null);
      }
      String tableName = ValueUtils.getValueAsString(otherValues.get("table_name"));
      return (StringUtils.hasContent(tableName) ? QContext.getQInstance().getTable(tableName) : null);
   }
}
