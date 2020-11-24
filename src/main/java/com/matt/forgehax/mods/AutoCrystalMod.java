package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getPlayerController;

import com.matt.forgehax.Helper;
import com.matt.forgehax.events.EntityRemovedEvent;
import com.matt.forgehax.mods.managers.PositionRotationManager;
import com.matt.forgehax.mods.managers.PositionRotationManager.RotationState.Local;
import com.matt.forgehax.mods.services.AIESP;
import com.matt.forgehax.mods.managers.FriendManager;
import com.matt.forgehax.util.BlockHelper;
import com.matt.forgehax.util.SimpleTimer;
import com.matt.forgehax.util.Utils;
import com.matt.forgehax.util.BlockHelper.BlockTraceInfo;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.common.PriorityEnum;
import com.matt.forgehax.util.entity.LocalPlayerInventory;
import com.matt.forgehax.util.entity.LocalPlayerInventory.InvItem;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.potion.Potion;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;

/**
 * Created on 3/12/2018 by exkerbinator
 * Updated by Tonio_Cartonio
 */
@RegisterMod
public class AutoCrystalMod extends ToggleMod implements PositionRotationManager.MovementUpdateListener {
  
  public final Setting<Float> detonate_reach =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("detonate-reach")
          .description("maximum distance to detonate crystals")
          .defaultTo(4f)
          .min(0f)
          .max(15f)
          .build();

  public final Setting<Float> detonate_reach_wall =
    getCommandStub()
        .builders()
        .<Float>newSettingBuilder()
        .name("detonate-wall-reach")
        .description("maximum distance to detonate crystals through walls")
        .defaultTo(3f)
        .min(0f)
        .max(15f)
        .build();

  public final Setting<Float> track_reach =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("track-reach")
          .description("distance at which to track crystal damage")
          .defaultTo(8f)
          .min(0f)
          .max(20f)
          .build();
  
  public final Setting<Float> place_reach =
    getCommandStub()
        .builders()
        .<Float>newSettingBuilder()
        .name("place-reach")
        .description("maximum distance to detonate crystals")
        .defaultTo(4f)
        .min(0f)
        .max(15f)
        .build();

  public final Setting<Boolean> stop_on_eating =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("eating-stop")
          .description("Stop crystaling when eating")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> stop_on_mining =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("mining-stop")
          .description("Stop crystaling when mining")
          .defaultTo(true)
          .build();
  
  public final Setting<Integer> delay =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("delay-detonate")
          .description("delay between detonations (ms)")
          .defaultTo(200)
          .min(0)
          .max(2000)
          .build();

  public final Setting<Integer> predelay = // too lazy to rename this
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("delay-place")
          .description("delay between placing attempts (ms)")
          .defaultTo(100)
          .min(0)
          .max(2000)
          .build();

  public final Setting<Integer> crystal_cooldown =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("crystal-cooldown")
          .description("ms to wait before attacking again the same crystal")
          .defaultTo(1000)
          .min(0)
          .max(5000)
          .build();
  
  public final Setting<Boolean> vis_check =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("vis-check")
          .description("Don't break crystals you cannot see")
          .defaultTo(false)
          .build();

  public final Setting<Boolean> silent =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("silent")
          .description("Don't rotate client for breaking and placing")
          .defaultTo(true)
          .build();
  
  public final Setting<Float> maxEnemyDistance =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("maxEnemyDistance")
          .description("maximum distance from crystal to enemy")
          .defaultTo(5f)
          .min(0f)
          .max(30f)
          .build();

  public final Setting<Boolean> place =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("autoplace")
          .description("Place Crystals in good positions")
          .defaultTo(true)
          .build();

  public final Setting<Integer> switch_back_delay =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("switch-delay")
          .description("Ticks to wait before restoring initial held item")
          .min(1)
          .max(60)
          .defaultTo(10)
          .build();

  public final Setting<Integer> multiplace =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("multiplace")
          .description("How many crystals to place at once")
          .defaultTo(1)
          .min(1)
          .max(64)
          .build();

