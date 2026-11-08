package io.github.defective4.cubebabel.room;

public record Sides(Side first, Side second) {

    public static final Sides NS = new Sides(Side.N, Side.S);

    public static final Sides NENW = new Sides(Side.NE, Side.NW);
    public static final Sides NESW = new Sides(Side.NE, Side.SW);
    public static final Sides NWSE = new Sides(Side.NW, Side.SE);

    public static final Sides SESW = new Sides(Side.SE, Side.SW);
    public static final Sides NNE = new Sides(Side.N, Side.NE);
    public static final Sides SSW = new Sides(Side.S, Side.SW);
    public static final Sides SNE = new Sides(Side.S, Side.NE);
    public static final Sides SNW = new Sides(Side.S, Side.NW);
    public static final Sides NSW = new Sides(Side.N, Side.SW);
    public static final Sides NSE = new Sides(Side.N, Side.SE);

    public boolean has(Side side) {
        if (first == null && second == null) return true;
        return first == side || second == side;
    }

    public Sides() {
        this(null, null);
    }
}
