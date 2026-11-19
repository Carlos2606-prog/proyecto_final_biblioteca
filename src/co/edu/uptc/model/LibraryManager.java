package co.edu.uptc.model;

import co.edu.uptc.persistence.DataManager;
import co.edu.uptc.persistence.DataManager.CopyData;
import co.edu.uptc.persistence.DataManager.LoanData;
import co.edu.uptc.pojo.Subject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Logica de la biblioteca. Los metodos lanzan excepciones con un mensaje claro
 * cuando una operacion no es valida; ClientHandler las convierte en "ERROR;mensaje".
 * El acceso concurrente se sincroniza en ClientHandler (synchronized sobre este objeto).
 */
public class LibraryManager extends Subject {

    private List<User> users;
    private List<Book> books;
    private List<Loan> loans;
    private int nextLoanId;
    private int nextCopyId;
    private DataManager dataManager;

    public LibraryManager() {
        this.users = new ArrayList<>();
        this.books = new ArrayList<>();
        this.loans = new ArrayList<>();
        this.nextLoanId = 1;
        this.nextCopyId = 1;
        this.dataManager = new DataManager();

        try {
            loadAllData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAllData() throws IOException {
        users = dataManager.loadUsers();
        books = dataManager.loadBooks();

        List<CopyData> copiesData = dataManager.loadCopies();
        for (CopyData copyData : copiesData) {
            Book book = findBookByCode(copyData.getBookCode());
            if (book != null) {
                Copy copy = new Copy(copyData.getCopyId(), copyData.getStatus(), book);
                book.addCopy(copy);

                if (copyData.getCopyId() >= nextCopyId) {
                    nextCopyId = copyData.getCopyId() + 1;
                }
            }
        }

        List<LoanData> loansData = dataManager.loadLoans();
        for (LoanData loanData : loansData) {
            User user = findUserById(loanData.getUserId());
            Copy copy = findCopyById(loanData.getCopyId());

            if (user != null && copy != null) {
                Loan loan = new Loan(loanData.getLoanId(), user, copy,
                        loanData.getLoanDate(), loanData.getReturnDate());
                loan.setStatus(loanData.getStatus());
                loans.add(loan);

                if (loanData.getLoanId() >= nextLoanId) {
                    nextLoanId = loanData.getLoanId() + 1;
                }
            }
        }

        reconcileCopyStatus();
    }

    /**
     * El estado de una copia debe coincidir con los prestamos: "Prestado" solo si
     * tiene un prestamo activo. (En los datos originales las copias 1 y 5 estaban
     * "Prestado" sin ningun prestamo activo.)
     */
    private void reconcileCopyStatus() {
        for (Book book : books) {
            for (Copy copy : book.getCopies()) {
                copy.setStatus("Disponible");
            }
        }
        for (Loan loan : loans) {
            if ("Activo".equals(loan.getStatus())) {
                loan.getCopy().setStatus("Prestado");
            }
        }
        saveAllData();
    }

    public void saveAllData() {
        try {
            dataManager.saveUsers(users);
            dataManager.saveBooks(books);

            List<CopyData> copiesData = new ArrayList<>();
            for (Book book : books) {
                for (Copy copy : book.getCopies()) {
                    copiesData.add(new CopyData(copy.getCopyId(),
                            book.getCode(),
                            copy.getStatus()));
                }
            }
            dataManager.saveCopies(copiesData);

            List<LoanData> loansData = new ArrayList<>();
            for (Loan loan : loans) {
                loansData.add(new LoanData(loan.getLoanId(),
                        loan.getUser().getUserId(),
                        loan.getCopy().getCopyId(),
                        loan.getLoanDate(),
                        loan.getReturnDate(),
                        loan.getStatus()));
            }
            dataManager.saveLoans(loansData);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ==========================================
    // USUARIOS
    // ==========================================

    public User addUser(String userId, String name, String type) {
        if (isBlank(userId) || isBlank(name)) {
            throw new IllegalArgumentException("El ID y el nombre del usuario son obligatorios");
        }
        if (findUserById(userId) != null) {
            throw new IllegalArgumentException("Ya existe un usuario con el ID " + userId);
        }
        User user = new User(userId, name, type);
        users.add(user);
        saveAllData();
        notifyObservers();
        return user;
    }

    public void editUser(String userId, String name, String type) {
        User user = findUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        if (isBlank(name)) {
            throw new IllegalArgumentException("El nombre del usuario es obligatorio");
        }
        user.setName(name);
        user.setType(type);
        saveAllData();
        notifyObservers();
    }

    public void removeUser(String userId) {
        User user = findUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        if (!getActiveLoansByUser(userId).isEmpty()) {
            throw new IllegalStateException("No se puede eliminar: el usuario tiene préstamos activos");
        }
        // Evita prestamos "huerfanos" (se perderian al recargar los JSON)
        loans.removeIf(loan -> loan.getUser().getUserId().equals(userId));
        users.remove(user);
        saveAllData();
        notifyObservers();
    }

    // ==========================================
    // LIBROS
    // ==========================================

    public Book addBook(int code, String title, String author, String genre, int year, int numCopies) {
        if (isBlank(title)) {
            throw new IllegalArgumentException("El título del libro es obligatorio");
        }
        if (numCopies < 0) {
            throw new IllegalArgumentException("El número de copias no puede ser negativo");
        }
        if (findBookByCode(code) != null) {
            throw new IllegalArgumentException("Ya existe un libro con el código " + code);
        }
        Book book = new Book(code, title, author, genre, year);
        for (int i = 0; i < numCopies; i++) {
            book.addCopy(new Copy(nextCopyId++, "Disponible", book));
        }
        books.add(book);
        saveAllData();
        notifyObservers();
        return book;
    }

    public void editBook(int code, String title, String author, String genre, int year, int newCopiesCount) {
        Book book = findBookByCode(code);
        if (book == null) {
            throw new IllegalArgumentException("Libro no encontrado");
        }
        if (isBlank(title)) {
            throw new IllegalArgumentException("El título del libro es obligatorio");
        }
        if (newCopiesCount < 0) {
            throw new IllegalArgumentException("El número de copias no puede ser negativo");
        }

        int currentCopies = book.getTotalCopies();
        if (newCopiesCount < currentCopies) {
            int toRemove = currentCopies - newCopiesCount;
            if (book.getAvailableCopies() < toRemove) {
                throw new IllegalStateException("No se pueden quitar " + toRemove
                        + " copias: solo hay " + book.getAvailableCopies() + " disponibles (el resto está prestado)");
            }
            List<Copy> copies = book.getCopies();
            for (int i = copies.size() - 1; i >= 0 && toRemove > 0; i--) {
                Copy copy = copies.get(i);
                if ("Disponible".equals(copy.getStatus())) {
                    removeLoansOfCopy(copy);
                    copies.remove(i);
                    toRemove--;
                }
            }
        } else if (newCopiesCount > currentCopies) {
            for (int i = 0; i < newCopiesCount - currentCopies; i++) {
                book.addCopy(new Copy(nextCopyId++, "Disponible", book));
            }
        }

        book.setTitle(title);
        book.setAuthor(author);
        book.setGenre(genre);
        book.setYear(year);
        saveAllData();
        notifyObservers();
    }

    public void removeBook(int code) {
        Book book = findBookByCode(code);
        if (book == null) {
            throw new IllegalArgumentException("Libro no encontrado");
        }
        for (Copy copy : book.getCopies()) {
            if (!"Disponible".equals(copy.getStatus())) {
                throw new IllegalStateException("No se puede eliminar: el libro tiene copias prestadas");
            }
        }
        for (Copy copy : book.getCopies()) {
            removeLoansOfCopy(copy);
        }
        books.remove(book);
        saveAllData();
        notifyObservers();
    }

    private void removeLoansOfCopy(Copy copy) {
        loans.removeIf(loan -> loan.getCopy() == copy);
    }

    // ==========================================
    // PRESTAMOS
    // ==========================================

    public Loan registerLoan(String userId, int copyId, LocalDate returnDate) {
        User user = findUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        Copy copy = findCopyById(copyId);
        if (copy == null) {
            throw new IllegalArgumentException("Copia no encontrada");
        }
        if (!"Disponible".equals(copy.getStatus())) {
            throw new IllegalStateException("La copia seleccionada no está disponible");
        }
        if (returnDate == null || returnDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de devolución no puede ser anterior a hoy");
        }

        Loan loan = new Loan(nextLoanId++, user, copy, LocalDate.now(), returnDate);
        copy.setStatus("Prestado");
        loans.add(loan);
        saveAllData();
        notifyObservers();
        return loan;
    }

    public void registerReturn(int loanId) {
        Loan loan = findLoanById(loanId);
        if (loan == null) {
            throw new IllegalArgumentException("Préstamo no encontrado");
        }
        if (!"Activo".equals(loan.getStatus())) {
            throw new IllegalStateException("Este préstamo ya fue devuelto");
        }
        loan.setStatus("Devuelto");
        loan.getCopy().setStatus("Disponible");
        saveAllData();
        notifyObservers();
    }

    // ==========================================
    // BUSQUEDAS
    // ==========================================

    public User findUserById(String userId) {
        for (User aux : users) {
            if (aux.getUserId().equals(userId)) {
                return aux;
            }
        }
        return null;
    }

    public Book findBookByCode(int code) {
        for (Book aux : books) {
            if (aux.getCode() == code) {
                return aux;
            }
        }
        return null;
    }

    private Copy findCopyById(int copyId) {
        for (Book book : books) {
            for (Copy aux : book.getCopies()) {
                if (aux.getCopyId() == copyId) {
                    return aux;
                }
            }
        }
        return null;
    }

    private Loan findLoanById(int loanId) {
        for (Loan aux : loans) {
            if (aux.getLoanId() == loanId) {
                return aux;
            }
        }
        return null;
    }

    /**
     * criterion: "Titulo", "Autor", "Genero" o "Codigo". Se compara solo la
     * primera letra para no depender de tildes/codificacion. Texto vacio = todos.
     */
    public List<Book> searchBooks(String criterion, String text) {
        String t = text == null ? "" : text.trim().toLowerCase();
        char c = (criterion == null || criterion.isEmpty()) ? 't' : Character.toLowerCase(criterion.charAt(0));
        List<Book> results = new ArrayList<>();
        for (Book book : books) {
            boolean match;
            if (t.isEmpty()) {
                match = true;
            } else if (c == 'a') {
                match = book.getAuthor() != null && book.getAuthor().toLowerCase().contains(t);
            } else if (c == 'g') {
                match = book.getGenre() != null && book.getGenre().toLowerCase().contains(t);
            } else if (c == 'c') {
                match = String.valueOf(book.getCode()).contains(t);
            } else {
                match = book.getTitle() != null && book.getTitle().toLowerCase().contains(t);
            }
            if (match) {
                results.add(book);
            }
        }
        return results;
    }

    public List<Loan> getLoansByUser(String userId) {
        List<Loan> results = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan.getUser().getUserId().equals(userId)) {
                results.add(loan);
            }
        }
        return results;
    }

    public List<Loan> getActiveLoansByUser(String userId) {
        List<Loan> results = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan.getUser().getUserId().equals(userId) && "Activo".equals(loan.getStatus())) {
                results.add(loan);
            }
        }
        return results;
    }

    // ==========================================
    // ESTADISTICAS
    // ==========================================

    public int getActiveLoansCount() {
        int count = 0;
        for (Loan loan : loans) {
            if ("Activo".equals(loan.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public int getOverdueLoansCount() {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (Loan loan : loans) {
            if ("Activo".equals(loan.getStatus()) &&
                    loan.getReturnDate() != null &&
                    loan.getReturnDate().isBefore(today)) {
                count++;
            }
        }
        return count;
    }

    public int getTotalCopies() {
        int count = 0;
        for (Book book : books) {
            count += book.getTotalCopies();
        }
        return count;
    }

    public int getAvailableCopies() {
        int count = 0;
        for (Book book : books) {
            count += book.getAvailableCopies();
        }
        return count;
    }

    public int getTotalUsers() {
        return users.size();
    }

    public int getTotalBooks() {
        return books.size();
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public List<Book> getAllBooks() {
        return new ArrayList<>(books);
    }

    public List<Loan> getAllLoans() {
        return new ArrayList<>(loans);
    }
}