  public final Setting<Boolean> insta_break =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("insta-break")
          .description("Reset break timer when placing a crystal")
          .defaultTo(false)
          .build();

  public final Setting<Float> minDamage =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("damage-min")
          .description("Minimum damage required to place a crystal")
          .defaultTo(5f)
          .min(0f)
          .max(50f)
          .build();

  public final Setting<Float> minDamageFace =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("face-damage-min")
          .description("Minimum damage required to place a crystal for targets in holes")
          .defaultTo(5f)
          .min(0f)
          .max(50f)
          .build();

  public final Setting<Float> minHPFace =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("face-hp-min")
          .description("Use face-damage threshold for players below this HP")
          .defaultTo(5f)
          .min(0f)
          .max(50f)
          .build();

  public final Setting<Float> maxSelfDamage =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("self-damage")
          .description("Maximum damage to deal to self")
          .defaultTo(5f)
          .min(0.0f)
          .max(50f)
          .build();

  public final Setting<Boolean> friend_filter =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("friend-filter")
          .description("Don't try to crystal friends")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> log_actions =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("log-actions")
          .description("Show where crystals are placed and which are being blown")
          .defaultTo(false)
          .build();
  public final Setting<Boolean> log_damage =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("log-damage")
          .description("Show expected damage values")
          .defaultTo(false)
          .build();
  public final Setting<Boolean> deleteCrystal =
        getCommandStub()
            .builders()
            .<Boolean>newSettingBuilder()
            .name("deleteCrystal")
            .description("Delete Crystals client side after you hit them")
            .defaultTo(false)
            .build();
  public final Setting<Boolean> spawnCrystal =
        getCommandStub()
            .builders()
            .<Boolean>newSettingBuilder()
            .name("spawnCrystal")
            .description("Immediately spawn crystals clientside after attempting to place them")
            .defaultTo(false)
            .build();


  public AutoCrystalMod() {
    super(Category.COMBAT, "AutoCrystal", false, "Automatically detonates nearby end crystals");
  }
  
  private SimpleTimer timer_break = new SimpleTimer(); // The timer
  private SimpleTimer timer_place = new SimpleTimer(); // The timer
  private boolean warned = false; // True if the player has been warned about lack of obsidian
  private List<PossibleTarget> places = new ArrayList<PossibleTarget>(); // Where we can place, I don't want to realloc this every tick
  
  private Map<Entity, Long> attacked_crystals = new ConcurrentHashMap<>();
  private int eid = -750;
  private List<EntityEnderCrystal> fake_crystals = new LinkedList<>();
  
  @Override
  protected void onEnabled() {
    PositionRotationManager.getManager().register(this, PriorityEnum.HIGHEST);
    timer_break.start();
    timer_place.start();
  }
  
  @Override
  protected void onDisabled() {
    PositionRotationManager.getManager().unregister(this);
    for (EntityEnderCrystal c : fake_crystals)
      getWorld().removeEntity(c);
    fake_crystals.clear();
  }

  @SubscribeEvent
  public void onEntityRemoved(EntityRemovedEvent event) {
    if (event.getEntity() instanceof EntityEnderCrystal && attacked_crystals.get(event.getEntity()) != null) {
      attacked_crystals.remove(event.getEntity());
    }
  }

  @SubscribeEvent
  public void onEntityAdded(EntityJoinWorldEvent event) {
    if (event.getEntity() instanceof EntityEnderCrystal) {
      for (EntityEnderCrystal c : fake_crystals) {
        if (c.getPosition().equals(event.getEntity().getPosition())) {
          fake_crystals.remove(c);
          getWorld().removeEntity(c);
          return;
        }
      }
    }
  }
  
