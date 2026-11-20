package co.edu.uptc.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import co.edu.uptc.model.Book;
import co.edu.uptc.model.Copy;
import co.edu.uptc.model.LibraryManager;
import co.edu.uptc.model.Loan;
import co.edu.uptc.model.User;
import co.edu.uptc.pojo.Observer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ClientHandler implements Runnable, Observer {
    private Socket socket;
    private final ExecutorService notifier = Executors.newSingleThreadExecutor();
    private LibraryManager libraryManager;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isAdmin;
    private String sessionUserId;

    public ClientHandler(Socket socket, LibraryManager libraryManager) {
        this.socket = socket;
        this.libraryManager = libraryManager;
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            libraryManager.attach(this);

            while (true) {
                String request = readMessage();
                writeMessage(processRequest(request));
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + socket.getInetAddress());
        } finally {
            libraryManager.detach(this);
            libraryManager.detach(this);
            notifier.shutdownNow();
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void update() {
        sendAsync("NOTIFICATION;DATA_CHANGED");
    }

    @Override
    public void onMessage(String message) {
        if (isAdmin || sessionUserId == null) {
            return;
        }
        sendAsync("NOTIFICATION;MESSAGE;" + message);
    }

    private void sendAsync(String payload) {
        try {
            notifier.submit(() -> {
                try {
                    if (out != null) {
                        writeMessage(payload);
                    }
                } catch (IOException e) {
                    System.err.println("Error enviando notificación: " + e.getMessage());
                }
            });
        } catch (RejectedExecutionException e) {
        }
    }


    private String readMessage() throws IOException {
        int length = in.readInt();
        byte[] data = new byte[length];
        in.readFully(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    private void writeMessage(String message) throws IOException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        synchronized (out) {
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        }
    }


    private String processRequest(String request) {
  
        String[] p = request.split(";", -1);
        try {
            synchronized (libraryManager) {
                return execute(p[0], p);
            }
        } catch (NumberFormatException e) {
            return "ERROR;Se esperaba un valor numérico válido";
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return "ERROR;" + msg;
        }
    }

    private void need(String[] p, int n) {
        if (p.length < n) {
            throw new IllegalArgumentException("Faltan parámetros en la solicitud");
        }
    }

    private String execute(String command, String[] p) {
        switch (command) {
            case "LOGIN_ADMIN":
                need(p, 3);
                if ("admin".equals(p[1]) && "123".equals(p[2])) {
                    isAdmin = true;
                    sessionUserId = null;
                    return "SUCCESS";
                }
                return "ERROR;Credenciales incorrectas";

            case "LOGIN_USER": {
                need(p, 3);
                User user = libraryManager.findUserById(p[1].trim());
                if (user != null && user.getName().equalsIgnoreCase(p[2].trim())) {
                    isAdmin = false;
                    sessionUserId = user.getUserId();
                    return "SUCCESS";
                }
                return "ERROR;Usuario no encontrado o datos incorrectos";
            }

            case "LOGOUT":
                isAdmin = false;
                sessionUserId = null;
                return "SUCCESS";
            default:
                break;
        }

        if (!isAdmin && sessionUserId == null) {
            return "ERROR;Debe iniciar sesión";
        }

        switch (command) {
            case "GET_BOOKS":
                return "SUCCESS;" + booksToJson(libraryManager.getAllBooks());

            case "SEARCH_BOOKS":
                need(p, 3);
                return "SUCCESS;" + booksToJson(libraryManager.searchBooks(p[1], p[2]));

            case "GET_AVAILABLE_COPIES": {
                need(p, 2);
                Book book = libraryManager.findBookByCode(Integer.parseInt(p[1].trim()));
                if (book == null) {
                    return "ERROR;Libro no encontrado";
                }
                JsonArray array = new JsonArray();
                for (Copy copy : book.getCopies()) {
                    if ("Disponible".equals(copy.getStatus())) {
                        array.add(copy.getCopyId());
                    }
                }
                return "SUCCESS;" + array.toString();
            }

            case "GET_MY_LOANS":
                return "SUCCESS;" + loansToJson(libraryManager.getLoansByUser(requireSessionUser().getUserId()));

            case "GET_MY_DETAILS": {
                User user = requireSessionUser();
                JsonObject o = new JsonObject();
                o.addProperty("userId", user.getUserId());
                o.addProperty("name", user.getName());
                o.addProperty("type", user.getType());
                o.addProperty("activeLoans", libraryManager.getActiveLoansByUser(user.getUserId()).size());
                return "SUCCESS;" + o.toString();
            }
            default:
                break;
        }

        if (!isAdmin) {
            return "ERROR;Operación permitida solo para administradores";
        }

        switch (command) {
            case "GET_USERS":
                return "SUCCESS;" + usersToJson(libraryManager.getAllUsers());

            case "ADD_USER":
                need(p, 4);
                libraryManager.addUser(p[1].trim(), p[2].trim(), p[3].trim());
                return "SUCCESS;Usuario agregado correctamente";

            case "EDIT_USER":
                need(p, 4);
                libraryManager.editUser(p[1].trim(), p[2].trim(), p[3].trim());
                return "SUCCESS;Usuario actualizado correctamente";

            case "DELETE_USER":
                need(p, 2);
                libraryManager.removeUser(p[1].trim());
                return "SUCCESS;Usuario eliminado correctamente";

            case "ADD_BOOK":
                need(p, 7);
                libraryManager.addBook(Integer.parseInt(p[1].trim()), p[2].trim(), p[3].trim(), p[4].trim(),
                        Integer.parseInt(p[5].trim()), Integer.parseInt(p[6].trim()));
                return "SUCCESS;Libro agregado correctamente";

            case "EDIT_BOOK":
                need(p, 7);
                libraryManager.editBook(Integer.parseInt(p[1].trim()), p[2].trim(), p[3].trim(), p[4].trim(),
                        Integer.parseInt(p[5].trim()), Integer.parseInt(p[6].trim()));
                return "SUCCESS;Libro actualizado correctamente";

            case "DELETE_BOOK":
                need(p, 2);
                libraryManager.removeBook(Integer.parseInt(p[1].trim()));
                return "SUCCESS;Libro eliminado correctamente";

            case "GET_LOANS":
                return "SUCCESS;" + loansToJson(libraryManager.getAllLoans());

            case "REGISTER_LOAN": {
                need(p, 4);
                LocalDate returnDate;
                try {
                    returnDate = LocalDate.parse(p[3].trim());
                } catch (DateTimeParseException e) {
                    return "ERROR;Fecha inválida";
                }
                libraryManager.registerLoan(p[1].trim(), Integer.parseInt(p[2].trim()), returnDate);
                return "SUCCESS;Préstamo registrado correctamente";
            }

            case "REGISTER_RETURN":
                need(p, 2);
                libraryManager.registerReturn(Integer.parseInt(p[1].trim()));
                return "SUCCESS;Devolución registrada correctamente";


            default:
                return "ERROR;Comando no reconocido";
        }
    }

    private User requireSessionUser() {
        User user = sessionUserId == null ? null : libraryManager.findUserById(sessionUserId);
        if (user == null) {
            throw new IllegalStateException("Esta operación requiere un usuario con sesión iniciada");
        }
        return user;
    }

    private String booksToJson(List<Book> books) {
        JsonArray array = new JsonArray();
        for (Book b : books) {
            JsonObject o = new JsonObject();
            o.addProperty("code", b.getCode());
            o.addProperty("title", b.getTitle());
            o.addProperty("author", b.getAuthor());
            o.addProperty("genre", b.getGenre());
            o.addProperty("year", b.getYear());
            o.addProperty("totalCopies", b.getTotalCopies());
            o.addProperty("availableCopies", b.getAvailableCopies());
            array.add(o);
        }
        return array.toString();
    }

    private String usersToJson(List<User> users) {
        JsonArray array = new JsonArray();
        for (User u : users) {
            JsonObject o = new JsonObject();
            o.addProperty("userId", u.getUserId());
            o.addProperty("name", u.getName());
            o.addProperty("type", u.getType());
            o.addProperty("activeLoans", libraryManager.getActiveLoansByUser(u.getUserId()).size());
            array.add(o);
        }
        return array.toString();
    }

    private String loansToJson(List<Loan> loans) {
        JsonArray array = new JsonArray();
        for (Loan l : loans) {
            JsonObject o = new JsonObject();
            o.addProperty("loanId", l.getLoanId());
            o.addProperty("userId", l.getUser().getUserId());
            o.addProperty("userName", l.getUser().getName());
            o.addProperty("bookTitle", l.getCopy().getBook().getTitle());
            o.addProperty("copyId", l.getCopy().getCopyId());
            o.addProperty("loanDate", l.getLoanDate() == null ? "" : l.getLoanDate().toString());
            o.addProperty("returnDate", l.getReturnDate() == null ? "" : l.getReturnDate().toString());
            o.addProperty("status", l.getStatus());
            array.add(o);
        }
        return array.toString();
    }
}
