package greed.client.module.combat.crystalmodules.utils.explosive;

import java.util.Arrays;

/** Reserve consumed items until inventory acknowledgement, never reuse a stale last item. */
public final class ExplosiveInventoryLedger {
    private final int[] observed=new int[10],pending=new int[10],until=new int[10];
    private int last=Integer.MIN_VALUE;
    public ExplosiveInventoryLedger(){clear();}
    public void clear(){Arrays.fill(observed,-1);Arrays.fill(pending,0);Arrays.fill(until,0);last=Integer.MIN_VALUE;}
    public void reset(int slot){observed[slot]=-1;pending[slot]=until[slot]=0;}
    public int available(int slot,int count,int tick){
        if(slot<0||slot>9||count<0)throw new IllegalArgumentException();
        if(tick<last)clear();last=tick;
        if(tick>=until[slot])pending[slot]=0;
        if(observed[slot]>=0){
            if(count<observed[slot])pending[slot]=Math.max(0,pending[slot]-(observed[slot]-count));
            else if(count>observed[slot])pending[slot]=0;
        }
        observed[slot]=count;
        return Math.max(0,count-pending[slot]);
    }
    public void reserve(int slot,int count,int tick,int ttl){
        if(slot<0||slot>9||count<1)throw new IllegalArgumentException();
        pending[slot]+=count;until[slot]=tick+Math.max(3,ttl);
    }
}
