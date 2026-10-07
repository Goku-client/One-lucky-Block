package com.luckyone;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** The lucky outcomes. Better ones unlock as the player's tier rises. */
public final class Lucky {

	@FunctionalInterface
	private interface Action {
		void run(ServerLevel l, ServerPlayer p, double x, double y, double z);
	}

	private record Outcome(String name, int weight, int minTier, boolean bad, Action action) {}

	private static final List<Outcome> OUTCOMES = new ArrayList<>();
	private static final Map<UUID, Deque<String>> RECENT = new HashMap<>();

	private static final Item[] LOGS = { Items.OAK_LOG, Items.BIRCH_LOG, Items.SPRUCE_LOG,
			Items.ACACIA_LOG, Items.JUNGLE_LOG, Items.DARK_OAK_LOG };
	private static final Item[] SAPLINGS = { Items.OAK_SAPLING, Items.BIRCH_SAPLING, Items.SPRUCE_SAPLING,
			Items.ACACIA_SAPLING, Items.JUNGLE_SAPLING, Items.DARK_OAK_SAPLING };

	// ---------- helpers ----------
	private static int rnd(int lo, int hi) {
		return ThreadLocalRandom.current().nextInt(lo, hi + 1);
	}

	private static Item pick(Item[] arr) {
		return arr[ThreadLocalRandom.current().nextInt(arr.length)];
	}

	private static void drop(ServerLevel l, double x, double y, double z, ItemStack s) {
		ItemEntity e = new ItemEntity(l, x, y, z, s);
		ThreadLocalRandom r = ThreadLocalRandom.current();
		e.setDeltaMovement((r.nextDouble() - 0.5) * 0.2, 0.25, (r.nextDouble() - 0.5) * 0.2);
		l.addFreshEntity(e);
	}

	private static void drop(ServerLevel l, double x, double y, double z, Item item, int count) {
		drop(l, x, y, z, new ItemStack(item, count));
	}

	private static void spawn(ServerLevel l, EntityType<?> type, double x, double y, double z) {
		Entity e = type.create(l, EntitySpawnReason.EVENT);
		if (e != null) {
			e.setPos(x, y, z);
			l.addFreshEntity(e);
		}
	}

	private static void ring(ServerLevel l, EntityType<?> type, int n, double x, double y, double z, double radius) {
		for (int i = 0; i < n; i++) {
			double a = 2 * Math.PI * i / n;
			spawn(l, type, x + Math.cos(a) * radius, y, z + Math.sin(a) * radius);
		}
	}

	private static void add(String name, int weight, int minTier, boolean bad, Action action) {
		OUTCOMES.add(new Outcome(name, weight, minTier, bad, action));
	}

