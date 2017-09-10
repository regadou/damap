package org.regadou.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.Namespace;
import org.regadou.damai.PropertyManager;
import org.regadou.damai.Repository;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;
import org.regadou.factory.ResourcePropertyFactory;
import org.regadou.util.FilterableIterable;

public class RdfRepository implements Repository<Resource> {

   private static final Collection<String> PRIMARY_KEY = Arrays.asList(ResourcePropertyFactory.ID_PROPERTY);

   private transient ResourceManager resourceManager;
   private transient PropertyManager propertyManager;
   private transient Map<String,Map<String,Resource>> resources = new TreeMap<>();
   private Collection<String> items = new TreeSet<>();


   public RdfRepository(ResourceManager resourceManager, PropertyManager propertyManager) {
      this.resourceManager = resourceManager;
      this.propertyManager = propertyManager;
   }

   @Override
   public Collection<String> getItems() {
      return items;
   }

   @Override
   public Map<String, Class> getKeys(String item) {
      Map<String,Resource> map = resources.get(item);
      if (map != null) {
         Map<String, Class> keys = new TreeMap<>();
         for (String id : map.keySet()) {
            if (!id.isEmpty())
               keys.put(id, map.get(id).getType());
         }
         return keys;
      }
      return null;
   }

   @Override
   public Collection<String> getPrimaryKeys(String item) {
      return PRIMARY_KEY;
   }

   @Override
   public Collection<Object> getIds(String item) {
      Map<String,Resource> map = resources.get(item);
      if (map != null) {
         Collection<Object> ids = new TreeSet<>();
         for (String id : map.keySet()) {
            if (!id.isEmpty())
               ids.add(id);
         }
         return ids;
      }
      return null;
   }

   @Override
   public Collection<Resource> getAny(String item, Expression filter) {
      Map<String,Resource> src = resources.get(item);
      if (src == null)
         return Collections.EMPTY_LIST;
      if (filter == null)
         return src.values();
      return new FilterableIterable<>(propertyManager, src.values()).filter(filter);
   }

   @Override
   public Resource getOne(String item, Object id) {
      Map<String,Resource> src = resources.get(item);
      if (src != null) {
         String name = getLocalName(id, item);
         if (name != null)
            return src.get(name);
      }
      return null;
   }

   @Override
   public Resource add(String item, Resource entity) {
      return setResource(item, entity, false);
   }

   @Override
   public Resource update(String item, Resource entity) {
      return setResource(item, entity, true);
   }

   @Override
   public boolean remove(String item, Object id) {
      Map<String,Resource> map = resources.get(item);
      if (map != null) {
         String name = getLocalName(id, item);
         if (name != null) {
            Resource old = map.remove(name);
            if (map.isEmpty()) {
               resources.remove(item);
               items.remove(item);
            }
            return old != null;
         }
      }
      return false;
   }

   private String getLocalName(Object id, String item) {
      if (id != null) {
         String name = id.toString();
         if (name.startsWith(item+":"))
            return name.substring(item.length()+1);
      }
      return null;
   }

   private Resource setResource(String item, Resource entity, boolean overwrite) {
      if (entity != null) {
         String name = getLocalName(entity.getId(), item);
         if (name != null) {
            Map<String,Resource> map = resources.get(item);
            if (map == null) {
               map = new TreeMap<>();
               map.put("", (Namespace)resourceManager.getResource(item+":"));
               resources.put(item, map);
               items.add(item);
            }
            else if (map.containsKey(name) && !overwrite)
               return null;
            map.put(name, entity);
            return entity;
         }
      }
      return null;
   }

}
