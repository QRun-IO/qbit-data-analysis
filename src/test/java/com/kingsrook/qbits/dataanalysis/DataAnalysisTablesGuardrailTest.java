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


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
import com.kingsrook.qqq.backend.core.testing.HelpBundleAssertions;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.processes.TableConfigDefaultsProcess;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.actions.help.HelpTargetCatalog;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundleEntry;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Guardrail (owner requirement 2026-10-14: "all tables should be populated and
 ** the qbit should be fully using them"). For every data_analysis_* table:
 ** - runtime code (not just its model, metadata or home screen) reads or writes
 **   it, so no table exists only on paper;
 ** - it is either seeded (analysis settings, by the Create rows action, from the
 **   configuration at run time) or listed here with why it starts empty;
 ** - it has bundle help that says what it is for and how its rows get created.
 ** A new table fails this test until it is wired, seeded or explained.
 *******************************************************************************/
class DataAnalysisTablesGuardrailTest
{
   private static final Path MAIN = Path.of("src", "main", "java", "com", "kingsrook", "qbits", "dataanalysis");

   /*******************************************************************************
    ** Seeded from the code configuration by "Create rows for analyzable tables".
    *******************************************************************************/
   static final List<String> SEEDED = List.of(DataAnalysisTables.TABLE_CONFIG);

   /*******************************************************************************
    ** Tables that legitimately start empty, and where their rows come from.
    *******************************************************************************/
   static final Map<String, String> STARTS_EMPTY = Map.of(
      DataAnalysisTables.SAVED, "user content: a person saves an analysis",
      DataAnalysisTables.SAVED_STAR, "user content: a person stars a saved analysis",
      DataAnalysisTables.SCHEDULE, "user content: a person schedules a saved analysis",
      DataAnalysisTables.WATCH, "user content: a person adds a watch to a saved analysis",
      DataAnalysisTables.RUN, "runtime: someone starts an exact analysis (it is also the result cache)");

   private QInstance instance;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      instance = TestSupport.instance();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Every table is used by runtime code.
    *******************************************************************************/
   @Test
   void everyTableIsUsedAtRuntime() throws Exception
   {
      Map<String, String> constantByTable = constantByTable();
      List<String>        runtimeSource;
      try(Stream<Path> files = Files.walk(MAIN))
      {
         runtimeSource = files.filter(p -> p.toString().endsWith(".java"))
            .filter(p -> !p.startsWith(MAIN.resolve("model")) && !p.startsWith(MAIN.resolve("metadata")) && !p.startsWith(MAIN.resolve("app")) && !p.startsWith(MAIN.resolve("help"))
               && !p.getFileName().toString().equals("DataAnalysisSchema.java"))
            .map(DataAnalysisTablesGuardrailTest::read).toList();
      }
      for(String table : analysisTables())
      {
         String constant = constantByTable.get(table);
         assertThat(constant).as("constant for " + table).isNotNull();
         Pattern use = Pattern.compile("DataAnalysisTables\\." + constant + "\\b");
         assertThat(runtimeSource.stream().anyMatch(source -> use.matcher(source).find()))
            .as(table + " (" + constant + ") must be read or written by runtime code, not only declared").isTrue();
      }
   }



   /*******************************************************************************
    ** Every table is seeded or explained.
    *******************************************************************************/
   @Test
   void everyTableIsSeededOrExplained() throws Exception
   {
      for(String table : analysisTables())
      {
         boolean seeded = SEEDED.contains(table);
         assertThat(seeded || STARTS_EMPTY.containsKey(table)).as(table + " must be seeded or listed in STARTS_EMPTY").isTrue();
         assertThat(seeded && STARTS_EMPTY.containsKey(table)).as(table + " cannot be both seeded and listed as starting empty").isFalse();
      }
      assertThat(STARTS_EMPTY.keySet()).allMatch(table -> instance.getTable(table) != null, "STARTS_EMPTY names only real tables");

      ///////////////////////////////////////////////////////////////////////////////
      // the seeded table has a real action that writes its rows, named in its help //
      ///////////////////////////////////////////////////////////////////////////////
      assertThat(instance.getProcess(TableConfigDefaultsProcess.NAME).getTableName()).isEqualTo(DataAnalysisTables.TABLE_CONFIG);
      assertThat(instance.getProcess(TableConfigDefaultsProcess.NAME).getMinInputRecords()).isEqualTo(0);
      assertThat(help(DataAnalysisTables.TABLE_CONFIG).getContent()).contains(instance.getProcess(TableConfigDefaultsProcess.NAME).getLabel());
   }



   /*******************************************************************************
    ** Every table and the app have bundle help that says how rows get created.
    *******************************************************************************/
   @Test
   void everyTableHasEmptyStateHelp() throws Exception
   {
      HelpBundleAssertions.forInstance(instance).bundles("help/qbit-data-analysis.yaml")
         .owning(target -> target.name().startsWith("data_analysis_") || target.name().startsWith("dataAnalysis") || target.name().equals("DATA_ANALYSIS_SAVED"))
         .assertComplete();
      for(String table : analysisTables())
      {
         assertThat(help(table).getContent()).as(table + " help says how rows get created").contains("How rows get created");
      }
      // RC.7's core gate requires apps, tables, processes and fields; also cover our widgets and value sources.
      List<String> keys = HelpBundles.of(instance).getBundles().getFirst().getEntries().stream().map(HelpBundleEntry::getKey).toList();
      HelpTargetCatalog.collect(instance).stream()
         .filter(target -> List.of("widget", "possibleValueSource").contains(target.kind()))
         .filter(target -> target.name().startsWith("data_analysis_") || target.name().startsWith("dataAnalysis") || target.name().equals("DATA_ANALYSIS_SAVED"))
         .forEach(target -> assertThat(keys).contains(target.key()));
      assertThat(keys).doesNotHaveDuplicates();
      assertThat(instance.getTable("helpContent")).isNull();
      HelpBundles.syncOnStartup(instance);
      assertThat(HelpBundles.of(instance).getBundles()).hasSize(1);
   }



   /*******************************************************************************
    ** Host bundles override the qbit without replacing its resource.
    *******************************************************************************/
   @Test
   void hostBundleOverridesQbit()
   {
      HelpBundles.register(instance, "help/host-override.yaml");
      var winner = HelpBundles.of(instance).resolveWinningEntries()
         .get(new HelpBundles.EntryKey("table:data_analysis_saved;slot:help", "ALL_SCREENS"));
      assertThat(winner.bundle().getId()).isEqualTo("test-host");
      assertThat(winner.entry().getSummary()).isEqualTo("Your host's saved analysis guidance.");
      assertThat(help(DataAnalysisTables.SAVED).getContent()).contains("How rows get created");
   }



   /*******************************************************************************
    ** Read the registered bundle without requiring host help storage.
    *******************************************************************************/
   private HelpBundleEntry help(String table)
   {
      return (HelpBundles.of(instance).getBundles().getFirst().getEntries().stream()
         .filter(entry -> entry.getKey().equals("table:" + table + ";slot:help")).findFirst().orElseThrow());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> analysisTables()
   {
      List<String> tables = instance.getTables().values().stream().map(QTableMetaData::getName).filter(n -> n.startsWith("data_analysis_")).sorted().toList();
      assertThat(tables).containsExactlyInAnyOrderElementsOf(DataAnalysisTables.ALL);
      return (tables);
   }



   /*******************************************************************************
    ** DataAnalysisTables declares X = "data_analysis_x".
    *******************************************************************************/
   private static Map<String, String> constantByTable() throws Exception
   {
      Map<String, String> rs = new HashMap<>();
      Matcher             m  = Pattern.compile("String\\s+([A-Z_]+)\\s*=\\s*\"(data_analysis_[a-z_]+)\"").matcher(read(MAIN.resolve("model").resolve("DataAnalysisTables.java")));
      while(m.find())
      {
         rs.put(m.group(2), m.group(1));
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String read(Path path)
   {
      try
      {
         return (Files.readString(path, StandardCharsets.UTF_8));
      }
      catch(Exception e)
      {
         throw (new IllegalStateException(e));
      }
   }

}
