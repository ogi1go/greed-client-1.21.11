package greed.client.module.combat.crystalmodules.utils.explosive;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import java.util.function.Predicate;

/** Responsive velocity/acceleration forecast with gravity, landing and swept axis clipping. */
public final class MotionPrediction {
    private record Track(Vec3d position, Vec3d velocity, Vec3d acceleration, int tick, boolean grounded, double gravity) {}
    private final Map<UUID,Track> tracks = new HashMap<>();
    public void clear() { tracks.clear(); }
    public void observe(UUID id,Vec3d position,int tick) { observe(id,position,tick,false,0); }
    public void observe(UUID id,Vec3d position,int tick,boolean grounded,double gravity) {
        if (!finite(position)) { tracks.remove(id); return; }
        Track old=tracks.get(id);
        if (old!=null && old.tick==tick) return;
        Vec3d velocity=Vec3d.ZERO, acceleration=Vec3d.ZERO;
        if (old!=null && tick>old.tick && tick-old.tick<=5) {
            double elapsed=tick-old.tick; Vec3d delta=position.subtract(old.position);
            if (delta.lengthSquared()<=64) {
                Vec3d raw=delta.multiply(1/elapsed);
                double horizontal=Math.hypot(raw.x,raw.z), factor=horizontal>1.2 ? 1.2/horizontal : 1;
                raw=new Vec3d(raw.x*factor,Math.clamp(raw.y,-4,2),raw.z*factor);
                boolean stop=raw.x*raw.x+raw.z*raw.z<.000001;
                boolean reverse=raw.x*old.velocity.x+raw.z*old.velocity.z<0;
                velocity=new Vec3d(stop?0:reverse?raw.x:raw.x*.9+old.velocity.x*.1,
                        grounded?0:raw.y,stop?0:reverse?raw.z:raw.z*.9+old.velocity.z*.1);
                if (!stop&&!reverse) acceleration=new Vec3d(Math.clamp((raw.x-old.velocity.x)/elapsed,-.09,.09),0,
                        Math.clamp((raw.z-old.velocity.z)/elapsed,-.09,.09));
            }
        }
        tracks.put(id,new Track(position,velocity,acceleration,tick,grounded,
                Double.isFinite(gravity)?Math.clamp(gravity,0,.2):.08));
        tracks.entrySet().removeIf(e -> tick<e.getValue().tick||tick-e.getValue().tick>40);
    }
    public Vec3d offset(UUID id,Box box,double ticks,Predicate<Box> free) {
        Track track=tracks.get(id);
        if (track==null || !Double.isFinite(ticks) || ticks<=0) return Vec3d.ZERO;
        double horizon=Math.clamp(ticks,0,8), dt=horizon/Math.max(1,(int)Math.ceil(horizon*4));
        int steps=Math.max(1,(int)Math.ceil(horizon*4));
        Vec3d offset=Vec3d.ZERO; double vx=track.velocity.x,vy=track.velocity.y,vz=track.velocity.z;
        boolean grounded=track.grounded;
        for (int i=0;i<steps;i++) {
            if (grounded && track.gravity>0 && free.test(box.offset(offset).offset(0,-.04,0))) grounded=false;
            double decay=Math.exp(-i*dt);
            vx=Math.clamp(vx+track.acceleration.x*dt*decay,-1.2,1.2);
            vz=Math.clamp(vz+track.acceleration.z*dt*decay,-1.2,1.2);
            double dy=grounded?0:vy*dt-.5*track.gravity*dt*dt;
            double y=clip(box,offset,0,dy,0,free); offset=offset.add(0,y,0);
            if (Math.abs(y-dy)>.00001) { if(dy<0)grounded=true;vy=0; }
            else if(!grounded)vy=(vy-track.gravity*dt)*Math.pow(.98,dt);
            double dx=vx*dt,x=clip(box,offset,dx,0,0,free);offset=offset.add(x,0,0);
            if(Math.abs(x-dx)>.00001)vx=0;
            double dz=vz*dt,z=clip(box,offset,0,0,dz,free);offset=offset.add(0,0,z);
            if(Math.abs(z-dz)>.00001)vz=0;
        }
        return finite(offset)?offset:Vec3d.ZERO;
    }
    private static double clip(Box box,Vec3d offset,double x,double y,double z,Predicate<Box> free) {
        double value=x!=0?x:y!=0?y:z;
        if(value==0)return 0;
        if(free.test(box.offset(offset).offset(x,y,z)))return value;
        double lo=0,hi=1;
        for(int i=0;i<7;i++){double mid=(lo+hi)*.5;
            if(free.test(box.offset(offset).offset(x*mid,y*mid,z*mid)))lo=mid;else hi=mid;}
        return value*lo;
    }
    private static boolean finite(Vec3d v){return Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z);}
}
