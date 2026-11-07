package greed.client.module.movement;

import greed.client.util.PlayerUtils;

import greed.mixin.EntityMixin2;
import greed.mixin.LivingEntityMixin;
import greed.client.MinecraftAccess;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.shape.VoxelShape;

public class SimulatedPlayerInput implements SimulatedMovementAccess, MinecraftAccess {
   public final PlayerEntity playerEntity2;
   public final SimulatedPlayerInput.SimulatedPlayerInputState nested0387;
   public Vec3d vec3d7;
   public Vec3d vec3d8;
   public Box box2;
   public float floatVal;
   public float floatVal2;
   public boolean flag;
   public float floatVal3;
   public int intVal;
   public boolean flag2;
   public boolean flag3;
   public boolean flag4;
   public boolean flag5;
   public boolean flag6;
   public boolean flag7;
   public boolean flag8;
   public boolean flag9;
   private final Object2DoubleMap<TagKey<Fluid>> fluidHeights;
   private final HashSet<TagKey<Fluid>> eyeFluidTags;
   private int tickCount = 0;
   private boolean movementAdjusted = false;

   private Vec3d adjustCollisions(Vec3d vec3d) {
      Box box = new Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).offset(this.vec3d7);
      List<VoxelShape> emptyCollisionShapes = Collections.emptyList();
      Vec3d vec3d2;
      if (vec3d.lengthSquared() == 0.0) {
         vec3d2 = vec3d;
      } else {
         vec3d2 = Entity.adjustMovementForCollisions(this.playerEntity2, vec3d, box, this.playerEntity2.getEntityWorld(), emptyCollisionShapes);
      }

      boolean collidedX = vec3d.x != vec3d2.x;
      boolean collidedY = vec3d.y != vec3d2.y;
      boolean collidedZ = vec3d.z != vec3d2.z;
      boolean canStepUp = this.flag4 || collidedY && vec3d.y < 0.0;
      if (this.playerEntity2.getStepHeight() > 0.0F && canStepUp && (collidedX || collidedZ)) {
         Vec3d vec3d3 = Entity.adjustMovementForCollisions(
            this.playerEntity2,
            new Vec3d(vec3d.x, this.playerEntity2.getStepHeight(), vec3d.z),
            box,
            this.playerEntity2.getEntityWorld(),
            emptyCollisionShapes
         );
         Vec3d vec3d4 = Entity.adjustMovementForCollisions(
            this.playerEntity2,
            new Vec3d(0.0, this.playerEntity2.getStepHeight(), 0.0),
            box.stretch(vec3d.x, 0.0, vec3d.z),
            this.playerEntity2.getEntityWorld(),
            emptyCollisionShapes
         );
         Vec3d vec3d5 = Entity.adjustMovementForCollisions(
               this.playerEntity2, new Vec3d(vec3d.x, 0.0, vec3d.z), box.offset(vec3d4), this.playerEntity2.getEntityWorld(), emptyCollisionShapes
            )
            .add(vec3d4);
         if (vec3d4.y < this.playerEntity2.getStepHeight() && vec3d5.horizontalLengthSquared() > vec3d3.horizontalLengthSquared()) {
            vec3d3 = vec3d5;
         }

         if (vec3d3.horizontalLengthSquared() > vec3d2.horizontalLengthSquared()) {
            return vec3d3.add(
               Entity.adjustMovementForCollisions(
                  this.playerEntity2,
                  new Vec3d(0.0, -vec3d3.y + vec3d.y, 0.0),
                  box.offset(vec3d3),
                  this.playerEntity2.getEntityWorld(),
                  emptyCollisionShapes
               )
            );
         }
      }

