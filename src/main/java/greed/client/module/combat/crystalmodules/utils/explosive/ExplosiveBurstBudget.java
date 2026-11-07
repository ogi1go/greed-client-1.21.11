package greed.client.module.combat.crystalmodules.utils.explosive;

/** Shared action limit, including immediate acknowledgements. */
public final class ExplosiveBurstBudget {
    public static final int MAX_ACTIONS = 1;
    private int tick = Integer.MIN_VALUE, used;
    public boolean acquire(int age,int count) {
        if(count<1||count>MAX_ACTIONS)return false;
        if(tick!=age){tick=age;used=0;}
        if(used+count>MAX_ACTIONS)return false;
        used+=count;return true;
    }
    public void reset(){tick=Integer.MIN_VALUE;used=0;}
}
