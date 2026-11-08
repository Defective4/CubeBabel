package io.github.defective4.cubebabel.room;

public enum Side {
    N(0), NE(1), NW(5), S(3), SE(2), SW(4);

    private final int wallIndex;

    private Side(int wallIndex) {
        this.wallIndex = wallIndex;
    }

    public int getWallIndex() {
        return wallIndex;
    }

}
