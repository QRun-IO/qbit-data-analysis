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
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;

/*******************************************************************************
 ** Primitive port. All operations use the current session with ENFORCE.
 *******************************************************************************/
public interface AnalysisBackend
{
   /*******************************************************************************
    ** Date units use the session zone and ISO Monday weeks.
    *******************************************************************************/
   enum DateUnit
   {
      DAY, WEEK, MONTH, YEAR, DAY_OF_WEEK, HOUR_OF_DAY
   }



   /*******************************************************************************
    ** Actual row coverage and deterministic selected population.
    *******************************************************************************/
   record Scope(QQueryFilter filter, long total, long rowsScanned, boolean truncated, boolean estimate)
   {
   }



   /*******************************************************************************
    ** Blank includes null and whitespace; distinct excludes null.
    *******************************************************************************/
   record FieldCounts(long nonNull, long nulls, long blanks, long distinct, QQueryFilter blankFilter, QQueryFilter distinctFilter)
   {
   }



   /*******************************************************************************
    ** Group and exact predicate producing its members.
    *******************************************************************************/
   record GroupCount(Serializable value, String label, long count, QQueryFilter condition)
   {
   }



   /*******************************************************************************
    ** Numeric moments, percentile_cont and sample standard deviation.
    *******************************************************************************/
   record NumericSummary(Map<String, BigDecimal> metrics, List<GroupCount> outliers, long outlierCount, QQueryFilter outlierFilter, List<GroupCount> counts)
   {
   }



   /*******************************************************************************
    ** Text lengths exclude null but include empty strings.
    *******************************************************************************/
   record LengthSummary(Map<String, BigDecimal> metrics, List<GroupCount> histogram)
   {
   }



   /*******************************************************************************
    ** Exact filtered count.
    *******************************************************************************/
   long count(String table, QQueryFilter filter) throws QException;

   /*******************************************************************************
    ** Select the bounded population used consistently by all operations.
    *******************************************************************************/
   Scope scope(String table, QQueryFilter filter, boolean exact) throws QException;

   /*******************************************************************************
    ** Batch field counts in groups of twenty.
    *******************************************************************************/
   Map<String, FieldCounts> fieldCounts(String table, QQueryFilter filter, List<String> fields) throws QException;

   /*******************************************************************************
    ** Ordered groups with search on the stored value.
    *******************************************************************************/
   List<GroupCount> topValues(String table, QQueryFilter filter, String field, int limit, int offset, String search) throws QException;

   /*******************************************************************************
    ** Numeric summary and ten extreme-value groups.
    *******************************************************************************/
   NumericSummary numericSummary(String table, QQueryFilter filter, String field, List<BigDecimal> percentiles) throws QException;

   /*******************************************************************************
    ** Half-open interior bins, closed final bin, and exterior bins.
    *******************************************************************************/
   List<GroupCount> numericHistogram(String table, QQueryFilter filter, String field, BigDecimal low, BigDecimal high, int bins) throws QException;

   /*******************************************************************************
    ** Date buckets include interior zero intervals.
    *******************************************************************************/
   List<GroupCount> dateBuckets(String table, QQueryFilter filter, String field, DateUnit unit, ZoneId zone) throws QException;

   /*******************************************************************************
    ** Typed minimum and maximum.
    *******************************************************************************/
   Map<String, Serializable> extrema(String table, QQueryFilter filter, String field) throws QException;

   /*******************************************************************************
    ** Length moments and histogram.
    *******************************************************************************/
   LengthSummary lengthSummary(String table, QQueryFilter filter, String field) throws QException;

   /*******************************************************************************
    ** Top shapes over a bounded primary-key prefix.
    *******************************************************************************/
   List<GroupCount> patterns(String table, QQueryFilter filter, String field, int sampleRows) throws QException;

   /*******************************************************************************
    ** Top groups with count greater than one.
    *******************************************************************************/
   List<GroupCount> duplicates(String table, QQueryFilter filter, List<String> fields, int limit) throws QException;
}
