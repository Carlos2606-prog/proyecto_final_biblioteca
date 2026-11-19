package co.edu.uptc.model;

import java.time.LocalDate;

public class Loan {
    private int loanId;
    private User user;
    private Copy copy;
    private LocalDate loanDate;
    private LocalDate returnDate;
    private String status;

    public Loan(int loanId, User user, Copy copy, LocalDate loanDate, LocalDate returnDate) {
        this.loanId = loanId;
        this.user = user;
        this.copy = copy;
        this.loanDate = loanDate;
        this.returnDate = returnDate;
        this.status = "Activo";
    }

    public int getLoanId() {
        return loanId;
    }

    public void setLoanId(int loanId) {
        this.loanId = loanId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Copy getCopy() {
        return copy;
    }

    public void setCopy(Copy copy) {
        this.copy = copy;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDate loanDate) {
        this.loanDate = loanDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
