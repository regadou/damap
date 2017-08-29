package org.regadou.factory;

import java.util.Collection;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Repository;
import org.regadou.reference.PropertyHolder;
import org.regadou.repository.RepositoryItem;

public class RepositoryPropertyFactory implements PropertyFactory<Repository> {

   @Override
   public Property getProperty(Repository repo, String name) {
      return repo.getItems().contains(name) ? new PropertyHolder(repo, name, new RepositoryItem(name, repo), true) : null;
   }

   @Override
   public String[] getProperties(Repository repo) {
      Collection<String> types = repo.getItems();
      return types.toArray(new String[types.size()]);
   }

   @Override
   public Property addProperty(Repository repo, String name, Object value) {
      //TODO: create a new property item
      return null;
   }

   @Override
   public boolean removeProperty(Repository repo, String name) {
      //TODO: can we remove an item from a repository ?
      return false;
   }
}
