package uy.edu.um.doors.model;

public enum UserType {
    ADMIN(32),
    GENERIC(16);

    private final int weight;

    UserType(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
