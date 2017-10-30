package org.regadou.resource;

import javax.script.ScriptContext;
import org.regadou.damai.Configuration;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;
import org.regadou.reference.GenericReference;

public class ScriptContextResource implements Resource {

   private Configuration configuration;
   private ScriptContext cx;
   private String name;
   private Integer scope;

   public ScriptContextResource(Configuration configuration, ScriptContext cx, String name, Integer scope) {
      this.configuration = configuration;
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
      return (scope != null) ? getContext().getAttribute(name, scope) : getContext().getAttribute(name);
   }

   @Override
   public Class getType() {
      return Object.class;
   }

   @Override
   public void setValue(Object value) {
      int scope = (this.scope != null) ? this.scope : getContext().getAttributesScope(name);
      if (scope < 0)
         scope = getContext().getScopes().get(0);
      getContext().setAttribute(name, value, scope);
   }

   @Override
   public Reference getOwner() {
      return new GenericReference(null, getContext(), true);
   }

   @Override
   public String getLocalName() {
      return name;
   }

   @Override
   public String[] getProperties() {
      return getFactory().getProperties(getContext());
   }

   @Override
   public Reference getProperty(String property) {
      return getFactory().getProperty(getContext(), property);
   }

   @Override
   public void setProperty(String property, Reference value) {
      Property p = getFactory().getProperty(getContext(), property);
      if (p != null) {
         Object v = (value == null) ? null : value.getValue();
         p.setValue(v);
      }
   }

   @Override
   public boolean addProperty(String property, Reference value) {
      Object v = (value == null) ? null : value.getValue();
      Property p = getFactory().addProperty(getContext(), property, v);
      return p != null;
   }

   private ScriptContext getContext() {
      return (cx == null) ? configuration.getContextFactory().getScriptContext() : cx;
   }

   private PropertyFactory<ScriptContext> getFactory() {
      Object value = getValue();
      Class type = (value == null) ? Void.class : value.getClass();
      return configuration.getPropertyManager().getPropertyFactory(type);
   }
}
