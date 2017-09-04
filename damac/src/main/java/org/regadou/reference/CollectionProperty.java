package org.regadou.reference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class CollectionProperty extends TypedProperty<Collection> {

   public static final List<String> SIZE_NAMES = Arrays.asList(new String[]{"size", "length", "count"});

   private String name;
   private Integer index;

   public CollectionProperty(Collection parent, Object key) {
      super(parent, Collection.class, parent.iterator().getClass(), "next");
      this.name = key.toString();
      if (!SIZE_NAMES.contains(name))
         index = Integer.parseInt(name);
   }

   public CollectionProperty(Collection parent, Object key, Class type) {
      super(parent, Collection.class, type);
      this.name = key.toString();
      if (!SIZE_NAMES.contains(name))
         index = Integer.parseInt(name);
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Object getValue() {
      Collection parent = getParent();
      if (index == null)
         return parent.size();
      if (index < 0 || index > parent.size())
         return null;
      if (parent instanceof List)
         return ((List)parent).get(index);
      Iterator i = parent.iterator();
      for (int n = 0; i.hasNext(); n++) {
         Object value = i.next();
         if (n == index)
            return value;
      }
      return null;
   }

   @Override
   public void setValue(Object value) {
      if (index == null || index < 0)
         return;
      Collection parent = getParent();
      while (index > parent.size())
         parent.add(null);
      if (parent.size() == index)
         parent.add(value);
      else if (parent instanceof List)
         ((List)parent).set(index, value);
      else {
         Class t = parent.getClass();
         List l = new ArrayList(parent);
         l.set(index, value);
         try {
            parent.clear();
            for (Object e : l)
               parent.add(e);
         }
         catch (Exception e) { throw new RuntimeException(e); }
      }
   }
}
