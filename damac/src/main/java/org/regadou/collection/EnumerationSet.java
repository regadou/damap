package org.regadou.collection;

import java.util.AbstractSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class EnumerationSet<T> extends AbstractSet<T> {

   private Set<T> data = new LinkedHashSet();

   public EnumerationSet(Enumeration<T> e) {
      while (e.hasMoreElements())
         data.add(e.nextElement());
   }

   @Override
   public Iterator<T> iterator() {
      return data.iterator();
   }

   @Override
   public int size() {
      return data.size();
   }

}
