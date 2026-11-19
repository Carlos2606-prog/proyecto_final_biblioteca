package co.edu.uptc.model;

public class User {
    private String userId;
    private String name;
    private String type; 

    public User(String userId, String name, String type) {
        this.userId = userId;
        this.name = name;
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
