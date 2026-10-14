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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.kingsrook.qbits.dataanalysis.result.Metric;
import com.kingsrook.qbits.dataanalysis.result.ValueType;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;

/*******************************************************************************
 ** Display-ready values shared by metrics, chart labels and the home widget.
 *******************************************************************************/
public final class AnalysisFormatting
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private AnalysisFormatting()
   {
   }



   /*******************************************************************************
    ** Ratios remain numeric and bounded, with stable JSON precision.
    *******************************************************************************/
   public static BigDecimal ratio(BigDecimal value)
   {
      return (value.max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros());
   }



   /*******************************************************************************
    ** Plain numeric defaults are independent of the server locale.
    *******************************************************************************/
   public static String number(Number value, int decimals)
   {
      return (String.format(Locale.US, "%,." + decimals + "f", new BigDecimal(value.toString())));
   }



   /*******************************************************************************
    ** Types and length keys determine display without changing numeric raw values.
    *******************************************************************************/
   public static String scalar(String key, Serializable value, ValueType type)
   {
      if(value == null)
      {
         return (null);
      }
      if(type == ValueType.PERCENT)
      {
         BigDecimal percent = new BigDecimal(value.toString()).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
         return (percent.signum() == 0 || percent.compareTo(BigDecimal.valueOf(100)) == 0 ? percent.toBigInteger() + "%" : percent.toPlainString() + "%");
      }
      if(type == ValueType.DATE_TIME)
      {
         return (instant((Instant) value));
      }
      if(value instanceof Number n)
      {
         return (number(n, "meanLength".equals(key) ? 1 : type == ValueType.COUNT || key.endsWith("Length") ? 0 : 2));
      }
      return (value.toString());
   }



   /*******************************************************************************
    ** Display formats belong to the host field; fallback defaults preserve fractions.
    *******************************************************************************/
   public static String numeric(QFieldMetaData field, String key, BigDecimal value)
   {
      if(value == null)
      {
         return (null);
      }
      String format = field.getDisplayFormat();
      if(format != null && !format.isBlank() && !DisplayFormat.DEFAULT.equals(format))
      {
         Serializable displayValue = format.endsWith("d") ? value.toBigInteger() : value;
         return (QValueFormatter.formatValue(format, displayValue));
      }
      return (number(value, field.getType() == QFieldType.DECIMAL || "mean".equals(key) || "stddev".equals(key) ? 2 : 0));
   }



   /*******************************************************************************
    ** Decimal statistics retain ten significant digits; formatting uses the original.
    *******************************************************************************/
   public static Metric numericMetric(QFieldMetaData field, String key, BigDecimal value)
   {
      BigDecimal raw = value == null || field.getType() != QFieldType.DECIMAL ? value : value.round(new MathContext(10));
      return (ProfileSupport.metric(key, raw, ValueType.DECIMAL).withFormatted(numeric(field, key, value)));
   }



   /*******************************************************************************
    ** Use the same field formatter on both ends of every histogram interval.
    *******************************************************************************/
   public static String interval(QFieldMetaData field, BigDecimal low, BigDecimal high)
   {
      return (low == null ? "Below " + edge(field, high)
         : high == null ? edge(field, low) + " and above"
         : edge(field, low) + " – " + edge(field, high));
   }



   /*******************************************************************************
    ** Whole edges omit redundant cents; unformatted fractional edges stay distinct.
    *******************************************************************************/
   private static String edge(QFieldMetaData field, BigDecimal value)
   {
      String format = field.getDisplayFormat();
      int scale = Math.max(0, value.stripTrailingZeros().scale());
      if(format == null || format.isBlank() || DisplayFormat.DEFAULT.equals(format))
      {
         return (number(value, scale));
      }
      String formatted = numeric(field, "min", value);
      return (scale == 0 && DisplayFormat.CURRENCY.equals(format) && formatted.endsWith(".00")
         ? formatted.substring(0, formatted.length() - 3) : formatted);
   }



   /*******************************************************************************
    ** Instants remain UTC in raw values and display in the current session zone.
    *******************************************************************************/
   public static String instant(Instant value)
   {
      return (DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm", Locale.US).format(value.atZone(ValueUtils.getSessionOrInstanceZoneId())));
   }



   /*******************************************************************************
    ** Calendar duration avoids misleading fixed-length month and year arithmetic.
    *******************************************************************************/
   public static String duration(LocalDate first, LocalDate last)
   {
      Period period = Period.between(first, last);
      List<String> parts = new ArrayList<>();
      if(period.getYears() != 0)
      {
         parts.add(period.getYears() + (period.getYears() == 1 ? " year" : " years"));
      }
      if(period.getMonths() != 0)
      {
         parts.add(period.getMonths() + (period.getMonths() == 1 ? " month" : " months"));
      }
      if(period.getDays() != 0 || parts.isEmpty())
      {
         parts.add(period.getDays() + (period.getDays() == 1 ? " day" : " days"));
      }
      return (String.join(" ", parts));
   }



   /*******************************************************************************
    ** Calendar labels describe one bucket; the raw value remains its ISO start.
    *******************************************************************************/
   public static String dateBucket(Serializable start, AnalysisBackend.DateUnit unit, ZoneId zone)
   {
      if(unit == AnalysisBackend.DateUnit.DAY_OF_WEEK)
      {
         return (DayOfWeek.of(((Number) start).intValue()).getDisplayName(TextStyle.FULL, Locale.US));
      }
      if(unit == AnalysisBackend.DateUnit.HOUR_OF_DAY)
      {
         return (String.format(Locale.US, "%02d:00", ((Number) start).intValue()));
      }
      LocalDate date = ProfileSupport.day(start, zone);
      return (switch(unit)
      {
         case YEAR -> Integer.toString(date.getYear());
         case QUARTER -> date.getYear() + " Q" + ((date.getMonthValue() - 1) / 3 + 1);
         case MONTH -> DateTimeFormatter.ofPattern("MMM uuuu", Locale.US).format(date);
         case WEEK -> "Week of " + date;
         case HOUR -> DateTimeFormatter.ofPattern("uuuu-MM-dd HH:00", Locale.US).format(((Instant) start).atZone(zone));
         default -> date.toString();
      });
   }
}
