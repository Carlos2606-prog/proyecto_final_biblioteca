package co.edu.uptc.model;

public class Copy {
    private int copyId;
    private String status;
    private transient Book book;

    public Copy(int copyId, String status, Book book) {
        this.copyId = copyId;
        this.status = status;
        this.book = book;
    }

    public int getCopyId() {
        return copyId;
    }

    public void setCopyId(int copyId) {
        this.copyId = copyId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }
}
