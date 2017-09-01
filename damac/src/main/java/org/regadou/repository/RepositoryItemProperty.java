package org.regadou.repository;

import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;

public class RepositoryItemProperty<T> implements Property<RepositoryItem,T> {

   private RepositoryItem<T> repoItem;
   private String name;
   private PropertyFactory factory;

   public RepositoryItemProperty(RepositoryItem<T> repotype, String name, PropertyFactory factory) {
      this.repoItem = repotype;
      this.name = name;
      this.factory = factory;
   }

   @Override
   public RepositoryItem<T> getParent() {
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
   public T getValue() {
      if (name == null)
         return null;
      return repoItem.getOne(name);
   }

   @Override
   public Class getType() {
      return Map.class;
   }

   @Override
   public void setValue(T value) {
      if (name == null) {
         value = repoItem.insert(value);
         Collection<String> keys = repoItem.getPrimaryKeys();
         switch (keys.size()) {
            case 0:
               name = "";
               break;
            case 1:
               name = factory.getProperty(value, keys.iterator().next()).toString();
               break;
            default:
               StringJoiner joiner = new StringJoiner(",");
               for (String key : keys)
                  joiner.add(String.valueOf(factory.getProperty(value, key)));
               name = joiner.toString();
         }
      }
      else
         repoItem.save(value);
   }

}
