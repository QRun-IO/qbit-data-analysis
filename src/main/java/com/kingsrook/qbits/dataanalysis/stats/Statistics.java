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

package com.kingsrook.qbits.dataanalysis.stats;

import java.util.Arrays;
import java.util.Locale;

/*******************************************************************************
 ** Dependency-free statistical tests. All tests use two-sided 5% significance.
 *******************************************************************************/
public final class Statistics
{
   /*******************************************************************************
    ** Non-applicable results never contain NaN, infinity or an invented p-value.
    *******************************************************************************/
   public record Result(String test, Double statistic, Double df, Double pValue, boolean applicable, String sentence, Double cramersV)
   {
   }

   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private Statistics()
   {
   }

   /*******************************************************************************
    ** Explain failed assumptions without claiming a negative test result.
    *******************************************************************************/
   public static Result unavailable(String test, String reason)
   {
      return (new Result(test, null, null, null, false, "Not applicable: " + reason, null));
   }

   /*******************************************************************************
    ** The strict comparison deliberately excludes p equal to 0.05.
    *******************************************************************************/
   public static String sentence(double p)
   {
      String formatted = p < 0.001 ? String.format(Locale.ROOT, "%.3g", p)
         : java.math.BigDecimal.valueOf(p).setScale(p < 0.01 ? 3 : 2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
      return ((p < 0.05 ? "A and B differ" : "No significant difference") + " (p = " + formatted + ")");
   }

   /*******************************************************************************
    ** Pooled standard error under equal proportions; require expected counts >= 5.
    *******************************************************************************/
   public static Result proportions(long successesA, long nA, long successesB, long nB)
   {
      String test = "Two-proportion z-test";
      if(nA < 2 || nB < 2)
      {
         return (unavailable(test, "too few records"));
      }
      if(successesA < 0 || successesB < 0 || successesA > nA || successesB > nB)
      {
         return (unavailable(test, "invalid counts"));
      }
      double pooled = ((double) successesA + successesB) / ((double) nA + nB);
      if(Math.min(Math.min(nA * pooled, nA * (1 - pooled)), Math.min(nB * pooled, nB * (1 - pooled))) < 5)
      {
         return (unavailable(test, "expected counts below 5"));
      }
      double z = ((double) successesA / nA - (double) successesB / nB) / Math.sqrt(pooled * (1 - pooled) * (1.0 / nA + 1.0 / nB));
      double p = SpecialFunctions.gammaQ(0.5, z * z / 2);
      return (new Result(test, z, null, p, true, sentence(p), null));
   }

   /*******************************************************************************
    ** Welch-Satterthwaite degrees of freedom from sample (ddof=1) variances.
    *******************************************************************************/
   public static Result welch(long nA, double meanA, double varianceA, long nB, double meanB, double varianceB)
   {
      String test = "Welch's t-test";
      if(nA < 2 || nB < 2)
      {
         return (unavailable(test, "too few records"));
      }
      if(!Double.isFinite(meanA) || !Double.isFinite(meanB) || !Double.isFinite(varianceA) || !Double.isFinite(varianceB) || varianceA < 0 || varianceB < 0)
      {
         return (unavailable(test, "invalid numeric summary"));
      }
      double a = varianceA / nA;
      double b = varianceB / nB;
      if(a + b == 0)
      {
         return (unavailable(test, "both groups have zero variance"));
      }
      double t = (meanA - meanB) / Math.sqrt(a + b);
      double df = (a + b) * (a + b) / (a * a / (nA - 1) + b * b / (nB - 1));
      double p = SpecialFunctions.beta(df / (df + t * t), df / 2, 0.5);
      return (new Result(test, t, df, p, true, sentence(p), null));
   }

   /*******************************************************************************
    ** Pearson chi-square without Yates correction; low expected categories pool
    ** into Other. Homogeneity preserves sample rows; independence pools both axes.
    *******************************************************************************/
   public static Result chiSquare(double[][] observed, boolean homogeneity)
   {
      String test = homogeneity ? "Chi-square homogeneity" : "Chi-square independence";
      if(observed == null || observed.length < 2 || observed[0] == null || observed[0].length < 2)
      {
         return (unavailable(test, "a table of at least 2 by 2 is required"));
      }
      int columns = observed[0].length;
      for(double[] row : observed)
      {
         if(row == null || row.length != columns || Arrays.stream(row).anyMatch(v -> !Double.isFinite(v) || v < 0))
         {
            return (unavailable(test, "invalid counts"));
         }
         if(homogeneity && Arrays.stream(row).sum() < 2)
         {
            return (unavailable(test, "too few records"));
         }
      }

      ////////////////////////////////////////////////////////////////////////////////
      // pool the smallest category into Other until every expected count is >= 5. //
      // homogeneity compares fixed samples (rows), so only columns are pooled.     //
      ////////////////////////////////////////////////////////////////////////////////
      double[][] table = Arrays.stream(observed).map(double[]::clone).toArray(double[][]::new);
      boolean rowOther    = false;
      boolean columnOther = false;
      while(!validExpected(table))
      {
         double[] rows = Arrays.stream(table).mapToDouble(r -> Arrays.stream(r).sum()).toArray();
         double[] cols = Arrays.stream(transpose(table)).mapToDouble(r -> Arrays.stream(r).sum()).toArray();
         int smallestRow = homogeneity ? -1 : smallestIndex(rows, rowOther);
         int smallestCol = smallestIndex(cols, columnOther);
         boolean poolRow = smallestRow >= 0 && (smallestCol < 0 || rows[smallestRow] <= cols[smallestCol]);
         if(poolRow)
         {
            table = transpose(poolInto(transpose(table), smallestRow, rowOther));
            rowOther = true;
         }
         else if(smallestCol >= 0)
         {
            table = poolInto(table, smallestCol, columnOther);
            columnOther = true;
         }
         else
         {
            break;
         }
         if(table.length < 2 || table[0].length < 2)
         {
            break;
         }
      }
      if(table.length < 2 || table[0].length < 2)
      {
         return (unavailable(test, "too few categories after merging expected counts below 5 into Other"));
      }
      if(!validExpected(table))
      {
         return (unavailable(test, homogeneity ? "too few records" : "expected counts below 5 after merging into Other"));
      }
      double[] rows = Arrays.stream(table).mapToDouble(r -> Arrays.stream(r).sum()).toArray();
      double[] cols = Arrays.stream(transpose(table)).mapToDouble(r -> Arrays.stream(r).sum()).toArray();
      double total = Arrays.stream(rows).sum();
      double statistic = 0;
      for(int i = 0; i < rows.length; i++)
      {
         for(int j = 0; j < cols.length; j++)
         {
            double expected = rows[i] * cols[j] / total;
            statistic += Math.pow(table[i][j] - expected, 2) / expected;
         }
      }
      double df = (rows.length - 1.0) * (cols.length - 1.0);
      double p = SpecialFunctions.gammaQ(df / 2, statistic / 2);
      double v = Math.sqrt(statistic / (total * Math.min(rows.length - 1, cols.length - 1)));
      return (new Result(test, statistic, df, p, true, sentence(p), v));
   }



   /*******************************************************************************
    ** Every expected count is at least five (and the table is at least 2 by 2).
    *******************************************************************************/
   private static boolean validExpected(double[][] table)
   {
      if(table.length < 2 || table[0].length < 2)
      {
         return (false);
      }
      double[] rows = Arrays.stream(table).mapToDouble(r -> Arrays.stream(r).sum()).toArray();
      double[] columns = Arrays.stream(transpose(table)).mapToDouble(r -> Arrays.stream(r).sum()).toArray();
      double total = Arrays.stream(rows).sum();
      return (total > 0 && Arrays.stream(rows).min().orElse(0) * Arrays.stream(columns).min().orElse(0) / total >= 5);
   }



   /*******************************************************************************
    ** The index of the smallest category, not counting a trailing Other (which
    ** only grows), or -1 when there is nothing left to pool.
    *******************************************************************************/
   private static int smallestIndex(double[] totals, boolean lastIsOther)
   {
      int limit = lastIsOther ? totals.length - 1 : totals.length;
      if(limit <= (lastIsOther ? 0 : 1))
      {
         return (-1);
      }
      int smallest = 0;
      for(int i = 1; i < limit; i++)
      {
         if(totals[i] < totals[smallest])
         {
            smallest = i;
         }
      }
      return (smallest);
   }



   /*******************************************************************************
    ** Move one column into a trailing Other column (creating it if needed).
    *******************************************************************************/
   private static double[][] poolInto(double[][] table, int column, boolean hasOther)
   {
      int columns = table[0].length;
      int width = hasOther ? columns - 1 : columns;
      double[][] result = new double[table.length][width];
      for(int i = 0; i < table.length; i++)
      {
         int target = 0;
         for(int j = 0; j < columns; j++)
         {
            if(j == column || (hasOther && j == columns - 1))
            {
               continue;
            }
            result[i][target++] = table[i][j];
         }
         result[i][width - 1] = table[i][column] + (hasOther ? table[i][columns - 1] : 0);
      }
      return (result);
   }



   /*******************************************************************************
    ** Transposition lets independence apply the same pooling rule to both axes.
    *******************************************************************************/
   private static double[][] transpose(double[][] input)
   {
      double[][] result = new double[input[0].length][input.length];
      for(int i = 0; i < input.length; i++)
      {
         for(int j = 0; j < input[i].length; j++)
         {
            result[j][i] = input[i][j];
         }
      }
      return (result);
   }

   /*******************************************************************************
    ** Normalize counts to shares; floor zero shares at 1e-6 for finite PSI.
    ** This descriptive index is not a p-value or a significance test.
    *******************************************************************************/
   public static Double psi(double[] first, double[] last)
   {
      if(first == null || last == null || first.length == 0 || first.length != last.length
         || Arrays.stream(first).anyMatch(v -> !Double.isFinite(v) || v < 0) || Arrays.stream(last).anyMatch(v -> !Double.isFinite(v) || v < 0))
      {
         return (null);
      }
      double a = Arrays.stream(first).sum();
      double b = Arrays.stream(last).sum();
      if(a <= 0 || b <= 0)
      {
         return (null);
      }
      double result = 0;
      for(int i = 0; i < first.length; i++)
      {
         double x = Math.max(1e-6, first[i] / a);
         double y = Math.max(1e-6, last[i] / b);
         result += (y - x) * Math.log(y / x);
      }
      return (result);
   }

   /*******************************************************************************
    ** Centered covariance avoids cancellation for large offsets; null on constants.
    *******************************************************************************/
   public static Double pearson(double[] x, double[] y)
   {
      if(x == null || y == null || x.length < 2 || x.length != y.length
         || Arrays.stream(x).anyMatch(v -> !Double.isFinite(v)) || Arrays.stream(y).anyMatch(v -> !Double.isFinite(v)))
      {
         return (null);
      }
      double meanX = Arrays.stream(x).average().orElseThrow();
      double meanY = Arrays.stream(y).average().orElseThrow();
      double xx = 0;
      double yy = 0;
      double xy = 0;
      for(int i = 0; i < x.length; i++)
      {
         xx += (x[i] - meanX) * (x[i] - meanX);
         yy += (y[i] - meanY) * (y[i] - meanY);
         xy += (x[i] - meanX) * (y[i] - meanY);
      }
      return (xx == 0 || yy == 0 ? null : Math.max(-1, Math.min(1, xy / Math.sqrt(xx * yy))));
   }
}
