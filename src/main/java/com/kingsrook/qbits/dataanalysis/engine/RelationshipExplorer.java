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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.dataanalysis.result.ChartKind;
import com.kingsrook.qbits.dataanalysis.result.CorrelationCell;
import com.kingsrook.qbits.dataanalysis.result.CorrelationRequest;
import com.kingsrook.qbits.dataanalysis.result.CorrelationResult;
import com.kingsrook.qbits.dataanalysis.result.CrosstabCell;
import com.kingsrook.qbits.dataanalysis.result.CrosstabRequest;
import com.kingsrook.qbits.dataanalysis.result.CrosstabResult;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.PivotHeader;
import com.kingsrook.qbits.dataanalysis.result.Point;
import com.kingsrook.qbits.dataanalysis.result.ScatterRequest;
import com.kingsrook.qbits.dataanalysis.result.ScatterResult;
import com.kingsrook.qbits.dataanalysis.result.Series;
import com.kingsrook.qbits.dataanalysis.result.TrendLine;
import com.kingsrook.qbits.dataanalysis.result.ValueType;
import com.kingsrook.qbits.dataanalysis.stats.Statistics;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;

/*******************************************************************************
 ** Pairwise correlation, bounded scatter samples and guarded contingency tables.
 *******************************************************************************/
public class RelationshipExplorer extends RelationshipSupport
{
   /*******************************************************************************
    ** Each operation has its own execution state and policy snapshot.
    *******************************************************************************/
   public RelationshipExplorer(AnalysisSecurity security, EffectiveConfig config, String table)
   {
      super(security, config, table);
   }

   /*******************************************************************************
    ** All unique CORR pairs share one aggregate call, including streamed fallback.
    ** Pair counts use core CountAction because Aggregate has no per-measure filter.
    *******************************************************************************/
   public CorrelationResult correlation(CorrelationRequest request) throws QException
   {
      List<String> fields = request.getFields();
      if(fields == null || fields.size() < 2 || fields.size() > 12 || new LinkedHashSet<>(fields).size() != fields.size())
      {
         throw new QBadRequestException("Choose 2 to 12 distinct numeric fields.");
      }
      for(String field : fields)
      {
         requireNumeric(field);
      }
      List<Aggregate> aggregates = new ArrayList<>();
      for(int i = 0; i < fields.size(); i++)
      {
         for(int j = i; j < fields.size(); j++)
         {
            aggregates.add(measure(fields.get(i), AggregateOperator.CORR).withOtherFieldName(fields.get(j)));
         }
      }
      var values = summarize(request.getFilter(), aggregates).getAggregateValues();
      CorrelationResult result = new CorrelationResult().withFields(List.copyOf(fields));
      List<Series> series = new ArrayList<>();
      for(int i = 0; i < fields.size(); i++)
      {
         String x = fields.get(i);
         Series row = new Series().withName(x);
         for(int j = 0; j < fields.size(); j++)
         {
            String y = fields.get(j);
            Aggregate key = measure(fields.get(Math.min(i, j)), AggregateOperator.CORR).withOtherFieldName(fields.get(Math.max(i, j)));
            BigDecimal value = decimal(values.get(key));
            long n = size(paired(request.getFilter(), x, y));
            boolean hidden = n < config.getMinGroupSize();
            Double r = hidden || n < 2 || value == null ? null : value.doubleValue();
            result.getCells().add(new CorrelationCell().withXField(x).withYField(y).withN(hidden ? null : n).withR(r)
               .withScatter(hidden ? null : new ScatterRequest().withTableName(table).withXField(x).withYField(y).withFilter(DrillFilters.copy(request.getFilter()))));
            row.getPoints().add(new Point().withX(y).withY(r == null ? null : BigDecimal.valueOf(r)).withLabel(x + " / " + y));
         }
         series.add(row);
      }
      return (result.withChart(chart(ChartKind.HEATMAP, series, "Pearson r; each cell includes its paired non-null count and scatter request.")));
   }

   /*******************************************************************************
    ** QueryInput has no sampling API. Fetch the first N paired records by primary
    ** key as the user, with core-enforced record locks and a maximum of 2,000.
    *******************************************************************************/
   public ScatterResult scatter(ScatterRequest request) throws QException
   {
      requireNumeric(request.getXField());
      requireNumeric(request.getYField());
      if(request.getLimit() == null || request.getLimit() < 1 || request.getLimit() > 2000)
      {
         throw new QBadRequestException("Scatter limit must be from 1 to 2000.");
      }
      String x = request.getXField();
      String y = request.getYField();
      String pk = primaryKey(table);
      QQueryFilter filter = paired(request.getFilter(), x, y);
      long n = size(filter);
      ScatterResult result = new ScatterResult().withN(n).withSampling("First N paired records by primary key").withSampleSize(0);
      if(n < config.getMinGroupSize())
      {
         return (result.withN(null).withChart(chart(ChartKind.SCATTER, List.of(), "Hidden: fewer than " + config.getMinGroupSize() + " paired records.")));
      }
      Set<String> selected = new LinkedHashSet<>(List.of(x, y));
      boolean drill = config.getMinGroupSize() == 0 && security.analyzableFields(table).stream().anyMatch(f -> f.getName().equals(pk));
      if(drill)
      {
         selected.add(pk);
      }
      var records = rows(table, filter, selected, request.getLimit());
      double[] xs = new double[records.size()];
      double[] ys = new double[records.size()];
      for(int i = 0; i < records.size(); i++)
      {
         var record = records.get(i);
         BigDecimal xv = decimal(record.getValue(x));
         BigDecimal yv = decimal(record.getValue(y));
         xs[i] = xv.doubleValue();
         ys[i] = yv.doubleValue();
         result.getPoints().add(new Point().withX(xv).withY(yv).withDrill(drill ? DrillFilters.drill(table, null,
            DrillFilters.condition(pk, QCriteriaOperator.EQUALS, record.getValue(pk))) : null));
      }
      result.withSampleSize(records.size()).withTrend(trend(xs, ys));
      Series points = new Series().withName("Sample").withPoints(result.getPoints());
      Series line = new Series().withName("Least-squares trend");
      if(result.getTrend() != null)
      {
         double low = java.util.Arrays.stream(xs).min().orElseThrow();
         double high = java.util.Arrays.stream(xs).max().orElseThrow();
         for(double value : new double[] {low, high})
         {
            line.getPoints().add(new Point().withX(BigDecimal.valueOf(value)).withY(BigDecimal.valueOf(result.getTrend().getIntercept() + result.getTrend().getSlope() * value)));
         }
      }
      return (result.withChart(chart(ChartKind.SCATTER, List.of(points, line), "Trend describes the displayed sample, not the full population.")));
   }

