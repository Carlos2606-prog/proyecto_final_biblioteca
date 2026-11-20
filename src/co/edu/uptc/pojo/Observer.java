package co.edu.uptc.pojo;

public interface Observer {
    void update();

    default void onMessage(String message) {
    }
}
