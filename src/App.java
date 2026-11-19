import co.edu.uptc.controller.Server;

public class App {
    public static void main(String[] args) {
        int puerto = 1234; // Asegúrate de usar este mismo puerto en el ServerClient
        Server server = new Server(puerto);
        server.start();
    }
}