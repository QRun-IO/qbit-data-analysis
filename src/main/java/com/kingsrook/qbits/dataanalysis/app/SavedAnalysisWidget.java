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

package com.kingsrook.qbits.dataanalysis.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qbits.dataanalysis.DataAnalysisRuntime;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.result.Chart;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.saved.AnalysisExport;
import com.kingsrook.qbits.dataanalysis.watch.WatchRecords;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChartSubheaderData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;

/*******************************************************************************
 ** Saved analysis rendered through Next's existing standard chart widget.
 *******************************************************************************/
public class SavedAnalysisWidget extends AbstractWidgetRenderer
{
   public static final String NAME = "DATA_ANALYSIS_SAVED";

   /*******************************************************************************
    ** Hosts clone this metadata and set savedId to place an analysis on a dashboard.
    *******************************************************************************/
   public static QWidgetMetaData widget()
   {
      return (new QWidgetMetaData().withName(NAME).withLabel("Saved analysis").withType(WidgetType.BAR_CHART.getType())
         .withGridColumns(12).withIsCard(true).withCodeReference(new QCodeReference(SavedAnalysisWidget.class))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED)));
   }

   /*******************************************************************************
    ** Values come from a fresh viewer run; no stored owner result is ever reused.
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      var defaults = ((QWidgetMetaData) input.getWidgetMetaData()).getDefaultValues();
      Object value = defaults == null ? null : defaults.get("savedId");
      if(value == null)
      {
         return (new RenderWidgetOutput(new ChartData("Saved analysis", "Choose a saved analysis for this widget.", "Analysis", List.of(), List.of())));
      }
      Integer id;
      try
      {
         id = Integer.valueOf(value.toString());
      }
      catch(NumberFormatException e)
      {
         throw new QBadRequestException("savedId must be an integer.");
      }
      var saved = WatchRecords.get(DataAnalysisTables.SAVED, id);
      var result = DataAnalysisRuntime.saved().run(id);
      List<Metric> metrics = AnalysisExport.headlines(result);
      Chart chart = null;
      if(result.columnProfile() != null)
      {
         chart = result.columnProfile().getChart();
      }
      else if(result.tableProfile() != null)
      {
         chart = result.tableProfile().getHeatmap();
      }
      else if(result.correlation() != null)
      {
         chart = result.correlation().getChart();
      }
      else if(result.shift() != null)
      {
         chart = result.shift().getChart();
      }
      else if(result.compare() != null && !result.compare().getFields().isEmpty())
      {
         chart = result.compare().getFields().getFirst().getChart();
      }
      String description = metrics.stream().filter(Objects::nonNull).map(m -> m.getLabel() + ": " + AnalysisExport.value(m))
         .collect(java.util.stream.Collectors.joining("; "));
      if(!result.warnings().isEmpty())
      {
         description += " " + String.join("; ", result.warnings());
      }
      ChartData data = new ChartData().withTitle(saved.getValueString("name")).withDescription(description).withHeight(280);
      String path = QContext.getQInstance().getTablePath(DataAnalysisTables.SAVED) + "/" + id;
      Number mainNumber = metrics.stream().filter(m -> !Boolean.TRUE.equals(m.getSuppressed()) && m.getValue() instanceof Number)
         .map(m -> (Number) m.getValue()).findFirst().orElse(null);
      data.withChartSubheaderData(new ChartSubheaderData().withMainNumber(mainNumber).withMainNumberUrl(path));
      List<String> labels = new ArrayList<>();
      List<ChartData.Data.Dataset> datasets = new ArrayList<>();
      if(chart != null && !chart.getSeries().isEmpty())
      {
         chart.getSeries().getFirst().getPoints().forEach(p -> labels.add(Objects.toString(p.getLabel(), Objects.toString(p.getX(), ""))));
         for(var series : chart.getSeries())
         {
            datasets.add(new ChartData.Data.Dataset().withLabel(series.getName())
               .withData(series.getPoints().stream().map(p -> (Number) p.getY()).toList()));
         }
      }
      else if(result.pivot() != null)
      {
         List<Number> numbers = new ArrayList<>();
         for(var cell : result.pivot().getCells())
         {
            labels.add(java.util.stream.Stream.concat(result.pivot().getRowHeaders().get(cell.getRowIndex()).stream(),
               result.pivot().getColumnHeaders().get(cell.getColumnIndex()).stream()).map(p -> p.getLabel()).collect(java.util.stream.Collectors.joining(" / ")));
            Metric metric = cell.getValues().getFirst();
            numbers.add(!Boolean.TRUE.equals(metric.getSuppressed()) && metric.getValue() instanceof Number n ? n : null);
         }
         datasets.add(new ChartData.Data.Dataset().withLabel(result.pivot().getMeasures().getFirst().getLabel()).withData(numbers));
      }
      else if(result.crosstab() != null)
      {
         List<Number> numbers = new ArrayList<>();
         for(var cell : result.crosstab().getCells())
         {
            labels.add(result.crosstab().getRows().get(cell.getRowIndex()).getLabel() + " / " + result.crosstab().getColumns().get(cell.getColumnIndex()).getLabel());
            Metric metric = cell.getCount();
            numbers.add(!Boolean.TRUE.equals(metric.getSuppressed()) && metric.getValue() instanceof Number n ? n : null);
         }
         datasets.add(new ChartData.Data.Dataset().withLabel("Count").withData(numbers));
      }
      data.withChartData(new ChartData.Data().withLabels(labels).withUrls(labels.stream().map(label -> path).toList()).withDatasets(datasets));
      data.withFooterHTML("<a href=\"" + path.replace("&", "&amp;").replace("\"", "&quot;") + "\">View saved analysis</a>");
      return (new RenderWidgetOutput(data));
   }
}
