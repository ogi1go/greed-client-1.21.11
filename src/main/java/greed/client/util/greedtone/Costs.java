package greed.client.util.greedtone;

public final class Costs {

    public static final double WALK = 20.0 / 4.317;
    public static final double SPRINT = 20.0 / 5.612;
    public static final double SWIM = 20.0 / 2.2;
    public static final double CLIMB_UP = 20.0 / 2.35;
    public static final double CLIMB_DOWN = 20.0 / 3.0;
    public static final double ASCEND = SPRINT + 4.0;
    public static final double PLACE = 20.0;
    public static final double BREAK_PENALTY = 3.0;
    public static final double DOOR = 6.0;
    public static final double PARKOUR_JUMP = 3.0;
    public static final double SLOW_MULT = 2.5;
    public static final double ICE_MULT = 0.9;
    public static final double SQRT2 = Math.sqrt(2.0);
    public static final double H_HORIZONTAL = SPRINT * 0.9;
    public static final double H_UP = 2.0;
    public static final double FLY = 20.0 / 10.9;
    public static double fall(int blocks) {
        return 5.0 * Math.sqrt(blocks);
    }

    private Costs() {
    }
}
