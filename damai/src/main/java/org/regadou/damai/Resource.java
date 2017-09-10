package org.regadou.damai;

public interface Resource<T> extends Reference<T> {

   @Override
   default String getId() {
      Namespace ns = getNamespace();
      String name = getLocalName();
      if (ns == null)
         return (name == null) ? null : name;
      else if (name == null)
         return ns.getPrefix() + ":";
      else
         return ns.getPrefix() + ":" + name;
   }

   String getLocalName();

   Namespace getNamespace();

   String[] getProperties();

   Resource getProperty(Resource property);

   void setProperty(Resource property, Resource value);

   boolean addProperty(Resource property, Resource value);
}
