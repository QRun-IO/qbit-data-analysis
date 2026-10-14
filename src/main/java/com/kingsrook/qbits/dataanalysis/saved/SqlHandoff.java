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

package com.kingsrook.qbits.dataanalysis.saved;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.dataanalysis.DataAnalysisService;
import com.kingsrook.qqq.backend.core.actions.interfaces.AggregateInterface;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;

/*******************************************************************************
 ** Request-local capture of core's actual aggregate SQL, never a separate SQL planner.
 *******************************************************************************/
public final class SqlHandoff
{
   private static final ThreadLocal<List<Statement>> CAPTURE = new ThreadLocal<>();
   private static final String NOTE = "Record security filters are applied by QQQ at run time. This SQL is for reading; ? placeholders remain bound by QQQ.";

   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private SqlHandoff()
   {
   }

   /*******************************************************************************
    ** Statement ranking prefers the main grouped matrix, then the widest metric batch.
    *******************************************************************************/
   private record Statement(String table, String sql, int dimensions, int measures)
   {
   }

   /*******************************************************************************
    ** Transported capability, statement and security note.
    *******************************************************************************/
   public record Result(Boolean available, String sql, String note)
   {
   }

   /*******************************************************************************
    ** Installation plus its configured access permission are both required.
    *******************************************************************************/
   public static boolean installedAndAllowed()
   {
      var instance = QContext.getQInstance();
      var app = instance.getApp("sqlWorkbench");
      boolean installed = app != null || instance.getTables().keySet().stream().anyMatch(t -> t.startsWith("sql_workbench_"));
      String base = app != null && app.getPermissionRules() != null && app.getPermissionRules().getPermissionBaseName() != null
         ? app.getPermissionRules().getPermissionBaseName() : "sql_workbench";
      return (installed && QContext.getQSession() != null && QContext.getQSession().hasPermission(base + ".hasAccess"));
   }

   /*******************************************************************************
    ** The backend metadata inheritance check avoids a production RDBMS dependency.
    *******************************************************************************/
   public static Result execute(SavedDefinition definition, DataAnalysisService service) throws QException
   {
      if(!installedAndAllowed())
      {
         return (new Result(false, null, "SQL Workbench is not installed or available to you."));
      }
      service.capabilities(definition.request().getTableName());
      var table = QContext.getQInstance().getTable(definition.request().getTableName());
      Class<?> type = QContext.getQInstance().getBackend(table.getBackendName()).getClass();
      boolean rdbms = false;
      while(type != null)
      {
         rdbms |= type.getSimpleName().equals("RDBMSBackendMetaData");
         type = type.getSuperclass();
      }
      if(!rdbms)
      {
         return (new Result(false, null, "SQL is unavailable for a non-RDBMS backend."));
      }
      List<Statement> statements = new ArrayList<>();
      CAPTURE.set(statements);
      try
      {
         definition.run(service, true);
      }
      finally
      {
         CAPTURE.remove();
      }
      java.util.Comparator<Statement> ranking = List.of("PIVOT", "CROSSTAB", "SHIFT").contains(definition.kind())
         ? java.util.Comparator.comparingInt(Statement::dimensions).thenComparingInt(Statement::measures)
         : java.util.Comparator.comparingInt(Statement::measures).thenComparingInt(s -> -s.dimensions());
      Statement main = statements.stream().filter(s -> s.table().equals(table.getName())).max(ranking).orElse(null);
      return (main == null ? new Result(false, null, "Core did not expose SQL for this aggregate. " + NOTE) : new Result(true, main.sql(), NOTE));
   }

   /*******************************************************************************
    ** Execute the ordinary authorized core action, then read its backend QueryStat.
    ** Core exposes QueryStat on the backend but not its action reference; this narrow
    ** bridge never changes action state, global logging, or QueryStatManager settings.
    *******************************************************************************/
   public static AggregateOutput aggregate(AggregateInput input) throws QException
   {
      if(CAPTURE.get() != null)
      {
         input.setTableMetaData(input.getTable().clone().withCapability(com.kingsrook.qqq.backend.core.model.metadata.tables.Capability.QUERY_STATS));
      }
      AggregateAction action = new AggregateAction();
      AggregateOutput output = action.execute(input);
      if(CAPTURE.get() != null)
      {
         try
         {
            Field field = AggregateAction.class.getDeclaredField("aggregateInterface");
            field.setAccessible(true);
            var stat = ((AggregateInterface) field.get(action)).getQueryStat();
            if(stat != null && stat.getQueryText() != null)
            {
               CAPTURE.get().add(new Statement(input.getTableName(), stat.getQueryText(), input.getGroupBys().size(), input.getAggregates().size()));
            }
         }
         catch(ReflectiveOperationException | RuntimeException expected)
         {
            // A future core change makes handoff unavailable rather than invent SQL.
         }
      }
      return (output);
   }
}
