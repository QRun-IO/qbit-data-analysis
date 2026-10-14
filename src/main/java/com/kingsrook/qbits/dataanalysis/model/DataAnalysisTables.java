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


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitConfig;
import com.kingsrook.qbits.dataanalysis.permissions.DataAnalysisPermissions;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.SectionFactory;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** Visible Data Analysis tables and their enum sources, joins and child lists.
 *******************************************************************************/
public final class DataAnalysisTables
{
   public static final String TABLE_CONFIG = "data_analysis_table_config";
   public static final String SAVED = "data_analysis_saved";
   public static final String SAVED_STAR = "data_analysis_saved_star";
   public static final String SCHEDULE = "data_analysis_schedule";
   public static final String WATCH = "data_analysis_watch";
   public static final String RUN = "data_analysis_run";
   public static final String USER_KEY = "data_analysis_user";
   public static final List<String> ALL = List.of(TABLE_CONFIG, SAVED, SAVED_STAR, SCHEDULE, WATCH, RUN);
   public static final List<String> NAV_TABLES = List.of(SAVED, WATCH, TABLE_CONFIG, RUN);

   private static final Map<String, List<String>> CHILD_COLUMNS = Map.of(
      SAVED_STAR, List.of("user_id", "createDate"),
      SCHEDULE, List.of("frequency", "enabled", "next_run_at", "last_status"),
      WATCH, List.of("detector", "enabled", "state", "last_evaluated_at"));



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DataAnalysisTables()
   {
   }



