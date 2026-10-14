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

import com.kingsrook.qbits.dataanalysis.stats.SpecialFunctions;
import com.kingsrook.qbits.dataanalysis.stats.Statistics;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/*******************************************************************************
 ** Independent SciPy 1.18.1 references, never computed with production helpers.
 *******************************************************************************/
class StatisticsTest
{
   /*******************************************************************************
    ** Verify welch references.
    *******************************************************************************/
   @Test
   void welchReferences()
   {
      // scipy.stats.ttest_ind_from_stats(15, sqrt(87.5), 13, 12, sqrt(39), 11, equal_var=False)
      var result = Statistics.welch(13, 15, 87.5, 11, 12, 39);
      assertThat(result.statistic()).isCloseTo(0.9358461935556048, within(1e-12));
      assertThat(result.pValue()).isCloseTo(0.35999818693244234, within(1e-12));
      // scipy.stats.ttest_ind([1,3,4,6,11,13,15,19,22,24,25,26,26], [2,4,6,9,11,13,14,15,18,19,21], equal_var=False).df
      assertThat(result.df()).isCloseTo(20.984611233429924, within(1e-12));
      assertThat(result.sentence()).isEqualTo("No significant difference (p = 0.36)");
      // scipy.stats.ttest_ind_from_stats(100, 2, 30, 104, 3, 25, equal_var=False)
      assertThat(Statistics.welch(30, 100, 4, 25, 104, 9).pValue()).isCloseTo(1.2223199525471597e-6, within(1e-16));
      assertThat(Statistics.welch(1, 0, 1, 2, 0, 1).sentence()).isEqualTo("Not applicable: too few records");
      assertThat(Statistics.welch(2, 0, 0, 2, 1, 0).applicable()).isFalse();
      assertThat(Statistics.welch(2, 0, -1, 2, 1, 1).applicable()).isFalse();
      assertThat(Statistics.welch(2, Double.NaN, 1, 2, 1, 1).applicable()).isFalse();
      assertThat(Statistics.welch(20, 1, 1, 20, 1, 1).pValue()).isEqualTo(1);
   }

   /*******************************************************************************
    ** Verify proportion reference and significance boundary.
    *******************************************************************************/
   @Test
   void proportionReferenceAndSignificanceBoundary()
   {
      // p=(30+45)/(150+200); z=(30/150-45/200)/sqrt(p*(1-p)*(1/150+1/200)); 2*scipy.stats.norm.sf(abs(z))
      var result = Statistics.proportions(30, 150, 45, 200);
      assertThat(result.statistic()).isCloseTo(-0.564076074817766, within(1e-12));
      assertThat(result.pValue()).isCloseTo(0.5727023597783104, within(1e-12));
      assertThat(Statistics.sentence(0.05)).isEqualTo("No significant difference (p = 0.05)");
      assertThat(Statistics.sentence(0.003)).isEqualTo("A and B differ (p = 0.003)");
      assertThat(Statistics.sentence(0.41)).isEqualTo("No significant difference (p = 0.41)");
      assertThat(Statistics.proportions(1, 1, 1, 2).applicable()).isFalse();
      assertThat(Statistics.proportions(0, 100, 0, 100).applicable()).isFalse();
      assertThat(Statistics.proportions(-1, 20, 10, 20).applicable()).isFalse();
      assertThat(Statistics.proportions(21, 20, 10, 20).applicable()).isFalse();
   }

