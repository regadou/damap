package org.regadou.factory;

import org.regadou.resource.Url;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;

public class UrlResourceFactory implements ResourceFactory {

   private Configuration configuration;
   private ResourceManager resourceManager;
   //TODO: we should detect what protocols are registered in the system
   private List<String> schemes = Arrays.asList("http", "file", "mailto");

   @Inject
   public UrlResourceFactory(ResourceManager resourceManager, Configuration configuration) {
      this.resourceManager = resourceManager;
      this.configuration = configuration;
   }

   @Override
   public Reference getResource(String id) {
      try { return new Url(id, configuration); }
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
