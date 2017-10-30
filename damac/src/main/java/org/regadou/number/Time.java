package org.regadou.number;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;

public class Time extends Number {

   public static final int MILLISECONDS_PER_DAY = 86400000;

   public static enum Measure { POSITION, DURATION }

   public static enum Unit {
      YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, MILLISECOND;

      public int getDuration() {
         switch (this) {
            case YEAR:
               return MILLISECONDS_PER_DAY * 365;
            case MONTH:
               return MILLISECONDS_PER_DAY * 30;
            case DAY:
               return MILLISECONDS_PER_DAY;
            case HOUR:
               return MILLISECONDS_PER_DAY / 24;
            case MINUTE:
               return 60000;
            case SECOND:
               return 1000;
            case MILLISECOND:
               return 1;
            default:
               throw new RuntimeException("Unknown time unit "+this);
         }
      }

      public int getValue(Date date) {
         switch (this) {
            case YEAR:
               return date.getYear();
            case MONTH:
               return date.getMonth();
            case DAY:
               return date.getDate();
            case HOUR:
               return date.getHours();
            case MINUTE:
               return date.getMinutes();
            case SECOND:
               return date.getSeconds();
            case MILLISECOND:
               return (int)(date.getTime() % 1000);
            default:
               throw new RuntimeException("Unknown time unit "+this);
         }
      }

      public Integer getMinimumValue(Date date) {
         switch (this) {
            case YEAR:
               return null;
            case MONTH:
               return 0;
            case DAY:
               return 1;
            case HOUR:
               return 0;
            case MINUTE:
               return 0;
            case SECOND:
               return 0;
            case MILLISECOND:
               return 0;
            default:
               throw new RuntimeException("Unknown time unit "+this);
         }
      }

      public float getCorrectionFactor() {
         switch (this) {
            case YEAR:
               return 1.2425f;
            case MONTH:
               return 1.436875f;
            case DAY:
               return 1.0006643835616438f;
            default:
               return 1;
         }
      }
   }

   private Long start;
   private Long end;
   private Long duration;
   private Measure measure;
   private String label;

   public Time() {
      start = System.currentTimeMillis();
      measure = Measure.POSITION;
   }

   public Time(Date d) {
      start = d.getTime();
      measure = Measure.POSITION;
   }

   public Time(Calendar c) {
      start = c.getTime().getTime();
      measure = Measure.POSITION;
   }

   public Time(Duration d) {
      duration = d.toMillis();
      measure = Measure.DURATION;
   }

   public Time(CharSequence t) {
      String s = t.toString().trim().toLowerCase();
      int i = s.indexOf('t');
      if (i < 0) {
         i = s.indexOf(' ');
         if (i < 0)
            i = s.indexOf('\t');
      }
      String[] date, time;
      if (i < 0) {
         i = s.indexOf('-');
         int i2 = s.indexOf(':');
         if (i2 < 0) {
            date = new String[]{s};
            time = null;
         }
         else if (i < 0 || (i2 >= 0 && i2 < i)) {
            date = null;
            time = s.split(":");
         }
         else {
            date = s.split("-");
            time = null;
         }
      }
      else {
         date = s.substring(0, i).trim().split("-");
         time = s.substring(i+1).trim().split(":");
      }

      //TODO: get the timestamp from date and/or time
   }

   @Override
   public String toString() {
      if (label == null)
         label = (measure == Measure.DURATION) ? duration+"ms" : new Timestamp(start).toString();
      return label;
   }

   public Long getStart() {
      if (start == null) {
         if (end == null) {

         }
         else {
            if (duration == null)
               duration = getPeriodDuration(end);
            start = end - duration;
         }
      }
      return start;
   }

   public Long getEnd() {
      if (end == null) {
         if (start == null) {

         }
         else {
            if (duration == null)
               duration = getPeriodDuration(start);
            end = start + duration;
         }
      }
      return end;
   }

   public Long getDuration() {
      if (duration == null) {
         if (start == null) {
            if (end != null)
               duration = getPeriodDuration(end);
         }
         else if (end == null)
            duration = getPeriodDuration(start);
         else
            duration = end - start;
      }
      return duration;
   }

   public Measure getMeasure() {
      return measure;
   }

   @Override
   public int intValue() {
      return getNumber().intValue();
   }

   @Override
   public long longValue() {
      return getNumber();
   }

   @Override
   public float floatValue() {
      return getNumber().floatValue();
   }

   @Override
   public double doubleValue() {
      return getNumber().doubleValue();
   }

   private Long getNumber() {
      return (start == null) ? ((duration == null) ? 0 : duration) : start;
   }

   private long getPeriodDuration(long timestamp) {
      Date date = new Date(timestamp);
      Unit[] units = Unit.values();
      for (int u = units.length-1; u >= 0; u--) {
         Unit unit = units[u];
         Integer minimum = unit.getMinimumValue(date);
         if (minimum == null || unit.getValue(date) > minimum)
            return Math.round(unit.getDuration() * unit.getCorrectionFactor());
      }
      throw new RuntimeException("Could not find period duration for "+date);
   }
}
