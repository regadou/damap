package org.regadou.factory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.regadou.damai.Action;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.PropertyManager;
import org.regadou.property.GenericProperty;
import org.regadou.collection.ClassIterator;

public class GenericPropertyFactory implements PropertyFactory {

   private static final List<String> GENERIC_PROPERTIES = Arrays.asList(
      "type", "class", "properties"
   );

   private static final List<Class> BASIC_TYPES = Arrays.asList(
      Void.class, Number.class, Boolean.class, Action.class,
      CharSequence.class, Property.class, Class.class,
      Collection.class, Map.class
   );

   private static final String[] TYPE_LEVELS = {
      "empty", "numeric", "numeric", "action",
      "text", "perception", "type",
      "group", "entity"
   };

   private PropertyManager propertyManager;

   public GenericPropertyFactory(PropertyManager propertyManager) {
      this.propertyManager = propertyManager;
   }

   @Override
   public Property getProperty(Object value, String name) {
      switch (name) {
         case "type":
            return new GenericProperty(value, "type", getTypeLevel(getType(value)), true);
         case "class":
            return new GenericProperty(value, "class", getType(value).getName(), true);
         case "properties":
            return new GenericProperty(value, "properties", getProperties(value), true);
         default:
            return null;
      }
   }

   @Override
   public String[] getProperties(Object value) {
      Set<String> names = new TreeSet<>(GENERIC_PROPERTIES);
      PropertyFactory factory = propertyManager.getPropertyFactory(getType(value));
      if (factory != null)
         names.addAll(Arrays.asList(factory.getProperties(value)));
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

   private String getTypeLevel(Class type) {
      ClassIterator it = new ClassIterator(type);
      while (it.hasNext()) {
         int index = BASIC_TYPES.indexOf(it.next());
         if (index >= 0)
            return TYPE_LEVELS[index];
      }
      return TYPE_LEVELS[BASIC_TYPES.indexOf(Map.class)];
   }

   private Class getType(Object src) {
      return (src == null) ? Void.class : src.getClass();
   }
}
