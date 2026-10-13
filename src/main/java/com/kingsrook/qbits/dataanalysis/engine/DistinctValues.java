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

import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.dataanalysis.result.DistinctValuesPage;
import com.kingsrook.qbits.dataanalysis.result.DistinctValuesRequest;
import com.kingsrook.qbits.dataanalysis.result.ValueCount;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;

/*******************************************************************************
 ** Distinct-value pages apply privacy before pagination, never page rare groups.
 *******************************************************************************/
public class DistinctValues
{
   private final AnalysisBackend backend;

   /*******************************************************************************
    ** Bind primitive operations.
    *******************************************************************************/
   public DistinctValues(AnalysisBackend backend)
   {
      this.backend = backend;
   }



   /*******************************************************************************
    ** Request validation caps the page size at 500 and rejects overflow.
    *******************************************************************************/
   public DistinctValuesPage page(DistinctValuesRequest request, QFieldMetaData field, EffectiveConfig config) throws QException
   {
      if(request.getPage() == null || request.getPage() < 0 || request.getPageSize() == null || request.getPageSize() < 1 || request.getPageSize() > 500
         || (long) request.getPage() * request.getPageSize() > Integer.MAX_VALUE - 501)
      {
         throw new QException("Invalid distinct-values page; pageSize must be between 1 and 500.");
      }
      AnalysisBackend.Scope scope = backend.scope(request.getTableName(), request.getFilter(), true);
      int offset = request.getPage() * request.getPageSize();
      boolean wholeListing = config.getMinGroupSize() > 0 || request.getSearch() != null && !request.getSearch().isBlank();
      int fetch = wholeListing ? Math.max(1, config.getMaxRows()) : request.getPageSize() + 1;
      List<ValueCount> values = ProfileSupport.values(request.getTableName(), scope.filter(), field,
         backend.topValues(request.getTableName(), scope.filter(), field.getName(), fetch, wholeListing ? 0 : offset, request.getSearch()), scope.rowsScanned());
      long totalDistinct = wholeListing ? values.stream().filter(v -> v.getValue() != null).count()
         : backend.fieldCounts(request.getTableName(), scope.filter(), List.of(field.getName())).get(field.getName()).distinct();
      values = new MinGroupSizeGuard(config.getMinGroupSize()).groups(values);
      int start = wholeListing ? Math.min(offset, values.size()) : 0;
      boolean more = values.size() > start + request.getPageSize();
      return (new DistinctValuesPage().withTableName(request.getTableName()).withFieldName(field.getName()).withFilter(DrillFilters.copy(request.getFilter()))
         .withPage(request.getPage()).withPageSize(request.getPageSize()).withHasMore(more).withValues(values.subList(start, Math.min(values.size(), start + request.getPageSize())))
         .withTotalDistinct(totalDistinct)
         .withRowsScanned(scope.rowsScanned()).withTruncated(scope.truncated()).withAsOf(Instant.now()));
   }
}
