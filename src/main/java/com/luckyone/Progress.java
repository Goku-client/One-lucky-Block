package com.luckyone;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The "Road to the Dragon" boss bar, dragon-kill detection, void rescue and Dragon Slayer powers. */
public final class Progress {
	private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
	private static boolean dragonSeen = false;

	public static void attach(ServerPlayer sp) {
		detach(sp);
		ServerBossEvent bar = new ServerBossEvent(Component.literal("Road to the Dragon: 0%"),
				BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
		bar.addPlayer(sp);
		BARS.put(sp.getUUID(), bar);
		refresh(sp);
	}

	public static void detach(ServerPlayer sp) {
		ServerBossEvent old = BARS.remove(sp.getUUID());
		if (old != null) old.removeAllPlayers();
	}

	/** 30% lucky blocks broken, 10% Nether, 15% End, 45% damage dealt to the dragon, 100% = dragon slain. */
	public static int percent(ServerPlayer sp) {
		if (Store.get(sp, "won") == 1) return 100;
		int pct = Math.min(Store.get(sp, "broken"), 200) * 30 / 200;
		if (Store.get(sp, "nether") == 1) pct += 10;
		if (Store.get(sp, "end") == 1) pct += 15;
		pct += Math.min(45, Store.get(sp, "dragon"));
		return Math.min(99, pct);
	}

	public static void refresh(ServerPlayer sp) {
		ServerBossEvent bar = BARS.get(sp.getUUID());
		if (bar == null) return;
		int pct = percent(sp);
		boolean won = Store.get(sp, "won") == 1;
		bar.setProgress(pct / 100.0F);
		if (won) {
			bar.setName(Component.literal("DRAGON SLAYER - 100%").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
			bar.setColor(BossEvent.BossBarColor.PURPLE);
		} else {
			bar.setName(Component.literal("Road to the Dragon: " + pct + "%  (blocks: " + Store.get(sp, "broken") + ")"));
			bar.setColor(pct >= 60 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.GREEN);
		}
	}

	public static void tick(MinecraftServer server) {
		int t = server.getTickCount();
		if (t % 5 == 0) voidWatch(server);
		if (t % 20 != 0) return;

		ServerLevel end = server.getLevel(Level.END);
		int dragonDmg = -1;
		if (end != null) {
			boolean playersInEnd = !end.players().isEmpty();
			List<? extends EnderDragon> dragons = end.getDragons();
			if (!playersInEnd) {
				dragonSeen = false;
			} else if (!dragons.isEmpty()) {
				dragonSeen = true;
				EnderDragon d = dragons.get(0);
				float frac = 1.0F - d.getHealth() / d.getMaxHealth();
				dragonDmg = Math.max(0, Math.min(45, Math.round(frac * 45.0F)));
			} else if (dragonSeen) {
				dragonSeen = false;
				victory(server, end);
			}
		}

		for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
			if (sp.level().dimension().equals(Level.NETHER)) Store.set(sp, "nether", 1);
			if (sp.level().dimension().equals(Level.END)) {
				Store.set(sp, "end", 1);
				if (dragonDmg > Store.get(sp, "dragon")) Store.set(sp, "dragon", dragonDmg);
			}
			refresh(sp);
			if (Store.get(sp, "won") == 1) applyDragonPowers(sp, t);
		}
	}

	private static void victory(MinecraftServer server, ServerLevel end) {
		for (ServerPlayer p : end.players()) {
			Store.set(p, "won", 1);
			Store.set(p, "dragon", 45);
			p.getInventory().placeItemBackInInventory(LuckyItems.dragonHeart());
			refresh(p);
		}
		server.getPlayerList().broadcastSystemMessage(
				Component.literal("THE ENDER DRAGON HAS FALLEN! The Dragon Slayers now hold its power: flight, strength, breath, roar and wing dash!")
						.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
	}

	private static void applyDragonPowers(ServerPlayer sp, int t) {
		if (!sp.isCreative() && !sp.isSpectator() && !sp.getAbilities().mayfly) {
			sp.getAbilities().mayfly = true;
			sp.onUpdateAbilities();
		}
		if (t % 40 == 0) {
			sp.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 1, true, false));
			sp.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0, true, false));
			sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false));
			sp.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 0, true, false));
		}
	}

	/** Falling into the void uses up a Void Saver and puts you back on the island. */
	private static void voidWatch(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		int limit = overworld.dimensionType().minY() - 16;
		for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
			if (!sp.level().dimension().equals(Level.OVERWORLD) || sp.getY() >= limit) continue;
			Inventory inv = sp.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack s = inv.getItem(i);
				if (LuckyItems.K_SAVER.equals(LuckyItems.keyOf(s))) {
					s.shrink(1);
					Island.teleportHome(overworld, sp);
					sp.displayClientMessage(Component.literal("Void Saver used!")
							.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
					break;
				}
			}
		}
	}

	private Progress() {}
}
