package org.regadou.factory;

import java.io.File;
import org.regadou.reference.UrlResource;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;

public class FileResourceFactory implements ResourceFactory {

   private ResourceManager resourceManager;
   private String[] schemes = new String[1];

   public FileResourceFactory(ResourceManager resourceManager) {
      this.resourceManager = resourceManager;
   }

   @Override
   public Resource getResource(String file) {
      File f = new File(file);
      try { return new UrlResource(f.toURI().toURL()); }
      catch (MalformedURLException e) { return null; }
   }

   @Override
   public String[] getSchemes() {
      return schemes;
   }

   @Override
   public ResourceManager getResourceManager() {
      return resourceManager;
   }
}
