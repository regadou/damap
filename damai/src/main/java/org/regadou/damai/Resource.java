package org.regadou.damai;

public interface Resource<T> extends Reference<T> {

   @Override
   default String getId() {
      Reference owner = getOwner();
      String name = getLocalName();
      if (owner == null)
         return name;
      Object value = owner.getValue();
      while (value instanceof Reference)
         value = ((Reference)value).getValue();
      if (value instanceof Namespace)
         return ((Namespace)value).getPrefix() + ":" + ((name == null) ? "" : name);
      String id = owner.getId();
      if (id == null)
         return name;
      if (id.endsWith("/") || id.endsWith("#") || id.endsWith(":"))
         return id + ((name == null) ? "" : name);
      else if (id.contains("/"))
         return id + "/" + ((name == null) ? "" : name);
      return id + ":" + name;
   }

   String getLocalName();

   Reference getOwner();

   String[] getProperties();

   Reference getProperty(String property);

   void setProperty(String property, Reference value);

   boolean addProperty(String property, Reference value);
}
