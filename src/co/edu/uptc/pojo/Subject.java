package co.edu.uptc.pojo;

import java.util.concurrent.CopyOnWriteArrayList;

public class Subject {
    private CopyOnWriteArrayList<Observer> observers = new CopyOnWriteArrayList<>();

    public void attach(Observer observer) {
        observers.addIfAbsent(observer);
    }

    public void detach(Observer observer) {
        observers.remove(observer);
    }

    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update();
        }
    }
}