package org.regadou.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.damai.Repository;
import org.regadou.reference.ReferenceHolder;
import org.regadou.script.GenericComparator;

public class RepositoryMap implements Map<String,RepositoryType> {

   private Repository repo;
   private Configuration configuration;
   private Comparator comparator;


   public RepositoryMap(Repository repo) {
      this(repo, null);
   }

   public RepositoryMap(Repository repo, Configuration configuration) {
      this.repo = repo;
      this.configuration = configuration;
      this.comparator = new GenericComparator(configuration);
   }

   public RepositoryMap(String url, Configuration configuration) {
      this.configuration = configuration;
      this.comparator = new GenericComparator(configuration);
      Reference r = configuration.getResourceManager().getResource(url);
      Object value = (r == null) ? null : r.getValue();
      if (value instanceof Repository) {
         this.repo = (Repository)value;
         return;
      }
      throw new RuntimeException("Invalid url for refering a repository: "+url);
   }

   @Override
   public String toString() {
      return repo.toString();
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
   public int size() {
      return repo.getTypes().size();
   }

   @Override
   public boolean isEmpty() {
      return repo.getTypes().isEmpty();
   }

   @Override
   public boolean containsKey(Object key) {
      return repo.getTypes().contains(key.toString());
   }

   @Override
   public boolean containsValue(Object value) {
      return values().contains(value);
   }

   @Override
   public RepositoryType get(Object key) {
      String type = key.toString();
      if (repo.getTypes().contains(type))
         return new RepositoryType(type, repo, comparator);
      return null;
   }

   @Override
   public RepositoryType put(String key, RepositoryType value) {
      return get(key);
   }

   @Override
   public RepositoryType remove(Object key) {
      return null;
   }

   @Override
   public void putAll(Map<? extends String, ? extends RepositoryType> m) {}

   @Override
   public void clear() {}

   @Override
   public Set<String> keySet() {
      return new LinkedHashSet<>(repo.getTypes());
   }

   @Override
   public Collection<RepositoryType> values() {
      Collection<RepositoryType> types = new ArrayList<>();
      for (String type : repo.getTypes())
         types.add(new RepositoryType(type, repo, comparator));
      return types;
   }

   @Override
   public Set<Entry<String, RepositoryType>> entrySet() {
      Set<Entry<String, RepositoryType>> entries = new LinkedHashSet<>();
      for (String type : repo.getTypes())
         entries.add(new ReferenceHolder(type, new RepositoryType(type, repo, comparator)).toMapEntry());
      return entries;
   }
}
