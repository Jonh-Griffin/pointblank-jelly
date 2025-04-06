package com.vicmatskiv.pointblank.util;

import com.vicmatskiv.pointblank.feature.ConditionContext;
import com.vicmatskiv.pointblank.item.GunItem;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.function.TriFunction;

public class Expression {
   private static final Pattern EXPRESSION_PATTERN = Pattern.compile("^(\\w+)\\s*(<=|>=|<|>|==)\\s*(-?\\d+(?:\\.\\d+)?)$");

   public static Predicate<ConditionContext> compile(String expression) {
      Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
      if (matcher.find()) {
         String varName = matcher.group(1);
         TriFunction tf;
         if (varName.equals("reloadIterationIndex")) {
            tf = (player, gunState, itemStack) -> {
               return gunState.getReloadIterationIndex();
            };
         } else {
            if (!varName.equals("ammoCount")) {
               throw new IllegalArgumentException("Unknown variable: " + varName);
            }

            tf = (player, gunState, itemStack) -> {
               return gunState.getAmmoCount(GunItem.getFireModeInstance(itemStack));
            };
         }

         String op = matcher.group(2);
         double value = Double.parseDouble(matcher.group(3));
         byte var8 = -1;
         switch(op.hashCode()) {
         case 60:
            if (op.equals("<")) {
               var8 = 0;
            }
            break;
         case 62:
            if (op.equals(">")) {
               var8 = 1;
            }
            break;
         case 1921:
            if (op.equals("<=")) {
               var8 = 2;
            }
            break;
         case 1952:
            if (op.equals("==")) {
               var8 = 4;
            }
            break;
         case 1983:
            if (op.equals(">=")) {
               var8 = 3;
            }
         }

         switch(var8) {
         case 0:
            return (context) -> {
               return ((Number)tf.apply(context.player(), context.gunClientState(), context.currentItemStack())).doubleValue() < value;
            };
         case 1:
            return (context) -> {
               return ((Number)tf.apply(context.player(), context.gunClientState(), context.currentItemStack())).doubleValue() > value;
            };
         case 2:
            return (context) -> {
               return ((Number)tf.apply(context.player(), context.gunClientState(), context.currentItemStack())).doubleValue() <= value;
            };
         case 3:
            return (context) -> {
               return ((Number)tf.apply(context.player(), context.gunClientState(), context.currentItemStack())).doubleValue() >= value;
            };
         case 4:
            return (context) -> {
               return ((Number)tf.apply(context.player(), context.gunClientState(), context.currentItemStack())).doubleValue() == value;
            };
         }
      }

      throw new IllegalArgumentException("Invalid expression: " + expression);
   }
}
