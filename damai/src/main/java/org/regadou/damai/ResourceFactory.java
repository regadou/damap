package org.regadou.damai;

public interface ResourceFactory {

   Resource getResource(String id);

   String[] getSchemes();

   ResourceManager getResourceManager();
}
