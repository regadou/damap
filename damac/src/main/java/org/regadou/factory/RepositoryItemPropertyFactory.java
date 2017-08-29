package org.regadou.factory;

import java.util.Collection;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.repository.RepositoryItem;
import org.regadou.repository.RepositoryItemProperty;

public class RepositoryItemPropertyFactory implements PropertyFactory<RepositoryItem> {

   @Override
   public Property getProperty(RepositoryItem item, String name) {
      return new RepositoryItemProperty(item, name);
   }

   @Override
   public String[] getProperties(RepositoryItem item) {
      Collection<String> items = item.getRepository().getItems();
      return items.toArray(new String[items.size()]);
   }

   @Override
   public Property addProperty(RepositoryItem item, String name, Object value) {
      Property p = new RepositoryItemProperty(item, name);
      p.setValue(value);
      return p;
   }

   @Override
   public boolean removeProperty(RepositoryItem item, String name) {
      return item.delete(name);
   }
}
