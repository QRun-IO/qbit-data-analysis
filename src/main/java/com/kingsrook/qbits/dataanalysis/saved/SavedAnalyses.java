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

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.dataanalysis.DataAnalysisService;
import com.kingsrook.qbits.dataanalysis.engine.DrillFilters;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qbits.dataanalysis.watch.WatchRecords;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Saved definitions always execute in the calling context, including shared reads.
 *******************************************************************************/
public class SavedAnalyses
{
   private final DataAnalysisService service;

   /*******************************************************************************
    ** Use the instance's existing engine and cache policy.
    *******************************************************************************/
   public SavedAnalyses(DataAnalysisService service)
   {
      this.service = service;
   }

   /*******************************************************************************
    ** Standard core READ and record locks precede source-table authorization.
    *******************************************************************************/
   public SavedResult run(Serializable id) throws QException
   {
      return (SavedDefinition.parse(WatchRecords.get(DataAnalysisTables.SAVED, id)).run(service, false));
   }

   /*******************************************************************************
    ** Readable list rows contain presentation metadata, never cached owner results.
    *******************************************************************************/
   public record Entry(Serializable id, String name, String kind, String tableName, String tableLabel, String owner,
      Boolean shared, Boolean starred, Instant updated)
   {
   }

   /*******************************************************************************
    ** Only stars belonging to this viewer decorate their core-visible saved rows.
    *******************************************************************************/
   public List<Entry> list() throws QException
   {
      Set<Serializable> stars = new HashSet<>();
      for(QRecord row : query(DataAnalysisTables.SAVED_STAR, DrillFilters.condition("user_id", QCriteriaOperator.EQUALS, user())))
      {
         stars.add(row.getValue("saved_id"));
      }
      List<Entry> result = new ArrayList<>();
      for(QRecord row : query(DataAnalysisTables.SAVED, new QQueryFilter().withOrderBy(new QFilterOrderBy("modifyDate", false))))
      {
         String tableName = row.getValueString("table_name");
         var table = QContext.getQInstance().getTable(tableName);
         result.add(new Entry(row.getValue("id"), row.getValueString("name"), row.getValueString("kind"), tableName,
            table == null ? tableName : table.getLabel(), row.getValueString("owner_user_id"), row.getValueBoolean("shared"),
            stars.contains(row.getValue("id")), row.getValueInstant("modifyDate")));
      }
      return (result);
   }

   /*******************************************************************************
    ** Idempotent per-runtime PUT/DELETE, with standard insert/delete permissions.
    *******************************************************************************/
   public synchronized Boolean star(Serializable id, boolean starred) throws QException
   {
      WatchRecords.get(DataAnalysisTables.SAVED, id);
      QQueryFilter filter = DrillFilters.and(DrillFilters.condition("saved_id", QCriteriaOperator.EQUALS, id),
         DrillFilters.condition("user_id", QCriteriaOperator.EQUALS, user()));
      List<QRecord> existing = query(DataAnalysisTables.SAVED_STAR, filter);
      if(starred && existing.isEmpty())
      {
         InsertInput input = new InsertInput(DataAnalysisTables.SAVED_STAR).withRecords(List.of(new QRecord().withValue("saved_id", id).withValue("user_id", user())));
         input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         QRecord record = new InsertAction().execute(input).getRecords().getFirst();
         if(record.getErrors() != null && !record.getErrors().isEmpty())
         {
            throw new QException("Could not save the star.");
         }
      }
      else if(!starred && !existing.isEmpty())
      {
         DeleteInput input = new DeleteInput(DataAnalysisTables.SAVED_STAR).withPrimaryKeys(existing.stream().map(r -> r.getValue("id")).toList());
         input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         new DeleteAction().execute(input);
      }
      return (starred);
   }

   /*******************************************************************************
    ** Shared enforced query helper for visible records, not a security-filter layer.
    *******************************************************************************/
   public static List<QRecord> query(String table, QQueryFilter filter) throws QException
   {
      QueryInput input = new QueryInput(table).withShouldFetchHeavyFields(true).withFilter(filter);
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new QueryAction().execute(input).getRecords());
   }

   /*******************************************************************************
    ** An authenticated identity is required even in an unprotected host app.
    *******************************************************************************/
   public static String user() throws QPermissionDeniedException
   {
      var session = QContext.getQSession();
      if(session == null || session.getUser() == null || session.getUser().getIdReference() == null)
      {
         throw new QPermissionDeniedException("Sign in to use saved analyses.");
      }
      return (session.getUser().getIdReference());
   }
}
