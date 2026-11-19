package co.edu.uptc.controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import co.edu.uptc.model.LibraryManager;

public class Server {
    private int port;
    private LibraryManager libraryManager;
    private boolean isRunning;

    public Server(int port) {
        this.port = port;
        this.libraryManager = new LibraryManager();
    }

    public void start() {
        isRunning = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor iniciado en el puerto " + port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado: " + clientSocket.getInetAddress());

                // Cada cliente se maneja en un hilo independiente
                ClientHandler handler = new ClientHandler(clientSocket, libraryManager);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }
}