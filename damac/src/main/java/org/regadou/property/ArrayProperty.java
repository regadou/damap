package org.regadou.property;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import org.regadou.damai.Property;

public class ArrayProperty<P,T> implements Property<P,T> {

   public static final List<String> LENGTH_NAMES = Arrays.asList(new String[]{"length", "size", "count"});

   private P parent;
   private String name;
   private Integer index;
   private Class type;

   public ArrayProperty(P parent, Object key) {
      this.parent = parent;
      name = key.toString();
      type = parent.getClass().getComponentType();
      if (!LENGTH_NAMES.contains(name))
         index = Integer.parseInt(name);
   }

   @Override
   public String toString() {
      return name+"@"+parent;
   }

   @Override
   public P getParent() {
      return parent;
   }

   @Override
   public Class getParentType() {
      return type.isPrimitive() ? parent.getClass() : Object[].class;
   }

   @Override
   public String getId() {
      return name;
   }

   @Override
   public T getValue() {
      if (index == null)
         return (T)((Integer)Array.getLength(parent));
      else if (index >= 0 && index < Array.getLength(parent))
         return (T)Array.get(parent, index);
      return null;
   }

   @Override
   public Class<T> getType() {
      return type;
   }

   @Override
   public void setValue(T value) {
      if (index != null && index >= 0 && index < Array.getLength(parent))
         Array.set(parent, index, value);
   }
}
