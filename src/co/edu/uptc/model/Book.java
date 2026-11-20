package co.edu.uptc.model;

import co.edu.uptc.structures.BinaryTree;
import java.util.Comparator;
import java.util.List;

public class Book {
    private int code;
    private String title;
    private String author;
    private String genre;
    private int year;
    private transient BinaryTree<Copy> copies;

    public Book(int code, String title, String author, String genre, int year) {
        this.code = code;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.year = year;
    }

    private BinaryTree<Copy> tree() {
        if (copies == null) {
            copies = new BinaryTree<>(Comparator.comparingInt(Copy::getCopyId));
        }
        return copies;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

     
    public List<Copy> getCopies() {
        return tree().inOrder();
    }

    public boolean addCopy(Copy copy) {
        return tree().add(copy);
    }

    public boolean removeCopy(Copy copy) {
        return tree().remove(copy);
    }

    public int getTotalCopies() {
        return tree().size();
    }

    public int getAvailableCopies() {
        int count = 0;
        for (Copy copy : tree().inOrder()) {
            if ("Disponible".equals(copy.getStatus())) {
                count++;
            }
        }
        return count;
    }
}