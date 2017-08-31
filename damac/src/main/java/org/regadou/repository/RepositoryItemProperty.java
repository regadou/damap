package org.regadou.repository;

import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;
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
      if (name == null)
         return null;
      return repoItem.getOne(name);
   }

   @Override
   public Class getType() {
      return Map.class;
   }

   @Override
   public void setValue(Map value) {
      if (name == null) {
         value = repoItem.insert(value);
         Collection<String> keys = repoItem.getPrimaryKeys();
         switch (keys.size()) {
            case 0:
               name = "";
               break;
            case 1:
               name = value.get(keys.iterator().next()).toString();
               break;
            default:
               StringJoiner joiner = new StringJoiner(",");
               for (String key : keys)
                  joiner.add(String.valueOf(value.get(key)));
               name = joiner.toString();
         }
      }
      else
         repoItem.save(value);
   }

}
