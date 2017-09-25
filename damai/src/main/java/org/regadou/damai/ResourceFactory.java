package org.regadou.damai;

public interface ResourceFactory {

   Reference getResource(String id);

   String[] getSchemes();

   ResourceManager getResourceManager();
}
