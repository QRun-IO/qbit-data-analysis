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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dataanalysis.result.CandidateKey;
import com.kingsrook.qbits.dataanalysis.result.ChartKind;
import com.kingsrook.qbits.dataanalysis.result.FieldSummary;
import com.kingsrook.qbits.dataanalysis.result.Point;
import com.kingsrook.qbits.dataanalysis.result.TableProfile;
import com.kingsrook.qbits.dataanalysis.result.TableProfileRequest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;

/*******************************************************************************
 ** Batched table completeness and candidate-key discovery over the selected scope.
 *******************************************************************************/
public class TableProfiler
{
   private final AnalysisBackend backend;

   /*******************************************************************************
    ** Bind the primitive operation port.
    *******************************************************************************/
   public TableProfiler(AnalysisBackend backend)
   {
      this.backend = backend;
   }



   /*******************************************************************************
    ** Candidate pairs consider the eight most unique readable selected fields.
    *******************************************************************************/
   public TableProfile profile(TableProfileRequest request, List<QFieldMetaData> fields, AnalysisBackend.Scope scope) throws QException
   {
      String table = request.getTableName();
      QQueryFilter filter = scope.filter();
      long rows = backend.count(table, filter);
      TableProfile result = new TableProfile().withTableName(table).withFilter(DrillFilters.restoreTypes(request.getTableName(), request.getFilter())).withEstimate(scope.estimate())
         .withSampleRows(scope.estimate() ? rows : 0L).withRowsScanned(scope.rowsScanned()).withTruncated(scope.truncated()).withAsOf(Instant.now())
         .withRowCount(ProfileSupport.count("rows", rows, table, filter, null));
      Map<String, AnalysisBackend.FieldCounts> counts = backend.fieldCounts(table, filter, fields.stream().map(QFieldMetaData::getName).toList());
      for(QFieldMetaData field : fields)
      {
         String name = field.getName();
         AnalysisBackend.FieldCounts c = counts.get(name);
         result.getFields().add(new FieldSummary().withField(name).withLabel(field.getLabel()).withType(field.getType().name())
            .withCompleteness(ProfileSupport.share(rows - c.blanks(), rows)).withDistinct(c.distinct()).withUniqueness(ProfileSupport.share(c.distinct(), c.nonNull()))
            .withConstant(c.distinct() == 1).withEmpty(c.blanks() == rows)
            .withNulls(ProfileSupport.count("nulls", c.nulls(), table, filter, DrillFilters.nulls(name, field.getType().isStringLike())))
            .withBlanks(ProfileSupport.count("blanks", c.blanks(), table, filter, c.blankFilter())));
         if(rows > 0 && c.distinct() == rows && c.nulls() == 0)
         {
            result.getCandidateKeys().add(key(table, filter, List.of(name), rows));
         }
      }
      List<FieldSummary> ranked = result.getFields().stream().sorted(Comparator.comparing(FieldSummary::getUniqueness).reversed().thenComparing(FieldSummary::getField)).limit(8).toList();
      for(int i = 0; i < ranked.size(); i++)
      {
         for(int j = i + 1; j < ranked.size(); j++)
         {
            List<String> pair = List.of(ranked.get(i).getField(), ranked.get(j).getField());
            if(rows > 0 && pair.stream().allMatch(f -> counts.get(f).nulls() == 0) && backend.duplicates(table, filter, pair, 1).isEmpty())
            {
               result.getCandidateKeys().add(key(table, filter, pair, rows));
            }
         }
      }
      result.withHeatmap(ProfileSupport.chart(ChartKind.HEATMAP, ChartKind.BAR, "field", List.of()));
      result.getHeatmap().getY().withLabel("Completeness").withValueType(com.kingsrook.qbits.dataanalysis.result.ValueType.PERCENT);
      result.getHeatmap().getSeries().getFirst().withName("Completeness").withPoints(new ArrayList<>());
      for(FieldSummary field : result.getFields())
      {
         result.getHeatmap().getSeries().getFirst().getPoints().add(new Point().withX(field.getField()).withLabel(field.getLabel()).withY(field.getCompleteness()));
      }
      if(scope.estimate() || scope.truncated())
      {
         result.getWarnings().add("Profiles and candidate keys describe only the first " + rows + " rows by primary key.");
      }
      return (result);
   }



   /*******************************************************************************
    ** Candidate-key counts drill to the whole assessed population.
    *******************************************************************************/
   private static CandidateKey key(String table, QQueryFilter filter, List<String> fields, long rows)
   {
      return (new CandidateKey().withFields(fields).withRowCount(rows).withDrill(DrillFilters.drill(table, filter, null)));
   }
}
