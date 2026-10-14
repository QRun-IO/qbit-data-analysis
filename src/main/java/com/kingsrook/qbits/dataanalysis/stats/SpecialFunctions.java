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

/*******************************************************************************
 ** Regularized beta and gamma using convergent series and continued fractions.
 ** Lanczos log-gamma keeps products in log space; tails are evaluated directly.
 *******************************************************************************/
public final class SpecialFunctions
{
   private static final double EPSILON = 1e-14;
   private static final double TINY = 1e-300;
   private static final int ITERATIONS = 10000;

   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private SpecialFunctions()
   {
   }

   /*******************************************************************************
    ** Student t cumulative distribution, with symmetry around zero.
    *******************************************************************************/
   public static double studentTCdf(double t, double df)
   {
      double tail = beta(df / (df + t * t), df / 2, 0.5) / 2;
      return (t < 0 ? tail : 1 - tail);
   }

   /*******************************************************************************
    ** Chi-square CDF; significance tests use gammaQ directly to retain tiny tails.
    *******************************************************************************/
   public static double chiSquareCdf(double x, double df)
   {
      return (1 - gammaQ(df / 2, x / 2));
   }

   /*******************************************************************************
    ** Lanczos approximation, reflected below one half.
    *******************************************************************************/
   public static double logGamma(double z)
   {
      double[] coefficients = {676.5203681218851, -1259.1392167224028, 771.3234287776531, -176.6150291621406,
         12.507343278686905, -0.13857109526572012, 9.984369578019572e-6, 1.5056327351493116e-7};
      if(z < 0.5)
      {
         return (Math.log(Math.PI) - Math.log(Math.sin(Math.PI * z)) - logGamma(1 - z));
      }
      double x = 0.99999999999980993;
      for(int i = 0; i < coefficients.length; i++)
      {
         x += coefficients[i] / (z + i);
      }
      double t = z + 6.5;
      return (0.5 * Math.log(2 * Math.PI) + (z - 0.5) * Math.log(t) - t + Math.log(x));
   }

   /*******************************************************************************
    ** Regularized incomplete beta I_x(a,b), with symmetry for stable tails.
    *******************************************************************************/
   public static double beta(double x, double a, double b)
   {
      if(x <= 0 || x >= 1)
      {
         return (x <= 0 ? 0 : 1);
      }
      double factor = Math.exp(logGamma(a + b) - logGamma(a) - logGamma(b) + a * Math.log(x) + b * Math.log1p(-x));
      return (clamp(x < (a + 1) / (a + b + 2) ? factor * betaFraction(x, a, b) / a : 1 - factor * betaFraction(1 - x, b, a) / b));
   }

   /*******************************************************************************
    ** Modified Lentz evaluation of the incomplete beta continued fraction.
    *******************************************************************************/
   private static double betaFraction(double x, double a, double b)
   {
      double c = 1;
      double d = 1 / nonzero(1 - (a + b) * x / (a + 1));
      double h = d;
      for(int m = 1; m <= ITERATIONS; m++)
      {
         double aa = m * (b - m) * x / ((a + 2 * m - 1) * (a + 2 * m));
         d = 1 / nonzero(1 + aa * d);
         c = nonzero(1 + aa / c);
         h *= d * c;
         aa = -(a + m) * (a + b + m) * x / ((a + 2 * m) * (a + 2 * m + 1));
         d = 1 / nonzero(1 + aa * d);
         c = nonzero(1 + aa / c);
         double delta = d * c;
         h *= delta;
         if(Math.abs(delta - 1) < EPSILON)
         {
            return (h);
         }
      }
      throw new ArithmeticException("Incomplete beta did not converge");
   }

   /*******************************************************************************
    ** Upper regularized gamma Q(a,x), the chi-square survival probability.
    *******************************************************************************/
   public static double gammaQ(double a, double x)
   {
      if(x <= 0)
      {
         return (1);
      }
      double factor = Math.exp(-x + a * Math.log(x) - logGamma(a));
      if(x < a + 1)
      {
         double term = 1 / a;
         double sum = term;
         for(int n = 1; n <= ITERATIONS; n++)
         {
            term *= x / (a + n);
            sum += term;
            if(Math.abs(term) < Math.abs(sum) * EPSILON)
            {
               return (clamp(1 - sum * factor));
            }
         }
      }
      else
      {
         double b = x + 1 - a;
         double c = 1 / TINY;
         double d = 1 / nonzero(b);
         double h = d;
         for(int n = 1; n <= ITERATIONS; n++)
         {
            double an = -n * (n - a);
            b += 2;
            d = 1 / nonzero(an * d + b);
            c = nonzero(b + an / c);
            double delta = d * c;
            h *= delta;
            if(Math.abs(delta - 1) < EPSILON)
            {
               return (clamp(factor * h));
            }
         }
      }
      throw new ArithmeticException("Incomplete gamma did not converge");
   }

   /*******************************************************************************
    ** Avoid divisions by zero without changing the sign of a fraction term.
    *******************************************************************************/
   private static double nonzero(double value)
   {
      return (Math.abs(value) < TINY ? Math.copySign(TINY, value) : value);
   }

   /*******************************************************************************
    ** Roundoff cannot create a probability outside its domain.
    *******************************************************************************/
   private static double clamp(double value)
   {
      return (Math.max(0, Math.min(1, value)));
   }
}
