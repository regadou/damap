package org.regadou.number;

public class Probability extends Number {

   private static float base = 100;
   private static String prefix = "";
   private static String suffix = "%";
   private static float minimumTruthValue = 0.5f;

   private Float value;

   public Probability(String txt) {

      if (!prefix.isEmpty() && txt.startsWith(prefix))
         value = new Float(txt.substring(prefix.length()))/base;
      else if (!suffix.isEmpty() && txt.endsWith(suffix))
         value = new Float(txt.substring(0,txt.length()-suffix.length()))/base;
      else
         value = new Float(txt);
      if (value < 0 || value > 1)
         throw new RuntimeException("Invalid probability value: "+txt);
   }

   public Probability(Number n) {
      value = n.floatValue();
      if (value < 0 || value > 1)
         throw new RuntimeException("Invalid probability value: "+n);
   }

   public Probability(boolean v) {
      value = v ? 1f : 0f;
   }

   @Override
   public String toString() {
      return (value*100)+"%";
   }

   @Override
   public int intValue() {
      return value.intValue();
   }

   @Override
   public long longValue() {
      return value.longValue();
   }

   @Override
   public float floatValue() {
      return value.floatValue();
   }

   @Override
   public double doubleValue() {
      return value.doubleValue();
   }

   public boolean toBoolean() {
      return value >= minimumTruthValue;
   }
}
