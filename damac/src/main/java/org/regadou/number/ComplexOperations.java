package org.regadou.number;

public class ComplexOperations {

   private ComplexOperations() {}

   public static Complex add(Complex a, Complex b) {
      double real = a.realValue() + b.realValue();
      double imag = a.imaginaryValue() + b.imaginaryValue();
      return new Complex(real, imag);
   }

   public static Complex subtract(Complex a, Complex b) {
      double real = a.realValue() - b.realValue();
      double imag = a.imaginaryValue() - b.imaginaryValue();
      return new Complex(real, imag);
   }

   public static Complex multiply(Complex a, Complex b) {
      double real = a.realValue() * b.realValue() - a.imaginaryValue() * b.imaginaryValue();
      double imag = a.realValue() * b.imaginaryValue() + a.imaginaryValue() * b.realValue();
      return new Complex(real, imag);
   }

   public static Complex divide(Complex a, Complex b) {
      return multiply(a, reciprocal(b));
   }

   public static Complex scale(Complex n, double alpha) {
      return new Complex(alpha * n.realValue(), alpha * n.imaginaryValue());
   }

   public static Complex conjugate(Complex n) {
      return new Complex(n.realValue(), -n.imaginaryValue());
   }

   public static Complex reciprocal(Complex n) {
      double scale = n.realValue() * n.realValue() + n.imaginaryValue() * n.imaginaryValue();
      return new Complex(n.realValue() / scale, -n.imaginaryValue() / scale);
   }

   public static Complex exp(Complex n) {
      return new Complex(Math.exp(n.realValue()) * Math.cos(n.imaginaryValue()), Math.exp(n.realValue()) * Math.sin(n.imaginaryValue()));
   }

   public static Complex sin(Complex n) {
      return new Complex(Math.sin(n.realValue()) * Math.cosh(n.imaginaryValue()), Math.cos(n.realValue()) * Math.sinh(n.imaginaryValue()));
   }

   public static Complex cos(Complex n) {
      return new Complex(Math.cos(n.realValue()) * Math.cosh(n.imaginaryValue()), -Math.sin(n.realValue()) * Math.sinh(n.imaginaryValue()));
   }

   public static Complex tan(Complex n) {
      return divide(sin(n), cos(n));
   }

   public static double abs(Complex n) {
      return Math.hypot(n.realValue(), n.imaginaryValue());
   }

   public static double phase(Complex n) {
      return Math.atan2(n.realValue(), n.imaginaryValue());
   }
}
