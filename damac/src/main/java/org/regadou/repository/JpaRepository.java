package org.regadou.repository;

import com.google.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import org.regadou.damai.Converter;
import org.regadou.damai.Repository;

public class JpaRepository implements Repository {

   private EntityManagerFactory factory;
   private Map<String, Class> nameToClassMap = new LinkedHashMap<>();
   private Map<Class, String> classToNameMap = new LinkedHashMap<>();

   @Inject
   public JpaRepository(Converter converter, Properties properties) throws ClassNotFoundException {
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
   }

   @Override
   public Collection<String> getTypes() {
      return nameToClassMap.keySet();
   }

   public Class getType(String name) {
      return (name == null) ? null : nameToClassMap.get(name.toLowerCase());
   }

   @Override
   public Collection<Map<String,Object>> getAll(String type) {
      return query(type, "select e from " + type, null);
   }

   @Override
   public Map<String,Object> getOne(String type, Object id) {
      String jpql = "select e from " + type + " e where e.id = ?1";
      Collection<Map<String,Object>> entities = query(type, jpql, Arrays.asList(id));
      return entities.isEmpty() ? null : entities.iterator().next();
   }

   @Override
   public Map<String,Object> save(String type, Map<String,Object> entity) {
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
   public boolean delete(String type, Object id) {
      return null != transaction(manager -> {
         Object entity = manager.find(nameToClassMap.get(type.toLowerCase()), id);
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

   private Collection<Map<String,Object>> query(String type, String jpql, List params) {
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
}
