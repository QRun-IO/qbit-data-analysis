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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisBackend;
import com.kingsrook.qbits.dataanalysis.engine.AnalysisFormatting;
import com.kingsrook.qbits.dataanalysis.engine.MetricLabels;
import com.kingsrook.qbits.dataanalysis.engine.NiceBins;
import com.kingsrook.qbits.dataanalysis.engine.ProfileSupport;
import com.kingsrook.qbits.dataanalysis.result.ValueType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*******************************************************************************
 ** Public presentation contracts, including precision and calendar edge cases.
 *******************************************************************************/
class AnalysisFormattingTest
{
   /*******************************************************************************
    ** Raw ratios remain numeric while display strings are ready for a CLI.
    *******************************************************************************/
   @Test
   void numbersPercentagesAndLengths()
   {
      assertThat(ProfileSupport.metric("rows", 12498L, ValueType.COUNT).getFormatted()).isEqualTo("12,498");
      for(String key : List.of("uniqueness", "completeness", "share"))
      {
         var metric = ProfileSupport.metric(key, new BigDecimal("0.987654321"), ValueType.PERCENT);
         assertThat(metric.getValue()).isEqualTo(new BigDecimal("0.987654"));
         assertThat(metric.getFormatted()).isEqualTo("98.8%");
         assertThat(metric.getValueType()).isEqualTo(ValueType.PERCENT);
      }
      assertThat(ProfileSupport.metric("share", BigDecimal.ONE, ValueType.PERCENT).getFormatted()).isEqualTo("100%");
      assertThat(ProfileSupport.metric("share", BigDecimal.ZERO, ValueType.PERCENT).getFormatted()).isEqualTo("0%");
      assertThat(ProfileSupport.share(1, 3)).isEqualByComparingTo("0.333333");
      assertThat(ProfileSupport.share(0, 0)).isZero();
      assertThat(ProfileSupport.metric("meanLength", new BigDecimal("12.345"), ValueType.DECIMAL).getFormatted()).isEqualTo("12.3");
      assertThat(ProfileSupport.metric("minLength", BigDecimal.TEN, ValueType.DECIMAL).getFormatted()).isEqualTo("10");
      assertThat(ProfileSupport.metric("max", null, ValueType.DECIMAL).getFormatted()).isNull();
      assertThatThrownBy(() -> MetricLabels.label("unknown", ValueType.COUNT)).isInstanceOf(NullPointerException.class).hasMessageContaining("Missing metric label");
   }



   /*******************************************************************************
    ** Host currency and custom display formats take precedence over numeric defaults.
    *******************************************************************************/
   @Test
   void fieldFormatsAndRawPrecision()
   {
      QFieldMetaData decimal = new QFieldMetaData("amount", QFieldType.DECIMAL);
      assertThat(AnalysisFormatting.numeric(decimal, "min", new BigDecimal("81753.32"))).isEqualTo("81,753.32");
      decimal.withDisplayFormat(DisplayFormat.CURRENCY);
      assertThat(AnalysisFormatting.numeric(decimal, "min", new BigDecimal("81753.32"))).isEqualTo("$81,753.32");
      var metric = AnalysisFormatting.numericMetric(decimal, "mean", new BigDecimal("125916.45167830173"));
      assertThat(metric.getValue()).isEqualTo(new BigDecimal("125916.4517"));
      assertThat(metric.getFormatted()).isEqualTo("$125,916.45");
      assertThat(AnalysisFormatting.numericMetric(decimal, "max", null).getValue()).isNull();
      QFieldMetaData integer = new QFieldMetaData("quantity", QFieldType.INTEGER);
      assertThat(AnalysisFormatting.numeric(integer, "max", new BigDecimal("12498"))).isEqualTo("12,498");
      assertThat(AnalysisFormatting.numeric(integer, "mean", new BigDecimal("12.345"))).isEqualTo("12.35");
      assertThat(AnalysisFormatting.numeric(integer, "stddev", new BigDecimal("12.345"))).isEqualTo("12.35");
      assertThat(AnalysisFormatting.numeric(integer.withDisplayFormat(DisplayFormat.DECIMAL1_COMMAS), "mean", new BigDecimal("12.345"))).isEqualTo("12.3");
      QFieldMetaData large = new QFieldMetaData("large", QFieldType.LONG).withDisplayFormat(DisplayFormat.COMMAS);
      assertThat(AnalysisFormatting.numeric(large, "sum", new BigDecimal("9000000000"))).isEqualTo("9,000,000,000");
   }



