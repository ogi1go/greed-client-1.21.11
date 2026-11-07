package greed.client.util.greedtone;

public final class Flags {

    public static final int UNKNOWN = 1;
    public static final int AIR = 1 << 1;
    public static final int SOLID = 1 << 2;
    public static final int STAND = 1 << 3;
    public static final int FLUID = 1 << 4;
    public static final int WATER = 1 << 5;
    public static final int LAVA = 1 << 6;
    public static final int CLIMB = 1 << 7;
    public static final int DOOR = 1 << 8;
    public static final int DOOR_CLOSED = 1 << 9;
    public static final int HAZARD = 1 << 10;
    public static final int FLOOR_BAD = 1 << 11;
    public static final int REPLACEABLE = 1 << 12;
    public static final int ICE = 1 << 13;
    public static final int SLOW = 1 << 14;
    public static final int FALLING = 1 << 15;

    private Flags() {
    }
}
