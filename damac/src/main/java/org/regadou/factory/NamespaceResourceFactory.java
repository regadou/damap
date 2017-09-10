package org.regadou.factory;

import org.regadou.damai.Namespace;
import org.regadou.damai.Reference;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;
import org.regadou.reference.GenericReference;

public class NamespaceResourceFactory implements ResourceFactory {

   private Namespace namespace;
   private String[] schemes;
   private ResourceManager resourceManager;

   public NamespaceResourceFactory(Namespace namespace, ResourceManager resourceManager) {
      this.namespace = namespace;
      this.schemes = new String[]{namespace.getPrefix()};
      this.resourceManager = resourceManager;
   }

   @Override
   public Reference getResource(String uri) {
      String name;
      if (uri.startsWith(namespace.getPrefix()+":"))
         name = uri.substring(namespace.getPrefix().length()+1);
      else if (uri.startsWith(namespace.getUri()))
         name = uri.substring(namespace.getUri().length());
      else
         return null;
      Object value = namespace.getRepository().getOne(namespace.getPrefix(), name);
      if (value == null)
         return null;
      else if (value instanceof Reference)
         return (Reference)value;
      else
         return new GenericReference(uri, value, true);
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
