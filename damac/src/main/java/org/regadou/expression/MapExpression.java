package org.regadou.expression;

import org.regadou.resource.ScriptContextResource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.regadou.damai.Configuration;
import org.regadou.damai.Operator;
import org.regadou.damai.Reference;
import org.regadou.reference.GenericReference;

public class MapExpression extends DefaultExpression {

   private Configuration configuration;

   public MapExpression(Map map, Configuration configuration) {
      super(null, Collections.EMPTY_LIST, configuration);
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
      addToken(new GenericReference(null, Operator.AND, true));
      for (Object key : map.keySet())
         addCondition(key, map.get(key));
   }

   private void addCondition(Object key, Object value) {
      List<Reference> tokens = Arrays.asList(new GenericReference(null, Operator.EQUAL, true),
         new ScriptContextResource(configuration, null, key.toString(), null),
         new GenericReference(null, value, true)
      );
      if (getAction() == null) {
         for (Reference token : tokens)
            addToken(token);
      }
      else
         addToken(new DefaultExpression(null, tokens, configuration));
   }
}
