package com.luckyone;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Custom (non-vanilla) items: Void Saver, Platform Wand, Lucky Wand, Dragon Heart and OP gear. */
public final class LuckyItems {
	public static final String K_SAVER = "lucky_saver";
	public static final String K_PLATFORM = "lucky_platform";
	public static final String K_WAND = "lucky_wand";
	public static final String K_HEART = "lucky_heart";
	private static final String[] KEYS = { K_SAVER, K_PLATFORM, K_WAND, K_HEART };

	private static final Map<String, Long> COOLDOWNS = new HashMap<>();

	private static Component name(String text, ChatFormatting color) {
		return Component.literal(text).withStyle(color, ChatFormatting.BOLD);
	}

	public static ItemStack make(Item item, int count, String key, Component name, List<String> lore) {
		ItemStack s = new ItemStack(item, count);
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(key, true);
		s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		s.set(DataComponents.CUSTOM_NAME, name);
		s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		List<Component> lines = new ArrayList<>();
		for (String l : lore) lines.add(Component.literal(l).withStyle(ChatFormatting.GRAY));
		s.set(DataComponents.LORE, new ItemLore(lines));
		return s;
	}

	public static String keyOf(ItemStack stack) {
		if (stack.isEmpty()) return null;
		CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
		if (cd == null) return null;
		for (String k : KEYS) {
			if (cd.copyTag().contains(k)) return k;
		}
		return null;
	}

	public static ItemStack voidSaver(int n) {
		return make(Items.POPPED_CHORUS_FRUIT, n, K_SAVER, name("Void Saver", ChatFormatting.AQUA),
				List.of("Keep it in your inventory.", "If you fall into the void, it saves you", "and teleports you to the island."));
	}

	public static ItemStack platformWand() {
		return make(Items.BLAZE_ROD, 1, K_PLATFORM, name("Platform Wand", ChatFormatting.GREEN),
				List.of("Right-click: builds a 5x5 platform", "under your feet. 10s recharge."));
	}

	public static ItemStack luckyWand() {
		return make(Items.BREEZE_ROD, 1, K_WAND, name("Lucky Wand", ChatFormatting.GOLD),
				List.of("Right-click: a free lucky roll!", "30s recharge."));
	}

	public static ItemStack dragonHeart() {
		return make(Items.HEART_OF_THE_SEA, 1, K_HEART, name("Dragon Heart", ChatFormatting.LIGHT_PURPLE),
				List.of("Right-click: Dragon Breath cone", "Sneak + right-click: Dragon Roar", "Look up + right-click: Wing Dash"));
	}

