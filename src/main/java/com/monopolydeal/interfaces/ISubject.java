package com.monopolydeal.interfaces;

/**
 * Subject interface for the Observer design pattern.
 * Implementing classes can register, remove, and notify observers about game events.
 */
public interface ISubject {

    /** Register an observer to receive game event notifications. */
    void attach(IGameObserver o);

    /** Remove a previously registered observer. */
    void detach(IGameObserver o);

    /** Notify all registered observers about a game event. */
    void notifyAllEvent(String event);
}
