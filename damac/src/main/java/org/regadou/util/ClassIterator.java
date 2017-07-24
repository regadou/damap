package org.regadou.util;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ClassIterator implements Iterator<Class> {

   private Class currentClass;
   private Class[] interfaces;
   private int at;

   public ClassIterator(Class c) {
      currentClass = (c == null) ? Void.class : c;
   }

   public ClassIterator(Object obj) {
      currentClass = (obj == null) ? Void.class : obj.getClass();
   }

   @Override
   public boolean hasNext() {
      //TODO: each interface can extends parent interfaces
      if (interfaces != null) {
         if (at < interfaces.length)
            return true;
         interfaces = null;
         currentClass = getSuperClass(currentClass);
      }
      return currentClass != null;
   }

   @Override
   public Class next() {
      //TODO: each interface can extends parent interfaces
      if (interfaces != null) {
         if (at < interfaces.length)
            return interfaces[at++];
         interfaces = null;
         currentClass = getSuperClass(currentClass);
      }
      if (currentClass == null)
         throw new NoSuchElementException("No more element to iterate over");
      interfaces = currentClass.getInterfaces();
      at = 0;
      return currentClass;
   }

   private Class getSuperClass(Class src) {
      if (src == null)
         return null;
      else if (src.isArray()) {
         Class comp = src.getComponentType();
         //TODO: we should check if component is an interface and loop through all implemented interfaces
         if (comp == Object.class || comp.isPrimitive())
            return Object.class;
         else
            return Array.newInstance(comp.getSuperclass(), 0).getClass();
      }
      else
         return src.getSuperclass();
   }
}