	// ---------- OP gear (enchant levels far above vanilla) ----------
	@SuppressWarnings("unchecked")
	public static ItemStack gear(ServerLevel level, Item item, Component name, Object... pairs) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME, name);
		for (int i = 0; i + 1 < pairs.length; i += 2) {
			ResourceKey<Enchantment> key = (ResourceKey<Enchantment>) pairs[i];
			int lvl = (Integer) pairs[i + 1];
			Holder<Enchantment> holder = level.registryAccess()
					.lookupOrThrow(Registries.ENCHANTMENT)
					.getOrThrow(key);
			stack.enchant(holder, lvl);
		}
		return stack;
	}

	public static ItemStack skyPickaxe(ServerLevel l) {
		return gear(l, Items.IRON_PICKAXE, name("Skyforged Pickaxe", ChatFormatting.YELLOW),
				Enchantments.EFFICIENCY, 8, Enchantments.FORTUNE, 5, Enchantments.UNBREAKING, 8, Enchantments.MENDING, 1);
	}

	public static ItemStack skyBlade(ServerLevel l) {
		return gear(l, Items.NETHERITE_SWORD, name("Skyforged Blade", ChatFormatting.GOLD),
				Enchantments.SHARPNESS, 12, Enchantments.LOOTING, 6, Enchantments.FIRE_ASPECT, 3,
				Enchantments.SWEEPING_EDGE, 6, Enchantments.UNBREAKING, 8, Enchantments.MENDING, 1);
	}

	public static ItemStack dragonbaneBow(ServerLevel l) {
		return gear(l, Items.BOW, name("Dragonbane Bow", ChatFormatting.DARK_PURPLE),
				Enchantments.POWER, 12, Enchantments.PUNCH, 3, Enchantments.FLAME, 1,
				Enchantments.INFINITY, 1, Enchantments.UNBREAKING, 8);
	}

	public static ItemStack voidreaver(ServerLevel l) {
		return gear(l, Items.MACE, name("Voidreaver", ChatFormatting.RED),
				Enchantments.DENSITY, 8, Enchantments.BREACH, 5, Enchantments.WIND_BURST, 3,
				Enchantments.UNBREAKING, 8, Enchantments.MENDING, 1);
	}

	public static ItemStack skyArmor(ServerLevel l) {
		int n = ThreadLocalRandom.current().nextInt(4);
		Item[] pieces = { Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS };
		String[] names = { "Skyforged Helm", "Skyforged Plate", "Skyforged Greaves", "Skyforged Boots" };
		if (n == 3) {
			return gear(l, pieces[n], name(names[n], ChatFormatting.AQUA), Enchantments.PROTECTION, 8,
					Enchantments.FEATHER_FALLING, 6, Enchantments.UNBREAKING, 8, Enchantments.MENDING, 1);
		}
		return gear(l, pieces[n], name(names[n], ChatFormatting.AQUA), Enchantments.PROTECTION, 8,
				Enchantments.UNBREAKING, 8, Enchantments.MENDING, 1);
	}

	// ---------- using the items ----------
	private static boolean cooling(ServerLevel l, ServerPlayer p, String id, long ticks) {
		String k = p.getUUID() + id;
		long now = l.getGameTime();
		long ready = COOLDOWNS.getOrDefault(k, 0L);
		if (now < ready) {
			p.displayClientMessage(Component.literal("Recharging... " + ((ready - now + 19) / 20) + "s")
					.withStyle(ChatFormatting.RED), true);
			return true;
		}
		COOLDOWNS.put(k, now + ticks);
		return false;
	}

	public static InteractionResult use(ServerLevel level, ServerPlayer sp, ItemStack stack, String key) {
		switch (key) {
			case K_PLATFORM -> {
				if (cooling(level, sp, key, 200)) return InteractionResult.SUCCESS;
				BlockPos base = sp.blockPosition().below();
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						BlockPos p = base.offset(dx, 0, dz);
						if (level.getBlockState(p).isAir()) {
							level.setBlock(p, Blocks.COBBLESTONE.defaultBlockState(), 3);
						}
					}
				}
				level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
			}
			case K_WAND -> {
				if (cooling(level, sp, key, 600)) return InteractionResult.SUCCESS;
				Lucky.roll(level, sp, sp.getX(), sp.getY() + 0.5, sp.getZ(), 0);
			}
			case K_HEART -> {
				if (Store.get(sp, "won") != 1) {
					sp.displayClientMessage(Component.literal("Only the Dragon Slayer can use this.")
							.withStyle(ChatFormatting.RED), true);
					return InteractionResult.SUCCESS;
				}
				if (sp.getXRot() < -60.0F) {
					if (!cooling(level, sp, key + "_dash", 60)) dash(level, sp);
				} else if (sp.isShiftKeyDown()) {
					if (!cooling(level, sp, key + "_roar", 160)) roar(level, sp);
				} else {
					if (!cooling(level, sp, key + "_breath", 30)) breath(level, sp);
				}
			}
			default -> { }
		}
		return InteractionResult.SUCCESS;
	}

	private static void breath(ServerLevel level, ServerPlayer sp) {
		Vec3 look = sp.getLookAngle();
		Vec3 eye = sp.getEyePosition();
		for (int i = 1; i <= 12; i++) {
			Vec3 p = eye.add(look.scale(i));
			level.sendParticles(ParticleTypes.PORTAL, p.x, p.y, p.z, 8, 0.3, 0.3, 0.3, 0.2);
			level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 4, 0.3, 0.3, 0.3, 0.02);
		}
		AABB box = sp.getBoundingBox().expandTowards(look.scale(12)).inflate(2.5);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, t -> t != sp)) {
			Vec3 to = e.position().add(0, e.getBbHeight() / 2.0, 0).subtract(eye);
			if (to.length() < 13.0 && to.normalize().dot(look) > 0.8) {
				e.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 1));
				e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
			}
		}
		level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.6F, 1.4F);
	}

	private static void roar(ServerLevel level, ServerPlayer sp) {
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, sp.getX(), sp.getY() + 1, sp.getZ(), 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1, sp.getZ(), 80, 3, 1, 3, 0.5);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(12), t -> t != sp)) {
			Vec3 away = e.position().subtract(sp.position());
			away = new Vec3(away.x, 0, away.z).normalize();
			e.push(away.x * 1.6, 0.6, away.z * 1.6);
			e.hurtMarked = true;
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
			e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
		}
		level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0F, 0.8F);
	}

	private static void dash(ServerLevel level, ServerPlayer sp) {
		Vec3 look = sp.getLookAngle();
		sp.setDeltaMovement(look.x * 2.2, Math.max(look.y * 2.2, 0.8), look.z * 2.2);
		sp.hurtMarked = true;
		sp.resetFallDistance();
		level.sendParticles(ParticleTypes.CLOUD, sp.getX(), sp.getY(), sp.getZ(), 30, 0.5, 0.2, 0.5, 0.1);
		level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 2.0F, 1.0F);
	}

	private LuckyItems() {}
}
