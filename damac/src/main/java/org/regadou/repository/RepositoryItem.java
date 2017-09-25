package org.regadou.repository;

import java.util.Collection;
import org.regadou.damai.Expression;
import org.regadou.damai.Filterable;
import org.regadou.damai.Repository;

public class RepositoryItem<T> implements Filterable {

   private transient Repository<T> repo;
   private String item;
   private String repository;
   private Collection<String> primaryKeys;

   public RepositoryItem(String item, Repository<T> repo) {
      this.item = item;
      this.repo = repo;
      this.repository = repo.toString();
      this.primaryKeys = repo.getPrimaryKeys(item);
   }

   @Override
   public String toString() {
      return item+"@"+repository;
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
      return repo.getAny(item, filterExpression);
   }

   public String getName() {
      return item;
   }

   public Repository getRepository() {
      return repo;
   }

   public Collection<String> getPrimaryKeys() {
      return primaryKeys;
   }

   public T getOne(Object id) {
      return repo.getOne(item, id);
   }

   public T insert(T value) {
      return repo.add(item, value);
   }

   public T save(T value) {
      return repo.update(item, value);
   }

   public boolean delete(Object id) {
      return repo.remove(item, id);
   }
}
