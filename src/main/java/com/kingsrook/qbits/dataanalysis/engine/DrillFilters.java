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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import com.kingsrook.qbits.dataanalysis.result.Drill;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;

/*******************************************************************************
 ** Immutable composition of the request filter and the producing condition.
 *******************************************************************************/
public final class DrillFilters
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DrillFilters()
   {
   }



   /*******************************************************************************
    ** Copy criteria only; pagination and ordering are not analysis predicates.
    *******************************************************************************/
   public static QQueryFilter copy(QQueryFilter filter)
   {
      QQueryFilter result = filter == null ? new QQueryFilter() : filter.clone();
      result.setLimit(null);
      result.setSkip(null);
      result.setOrderBys(null);
      if(result.getSubFilters() != null)
      {
         result.setSubFilters(result.getSubFilters().stream().map(DrillFilters::copy).toList());
      }
      return (result);
   }



   /*******************************************************************************
    ** Preserve OR semantics of both operands by nesting them under AND.
    *******************************************************************************/
   public static QQueryFilter and(QQueryFilter original, QQueryFilter condition)
   {
      return (new QQueryFilter().withSubFilter(copy(original)).withSubFilter(copy(condition)));
   }



   /*******************************************************************************
    ** A drill always owns its filters.
    *******************************************************************************/
   public static Drill drill(String table, QQueryFilter original, QQueryFilter condition)
   {
      QQueryFilter filter = and(original, condition);
      var instance = com.kingsrook.qqq.backend.core.context.QContext.getQInstance();
      if(instance != null && "memory".equals(instance.getBackend(instance.getTable(table).getBackendName()).getBackendType()))
      {
         filter = memoryTemporalBounds(restoreTypes(table, filter));
      }
      return (new Drill().withTableName(table).withFilter(filter));
   }



   /*******************************************************************************
    ** Memory's inclusive lower bound uses Java equality, which differs after
    ** HTTP turns dates into strings. Strict lower bounds avoid that equality.
    ** Only memory uses this adjustment: SQL timestamp precision may be coarser.
    *******************************************************************************/
   private static QQueryFilter memoryTemporalBounds(QQueryFilter filter)
   {
      filter.setSubFilters(new java.util.ArrayList<>(filter.getSubFilters()));
      for(var criterion : new java.util.ArrayList<>(filter.getCriteria()))
      {
         if(criterion.getValues() != null && !criterion.getValues().isEmpty()
            && criterion.getValues().stream().allMatch(v -> v instanceof java.time.Instant || v instanceof LocalDate)
            && (criterion.getOperator() == QCriteriaOperator.EQUALS || criterion.getOperator() == QCriteriaOperator.IN))
         {
            QQueryFilter members = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR);
            for(Serializable value : criterion.getValues())
            {
               members.withSubFilter(range(criterion.getFieldName(), value, value, true));
            }
            filter.getCriteria().remove(criterion);
            filter.withSubFilter(members);
            continue;
         }
         if(criterion.getValues() != null && criterion.getValues().size() == 1)
         {
            Serializable value = criterion.getValues().getFirst();
            if(value instanceof java.time.Instant || value instanceof LocalDate)
            {
               Serializable previous = value instanceof java.time.Instant instant ? instant.minusNanos(1) : ((LocalDate) value).minusDays(1);
               if(criterion.getOperator() == QCriteriaOperator.GREATER_THAN_OR_EQUALS)
               {
                  criterion.setOperator(QCriteriaOperator.GREATER_THAN);
                  criterion.setValues(List.of(previous));
               }
               else if(criterion.getOperator() == QCriteriaOperator.LESS_THAN)
               {
                  criterion.setOperator(QCriteriaOperator.LESS_THAN_OR_EQUALS);
                  criterion.setValues(List.of(previous));
               }
            }
         }
      }
      filter.getSubFilters().forEach(DrillFilters::memoryTemporalBounds);
      return (filter);
   }

   /*******************************************************************************
    ** One criterion, including predicates without operands.
    *******************************************************************************/
   public static QQueryFilter condition(String field, QCriteriaOperator operator, Serializable... values)
   {
      return (new QQueryFilter().withCriteria(field, operator, values));
   }



   /*******************************************************************************
    ** RC.5 IS_BLANK includes empty strings. Applying it to StringLength
    ** distinguishes null from length zero on both SQL and memory backends.
    *******************************************************************************/
   public static QQueryFilter nulls(String field, boolean string)
   {
      QQueryFilter result = condition(field, QCriteriaOperator.IS_BLANK);
      if(string)
      {
         result.withCriteria(new com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria(field, QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, 0)
            .withFieldFunction(new com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction().withFieldName(field)
               .withFunctionTypeIdentifier(com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction.IDENTIFIER)));
      }
      return (result);
   }



   /*******************************************************************************
    ** Non-null includes an empty string, unlike core IS_NOT_BLANK.
    *******************************************************************************/
   public static QQueryFilter nonNull(String field, boolean string)
   {
      QQueryFilter result = condition(field, QCriteriaOperator.IS_NOT_BLANK);
      if(string)
      {
         result.withBooleanOperator(QQueryFilter.BooleanOperator.OR).withSubFilter(emptyString(field));
      }
      return (result);
   }



   /*******************************************************************************
    ** A group equality uses a null-only filter for a null group.
    *******************************************************************************/
   public static QQueryFilter value(String field, Serializable value, boolean string)
   {
      return (value == null ? nulls(field, string) : "".equals(value) ? emptyString(field)
         : value instanceof java.time.Instant || value instanceof LocalDate ? range(field, value, value, true) : condition(field, QCriteriaOperator.EQUALS, value));
   }



   /*******************************************************************************
    ** Half-open bounds, except that the final interior histogram bin is closed.
    ** Explicit non-null avoids RC.5 memory treating null as less than a value.
    *******************************************************************************/
   public static QQueryFilter range(String field, Serializable low, Serializable high, boolean closed)
   {
      QQueryFilter result = condition(field, QCriteriaOperator.IS_NOT_BLANK);
      if(low != null)
      {
         result.withCriteria(field, QCriteriaOperator.GREATER_THAN_OR_EQUALS, low);
      }
      if(high != null)
      {
         result.withCriteria(field, closed ? QCriteriaOperator.LESS_THAN_OR_EQUALS : QCriteriaOperator.LESS_THAN, high);
      }
      return (result);
   }



   /*******************************************************************************
    ** Local midnight boundaries preserve DST and the user's time zone.
    *******************************************************************************/
   public static QQueryFilter dateRange(String field, LocalDate low, LocalDate high, boolean dateTime, ZoneId zone)
   {
      return (range(field, dateTime ? low.atStartOfDay(zone).toInstant() : low,
         dateTime ? high.atStartOfDay(zone).toInstant() : high, false));
   }



   /*******************************************************************************
    ** Strict Tukey fences, excluding nulls on every backend.
    *******************************************************************************/
   public static QQueryFilter outliers(String field, Serializable low, Serializable high)
   {
      QQueryFilter outside = condition(field, QCriteriaOperator.LESS_THAN, low)
         .withBooleanOperator(QQueryFilter.BooleanOperator.OR).withCriteria(field, QCriteriaOperator.GREATER_THAN, high);
      return (and(condition(field, QCriteriaOperator.IS_NOT_BLANK), outside));
   }



   /*******************************************************************************
    ** Exact membership for streamed shapes, lengths and duplicate tuples.
    *******************************************************************************/
   public static QQueryFilter in(String field, List<? extends Serializable> values)
   {
      if(values.stream().anyMatch(v -> v instanceof java.time.Instant || v instanceof LocalDate))
      {
         QQueryFilter result = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR);
         values.forEach(value -> result.withSubFilter(range(field, value, value, true)));
         return (result);
      }
      if(values.contains(""))
      {
         List<? extends Serializable> nonEmpty = values.stream().filter(v -> !"".equals(v)).toList();
         return (new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withSubFilter(emptyString(field)).withSubFilter(in(field, nonEmpty)));
      }
      return (values.isEmpty() ? condition(field, QCriteriaOperator.FALSE) : new QQueryFilter().withCriteria(field, QCriteriaOperator.IN, values));
   }



   /*******************************************************************************
    ** Core HTTP interpretation treats empty string operands as missing inputs.
    ** A zero-length predicate expresses the same set without an empty operand.
    *******************************************************************************/
   private static QQueryFilter emptyString(String field)
   {
      return (new QQueryFilter().withCriteria(new com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria(field, QCriteriaOperator.EQUALS, 0)
         .withFieldFunction(new com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction().withFieldName(field)
            .withFunctionTypeIdentifier(com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction.IDENTIFIER))));
   }

   /*******************************************************************************
    ** JSON has no temporal scalar types. Restore them from current metadata before
    ** a cached drill is handed back to core, without mutating the stored filter.
    *******************************************************************************/
   public static QQueryFilter restoreTypes(String table, QQueryFilter filter)
   {
      QQueryFilter result = copy(filter);
      var instance = com.kingsrook.qqq.backend.core.context.QContext.getQInstance();
      if(instance == null || instance.getTable(table) == null)
      {
         return (result);
      }
      if(result.getCriteria() != null)
      {
         for(var criterion : result.getCriteria())
         {
            var field = instance.getTable(table).getFields().get(criterion.getFieldName());
            if(field != null && criterion.getValues() != null)
            {
               var type = field.getType();
               if(type == com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType.DATE
                  || type == com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType.DATE_TIME
                  || type == com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType.DECIMAL
                  || criterion.getOperator() == QCriteriaOperator.EQUALS || criterion.getOperator() == QCriteriaOperator.IN)
               {
                  criterion.setValues(criterion.getValues().stream().map(v -> restoreValue(type, v)).toList());
               }
            }
         }
      }
      if(result.getSubFilters() != null)
      {
         result.setSubFilters(result.getSubFilters().stream().map(f -> restoreTypes(table, f)).toList());
      }
      return (result);
   }



   /*******************************************************************************
    ** String decimal operands preserve exact scale and precision across JSON round trips.
    *******************************************************************************/
   private static Serializable restoreValue(com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType type, Serializable value)
   {
      if(type == com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType.DECIMAL)
      {
         return (value == null ? null : value.toString());
      }
      return (com.kingsrook.qqq.backend.core.utils.ValueUtils.getValueAsFieldType(type, value));
   }
}
