/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author laim0nas100
 */
public class StateGraph {

    private HashMap<String, State> states = new HashMap<>();

    public StateGraph() {
    }

    public StateGraph addState(String name, Runnable onEnter, Runnable onExit) {
        if (states.containsKey(name)) {
            throw new IllegalArgumentException(name + " state is allready defined");
        }
        states.put(name, new State(name, onEnter, onExit));
        return this;
    }

    public StateGraph addState(String name, Runnable onEnter) {
        return this.addState(name, onEnter, null);
    }

    public StateGraph addState(String name) {
        return this.addState(name, null, null);
    }
    
    public StateGraph linkStates(String nameFrom, String nameTo){
        if(this.states.containsKey(nameFrom) && this.states.containsKey(nameTo)){
            this.states.get(nameFrom).transitions.put(nameTo, this.states.get(nameTo));
            return this;
        }else{
            throw new IllegalArgumentException("Not both states present "+nameFrom+" "+nameTo);
        }
    }

    public StateGraph init(String name) {
        if (this.currentState == null) {
            this.currentState = states.get(name);
            return this;
        }
        throw new IllegalStateException("StateGraph is allready initialized");
    }

    public boolean canTransitionTo(String stateName) {
        if (this.currentState == null) {
            return false;
        }
        return this.currentState.transitions.containsKey(stateName);
    }

    public void transition(String name) {
        if (this.canTransitionTo(name)) {
            if (this.currentState.onExit != null) {
                this.currentState.onExit.run();
            }
            State newState = this.currentState.transitions.get(name);
            this.currentState = newState;
            if (newState.onEnter != null) {
                newState.onEnter.run();
            }
        }
    }

    private State currentState;

    private static class State {

        public Map<String, State> transitions = new HashMap<>();
        public final String name;
        public final Runnable onEnter;
        public final Runnable onExit;

        public State(String n, Runnable onEnter, Runnable onExit) {
            this.name = n;
            this.onEnter = onEnter;
            this.onExit = onExit;
        }
    }

}
