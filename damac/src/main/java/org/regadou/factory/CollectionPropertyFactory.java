package org.regadou.factory;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.reference.CollectionProperty;

public class CollectionPropertyFactory implements PropertyFactory<Collection> {

   @Override
   public Property getProperty(Collection value, String name) {
      if (CollectionProperty.SIZE_NAMES.contains(name) || CollectionProperty.SUBTYPE_NAME.contains(name))
         return new CollectionProperty(value, name);
      try {
         int index = Integer.parseInt(name);
         if (index >= 0 && index < value.size())
            return new CollectionProperty(value, name);
      }
      catch (Exception e) {}
      return null;
   }

   @Override
   public String[] getProperties(Collection value) {
      Set<String> names = new TreeSet<>();
      names.add(CollectionProperty.SIZE_NAMES.get(0));
      names.add(CollectionProperty.SUBTYPE_NAME);
      int size = value.size();
      for (int i = 0; i < size; i++)
         names.add(String.valueOf(i));
      return names.toArray(new String[names.size()]);
   }
}