   /*******************************************************************************
    ** Verify chi square reference and pooling.
    *******************************************************************************/
   @Test
   void chiSquareReferenceAndPooling()
   {
      // scipy.stats.chi2_contingency([[30,20],[10,40]], correction=False)
      var result = Statistics.chiSquare(new double[][] {{30, 20}, {10, 40}}, false);
      assertThat(result.statistic()).isCloseTo(16.666666666666668, within(1e-12));
      assertThat(result.df()).isEqualTo(1);
      assertThat(result.pValue()).isCloseTo(4.455709060405612e-5, within(1e-14));
      // scipy.stats.contingency.association([[30,20],[10,40]], method='cramer', correction=False)
      assertThat(result.cramersV()).isCloseTo(0.408248290463863, within(1e-12));
      // scipy.stats.chi2_contingency([[20,30,50],[30,40,30]], correction=False)
      assertThat(Statistics.chiSquare(new double[][] {{20, 30, 50}, {30, 40, 30}}, true).pValue()).isCloseTo(0.01478287719483942, within(1e-12));
      // Ten rare columns pool into Other, yielding [[40,10],[20,30]]; scipy.stats.chi2_contingency(..., correction=False).
      double[][] rare = {{40, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {20, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3}};
      assertThat(Statistics.chiSquare(rare, true).pValue()).isCloseTo(4.455709060405612e-5, within(1e-14));
      // scipy.stats.chi2_contingency([[40,10],[20,30],[4,8]], correction=False); four rare rows merge into Other.
      assertThat(Statistics.chiSquare(new double[][] {{40, 10}, {20, 30}, {1, 2}, {1, 2}, {1, 2}, {1, 2}}, false).pValue())
         .isCloseTo(5.993665986784811e-5, within(1e-14));
      assertThat(Statistics.chiSquare(new double[][] {{3, 3}, {3, 3}}, true).applicable()).isFalse();
      assertThat(Statistics.chiSquare(new double[][] {{20, 1}, {20, 1}}, true).applicable()).isFalse();
      assertThat(Statistics.chiSquare(new double[][] {{1, 0}, {10, 10}}, true).applicable()).isFalse();
      assertThat(Statistics.chiSquare(new double[][] {{10}, {10}}, true).applicable()).isFalse();
      assertThat(Statistics.chiSquare(new double[][] {{10, 10}, {10, -1}}, true).applicable()).isFalse();
      assertThat(Statistics.chiSquare(new double[][] {{10, 10}, {10}}, true).applicable()).isFalse();
   }

   /*******************************************************************************
    ** Verify descriptive references.
    *******************************************************************************/
   @Test
   void descriptiveReferences()
   {
      // scipy.stats.pearsonr([1,2,3,4,5], [2,1,4,3,6]).statistic
      assertThat(Statistics.pearson(new double[] {1, 2, 3, 4, 5}, new double[] {2, 1, 4, 3, 6})).isCloseTo(0.8219949365267865, within(1e-14));
      // scipy.special.rel_entr(q,p).sum() + scipy.special.rel_entr(p,q).sum(), p=[.2,.3,.5], q=[.3,.3,.4]
      assertThat(Statistics.psi(new double[] {20, 30, 50}, new double[] {30, 30, 40})).isCloseTo(0.06286086594223739, within(1e-14));
      assertThat(Statistics.psi(new double[] {0, 1}, new double[] {1, 0})).isFinite();
      assertThat(Statistics.psi(new double[] {0, 0}, new double[] {1, 1})).isNull();
      assertThat(Statistics.psi(new double[] {-1, 1}, new double[] {1, 1})).isNull();
      assertThat(Statistics.psi(new double[] {1}, new double[] {1, 1})).isNull();
      assertThat(Statistics.pearson(new double[] {1, 1}, new double[] {2, 3})).isNull();
      assertThat(Statistics.pearson(new double[] {1}, new double[] {2})).isNull();
      assertThat(Statistics.pearson(new double[] {1, Double.NaN}, new double[] {2, 3})).isNull();
   }

   /*******************************************************************************
    ** Verify distribution tails.
    *******************************************************************************/
   @Test
   void distributionTails()
   {
      // scipy.stats.t.cdf(0, 10), t.cdf(1, 1), t.cdf(-1, 1); scipy.stats.chi2.cdf(2, 4)
      assertThat(SpecialFunctions.studentTCdf(0, 10)).isEqualTo(.5);
      assertThat(SpecialFunctions.studentTCdf(1, 1)).isCloseTo(.75, within(1e-13));
      assertThat(SpecialFunctions.studentTCdf(-1, 1)).isCloseTo(.25, within(1e-13));
      assertThat(SpecialFunctions.chiSquareCdf(2, 4)).isCloseTo(.2642411176571153, within(1e-13));
      // scipy.special.betainc(2,5,.2), betainc(2,5,.8), betainc(.5,.5,.5), betainc(100,10,.02)
      assertThat(SpecialFunctions.beta(.2, 2, 5)).isCloseTo(.3446400000000002, within(1e-13));
      assertThat(SpecialFunctions.beta(.8, 2, 5)).isCloseTo(.9984, within(1e-13));
      assertThat(SpecialFunctions.beta(.5, .5, .5)).isCloseTo(.5, within(1e-13));
      assertThat(SpecialFunctions.beta(.02, 100, 10)).isCloseTo(4.514221366229434e-158, within(1e-169));
      assertThat(SpecialFunctions.beta(0, 2, 5)).isZero();
      assertThat(SpecialFunctions.beta(1, 2, 5)).isEqualTo(1);
      // scipy.special.gammaincc(2,1), gammaincc(2,8), gammaincc(.5,2), gammaincc(20,18)
      assertThat(SpecialFunctions.gammaQ(2, 1)).isCloseTo(.7357588823428847, within(1e-13));
      assertThat(SpecialFunctions.gammaQ(2, 8)).isCloseTo(.003019163651122605, within(1e-13));
      assertThat(SpecialFunctions.gammaQ(.5, 2)).isCloseTo(.04550026389635857, within(1e-13));
      assertThat(SpecialFunctions.gammaQ(20, 18)).isCloseTo(.6509161279807018, within(1e-13));
      assertThat(SpecialFunctions.gammaQ(2, 0)).isEqualTo(1);
   }
}
