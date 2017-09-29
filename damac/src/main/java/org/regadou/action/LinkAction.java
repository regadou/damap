package org.regadou.action;

import org.regadou.damai.Action;
import org.regadou.damai.Command;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.reference.GenericReference;

public class LinkAction implements Action {

   private static final Class[] PARAMETERS = new Class[]{Object.class, Object.class};

   private Configuration configuration;
   private Action getter;

   public LinkAction (Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public Object execute(Object... parameters) {
      Object variable, value = null;
      switch (parameters.length) {
         default:
         case 2:
            value = parameters[1];
         case 1:
            variable = parameters[0];
            break;
         case 0:
            return null;
      }

      Reference ref;
      if (variable instanceof Reference)
         ref = (Reference)variable;
      else {
         if (getter == null)
            getter = new BinaryAction(configuration, null, Command.GET);
         Object result = getter.execute(variable);
         if (result instanceof Reference)
            ref = (Reference)variable;
         else if (result == null)
            return null;
         else
            return new GenericReference(null, value, true);
      }
      ref.setValue(value);
      return ref;
   }

   @Override
   public String getName() {
      return "link";
   }

   @Override
   public Class getReturnType() {
      return Reference.class;
   }

   @Override
   public Class[] getParameterTypes() {
      return PARAMETERS;
   }

}
