package org.regadou.reference;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.regadou.damai.Property;

public class CollectionProperty implements Property {

   public static final List<String> lengthNames = Arrays.asList(new String[]{"length", "size", "count"});

   private Collection collection;
   private Object array;
   private String name;
   private Supplier getter;
   private Consumer setter;
   private int index;

   public CollectionProperty(Collection parent, Object key) {
      collection = (parent == null) ? new ArrayList() : parent;
      setNameAndFunctions(key);
   }

   public CollectionProperty(Object[] parent, Object key) {
      array = (parent == null) ? new Object[0] : parent;
      setNameAndFunctions(key);
   }

   public CollectionProperty(Object parent, Object key) {
      if (parent == null)
         parent = new Object[0];
      else if (!parent.getClass().isArray())
         parent = new Object[]{parent};
      array = parent;
   }

   @Override
   public Object getParent() {
      return (collection == null) ? array : collection;
   }

   @Override
   public Class getParentType() {
      return (array != null) ? array.getClass() : Collection.class;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Object getValue() {
      return (getter == null) ? null : getter.get();
   }

   @Override
   public Class getType() {
      Object value = getValue();
      return (value == null) ? Void.class : value.getClass();
   }

   @Override
   public void setValue(Object value) {
      if (setter != null)
         setter.accept(value);
   }

   private void setNameAndFunctions(Object key) {
      String name = (key == null) ? null : key.toString();
      if (name != null) {
         if (lengthNames.contains(name)) {
            if (collection != null)
               getter = () -> collection.size();
            else
               getter = () -> Array.getLength(array);
         }
         else if (setIndex(key)) {
            if (collection != null) {
               getter = collectionGetter;
               setter = collectionSetter;
            }
            else {
               getter = () -> Array.get(array, index);
               setter = value -> Array.set(array, index, value);
            }
         }
      }
   }

   private boolean setIndex(Object key) {
      int n;
      if (key instanceof Number)
         n = ((Number)key).intValue();
      else {
         try { n = new Double(key.toString()).intValue(); }
         catch (Exception e) { return false; }
      }
      if (n < 0)
         return false;
      if (array != null && n >= Array.getLength(array))
         return false;
      index = n;
      return true;
   }

   private Supplier collectionGetter = () -> {
      if (index >= collection.size())
         return null;
      if (collection instanceof List)
         return ((List)collection).get(index);
      Iterator i = collection.iterator();
      for (int n = 0; i.hasNext(); n++) {
         Object value = i.next();
         if (n == index)
            return value;
      }
      return null;
   };

   private Consumer collectionSetter = value -> {
      while (collection.size() < index-1)
         collection.add(null);
      if (collection.size() == index-1)
         collection.add(value);
      else if (collection instanceof List)
         ((List)collection).set(index, value);
      else {
         Class t = collection.getClass();
         List l = new ArrayList(collection);
         l.set(index, value);
         try { collection = (Collection) t.getConstructor(Collection.class).newInstance(l); }
         catch (Exception e) { throw new RuntimeException(e); }
      }
   };
}
