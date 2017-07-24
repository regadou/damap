package org.regadou.damai;

public interface ResourceManager {

   ResourceFactory getFactory(String scheme);

   ResourceFactory[] getFactories();

   String[] getSchemes();

   void registerFactory(ResourceFactory factory);
}
