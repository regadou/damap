package org.regadou.repository;

import java.util.Collection;
import java.util.Map;
import org.regadou.damai.Expression;
import org.regadou.damai.Filterable;
import org.regadou.damai.Repository;

public class RepositoryItem implements Filterable {

   private transient Repository repo;
   private String name;
   private String repository;
   private Collection<String> primaryKeys;

   public RepositoryItem(String name, Repository repo) {
      this.name = name;
      this.repo = repo;
      this.repository = repo.toString();
      this.primaryKeys = repo.getPrimaryKeys(name);
   }

   @Override
   public String toString() {
      return name+"@"+repo;
   }

   @Override
   public boolean equals(Object that) {
      return toString().equals(that.toString());
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }

   @Override
   public Collection filter(Expression filterExpression) {
      return repo.getAny(name, filterExpression);
   }

   public String getName() {
      return name;
   }

   public Repository getRepository() {
      return repo;
   }

   public Collection<String> getPrimaryKeys() {
      return primaryKeys;
   }

   public Map getOne(Object id) {
      return repo.getOne(name, id);
   }

   public Map insert(Map value) {
      return repo.insert(name, value);
   }

   public Map save(Map value) {
      return repo.save(name, value);
   }

   public boolean delete(Object id) {
      return repo.delete(name, id);
   }
}
