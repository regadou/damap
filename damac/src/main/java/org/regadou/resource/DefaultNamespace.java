package org.regadou.resource;

import org.regadou.damai.Namespace;
import org.regadou.damai.Repository;

public class DefaultNamespace implements Namespace {

   private String prefix;
   private String iri;
   private Repository repository;

   public DefaultNamespace(String prefix, String iri, Repository repository) {
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
   public String getIri() {
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
}
