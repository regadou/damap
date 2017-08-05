package org.regadou.damai;

public interface ResourceFactory {

   Reference getResource(String uri);

   String[] getSchemes();

   ResourceManager getResourceManager();
}
