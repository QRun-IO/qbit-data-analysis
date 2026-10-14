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
import com.kingsrook.qbits.dataanalysis.app.DataAnalysisHome;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Real home counts, empty states and permission-scoped results.
 *******************************************************************************/
class DataAnalysisHomeTest
{
   /*******************************************************************************
    ** Release the thread-local context.
    *******************************************************************************/
   @AfterEach
   void cleanup()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Empty counts explain why; missing permissions are not represented as zero.
    *******************************************************************************/
   @Test
   void emptyAndDenied() throws Exception
   {
      QInstance instance = TestSupport.instance();
      QContext.init(instance, TestSupport.user("alice").withPermissions("data_analysis.saved.read", "data_analysis.watch.read", "data_analysis.run.read"));
      String html = DataAnalysisHome.render(Instant.now());
      assertThat(html).contains("Mine: 0", "Starred by me: 0", "No saved analyses yet",
         "No watches yet.", "No analysis runs yet.", "View all", "Open any table and choose Analyze on a column header to explore its data.", "<h3>");
      var widget = new DataAnalysisHome().render(new RenderWidgetInput()).getWidgetData();
      assertThat(widget).isInstanceOf(com.kingsrook.qqq.backend.core.model.dashboard.widgets.RawHTML.class);
      assertThat(((com.kingsrook.qqq.backend.core.model.dashboard.widgets.RawHTML) widget).getType()).isEqualTo("html");
      assertThat(html).contains("&middot;").doesNotContain("Â", "later", "phase", "<table", "color:", "width:360", "width: 360");
      assertThat(html.chars().allMatch(c -> c < 128)).isTrue();
      QContext.init(instance, TestSupport.user("alice"));
      html = DataAnalysisHome.render(Instant.now());
      assertThat(html).contains("you need data_analysis.saved.read", "you need data_analysis.watch.read").doesNotContain("Mine: 0", "Complete: 0");
      QContext.init(instance, new QSession());
      assertThat(DataAnalysisHome.render(Instant.now())).contains("Sign in to see your saved analyses");
   }



   /*******************************************************************************
    ** Different owners, shared flags, stars, disabled watches and date boundaries.
    *******************************************************************************/
   @Test
   void countsUseCurrentUserAndRealRows() throws Exception
   {
      QInstance instance = TestSupport.instance();
      Instant now = Instant.parse("2026-10-13T12:00:00Z");
      QRecord mine = TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("alice", true));
      TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("alice", false));
      QRecord bob = TestSupport.insert(DataAnalysisTables.SAVED, TestSupport.saved("bob", true));
      TestSupport.insert(DataAnalysisTables.SAVED_STAR, new QRecord().withValue("saved_id", mine.getValue("id")).withValue("user_id", "alice"));
      TestSupport.insert(DataAnalysisTables.SAVED_STAR, new QRecord().withValue("saved_id", bob.getValue("id")).withValue("user_id", "alice"));
      TestSupport.insert(DataAnalysisTables.SAVED_STAR, new QRecord().withValue("saved_id", mine.getValue("id")).withValue("user_id", "bob"));
      TestSupport.insert(DataAnalysisTables.WATCH, WatchDetectorsTest.definition("SPIKE", "COUNT", null).withValue("enabled", true).withValue("state", "FIRING"));
      TestSupport.insert(DataAnalysisTables.WATCH, WatchDetectorsTest.definition("DROP", "COUNT", null).withValue("enabled", true));
      TestSupport.insert(DataAnalysisTables.WATCH, WatchDetectorsTest.definition("DROP", "COUNT", null).withValue("enabled", false));
      run("alice", "COMPLETE", now);
      run("alice", "COMPLETE", now.minus(7, ChronoUnit.DAYS));
      run("alice", "FAILED", now.minus(1, ChronoUnit.DAYS));
      run("alice", "COMPLETE", now.minus(7, ChronoUnit.DAYS).minusSeconds(1));
      run("alice", "FAILED", now.plusSeconds(1));
      run("alice", "RUNNING", now);
      run("bob", "FAILED", now);
      QContext.init(instance, TestSupport.user("alice").withPermissions("data_analysis.saved.read", "data_analysis.watch.read", "data_analysis.run.read"));
      String html = DataAnalysisHome.render(now);
      assertThat(html).contains("Mine: 2", "Shared (visible to you): 1", "Starred by me: 1", "Enabled: 2", "Firing: 1", "Recent analysis runs", "Complete", "Failed", "Running", "Not finished")
         .doesNotContain("No saved analyses", "No watches yet", "No analysis runs yet");
      QContext.init(instance, TestSupport.user("bob").withPermissions("data_analysis.saved.read", "data_analysis.run.read"));
      html = DataAnalysisHome.render(now);
      assertThat(html).contains("Mine: 1", "Shared (visible to you): 1", "Starred by me: 0", "Failed", "you need data_analysis.watch.read");
   }



   /*******************************************************************************
    ** Latest five remain owner-scoped even with administrator bypass and escape HTML.
    *******************************************************************************/
   @Test
   void recentRunDetailsAndLimit() throws Exception
   {
      QInstance instance = TestSupport.instance();
      instance.getTable(DataAnalysisTables.SAVED).withLabel("Saved <analysis> & review");
      Instant now = Instant.parse("2026-10-13T23:10:00Z");
      for(int i = 0; i < 7; i++)
      {
         TestSupport.insert(DataAnalysisTables.RUN, new QRecord().withValue("table_name", DataAnalysisTables.SAVED)
            .withValue("operation", "COLUMN_PROFILE").withValue("user_id", "admin").withValue("status", "COMPLETE")
            .withValue("started_at", now.minusSeconds(i * 60L)).withValue("finished_at", now.minusSeconds(i * 60L)));
      }
      TestSupport.insert(DataAnalysisTables.RUN, new QRecord().withValue("table_name", "private_bob_table")
         .withValue("operation", "TABLE_PROFILE").withValue("user_id", "bob").withValue("status", "FAILED").withValue("started_at", now));
      QContext.init(instance, TestSupport.admin().withValue(QSession.VALUE_KEY_USER_TIMEZONE, "America/Chicago"));
      String html = DataAnalysisHome.render(now);
      assertThat(html).contains("Saved &lt;analysis&gt; &amp; review", "Column Profile", "2026-10-13 18:10", "2026-10-13 18:06", "<ul", "<dl")
         .doesNotContain("private_bob_table", "2026-10-13 18:05", "<analysis>");
      assertThat(html.split("<li>", -1)).hasSize(6);
      assertThat(html).contains("href=\"" + instance.getTablePath(DataAnalysisTables.RUN) + "/1\"");
      assertThat(html).contains("href=\"" + instance.getTablePath(DataAnalysisTables.RUN) + "\">View all</a>");
   }



   /*******************************************************************************
    ** Insert a real run row with an explicit owner and start time.
    *******************************************************************************/
   private static void run(String user, String status, Instant started) throws Exception
   {
      TestSupport.insert(DataAnalysisTables.RUN, new QRecord().withValue("table_name", "orders").withValue("operation", "COLUMN_PROFILE")
         .withValue("user_id", user).withValue("status", status).withValue("started_at", started));
   }
}