	// ---------- the outcome list ----------
	static {
		// ===== Tier 0: survival basics =====
		add("Wood Haul", 10, 0, false, (l, p, x, y, z) -> drop(l, x, y, z, pick(LOGS), rnd(12, 24)));
		add("Sapling Pack", 8, 0, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, pick(SAPLINGS), rnd(2, 4));
			drop(l, x, y, z, Items.BONE_MEAL, rnd(6, 12));
		});
		add("Stone Age", 8, 0, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.COBBLESTONE, rnd(24, 48));
			drop(l, x, y, z, Items.STONE_PICKAXE, 1);
		});
		add("Sand & Gravel", 5, 0, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.SAND, rnd(12, 24));
			drop(l, x, y, z, Items.GRAVEL, rnd(12, 24));
		});
		add("Farm Pack", 6, 0, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.WHEAT_SEEDS, 8);
			drop(l, x, y, z, Items.CARROT, 4);
			drop(l, x, y, z, Items.POTATO, 4);
		});
		add("Water Bucket", 5, 0, false, (l, p, x, y, z) -> drop(l, x, y, z, Items.WATER_BUCKET, 1));
		add("Lava Bucket", 3, 0, false, (l, p, x, y, z) -> drop(l, x, y, z, Items.LAVA_BUCKET, 1));
		add("Snack Time", 8, 0, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.BREAD, 12);
			drop(l, x, y, z, Items.COOKED_BEEF, 6);
		});
		add("Animal Friends", 6, 0, false, (l, p, x, y, z) -> {
			spawn(l, EntityType.COW, x + 1.5, y, z);
			spawn(l, EntityType.PIG, x - 1.5, y, z);
			spawn(l, EntityType.SHEEP, x, y, z + 1.5);
			spawn(l, EntityType.CHICKEN, x, y, z - 1.5);
			spawn(l, EntityType.CHICKEN, x + 1, y, z - 1);
		});
		add("XP Shower", 5, 0, false, (l, p, x, y, z) -> ExperienceOrb.award(l, new Vec3(x, y, z), rnd(20, 50)));
		add("Coal & Torches", 5, 0, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.COAL, 16);
			drop(l, x, y, z, Items.TORCH, 16);
		});
		add("Dirt Delivery", 5, 0, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.DIRT, 32);
			drop(l, x, y, z, Items.GRASS_BLOCK, 8);
		});
		add("Iron Bits", 5, 0, false, (l, p, x, y, z) -> drop(l, x, y, z, Items.IRON_INGOT, rnd(4, 8)));
		add("Lucky Gift", 4, 0, false, (l, p, x, y, z) ->
				drop(l, x, y, z, ModContent.LUCKY_BLOCK_ITEM, rnd(1, 2)));
		add("Void Saver", 4, 0, false, (l, p, x, y, z) -> drop(l, x, y, z, LuckyItems.voidSaver(rnd(1, 2))));
		add("Platform Wand", 3, 0, false, (l, p, x, y, z) -> drop(l, x, y, z, LuckyItems.platformWand()));

		add("Zombie Ambush", 5, 0, true, (l, p, x, y, z) -> ring(l, EntityType.ZOMBIE, rnd(2, 3), x, y, z, 3.0));
		add("Creeper Surprise", 3, 0, true, (l, p, x, y, z) -> spawn(l, EntityType.CREEPER, x + 2, y, z));
		add("Poison Cloud", 4, 0, true, (l, p, x, y, z) -> {
			p.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 0));
			p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
		});
		add("Lightning Strike", 3, 0, true, (l, p, x, y, z) -> {
			var bolt = EntityType.LIGHTNING_BOLT.create(l, EntitySpawnReason.EVENT);
			if (bolt != null) {
				bolt.setPos(p.getX(), p.getY(), p.getZ());
				l.addFreshEntity(bolt);
			}
		});
		add("Slippery Fingers", 3, 0, true, (l, p, x, y, z) -> {
			p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 1));
			p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 200, 1));
		});

		// ===== Tier 1: after ~40 breaks =====
		add("Iron Gear", 6, 1, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.IRON_PICKAXE, 1);
			drop(l, x, y, z, Items.IRON_AXE, 1);
			drop(l, x, y, z, Items.IRON_SHOVEL, 1);
		});
		add("Ore Pile", 5, 1, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.GOLD_INGOT, rnd(6, 10));
			drop(l, x, y, z, Items.REDSTONE, 16);
			drop(l, x, y, z, Items.LAPIS_LAZULI, 16);
		});
		add("Bow Kit", 5, 1, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.BOW, 1);
			drop(l, x, y, z, Items.ARROW, 32);
		});
		add("Study Set", 3, 1, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.ENCHANTING_TABLE, 1);
			drop(l, x, y, z, Items.BOOKSHELF, 15);
			drop(l, x, y, z, Items.LAPIS_LAZULI, 16);
		});
		add("Obsidian Stash", 4, 1, false, (l, p, x, y, z) -> drop(l, x, y, z, Items.OBSIDIAN, rnd(8, 14)));
		add("Lucky Wand", 3, 1, false, (l, p, x, y, z) -> drop(l, x, y, z, LuckyItems.luckyWand()));
		add("Nether Goodies", 4, 1, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.BLAZE_ROD, rnd(4, 8));
			drop(l, x, y, z, Items.ENDER_PEARL, rnd(3, 6));
		});
		add("Skyforged Pickaxe", 3, 1, false, (l, p, x, y, z) -> drop(l, x, y, z, LuckyItems.skyPickaxe(l)));
		add("Skeleton Squad", 4, 1, true, (l, p, x, y, z) -> ring(l, EntityType.SKELETON, rnd(2, 4), x, y, z, 3.5));
		add("Blast!", 2, 1, true, (l, p, x, y, z) -> l.explode(null, x, y + 1, z, 3.0F, Level.ExplosionInteraction.NONE));
		add("Famine", 3, 1, true, (l, p, x, y, z) -> p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400, 2)));

		// ===== Tier 2: after ~150 breaks, or after the Nether =====
		add("Diamond Gear", 5, 2, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.DIAMOND_PICKAXE, 1);
			drop(l, x, y, z, Items.DIAMOND_SWORD, 1);
			drop(l, x, y, z, Items.DIAMOND_AXE, 1);
		});
		add("Diamond Pile", 5, 2, false, (l, p, x, y, z) -> drop(l, x, y, z, Items.DIAMOND, rnd(6, 12)));
		add("Bed Bomb Kit", 3, 2, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.WHITE_BED, 4);
			drop(l, x, y, z, Items.OBSIDIAN, 6);
		});
		add("Golden Apples", 4, 2, false, (l, p, x, y, z) -> drop(l, x, y, z, Items.GOLDEN_APPLE, rnd(4, 8)));
		add("Totem", 3, 2, false, (l, p, x, y, z) -> drop(l, x, y, z, Items.TOTEM_OF_UNDYING, 1));
		add("Dragonbane Bow", 3, 2, false, (l, p, x, y, z) -> drop(l, x, y, z, LuckyItems.dragonbaneBow(l)));
		add("Skyforged Blade", 3, 2, false, (l, p, x, y, z) -> drop(l, x, y, z, LuckyItems.skyBlade(l)));
		add("Skyforged Armor", 3, 2, false, (l, p, x, y, z) -> drop(l, x, y, z, LuckyItems.skyArmor(l)));
		add("Super Lucky Gift", 2, 2, false, (l, p, x, y, z) -> drop(l, x, y, z, ModContent.SUPER_LUCKY_BLOCK_ITEM, 1));
		add("Phantom Swarm", 3, 2, true, (l, p, x, y, z) -> ring(l, EntityType.PHANTOM, 3, x, y + 3, z, 3.0));
		add("Zero Gravity", 3, 2, true, (l, p, x, y, z) -> p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 1)));
		add("Wither Touch", 3, 2, true, (l, p, x, y, z) -> p.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1)));

		// ===== Tier 3: after ~400 breaks, or once you've entered the End =====
		add("Netherite Gear", 4, 3, false, (l, p, x, y, z) -> {
			drop(l, x, y, z, Items.NETHERITE_SWORD, 1);
			drop(l, x, y, z, Items.NETHERITE_PICKAXE, 1);
			drop(l, x, y, z, Items.NETHERITE_AXE, 1);
		});
		add("Eyes of Ender", 4, 3, false, (l, p, x, y, z) -> drop(l, x, y, z, Items.ENDER_EYE, rnd(6, 12)));
		add("God Apples", 3, 3, false, (l, p, x, y, z) -> drop(l, x, y, z, Items.ENCHANTED_GOLDEN_APPLE, rnd(2, 4)));
		add("Voidreaver", 3, 3, false, (l, p, x, y, z) -> drop(l, x, y, z, LuckyItems.voidreaver(l)));
		add("Skyforged Armor II", 4, 3, false, (l, p, x, y, z) -> drop(l, x, y, z, LuckyItems.skyArmor(l)));
		add("Enderman Ambush", 3, 3, true, (l, p, x, y, z) -> ring(l, EntityType.ENDERMAN, 2, x, y, z, 4.0));
		add("Blaze Ambush", 3, 3, true, (l, p, x, y, z) -> ring(l, EntityType.BLAZE, 2, x, y + 2, z, 3.0));
	}

	// ---------- rolling ----------
	public static int tier(ServerPlayer sp) {
		int b = Store.get(sp, "broken");
		int t = b >= 400 ? 3 : b >= 150 ? 2 : b >= 40 ? 1 : 0;
		if (Store.get(sp, "nether") == 1) t = Math.max(t, 2);
		if (Store.get(sp, "end") == 1) t = 3;
		return t;
	}

	public static void roll(ServerLevel level, ServerPlayer sp, double x, double y, double z, int bonusTier) {
		int tier = Math.min(3, tier(sp) + bonusTier);
		Deque<String> recent = RECENT.computeIfAbsent(sp.getUUID(), k -> new ArrayDeque<>());

		List<Outcome> pool = new ArrayList<>();
		int total = 0;
		for (Outcome o : OUTCOMES) {
			if (o.minTier() <= tier && !recent.contains(o.name())) {
				pool.add(o);
				total += o.weight();
			}
		}
		if (pool.isEmpty()) {
			for (Outcome o : OUTCOMES) {
				if (o.minTier() <= tier) {
					pool.add(o);
					total += o.weight();
				}
			}
		}

		int r = ThreadLocalRandom.current().nextInt(total);
		Outcome chosen = pool.get(0);
		for (Outcome o : pool) {
			r -= o.weight();
			if (r < 0) {
				chosen = o;
				break;
			}
		}

		chosen.action().run(level, sp, x, y, z);

		recent.addLast(chosen.name());
		while (recent.size() > 5) recent.removeFirst();

		if (chosen.bad()) {
			level.sendParticles(ParticleTypes.SMOKE, x, y, z, 25, 0.5, 0.5, 0.5, 0.05);
			level.playSound(null, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.8F);
			sp.displayClientMessage(Component.literal("UNLUCKY: " + chosen.name())
					.withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
		} else {
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 20, 0.5, 0.5, 0.5, 0.1);
			level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.2F);
			sp.displayClientMessage(Component.literal("LUCKY: " + chosen.name())
					.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
		}
	}

	private Lucky() {}
}
