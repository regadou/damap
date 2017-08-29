package org.regadou.factory;

import java.lang.reflect.Array;
import java.util.Set;
import java.util.TreeSet;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.reference.ArrayProperty;

public class ArrayPropertyFactory implements PropertyFactory {

   @Override
   public Property getProperty(Object value, String name) {
      if (ArrayProperty.LENGTH_NAMES.contains(name))
         return new ArrayProperty(value, name);
      try {
         int index = Integer.parseInt(name);
         if (index >= 0 && index < Array.getLength(value))
            return new ArrayProperty(value, name);
      }
      catch (Exception e) {}
      return null;
   }

   @Override
   public String[] getProperties(Object value) {
      Set<String> names = new TreeSet<>();
      names.add(ArrayProperty.LENGTH_NAMES.get(0));
      int length = Array.getLength(value);
      for (int i = 0; i < length; i++)
         names.add(String.valueOf(i));
      return names.toArray(new String[names.size()]);
   }

   @Override
   public Property addProperty(Object parent, String name, Object value) {
      return null;
   }

   @Override
   public boolean removeProperty(Object parent, String name) {
      return false;
   }
}
