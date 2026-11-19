import co.edu.uptc.controller.Server;

public class App {
    public static void main(String[] args) {
        int puerto = 1234;  
        Server server = new Server(puerto);
        server.start();
    }
}