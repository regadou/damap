package org.regadou.property;

import javax.script.ScriptContext;
import org.regadou.damai.Property;
import org.regadou.damai.ScriptContextFactory;

public class ScriptContextProperty implements Property<ScriptContext,Object> {

   private ScriptContextFactory factory;
   private ScriptContext cx;
   private String name;
   private Integer scope;

   public ScriptContextProperty(ScriptContextFactory factory, String name) {
      this.factory = factory;
      this.name = name;
   }

   public ScriptContextProperty(ScriptContextFactory factory, String name, Integer scope) {
      this.factory = factory;
      this.name = name;
      this.scope = scope;
   }

   public ScriptContextProperty(ScriptContext cx, String name) {
      this.cx = cx;
      this.name = name;
   }

   public ScriptContextProperty(ScriptContext cx, String name, Integer scope) {
      this.cx = cx;
      this.name = name;
      this.scope = scope;
   }

   @Override
   public String toString() {
      return name;
   }

   @Override
   public String getId() {
      return name;
   }

   @Override
   public Object getValue() {
      return (scope != null) ? getParent().getAttribute(name, scope) : getParent().getAttribute(name);
   }

   @Override
   public Class getType() {
      Object value = getValue();
      return (value == null) ? Object.class : value.getClass();
   }

   @Override
   public void setValue(Object value) {
      int scope = (this.scope != null) ? this.scope : getParent().getAttributesScope(name);
      if (scope < 0)
         scope = getParent().getScopes().get(0);
      getParent().setAttribute(name, value, scope);
   }

   @Override
   public ScriptContext getParent() {
      return (cx == null) ? factory.getScriptContext() : cx;
   }

   @Override
   public Class getParentType() {
      return ScriptContext.class;
   }
}
