package org.regadou.util;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayIterator<T> implements Iterator<T> {

   private Object array;
   private int length;
   private int at;

   public ArrayIterator(Object obj) {
      length = Array.getLength(obj);
      array = obj;
   }

   @Override
   public boolean hasNext() {
      return at < length;
   }

   @Override
   public T next() {
      if (at >= length)
         throw new NoSuchElementException("Maximum of "+length+" elements have been reached");
      try { return (T)Array.get(array, at);}
      finally { at++; }
   }
}