   /*******************************************************************************
    ** Centered least squares remains stable when coordinates have large offsets.
    *******************************************************************************/
   private TrendLine trend(double[] x, double[] y)
   {
      if(x.length < 2)
      {
         return (null);
      }
      double meanX = java.util.Arrays.stream(x).average().orElseThrow();
      double meanY = java.util.Arrays.stream(y).average().orElseThrow();
      double xx = 0;
      double xy = 0;
      for(int i = 0; i < x.length; i++)
      {
         xx += (x[i] - meanX) * (x[i] - meanX);
         xy += (x[i] - meanX) * (y[i] - meanY);
      }
      return (xx == 0 ? null : new TrendLine().withSlope(xy / xx).withIntercept(meanY - xy / xx * meanX).withR(Statistics.pearson(x, y)));
   }

   /*******************************************************************************
    ** Cell counts include zero combinations. Suppression also removes shares
    ** that could reconstruct a hidden cell from a known marginal total.
    *******************************************************************************/
   public CrosstabResult crosstab(CrosstabRequest request) throws QException
   {
      full(request.getRowField());
      full(request.getColumnField());
      if(request.getRowField().equals(request.getColumnField()) || request.getLimit() == null || request.getLimit() < 1 || request.getLimit() > 20)
      {
         throw new QBadRequestException("Choose distinct crosstab fields and a limit from 1 to 20.");
      }
      List<Category> rows = categories(request.getRowField(), request.getFilter(), request.getLimit());
      List<Category> columns = categories(request.getColumnField(), request.getFilter(), request.getLimit());
      long[] rowTotals = new long[rows.size()];
      long[] columnTotals = new long[columns.size()];
      boolean[] hiddenRows = new boolean[rows.size()];
      boolean[] hiddenColumns = new boolean[columns.size()];
      double[][] counts = new double[rows.size()][columns.size()];
      boolean hidden = false;
      for(int i = 0; i < rows.size(); i++)
      {
         for(int j = 0; j < columns.size(); j++)
         {
            long n = size(DrillFilters.and(request.getFilter(), DrillFilters.and(rows.get(i).condition(), columns.get(j).condition())));
            counts[i][j] = n;
            rowTotals[i] += n;
            columnTotals[j] += n;
            if(n < config.getMinGroupSize())
            {
               hidden = true;
               hiddenRows[i] = true;
               hiddenColumns[j] = true;
            }
         }
      }
      CrosstabResult result = new CrosstabResult().withRows(rows.stream().map(c -> new PivotHeader().withValue(c.value()).withLabel(c.label())).toList())
         .withColumns(columns.stream().map(c -> new PivotHeader().withValue(c.value()).withLabel(c.label())).toList());
      for(int i = 0; i < rows.size(); i++)
      {
         for(int j = 0; j < columns.size(); j++)
         {
            long n = (long) counts[i][j];
            QQueryFilter condition = DrillFilters.and(rows.get(i).condition(), columns.get(j).condition());
            Metric count = metric("count", n, ValueType.COUNT, request.getFilter(), condition);
            Metric rowShare = metric("rowShare", ProfileSupport.share(n, rowTotals[i]), ValueType.PERCENT, request.getFilter(), condition);
            Metric columnShare = metric("columnShare", ProfileSupport.share(n, columnTotals[j]), ValueType.PERCENT, request.getFilter(), condition);
            List<Metric> metrics = List.of(count, rowShare, columnShare);
            if(rows.get(i).privateGroup() || columns.get(j).privateGroup())
            {
               metrics.forEach(m -> m.withDrill(null));
               hidden = true;
            }
            guard(metrics, n);
            if(hiddenRows[i])
            {
               guard(List.of(rowShare), 0);
            }
            if(hiddenColumns[j])
            {
               guard(List.of(columnShare), 0);
            }
            result.getCells().add(new CrosstabCell().withRowIndex(i).withColumnIndex(j).withCount(count).withRowShare(rowShare).withColumnShare(columnShare));
         }
      }
      return (result.withSignificance(significance(hidden ? Statistics.unavailable("Chi-square independence", "privacy guard") : Statistics.chiSquare(counts, false))));
   }
}
