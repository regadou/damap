package org.regadou.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.regadou.collection.MultiMap;
import org.regadou.damai.Action;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.PropertyManager;
import org.regadou.damai.Reference;

public class AllAction implements Action<Collection> {

   private PropertyManager propertyManager;
   private Map map;

   public AllAction(PropertyManager manager, Map...maps) {
      this.propertyManager = manager;
      this.map = (maps == null) ? Collections.EMPTY_MAP : new MultiMap(maps);
   }
   @Override
   public Collection execute(Object... parameters) {
      switch (parameters.length) {
         case 0:
            return map.keySet();
         case 1:
            Object value = getValue(parameters[0]);
            if (value == null)
               return map.keySet();
            return getProperties(value);
         default:
            Collection result = new ArrayList();
            for (Object param : parameters)
               result.add(getProperties(getValue(param)));
            return result;
      }
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public String getName() {
      return "all";
   }

   @Override
   public Class<Collection> getReturnType() {
      return Collection.class;
   }

   private Collection getProperties(Object src) {
      Class type = (src == null) ? Void.class : src.getClass();
      PropertyFactory factory = propertyManager.getPropertyFactory(type);
      String[] properties = (factory == null) ? new String[0] : factory.getProperties(src);
      return Arrays.asList(properties);
   }

   private Object getValue(Object value) {
      while (value instanceof Reference)
         value = ((Reference)value).getValue();
      return value;
   }
}
