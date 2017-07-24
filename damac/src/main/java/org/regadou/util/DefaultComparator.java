package org.regadou.util;

import java.util.Comparator;

public class DefaultComparator implements Comparator {

   @Override
   public int compare(Object o1, Object o2) {
      String t1 = (o1 == null) ? "" : o1.toString();
      String t2 = (o2 == null) ? "" : o2.toString();
      return t1.compareTo(t2);
   }
}
