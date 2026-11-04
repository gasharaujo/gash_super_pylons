package net.gasharaujo.superpylons.block;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class PylonToolItem extends Item {
    private static final String KEY = "gsp_tool_data";
    private final PylonToolActions.Kind kind;

    public PylonToolItem(PylonToolActions.Kind kind, Properties properties) {
        super(properties.stacksTo(kind.isWire() ? 64 : 1));
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind.isWire() || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || saved(stack).isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            String value = saved(stack);
            save(stack, kind == PylonToolActions.Kind.WRENCH && PylonToolActions.isDirectionMode(value)
                ? PylonToolActions.cycleWrenchDirection(value, player) : "");
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PylonBlockEntityBase pylon &&
            applyTo(pylon, context.getPlayer(), context.getHand())) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        return InteractionResult.PASS;
    }

    public static void toggleWrenchMode(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (player.isSpectator() || !(stack.getItem() instanceof PylonToolItem tool) ||
            tool.kind != PylonToolActions.Kind.WRENCH) return;
        save(stack, PylonToolActions.toggleWrenchMode(saved(stack), player));
    }

    boolean applyTo(PylonBlockEntityBase pylon, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || player == null || player.isSpectator()) return false;
        ItemStack stack = player.getItemInHand(hand);
        PylonToolActions.use(kind, pylon, player, stack, saved(stack), value -> save(stack, value));
        return true;
    }

    private static String saved(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(KEY);
    }

    private static void save(ItemStack stack, String value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (value.isEmpty()) tag.remove(KEY);
            else tag.putString(KEY, value);
        });
    }

    static Block blockById(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return Blocks.IRON_BLOCK;
        return BuiltInRegistries.BLOCK.get(id);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || !saved(stack).isEmpty();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        PylonToolActions.tooltip(kind, saved(stack), lines::add);
    }
}
