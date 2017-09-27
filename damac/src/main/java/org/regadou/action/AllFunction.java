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

public class AllFunction implements Action<Collection> {

   private PropertyManager propertyManager;
   private Map map;

   public AllFunction(PropertyManager manager, Map...maps) {
      this.propertyManager = manager;
      this.map = (maps == null) ? Collections.EMPTY_MAP : new MultiMap(maps);
   }
   @Override
   public Collection execute(Object... parameters) {
      switch (parameters.length) {
         case 0:
            return map.keySet();
         case 1:
            return getProperties(parameters[0]);
         default:
            Collection result = new ArrayList();
            for (Object param : parameters)
               result.add(getProperties(param));
            return result;
      }
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
}
