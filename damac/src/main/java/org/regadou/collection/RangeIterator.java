package org.regadou.collection;

import java.util.ListIterator;
import java.util.NoSuchElementException;

public class RangeIterator implements ListIterator<Number> {

   private Range range;
   private int start, current;

   public RangeIterator(Range parent) {
      this(parent, 0);
   }

   public RangeIterator(Range parent, int index) {
      this.range = parent;
      this.start = index;
   }

   @Override
   public boolean hasNext() {
      return current < range.size();
   }

   @Override
   public Number next() {
      if (current >= range.size())
         throw new NoSuchElementException("End of range has been reached");
      Number n = range.get(current);
      current++;
      return n;
   }

   @Override
   public boolean hasPrevious() {
      return current >= start;
   }

   @Override
   public Number previous() {
      if (current >= range.size())
         throw new NoSuchElementException("Begining of range has been reached");
      Number n = range.get(current);
      current--;
      return n;
   }

   @Override
   public int nextIndex() {
      return current + 1;
   }

   @Override
   public int previousIndex() {
      return current - 1;
   }

   @Override
   public void remove() {
      range.remove(current);
   }

   @Override
   public void set(Number n) {
      range.set(current, n);
   }

   @Override
   public void add(Number n) {
      range.add(current, n);
   }
}
