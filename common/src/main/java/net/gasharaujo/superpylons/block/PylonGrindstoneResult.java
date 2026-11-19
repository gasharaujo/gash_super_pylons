package net.gasharaujo.superpylons.block;

import java.util.List;
import net.minecraft.world.item.ItemStack;

record PylonGrindstoneResult(ItemStack item, List<ItemStack> books, int experience) {}
