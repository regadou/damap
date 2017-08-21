package org.regadou.reference;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.script.ScriptContext;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.Operator;
import org.regadou.damai.Reference;
import org.regadou.script.GenericComparator;

public class PathExpression implements Expression {

   private Configuration configuration;
   private GenericComparator comparator;
   private Object root;
   private List path = new ArrayList();
   private String text;

   public PathExpression(Configuration configuration, Object root, Object[] path) {
      this.configuration = configuration;
      this.comparator = new GenericComparator(configuration);
      this.root = root;
      if (path != null) {
         for (Object part : path)
            this.path.add(normalizeElement(part));
      }
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
      path.add(normalizeElement(token));
      text = null;
   }

   @Override
   public Reference getValue(ScriptContext context) {
      Reference result = new ReferenceHolder(null, root, true);
      for (Object part : path) {
         if (context == null)
            context = configuration.getContextFactory().getScriptContext();
         result = getProperty(result.getValue(), part, context);
         if (result == null)
            return null;
      }
      return result;
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

   private Object normalizeElement(Object obj) {
      if (obj instanceof CharSequence || obj instanceof Number || obj == null)
         return obj;
      if (obj.getClass().isArray())
         return normalizeArray(obj);
      if (obj instanceof Collection)
         return normalizeArray(((Collection)obj).toArray());
      if (obj instanceof Map)
         return new MapExpression((Map)obj, configuration);
      if (obj instanceof Expression)
         return obj;
      if (obj instanceof Reference)
         return normalizeElement(((Reference)obj).getValue());
      return obj;
   }

   private Object normalizeArray(Object obj) {
      switch (Array.getLength(obj)) {
         case 0:
            return null;
         case 1:
            return Array.get(obj, 0);
         default:
            return obj;
      }
   }

   private Reference getProperty(Object value, Object property, ScriptContext cx) {
      if (property == null)
         return null;
      if (property.getClass().isArray()) {
         int length = Array.getLength(property);
         List values = new ArrayList(length);
         for (int i = 0; i < length; i++)
            values.add(getProperty(value, Array.get(property, i), cx));
         return new ReferenceHolder(null, values, true);
      }
      if (property instanceof Expression) {
         Collection result = comparator.getFilteredCollection(value, (Expression)property);
         return new ReferenceHolder(null, result, true);
      }
      return configuration.getPropertyManager().getProperty(value, String.valueOf(property));
   }
}
