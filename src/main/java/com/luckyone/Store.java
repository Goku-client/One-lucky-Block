package com.luckyone;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

/** Tiny per-player number storage kept inside player tags (survives restarts and death). */
public final class Store {
	public static int get(Player p, String key) {
		String pre = "lk_" + key + "_";
		for (String t : p.getTags()) {
			if (t.startsWith(pre)) {
				try {
					return Integer.parseInt(t.substring(pre.length()));
				} catch (NumberFormatException e) {
					return 0;
				}
			}
		}
		return 0;
	}

	public static void set(Player p, String key, int value) {
		if (get(p, key) == value && (value != 0 || p.getTags().stream().anyMatch(t -> t.startsWith("lk_" + key + "_")))) {
			return;
		}
		String pre = "lk_" + key + "_";
		for (String t : new ArrayList<>(p.getTags())) {
			if (t.startsWith(pre)) p.removeTag(t);
		}
		p.addTag(pre + value);
	}

	public static int add(Player p, String key, int amount) {
		int v = get(p, key) + amount;
		set(p, key, v);
		return v;
	}

	private Store() {}
}
