package com.vicmatskiv.pointblank.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StateMachine<T extends Enum<T>, Context> {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private T currentState;
   private List<Transition<T, Context>> allTransitions;
   private Set<T> allStates = new HashSet();
   private Map<T, Action<T, Context>> onSetStateActions;
   private Action<T, Context> onChangeStateAction;
   private StateHistory<T> stateHistory;
   private Map<T, List<Transition<T, Context>>> transitionsByFromState;
   private Map<T, List<Transition<T, Context>>> transitionsByToState;

   private StateMachine(T initialState, List<Transition<T, Context>> allowedTransitions, Map<T, Action<T, Context>> onSetStateActions, Action<T, Context> onChangeStateAction) {
      this.currentState = initialState;
      this.allTransitions = Collections.unmodifiableList(allowedTransitions);
      this.onChangeStateAction = onChangeStateAction;
      this.transitionsByFromState = new HashMap();
      this.transitionsByToState = new HashMap();
      Iterator var5 = allowedTransitions.iterator();

      while(var5.hasNext()) {
         Transition<T, Context> transition = (Transition)var5.next();
         this.allStates.add(transition.fromState);
         this.allStates.add(transition.toState);
         ((List)this.transitionsByFromState.computeIfAbsent(transition.fromState, (k) -> {
            return new ArrayList();
         })).add(transition);
         ((List)this.transitionsByToState.computeIfAbsent(transition.toState, (k) -> {
            return new ArrayList();
         })).add(transition);
      }

      this.onSetStateActions = Collections.unmodifiableMap(onSetStateActions);
      this.stateHistory = new StateHistory(this.allStates.size() + 1);
      this.stateHistory.add(initialState);
   }

   public T getCurrentState() {
      return this.currentState;
   }

   public void resetToState(T newState) {
      if (newState != this.currentState) {
         this.currentState = newState;
      }

   }

   public T setState(Context context, T newState) {
      boolean result = false;
      List<Transition<T, Context>> possibleTransitions = (List)this.transitionsByToState.get(newState);
      if (possibleTransitions == null) {
         throw new IllegalArgumentException("Unknown state: " + newState);
      } else {
         Iterator var5 = possibleTransitions.iterator();

         while(var5.hasNext()) {
            Transition<T, Context> transition = (Transition)var5.next();
            if (Objects.equals(this.currentState, transition.fromState) && Objects.equals(newState, transition.toState) && transition.p.test(context)) {
               this.setState(context, transition);
               result = true;
               break;
            }
         }

         if (result) {
            this.update(context);
         }

         return result ? this.currentState : null;
      }
   }

   public T setStateToAnyOf(Context context, Collection<T> toStates) {
      T result = null;
      Iterator var4 = toStates.iterator();

      while(var4.hasNext()) {
         T toState = (Enum)var4.next();
         result = this.setState(context, toState);
         if (result != null) {
            break;
         }
      }

      return result;
   }

   public T update(Context context) {
      int maxAllowedTransitions = this.allStates.size();

      for(int i = 0; i < maxAllowedTransitions; ++i) {
         Transition<T, Context> transition = this.next(context, TransitionMode.AUTO);
         if (transition == null) {
            break;
         }

         this.setState(context, transition);
      }

      return this.currentState;
   }

   private void setState(Context context, Transition<T, Context> transition) {
      if (transition.preAction != null) {
         transition.preAction.execute(context, transition.fromState, transition.toState);
      }

      this.currentState = transition.toState;
      this.stateHistory.add(this.currentState);
      if (transition.postAction != null) {
         transition.postAction.execute(context, transition.fromState, transition.toState);
      }

      Action<T, Context> onSetStateAction = (Action)this.onSetStateActions.get(this.currentState);
      if (onSetStateAction != null) {
         onSetStateAction.execute(context, transition.fromState, transition.toState);
      }

      if (this.onChangeStateAction != null) {
         this.onChangeStateAction.execute(context, transition.fromState, transition.toState);
      }

      LOGGER.trace("Transitioned: {} -> {}", transition.fromState, transition.toState);
   }

   private Transition<T, Context> next(Context context, TransitionMode transitionMode) {
      List<Transition<T, Context>> possibleTransitions = (List)this.transitionsByFromState.get(this.currentState);
      Iterator var4 = possibleTransitions.iterator();

      Transition transition;
      do {
         if (!var4.hasNext()) {
            return null;
         }

         transition = (Transition)var4.next();
      } while(!Objects.equals(this.currentState, transition.fromState) || !transition.p.test(context) || transition.mode != transitionMode);

      return transition;
   }

   public List<Transition<T, Context>> getAllTransitions() {
      return this.allTransitions;
   }

   public Collection<T> getStateHistory() {
      return Collections.unmodifiableCollection(this.stateHistory.deque);
   }

   public interface Action<T extends Enum<T>, Context> {
      void execute(Context var1, T var2, T var3);
   }

   private static class Transition<T extends Enum<T>, Context> {
      T fromState;
      T toState;
      Predicate<Context> p;
      Action<T, Context> preAction;
      Action<T, Context> postAction;
      TransitionMode mode;

      public Transition(T fromState, T toState, Predicate<Context> p, Action<T, Context> preAction, Action<T, Context> postAction, TransitionMode transitionMode) {
         this.fromState = fromState;
         this.toState = toState;
         this.p = p;
         this.preAction = preAction;
         this.postAction = postAction;
         this.mode = transitionMode;
      }

      public String toString() {
         return String.format("Tr[%s->%s, %s, pre: %s, post: %s]", this.fromState, this.toState, this.mode, this.preAction, this.postAction);
      }
   }

   public static class StateHistory<E> {
      private final ArrayDeque<E> deque;
      private final int maxSize;

      public StateHistory(int size) {
         this.deque = new ArrayDeque(size);
         this.maxSize = size;
      }

      public void add(E element) {
         if (this.deque.size() == this.maxSize) {
            this.deque.poll();
         }

         this.deque.offer(element);
      }

      public E remove() {
         return this.deque.poll();
      }

      public int size() {
         return this.deque.size();
      }

      public boolean contains(E element) {
         return this.deque.contains(element);
      }
   }

   public static enum TransitionMode {
      AUTO,
      EVENT_OR_AUTO,
      EVENT;

      // $FF: synthetic method
      private static TransitionMode[] $values() {
         return new TransitionMode[]{AUTO, EVENT_OR_AUTO, EVENT};
      }
   }

   public static class Builder<T extends Enum<T>, Context> {
      private List<Transition<T, Context>> allowedTransitions = new ArrayList();
      private final Set<T> allStates = new HashSet();
      private Map<T, Action<T, Context>> onSetStateActions = new HashMap();
      private Action<T, Context> onChangeStateAction;

      public Builder<T, Context> withOnSetStateAction(T toState, Action<T, Context> action) {
         this.onSetStateActions.put(toState, action);
         return this;
      }

      public Builder<T, Context> withOnChangeStateAction(Action<T, Context> onChangeStateAction) {
         this.onChangeStateAction = onChangeStateAction;
         return this;
      }

      public Builder<T, Context> withTransition(List<T> fromStates, T toState, Predicate<Context> predicate, TransitionMode transitionMode, Action<T, Context> preAction, Action<T, Context> postAction) {
         if (predicate == null) {
            predicate = (context) -> {
               return true;
            };
         }

         Iterator var7 = fromStates.iterator();

         while(var7.hasNext()) {
            T fromState = (Enum)var7.next();
            this.withTransition(fromState, toState, predicate, transitionMode, preAction, postAction);
         }

         return this;
      }

      public Builder<T, Context> withTransition(T fromState, T toState, Predicate<Context> predicate, TransitionMode transitionMode, Action<T, Context> preAction, Action<T, Context> postAction) {
         if (predicate == null) {
            predicate = (context) -> {
               return true;
            };
         }

         this.allowedTransitions.add(new Transition(fromState, toState, predicate, preAction, postAction, transitionMode));
         this.allStates.add(fromState);
         this.allStates.add(toState);
         return this;
      }

      public Builder<T, Context> withTransition(T fromState, T toState, Predicate<Context> predicate, TransitionMode transitionMode) {
         return this.withTransition((Enum)fromState, toState, predicate, transitionMode, (Action)null, (Action)null);
      }

      public Builder<T, Context> withTransition(T fromState, T toState) {
         return this.withTransition((Enum)fromState, toState, (Predicate)null, TransitionMode.EVENT, (Action)null, (Action)null);
      }

      public Builder<T, Context> withTransition(T fromState, T toState, Predicate<Context> predicate) {
         return this.withTransition((Enum)fromState, toState, predicate, TransitionMode.AUTO, (Action)null, (Action)null);
      }

      private void validate() {
         Set<T> statesWithOutgoingTransitions = new HashSet();
         Iterator var2 = this.allowedTransitions.iterator();

         while(var2.hasNext()) {
            Transition<T, Context> transition = (Transition)var2.next();
            statesWithOutgoingTransitions.add(transition.fromState);
         }

         var2 = this.allStates.iterator();

         Enum state;
         do {
            if (!var2.hasNext()) {
               var2 = this.onSetStateActions.keySet().iterator();

               do {
                  if (!var2.hasNext()) {
                     return;
                  }

                  state = (Enum)var2.next();
               } while(this.allStates.contains(state));

               throw new IllegalArgumentException("No transitions defined for state used in action: " + state);
            }

            state = (Enum)var2.next();
         } while(statesWithOutgoingTransitions.contains(state));

         throw new IllegalArgumentException("No outgoing transitions found for state " + state);
      }

      public StateMachine<T, Context> build(T initialState) {
         this.validate();
         return new StateMachine(initialState, this.allowedTransitions, this.onSetStateActions, this.onChangeStateAction);
      }
   }
}
