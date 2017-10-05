package org.regadou.damai;

public interface Namespace extends Resource<String> {

   String getUri();

   String getPrefix();

   Repository getRepository();

   @Override
   default String getLocalName() {
      return null;
   }

   @Override
   default Namespace getNamespace() {
      return this;
   }

   @Override
   default String getValue() {
      return getUri();
   }

   @Override
   default Class<String> getType() {
      return String.class;
   }
}
