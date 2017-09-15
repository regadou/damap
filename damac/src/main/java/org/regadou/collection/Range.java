package org.regadou.collection;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Range implements List<Number> {

   private double start, end, step;
   private Collection<Number> lazyCollection;

   public Range() {}

   public Range(Number start, Number end) {
      this(start, end, null);
   }

   public Range(Number start, Number end, Number step) {
      this.start = (start == null) ? 0 : start.doubleValue();
      this.end = (end == null) ? 0 : end.doubleValue();
      this.step = (step == null) ? 1 : step.doubleValue();
      validateStep();
   }

   public Range(Number[] a) {
      this((a == null) ? Collections.EMPTY_LIST : Arrays.asList(a));
   }

   public Range(Collection<Number> c) {
      lazyCollection = (c == null) ? Collections.EMPTY_LIST : c;
   }

   @Override
   public String toString() {
      checkLazyCollection();
      String fin = (Math.abs(step) == 1) ? "]" : " / "+step+"]";
      return "["+start+" -> "+end+fin;
   }

   public double getStart() {
      checkLazyCollection();
      return start;
   }

   public void setStart(double start) {
      this.start = start;
      validateStep();
   }

   public double getEnd() {
      checkLazyCollection();
      return end;
   }

   public void setEnd(double end) {
      checkLazyCollection();
      this.end = end;
      validateStep();
   }

   public double getStep() {
      checkLazyCollection();
      return step;
   }

   public void setStep(double step) {
      checkLazyCollection();
      this.step = step;
      validateStep();
   }

   @Override
   public int size() {
      checkLazyCollection();
      return (step == 0) ? 0 : (int)Math.round((end - start) / step) + 1;
   }

   @Override
   public boolean isEmpty() {
      checkLazyCollection();
      return step == 0;
   }

   @Override
   public boolean contains(Object o) {
      checkLazyCollection();
      return indexOf(o) >= 0;
   }

   @Override
   public Iterator<Number> iterator() {
      checkLazyCollection();
      return new RangeIterator(this);
   }

   @Override
   public Object[] toArray() {
      checkLazyCollection();
      int n = size();
      Number[] a = new Number[n];
      for (int i = 0; i < n; i++)
         a[i] = get(i);
      return a;
   }

   @Override
   public <T> T[] toArray(T[] a) {
      checkLazyCollection();
      int n = size();
      Class c = (a == null) ? Object.class : a.getClass().getComponentType();
      if (a == null || a.length < n)
         a = (T[])Array.newInstance(c, n);
      for (int i = 0; i < n; i++)
         a[i] = (T)get(i);
      return a;
   }

   @Override
   public boolean add(Number n) {
      checkLazyCollection();
      if (n == null)
         return false;
      double d = n.doubleValue();
      if (d < start) {
         start = d;
         return true;
      }
      else if (d > end) {
         end = d;
         return true;
      }
      else
         return false;
   }

   @Override
   public boolean remove(Object o) {
      throw new UnsupportedOperationException("Cannot remove an element from a range");
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      checkLazyCollection();
      for (Object e : c) {
         if (!contains(e))
            return false;
      }
      return true;
   }

   @Override
   public boolean addAll(Collection<? extends Number> c) {
      checkLazyCollection();
      boolean changed = false;
      for (Number n : c) {
         if (add(n))
            changed = true;
      }
      return changed;
   }

   @Override
   public boolean addAll(int index, Collection<? extends Number> c) {
      if (index != 0)
         throw new UnsupportedOperationException("Cannot set an element in a specific index");
      return addAll(c);
   }

   @Override
   public  boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException("Cannot remove elements from a range");
   }

   @Override
   public  boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException("Cannot remove elements from a range");
   }

   @Override
   public void clear() {
      throw new UnsupportedOperationException("Cannot clear elements from a range");
   }

   @Override
   public boolean equals(Object o) {
      checkLazyCollection();
      Iterator me, you;
      if (o instanceof Range) {
         Range r = (Range)o;
         return start == r.start && end == r.end && step == r.step;
      }

      int n = size();
      if (o == null)
         return n == 0;
      else if (o instanceof Collection) {
         Collection c = (Collection)o;
         if (c.size() != n)
            return false;
         you = c.iterator();
      }
      else if (o instanceof Object[]) {
         Object[] a = (Object[])o;
         if (a.length != n)
            return false;
         you = Arrays.asList(a).iterator();
      }
      else if (o.getClass().isArray()) {
         if (Array.getLength(o) != n)
            return false;
         Object[] a = new Object[n];
         for (int i = 0; i < n; i++)
            a[i] = Array.get(o, i);
         you = Arrays.asList(a).iterator();
      }
      else
         return size() == 1 && get(0).equals(o);

      me = iterator();
      while (me.hasNext()) {
         if (!me.next().equals(you.next()))
            return false;
      }
      return true;
   }

   @Override
   public Number get(int index) {
      checkLazyCollection();
      if (index >= 0 && index < size())
         return start + index*step;
      else
         throw new IndexOutOfBoundsException("No element at index "+index);
   }

   @Override
   public Number set(int index, Number n) {
      throw new UnsupportedOperationException("Cannot set an element in a specific index");
   }

   @Override
   public void add(int index, Number n) {
      if (index >= 0 && index < size())
         throw new UnsupportedOperationException("Cannot set an element in a specific index");
      add(n);
   }

   @Override
   public Number remove(int index) {
      throw new UnsupportedOperationException("Cannot remove an element from a range");
   }

   @Override
   public int indexOf(Object o) {
      checkLazyCollection();
      double n;
      if (o instanceof Number)
         n = ((Number)o).doubleValue();
      else if (o instanceof Boolean)
         n = (Boolean)o ? 1 : 0;
      else
         return -1;

      if (n < start || n > end)
         return -1;
      double pos = (n - start) / step;
      int index = (int)pos;
      return (index == pos) ? index : -1;
   }

   @Override
   public int lastIndexOf(Object o) {
      return indexOf(o);
   }

   @Override
   public ListIterator<Number> listIterator() {
      return new RangeIterator(this);
   }

   @Override
   public ListIterator<Number> listIterator(int index) {
      return new RangeIterator(this, index);
   }

   @Override
   public List<Number> subList(int fromIndex, int toIndex) {
      return new Range(fromIndex, toIndex, step);
   }

   private void validateStep() {
      if (  (this.start > this.end && this.step > 0)
         || (this.start < this.end && this.step < 0) )
         this.step = -this.step;
   }

   private void checkLazyCollection() {
      if (lazyCollection == null)
         return;
      step = 1;
      Iterator<Number> i = lazyCollection.iterator();
      for (int n = 0; i != null && i.hasNext(); n++) {
         switch (n) {
            case 0:
               start = i.next().doubleValue();
               break;
            case 1:
               end = i.next().doubleValue();
               break;
            case 2:
               step = i.next().doubleValue();
               break;
            default:
               i = null;
         }
      }
      validateStep();
      lazyCollection = null;
   }
}
