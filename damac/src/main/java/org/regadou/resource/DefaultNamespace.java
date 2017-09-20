package org.regadou.resource;

import java.util.Collection;
import org.regadou.damai.Namespace;
import org.regadou.damai.Repository;
import org.regadou.damai.Resource;

public class DefaultNamespace implements Namespace {

   private String prefix;
   private String iri;
   private Repository<Resource> repository;

   public DefaultNamespace(String prefix, String iri, Repository<Resource> repository) {
      this.prefix = prefix;
      this.iri = iri;
      this.repository = repository;
   }

   @Override
   public String toString() {
      return prefix + ":";
   }

   @Override
   public boolean equals(Object that) {
      return that != null && toString().equals(that.toString());
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }

   @Override
   public String getUri() {
      return iri;
   }

   @Override
   public String getPrefix() {
      return prefix;
   }

   @Override
   public Repository getRepository() {
      return repository;
   }

   @Override
   public String[] getProperties() {
      Collection<Object> ids = repository.getIds(prefix);
      return ids.toArray(new String[ids.size()]);
   }

   @Override
   public Resource getProperty(Resource property) {
      return repository.getOne(prefix, property);
   }

   @Override
   public void setProperty(Resource property, Resource value) {
      repository.update(prefix, value);
   }

   @Override
   public boolean addProperty(Resource property, Resource value) {
      return repository.add(prefix, value) != null;
   }

   @Override
   public void setValue(Object value) {}
}