  /* Returns the closest valid enemy, or null. Keeps track of friends if setting is enabled */
  private EntityPlayer enemyWithinDistance(Entity e, float dist) {
    Vec3d delta = new Vec3d(dist, dist, dist);
    AxisAlignedBB bb =
        new AxisAlignedBB(e.getPositionVector().subtract(delta), e.getPositionVector().add(delta));
    return getWorld()
        .getEntitiesWithinAABB(EntityPlayer.class, bb)
        .stream()
        .filter(p -> !p.isEntityEqual(getLocalPlayer()))
        .filter(p -> !friend_filter.get() || !FriendManager.isFriendly(p.getName()))
        .min(Comparator.comparing(p -> e.getDistanceSq(p) < dist * dist))
        .orElse(null);
  }

  // Useful Objects for streams
  private class PossibleTarget {
    final BlockPos pos;
    final float damage, selfdamage;

    PossibleTarget(BlockPos pos, float damage, float selfdamage) {
      this.pos = pos;
      this.damage = damage;
      this.selfdamage = selfdamage;
    }

    public BlockPos getPos() { return this.pos; }
    public float getDamage() { return this.damage; }
    public float getSelfDamage() { return this.selfdamage; }
  }
  private class PossibleCrystal {
    final EntityEnderCrystal ec;
    final float selfdamage;
    float damage = 0;
    EntityPlayer target = null;

    PossibleCrystal(EntityEnderCrystal ec) {
      this.ec = ec;
      this.selfdamage = calculateDamage(this.ec, getLocalPlayer());
    }

    public EntityEnderCrystal getCrystal() { return this.ec; }
    public float getDamage() { return this.damage; }
    public float getSelfDamage() { return this.selfdamage; }
    public EntityPlayer getTarget() { return this.target; }


    public boolean findTarget(float maxdist) {
      this.target = enemyWithinDistance(this.ec, maxdist);
      if (this.target != null) {
        this.damage = calculateDamage(this.ec, this.target);
        return true;
      }
      return false;
    }
  }

