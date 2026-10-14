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

package com.kingsrook.qbits.dataanalysis.schedule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.TimeZone;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.quartz.CronExpression;

/*******************************************************************************
 ** Calendar schedules use local wall time; cron uses Quartz's existing parser.
 *******************************************************************************/
public final class ScheduleTiming
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private ScheduleTiming()
   {
   }

   /*******************************************************************************
    ** Strictly after now; missing monthly dates clamp to that month's last day.
    ** Java resolves spring gaps forward and chooses the first autumn occurrence.
    *******************************************************************************/
   public static Instant next(QRecord row, Instant now) throws QBadRequestException
   {
      try
      {
         ZoneId zone = ZoneId.of(row.getValueString("time_zone") == null ? "UTC" : row.getValueString("time_zone"));
         String cron = row.getValueString("cron");
         if(cron != null && !cron.isBlank())
         {
            CronExpression expression = new CronExpression(cron);
            expression.setTimeZone(TimeZone.getTimeZone(zone));
            Date next = expression.getNextValidTimeAfter(Date.from(now));
            if(next == null)
            {
               throw new IllegalArgumentException("Cron has no future occurrence.");
            }
            return (next.toInstant());
         }
         LocalTime time = LocalTime.parse(row.getValueString("time"));
         String frequency = row.getValueString("frequency");
         Integer day = row.getValueInteger("day");
         LocalDate date = now.atZone(zone).toLocalDate();
         for(int attempt = 0; attempt < 3; attempt++)
         {
            LocalDate candidate = switch(frequency)
            {
               case "DAILY" -> date;
               case "WEEKLY" -> date.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.of(day)));
               case "MONTHLY" -> date.withDayOfMonth(Math.min(requireDay(day), date.lengthOfMonth()));
               default -> throw new IllegalArgumentException("Choose DAILY, WEEKLY or MONTHLY.");
            };
            Instant instant = candidate.atTime(time).atZone(zone).toInstant();
            if(instant.isAfter(now))
            {
               return (instant);
            }
            date = switch(frequency)
            {
               case "WEEKLY" -> candidate.plusWeeks(1);
               case "MONTHLY" -> candidate.plusMonths(1).withDayOfMonth(1);
               default -> candidate.plusDays(1);
            };
         }
         throw new IllegalArgumentException("No future occurrence.");
      }
      catch(Exception e)
      {
         throw new QBadRequestException("Invalid schedule: choose a valid frequency/day, local time, time zone or Quartz cron.");
      }
   }

   /*******************************************************************************
    ** Month days are one-based and never silently wrap into another month.
    *******************************************************************************/
   private static int requireDay(Integer day)
   {
      if(day == null || day < 1 || day > 31)
      {
         throw new IllegalArgumentException("Monthly day must be 1 to 31.");
      }
      return (day);
   }
}
