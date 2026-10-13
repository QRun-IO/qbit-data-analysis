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

package com.kingsrook.qbits.dataanalysis.engine;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitConfig;
import com.kingsrook.qbits.dataanalysis.TableAnalysisDefaults;
import com.kingsrook.qbits.dataanalysis.model.DataAnalysisTables;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;

/*******************************************************************************
 ** Instance-owned configuration resolver; only the settings lookup is privileged.
 *******************************************************************************/
public class EffectiveConfigResolver
{
   private final DataAnalysisQBitConfig defaults;
   private final ConcurrentHashMap<String, CachedConfig> cache = new ConcurrentHashMap<>();

   /*******************************************************************************
    ** A 60-second configuration snapshot, never a user-data cache.
    *******************************************************************************/
   private record CachedConfig(long expires, EffectiveConfig config)
   {
   }



   /*******************************************************************************
    ** Bind code defaults to one host runtime.
    *******************************************************************************/
   public EffectiveConfigResolver(DataAnalysisQBitConfig defaults)
   {
      this.defaults = defaults;
   }



   /*******************************************************************************
    ** Return a detached snapshot so callers cannot change memoized policy.
    *******************************************************************************/
   public EffectiveConfig resolve(String table) throws QException
   {
      CachedConfig cached = cache.get(table);
      if(cached == null || cached.expires() <= System.currentTimeMillis())
      {
         cached = new CachedConfig(System.currentTimeMillis() + 60_000, load(table));
         cache.put(table, cached);
      }
      EffectiveConfig c = cached.config();
      return (new EffectiveConfig().withEnabled(c.getEnabled()).withMaxRows(c.getMaxRows()).withSampleRows(c.getSampleRows())
         .withSampleThresholdRows(c.getSampleThresholdRows()).withMinGroupSize(c.getMinGroupSize()).withCacheTtlMinutes(c.getCacheTtlMinutes())
         .withAnalyzableFields(Set.copyOf(c.getAnalyzableFields())).withExcludedFields(Set.copyOf(c.getExcludedFields())));
   }



   /*******************************************************************************
    ** Settings editors may invalidate immediately instead of waiting for expiry.
    *******************************************************************************/
   public void invalidate()
   {
      cache.clear();
   }



   /*******************************************************************************
    ** Runtime row overrides per-table code defaults, then global defaults.
    *******************************************************************************/
   private EffectiveConfig load(String table) throws QException
   {
      TableAnalysisDefaults d = defaults.getTableDefaults().getOrDefault(table, new TableAnalysisDefaults());
      EffectiveConfig c = new EffectiveConfig().withEnabled(!Boolean.FALSE.equals(d.getEnabled()))
         .withMaxRows(or(d.getMaxRows(), defaults.getDefaultMaxRows())).withSampleRows(or(d.getSampleRows(), defaults.getDefaultSampleRows()))
         .withSampleThresholdRows(or(d.getSampleThresholdRows(), defaults.getDefaultSampleThresholdRows()))
         .withMinGroupSize(or(d.getMinGroupSize(), defaults.getDefaultMinGroupSize())).withCacheTtlMinutes(or(d.getCacheTtlMinutes(), defaults.getDefaultCacheTtlMinutes()))
         .withAnalyzableFields(d.getAnalyzableFields() == null ? Set.of() : Set.copyOf(d.getAnalyzableFields()))
         .withExcludedFields(d.getExcludedFields() == null ? Set.of() : Set.copyOf(d.getExcludedFields()));
      QSession original = QContext.getQSession();
      List<QRecord> rows;
      try
      {
         QContext.setQSession(new QSystemUserSession());
         QueryInput input = new QueryInput(DataAnalysisTables.TABLE_CONFIG)
            .withFilter(DrillFilters.condition("table_name", QCriteriaOperator.EQUALS, table).withLimit(1))
            .withFieldNamesToInclude(Set.of("enabled", "max_rows", "sample_rows", "sample_threshold_rows", "min_group_size", "cache_ttl_minutes", "analyzable_fields", "excluded_fields"))
            .withShouldFetchHeavyFields(true);
         input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         rows = new QueryAction().execute(input).getRecords();
      }
      finally
      {
         QContext.setQSession(original);
      }
      if(!rows.isEmpty())
      {
         QRecord r = rows.getFirst();
         if(r.getValue("enabled") != null)
         {
            c.withEnabled(r.getValueBoolean("enabled"));
         }
         c.withMaxRows(or(r.getValueInteger("max_rows"), c.getMaxRows())).withSampleRows(or(r.getValueInteger("sample_rows"), c.getSampleRows()))
            .withSampleThresholdRows(or(r.getValueInteger("sample_threshold_rows"), c.getSampleThresholdRows()))
            .withMinGroupSize(or(r.getValueInteger("min_group_size"), c.getMinGroupSize())).withCacheTtlMinutes(or(r.getValueInteger("cache_ttl_minutes"), c.getCacheTtlMinutes()));
         if(r.getValue("analyzable_fields") != null)
         {
            c.withAnalyzableFields(csv(r.getValueString("analyzable_fields")));
         }
         if(r.getValue("excluded_fields") != null)
         {
            c.withExcludedFields(csv(r.getValueString("excluded_fields")));
         }
      }
      c.withEnabled(c.getEnabled() && (defaults.getIncludeTables().isEmpty() || defaults.getIncludeTables().contains(table)) && !defaults.getExcludeTables().contains(table));
      if(List.of(c.getMaxRows(), c.getSampleRows(), c.getSampleThresholdRows(), c.getMinGroupSize(), c.getCacheTtlMinutes()).stream().anyMatch(n -> n < 0))
      {
         throw new QException("Analysis settings contain an invalid limit.");
      }
      return (c);
   }



   /*******************************************************************************
    ** Null inherits, zero is a real configured limit.
    *******************************************************************************/
   private static Integer or(Integer value, Integer fallback)
   {
      return (value == null ? fallback : value);
   }



   /*******************************************************************************
    ** Blank lists mean all readable fields or no exclusions.
    *******************************************************************************/
   private static Set<String> csv(String text)
   {
      return (Arrays.stream(text.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet()));
   }
}