  @Override
  public void onLocalPlayerMovementUpdate(Local state) {
    if (getWorld() == null || getLocalPlayer() == null) return;

    // Don't interfere with chorush, gaps, potions and xp bottles
    if (stop_on_eating.get() && MC.gameSettings.keyBindUseItem.isKeyDown() && 
        (LocalPlayerInventory.getSelected().getItem().equals(Items.GOLDEN_APPLE) ||
        LocalPlayerInventory.getSelected().getItem().equals(Items.CHORUS_FRUIT) ||
        LocalPlayerInventory.getSelected().getItem().equals(Items.POTIONITEM) ||
        LocalPlayerInventory.getSelected().getItem().equals(Items.EXPERIENCE_BOTTLE)))
      return;

    // Don't intergere with mining (with top gear)
    if (stop_on_mining.get() && MC.gameSettings.keyBindAttack.isKeyDown() && 
        (LocalPlayerInventory.getSelected().getItem().equals(Items.DIAMOND_PICKAXE)))
      return;

    // Detonate
    final int wait_before_attacking_again = crystal_cooldown.get(); // fuck me long ass name, find better one
    Vec3d delta = new Vec3d(detonate_reach.get(), detonate_reach.get(), detonate_reach.get());
    AxisAlignedBB bb = // The bounding Box by the size of the reach setting
        new AxisAlignedBB(
            getLocalPlayer().getPositionVector().subtract(delta),
            getLocalPlayer().getPositionVector().add(delta));

    PossibleCrystal target; // Best Crystal to explode
      
    target = getWorld().getEntitiesWithinAABB(EntityEnderCrystal.class, bb)
        .stream()
        .filter(c -> attacked_crystals.get(c) == null || System.currentTimeMillis() - attacked_crystals.get(c) > wait_before_attacking_again)
        .filter(c -> !vis_check.get() || getLocalPlayer().canEntityBeSeen(c))
        .filter(c -> detonate_reach_wall.get() > detonate_reach.get() || 
                (!getLocalPlayer().canEntityBeSeen(c) ?
                      getLocalPlayer().getDistance(c) < detonate_reach_wall.get() : true))
        .map(c -> new PossibleCrystal(c)) // Make data structure to make easier
        .filter(c -> c.getSelfDamage() < maxSelfDamage.get())  // Does not deal too much self damage
        .filter(c -> c.findTarget(maxEnemyDistance.get()))  // Has an enemy in range
        .filter(c -> (minDamageFace.get() >=  minDamage.get() || !((c.getTarget().getHealth() + c.getTarget().getAbsorptionAmount()) < minHPFace.get())) ?
                      c.getDamage() > minDamage.get() : c.getDamage() > minDamageFace.get())  // Deals enough damage
        .max(Comparator.comparing(c -> ((PossibleCrystal) c).getDamage())  // The one that deals most damage to enemy and least to player
                  .thenComparing(Comparator.comparing(c -> ((PossibleCrystal) c).getSelfDamage())).reversed())
        .orElse(null);
    
    if (target != null) { // Set serverside angles, send the attack control and swing. Reset timer.
      if (timer_break.hasTimeElapsed(delay.get())) {
        state.setViewAngles(Utils.getLookAtAngles(target.getCrystal().getPositionVector()), silent.get());
        getPlayerController().attackEntity(getLocalPlayer(), target.getCrystal());
        getLocalPlayer().swingArm(EnumHand.MAIN_HAND);
        timer_break.start();
        if (fake_crystals.contains(target.getCrystal())) {
          getWorld().removeEntity(target.getCrystal());
          fake_crystals.remove(target.getCrystal());
        } else {
          attacked_crystals.put(target.getCrystal(), System.currentTimeMillis());
          if (deleteCrystal.get()) target.getCrystal().setDead(); //clientside crystals go brrrrrrrrr
          if (log_actions.get()) AIESP.addCrystalDetonation(target.getCrystal(), System.currentTimeMillis()); // Debug
          if (log_damage.get()) Helper.printInform("Detonated Crystal should do %.1f damage", target.getDamage());
        }
      }

    }

    if (place.get() && timer_place.hasTimeElapsed(predelay.get())) {
      // Place a crystal
      Vec3d delta_p = new Vec3d(track_reach.get(), track_reach.get(), track_reach.get());
      AxisAlignedBB bb_p =  // Bigger BB because we need to keep track of players in reach of the explosion
          new AxisAlignedBB(
              getLocalPlayer().getPositionVector().subtract(delta_p),
              getLocalPlayer().getPositionVector().add(delta_p));

      InvItem crystal;
      boolean offhand;
      if (LocalPlayerInventory.getOffhand().getItemStack().getItem().equals(Items.END_CRYSTAL)) {
        offhand = true;
        crystal = LocalPlayerInventory.getOffhand();
      } else {
        offhand = false;
        crystal = LocalPlayerInventory.getHotbarInventory()
          .stream() // Find EndCrystal in hotbar
          .filter(InvItem::nonNull)
          .filter(held_item -> held_item.getItem().equals(Items.END_CRYSTAL))
          .max(Comparator.comparingInt(LocalPlayerInventory::getHotbarDistance))
          .orElse(InvItem.EMPTY);
      
        if (crystal == null || crystal.equals(InvItem.EMPTY)) {
          if (!warned) {
            Helper.printError("Out of Crystals");
            warned = true;
          }
          return;
        }
      }

      warned = false;

      // Get a sphere of all the block positions where a crystal can be placed and that do not deal too much damage to self
      List<BlockPos> blocks = getSphere(getLocalPlayer().getPosition(), place_reach.get(), place_reach.getAsInteger(), false, true, 0).stream()
        .filter(pos -> canPlaceCrystal(pos))
        .filter(pos -> calculateDamage(pos, getLocalPlayer()) < maxSelfDamage.get())
        .collect(Collectors.toList());

      if (blocks.isEmpty()) return; // Nowhere to place anyway

      places.clear();
      // Make a list with all the possible blockPositions, sorted by best damage
      getWorld()
        .getEntitiesWithinAABB(EntityPlayer.class, bb_p)
        .stream()
        .filter(p -> !p.equals(getLocalPlayer()))
        .filter(p -> !friend_filter.get() || !FriendManager.isFriendly(p.getName()))
        .forEach(p -> {
              blocks.stream()
                    .map(pos -> new PossibleTarget(pos, calculateDamage(pos, p), calculateDamage(pos, getLocalPlayer())))
                    .filter(t -> (minDamageFace.get() >=  minDamage.get() || !((p.getHealth() + p.getAbsorptionAmount()) < minHPFace.get())) ?
                            t.getDamage() > minDamage.get() : t.getDamage() > minDamageFace.get())  // Deals enough damage
                    .forEach(t -> places.add(t));
        });

      if (places.isEmpty()) return;

      // I want to get the block which deals most damage to target and least damage to player
      List<PossibleTarget> targets = places.stream()                           // WTF Java why this cast!!!
                  .sorted(Comparator.comparingDouble(t -> ((PossibleTarget) t).getDamage()).reversed()
                          .thenComparing(Comparator.comparingDouble(t -> ((PossibleTarget) t).getSelfDamage())))
                  // .map(t -> t.getPos())
                  .collect(Collectors.toList());
      
      if (!offhand) LocalPlayerInventory.setSelected(crystal, true, ticks -> ticks > switch_back_delay.get());
      
      int placed = 0;
      for (PossibleTarget tgt : targets) {
        if (placed >= multiplace.get()) break;
        if (placed > 0 && !canPlaceCrystal(tgt.getPos())) continue; // recheck for multiplace
        if (log_damage.get()) Helper.printInform("Placed Crystal should do %.1f damage", tgt.getDamage());
        placeCrystal(state, tgt.getPos(), crystal, (offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND));
        if (log_actions.get()) AIESP.addCrystalPlacement(tgt.getPos(), System.currentTimeMillis()); // Debug
        placed++;
      }
      timer_place.start();
      if (insta_break.get())
        timer_break.reset();
    }
  }
  
