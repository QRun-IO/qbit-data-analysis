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

import java.util.List;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfile;
import com.kingsrook.qbits.dataanalysis.result.CompareResult;
import com.kingsrook.qbits.dataanalysis.result.CorrelationResult;
import com.kingsrook.qbits.dataanalysis.result.CrosstabResult;
import com.kingsrook.qbits.dataanalysis.result.PivotResult;
import com.kingsrook.qbits.dataanalysis.result.ShiftResult;
import com.kingsrook.qbits.dataanalysis.result.TableProfile;

/*******************************************************************************
 ** Discriminated result: exactly the member named by kind is populated when runnable.
 *******************************************************************************/
public record SavedResult(String kind, List<String> warnings, ColumnProfile columnProfile, TableProfile tableProfile,
   PivotResult pivot, CompareResult compare, CorrelationResult correlation, CrosstabResult crosstab, ShiftResult shift)
{
   /*******************************************************************************
    ** Preserve typed engine results without an untyped JSON payload.
    *******************************************************************************/
   public static SavedResult of(String kind, Object result, List<String> warnings)
   {
      java.util.ArrayList<String> notices = new java.util.ArrayList<>(warnings);
      if(result instanceof ColumnProfile p)
      {
         notices.addAll(p.getWarnings());
      }
      else if(result instanceof TableProfile p)
      {
         notices.addAll(p.getWarnings());
      }
      else if(result instanceof PivotResult p)
      {
         notices.addAll(p.getWarnings());
      }
      return (new SavedResult(kind, notices.stream().distinct().toList(), result instanceof ColumnProfile p ? p : null, result instanceof TableProfile p ? p : null,
         result instanceof PivotResult p ? p : null, result instanceof CompareResult p ? p : null,
         result instanceof CorrelationResult p ? p : null, result instanceof CrosstabResult p ? p : null, result instanceof ShiftResult p ? p : null));
   }
}
