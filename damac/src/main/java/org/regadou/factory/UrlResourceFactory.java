package org.regadou.factory;

import org.regadou.reference.UrlResource;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;

public class UrlResourceFactory implements ResourceFactory {

   private ResourceManager resourceManager;
   private List<String> schemes = Arrays.asList("http", "file", "mailto");

   public UrlResourceFactory(ResourceManager resourceManager) {
      this.resourceManager = resourceManager;
   }

   @Override
   public Resource getResource(String uri) {
      try { return new UrlResource(uri); }
      catch (MalformedURLException e) { return null; }
   }

   @Override
   public String[] getSchemes() {
      return schemes.toArray(new String[schemes.size()]);
   }

   @Override
   public ResourceManager getResourceManager() {
      return resourceManager;
   }
}
