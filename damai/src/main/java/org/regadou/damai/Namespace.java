package org.regadou.damai;

public interface Namespace extends ResourceFactory {

   String getIri();

   String getPrefix();

   String[] getNames();

   Reference addResource(String uri);

   boolean removeResource(String uri);

   @Override
   default String[] getSchemes() {
      return new String[]{getPrefix()};
   }
}
