package org.regadou.reference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.script.ScriptContext;
import org.regadou.damai.Action;
import org.regadou.damai.Expression;
import org.regadou.damai.Operator;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Reference;

public class PathExpression implements Expression {

   private PropertyFactory factory;
   private Object root;
   private List path;
   private String text;

   public PathExpression(PropertyFactory factory, Object root, Object[] path) {
      this.factory = factory;
      this.root = root;
      this.path = new ArrayList(Arrays.asList((path == null) ? new Object[0] : path));
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public Action getAction() {
      return Operator.HAVE;
   }

   @Override
   public Reference[] getTokens() {
      List<Reference> tokens = new ArrayList<>();
      tokens.add((root instanceof Reference) ? (Reference)root : new ReferenceHolder(null, root));
      for (Object token : path)
         tokens.add((token instanceof Reference) ? (Reference)token : new ReferenceHolder(null, token));
      return tokens.toArray(new Reference[tokens.size()]);
   }

   @Override
   public void addToken(Reference token) {
      path.add(token);
      text = null;
   }

   @Override
   public Reference getValue(ScriptContext context) {
      Object value = root;
      if (!path.isEmpty()) {
         for (Object part : path) {
            if (value == null)
               break;
            Property p = getProperty(root, part);
            value = (p == null) ? null : p.getValue();
         }
      }
      return (value instanceof Reference) ? (Reference)value : new ReferenceHolder(null, value);
   }

   @Override
   public String getName() {
      if (text == null) {
         text = Operator.HAVE.getName();
         for (Object token : path) {
            if (!text.isEmpty())
               text += " ";
            text += String.valueOf(token);
         }
         text = "("+text+")";
      }
      return text;
   }

   @Override
   public Reference getValue() {
      return getValue(null);
   }

   @Override
   public Class<Reference> getType() {
      return Reference.class;
   }

   @Override
   public void setValue(Reference value) {
      Reference result = getValue();
      if (result != null)
         result.setValue(value);
   }

   private Property getProperty(Object value, Object property) {
      if (property == null)
         return null;
      Map<String, Property> map = factory.getProperties(value);
      return (map == null) ? null : map.get(property);
      //TODO: check if property is a reference of a charsequence or an array|collection|map
   }
}
