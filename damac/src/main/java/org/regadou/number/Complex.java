package org.regadou.number;

public class Complex extends Number {

   private double real;
   private double imaginary;

   public Complex(String txt) {
      int index = txt.indexOf(txt.toLowerCase());
      if (index < 0)
         real = Double.parseDouble(txt);
      else if (index == 0)
         imaginary = Double.parseDouble(txt.substring(1));
      else {
         int sign = 1, skip = 0;
         switch (txt.charAt(index-1)) {
            case '-':
               sign = -1;
            case '+':
               if (index == 1)
                  txt = "0"+txt;
               else
                  skip = 1;
         }
         real = Double.parseDouble(txt.substring(0, index-skip));
         imaginary = Double.parseDouble(txt.substring(index+1)) * sign;
      }
   }

   public Complex(Number n) {
      if (n instanceof Complex) {
         Complex c = (Complex)n;
         real = c.real;
         imaginary = c.imaginary;
      }
      else
         real = n.doubleValue();
   }

   public Complex(double real, double imaginary) {
      this.real = real;
      this.imaginary = imaginary;
   }

   @Override
   public String toString() {
      String sign = (imaginary < 0) ? "-" : "+";
      return real+sign+"i"+Math.abs(imaginary);
   }

   @Override
   public int intValue() {
      return (int)real;
   }

   @Override
   public long longValue() {
      return (long)real;
   }

   @Override
   public float floatValue() {
      return (float)real;
   }

   @Override
   public double doubleValue() {
      return real;
   }

   public double realValue() {
      return real;
   }

   public double imaginaryValue() {
      return imaginary;
   }
}
