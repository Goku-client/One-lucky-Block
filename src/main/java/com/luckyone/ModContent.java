package com.luckyone;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModContent {
	public static Block LUCKY_BLOCK;
	public static Block SUPER_LUCKY_BLOCK;
	public static Item LUCKY_BLOCK_ITEM;
	public static Item SUPER_LUCKY_BLOCK_ITEM;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(LuckyOneMod.MOD_ID, path);
	}

	public static void register() {
		ResourceKey<Block> luckyKey = ResourceKey.create(Registries.BLOCK, id("lucky_block"));
		LUCKY_BLOCK = Registry.register(BuiltInRegistries.BLOCK, luckyKey,
				new Block(BlockBehaviour.Properties.of()
						.strength(1.0F, 3600000.0F)
						.lightLevel(s -> 8)
						.sound(SoundType.WOOD)
						.setId(luckyKey)));
		ResourceKey<Item> luckyItemKey = ResourceKey.create(Registries.ITEM, id("lucky_block"));
		LUCKY_BLOCK_ITEM = Registry.register(BuiltInRegistries.ITEM, luckyItemKey,
				new BlockItem(LUCKY_BLOCK, new Item.Properties().setId(luckyItemKey).useBlockDescriptionPrefix()));

		ResourceKey<Block> superKey = ResourceKey.create(Registries.BLOCK, id("super_lucky_block"));
		SUPER_LUCKY_BLOCK = Registry.register(BuiltInRegistries.BLOCK, superKey,
				new Block(BlockBehaviour.Properties.of()
						.strength(1.0F, 3600000.0F)
						.lightLevel(s -> 12)
						.sound(SoundType.AMETHYST)
						.setId(superKey)));
		ResourceKey<Item> superItemKey = ResourceKey.create(Registries.ITEM, id("super_lucky_block"));
		SUPER_LUCKY_BLOCK_ITEM = Registry.register(BuiltInRegistries.ITEM, superItemKey,
				new BlockItem(SUPER_LUCKY_BLOCK, new Item.Properties().setId(superItemKey).useBlockDescriptionPrefix()));
	}

	private ModContent() {}
}
