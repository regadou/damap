package org.regadou.action;

import java.util.ArrayList;
import java.util.List;
import org.regadou.damai.Action;
import org.regadou.damai.Command;
import org.regadou.damai.Configuration;
import org.regadou.damai.Operator;
import org.regadou.damai.StandardAction;

public class ActionBuilder {

   private static final String CASE_CONFLICT_ERROR = "Conflicting configuration: both upper case and lower case have been set";

   public static String getSymbol(Operator operator, boolean standard) {
      switch (operator) {
         case ADD: return "+";
         case SUBTRACT: return "-";
         case MULTIPLY: return "*";
         case DIVIDE: return "/";
         case MODULO: return "%";
         case POWER: return "^";
         case ROOT: return "\\/";
         case LOGARITHM: return "\\";
         case LESS: return "<";
         case LESSEQUAL: return "<=";
         case MORE: return ">";
         case MOREQUAL: return ">=";
         case EQUAL: return standard ? "==" : "=";
         case NOTEQUAL: return "!=";
         case AND: return standard ? "&&" : "&";
         case OR: return standard ? "||" : "|";
         case NOT: return "!";
         case IN: return "@";
         case FROM: return "<-";
         case TO: return "->";
         case IS: return "?:";
         case DO: return "=>";
         case HAVE: return ".";
         case JOIN: return ",";
         case CASE: return "?";
         case WHILE: return "?*";
         default: throw new RuntimeException("Unknown operator "+operator);
      }
   }

   private Configuration configuration;
   private boolean wantOperators;
   private boolean wantCommands;
   private boolean wantLowerCase;
   private boolean wantUpperCase;
   private boolean wantSymbols;
   private boolean wantStandard;
   private boolean wantOptimized;
   private boolean ignorePrecedence;
   private List<Action> actions = new ArrayList<>();

   public ActionBuilder(Configuration configuration) {
      this.configuration = configuration;
   }

   public boolean isWantOperators() {
      return wantOperators;
   }

   public ActionBuilder setWantOperators(boolean wantOperators) {
      this.wantOperators = wantOperators;
      return this;
   }

   public boolean isWantCommands() {
      return wantCommands;
   }

   public ActionBuilder setWantCommands(boolean wantCommands) {
      this.wantCommands = wantCommands;
      return this;
   }

   public boolean isWantLowerCase() {
      return wantLowerCase;
   }

   public ActionBuilder setWantLowerCase(boolean wantLowerCase) {
      this.wantLowerCase = wantLowerCase;
      return this;
   }

   public boolean isWantUpperCase() {
      return wantUpperCase;
   }

   public ActionBuilder setWantUpperCase(boolean wantUpperCase) {
      this.wantUpperCase = wantUpperCase;
      return this;
   }

   public boolean isWantSymbols() {
      return wantSymbols;
   }

   public ActionBuilder setWantSymbols(boolean wantSymbols) {
      this.wantSymbols = wantSymbols;
      return this;
   }

   public boolean isWantStandard() {
      return wantStandard;
   }

   public ActionBuilder setWantStandard(boolean wantStandard) {
      this.wantStandard = wantStandard;
      return this;
   }

   public boolean isWantOptimized() {
      return wantOptimized;
   }

   public ActionBuilder setWantOptimized(boolean wantOptimized) {
      this.wantOptimized = wantOptimized;
      return this;
   }

   public boolean isIgnorePrecedence() {
      return ignorePrecedence;
   }

   public ActionBuilder setIgnorePrecedence(boolean ignorePrecedence) {
      this.ignorePrecedence = ignorePrecedence;
      return this;
   }

   public List<Action> getActions() {
      return actions;
   }

   public ActionBuilder setActions(List<Action> addons) {
      this.actions = addons;
      return this;
   }

   public ActionBuilder addActions(Action...actions) {
      if (actions != null) {
         for (Action action : actions) {
            if (action != null)
               this.actions.add(action);
         }
      }
      return this;
   }

   public ActionBuilder addActions(Class...types) {
      if (types != null) {
         for (Class type : types) {
            Action action;
            if (Action.class.isAssignableFrom(type)) {
               try { action = (Action)type.getConstructor(Configuration.class).newInstance(configuration); }
               catch (Exception e) { action = null; }
               if (action != null)
                  actions.add(action);
            }
         }
      }
      return this;
   }

   public List<Action> buildAll() {
      if (wantLowerCase && wantUpperCase)
         throw new RuntimeException(CASE_CONFLICT_ERROR);
      List<Action> actions = new ArrayList<>();
      if (wantOperators) {
         for (Operator op : Operator.values())
            actions.add(buildAction(op));
      }
      if (wantCommands) {
         for (Command cmd : Command.values())
            actions.add(buildAction(cmd));
      }
      for (Action action : this.actions)
         actions.add(action);
      return actions;
   }

   public Action buildAction(Action parent) {
      Integer precedence = ignorePrecedence ? 0 : null;
      String name;
      if (parent instanceof Operator) {
         Operator op = (Operator)parent;
         if (wantSymbols)
            name = getSymbol(op, wantStandard);
         else if (wantLowerCase) {
            if (wantUpperCase)
               throw new RuntimeException(CASE_CONFLICT_ERROR);
            name = parent.getName().toLowerCase();
         }
         else if (wantUpperCase)
            name = parent.getName().toUpperCase();
         else
            name = parent.getName();
         if (wantOptimized) {
            try { return new LogicalAction(configuration, op, name, precedence); }
            catch (Exception e) {}
         }
      }
      else if (parent instanceof Command) {
         name = parent.getName();
         if (wantLowerCase)
            name = name.toLowerCase();
         else if (wantUpperCase)
            name = parent.getName().toUpperCase();
      }
      else
         return parent;

      return new DefaultAction(configuration, name, (StandardAction)parent, null, precedence);
   }
}
