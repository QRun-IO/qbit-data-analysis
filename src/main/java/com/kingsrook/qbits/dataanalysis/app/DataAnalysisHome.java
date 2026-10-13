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
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitConfig;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RawHTML;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
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
    ** Standard QQQ widget renderer entry point.
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
      html.append("<section><h2>Saved analyses</h2>");
      if(userId == null)
      {
         html.append("<p>Sign in to see your saved analyses and stars.</p>");
      }
      else if(canRead(DataAnalysisTables.SAVED))
      {
         int mine = count(DataAnalysisTables.SAVED, equal("owner_user_id", userId));
         int shared = count(DataAnalysisTables.SAVED, equal("shared", true));
         html.append("<p>Mine: ").append(mine).append(" Â· Shared (visible to you): ").append(shared).append("</p>");
         if(canRead(DataAnalysisTables.SAVED_STAR))
         {
            CountInput stars = new CountInput(DataAnalysisTables.SAVED).withIncludeDistinctCount(true)
               .withQueryJoin(new QueryJoin(QContext.getQInstance().getJoin(DataAnalysisTables.joinName(DataAnalysisTables.SAVED_STAR))).withType(QueryJoin.Type.INNER))
               .withFilter(equal(DataAnalysisTables.SAVED_STAR + ".user_id", userId));
            stars.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
            int starred = new CountAction().execute(stars).getDistinctCount();
            html.append("<p>Starred by me: ").append(starred).append("</p>");
         }
         if(mine == 0 && shared == 0)
         {
            html.append("<p>No saved analyses yet. Saving analyses from the Analyze action is planned for a later release.</p>");
         }
         html.append("<p>The Analyze action will be available in a later UI release.</p>");
         link(html, DataAnalysisTables.SAVED);
      }
      else
      {
         html.append("<p>Saved analyses are unavailable: you need data_analysis.saved.read.</p>");
      }
      html.append("</section><section><h2>Watches</h2>");
      if(canRead(DataAnalysisTables.WATCH))
      {
         int enabled = count(DataAnalysisTables.WATCH, equal("enabled", true));
         int firing = count(DataAnalysisTables.WATCH, equal("state", "FIRING"));
         html.append("<p>Enabled: ").append(enabled).append(" Â· Firing: ").append(firing).append("</p>");
         if(enabled == 0 && firing == 0)
         {
            html.append("<p>No active watches: watch evaluation is planned for phase e.</p>");
         }
         link(html, DataAnalysisTables.WATCH);
      }
      else
      {
         html.append("<p>Watches are unavailable: you need data_analysis.watch.read.</p>");
      }
      html.append("</section><section><h2>Runs in the last 7 days</h2>");
      if(canRead(DataAnalysisTables.RUN))
      {
         int complete = count(DataAnalysisTables.RUN, recentRuns("COMPLETE", now));
         int failed = count(DataAnalysisTables.RUN, recentRuns("FAILED", now));
         html.append("<p>Complete: ").append(complete).append(" Â· Failed: ").append(failed).append("</p>");
         if(complete == 0 && failed == 0)
         {
            html.append("<p>No completed or failed runs in this period.</p>");
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
    ** Runs started within the inclusive seven-day interval, excluding future rows.
    *******************************************************************************/
   private static QQueryFilter recentRuns(String status, Instant now)
   {
      return (equal("status", status)
         .withCriteria(new QFilterCriteria("started_at", QCriteriaOperator.GREATER_THAN_OR_EQUALS, now.minus(7, ChronoUnit.DAYS)))
         .withCriteria(new QFilterCriteria("started_at", QCriteriaOperator.LESS_THAN_OR_EQUALS, now)));
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
      html.append("<a href=\"").append(path.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;"))
         .append("\">View all</a>");
   }
}
