package org.regadou.damai;

public interface Resource {

   String getId();

   Namespace getNamespace();

   String[] getProperties();

   Resource getProperty(Resource property);

   void setProperty(Resource property, Resource value);

   boolean addProperty(Resource property, Resource value);
}
