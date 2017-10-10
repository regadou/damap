package org.regadou.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.regadou.damai.Expression;
import org.regadou.damai.Namespace;
import org.regadou.damai.Repository;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;
import org.regadou.factory.ResourcePropertyFactory;
import org.regadou.collection.FilterableIterable;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.resource.DefaultNamespace;
import org.regadou.resource.GenericResource;

public class RdfRepository implements Repository<Resource> {

   private static final Collection<String> PRIMARY_KEY = Arrays.asList(ResourcePropertyFactory.ID_PROPERTY);

   private transient ResourceManager resourceManager;
   private transient Configuration configuration;
   private transient Map<String,Map<String,Resource>> resources = new TreeMap<>();
   private Collection<String> items = new TreeSet<>();


   public RdfRepository(Configuration configuration, ResourceManager resourceManager) {
      this.configuration = configuration;
      this.resourceManager = resourceManager;
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
      return new FilterableIterable<>(configuration.getPropertyManager(), src.values()).filter(filter);
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

   @Override
   public void createItem(String item, Object definition) throws IllegalArgumentException {
      if (resources.containsKey(item))
         throw new IllegalArgumentException("Item "+item+" already exists");
      if (!(definition instanceof CharSequence))
         throw new IllegalArgumentException("Item definition must be a CharSequence");
      Namespace ns = new DefaultNamespace(item, definition.toString(), this);
      if (!resourceManager.registerNamespace(ns))
         throw new IllegalArgumentException("Cannot register namespace "+ns);
      addItem(item);
   }

   private String getLocalName(Object id, String item) {
      if (id != null) {
         String name = id.toString();
         int index = name.indexOf(':');
         if (index < 0)
            return name;
         if (name.substring(0, index).equals(item))
            return name.substring(index+1);
         Reference r = resourceManager.getResource(item+":");
         if (r != null) {
            String uri = r.getValue().toString();
            if (name.startsWith(uri))
               return name.substring(uri.length());
         }
      }
      return null;
   }

   private Resource setResource(String item, Resource entity, boolean overwrite) {
      if (entity != null) {
         String name = getLocalName(entity.getId(), item);
         if (name != null) {
            Map<String,Resource> map = resources.get(item);
            if (map == null)
               map = addItem(item);
            else if (map.containsKey(name) && !overwrite)
               return null;
            map.put(name, entity);
            return entity;
         }
      }
      return null;
   }

   private Map<String,Resource> addItem(String item) {
      Map<String,Resource> map = new TreeMap<>();
      Reference r = resourceManager.getResource(item+":");
      map.put("", (r instanceof Resource) ? (Resource)r : new GenericResource(r.getId(), r, true, configuration));
      resources.put(item, map);
      items.add(item);
      return map;
   }

}
