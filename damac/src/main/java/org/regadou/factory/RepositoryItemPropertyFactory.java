package org.regadou.factory;

import java.util.Collection;
import java.util.Map;
import org.regadou.damai.Configuration;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.repository.RepositoryItem;
import org.regadou.repository.RepositoryItemProperty;

public class RepositoryItemPropertyFactory implements PropertyFactory<RepositoryItem> {

   private Configuration configuration;

   public RepositoryItemPropertyFactory(Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public Property getProperty(RepositoryItem item, String name) {
      PropertyFactory factory = configuration.getPropertyManager().getPropertyFactory(item.getClass());
      return new RepositoryItemProperty(item, name, factory);
   }

   @Override
   public String[] getProperties(RepositoryItem item) {
      Collection<String> items = item.getRepository().getItems();
      return items.toArray(new String[items.size()]);
   }

   @Override
   public Property addProperty(RepositoryItem item, String name, Object value) {
      Property<RepositoryItem,Map> p = new RepositoryItemProperty(item, name, configuration.getPropertyManager().getPropertyFactory(item.getClass()));
      p.setValue(configuration.getConverter().convert(value, Map.class));
      return p;
   }

   @Override
   public boolean removeProperty(RepositoryItem item, String name) {
      return item.delete(name);
   }
}
