package org.regadou.repository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.damai.Expression;
import org.regadou.damai.Repository;

public class JpaRepository implements Repository<Map> {

   private transient EntityManagerFactory factory;
   private transient Map<String, Class> nameToClassMap = new LinkedHashMap<>();
   private transient Map<Class, String> classToNameMap = new LinkedHashMap<>();
   private transient Map<Class,Map<String,Class>> keysMap = new LinkedHashMap<>();
   private Collection<String> items;

   @Inject
   public JpaRepository(Properties properties) throws ClassNotFoundException {
      Class.forName("org.hibernate.jpa.HibernatePersistenceProvider");
      factory = Persistence.createEntityManagerFactory("javatest", properties);
      EntityManager manager = factory.createEntityManager();
      for (EntityType type : manager.getMetamodel().getEntities()) {
         String n = type.getName().toLowerCase();
         Class c = type.getJavaType();
         nameToClassMap.put(n, c);
         classToNameMap.put(c, n);
      }
      manager.close();
      items = new TreeSet<>(nameToClassMap.keySet());
   }

   @Override
   public Collection<String> getItems() {
      return items;
   }

   public Class getType(String item) {
      return nameToClassMap.get(item.toLowerCase());
   }

   @Override
   public Map<String,Class> getKeys(String item) {
      Class type = getType(item);
      if (type == null)
         return null;
      Map<String,Class> keys = keysMap.get(type);
      if (keys == null) {
         try {
            BeanMap bean = new BeanMap(type.newInstance());
            keysMap.put(type, keys = new TreeMap<>());
            for (Object key : bean.keySet()) {
               String name = key.toString();
               if (name.equals("class"))
                  continue;
               keys.put(name, bean.getType(name));
            }
         }
         catch (InstantiationException|IllegalAccessException e) {
            throw new RuntimeException(e);
         }
      }
      return keys;
   }

   @Override
   public Collection<String> getPrimaryKeys(String item) {
      return Collections.singleton("id");
   }

   @Override
   public Collection<Object> getIds(String item) {
      Collection<Object> ids = new ArrayList<>();
      for (Map row : query("select e.id from " + item + " e", null))
         ids.add(row.get("id"));
      return ids;
   }

   @Override
   public Collection<Map> getAll(String item) {
      return query("select e from " + item + " e", null);
   }

   @Override
   public Collection<Map> getAny(String item, Expression filter) {
      String jpql = "select e from " + item + " e";
      List params = null;
      if (filter != null)
         jpql += " where " + getClause(filter);
      return query(jpql, params);
   }

   @Override
   public Map getOne(String item, Object id) {
      String jpql = "select e from " + item + " e where e.id = ?1";
      Collection<Map> entities = query(jpql, Arrays.asList(id));
      return entities.isEmpty() ? null : entities.iterator().next();
   }

   @Override
   public Map insert(String item, Map entity) {
      return transaction(manager -> {
         manager.persist(entity);
         return entity;
      });
   }

   @Override
   public Map save(String item, Map entity) {
      return transaction(manager -> {
         Set ids = manager.getMetamodel().entity(entity.getClass()).getIdClassAttributes();
         for (Object id : ids) {
            if (getValue(entity, id) == null) {
               manager.persist(entity);
               return entity;
            }
         }
         return manager.merge(entity);
      });
   }

   @Override
   public boolean delete(String item, Object id) {
      return null != transaction(manager -> {
         Object entity = manager.find(nameToClassMap.get(item.toLowerCase()), id);
         if (entity != null) {
            manager.remove(entity);
         }
         return entity;
      });
   }

   private <T> T transaction(Function<EntityManager, T> function) {
      EntityManager manager = factory.createEntityManager();
      EntityTransaction t = manager.getTransaction();
      if (!t.isActive()) {
         t.begin();
      }
      boolean error = false;
      try {
         return function.apply(manager);
      } catch (Exception e) {
         error = true;
         RuntimeException rte = (e instanceof RuntimeException)
                 ? (RuntimeException) e
                 : new RuntimeException(e);
         throw rte;
      } finally {
         if (!t.isActive())
                ; else if (error) {
            t.rollback();
         } else {
            t.commit();
         }
         manager.close();
      }
   }

   private Collection<Map> query(String jpql, List params) {
      EntityManager manager = factory.createEntityManager();
      Query query = manager.createQuery(jpql);
      if (params != null) {
         for (int p = 0; p < params.size(); p++) {
            query.setParameter(p + 1, params.get(p));
         }
      }
      try {
         return query.getResultList();
      } finally {
         manager.close();
      }
   }

   private Object getValue(Object entity, Object id) {
      SingularAttribute att = (SingularAttribute) id;
      String name = att.getName();
      try {
         Method getter = entity.getClass().getMethod("get" + name.substring(0, 1).toUpperCase() + name.substring(1));
         return getter.invoke(entity);
      } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
         throw new RuntimeException(e);
      }
   }

   private String getClause(Expression exp) {
      throw new RuntimeException("Filter not supported in JpaRepository");
   }
}
