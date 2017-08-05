package org.regadou.script;

import javax.script.ScriptContext;
import org.regadou.damai.Reference;

public class ScriptContextReference implements Reference {

   private ScriptContext cx;
   private String name;

   public ScriptContextReference(ScriptContext cx, String name) {
      this.cx = cx;
      this.name = name;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Object getValue() {
      return cx.getAttribute(name);
   }

   @Override
   public Class getType() {
      Object value = getValue();
      return (value == null) ? Void.class : value.getClass();
   }

   @Override
   public void setValue(Object value) {
      int scope = cx.getAttributesScope(name);
      if (scope < 0)
         scope = cx.getScopes().get(0);
      cx.setAttribute(name, value, scope);
   }
}
