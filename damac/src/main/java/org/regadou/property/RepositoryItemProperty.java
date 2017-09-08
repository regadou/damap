package org.regadou.property;

import java.util.Collection;
import java.util.StringJoiner;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.PropertyManager;
import org.regadou.repository.RepositoryItem;

public class RepositoryItemProperty extends TypedProperty<RepositoryItem> {

   private String name;
   private PropertyFactory factory;

   public RepositoryItemProperty(RepositoryItem item, String name, PropertyManager manager) {
      super(item, RepositoryItem.class, item.getRepository().getClass(), "getOne", String.class, Object.class);
      this.name = name;
      this.factory = manager.getPropertyFactory(getType());
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Object getValue() {
      if (name == null)
         return null;
      return getParent().getOne(name);
   }

   @Override
   public void setValue(Object value) {
      RepositoryItem repoItem = getParent();
      if (name == null) {
         value = repoItem.insert(value);
         Collection<String> keys = repoItem.getPrimaryKeys();
         switch (keys.size()) {
            case 0:
               name = "";
               break;
            case 1:
               name = getPropertyValue(value, keys.iterator().next());
               break;
            default:
               StringJoiner joiner = new StringJoiner(",");
               for (String key : keys)
                  joiner.add(getPropertyValue(value, key));
               name = joiner.toString();
         }
      }
      else
         repoItem.save(value);
   }

   private String getPropertyValue(Object value, String key) {
      Property p = factory.getProperty(value, key);
      if (p == null)
         return "";
      return String.valueOf(p.getValue());
   }
}