  private void placeCrystal(Local state, BlockPos target, InvItem item, EnumHand hand) {
    // Trace to get the orientation player should have
    BlockTraceInfo trace =
      Optional.ofNullable(BlockHelper.newBlockTrace(target, EnumFacing.UP))
          .orElse(null);
    
    if (trace == null) {
      LOGGER.warn("Trace for placing Crystal failed");
      // we really cannot place here
      return;
    }

    Vec3d hit = trace.getHitVec();
    state.setViewAngles(Utils.getLookAtAngles(hit.addVector(0d, 1d, 0d)), silent.get()); // Set angles

    getPlayerController() // Click
        .processRightClickBlock(
            getLocalPlayer(),
            getWorld(),
            target,
            (target.getY() < 255 ? EnumFacing.UP : EnumFacing.DOWN), // this to place at y limit
            hit,
            hand);

    // getNetworkManager().sendPacket(new CPacketAnimation(EnumHand.MAIN_HAND));
    getLocalPlayer().swingArm(hand); // swing swing

    if (spawnCrystal.get()) {
      EntityEnderCrystal newcrystal = new EntityEnderCrystal(getWorld(),
            target.getX() + 0.5d, target.getY() + 1d, target.getZ() + 0.5d);
      // newcrystal.getDataManager().register(SHOW_BOTTOM, Boolean.valueOf(false));
      getWorld().addEntityToWorld(eid, newcrystal);
      eid--;
      fake_crystals.add(newcrystal);
    }
  }

  // uggh all this shit just to hide bottom
  // private static final DataParameter<Boolean> SHOW_BOTTOM = EntityDataManager.<Boolean>createKey(EntityEnderCrystal.class, DataSerializers.BOOLEAN);

