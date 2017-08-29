package org.regadou.reference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.regadou.damai.Property;

public class CollectionProperty implements Property {

   public static final List<String> SIZE_NAMES = Arrays.asList(new String[]{"size", "length", "count"});

   public static Class getCollectionType(Collection collection) {
      try { return collection.getClass().getMethod("get", Integer.TYPE).getReturnType(); }
      catch (NoSuchMethodException|SecurityException e) { throw new RuntimeException(e); }
   }

   private Collection parent;
   private String name;
   private Integer index;
   private Class type;

   public CollectionProperty(Collection parent, Object key, Class type) {
      this.parent = parent;
      this.name = key.toString();
      this.type = (type == null) ? getCollectionType(parent) : type;
      if (!SIZE_NAMES.contains(name))
         index = Integer.parseInt(name);
   }

   @Override
   public Object getParent() {
      return parent;
   }

   @Override
   public Class getParentType() {
      return parent.getClass();
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Object getValue() {
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
   public Class getType() {
      return type;
   }

   @Override
   public void setValue(Object value) {
      if (index == null || index < 0)
         return;
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
         try { parent = (Collection) t.getConstructor(Collection.class).newInstance(l); }
         catch (Exception e) { throw new RuntimeException(e); }
      }
   }
}
