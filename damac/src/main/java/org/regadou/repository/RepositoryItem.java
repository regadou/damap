package org.regadou.repository;

import java.util.Collection;
import org.regadou.damai.Expression;
import org.regadou.damai.Filterable;
import org.regadou.damai.Repository;

public class RepositoryItem<T> implements Filterable {

   private transient Repository<T> repo;
   private String name;
   private String repository;
   private Collection<String> primaryKeys;

   public RepositoryItem(String name, Repository<T> repo) {
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

   public T getOne(Object id) {
      return repo.getOne(name, id);
   }

   public T insert(T value) {
      return repo.insert(name, value);
   }

   public T save(T value) {
      return repo.save(name, value);
   }

   public boolean delete(Object id) {
      return repo.delete(name, id);
   }
}