      return vec3d2;
   }

   private void travel(Vec3d vec3d) {
      if (this.flag8 && !this.playerEntity2.hasVehicle()) {
         double lookY = this.getRotationVector().y;
         double swimGravity = lookY < -0.2 ? 0.085 : 0.06;
         BlockPos blockPos = new BlockPos(
            MathHelper.floor(this.vec3d7.x), MathHelper.floor(this.vec3d7.y + 1.0 - 0.1), MathHelper.floor(this.vec3d7.z)
         );
         if (lookY <= 0.0 || this.nested0387.playerInput2.jump() || !this.playerEntity2.getEntityWorld().getBlockState(blockPos).getFluidState().isEmpty()) {
            this.vec3d8 = this.vec3d8.add(0.0, (lookY - this.vec3d8.y) * swimGravity, 0.0);
         }
      }

      double velocityY = this.vec3d8.y;
      double gravity = 0.08;
      boolean isFalling = this.vec3d8.y <= 0.0;
      if (this.vec3d8.y <= 0.0 && this.matchesCriteria4(StatusEffects.SLOW_FALLING)) {
         gravity = 0.01;
         this.resetFallDistance();
      }

      if (this.isTouchingWater() && this.playerEntity2.shouldSwimInFluids()) {
         double swimStartY = this.vec3d7.y;
         float horizontalDrag = this.isSprinting() ? 0.9F : 0.8F;
         float swimSpeed = 0.02F;
         float waterMovementEfficiency = (float)this.computeDouble2(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
         if (!this.flag4) {
            waterMovementEfficiency *= 0.5F;
         }

         if (waterMovementEfficiency > 0.0F) {
            horizontalDrag += (0.54600006F - horizontalDrag) * waterMovementEfficiency / 3.0F;
            swimSpeed += (this.getBaseWaterMovementSpeed() - swimSpeed) * waterMovementEfficiency / 3.0F;
         }

         if (this.matchesCriteria4(StatusEffects.DOLPHINS_GRACE)) {
            horizontalDrag = 0.96F;
         }

         this.applyMovementInput(swimSpeed, vec3d);
         this.moveEntity(this.vec3d8);
         Vec3d vec3d2 = this.vec3d8;
         if (this.flag5 && this.checkState3()) {
            vec3d2 = new Vec3d(vec3d2.x, 0.2, vec3d2.z);
         }

         this.vec3d8 = vec3d2.multiply(horizontalDrag, 0.8, horizontalDrag);
         Vec3d vec3d3 = this.playerEntity2.applyFluidMovingSpeed(gravity, isFalling, this.vec3d8);
         this.vec3d8 = vec3d3;
         if (this.flag5 && this.canStepUpAt(vec3d3.x, vec3d3.y + 0.6 - this.vec3d7.y + swimStartY, vec3d3.z)) {
            this.vec3d8 = new Vec3d(vec3d3.x, 0.3, vec3d3.z);
         }
      } else if (this.checkState() && this.playerEntity2.shouldSwimInFluids()) {
         double lavaStartY = this.vec3d7.y;
         this.applyMovementInput(0.02F, vec3d);
         this.moveEntity(this.vec3d8);
         if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
            this.vec3d8 = this.vec3d8.multiply(0.5, 0.8, 0.5);
            this.vec3d8 = this.playerEntity2.applyFluidMovingSpeed(gravity, isFalling, this.vec3d8);
         } else {
            this.vec3d8 = this.vec3d8.multiply(0.5);
         }

         if (!this.playerEntity2.hasNoGravity()) {
            this.vec3d8 = this.vec3d8.add(0.0, -gravity / 4.0, 0.0);
         }

         if (this.flag5 && this.canStepUpAt(this.vec3d8.x, this.vec3d8.y + 0.6 - this.vec3d7.y + lavaStartY, this.vec3d8.z)) {
            this.vec3d8 = new Vec3d(this.vec3d8.x, 0.3, this.vec3d8.z);
         }
      } else if (this.flag3) {
         Vec3d vec3d4 = this.vec3d8;
         if (vec3d4.y > -0.5) {
            this.floatVal3 = 1.0F;
         }

         Vec3d vec3d5 = this.getRotationVector();
         float pitchRad = this.floatVal2 * (float) (Math.PI / 180.0);
         double lookHorizontalLength = Math.sqrt(vec3d5.x * vec3d5.x + vec3d5.z * vec3d5.z);
         double horizontalVelocity = this.vec3d8.horizontalLength();
         double lookLength = vec3d5.length();
         float pitchCos = MathHelper.cos(pitchRad);
         pitchCos = (float)(pitchCos * (pitchCos * Math.min(1.0, lookLength / 0.4)));
         vec3d4 = this.vec3d8.add(0.0, gravity * (-1.0 + pitchCos * 0.75), 0.0);
         if (vec3d4.y < 0.0 && lookHorizontalLength > 0.0) {
            double glideLift = vec3d4.y * -0.1 * pitchCos;
            vec3d4 = vec3d4.add(vec3d5.x * glideLift / lookHorizontalLength, glideLift, vec3d5.z * glideLift / lookHorizontalLength);
         }

         if (pitchRad < 0.0F && lookHorizontalLength > 0.0) {
            double glideForward = horizontalVelocity * -MathHelper.sin(pitchRad) * 0.04;
            vec3d4 = vec3d4.add(-vec3d5.x * glideForward / lookHorizontalLength, glideForward * 3.2, -vec3d5.z * glideForward / lookHorizontalLength);
         }

         if (lookHorizontalLength > 0.0) {
            vec3d4 = vec3d4.add(
               (vec3d5.x / lookHorizontalLength * horizontalVelocity - vec3d4.x) * 0.1, 0.0, (vec3d5.z / lookHorizontalLength * horizontalVelocity - vec3d4.z) * 0.1
            );
         }

         this.vec3d8 = vec3d4.multiply(0.99, 0.98, 0.99);
         this.moveEntity(this.vec3d8);
      } else {
         BlockPos blockPos2 = this.getBlockPos();
         float slipperiness = this.playerEntity2.getEntityWorld().getBlockState(blockPos2).getBlock().getSlipperiness();
         float groundFriction = this.flag4 ? slipperiness * 0.91F : 0.91F;
         Vec3d vec3d6 = this.applyGroundMovement(vec3d, slipperiness);
         double motionY = vec3d6.y;
         if (this.matchesCriteria4(StatusEffects.LEVITATION)) {
            StatusEffectInstance statusEffectInstance = this.getStatusEffectInstance(StatusEffects.LEVITATION);
            if (statusEffectInstance != null) {
               motionY += (0.05 * (statusEffectInstance.getAmplifier() + 1) - vec3d6.y) * 0.2;
            }
         } else if (this.playerEntity2.getEntityWorld().isClient() && !this.playerEntity2.getEntityWorld().isChunkLoaded(blockPos2)) {
            motionY = this.vec3d7.y > this.playerEntity2.getEntityWorld().getBottomY() ? -0.1 : 0.0;
         } else if (!this.playerEntity2.hasNoGravity()) {
            motionY -= gravity;
         }

         if (this.playerEntity2.hasNoDrag()) {
            this.vec3d8 = new Vec3d(vec3d6.x, motionY, vec3d6.z);
         } else {
            this.vec3d8 = new Vec3d(vec3d6.x * groundFriction, motionY * 0.98F, vec3d6.z * groundFriction);
         }
      }

      if (this.playerEntity2.getAbilities().flying && !this.playerEntity2.hasVehicle()) {
         this.vec3d8 = new Vec3d(this.vec3d8.x, velocityY * 0.6, this.vec3d8.z);
         this.resetFallDistance();
      }
   }

   public SimulatedPlayerInput(
      PlayerEntity playerEntity,
      SimulatedPlayerInput.SimulatedPlayerInputState state,
      Vec3d vec3d,
      Vec3d vec3d2,
      Box box,
      float yaw,
      float pitch,
      boolean sprinting,
      float fallDistance,
      int noJumpDelay,
      boolean jumping,
      boolean gliding,
      boolean onGround,
      boolean horizontalCollision,
      boolean verticalCollision,
      boolean touchingWater,
      boolean swimming,
      boolean submergedInWater,
      Object2DoubleMap<TagKey<Fluid>> initialFluidHeights,
      HashSet<TagKey<Fluid>> initialEyeFluidTags
   ) {
      this.playerEntity2 = playerEntity;
      this.nested0387 = state;
      this.vec3d7 = vec3d;
      this.vec3d8 = vec3d2;
      this.box2 = box;
      this.floatVal = yaw;
      this.floatVal2 = pitch;
      this.flag = sprinting;
      this.floatVal3 = fallDistance;
      this.intVal = noJumpDelay;
      this.flag2 = jumping;
      this.flag3 = gliding;
      this.flag4 = onGround;
      this.flag5 = horizontalCollision;
      this.flag6 = verticalCollision;
      this.flag7 = touchingWater;
      this.flag8 = swimming;
      this.flag9 = submergedInWater;
      this.fluidHeights = initialFluidHeights;
      this.eyeFluidTags = initialEyeFluidTags;
   }

   private void applyMovementInput(float speed, Vec3d vec3d) {
      if (!(vec3d.lengthSquared() < 1.0E-7)) {
         Vec3d vec3d2 = (vec3d.lengthSquared() > 1.0 ? vec3d.normalize() : vec3d).multiply(speed);
         this.vec3d8 = this.vec3d8
            .add(
               vec3d2.x * MathHelper.cos(this.floatVal * (float) (Math.PI / 180.0))
                  - vec3d2.z * MathHelper.sin(this.floatVal * (float) (Math.PI / 180.0)),
               vec3d2.y,
               vec3d2.z * MathHelper.cos(this.floatVal * (float) (Math.PI / 180.0))
                  + vec3d2.x * MathHelper.sin(this.floatVal * (float) (Math.PI / 180.0))
            );
      }
   }

   private StatusEffectInstance getStatusEffectInstance(RegistryEntry<StatusEffect> effect) {
      StatusEffectInstance statusEffect = this.playerEntity2.getStatusEffect(effect);
      return statusEffect != null && statusEffect.getDuration() >= this.tickCount ? statusEffect : null;
   }

   private void moveEntity(Vec3d vec3d) {
      Vec3d vec3d2 = vec3d;
      vec3d2 = this.adjustHorizontalCollision(vec3d2);
      Vec3d vec3d3 = this.adjustCollisions(vec3d2);
      if (vec3d3.lengthSquared() > 1.0E-7) {
         this.vec3d7 = this.vec3d7.add(vec3d3);
         this.box2 = this.playerEntity2.getDimensions(this.playerEntity2.getPose()).getBoxAt(this.vec3d7);
      }

      boolean collidedX = !MathHelper.approximatelyEquals(vec3d.x, vec3d3.x);
      boolean collidedZ = !MathHelper.approximatelyEquals(vec3d.z, vec3d3.z);
      this.flag5 = collidedX || collidedZ;
      this.flag6 = vec3d.y != vec3d3.y;
      this.flag4 = this.flag6 && vec3d.y < 0.0;
      if (!this.isTouchingWater()) {
         this.updateTouchingWater();
      }

      if (this.flag4) {
         this.resetFallDistance();
      } else if (vec3d.y < 0.0) {
         this.floatVal3 = this.floatVal3 - (float)vec3d.y;
      }

      Vec3d vec3d4 = this.vec3d8;
      if (this.flag5 || this.flag6) {
         this.vec3d8 = new Vec3d(collidedX ? 0.0 : vec3d4.x, this.flag4 ? 0.0 : vec3d4.y, collidedZ ? 0.0 : vec3d4.z);
      }
   }

   private Vec3d applyGroundMovement(Vec3d vec3d, float slipperiness) {
      this.applyMovementInput(this.getWaterMovementSpeed(slipperiness), vec3d);
      this.vec3d8 = this.applyClimbingSpeed(this.vec3d8);
      this.moveEntity(this.vec3d8);
      Vec3d vec3d2 = this.vec3d8;
      BlockPos blockPos = this.getBlockPos2(this.vec3d7);
      BlockState blockState = this.getBlockState2(blockPos);
      if ((this.flag5 || this.flag2)
         && (this.checkState3() || blockState != null && blockState.isOf(Blocks.POWDER_SNOW) && PowderSnowBlock.canWalkOnPowderSnow(this.playerEntity2))) {
         vec3d2 = new Vec3d(vec3d2.x, 0.2, vec3d2.z);
      }

      return vec3d2;
   }

   private float getJumpVelocity() {
      return 0.42F * this.getJumpVelocityMultiplier() + this.getJumpBoostVelocity();
   }

   public static SimulatedPlayerInput getSimulatedPlayerInput(int ticks) {
      SimulatedPlayerInput simulatedPlayerInput = getSimulatedPlayerInput4(SimulatedPlayerInput.SimulatedPlayerInputState.fromClientPlayer(mc.player.input.playerInput));

      for (int index = 0; index < ticks; index++) {
         simulatedPlayerInput.updateState();
      }

      return simulatedPlayerInput;
   }

   private Vec3d adjustHorizontalCollision(Vec3d vec3d) {
      if (vec3d.y <= 0.0 && this.isOnGroundOrLanded()) {
         double deltaX = vec3d.x;
         double deltaZ = vec3d.z;

         double step;
         for (step = 0.05;
            deltaX != 0.0 && this.playerEntity2.getEntityWorld().isSpaceEmpty(this.playerEntity2, this.box2.offset(deltaX, -0.5, 0.0));
            deltaX += deltaX > 0.0 ? -step : step
         ) {
            if (deltaX < step && deltaX >= -step) {
               deltaX = 0.0;
               break;
            }
         }

         while (deltaZ != 0.0 && this.playerEntity2.getEntityWorld().isSpaceEmpty(this.playerEntity2, this.box2.offset(0.0, -0.5, deltaZ))) {
            if (deltaZ < step && deltaZ >= -step) {
               deltaZ = 0.0;
               break;
            }

            deltaZ += deltaZ > 0.0 ? -step : step;
         }

         while (
            deltaX != 0.0
               && deltaZ != 0.0
               && this.playerEntity2.getEntityWorld().isSpaceEmpty(this.playerEntity2, this.box2.offset(deltaX, -0.5, deltaZ))
         ) {
            deltaX = deltaX < step && deltaX >= -step
               ? 0.0
               : (deltaX > 0.0 ? deltaX - step : deltaX + step);
            if (deltaZ < step && deltaZ >= -step) {
               deltaZ = 0.0;
               break;
            }

            deltaZ += deltaZ > 0.0 ? -step : step;
         }

         if (vec3d.x != deltaX || vec3d.z != deltaZ) {
            this.movementAdjusted = true;
         }

         if (this.checkState7()) {
            vec3d = new Vec3d(deltaX, vec3d.y, deltaZ);
         }
      }

      return vec3d;
   }

   private boolean updateMovementInFluid(TagKey<Fluid> fluidTag, double velocityScale) {
      if (this.isRegionLoaded()) {
         return false;
      }

      Box box = this.box2.contract(0.001);
      int minX = MathHelper.floor(box.minX);
      int maxX = MathHelper.ceil(box.maxX);
      int minY = MathHelper.floor(box.minY);
      int maxY = MathHelper.ceil(box.maxY);
      int minZ = MathHelper.floor(box.minZ);
      int maxZ = MathHelper.ceil(box.maxZ);
      double fluidHeight = 0.0;
      boolean hasVelocity = true;
      boolean touchingFluid = false;
      Vec3d vec3d = Vec3d.ZERO;
      int sampleCount = 0;
      Mutable mutable = new Mutable();

      for (int index = minX; index < maxX; index++) {
         for (int index2 = minY; index2 < maxY; index2++) {
            for (int index3 = minZ; index3 < maxZ; index3++) {
               mutable.set(index, index2, index3);
               FluidState fluidState = this.playerEntity2.getEntityWorld().getFluidState(mutable);
               if (fluidState.isIn(fluidTag)) {
                  double fluidSurfaceY = index2 + fluidState.getHeight(this.playerEntity2.getEntityWorld(), mutable);
                  if (fluidSurfaceY >= box.minY) {
                     touchingFluid = true;
                     fluidHeight = Math.max(fluidSurfaceY - box.minY, fluidHeight);
                     if (hasVelocity) {
                        Vec3d vec3d2 = fluidState.getVelocity(this.playerEntity2.getEntityWorld(), mutable);
                        if (fluidHeight < 0.4) {
                           vec3d2 = vec3d2.multiply(fluidHeight);
                        }

                        vec3d = vec3d.add(vec3d2);
                        sampleCount++;
                     }
                  }
               }
            }
         }
      }

      if (vec3d.length() > 0.0) {
         if (sampleCount > 0) {
            vec3d = vec3d.multiply(1.0 / sampleCount);
         }

         vec3d = vec3d.multiply(velocityScale);
         if (Math.abs(this.vec3d8.x) < 0.003 && Math.abs(this.vec3d8.z) < 0.003 && vec3d.length() < 0.0045) {
            vec3d = vec3d.normalize().multiply(0.0045);
         }

         this.vec3d8 = this.vec3d8.add(vec3d);
      }

      this.fluidHeights.put(fluidTag, fluidHeight);
      return touchingFluid;
   }

   public boolean checkState() {
      return this.fluidHeights.getDouble(FluidTags.LAVA) > 0.0;
   }

   private BlockPos getBlockPos() {
      return BlockPos.ofFloored(this.vec3d7.x, this.box2.minY - 0.5000001, this.vec3d7.z);
   }

   private void updateFluidState() {
      this.flag9 = this.eyeFluidTags.contains(FluidTags.WATER);
      this.eyeFluidTags.clear();
      double eyeY = this.getEyeY() - 0.11111111F;
      if (!(
         this.playerEntity2.getVehicle() instanceof AbstractBoatEntity abstractBoatEntity
            && !abstractBoatEntity.isSubmergedInWater()
            && abstractBoatEntity.getBoundingBox().maxY >= eyeY
            && abstractBoatEntity.getBoundingBox().minY <= eyeY
      )) {
         BlockPos blockPos = BlockPos.ofFloored(this.vec3d7.x, eyeY, this.vec3d7.z);
         FluidState fluidState = this.playerEntity2.getEntityWorld().getFluidState(blockPos);
         double fluidSurfaceY = blockPos.getY() + fluidState.getHeight(this.playerEntity2.getEntityWorld(), blockPos);
         if (fluidSurfaceY > eyeY) {
            this.eyeFluidTags.addAll(fluidState.streamTags().toList());
         }
      }
   }

   public static SimulatedPlayerInput getSimulatedPlayerInput2(PlayerEntity playerEntity, int ticks) {
      SimulatedPlayerInput simulatedPlayerInput = getSimulatedPlayerInput3(playerEntity, SimulatedPlayerInput.SimulatedPlayerInputState.guessInput(playerEntity));

      for (int index = 0; index < ticks; index++) {
         simulatedPlayerInput.updateState();
      }

      return simulatedPlayerInput;
   }

   @Override
   public Vec3d getVec3d() {
      return this.playerEntity2.getEntityPos();
   }

   private boolean canStepUpAt(double offsetX, double offsetY, double offsetZ) {
      return this.isBoxClear(this.box2.offset(offsetX, offsetY, offsetZ));
   }

   private boolean isTrapdoorLadder(BlockPos blockPos, BlockState blockState) {
      if (!blockState.get(TrapdoorBlock.OPEN)) {
         return false;
      }

      BlockState blockState2 = this.playerEntity2.getEntityWorld().getBlockState(blockPos.down());
      return blockState2.isOf(Blocks.LADDER) && blockState2.get(LadderBlock.FACING).equals(blockState.get(TrapdoorBlock.FACING));
   }

   private void applyFluidJump(TagKey<Fluid> fluidTag) {
      this.vec3d8 = this.vec3d8.add(0.0, 0.04F, 0.0);
   }

   private float getJumpVelocityMultiplier() {
      float jumpMultiplier = 0.0F;
      Block block = this.getBlockState2(this.getBlockPos2(this.vec3d7)).getBlock();
      if (block != null) {
         jumpMultiplier = block.getJumpVelocityMultiplier();
      }

      float belowJumpMultiplier = 0.0F;
      Block block2 = this.getBlockState2(this.getBlockPos()).getBlock();
      if (block2 != null) {
         belowJumpMultiplier = block2.getJumpVelocityMultiplier();
      }

      return jumpMultiplier == 1.0F ? belowJumpMultiplier : jumpMultiplier;
   }

   private Vec3d getRotationVectorFromAngles(float pitch, float yaw) {
      float pitchRad = (float)(pitch * Math.PI / 180.0);
      float negYawRad = (float)(-yaw * Math.PI / 180.0);
      float cosYaw = MathHelper.cos(negYawRad);
      float sinYaw = MathHelper.sin(negYawRad);
      float cosPitch = MathHelper.cos(pitchRad);
      float sinPitch = MathHelper.sin(pitchRad);
      return new Vec3d(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
   }

   public BlockState getBlockState2(BlockPos blockPos) {
      return this.playerEntity2.getEntityWorld().getBlockState(blockPos);
   }

   public BlockPos getBlockPos2(Vec3d vec3d) {
      return new BlockPos(MathHelper.floor(vec3d.x), MathHelper.floor(vec3d.y), MathHelper.floor(vec3d.z));
   }

   private double getFluidJumpThreshold() {
      return this.playerEntity2.getStandingEyeHeight() < 0.4 ? 0.0 : 0.4;
   }

   private float getJumpBoostVelocity() {
      if (this.matchesCriteria4(StatusEffects.JUMP_BOOST)) {
         StatusEffectInstance statusEffectInstance = this.getStatusEffectInstance(StatusEffects.JUMP_BOOST);
         return 0.1F * (statusEffectInstance.getAmplifier() + 1);
      } else {
         return 0.0F;
      }
   }

   private void updateTouchingWater() {
      if (this.playerEntity2.getVehicle() instanceof AbstractBoatEntity abstractBoatEntity && !abstractBoatEntity.isSubmergedInWater()) {
         this.flag7 = false;
      } else {
         if (this.updateMovementInFluid(FluidTags.WATER, 0.014)) {
            this.resetFallDistance();
            this.flag7 = true;
         } else {
            this.flag7 = false;
         }
      }
   }

   @Override
   public void updateState() {
      this.tickCount++;
      this.movementAdjusted = false;
      if (!(this.vec3d7.y <= -70.0)) {
         this.nested0387.update();
         this.updateTouchingWater();
         this.updateFluidState();
         this.updateSwimmingState();
         if (this.intVal > 0) {
            this.intVal--;
         }

         this.flag2 = this.nested0387.playerInput2.jump();
         double velocityX = this.vec3d8.x;
         double velocityY = this.vec3d8.y;
         double velocityZ = this.vec3d8.z;
         if (Math.abs(this.vec3d8.x) < 0.003) {
            velocityX = 0.0;
         }

         if (Math.abs(this.vec3d8.y) < 0.003) {
            velocityY = 0.0;
         }

         if (Math.abs(this.vec3d8.z) < 0.003) {
            velocityZ = 0.0;
         }

         if (this.flag4) {
            this.flag3 = false;
         }

         this.vec3d8 = new Vec3d(velocityX, velocityY, velocityZ);
         if (this.flag2) {
            double fluidHeight = this.checkState() ? this.getFluidHeight(FluidTags.LAVA) : this.getFluidHeight(FluidTags.WATER);
            boolean canJumpOut = this.isTouchingWater() && fluidHeight > 0.0;
            double jumpThreshold = this.getFluidJumpThreshold();
            if (!canJumpOut || this.flag4 && !(fluidHeight > jumpThreshold)) {
               if (!this.checkState() || this.flag4 && !(fluidHeight > jumpThreshold)) {
                  if ((this.flag4 || canJumpOut && fluidHeight <= jumpThreshold) && this.intVal == 0) {
                     this.updateState6();
                     if (this.playerEntity2.equals(mc.player)) {
                        this.intVal = 10;
                     }
                  }
               } else {
                  this.applyFluidJump(FluidTags.LAVA);
               }
            } else {
               this.applyFluidJump(FluidTags.WATER);
            }
         }

         float forwardInput = this.nested0387.floatVal5 * 0.98F;
         float strafeInput = this.nested0387.floatVal4 * 0.98F;
         float verticalInput = 0.0F;
         if (this.matchesCriteria4(StatusEffects.SLOW_FALLING) || this.matchesCriteria4(StatusEffects.LEVITATION)) {
            this.resetFallDistance();
         }

         this.travel(new Vec3d(forwardInput, verticalInput, strafeInput));
      }
   }

   private void resetFallDistance() {
      this.floatVal3 = 0.0F;
   }

   private void updateSwimmingState() {
      if (this.flag8) {
         this.flag8 = this.isSprinting() && this.isTouchingWater() && !this.playerEntity2.hasVehicle();
      } else {
         this.flag8 = this.isSprinting()
            && this.checkState8()
            && !this.playerEntity2.hasVehicle()
            && this.playerEntity2.getEntityWorld().getFluidState(this.getBlockPos2(this.vec3d7)).isIn(FluidTags.WATER);
      }
   }

   private boolean isSprinting() {
      return this.flag;
   }

   public double computeDouble2(RegistryEntry<EntityAttribute> attribute) {
      return this.playerEntity2.getAttributes().getValue(attribute);
   }

   private float getWaterMovementSpeed(float slipperiness) {
      return this.flag4 ? this.getBaseWaterMovementSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness)) : this.getBaseMovementSpeed();
   }

   private float getBaseWaterMovementSpeed() {
      return 0.1F;
   }

   public boolean matchesCriteria4(RegistryEntry<StatusEffect> effect) {
      StatusEffectInstance statusEffect = this.playerEntity2.getStatusEffect(effect);
      return statusEffect != null && statusEffect.getDuration() >= this.tickCount;
   }

   public boolean checkState3() {
      BlockPos blockPos = this.getBlockPos2(this.vec3d7);
      BlockState blockState = this.getBlockState2(blockPos);
      return blockState.isIn(BlockTags.CLIMBABLE) ? true : blockState.getBlock() instanceof TrapdoorBlock && this.isTrapdoorLadder(blockPos, blockState);
   }

   private Vec3d applyClimbingSpeed(Vec3d vec3d) {
      if (!this.checkState3()) {
         return vec3d;
      }

      this.resetFallDistance();
      double clampedX = MathHelper.clamp(vec3d.x, -0.15F, 0.15F);
      double clampedZ = MathHelper.clamp(vec3d.z, -0.15F, 0.15F);
      double clampedY = Math.max(vec3d.y, -0.15F);
      if (clampedY < 0.0 && !this.getBlockState2(this.getBlockPos2(this.vec3d7)).isOf(Blocks.SCAFFOLDING) && this.playerEntity2.isHoldingOntoLadder()) {
         clampedY = 0.0;
      }

      return new Vec3d(clampedX, clampedY, clampedZ);
   }

   private double getFluidHeight(TagKey<Fluid> fluidTag) {
      return this.fluidHeights.getDouble(fluidTag);
   }

   public static SimulatedPlayerInput getSimulatedPlayerInput3(PlayerEntity playerEntity, SimulatedPlayerInput.SimulatedPlayerInputState state) {
      return new SimulatedPlayerInput(
         playerEntity,
         state,
         playerEntity.getEntityPos(),
         playerEntity.getEntityPos().subtract(new Vec3d(playerEntity.lastX, playerEntity.lastY, playerEntity.lastZ)),
         playerEntity.getBoundingBox(),
         playerEntity.getYaw(),
         playerEntity.getPitch(),
         playerEntity.isSprinting(),
         (float)playerEntity.fallDistance,
         ((LivingEntityMixin)playerEntity).getNoJumpDelay(),
         ((LivingEntityMixin)playerEntity).getJumping(),
         playerEntity.isGliding(),
         playerEntity.isOnGround(),
         playerEntity.horizontalCollision,
         playerEntity.verticalCollision,
         playerEntity.isTouchingWater(),
         playerEntity.isSwimming(),
         playerEntity.isSubmergedInWater(),
         new Object2DoubleArrayMap(((EntityMixin2)playerEntity).getFluidHeight()),
         new HashSet<>(((EntityMixin2)playerEntity).getFluidOnEyes())
      );
   }

   private boolean isRegionLoaded() {
      Box box = this.box2.expand(1.0);
      int minX = MathHelper.floor(box.minX);
      int maxX = MathHelper.ceil(box.maxX);
      int minZ = MathHelper.floor(box.minZ);
      int maxZ = MathHelper.ceil(box.maxZ);
      return !this.playerEntity2.getEntityWorld().isRegionLoaded(minX, minZ, maxX, maxZ);
   }

   public static SimulatedPlayerInput getSimulatedPlayerInput4(SimulatedPlayerInput.SimulatedPlayerInputState state) {
      ClientPlayerEntity playerEntity = mc.player;
      return new SimulatedPlayerInput(
         playerEntity,
         state,
         playerEntity.getEntityPos(),
         playerEntity.getVelocity(),
         playerEntity.getBoundingBox(),
         playerEntity.getYaw(),
         playerEntity.getPitch(),
         playerEntity.isSprinting(),
         (float)playerEntity.fallDistance,
         ((LivingEntityMixin)playerEntity).getNoJumpDelay(),
         ((LivingEntityMixin)playerEntity).getJumping(),
         playerEntity.isGliding(),
         playerEntity.isOnGround(),
         playerEntity.horizontalCollision,
         playerEntity.verticalCollision,
         playerEntity.isTouchingWater(),
         playerEntity.isSwimming(),
         playerEntity.isSubmergedInWater(),
         new Object2DoubleArrayMap(((EntityMixin2)playerEntity).getFluidHeight()),
         new HashSet<>(((EntityMixin2)playerEntity).getFluidOnEyes())
      );
   }

   private double getEyeY() {
      return this.vec3d7.y + this.playerEntity2.getStandingEyeHeight();
   }

   private boolean isOnGroundOrLanded() {
      return this.flag4
         || this.floatVal3 < 0.5 && !this.playerEntity2.getEntityWorld().isSpaceEmpty(this.playerEntity2, this.box2.offset(0.0, this.floatVal3 - 0.5, 0.0));
   }

   private Vec3d getRotationVector() {
      return this.getRotationVectorFromAngles(this.floatVal2, this.floatVal);
   }

   private float getBaseMovementSpeed() {
      float baseSpeed = 0.02F;
      return this.nested0387.playerInput2.sprint() ? baseSpeed + 0.006F : baseSpeed;
   }

   public void updateState6() {
      this.vec3d8 = this.vec3d8.add(0.0, this.getJumpVelocity() - this.vec3d8.y, 0.0);
      if (this.isSprinting()) {
         float yawRad = (float)Math.toRadians(this.floatVal);
         this.vec3d8 = this.vec3d8.add(-MathHelper.sin(yawRad) * 0.2, 0.0, MathHelper.cos(yawRad) * 0.2);
      }
   }

   public SimulatedPlayerInput clone() {
      return new SimulatedPlayerInput(
         this.playerEntity2,
         this.nested0387,
         this.vec3d7,
         this.vec3d8,
         this.box2,
         this.floatVal,
         this.floatVal2,
         this.flag,
         this.floatVal3,
         this.intVal,
         this.flag2,
         this.flag3,
         this.flag4,
         this.flag5,
         this.flag6,
         this.flag7,
         this.flag8,
         this.flag9,
         new Object2DoubleArrayMap(this.fluidHeights),
         new HashSet<>(this.eyeFluidTags)
      );
   }

   private boolean isTouchingWater() {
      return this.flag7;
   }

   protected boolean checkState7() {
      return this.nested0387.playerInput2.sneak() || this.nested0387.flag11;
   }

   public boolean checkState8() {
      return this.flag9 && this.isTouchingWater();
   }

   private boolean isBoxClear(Box box) {
      return this.playerEntity2.getEntityWorld().isSpaceEmpty(this.playerEntity2, box) && !this.playerEntity2.getEntityWorld().containsFluid(box);
   }

   public static class SimulatedPlayerInputState extends Input {
      public boolean flag11 = false;
      public float floatVal4;
      public float floatVal5;
      public PlayerInput playerInput2;
      public static final double doubleVal = 0.121;

      public void update() {
         if (this.playerInput2.forward() != this.playerInput2.backward()) {
            this.floatVal4 = this.playerInput2.forward() ? 1.0F : -1.0F;
         } else {
            this.floatVal4 = 0.0F;
         }

         if (this.playerInput2.left() == this.playerInput2.right()) {
            this.floatVal5 = 0.0F;
         } else {
            this.floatVal5 = this.playerInput2.left() ? 1.0F : -1.0F;
         }

         if (this.playerInput2.sneak()) {
            this.floatVal5 *= 0.3F;
            this.floatVal4 *= 0.3F;
         }
      }

      public SimulatedPlayerInputState(PlayerInput playerInput) {
         this.playerInput2 = playerInput;
      }

      public static SimulatedPlayerInput.SimulatedPlayerInputState guessInput(PlayerEntity playerEntity) {
         Vec3d vec3d = playerEntity.getEntityPos().subtract(new Vec3d(playerEntity.lastX, playerEntity.lastY, playerEntity.lastZ));
         double horizontalSpeedSq = vec3d.horizontalLengthSquared();
         PlayerInput playerInput = new PlayerInput(false, false, false, false, !playerEntity.isOnGround(), playerEntity.isSneaking(), horizontalSpeedSq >= 0.014641);
         if (horizontalSpeedSq > 0.0025000000000000005) {
            double relativeAngle = PlayerUtils.computeDouble2(vec3d, playerEntity.getYaw());
            double wrappedAngle = MathHelper.wrapDegrees(relativeAngle);
            playerInput = PlayerUtils.getPlayerInput2(playerInput, wrappedAngle);
         }

         return new SimulatedPlayerInput.SimulatedPlayerInputState(playerInput);
      }

      public static SimulatedPlayerInput.SimulatedPlayerInputState fromClientPlayer(PlayerInput playerInput) {
         return new SimulatedPlayerInput.SimulatedPlayerInputState(playerInput);
      }

      @Override
      public String toString() {
         return "SimulatedPlayerInput(forwards={"
            + this.playerInput2.forward()
            + "}, backwards={"
            + this.playerInput2.backward()
            + "}, left={"
            + this.playerInput2.left()
            + "}, right={"
            + this.playerInput2.right()
            + "}, jumping={"
            + this.playerInput2.jump()
            + "}, sprinting="
            + this.playerInput2.sprint()
            + ", slowDown="
            + this.playerInput2.sneak()
            + ")";
      }
   }
}
