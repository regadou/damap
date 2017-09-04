package org.regadou.damai;

public interface ResourceManager {

   Reference getResource(String name);

   ResourceFactory getFactory(String scheme);

   ResourceFactory[] getFactories();

   String[] getSchemes();

   boolean registerFactory(ResourceFactory factory);
}
