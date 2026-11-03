package net.gasharaujo.superpylons.block;

import com.mojang.authlib.GameProfile;
import java.util.EnumMap;
import java.util.UUID;
import java.util.stream.Collectors;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class PylonBlockEntityBase extends BlockEntity {
    private static final GameProfile CLICKER_PROFILE =
        new GameProfile(UUID.fromString("507ad2bf-5600-42ed-9fc4-4b7076fc34b5"), "[Super Pylon]");
    private static final int BUCKET_MB = 1000;
    private static final int CONFIRM_TICKS = 100;
    private static final int MAX_LOOSE_STACKS = 64;

    protected final PylonContents contents = new PylonContents();
    private final EnumMap<PylonUpgrade.Slot, PylonUpgrade> upgrades = new EnumMap<>(PylonUpgrade.Slot.class);
    private UUID warnedPlayer;
    private long warningExpires;
    private boolean contentsDropped;
    private boolean packedDrop;

    protected PylonBlockEntityBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public long getStoredCount() { return contents.count(); }
    public ItemStack getStoredItem() { return contents.displayItem(); }
    public long getItemCapacity() { return PylonUpgrade.itemCapacity(upgrades.get(PylonUpgrade.Slot.STORAGE)); }

    public PylonUpgrade getSpecialty() { return upgrades.get(PylonUpgrade.Slot.SPECIALTY); }

    public boolean isTank() {
        PylonUpgrade specialty = getSpecialty();
        return specialty != null && specialty.isTank();
    }

    public long getFluidCapacity() { return isTank() ? fluidCapacity(getItemCapacity()) : 0; }

    private static long fluidCapacity(long itemCapacity) {
        return itemCapacity > Long.MAX_VALUE / BUCKET_MB ? Long.MAX_VALUE : itemCapacity * BUCKET_MB;
    }

    public boolean canFillFluid(Fluid fluid) {
        PylonUpgrade specialty = getSpecialty();
        return specialty != null && specialty.isTank() &&
            (!specialty.generatesFluid() || specialty.generatedFluid() == fluid);
    }

    public abstract long getFluidAmount();
    public abstract Component getFluidName();
    protected abstract void clearFluid();
    protected abstract void generateFluid(Fluid fluid, int amount);
    protected abstract void interactWithTank(Player player, InteractionHand hand);
    protected abstract void fluidModeChanged();
    protected abstract ServerPlayer getClickerPlayer(ServerLevel level, GameProfile profile);
    protected abstract ItemStack createPackedDrop();
    protected abstract void notifyPlayer(Player player, Component message);

    private Item generatedItem() {
        PylonUpgrade specialty = getSpecialty();
        return specialty == null ? Items.AIR : specialty.generatedItem();
    }

    public int insertFrom(ItemStack input) {
        if (!(level instanceof ServerLevel) || isRemoved()) return 0;
        PylonUpgrade specialty = getSpecialty();
        if (specialty != null && specialty.locksItems()) return 0;
        Item output = generatedItem();
        if (output != Items.AIR && !PylonItemRules.exactlyEqual(new ItemStack(output), input)) return 0;
        int inserted = contents.insert(input, getItemCapacity());
        if (inserted > 0) sync();
        return inserted;
    }

    public boolean use(Player player, InteractionHand hand) {
        if (player.isSpectator()) return false;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown() && tryRemoveUpgrade(player);
        }
        if (held.getItem() instanceof PylonUpgradeItem upgrade) {
            if (level instanceof ServerLevel) install(player, held, upgrade.upgrade());
            return true;
        }
        if (PylonItemRules.isModifier(held)) return false;
        if (level instanceof ServerLevel) {
            if (isTank()) interactWithTank(player, hand);
            else insertFrom(held);
        }
        return true;
    }

    private void install(Player player, ItemStack held, PylonUpgrade next) {
        PylonUpgrade previous = upgrades.get(next.slot());
        if (previous != null && previous.tier() >= next.tier()) {
            warn(player, "upgrade_slot_occupied");
            return;
        }
        if (next.locksItems() && getStoredCount() > 0) {
            warn(player, "upgrade_needs_empty");
            return;
        }
        if (next.generatedItem() != Items.AIR && getStoredCount() > 0 &&
            !PylonItemRules.exactlyEqual(getStoredItem(), new ItemStack(next.generatedItem()))) {
            warn(player, "upgrade_incompatible_items");
            return;
        }
        held.shrink(1);
        upgrades.put(next.slot(), next);
        warnedPlayer = null;
        if (next.isTank()) fluidModeChanged();
        sync();
        if (previous != null) giveUpgrade(player, previous);
    }

    private boolean tryRemoveUpgrade(Player player) {
        PylonUpgrade upgrade = upgrades.values().stream().findFirst().orElse(null);
        if (upgrade == null) return false;
        if (!(level instanceof ServerLevel)) return true;

        if (upgrade == PylonUpgrade.FLUID_STORAGE && getFluidAmount() > 0) {
            warn(player, "tank_needs_empty");
            return true;
        }
        if (upgrade.slot() == PylonUpgrade.Slot.STORAGE &&
            (getStoredCount() > PylonContents.CAPACITY || getFluidAmount() > fluidCapacity(PylonContents.CAPACITY))) {
            warn(player, "storage_over_capacity");
            return true;
        }
        if (upgrade.generatesFluid() && getFluidAmount() > 0) {
            long now = level.getGameTime();
            if (!player.getUUID().equals(warnedPlayer) || now >= warningExpires) {
                warnedPlayer = player.getUUID();
                warningExpires = now + CONFIRM_TICKS;
                warn(player, "confirm_fluid_void");
                return true;
            }
            clearFluid();
        }
        upgrades.remove(upgrade.slot());
        warnedPlayer = null;
        if (upgrade.isTank()) fluidModeChanged();
        sync();
        giveUpgrade(player, upgrade);
        return true;
    }

    private void warn(Player player, String key) {
        notifyPlayer(player, Component.translatable("message.gash_super_pylons." + key));
    }

    private static void giveUpgrade(Player player, PylonUpgrade upgrade) {
        player.getInventory().placeItemBackInInventory(upgradeStack(upgrade));
    }

    private static ItemStack upgradeStack(PylonUpgrade upgrade) {
        return new ItemStack(SuperPylonsMod.UPGRADES.get(upgrade).get());
    }

    public void withdraw(Player player) {
        if (!(level instanceof ServerLevel) || player.isSpectator()) return;
        ItemStack removed = contents.extract(player.isShiftKeyDown() ? Integer.MAX_VALUE : 1);
        if (removed.isEmpty()) return;
        sync();
        player.getInventory().placeItemBackInInventory(removed);
    }

    public void tick(ServerLevel server) {
        PylonUpgrade specialty = getSpecialty();
        if (specialty == null) return;
        PylonUpgrade speed = upgrades.get(PylonUpgrade.Slot.SPEED);
        int interval = speed == null ? 40 : speed.value();
        if (Math.floorMod(server.getGameTime(), interval) != Math.floorMod(worldPosition.hashCode(), interval)) return;

        if (specialty == PylonUpgrade.CLICKER) {
            clickBelow(server);
            return;
        }
        PylonUpgrade quantity = upgrades.get(PylonUpgrade.Slot.QUANTITY);
        int amount = quantity == null ? 1 : quantity.value();
        if (specialty.generatesFluid()) {
            generateFluid(specialty.generatedFluid(), amount * 100);
        } else if (specialty.generatedItem() != Items.AIR && getStoredCount() < getItemCapacity()) {
            if (contents.add(new ItemStack(specialty.generatedItem()), amount, getItemCapacity()) > 0) sync();
        }
    }

    private void clickBelow(ServerLevel server) {
        if (contents.count() == 0) return;
        BlockPos target = worldPosition.below();
        if (!server.hasChunkAt(target) || server.getBlockState(target).isAir() ||
            !server.getWorldBorder().isWithinBounds(target)) return;
        ServerPlayer clicker = getClickerPlayer(server, CLICKER_PROFILE);
        if (!server.mayInteract(clicker, target) ||
            server.getServer().isUnderSpawnProtection(server, target, clicker)) return;

        Vec3 hit = Vec3.atCenterOf(target).add(0, 0.5, 0);
        clicker.setPos(hit.x, hit.y, hit.z);
        clicker.setXRot(90);
        clicker.setYRot(0);
        ItemStack original = contents.displayItem();
        clicker.setItemInHand(InteractionHand.MAIN_HAND, contents.extract(1));
        try {
            clicker.gameMode.useItemOn(clicker, server, clicker.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(hit, Direction.UP, target, false));
        } finally {
            ItemStack remaining = clicker.getMainHandItem();
            boolean changed = remaining.getCount() != 1 || !PylonItemRules.exactlyEqual(original, remaining);
            clicker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            int kept = isRemoved() ? 0 : contents.add(remaining, remaining.getCount(), getItemCapacity());
            remaining.shrink(kept);
            if (!remaining.isEmpty()) Block.popResource(server, worldPosition, remaining);
            for (int slot = 0; slot < clicker.getInventory().getContainerSize(); slot++) {
                ItemStack extra = clicker.getInventory().removeItemNoUpdate(slot);
                if (!extra.isEmpty()) Block.popResource(server, worldPosition, extra);
            }
            if (changed && !isRemoved()) sync();
        }
    }

    protected String saveUpgrades() {
        return upgrades.values().stream().map(PylonUpgrade::id).collect(Collectors.joining(","));
    }

    protected void loadUpgrades(String saved) {
        upgrades.clear();
        for (String id : saved.split(",")) {
            PylonUpgrade upgrade = PylonUpgrade.byId(id);
            if (upgrade != null) upgrades.put(upgrade.slot(), upgrade);
        }
    }

    public boolean hasPackedDrop() {
        return packedDrop || (getStoredCount() > 0 &&
            getStoredCount() > (long) MAX_LOOSE_STACKS * getStoredItem().getMaxStackSize());
    }

    public void dropContents() {
        if (!(level instanceof ServerLevel) || contentsDropped) return;
        contentsDropped = true;
        clearFluid();
        packedDrop = hasPackedDrop();
        if (packedDrop) {
            Block.popResource(level, worldPosition, createPackedDrop());
            contents.restore(ItemStack.EMPTY, 0);
        } else {
            while (contents.count() > 0) {
                Block.popResource(level, worldPosition, contents.extract(Integer.MAX_VALUE));
            }
            for (PylonUpgrade upgrade : upgrades.values()) {
                Block.popResource(level, worldPosition, upgradeStack(upgrade));
            }
        }
        upgrades.clear();
    }

    protected void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
}