   /*******************************************************************************
    ** Standard CRUD permissions under the given base name.
    *******************************************************************************/
   public static QPermissionRules permissionRules(String baseName)
   {
      return (new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS)
         .withPermissionBaseName(baseName));
   }



   /*******************************************************************************
    ** The host supplies its authenticated user id as the value of this key.
    *******************************************************************************/
   public static QSecurityKeyType userKeyType()
   {
      return (new QSecurityKeyType().withName(USER_KEY).withAllAccessKeyName(DataAnalysisPermissions.ADMIN));
   }



   /*******************************************************************************
    ** All enum and table possible-value sources.
    *******************************************************************************/
   public static List<QPossibleValueSource> possibleValueSources()
   {
      List<QPossibleValueSource> sources = new ArrayList<>();
      sources.add(new QPossibleValueSource().withName(DataAnalysisFieldPossibleValueProvider.NAME)
         .withType(QPossibleValueSourceType.CUSTOM).withIdType(QFieldType.STRING)
         .withCustomCodeReference(new QCodeReference(DataAnalysisFieldPossibleValueProvider.class)));
      sources.add(enumSource("kind", "COLUMN_PROFILE", "TABLE_PROFILE", "PIVOT", "COMPARE", "CORRELATION", "CROSSTAB", "SHIFT"));
      sources.add(enumSource("chart_kind", "HISTOGRAM", "BOX", "BAR", "PARETO", "TIME_SERIES", "SCATTER", "HEATMAP"));
      sources.add(enumSource("detector", "SPIKE", "DROP", "OUT_OF_RANGE", "NEW_CATEGORY", "NULL_RATE_CREEP"));
      sources.add(enumSource("watch_state", "OK", "FIRING"));
      sources.add(enumSource("watch_bucket", "HOUR", "DAY", "WEEK", "MONTH"));
      sources.add(enumSource("watch_severity", "INFO", "WARNING", "ERROR", "CRITICAL"));
      sources.add(enumSource("run_status", "RUNNING", "COMPLETE", "FAILED"));
      sources.add(enumSource("schedule_frequency", "DAILY", "WEEKLY", "MONTHLY"));
      sources.add(enumSource("schedule_format", "HTML_SUMMARY", "CSV", "XLSX"));
      for(String table : ALL)
      {
         sources.add(QPossibleValueSource.newForTable(table));
      }
      return (sources);
   }



   /*******************************************************************************
    ** An enum source with stable string ids and readable labels.
    *******************************************************************************/
   private static QPossibleValueSource enumSource(String name, String... values)
   {
      List<QPossibleValue<?>> entries = new ArrayList<>();
      for(String value : values)
      {
         entries.add(new QPossibleValue<>(value, label(value)));
      }
      return (new QPossibleValueSource().withName("data_analysis_" + name).withType(QPossibleValueSourceType.ENUM).withEnumValues(entries));
   }



   /*******************************************************************************
    ** Readable labels for snake case identifiers.
    *******************************************************************************/
   private static String label(String name)
   {
      StringBuilder result = new StringBuilder();
      for(String word : name.toLowerCase(Locale.ROOT).split("_"))
      {
         result.append(result.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
      }
      return (result.toString());
   }



   /*******************************************************************************
    ** Base metadata; backendName identifies the connection, SQL names infer from table names.
    *******************************************************************************/
   private static QTableMetaData base(DataAnalysisQBitConfig config, String name, String title, String icon, String permissionBase)
   {
      return (new QTableMetaData().withName(name).withLabel(title).withIcon(new QIcon(icon))
         .withBackendName(config.getBackendName()).withPrimaryKeyField("id").withPermissionRules(permissionRules(permissionBase))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withLabel("ID").withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withLabel("Created").withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withLabel("Modified").withIsEditable(false)));
   }



   /*******************************************************************************
    ** A field with a readable label and bounded strings.
    *******************************************************************************/
   private static QFieldMetaData field(String name, QFieldType type)
   {
      QFieldMetaData field = new QFieldMetaData(name, type).withLabel(label(name));
      if(type == QFieldType.STRING)
      {
         field.withMaxLength(250);
      }
      return (field);
   }



   /*******************************************************************************
    ** Owner-only reads and writes, with null owners denied.
    *******************************************************************************/
   private static RecordSecurityLock ownerLock(String field)
   {
      return (new RecordSecurityLock().withSecurityKeyType(USER_KEY).withFieldName(field)
         .withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
   }



   /*******************************************************************************
    ** The six tables in spec section 7.1.
    *******************************************************************************/
   public static List<QTableMetaData> tables(DataAnalysisQBitConfig config)
   {
      List<QTableMetaData> tables = new ArrayList<>();
      tables.add(base(config, TABLE_CONFIG, "Analysis Settings", "settings", "data_analysis.settings")
         .withRecordLabelFormatAndFields("%s", "table_name")
         .withField(field("table_name", QFieldType.STRING).withPossibleValueSourceName(TablesPossibleValueSourceMetaDataProvider.NAME).withIsRequired(true))
         .withField(field("enabled", QFieldType.BOOLEAN).withDefaultValue(true))
         .withField(field("max_rows", QFieldType.INTEGER))
         .withField(field("sample_threshold_rows", QFieldType.INTEGER))
         .withField(field("sample_rows", QFieldType.INTEGER))
         .withField(field("min_group_size", QFieldType.INTEGER))
         // add possibleValueSourceDependsOn("table_name") once core has it
         .withField(field("analyzable_fields", QFieldType.TEXT).withPossibleValueSourceName(DataAnalysisFieldPossibleValueProvider.NAME))
         // add possibleValueSourceDependsOn("table_name") once core has it
         .withField(field("excluded_fields", QFieldType.TEXT).withPossibleValueSourceName(DataAnalysisFieldPossibleValueProvider.NAME))
         .withField(field("cache_ttl_minutes", QFieldType.INTEGER))
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD))
         .withUniqueKey(new UniqueKey("table_name")));
      tables.add(base(config, SAVED, "Saved Analyses", "insights", "data_analysis.saved")
         .withRecordLabelFormatAndFields("%s", "name")
         .withField(field("name", QFieldType.STRING).withIsRequired(true))
         .withField(field("description", QFieldType.TEXT))
         .withField(field("table_name", QFieldType.STRING).withPossibleValueSourceName(TablesPossibleValueSourceMetaDataProvider.NAME).withIsRequired(true))
         .withField(field("filter_json", QFieldType.TEXT))
         .withField(field("kind", QFieldType.STRING).withPossibleValueSourceName("data_analysis_kind").withIsRequired(true))
         .withField(field("definition_json", QFieldType.TEXT).withIsRequired(true))
         .withField(field("chart_kind", QFieldType.STRING).withPossibleValueSourceName("data_analysis_chart_kind"))
         .withField(field("owner_user_id", QFieldType.STRING).withIsRequired(true))
         .withField(field("shared", QFieldType.BOOLEAN).withDefaultValue(false))
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD))
         .withRecordSecurityLock(ownerLock("owner_user_id")));
      tables.add(base(config, SAVED_STAR, "Stars", "star", "data_analysis.saved")
         .withRecordLabelFormatAndFields("%s", "user_id")
         .withField(field("saved_id", QFieldType.INTEGER).withPossibleValueSourceName("data_analysis_saved").withIsRequired(true))
         .withField(field("user_id", QFieldType.STRING).withIsRequired(true))
         .withUniqueKey(new UniqueKey("saved_id", "user_id"))
         .withRecordSecurityLock(ownerLock("user_id")));
      tables.add(base(config, SCHEDULE, "Schedules", "schedule", "data_analysis.saved")
         .withRecordLabelFormatAndFields("%s", "saved_id")
         .withField(field("saved_id", QFieldType.INTEGER).withPossibleValueSourceName("data_analysis_saved").withIsRequired(true))
         .withField(field("cron", QFieldType.STRING))
         .withField(field("frequency", QFieldType.STRING).withPossibleValueSourceName("data_analysis_schedule_frequency"))
         .withField(field("day", QFieldType.INTEGER))
         .withField(field("time", QFieldType.STRING))
         .withField(field("time_zone", QFieldType.STRING))
         .withField(field("format", QFieldType.STRING).withDefaultValue("HTML_SUMMARY").withPossibleValueSourceName("data_analysis_schedule_format"))
         .withField(field("recipients", QFieldType.TEXT))
         .withField(field("enabled", QFieldType.BOOLEAN).withDefaultValue(false))
         .withField(field("last_run_at", QFieldType.DATE_TIME))
         .withField(field("last_status", QFieldType.STRING).withPossibleValueSourceName("data_analysis_run_status"))
         .withField(field("next_run_at", QFieldType.DATE_TIME))
         .withRecordSecurityLock(ownerLock(SAVED + ".owner_user_id").withJoinNameChain(List.of(joinName(SCHEDULE)))));
      tables.add(base(config, WATCH, "Watches", "notifications_active", "data_analysis.watch")
         .withRecordLabelFormatAndFields("%s", "detector")
         .withField(field("saved_id", QFieldType.INTEGER).withPossibleValueSourceName("data_analysis_saved")
            .withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("table_name", QCriteriaOperator.EQUALS, "${input.table_name}"))))
         .withField(field("table_name", QFieldType.STRING).withPossibleValueSourceName(TablesPossibleValueSourceMetaDataProvider.NAME))
         .withField(field("filter_json", QFieldType.TEXT))
         .withField(field("metric_json", QFieldType.TEXT))
         .withField(field("detector", QFieldType.STRING).withPossibleValueSourceName("data_analysis_detector").withIsRequired(true))
         .withField(field("parameters_json", QFieldType.TEXT))
         .withField(field("date_field", QFieldType.STRING).withIsRequired(true))
         .withField(field("bucket", QFieldType.STRING).withDefaultValue("DAY").withIsRequired(true).withPossibleValueSourceName("data_analysis_watch_bucket"))
         .withField(field("severity", QFieldType.STRING).withDefaultValue("WARNING").withIsRequired(true).withPossibleValueSourceName("data_analysis_watch_severity"))
         .withField(field("owner_user_id", QFieldType.STRING).withIsRequired(true))
         .withField(field("enabled", QFieldType.BOOLEAN).withDefaultValue(false))
         .withField(field("last_evaluated_at", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(field("last_value", QFieldType.DECIMAL).withIsEditable(false))
         .withField(field("last_message", QFieldType.TEXT).withIsEditable(false))
         .withField(field("state", QFieldType.STRING).withDefaultValue("OK").withIsEditable(false).withPossibleValueSourceName("data_analysis_watch_state"))
         .withField(field("alert_key", QFieldType.STRING).withIsEditable(false))
         .withRecordSecurityLock(ownerLock("owner_user_id"))
         .withCustomizer(com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers.PRE_INSERT_RECORD,
            new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(com.kingsrook.qbits.dataanalysis.watch.WatchCustomizer.class))
         .withCustomizer(com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers.PRE_UPDATE_RECORD,
            new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(com.kingsrook.qbits.dataanalysis.watch.WatchCustomizer.class)));
      tables.add(base(config, RUN, "Analysis Runs", "history", "data_analysis.run")
         .withRecordLabelFormatAndFields("%s", "operation")
         .withField(field("table_name", QFieldType.STRING).withPossibleValueSourceName(TablesPossibleValueSourceMetaDataProvider.NAME).withIsRequired(true))
         .withField(field("operation", QFieldType.STRING).withIsRequired(true))
         .withField(field("request_hash", QFieldType.STRING))
         .withField(field("filter_hash", QFieldType.STRING))
         .withField(field("data_version", QFieldType.STRING))
         .withField(field("status", QFieldType.STRING).withDefaultValue("RUNNING").withPossibleValueSourceName("data_analysis_run_status").withIsRequired(true))
         .withField(field("started_at", QFieldType.DATE_TIME))
         .withField(field("finished_at", QFieldType.DATE_TIME))
         .withField(field("rows_scanned", QFieldType.LONG))
         .withField(field("sampled", QFieldType.BOOLEAN).withDefaultValue(false))
         .withField(field("result_json", QFieldType.TEXT))
         .withField(field("error", QFieldType.TEXT))
         .withField(field("user_id", QFieldType.STRING).withIsRequired(true))
         .withRecordSecurityLock(ownerLock("user_id")));
      for(QTableMetaData table : tables)
      {
         List<String> fields = new ArrayList<>(table.getFields().keySet());
         fields.removeAll(List.of("id", "createDate", "modifyDate"));
         int headlineSize = Math.min(4, fields.size());
         table.withSection(SectionFactory.defaultT1(fields.subList(0, headlineSize).toArray(String[]::new)));
         if(fields.size() > headlineSize)
         {
            table.withSection(new QFieldSection("details", "Details", new QIcon("list"), Tier.T2, fields.subList(headlineSize, fields.size())));
         }
         table.withSection(SectionFactory.defaultT3("id", "createDate", "modifyDate"));
         QInstanceEnricher.setInferredFieldBackendNames(table);
         if(SAVED.equals(table.getName()))
         {
            for(String child : List.of(SAVED_STAR, SCHEDULE, WATCH))
            {
               String title = child.equals(SAVED_STAR) ? "Stars" : child.equals(SCHEDULE) ? "Schedules" : "Watches";
               table.withSection(new QFieldSection(joinName(child), new QIcon("list"), Tier.T2).withLabel(title).withWidgetName(joinName(child)));
               table.withExposedJoin(new ExposedJoin().withLabel(title).withJoinTable(child).withJoinPath(List.of(joinName(child))));
            }
         }
      }
      return (tables);
   }



   /*******************************************************************************
    ** Stable names shared by joins, widgets and schedule locks.
    *******************************************************************************/
   public static String joinName(String child)
   {
      return (SAVED + "_to_" + child);
   }



   /*******************************************************************************
    ** Saved analyses link to their stars, schedules and watches.
    *******************************************************************************/
   public static List<QJoinMetaData> joins()
   {
      List<QJoinMetaData> joins = new ArrayList<>();
      for(String child : List.of(SAVED_STAR, SCHEDULE, WATCH))
      {
         joins.add(new QJoinMetaData().withName(joinName(child)).withLeftTable(SAVED).withRightTable(child)
            .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "saved_id")));
      }
      return (joins);
   }



   /*******************************************************************************
    ** Child lists use the standard renderer and at most four visible columns.
    *******************************************************************************/
   public static List<QWidgetMetaData> childListWidgets()
   {
      List<QWidgetMetaData> widgets = new ArrayList<>();
      for(QJoinMetaData join : joins())
      {
         String child = join.getRightTable();
         widgets.add(ChildRecordListRenderer.widgetMetaDataBuilder(join).withName(join.getName())
            .withLabel(child.equals(SAVED_STAR) ? "Stars" : child.equals(SCHEDULE) ? "Schedules" : "Watches")
            .withMaxRows(10).withOnlyIncludeFieldNames(CHILD_COLUMNS.get(child)).getWidgetMetaData());
      }
      return (widgets);
   }
}
