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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/*******************************************************************************
 ** Exact decimal boundaries with a readable step and approximately twenty bins.
 *******************************************************************************/
public record NiceBins(BigDecimal low, BigDecimal high, int count)
{
   /*******************************************************************************
    ** Select the closest 10..25-bin enclosure from 1, 2, 2.5 and 5 times powers of ten.
    *******************************************************************************/
   public static NiceBins covering(BigDecimal low, BigDecimal high)
   {
      return (covering(low, high, 20));
   }

   /*******************************************************************************
    ** Choose readable enclosing edges for a requested pivot bin count.
    *******************************************************************************/
   public static NiceBins covering(BigDecimal low, BigDecimal high, int target)
   {
      BigDecimal span = high.subtract(low);
      if(span.signum() == 0)
      {
         span = low.abs().max(BigDecimal.ONE);
         low = low.subtract(span.divide(BigDecimal.valueOf(2)));
         high = high.add(span.divide(BigDecimal.valueOf(2)));
      }
      int exponent = span.precision() - span.scale() - Integer.toString(target).length();
      NiceBins best = null;
      for(int power = exponent - 2; power <= exponent + 2; power++)
      {
         for(String factor : List.of("1", "2", "2.5", "5"))
         {
            BigDecimal step = new BigDecimal(factor).scaleByPowerOfTen(power);
            BigDecimal first = low.divide(step, 0, RoundingMode.FLOOR).multiply(step);
            BigDecimal last = high.divide(step, 0, RoundingMode.CEILING).multiply(step);
            int count = last.subtract(first).divide(step).intValueExact();
            if(count >= Math.max(1, target / 2) && count <= Math.max(2, target * 5 / 4) && (best == null || Math.abs(count - target) < Math.abs(best.count() - target)))
            {
               best = new NiceBins(first, last, count);
            }
         }
      }
      return (java.util.Objects.requireNonNull(best));
   }
}
