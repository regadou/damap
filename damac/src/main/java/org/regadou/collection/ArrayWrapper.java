package org.regadou.collection;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.StringJoiner;

public class ArrayWrapper<T> extends AbstractList<T> {

   private Object array;
   private int length;

   public ArrayWrapper(Object obj) {
      length = Array.getLength(obj);
      array = obj;
   }

   @Override
   public String toString() {
      StringJoiner joiner = new StringJoiner(", ", "[", "]");
      for (int i = 0; i < length; i++)
         joiner.add(String.valueOf(Array.get(array, i)));
      return joiner.toString();
   }

   @Override
   public T get(int index) {
      return (T)Array.get(array, index);
   }

   @Override
   public T set(int index, T value) {
      T old = (T)Array.get(array, index);
      Array.set(array, index, value);
      return old;
   }

   @Override
   public int size() {
      return length;
   }
}
