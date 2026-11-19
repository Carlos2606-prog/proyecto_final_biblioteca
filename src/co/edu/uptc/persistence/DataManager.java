package co.edu.uptc.persistence;

import com.google.gson.reflect.TypeToken;
import co.edu.uptc.model.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;

public class DataManager {
    private static final String USERS_PATH = "data/users.json";
    private static final String BOOKS_PATH = "data/books.json";
    private static final String COPIES_PATH = "data/copies.json";
    private static final String LOANS_PATH = "data/loans.json";
    private Reader reader;
    private Writer writer;

    public DataManager() {
        this.reader = new Reader();
        this.writer = new Writer();
    }


    public List<User> loadUsers() throws IOException {
        Type type = new TypeToken<List<User>>() {
        }.getType();
        return reader.readList(USERS_PATH, type);
    }

    public void saveUsers(List<User> users) throws IOException {
        writer.writeList(USERS_PATH, users);
    }

    public List<Book> loadBooks() throws IOException {
        Type type = new TypeToken<List<Book>>() {
        }.getType();
        return reader.readList(BOOKS_PATH, type);
    }

    public void saveBooks(List<Book> books) throws IOException {
        writer.writeList(BOOKS_PATH, books);
    }

    public List<CopyData> loadCopies() throws IOException {
        Type type = new TypeToken<List<CopyData>>() {
        }.getType();
        return reader.readList(COPIES_PATH, type);
    }

    public void saveCopies(List<CopyData> copies) throws IOException {
        writer.writeList(COPIES_PATH, copies);
    }

    public List<LoanData> loadLoans() throws IOException {
        Type type = new TypeToken<List<LoanData>>() {
        }.getType();
        return reader.readList(LOANS_PATH, type);
    }

    public void saveLoans(List<LoanData> loans) throws IOException {
        writer.writeList(LOANS_PATH, loans);
    }

    public static class CopyData {
        private int copyId;
        private int bookCode;
        private String status;

        public CopyData(int copyId, int bookCode, String status) {
            this.copyId = copyId;
            this.bookCode = bookCode;
            this.status = status;
        }

        public int getCopyId() {
            return copyId;
        }

        public int getBookCode() {
            return bookCode;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class LoanData {
        private int loanId;
        private String userId;
        private int copyId;
        private LocalDate loanDate;
        private LocalDate returnDate;
        private String status;

        public LoanData(int loanId, String userId, int copyId, LocalDate loanDate,
                LocalDate returnDate, String status) {
            this.loanId = loanId;
            this.userId = userId;
            this.copyId = copyId;
            this.loanDate = loanDate;
            this.returnDate = returnDate;
            this.status = status;
        }

        public int getLoanId() {
            return loanId;
        }

        public String getUserId() {
            return userId;
        }

        public int getCopyId() {
            return copyId;
        }

        public LocalDate getLoanDate() {
            return loanDate;
        }

        public LocalDate getReturnDate() {
            return returnDate;
        }

        public String getStatus() {
            return status;
        }
    }
}
