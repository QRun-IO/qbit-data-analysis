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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitConfig;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitProducer;
import com.kingsrook.qbits.dataanalysis.DataAnalysisRuntime;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.permissions.DataAnalysisPermissions;
import com.kingsrook.qbits.dataanalysis.saved.SavedAnalyses;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.permissions.UseOtherPermissionNameCustomPermissionChecker;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiStatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;

/*******************************************************************************
 ** Standard QQQ statistics and narrow table widgets, all queried as the viewer.
 *******************************************************************************/
public class DataAnalysisHome extends AbstractWidgetRenderer
{
   public static final String SETUP = "dataAnalysisSetup";
   public static final String HOME_WIDGET = "dataAnalysisHome";
   public static final String STARRED = "dataAnalysisStarred";
   public static final String FIRING = "dataAnalysisFiring";
   public static final String RECENT = "dataAnalysisRecent";

   /*******************************************************************************
    ** The existing home name now supplies standard multi-statistics data.
    *******************************************************************************/
   public static QWidgetMetaData widget()
   {
      return (widget(HOME_WIDGET, "Analysis overview", WidgetType.MULTI_STATISTICS));
   }

   /*******************************************************************************
    ** Full-width tables have three or four columns and honest empty states.
    *******************************************************************************/
   public static List<QWidgetMetaData> widgets()
   {
      return (List.of(setupWidget(), widget().withPermissionRules(readRules(DataAnalysisTables.SAVED)),
         widget(STARRED, "Starred analyses", WidgetType.TABLE).withPermissionRules(readRules(DataAnalysisTables.SAVED)),
         widget(FIRING, "Firing watches", WidgetType.TABLE).withPermissionRules(readRules(DataAnalysisTables.WATCH)),
         widget(RECENT, "My recent runs", WidgetType.TABLE).withPermissionRules(readRules(DataAnalysisTables.RUN))));
   }

   /*******************************************************************************
    ** Names of the widgets placed on the business home. Setup is system content and
    ** is deliberately absent; hosts place {@link DataAnalysisQBitProducer#SYSTEM_WIDGETS}
    ** on their Admin area instead.
    *******************************************************************************/
   public static List<String> homeWidgetNames()
   {
      return (widgets().stream().map(QWidgetMetaData::getName).filter(name -> !DataAnalysisQBitProducer.SYSTEM_WIDGETS.contains(name)).toList());
   }