  // Thanks 086!
  private boolean canPlaceCrystal(BlockPos blockPos) {
    BlockPos boost = blockPos.add(0, 1, 0);
    BlockPos boost2 = blockPos.add(0, 2, 0);
    return (getWorld().getBlockState(blockPos).getBlock() == Blocks.BEDROCK
              || getWorld().getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN)
            && getWorld().getBlockState(boost).getBlock() == Blocks.AIR
            && getWorld().getBlockState(boost2).getBlock() == Blocks.AIR
            && getWorld().getEntitiesWithinAABB(EntityEnderCrystal.class, // No Ender Crystals around!
                                            new AxisAlignedBB(blockPos.add(1, 1, 1), blockPos.add(-1, -1, -1))).isEmpty()
            && getWorld().getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost)).isEmpty()
            && getWorld().getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost2)).isEmpty();
  }

  public List<BlockPos> getSphere(BlockPos loc, float r, int h, boolean hollow, boolean sphere, int plus_y) {
    List<BlockPos> circleblocks = new ArrayList<>();
    int cx = loc.getX();
    int cy = loc.getY();
    int cz = loc.getZ();
    for (int x = cx - (int) r; x <= cx + r; x++) {
        for (int z = cz - (int) r; z <= cz + r; z++) {
            for (int y = (sphere ? cy - (int) r : cy); y < (sphere ? cy + r : cy + h); y++) {
                double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0);
                if (dist < r * r && !(hollow && dist < (r - 1) * (r - 1))) {
                    BlockPos l = new BlockPos(x, y + plus_y, z);
                    circleblocks.add(l);
                }
            }
        }
    }
    return circleblocks;
  }

  public static float calculateDamage(EntityEnderCrystal crystal, Entity entity) {
    return calculateDamage(crystal.posX, crystal.posY, crystal.posZ, entity);
  }

  public static float calculateDamage(BlockPos pos, Entity entity) {
    return calculateDamage(pos.getX() + 0.5D, pos.getY() + 1D, pos.getZ() + 0.5D, entity);
  }

  public static float calculateDamage(double posX, double posY, double posZ, Entity entity) {
    float doubleExplosionSize = 6.0F * 2.0F;
    double distancedSize = entity.getDistance(posX, posY, posZ) / (double) doubleExplosionSize;
    Vec3d vec3d = new Vec3d(posX, posY, posZ);
    double blockDensity = entity.world.getBlockDensity(vec3d, entity.getEntityBoundingBox());
    double v = (1.0D - distancedSize) * blockDensity;
    float damage = (float) ((int) ((v * v + v) / 2.0D * 7.0D * (double) doubleExplosionSize + 1.0D));
    double finalD = 1;
    /*if (entity instanceof EntityLivingBase)
        finalD = getBlastReduction((EntityLivingBase) entity,getDamageMultiplied(damage));*/
    if (entity instanceof EntityLivingBase) {
        finalD = getBlastReduction((EntityLivingBase) entity, getDamageMultiplied(damage), new Explosion(getWorld(), null, posX, posY, posZ, 6F, false, true));
    }
    return (float) finalD;
  }

  public static float getBlastReduction(EntityLivingBase entity, float damage, Explosion explosion) {
    if (entity instanceof EntityPlayer) {
        EntityPlayer ep = (EntityPlayer) entity;
        DamageSource ds = DamageSource.causeExplosionDamage(explosion);
        damage = CombatRules.getDamageAfterAbsorb(damage, (float) ep.getTotalArmorValue(), 
                          (float) ep.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());

        int k = EnchantmentHelper.getEnchantmentModifierDamage(ep.getArmorInventoryList(), ds);
        float f = MathHelper.clamp(k, 0.0F, 20.0F);
        damage = damage * (1.0F - f / 25.0F);

        if (entity.isPotionActive(Objects.requireNonNull(Potion.getPotionById(11)))) {
            damage = damage - (damage / 5);
        }

        damage = Math.max(damage, 0.0F);
        return damage;
    }
    damage = CombatRules.getDamageAfterAbsorb(damage, (float) entity.getTotalArmorValue(),
                        (float) entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
    return damage;
  }

  private static float getDamageMultiplied(float damage) {
    int diff = getWorld().getDifficulty().getDifficultyId();
    return damage * (diff == 0 ? 0 : (diff == 2 ? 1 : (diff == 1 ? 0.5f : 1.5f)));
  }
}
