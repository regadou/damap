package org.regadou.damai;

public interface ResourceFactory {

   Resource getResource(String uri);

   String[] getSchemes();

   ResourceManager getResourceManager();
}
