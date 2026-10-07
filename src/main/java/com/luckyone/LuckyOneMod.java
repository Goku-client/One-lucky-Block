package com.luckyone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class LuckyOneMod implements ModInitializer {
	public static final String MOD_ID = "luckyone";
	private static final String STARTED_TAG = "luckyone_started";

	@Override
	public void onInitialize() {
		ModContent.register();

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
			entries.accept(ModContent.LUCKY_BLOCK_ITEM);
			entries.accept(ModContent.SUPER_LUCKY_BLOCK_ITEM);
		});

		ServerTickEvents.END_SERVER_TICK.register(Progress::tick);

		// Build the island, send new players to it, show the progress bar.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer p = handler.getPlayer();
			ServerLevel overworld = server.overworld();
			Island.ensure(overworld);
			if (!p.getTags().contains(STARTED_TAG)) {
				p.addTag(STARTED_TAG);
				Island.teleportHome(overworld, p);
				p.getInventory().placeItemBackInInventory(LuckyItems.voidSaver(2));
				p.sendSystemMessage(Component.literal(
						"Welcome to Lucky One Block! Break the Lucky Block again and again. Beat the Ender Dragon (the End portal is on the west side, the Nether portal on the east side) to gain its powers!")
						.withStyle(ChatFormatting.GOLD));
			}
			Progress.attach(p);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Progress.detach(handler.getPlayer()));

		// Dying sends you back to the island.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive && newPlayer.level() instanceof ServerLevel lvl) {
				Island.teleportHome(lvl.getServer().overworld(), newPlayer);
			}
			Progress.attach(newPlayer);
		});

		// Breaking a Lucky Block = a lucky roll. The island block always comes back.
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
			if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) return true;
			boolean normal = state.is(ModContent.LUCKY_BLOCK);
			boolean superBlock = state.is(ModContent.SUPER_LUCKY_BLOCK);
			if (!normal && !superBlock) return true;

			boolean center = level.dimension().equals(Level.OVERWORLD) && pos.equals(Island.LUCKY_POS);
			level.destroyBlock(pos, false, sp);
			if (center) level.setBlock(pos, ModContent.LUCKY_BLOCK.defaultBlockState(), 3);

			Store.add(sp, "broken", 1);
			double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
			int rolls = superBlock ? 3 : 1;
			for (int i = 0; i < rolls; i++) {
				Lucky.roll(level, sp, x, y, z, superBlock ? 1 : 0);
			}
			Progress.refresh(sp);
			return false;
		});

		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			String key = LuckyItems.keyOf(stack);
			if (key == null) return InteractionResult.PASS;
			if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
			return LuckyItems.use(level, sp, stack, key);
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("luckyisland").executes(ctx -> {
				ServerPlayer p = ctx.getSource().getPlayerOrException();
				Island.teleportHome(ctx.getSource().getServer().overworld(), p);
				return 1;
			}));
			dispatcher.register(Commands.literal("dragonheart").executes(ctx -> {
				ServerPlayer p = ctx.getSource().getPlayerOrException();
				if (Store.get(p, "won") != 1) {
					p.sendSystemMessage(Component.literal("Defeat the Ender Dragon first!").withStyle(ChatFormatting.RED));
				} else if (hasHeart(p)) {
					p.sendSystemMessage(Component.literal("You already have the Dragon Heart.").withStyle(ChatFormatting.YELLOW));
				} else {
					p.getInventory().placeItemBackInInventory(LuckyItems.dragonHeart());
				}
				return 1;
			}));
			dispatcher.register(Commands.literal("luckyprogress").executes(ctx -> {
				ServerPlayer p = ctx.getSource().getPlayerOrException();
				p.sendSystemMessage(Component.literal("Progress " + Progress.percent(p) + "% | Lucky blocks broken: "
						+ Store.get(p, "broken") + " | Tier " + Lucky.tier(p)).withStyle(ChatFormatting.GOLD));
				return 1;
			}));
		});
	}

	private static boolean hasHeart(ServerPlayer p) {
		Inventory inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (LuckyItems.K_HEART.equals(LuckyItems.keyOf(inv.getItem(i)))) return true;
		}
		return false;
	}
}
