package org.regadou.factory;

import com.google.inject.Injector;
import org.regadou.damai.InstanceFactory;
import org.regadou.damai.Reference;

public class DefaultInstanceFactory implements InstanceFactory {

   private Injector injector;

   @Override
   public <T> T getInstance(Class<T> type, Reference... properties) {
      return injector.getInstance(type);
   }

   @Override
   public void registerInstance(Class iface, Class impl, Reference... properties) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   public void setInjector(Injector injector) {
      if (this.injector == null)
         this.injector = injector;
   }
}
