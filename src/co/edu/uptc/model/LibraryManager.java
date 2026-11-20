package co.edu.uptc.model;

import co.edu.uptc.persistence.DataManager;
import co.edu.uptc.persistence.DataManager.CopyData;
import co.edu.uptc.persistence.DataManager.LoanData;
import co.edu.uptc.pojo.Subject;
import co.edu.uptc.structures.BinaryTree;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LibraryManager extends Subject {

    private final BinaryTree<User> users = new BinaryTree<>(Comparator.comparing(User::getUserId));
    private final BinaryTree<Book> books = new BinaryTree<>(Comparator.comparingInt(Book::getCode));
    private final BinaryTree<Loan> loans = new BinaryTree<>(Comparator.comparingInt(Loan::getLoanId));
    private final BinaryTree<Copy> copiesIndex = new BinaryTree<>(Comparator.comparingInt(Copy::getCopyId));
    private int nextLoanId;
    private int nextCopyId;
    private DataManager dataManager;

    public LibraryManager() {
        this.nextLoanId = 1;
        this.nextCopyId = 1;
        this.dataManager = new DataManager();

        try {
            loadAllData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addUser(User user) {
        if (isBlank(user.getUserId()) || isBlank(user.getName())) {
            throw new IllegalArgumentException("El ID y el nombre del usuario son obligatorios");
        }
        if (findUserById(user.getUserId()) != null) {
            throw new IllegalArgumentException("Ya existe un usuario con el ID " + user.getUserId());
        }
        users.add(user);
        saveAllData();
        notifyObservers();
    }

    public synchronized void editUser(User updated) {
        User user = findUserById(updated.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        if (isBlank(updated.getName())) {
            throw new IllegalArgumentException("El nombre del usuario es obligatorio");
        }
        user.setName(updated.getName());
        user.setType(updated.getType());
        saveAllData();
        notifyObservers();
    }

    public synchronized void removeUser(String userId) {
        User user = findUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        if (!getActiveLoansByUser(userId).isEmpty()) {
            throw new IllegalStateException("No se puede eliminar: el usuario tiene préstamos activos");
        }
        for (Loan loan : loans.inOrder()) {
            if (loan.getUser().getUserId().equals(userId)) {
                loans.remove(loan);
            }
        }
        users.remove(user);
        saveAllData();
        notifyObservers();
    }

    public synchronized void addBook(Book book, int numCopies) {
        if (isBlank(book.getTitle())) {
            throw new IllegalArgumentException("El título del libro es obligatorio");
        }
        if (numCopies < 0) {
            throw new IllegalArgumentException("El número de copias no puede ser negativo");
        }
        if (findBookByCode(book.getCode()) != null) {
            throw new IllegalArgumentException("Ya existe un libro con el código " + book.getCode());
        }
        for (int i = 0; i < numCopies; i++) {
            attachCopy(book, new Copy(nextCopyId++, "Disponible", book));
        }
        books.add(book);
        saveAllData();
        notifyObservers();
        notifyMessage("Libro añadido: " + book.getTitle());
    }

    public synchronized void editBook(Book updated, int newCopiesCount) {
        Book book = findBookByCode(updated.getCode());
        if (book == null) {
            throw new IllegalArgumentException("Libro no encontrado");
        }
        if (isBlank(updated.getTitle())) {
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
                        + " copias: solo hay " + book.getAvailableCopies()
                        + " disponibles (el resto está prestado)");
            }
            List<Copy> copies = book.getCopies();
            for (int i = copies.size() - 1; i >= 0 && toRemove > 0; i--) {
                Copy copy = copies.get(i);
                if ("Disponible".equals(copy.getStatus())) {
                    detachCopy(book, copy);
                    toRemove--;
                }
            }
        } else if (newCopiesCount > currentCopies) {
            for (int i = 0; i < newCopiesCount - currentCopies; i++) {
                attachCopy(book, new Copy(nextCopyId++, "Disponible", book));
            }
        }

        book.setTitle(updated.getTitle());
        book.setAuthor(updated.getAuthor());
        book.setGenre(updated.getGenre());
        book.setYear(updated.getYear());
        saveAllData();
        notifyObservers();
        if (newCopiesCount != currentCopies) {
            notifyMessage("Actualización de número de copias: " + book.getTitle() + " "
                    + currentCopies + " -> " + newCopiesCount);
        }
    }

    public synchronized void removeBook(int code) {
        Book book = findBookByCode(code);
        if (book == null) {
            throw new IllegalArgumentException("Libro no encontrado");
        }
        List<Copy> copies = book.getCopies();
        for (Copy copy : copies) {
            if (!"Disponible".equals(copy.getStatus())) {
                throw new IllegalStateException("No se puede eliminar: el libro tiene copias prestadas");
            }
        }
        for (Copy copy : copies) {
            detachCopy(book, copy);
        }
        books.remove(book);
        saveAllData();
        notifyObservers();
    }

    private void removeLoansOfCopy(Copy copy) {
        for (Loan loan : loans.inOrder()) {
            if (loan.getCopy() == copy) {
                loans.remove(loan);
            }
        }
    }

    public synchronized Loan registerLoan(String userId, int copyId, LocalDate returnDate) {
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

        Book book = copy.getBook();
        int before = book.getAvailableCopies();

        Loan loan = new Loan(nextLoanId++, user, copy, LocalDate.now(), returnDate);

        copy.setStatus("Prestado");
        loans.add(loan);
        saveAllData();
        notifyObservers();

        notifyMessage("Actualización de número de copias: " + book.getTitle() + " "
                + before + " -> " + book.getAvailableCopies());

        return loan;
    }

    public synchronized void registerReturn(int loanId) {
        Loan loan = findLoanById(loanId);
        if (loan == null) {
            throw new IllegalArgumentException("Préstamo no encontrado");
        }
        if (!"Activo".equals(loan.getStatus())) {
            throw new IllegalStateException("Este préstamo ya fue devuelto");
        }
        Book book = loan.getCopy().getBook();
        int before = book.getAvailableCopies();
        loan.setStatus("Devuelto");
        loan.getCopy().setStatus("Disponible");
        saveAllData();
        notifyObservers();
        notifyMessage("Actualización de número de copias: " + book.getTitle() + " "
                + before + " -> " + book.getAvailableCopies());
    }

    public synchronized User findUserById(String userId) {
        if (userId == null) {
            return null;
        }
        return users.find(new User(userId, null, null));
    }

    public synchronized Book findBookByCode(int code) {
        return books.find(new Book(code, null, null, null, 0));
    }

    private Copy findCopyById(int copyId) {
        return copiesIndex.find(new Copy(copyId, null, null));
    }

    private Loan findLoanById(int loanId) {
        return loans.find(new Loan(loanId, null, null, null, null));
    }

    public synchronized List<Book> searchBooks(String criterion, String text) {
        String t = text == null ? "" : text.trim().toLowerCase();
        char c = (criterion == null || criterion.isEmpty()) ? 't' : Character.toLowerCase(criterion.charAt(0));
        List<Book> results = new ArrayList<>();
        for (Book book : books.inOrder()) {
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

    public synchronized List<Loan> getLoansByUser(String userId) {
        List<Loan> results = new ArrayList<>();
        for (Loan loan : loans.inOrder()) {
            if (loan.getUser().getUserId().equals(userId)) {
                results.add(loan);
            }
        }
        return results;
    }

    public synchronized List<Loan> getActiveLoansByUser(String userId) {
        List<Loan> results = new ArrayList<>();
        for (Loan loan : loans.inOrder()) {
            if (loan.getUser().getUserId().equals(userId) && "Activo".equals(loan.getStatus())) {
                results.add(loan);
            }
        }
        return results;
    }

    public synchronized int getTotalCopies() {
        return copiesIndex.size();
    }

    public synchronized int getAvailableCopies() {
        int count = 0;
        for (Copy copy : copiesIndex.inOrder()) {
            if ("Disponible".equals(copy.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public synchronized int getTotalUsers() {
        return users.size();
    }

    public synchronized int getTotalBooks() {
        return books.size();
    }

    public synchronized List<User> getAllUsers() {
        return users.inOrder();
    }

    public synchronized List<Book> getAllBooks() {
        return books.inOrder();
    }

    public synchronized List<Loan> getAllLoans() {
        return loans.inOrder();
    }

    private void loadAllData() throws IOException {
        for (User user : dataManager.loadUsers()) {
            users.add(user);
        }
        for (Book book : dataManager.loadBooks()) {
            books.add(book);
        }

        for (CopyData copyData : dataManager.loadCopies()) {
            Book book = findBookByCode(copyData.getBookCode());
            if (book != null) {
                attachCopy(book, new Copy(copyData.getCopyId(), copyData.getStatus(), book));
                if (copyData.getCopyId() >= nextCopyId) {
                    nextCopyId = copyData.getCopyId() + 1;
                }
            }
        }

        for (LoanData loanData : dataManager.loadLoans()) {
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

    private void attachCopy(Book book, Copy copy) {
        book.addCopy(copy);
        copiesIndex.add(copy);
    }

    private void detachCopy(Book book, Copy copy) {
        removeLoansOfCopy(copy);
        book.removeCopy(copy);
        copiesIndex.remove(copy);
    }

    private void reconcileCopyStatus() {
        for (Copy copy : copiesIndex.inOrder()) {
            copy.setStatus("Disponible");
        }
        for (Loan loan : loans.inOrder()) {
            if ("Activo".equals(loan.getStatus())) {
                loan.getCopy().setStatus("Prestado");
            }
        }
        saveAllData();
    }

    public synchronized void saveAllData() {
        try {
            dataManager.saveUsers(users.inOrder());
            dataManager.saveBooks(books.inOrder());

            List<CopyData> copiesData = new ArrayList<>();
            for (Book book : books.inOrder()) {
                for (Copy copy : book.getCopies()) {
                    copiesData.add(new CopyData(copy.getCopyId(), book.getCode(), copy.getStatus()));
                }
            }
            dataManager.saveCopies(copiesData);

            List<LoanData> loansData = new ArrayList<>();
            for (Loan loan : loans.inOrder()) {
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
}