   /*******************************************************************************
    ** The Setup card shows engine configuration, so only Data Analysis admins get it;
    ** everyone else has it omitted from metadata.
    *******************************************************************************/
   private static QWidgetMetaData setupWidget()
   {
      return (widget(SETUP, "Setup", WidgetType.TABLE).withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
         .withDenyBehavior(DenyBehavior.HIDDEN).withCustomPermissionChecker(UseOtherPermissionNameCustomPermissionChecker.build(DataAnalysisPermissions.ADMIN))));
   }

   /*******************************************************************************
    ** A data widget is omitted from metadata for users who cannot read its table.
    *******************************************************************************/
   private static QPermissionRules readRules(String table)
   {
      return (new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION).withDenyBehavior(DenyBehavior.HIDDEN)
         .withCustomPermissionChecker(TableReadWidgetPermissionChecker.build(table)));
   }

   /*******************************************************************************
    ** The widget itself grants no privilege; each data section checks table READ.
    *******************************************************************************/
   private static QWidgetMetaData widget(String name, String label, WidgetType type)
   {
      return (new QWidgetMetaData().withName(name).withLabel(label).withType(type.getType()).withGridColumns(12).withIsCard(true)
         .withShowReloadButton(true).withCodeReference(new QCodeReference(DataAnalysisHome.class))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED)));
   }

   /*******************************************************************************
    ** Main tables remain navigation; schedules and stars remain child lists.
    *******************************************************************************/
   public static QAppMetaData app(DataAnalysisQBitConfig config, List<QTableMetaData> tables)
   {
      QAppMetaData app = new QAppMetaData().withName(config.getAppName()).withLabel(config.getAppLabel())
         .withIcon(new QIcon("insights")).withWidgets(homeWidgetNames())
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
    ** Dispatch data by registered metadata name, never by an arbitrary table input.
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      return (new RenderWidgetOutput(render(input.getWidgetMetaData().getName(), Instant.now())));
   }

   /*******************************************************************************
    ** Counts and table rows share core-enforced permissions and record locks.
    *******************************************************************************/
   public static QWidgetData render(String name, Instant now) throws QException
   {
      if(SETUP.equals(name))
      {
         return (setup());
      }
      if(HOME_WIDGET.equals(name))
      {
         return (statistics(now));
      }
      if(STARRED.equals(name))
      {
         TableData data = table("Starred analyses", DataAnalysisTables.SAVED, "No starred analyses yet. Star a saved analysis to see it here.", "Name", "Table", "Kind");
         if(canRead(DataAnalysisTables.SAVED))
         {
            for(var entry : DataAnalysisRuntime.saved().list().stream().filter(SavedAnalyses.Entry::starred).toList())
            {
               data.getRows().add(values(link(DataAnalysisTables.SAVED, entry.id(), entry.name()), entry.tableLabel(), entry.kind()));
            }
         }
         return (data);
      }
      String source = FIRING.equals(name) ? DataAnalysisTables.WATCH : DataAnalysisTables.RUN;
      TableData data = FIRING.equals(name)
         ? table("Firing watches", source, "No firing watches.", "Watch", "Table", "Last evaluated", "Message")
         : table("My recent runs", source, "No analysis runs yet.", "Analysis", "Table", "Status", "Started");
      if(!canRead(source))
      {
         return (data);
      }
      QQueryFilter filter = FIRING.equals(name) ? equal("state", "FIRING")
         : equal("user_id", SavedAnalyses.user()).withCriteria("started_at", QCriteriaOperator.LESS_THAN_OR_EQUALS, now);
      filter.withOrderBy(new QFilterOrderBy(FIRING.equals(name) ? "last_evaluated_at" : "started_at", false)).withLimit(10);
      for(var row : SavedAnalyses.query(source, filter))
      {
         String tableName = row.getValueString("table_name");
         var metadata = QContext.getQInstance().getTable(tableName);
         String label = metadata == null ? Objects.toString(tableName, "Saved analysis") : metadata.getLabel();
         data.getRows().add(FIRING.equals(name)
            ? values(link(source, row.getValue("id"), row.getValueString("detector")), label, row.getValue("last_evaluated_at"), row.getValue("last_message"))
            : values(link(source, row.getValue("id"), row.getValueString("operation")), label, row.getValue("status"), row.getValue("started_at")));
      }
      return (data);
   }

   /*******************************************************************************
    ** The Setup card: each settings or content table, its state and the next step.
    *******************************************************************************/
   public static TableData setup() throws QException
   {
      TableData data = new TableData("Setup", List.of(new TableData.Column("html", "Part", "c0", null, "left"), new TableData.Column("text", "State", "c1", null, "left"),
         new TableData.Column("text", "What it means", "c2", null, "left"), new TableData.Column("text", "Next step", "c3", null, "left")), new ArrayList<>())
         .withNoRowsFoundHTML("Nothing to set up.").withRowsPerPage(10);
      for(DataAnalysisSetupStatus.Item item : DataAnalysisSetupStatus.load())
      {
         String path = QContext.getQInstance().getTablePath(item.tableName());
         String part = "<a href=\"" + escape(path) + "\">" + escape(item.label()) + "</a>";
         String state = switch(item.state())
         {
            case OK -> "Set up";
            case DEFAULT -> "Using defaults";
            case OPTIONAL -> "Optional";
            case UNAVAILABLE -> "Unavailable";
            case UNKNOWN -> "Needs attention";
         };
         data.getRows().add(values(part, state, item.detail(), item.nextStep()));
      }
      return (data);
   }

   /*******************************************************************************
    ** One standard group per subject the viewer can read; unreadable subjects are left out.
    *******************************************************************************/
   public static MultiStatisticsData statistics(Instant now) throws QException
   {
      List<MultiStatisticsData.StatisticsGroupData> groups = new ArrayList<>();
      if(canRead(DataAnalysisTables.SAVED))
      {
         var saved = group("Saved analyses");
         List<SavedAnalyses.Entry> entries = DataAnalysisRuntime.saved().list();
         String user = SavedAnalyses.user();
         saved.withStatisticList(List.of(stat("Mine", (int) entries.stream().filter(e -> e.owner().equals(user)).count(), DataAnalysisTables.SAVED),
            stat("Shared", (int) entries.stream().filter(e -> Boolean.TRUE.equals(e.shared())).count(), DataAnalysisTables.SAVED),
            stat("Starred", (int) entries.stream().filter(SavedAnalyses.Entry::starred).count(), DataAnalysisTables.SAVED)));
         groups.add(saved);
      }
      if(canRead(DataAnalysisTables.WATCH))
      {
         var watches = group("Watches");
         watches.withStatisticList(List.of(stat("Enabled", count(DataAnalysisTables.WATCH, equal("enabled", true)), DataAnalysisTables.WATCH),
            stat("Firing", count(DataAnalysisTables.WATCH, equal("state", "FIRING")), DataAnalysisTables.WATCH)));
         groups.add(watches);
      }
      if(canRead(DataAnalysisTables.RUN))
      {
         var runs = group("My runs");
         runs.withStatisticList(List.of(stat("Last 7 days", count(DataAnalysisTables.RUN, equal("user_id", SavedAnalyses.user())
            .withCriteria("started_at", QCriteriaOperator.GREATER_THAN_OR_EQUALS, now.minus(7, ChronoUnit.DAYS))
            .withCriteria("started_at", QCriteriaOperator.LESS_THAN_OR_EQUALS, now)), DataAnalysisTables.RUN)));
         groups.add(runs);
      }
      return (new MultiStatisticsData("Analyses", groups));
   }

   /*******************************************************************************
    ** A group is only built for a readable subject, so its subheader is always true.
    *******************************************************************************/
   private static MultiStatisticsData.StatisticsGroupData group(String label)
   {
      return (new MultiStatisticsData.StatisticsGroupData().withHeader(label).withStatisticList(List.of()).withSubheader("Visible to you"));
   }

   /*******************************************************************************
    ** Link every count to its actual table path.
    *******************************************************************************/
   private static MultiStatisticsData.StatisticsGroupData.Statistic stat(String label, int count, String table) throws QException
   {
      return (new MultiStatisticsData.StatisticsGroupData.Statistic(label, count, QContext.getQInstance().getTablePath(table)));
   }

   /*******************************************************************************
    ** Standard table cells support escaped record links; other columns are plain text.
    *******************************************************************************/
   private static TableData table(String label, String table, String empty, String... headers) throws QException
   {
      List<TableData.Column> columns = new ArrayList<>();
      for(int i = 0; i < headers.length; i++)
      {
         columns.add(new TableData.Column(i == 0 ? "html" : "text", headers[i], "c" + i, null, "left"));
      }
      return (new TableData(label, columns, new ArrayList<>()).withNoRowsFoundHTML(empty)
         .withLinkText("View all").withLinkURL(QContext.getQInstance().getTablePath(table)).withRowsPerPage(10));
   }

   /*******************************************************************************
    ** Preserve real values while avoiding null map entries and executable HTML.
    *******************************************************************************/
   private static Map<String, Object> values(Object... values)
   {
      Map<String, Object> row = new LinkedHashMap<>();
      for(int i = 0; i < values.length; i++)
      {
         row.put("c" + i, Objects.toString(values[i], ""));
      }
      return (row);
   }

   /*******************************************************************************
    ** Standard table HTML cell contains only an escaped link and escaped text.
    *******************************************************************************/
   private static String link(String table, Object id, String label) throws QException
   {
      return ("<a href=\"" + escape(QContext.getQInstance().getTablePath(table) + "/" + id) + "\">" + escape(label) + "</a>");
   }

   /*******************************************************************************
    ** Metadata labels and stored names are never trusted markup.
    *******************************************************************************/
   private static String escape(String value)
   {
      return (Objects.toString(value, "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"));
   }

   /*******************************************************************************
    ** Explicit checks distinguish denied sections from empty sections.
    *******************************************************************************/
   static boolean canRead(String table)
   {
      CountInput input = new CountInput(table);
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (PermissionsHelper.hasTablePermission(input, table, TablePermissionSubType.READ));
   }

   /*******************************************************************************
    ** Simple filter; record security is applied exclusively by core.
    *******************************************************************************/
   private static QQueryFilter equal(String field, java.io.Serializable value)
   {
      return (DrillFilters.condition(field, QCriteriaOperator.EQUALS, value));
   }

   /*******************************************************************************
    ** Counts use the viewer's session and explicitly enforce permissions.
    *******************************************************************************/
   static int count(String table, QQueryFilter filter) throws QException
   {
      CountInput input = new CountInput(table).withFilter(filter);
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new CountAction().execute(input).getCount());
   }
}
