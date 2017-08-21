package org.regadou.factory;

import java.util.Set;
import java.util.TreeSet;
import javax.script.Bindings;
import javax.script.ScriptContext;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.script.ScriptContextProperty;

public class ScriptContextPropertyFactory implements PropertyFactory<ScriptContext> {

   @Override
   public Property getProperty(ScriptContext cx, String name) {
      int scope = cx.getAttributesScope(name);
      return (scope < 0) ? null : new ScriptContextProperty(cx, name, scope);
   }

   @Override
   public String[] getProperties(ScriptContext cx) {
      Set<String> names = new TreeSet<>();
      for (Integer scope : cx.getScopes()) {
         Bindings b = cx.getBindings(scope);
         if (b != null)
            names.addAll(b.keySet());
      }
      return names.toArray(new String[names.size()]);
   }
}
