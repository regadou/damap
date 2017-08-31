package org.regadou.script;

import java.io.File;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.script.Bindings;
import javax.script.ScriptContext;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.Filterable;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.PropertyManager;
import org.regadou.damai.Reference;
import org.regadou.damai.Repository;
import org.regadou.reference.CollectionProperty;
import org.regadou.reference.MapProperty;
import org.regadou.util.ClassIterator;
import org.regadou.util.EnumerationSet;
import org.regadou.util.ArrayIterator;

public class GenericComparator implements Comparator {

   private static final List<Class> STRINGABLES = Arrays.asList(new Class[]{
      CharSequence.class, Number.class, Boolean.class, Character.class,
      URL.class, URI.class, File.class
   });

   private static final List<Class> ITERABLES = Arrays.asList(new Class[]{
      Iterable.class, Iterator.class, Enumeration.class, Map.class, ScriptContext.class
   });

   private Configuration configuration;

   @Inject
   public GenericComparator(Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public int compare(Object o1, Object o2) {
      o1 = getValue(o1);
      o2 = getValue(o2);
      if (isStringable(o1) && isStringable(o2))
         return compareStringables(o1, o2);
      List<String> l1 = getSortedStringList(o1);
      List<String> l2 = getSortedStringList(o2);
      int size1 = l1.size();
      int size2 = l2.size();
      for (int i = 0; i < size1; i++) {
         if (i >= size2)
            return 1;
         int dif = l1.get(i).compareTo(l2.get(i));
         if (dif != 0)
            return dif;
      }
      return (size2 > size1) ? -1 : 0;
   }

   public boolean isEmpty(Object src) {
      src = getValue(src);
      if (src == null)
         return true;
      if (src instanceof Boolean)
         return (Boolean)src == false;
      if (src instanceof Number)
         return ((Number)src).doubleValue() == 0;
      if (src instanceof CharSequence)
         return src.toString().trim().isEmpty();
      if (src instanceof Map)
         return ((Map)src).isEmpty();
      if (src instanceof Iterable)
         return !((Iterable)src).iterator().hasNext();
      if (src.getClass().isArray())
         return Array.getLength(src) == 0;
      if (src instanceof Iterator)
         return !((Iterator)src).hasNext();
      if (src instanceof Enumeration)
         return !((Enumeration)src).hasMoreElements();
      return false;
   }

   public boolean isStringable(Object src) {
      src = getValue(src);
      ClassIterator it = new ClassIterator(src);
      while (it.hasNext()) {
         Class c = it.next();
         if (STRINGABLES.contains(c))
            return true;
      }
      return false;
   }

   public boolean isIterable(Object src) {
      src = getValue(src);
      if (src != null && src.getClass().isArray())
         return true;
      ClassIterator it = new ClassIterator(src);
      while (it.hasNext()) {
         Class c = it.next();
         if (ITERABLES.contains(c))
            return true;
      }
      return false;
   }

   public String getString(Object obj) {
      if (isStringable(obj))
         return obj.toString();
      if (obj instanceof Class)
         return ((Class)obj).getName();
      if (obj == null)
         return "";
      if (obj instanceof Reference) {
         Reference r = (Reference)obj;
         String name = r.getName();
         return (name == null) ? getString(r.getValue()) : name;
      }
      if (obj instanceof Map.Entry) {
         Map.Entry e = (Map.Entry)obj;
         return getString(e.getKey())+getString(e.getValue());
      }

      StringJoiner joiner = new StringJoiner(",");
      List<String> list = getSortedStringList(obj);
      for (String item : list)
         joiner.add(item);
      return joiner.toString();
   }

   public Double getNumeric(Object src, Double defaultValue) {
      src = getValue(src);
      if (src instanceof Number)
         return ((Number)src).doubleValue();
      if (src instanceof Boolean)
         return ((Boolean)src) ? 1d : 0d;
      if (src instanceof Date)
         return ((Long)((Date)src).getTime()).doubleValue();
      if (src == null)
         return 0d;
      if (src instanceof CharSequence) {
         try { return new Double(src.toString()); }
         catch (Exception e) { return defaultValue; }
      }
      if (src instanceof Character) {
         char c = (Character)src;
         return (c >= '0' && c <= '9') ? ((Integer)(c - '0')).doubleValue() : defaultValue;
      }
      if (src instanceof Collection) {
         Collection c = (Collection)src;
         switch (c.size()) {
            case 0:
               return 0d;
            case 1:
               return getNumeric(c.iterator().next(), defaultValue);
            default:
               return defaultValue;
         }
      }
      if (src.getClass().isArray()) {
         switch (Array.getLength(src)) {
            case 0:
               return 0d;
            case 1:
               return getNumeric(Array.get(src, 0), defaultValue);
            default:
               return defaultValue;
         }
      }
      return defaultValue;
   }

   public Iterator getIterator(Object src) {
      src = getValue(src);
      if (src instanceof Iterator)
         return (Iterator)src;
      if (src instanceof Enumeration)
         return new EnumerationSet((Enumeration)src).iterator();
      if (src instanceof Map)
         return ((Map)src).keySet().iterator();
      if (src instanceof Iterable)
         return ((Iterable)src).iterator();
      if (src == null)
         return Collections.EMPTY_LIST.iterator();
      if (src.getClass().isArray())
         return new ArrayIterator(src);
      if (src instanceof Repository)
         return ((Repository)src).getItems().iterator();
      if (src instanceof ScriptContext) {
         ScriptContext cx = (ScriptContext)src;
         Set keys = new TreeSet();
         for (Integer scope : cx.getScopes()) {
            Bindings b = cx.getBindings(scope);
            if (b != null)
               keys.addAll(b.keySet());
         }
         return keys.iterator();
      }
      if (src instanceof Class || src instanceof Map.Entry || isStringable(src))
         return Collections.singletonList(src).iterator();
      if (src instanceof Reference) {
         Reference r = (Reference)src;
         String name = r.getName();
         return (name == null) ? getIterator(r.getValue()) : getIterator(name);
      }
      return new BeanMap(src).keySet().iterator();
   }

   public Map getMap(Object src) {
      src = getValue(src);
      if (src instanceof Map)
         return (Map)src;
      if (src == null)
         return Collections.EMPTY_MAP;
      return new BeanMap(src);
   }

   public List<String> getSortedStringList(Object src) {
      src = getValue(src);
      List<String> list = new ArrayList<>();
      Iterator it = getIterator(src);
      while (it.hasNext())
         list.add(getString(it.next()));
      Collections.sort(list);
      return list;
   }

   public Collection getFilteredCollection(Object src, Expression filter) {
      src = getValue(src);
      if (src instanceof Filterable)
         return ((Filterable)src).filter(filter);
      Iterator it;
      if (src instanceof Map)
         it = ((Map)src).values().iterator();
      else if (src instanceof ScriptContext) {
         ScriptContext cx = (ScriptContext)src;
         List values = new ArrayList();
         for (Integer scope : cx.getScopes()) {
            Bindings b = cx.getBindings(scope);
            if (b != null)
               values.addAll(b.values());
         }
         it = values.iterator();
      }
      else if (isIterable(src))
         it = getIterator(src);
      else if (isStringable(src))
         it = Collections.singleton(src).iterator();
      else
         it = new BeanMap(src).values().iterator();

      List dst = new ArrayList();
      while (it.hasNext()) {
         Object item = it.next();
         ScriptContext cx = new PropertiesScriptContext(item, configuration.getPropertyManager(),
                                                              configuration.getContextFactory());
         if (!isEmpty(filter.getValue(cx)))
            dst.add(item);
      }
      return dst;
   }

   public Object getValue(Object value) {
      while (value instanceof Reference)
         value = ((Reference)value).getValue();
      return value;
   }

   public Object setValue(Reference ref, Object value) {
      while (ref instanceof Expression)
         ref = ((Expression)ref).getValue();
      Class srcType = (value == null) ? Object.class : value.getClass();
      Class dstType = ref.getType();
      if (!dstType.isAssignableFrom(srcType))
         value = configuration.getConverter().convert(value, dstType);
      ref.setValue(value);
      return ref;
   }

   public Object addValue(Reference ref, Object value) {
      Object target = getValue(ref);
      Class type = (target == null) ? Void.class : target.getClass();
      PropertyFactory factory = configuration.getPropertyManager().getPropertyFactory(type);
      return (factory == null) ? null : factory.addProperty(target, null, value);
   }

   public Object mergeValue(Reference ref, Object value) {
      Object target = getValue(ref);
      Class srcType = (value == null) ? Void.class : value.getClass();
      Class dstType = (target == null) ? Void.class : target.getClass();
      PropertyManager manager = configuration.getPropertyManager();
      PropertyFactory srcFactory = manager.getPropertyFactory(srcType);
      PropertyFactory dstFactory = manager.getPropertyFactory(dstType);
      Double index;
      for (String name : srcFactory.getProperties(value)) {
         Property p = dstFactory.getProperty(target, name);
         if (p != null)
            setValue(p, srcFactory.getProperty(value, name).getValue());
         else if (target instanceof Map)
            setValue(new MapProperty((Map)target, name, null), srcFactory.getProperty(value, name).getValue());
         else if (target instanceof Collection && (index = getNumeric(name, null)) != null)
            setValue(new CollectionProperty((Collection)target, index.intValue(), null), srcFactory.getProperty(value, name).getValue());
         //TODO: do it for array, bean. script or find a generic way to do it
      }
      return target;
   }

   public boolean removeValue(Reference parent, String id) {
      Object target = getValue(parent);
      Class type = (target == null) ? Void.class : target.getClass();
      PropertyFactory factory = configuration.getPropertyManager().getPropertyFactory(type);
      return (factory == null) ? false : factory.removeProperty(target, id);
   }

   private int compareStringables(Object o1, Object o2) {
      Double n1 = getNumeric(o1, null);
      if (n1 != null) {
         Double n2 = getNumeric(o2, null);
         if (n2 != null)
            return n1.compareTo(n2);
      }
      return getString(o1).compareToIgnoreCase(getString(o2));
   }
}
