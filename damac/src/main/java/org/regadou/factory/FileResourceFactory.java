package org.regadou.factory;

import com.google.inject.Inject;
import java.io.File;
import org.regadou.reference.UrlReference;
import java.net.MalformedURLException;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;

public class FileResourceFactory implements ResourceFactory {

   private Configuration configuration;
   private ResourceManager resourceManager;
   private String[] schemes = new String[1];

   @Inject
   public FileResourceFactory(ResourceManager resourceManager, Configuration configuration) {
      this.resourceManager = resourceManager;
      this.configuration = configuration;
   }

   @Override
   public Reference getResource(String file) {
      File f = new File(file);
      try { return new UrlReference(f.toURI().toURL(), configuration); }
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
