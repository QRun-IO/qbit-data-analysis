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

package com.kingsrook.qbits.dataanalysis;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import com.kingsrook.qbits.dataanalysis.api.DataAnalysisApi;
import com.kingsrook.qbits.dataanalysis.app.DataAnalysisHome;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.TableFeaturePermission;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.TableFeaturePermissionsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;


/*******************************************************************************
 ** Entry point for the Data Analysis qbit; scans only its metadata package.
 *******************************************************************************/
public class DataAnalysisQBitProducer implements QBitMetaDataProducer<DataAnalysisQBitConfig>
{
   public static final String GROUP_ID = "com.kingsrook.qbits";
   public static final String ARTIFACT_ID = "qbit-data-analysis";
   public static final String VERSION = loadVersion();

   /////////////////////////////////////////////////////////////////////////////////
   // Widgets that show engine configuration. They are registered with the qbit   //
   // (admin-only, hidden from everyone else) but kept off the business home; the //
   // host places them on its Admin area, e.g. withWidgets(SYSTEM_WIDGETS).       //
   /////////////////////////////////////////////////////////////////////////////////
   public static final List<String> SYSTEM_WIDGETS = List.of(DataAnalysisHome.SETUP);

   private DataAnalysisQBitConfig qBitConfig;
   private String namespace;



   /*******************************************************************************
    ** Read the artifact version from the Maven-filtered properties resource.
    *******************************************************************************/
   private static String loadVersion()
   {
      try(InputStream inputStream = DataAnalysisQBitProducer.class.getClassLoader().getResourceAsStream("qbit-data-analysis.properties"))
      {
         if(inputStream == null)
         {
            throw (new IllegalStateException("Missing qbit-data-analysis.properties; build with Maven to generate the artifact version"));
         }
         Properties properties = new Properties();
         properties.load(inputStream);
         String version = properties.getProperty("version");
         if(version == null || version.isBlank() || version.contains("${"))
         {
            throw (new IllegalStateException("Missing or unfiltered version in qbit-data-analysis.properties; build with Maven resource filtering"));
         }
         return (version);
      }
      catch(IOException e)
      {
         throw (new IllegalStateException("Could not read artifact version from qbit-data-analysis.properties", e));
      }
   }



   /*******************************************************************************
    ** Fluent configuration setter.
    *******************************************************************************/
   public DataAnalysisQBitProducer withQBitConfig(DataAnalysisQBitConfig config)
   {
      qBitConfig = config;
      return (this);
   }



   /*******************************************************************************
    ** Configuration for component producers.
    *******************************************************************************/
   @Override
   public DataAnalysisQBitConfig getQBitConfig()
   {
      return (qBitConfig);
   }



   /*******************************************************************************
    ** Fluent namespace setter.
    *******************************************************************************/
   public DataAnalysisQBitProducer withNamespace(String namespace)
   {
      this.namespace = namespace;
      return (this);
   }



   /*******************************************************************************
    ** Qbit namespace.
    *******************************************************************************/
   @Override
   public String getNamespace()
   {
      return (namespace);
   }



   /*******************************************************************************
    ** Qbit identity.
    *******************************************************************************/
   @Override
   public QBitMetaData getQBitMetaData()
   {
      return (new QBitMetaData().withGroupId(GROUP_ID).withArtifactId(ARTIFACT_ID).withVersion(VERSION).withNamespace(namespace).withConfig(qBitConfig));
   }



   /*******************************************************************************
    ** Unconfigured producers discovered by host scanning are disabled.
    *******************************************************************************/
   @Override
   public boolean isEnabled()
   {
      return (qBitConfig != null);
   }



   /*******************************************************************************
    ** Helpers, renderers and the top-level producer are never component producers.
    *******************************************************************************/
   @Override
   public String getPackageNameForFindingMetaDataProducers()
   {
      return ("com.kingsrook.qbits.dataanalysis.metadata");
   }



   /*******************************************************************************
    ** Install metadata and attach the app to its optional parent, like reference qbits.
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance instance) throws QException
   {
      qBitConfig.validate(instance);
      if(instance.getPossibleValueSource(TablesPossibleValueSourceMetaDataProvider.NAME) == null)
      {
         instance.addPossibleValueSource(TablesPossibleValueSourceMetaDataProvider.defineTablesPossibleValueSource(instance));
      }
      MetaDataProducerMultiOutput output = QBitMetaDataProducer.super.produce(instance);
      output.addSelfToInstance(instance);
      new DataAnalysisRuntime(qBitConfig).addSelfToInstance(instance);
      com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData.ofOrWithNew(instance).withAlertType(
         new com.kingsrook.qqq.backend.core.alerts.QAlertTypeMetaData().withName(com.kingsrook.qbits.dataanalysis.watch.WatchEvaluator.ALERT_TYPE)
            .withLabel("Data anomaly").withSourceName(ARTIFACT_ID).withGroupName("data-analysis")
            .withDefaultSeverity(com.kingsrook.qqq.backend.core.alerts.AlertSeverity.WARNING)
            .withResolveMode(com.kingsrook.qqq.backend.core.alerts.ResolveMode.PRODUCER)
            .withSubjectTableName(com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables.WATCH)
            .withVisibilityPermission("data_analysis.watch.read"));
      com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData.ofOrWithNew(instance).withAlertType(
         new com.kingsrook.qqq.backend.core.alerts.QAlertTypeMetaData().withName(com.kingsrook.qbits.dataanalysis.schedule.ScheduleRunner.ALERT_TYPE)
            .withLabel("Analysis schedule failed").withSourceName(ARTIFACT_ID).withGroupName("data-analysis")
            .withDefaultSeverity(com.kingsrook.qqq.backend.core.alerts.AlertSeverity.ERROR)
            .withResolveMode(com.kingsrook.qqq.backend.core.alerts.ResolveMode.PRODUCER)
            .withSubjectTableName(com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables.SCHEDULE)
            .withVisibilityPermission("data_analysis.saved.read"));
      var included = java.util.Set.copyOf(qBitConfig.getIncludeTables());
      var excluded = java.util.Set.copyOf(qBitConfig.getExcludeTables());
      TableFeaturePermissionsMetaData.ofOrWithNew(instance).withPermission(new TableFeaturePermission()
         .withSuffix("analyze").withLabel("Analyze").withImpliedBy(null)
         .withAppliesTo((qInstance, table) -> !table.getName().startsWith(DataAnalysisQBitConfig.TABLE_NAME_PREFIX)
            && (included.isEmpty() || included.contains(table.getName())) && !excluded.contains(table.getName())));
      try
      {
         Class.forName("io.javalin.Javalin");
         Class.forName("com.kingsrook.qqq.middleware.javalin.QJavalinImplementation");
         DataAnalysisApi.register(instance);
      }
      catch(ClassNotFoundException expected)
      {
         // The engine and metadata also work without the optional middleware.
      }
      if(qBitConfig.getParentAppName() != null && instance.getApp(qBitConfig.getParentAppName()) != null)
      {
         instance.getApp(qBitConfig.getParentAppName()).withChild(instance.getApp(qBitConfig.getAppName()));
      }
      // Registration is metadata-only; core startup sync guards hosts without helpContent.
      HelpBundles.register(instance, "help/qbit-data-analysis.yaml");
      HelpBundles.of(instance).getBundles().getLast().getEntries().stream()
         .filter(entry -> "app:dataAnalysis;slot:help".equals(entry.getKey()))
         .forEach(entry -> entry.setKey("app:" + qBitConfig.getAppName() + ";slot:help"));
      return (new MetaDataProducerMultiOutput());
   }
}
