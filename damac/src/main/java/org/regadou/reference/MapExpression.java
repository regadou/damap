package org.regadou.reference;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.regadou.damai.Configuration;
import org.regadou.damai.Operator;
import org.regadou.damai.Reference;
import org.regadou.script.CompiledExpression;
import org.regadou.script.ScriptContextProperty;

public class MapExpression extends CompiledExpression {

   private Configuration configuration;

   public MapExpression(Map map, Configuration configuration) {
      super(null, null, configuration);
      this.configuration = configuration;
      if (map != null) {
         switch (map.size()) {
            case 0:
               break;
            case 1:
               addCondition(map.keySet().iterator().next(), map.values().iterator().next());
               break;
            default:
               addConditions(map);
         }
      }
   }

   private void addConditions(Map map) {
      addToken(new ReferenceHolder(null, Operator.AND, true));
      for (Object key : map.keySet())
         addCondition(key, map.get(key));
   }

   private void addCondition(Object key, Object value) {
      Collection<Reference> tokens = Arrays.asList(
         new ReferenceHolder(null, Operator.EQUAL, true),
         new ScriptContextProperty(configuration.getContextFactory(), key.toString()),
         new ReferenceHolder(null, value, true)
      );
      if (getAction() == null) {
         for (Reference token : tokens)
            addToken(token);
      }
      else
         addToken(new CompiledExpression(null, tokens, configuration));
   }
}