   /*******************************************************************************
    ** Nice boundaries remain exact at small scales, negative values and constants.
    *******************************************************************************/
   @Test
   void niceEdgesAndLabels()
   {
      for(List<String> range : List.of(List.of("25317.13", "65781.79"), List.of("-0.0093", "0.0357"), List.of("42", "42"), List.of("0", "0"), List.of("-9", "905")))
      {
         BigDecimal low = new BigDecimal(range.getFirst());
         BigDecimal high = new BigDecimal(range.getLast());
         NiceBins bins = NiceBins.covering(low, high);
         assertThat(bins.count()).isBetween(10, 25);
         assertThat(bins.low()).isLessThanOrEqualTo(low);
         assertThat(bins.high()).isGreaterThanOrEqualTo(high);
         BigDecimal step = bins.high().subtract(bins.low()).divide(BigDecimal.valueOf(bins.count())).stripTrailingZeros();
         assertThat(step.scaleByPowerOfTen(step.scale() - step.precision() + 1).toPlainString()).isIn("1", "2", "2.5", "5");
         assertThat(bins.low().remainder(step)).isZero();
         assertThat(bins.high().remainder(step)).isZero();
      }
      QFieldMetaData field = new QFieldMetaData("amount", QFieldType.INTEGER);
      assertThat(AnalysisFormatting.interval(field, new BigDecimal("60000"), new BigDecimal("65000"))).isEqualTo("60,000 – 65,000");
      assertThat(AnalysisFormatting.interval(field, null, new BigDecimal("25000"))).isEqualTo("Below 25,000");
      assertThat(AnalysisFormatting.interval(field, new BigDecimal("65000"), null)).isEqualTo("65,000 and above");
      assertThat(AnalysisFormatting.interval(field, new BigDecimal("1.25"), new BigDecimal("1.5"))).isEqualTo("1.25 – 1.5");
      field.withDisplayFormat(DisplayFormat.CURRENCY);
      assertThat(AnalysisFormatting.interval(field, new BigDecimal("60000"), new BigDecimal("65000"))).isEqualTo("$60,000 – $65,000");
   }



   /*******************************************************************************
    ** Every supported unit has a human label without replacing its ISO coordinate.
    *******************************************************************************/
   @Test
   void dateUnitsAndDurations()
   {
      ZoneId zone = ZoneId.of("America/Chicago");
      LocalDate date = LocalDate.of(2024, 3, 4);
      Map<AnalysisBackend.DateUnit, String> labels = Map.of(
         AnalysisBackend.DateUnit.YEAR, "2024", AnalysisBackend.DateUnit.QUARTER, "2024 Q1", AnalysisBackend.DateUnit.MONTH, "Mar 2024",
         AnalysisBackend.DateUnit.WEEK, "Week of 2024-03-04", AnalysisBackend.DateUnit.DAY, "2024-03-04");
      labels.forEach((unit, label) -> assertThat(AnalysisFormatting.dateBucket(date, unit, zone)).isEqualTo(label));
      assertThat(AnalysisFormatting.dateBucket(Instant.parse("2024-03-04T19:00:00Z"), AnalysisBackend.DateUnit.HOUR, zone)).isEqualTo("2024-03-04 13:00");
      for(int i = 1; i <= 7; i++)
      {
         assertThat(AnalysisFormatting.dateBucket(i, AnalysisBackend.DateUnit.DAY_OF_WEEK, zone))
            .isEqualTo(List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").get(i - 1));
      }
      for(int i = 0; i < 24; i++)
      {
         assertThat(AnalysisFormatting.dateBucket(i, AnalysisBackend.DateUnit.HOUR_OF_DAY, zone)).isEqualTo(String.format(java.util.Locale.US, "%02d:00", i));
      }
      assertThat(AnalysisFormatting.duration(date, date.plusYears(48).plusMonths(2))).isEqualTo("48 years 2 months");
      assertThat(AnalysisFormatting.duration(date, date.plusDays(12))).isEqualTo("12 days");
      assertThat(AnalysisFormatting.duration(date, date.plusYears(1).plusMonths(1).plusDays(1))).isEqualTo("1 year 1 month 1 day");
      assertThat(AnalysisFormatting.duration(date, date)).isEqualTo("0 days");
      assertThat(ProfileSupport.metric("min", date, ValueType.DATE).getLabel()).isEqualTo("Earliest");
      assertThat(ProfileSupport.metric("max", date, ValueType.DATE).getLabel()).isEqualTo("Latest");
      assertThat(ProfileSupport.metric("min", date, ValueType.DATE).getFormatted()).isEqualTo("2024-03-04");
   }
}
