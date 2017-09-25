package org.regadou.damai;

public interface ResourceManager {

   Reference getResource(String id);

   ResourceFactory getFactory(String scheme);

   ResourceFactory[] getFactories();

   String[] getSchemes();

   boolean registerFactory(ResourceFactory factory);

   boolean registerNamespace(Namespace namespace);
}
