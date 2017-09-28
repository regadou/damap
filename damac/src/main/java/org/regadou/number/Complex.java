package org.regadou.number;

public class Complex extends Number {

   private double real;
   private double imaginary;

   public Complex(String txt) {

   }

   @Override
   public String toString() {
      String sign = (imaginary < 0) ? "-" : "+";
      return real+sign+"i"+Math.abs(imaginary);
   }

   @Override
   public int intValue() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public long longValue() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public float floatValue() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public double doubleValue() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }
}
