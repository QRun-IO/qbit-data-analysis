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


import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitConfig;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisFormatting;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RawHTML;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Analyses app home: real counts under the current user's permissions and locks.
 *******************************************************************************/
public class DataAnalysisHome extends AbstractWidgetRenderer
{
   public static final String HOME_WIDGET = "dataAnalysisHome";



   /*******************************************************************************
    ** The home itself has no data privilege; each section checks its table READ.
    *******************************************************************************/
   public static QWidgetMetaData widget()
   {
      return (new QWidgetMetaData().withName(HOME_WIDGET).withLabel("Overview").withType(WidgetType.HTML.getType())
         .withCodeReference(new QCodeReference(DataAnalysisHome.class)).withGridColumns(12).withIsCard(false)
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED)));
   }



   /*******************************************************************************
    ** Only main tables appear in navigation; schedules and stars are record children.
    *******************************************************************************/
   public static QAppMetaData app(DataAnalysisQBitConfig config, List<QTableMetaData> tables)
   {
      QAppMetaData app = new QAppMetaData().withName(config.getAppName()).withLabel(config.getAppLabel())
         .withIcon(new QIcon("insights")).withWidgets(List.of(HOME_WIDGET))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withSection(new QAppSection().withName("analyses").withLabel("Analyses").withIcon(new QIcon("insights")).withTables(DataAnalysisTables.NAV_TABLES));
      app.setParentAppName(config.getParentAppName());
      for(String name : DataAnalysisTables.NAV_TABLES)
      {
         tables.stream().filter(table -> name.equals(table.getName())).findFirst().ifPresent(app::withChild);
      }
      return (app);
   }



   /*******************************************************************************
    ** RawHTML is carried as a JSON string by the HTML widget, not a text/html response.
    ** Entities keep static separators independent of any host charset handling.
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      return (new RenderWidgetOutput(new RawHTML(null, render(Instant.now()))));
   }



   /*******************************************************************************
    ** Counts are evaluated at one instant; no system session or synthetic numbers.
    *******************************************************************************/
   public static String render(Instant now) throws QException
   {
      StringBuilder html = new StringBuilder("<div class=\"data-analysis-home\" style=\"display:grid;gap:1rem;min-width:0;overflow-wrap:anywhere\">");
      String userId = QContext.getQSession() == null || QContext.getQSession().getUser() == null ? null : QContext.getQSession().getUser().getIdReference();
      html.append("<section><h3>Saved analyses</h3>");
      if(userId == null)
      {
         html.append("<p>Sign in to see your saved analyses and stars.</p>");
      }
      else if(canRead(DataAnalysisTables.SAVED))
      {
         int mine = count(DataAnalysisTables.SAVED, equal("owner_user_id", userId));
         int shared = count(DataAnalysisTables.SAVED, equal("shared", true));
         html.append("<p>Mine: ").append(AnalysisFormatting.number(mine, 0)).append(" &middot; Shared (visible to you): ").append(AnalysisFormatting.number(shared, 0)).append("</p>");
         if(canRead(DataAnalysisTables.SAVED_STAR))
         {
            CountInput stars = new CountInput(DataAnalysisTables.SAVED).withIncludeDistinctCount(true)
               .withQueryJoin(new QueryJoin(QContext.getQInstance().getJoin(DataAnalysisTables.joinName(DataAnalysisTables.SAVED_STAR))).withType(QueryJoin.Type.INNER))
               .withFilter(equal(DataAnalysisTables.SAVED_STAR + ".user_id", userId));
            stars.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
            int starred = new CountAction().execute(stars).getDistinctCount();
            html.append("<p>Starred by me: ").append(AnalysisFormatting.number(starred, 0)).append("</p>");
         }
         if(mine == 0 && shared == 0)
         {
            html.append("<p>No saved analyses yet. Open any table and choose Analyze on a column header to explore its data.</p>");
         }
         link(html, DataAnalysisTables.SAVED);
      }
      else
      {
         html.append("<p>Saved analyses are unavailable: you need data_analysis.saved.read.</p>");
      }
      html.append("</section><section><h3>Watches</h3>");
      if(canRead(DataAnalysisTables.WATCH))
      {
         int total = count(DataAnalysisTables.WATCH, new QQueryFilter());
         int enabled = count(DataAnalysisTables.WATCH, equal("enabled", true));
         int firing = count(DataAnalysisTables.WATCH, equal("state", "FIRING"));
         html.append("<p>Total: ").append(AnalysisFormatting.number(total, 0)).append(" &middot; Enabled: ").append(AnalysisFormatting.number(enabled, 0)).append(" &middot; Firing: ").append(AnalysisFormatting.number(firing, 0)).append("</p>");
         if(total == 0)
         {
            html.append("<p>No watches yet.</p>");
         }
         link(html, DataAnalysisTables.WATCH);
      }
      else
      {
         html.append("<p>Watches are unavailable: you need data_analysis.watch.read.</p>");
      }
      html.append("</section><section><h3>Recent analysis runs</h3>");
      if(userId == null)
      {
         html.append("<p>Sign in to see your recent analysis runs.</p>");
      }
      else if(canRead(DataAnalysisTables.RUN))
      {
         QueryInput query = new QueryInput(DataAnalysisTables.RUN).withFilter(equal("user_id", userId)
            .withCriteria(new QFilterCriteria("started_at", QCriteriaOperator.LESS_THAN_OR_EQUALS, now))
            .withOrderBy(new QFilterOrderBy("started_at", false)).withOrderBy(new QFilterOrderBy("id", false)).withLimit(5));
         query.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         List<QRecord> runs = new QueryAction().execute(query).getRecords();
         if(runs.isEmpty())
         {
            html.append("<p>No analysis runs yet.</p>");
         }
         else
         {
            html.append("<ul class=\"data-analysis-runs\">");
            for(QRecord run : runs)
            {
               QTableMetaData table = QContext.getQInstance().getTable(run.getValueString("table_name"));
               String tableLabel = table == null ? run.getValueString("table_name") : table.getLabel();
               String path = QContext.getQInstance().getTablePath(DataAnalysisTables.RUN) + "/" + run.getValueInteger("id");
               html.append("<li><a href=\"").append(escape(path)).append("\">").append(escape(tableLabel)).append(" &middot; ")
                  .append(escape(words(run.getValueString("operation")))).append("</a><dl class=\"data-analysis-run-details\"><dt>Status</dt><dd>")
                  .append(escape(words(run.getValueString("status")))).append("</dd><dt>Finished</dt><dd>")
                  .append(run.getValue("finished_at") == null ? "Not finished" : escape(AnalysisFormatting.instant(run.getValueInstant("finished_at"))))
                  .append("</dd></dl></li>");
            }
            html.append("</ul>");
         }
         link(html, DataAnalysisTables.RUN);
      }
      else
      {
         html.append("<p>Run history is unavailable: you need data_analysis.run.read.</p>");
      }
      return (html.append("</section></div>").toString());
   }



   /*******************************************************************************
    ** Explicit permission checks avoid presenting denied sections as empty data.
    *******************************************************************************/
   private static boolean canRead(String table)
   {
      return (PermissionsHelper.hasTablePermission(new CountInput(table), table, TablePermissionSubType.READ));
   }



   /*******************************************************************************
    ** Equality filter for one count.
    *******************************************************************************/
   private static QQueryFilter equal(String field, java.io.Serializable value)
   {
      return (new QQueryFilter().withCriteria(new QFilterCriteria(field, QCriteriaOperator.EQUALS, value)));
   }



   /*******************************************************************************
    ** Stored operation and status keys are displayed as ordinary words.
    *******************************************************************************/
   private static String words(String value)
   {
      return (value == null ? "Unknown" : Arrays.stream(value.toLowerCase(Locale.ROOT).split("_"))
         .filter(part -> !part.isEmpty()).map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1)).collect(Collectors.joining(" ")));
   }



   /*******************************************************************************
    ** Metadata and persisted values must never become executable widget markup.
    *******************************************************************************/
   private static String escape(String value)
   {
      return (value == null ? "Unknown" : value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;"));
   }



   /*******************************************************************************
    ** CountAction enforces table permission and record locks using QContext's session.
    *******************************************************************************/
   private static int count(String table, QQueryFilter filter) throws QException
   {
      CountInput input = new CountInput(table).withFilter(filter);
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new CountAction().execute(input).getCount());
   }



   /*******************************************************************************
    ** Use the instance's actual app path, including custom names and parent apps.
    *******************************************************************************/
   private static void link(StringBuilder html, String table) throws QException
   {
      String path = QContext.getQInstance().getTablePath(table);
      html.append("<a href=\"").append(escape(path))
         .append("\">View all</a>");
   }
}
