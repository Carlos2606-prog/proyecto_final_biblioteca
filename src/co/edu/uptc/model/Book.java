package co.edu.uptc.model;

import java.util.ArrayList;
import java.util.List;

public class Book {
    private int code;
    private String title;
    private String author;
    private String genre;
    private int year;
    private transient List<Copy> copies;

    public Book(int code, String title, String author, String genre, int year) {
        this.code = code;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.year = year;
        this.copies = new ArrayList<>();
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public List<Copy> getCopies() {
        if (copies == null) {
            copies = new ArrayList<>();
        }
        return copies;
    }

    public void addCopy(Copy copy) {
        if (copies == null) {
            copies = new ArrayList<>();
        }
        copies.add(copy);
    }

    public int getTotalCopies() {
        if (copies == null) {
            return 0;
        }
        return copies.size();
    }

    public int getAvailableCopies() {
        if (copies == null) {
            return 0;
        }
        int count = 0;
        for (Copy copy : copies) {
            if ("Disponible".equals(copy.getStatus())) {
                count++;
            }
        }
        return count;
    }
}