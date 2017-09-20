package org.regadou.damai;

public interface Namespace extends Resource {

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
   default Object getValue() {
      return getUri();
   }

   @Override
   default Class getType() {
      return String.class;
   }
}
