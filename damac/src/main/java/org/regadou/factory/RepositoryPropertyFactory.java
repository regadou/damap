package org.regadou.factory;

import java.util.Collection;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Repository;
import org.regadou.property.GenericProperty;
import org.regadou.repository.RepositoryItem;

public class RepositoryPropertyFactory implements PropertyFactory<Repository> {

   @Override
   public Property getProperty(Repository repo, String name) {
      return repo.getItems().contains(name) ? new GenericProperty(repo, name, new RepositoryItem(name, repo), true) : null;
   }

   @Override
   public String[] getProperties(Repository repo) {
      Collection<String> types = repo.getItems();
      return types.toArray(new String[types.size()]);
   }

   @Override
   public Property addProperty(Repository repo, String name, Object value) {
      try {
         repo.createItem(name, value);
         return new GenericProperty(repo, name, new RepositoryItem(name, repo), true);
      }
      catch (IllegalArgumentException e) {
         //TODO: log the exception somehow
         return null;
      }
   }

   @Override
   public boolean removeProperty(Repository repo, String name) {
      //TODO: we could have a removeItem() method in the repository interface
      //      but I find it dangerous to easily loose persisted data
      return false;
   }
}
