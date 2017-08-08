package org.regadou.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import org.regadou.damai.Repository;
import org.regadou.reference.ReferenceHolder;

public class RepositoryType implements Map<Object,Bindings> {

   private String type;
   private Repository repo;

   public RepositoryType(String type, Repository repo) {
      this.type = type;
      this.repo = repo;
   }

   @Override
   public String toString() {
      return type+"@"+repo;
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
      return repo.getIds(type).size();
   }

   @Override
   public boolean isEmpty() {
      return repo.getIds(type).isEmpty();
   }

   @Override
   public boolean containsKey(Object key) {
      return repo.getIds(type).contains(key);
   }

   @Override
   public boolean containsValue(Object value) {
      return values().contains(value);
   }

   @Override
   public Bindings get(Object key) {
      return repo.getOne(type, key);
   }

   @Override
   public Bindings put(Object key, Bindings value) {
      Bindings old = repo.getOne(type, key);
      //TODO: we might want to set keys in bindings before saving
      repo.save(type, value);
      return old;
   }

   @Override
   public Bindings remove(Object key) {
      Bindings old = repo.getOne(type, key);
      return repo.delete(type, key) ? old : null;
   }

   @Override
   public void putAll(Map<? extends Object, ? extends Bindings> m) {
      if (m != null) {
         for (Object key : m.keySet())
            put(key, m.get(key));
      }
   }

   @Override
   public void clear() {
      for (Object key : keySet())
         repo.delete(type, key);
   }

   @Override
   public Set<Object> keySet() {
      Set<Object> keys = new LinkedHashSet<>();
      for (Object id : repo.getIds(type))
         keys.add(id);
      return keys;
   }

   @Override
   public Collection<Bindings> values() {
      Collection<Bindings> rows = new ArrayList<>();
      for (Object id : repo.getIds(type))
         rows.add(repo.getOne(type, id));
      return rows;
   }

   @Override
   public Set<Entry<Object, Bindings>> entrySet() {
      Set<Entry<Object, Bindings>> entries = new LinkedHashSet<>();
      for (Object id : repo.getIds(type))
         entries.add(new ReferenceHolder(id.toString(), repo.getOne(type, id)).toMapEntry());
      return entries;
   }
}
