package org.regadou.repository;

import java.util.Map;
import org.regadou.damai.Property;

public class RepositoryItemProperty implements Property<RepositoryItem,Map> {

   private RepositoryItem repoItem;
   private String name;

   public RepositoryItemProperty(RepositoryItem repotype, String name) {
      this.repoItem = repotype;
      this.name = name;
   }

   @Override
   public RepositoryItem getParent() {
      return repoItem;
   }

   @Override
   public Class getParentType() {
      return RepositoryItem.class;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Map getValue() {
      return repoItem.getOne(name);
   }

   @Override
   public Class getType() {
      return Map.class;
   }

   @Override
   public void setValue(Map value) {
      repoItem.save(value);
   }

}
