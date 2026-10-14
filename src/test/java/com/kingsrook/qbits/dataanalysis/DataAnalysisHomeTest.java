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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.kingsrook.qbits.dataanalysis.app.DataAnalysisHome;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiStatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DataAnalysisHomeTest
{
   @AfterEach
   void cleanup()
   {
      QContext.clear();
   }

   @Test
   void standardWidgetsWithEmptyAndDeniedStates() throws Exception
   {
      var instance = TestSupport.instance();
      QContext.init(instance, TestSupport.user("alice").withPermissions("data_analysis.saved.read", "data_analysis.watch.read", "data_analysis.run.read"));
      var counts = DataAnalysisHome.statistics(Instant.now());
      assertThat(counts.getStatisticsGroupData()).flatExtracting(g -> g.getStatisticList()).allSatisfy(s -> assertThat(s.getValue()).isZero());
      for(var widget : DataAnalysisHome.widgets())
      {
         assertThat(widget.getType()).isIn("multiStatistics", "table");
         var data = new DataAnalysisHome().render(new RenderWidgetInput().withWidgetMetaData(widget)).getWidgetData();
         if(data instanceof TableData table)
         {
            assertThat(table.getColumns()).hasSizeLessThanOrEqualTo(4);
            assertThat(table.getRows()).isEmpty();
            assertThat(table.getNoRowsFoundHTML()).startsWith("No ");
         }
      }
      QContext.init(instance, TestSupport.user("alice"));
      assertThat(DataAnalysisHome.statistics(Instant.now()).getStatisticsGroupData()).allSatisfy(g ->
      {
         assertThat(g.getStatisticList()).isEmpty();
         assertThat(g.getSubheader()).contains("Unavailable");
      });
      assertThat(((TableData) DataAnalysisHome.render(DataAnalysisHome.FIRING, Instant.now())).getNoRowsFoundHTML()).contains("Unavailable");
   }

   @Test
   void countsRowsLinksAndDateBoundaries() throws Exception
   {
      var instance = TestSupport.instance();
      Instant now = Instant.parse("2026-10-13T12:00:00Z");
      var mine = TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("alice", false).withValue("name", "<script>& profile"));
      var shared = TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("bob", true));
      var hidden = TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("bob", false));
      for(var saved : List.of(mine, shared, hidden))
      {
         TestSupport.insert(DataAnalysisTables.SAVED_STAR, new QRecord().withValue("saved_id", saved.getValue("id")).withValue("user_id", "alice"));
      }
      for(Instant start : List.of(now, now.minus(7, ChronoUnit.DAYS), now.minus(7, ChronoUnit.DAYS).minusSeconds(1), now.plusSeconds(1)))
      {
         run("alice", start);
      }
      run("bob", now);
      QContext.init(instance, TestSupport.user("alice").withPermissions("data_analysis.saved.read", "data_analysis.run.read"));
      var counts = DataAnalysisHome.statistics(now).getStatisticsGroupData();
      assertThat(counts.getFirst().getStatisticList()).extracting(s -> s.getValue()).containsExactly(1, 1, 2);
      assertThat(counts.getLast().getStatisticList().getFirst().getValue()).isEqualTo(2);
      TableData starred = (TableData) DataAnalysisHome.render(DataAnalysisHome.STARRED, now);
      assertThat(starred.getRows()).hasSize(2);
      assertThat(starred.getRows().toString()).contains("&lt;script&gt;&amp; profile", instance.getTablePath(DataAnalysisTables.SAVED)).doesNotContain("<script>");
      TableData recent = (TableData) DataAnalysisHome.render(DataAnalysisHome.RECENT, now);
      assertThat(recent.getRows()).hasSize(3);
      assertThat(recent.getLinkURL()).isEqualTo(instance.getTablePath(DataAnalysisTables.RUN));
   }

   private static void run(String user, Instant started) throws Exception
   {
      TestSupport.insert(DataAnalysisTables.RUN, new QRecord().withValue("table_name", "orders").withValue("operation", "COLUMN_PROFILE")
         .withValue("user_id", user).withValue("status", "COMPLETE").withValue("started_at", started));
   }
}
