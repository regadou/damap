package org.regadou.repository;

import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import org.regadou.damai.Expression;
import org.regadou.damai.Repository;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;

public class RdfRepository implements Repository<Resource> {

   private transient ResourceManager resourceManager;
   private Collection<String> items = new TreeSet<>();


   public RdfRepository(ResourceManager resourceManager) {
      this.resourceManager = resourceManager;
   }

   @Override
   public Collection<String> getItems() {
      return items;
   }

   @Override
   public Map<String, Class> getKeys(String item) {
      throw new RuntimeException("Not implemented");
   }

   @Override
   public Collection<String> getPrimaryKeys(String item) {
      throw new RuntimeException("Not implemented");
   }

   @Override
   public Collection<Object> getIds(String item) {
      throw new RuntimeException("Not implemented");
   }

   @Override
   public Collection<Resource> getAny(String item, Expression filter) {
      throw new RuntimeException("Not implemented");
   }

   @Override
   public Resource getOne(String item, Object id) {
      throw new RuntimeException("Not implemented");
   }

   @Override
   public Resource add(String item, Resource entity) {
      throw new RuntimeException("Not implemented");
   }

   @Override
   public Resource update(String item, Resource entity) {
      throw new RuntimeException("Not implemented");
   }

   @Override
   public boolean remove(String item, Object id) {
      throw new RuntimeException("Not implemented");
   }
 }
