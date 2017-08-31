package org.regadou.factory;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.regadou.damai.Converter;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.reference.CollectionProperty;

public class CollectionPropertyFactory implements PropertyFactory<Collection> {

   private Converter converter;

   public CollectionPropertyFactory(Converter converter) {
      this.converter = converter;
   }

   @Override
   public Property getProperty(Collection collection, String name) {
      if (CollectionProperty.SIZE_NAMES.contains(name))
         return new CollectionProperty(collection, name, Integer.TYPE);
      try {
         int index = Integer.parseInt(name);
         if (index >= 0 && index < collection.size())
            return new CollectionProperty(collection, name, null);
      }
      catch (Exception e) {}
      return null;
   }

   @Override
   public String[] getProperties(Collection collection) {
      Set<String> names = new TreeSet<>();
      names.add(CollectionProperty.SIZE_NAMES.get(0));
      int size = collection.size();
      for (int i = 0; i < size; i++)
         names.add(String.valueOf(i));
      return names.toArray(new String[names.size()]);
   }

   @Override
   public Property addProperty(Collection collection, String name, Object value) {
      try {
         int index = (name == null) ? collection.size() : Integer.parseInt(name);
         if (index >= collection.size()) {
            Property p = new CollectionProperty(collection, index, null);
            p.setValue(converter.convert(value, p.getType()));
            return p;
         }
      }
      catch (Exception e) {}
      return null;
   }

   @Override
   public boolean removeProperty(Collection collection, String name) {
      try {
         int index = Integer.parseInt(name);
         int size = collection.size();
         if (index >= 0 && index < size) {
            if (collection instanceof List)
               ((List)collection).remove(index);
            else {
               Iterator it = collection.iterator();
               for (int i = 0; i <= index; i++)
                  it.next();
               it.remove();
            }
         }
         return size > collection.size();
      }
      catch (Exception e) {}
      return false;
   }
}
