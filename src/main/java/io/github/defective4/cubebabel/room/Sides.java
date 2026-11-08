package io.github.defective4.cubebabel.room;

public record Sides(Side first, Side second) {
    public boolean has(Side side) {
        return first == side || second == side;
    }

    public Sides() {
        this(null, null);
    }
}
