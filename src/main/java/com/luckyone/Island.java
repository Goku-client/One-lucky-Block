package com.luckyone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** The floating starter island: grass, one tree, the Lucky Block, a Nether portal and an End portal. */
public final class Island {
	public static final int Y = 100;
	public static final int R = 10;
	public static final BlockPos LUCKY_POS = new BlockPos(0, Y, 0);
	// End portal sits on the west side, Nether portal on the east side.
	private static final int END_CX = -6;
	private static final int NETHER_X = 6;
	private static final BlockPos MARKER = new BlockPos(END_CX - 2, Y, 0); // an End portal frame

	public static void ensure(ServerLevel level) {
		if (!level.getBlockState(MARKER).is(Blocks.END_PORTAL_FRAME)) {
			build(level);
		}
		if (!level.getBlockState(LUCKY_POS).is(ModContent.LUCKY_BLOCK)) {
			level.setBlock(LUCKY_POS, ModContent.LUCKY_BLOCK.defaultBlockState(), 3);
		}
	}

	public static void teleportHome(ServerLevel overworld, ServerPlayer sp) {
		sp.teleport(new TeleportTransition(overworld, new Vec3(0.5, Y + 1.0, 4.5),
				Vec3.ZERO, 180.0F, 0.0F, TeleportTransition.DO_NOTHING));
		sp.resetFallDistance();
	}

	private static void build(ServerLevel level) {
		BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
		BlockState dirt = Blocks.DIRT.defaultBlockState();
		BlockState stone = Blocks.STONE.defaultBlockState();

		// Tapered island body
		for (int dx = -R; dx <= R; dx++) {
			for (int dz = -R; dz <= R; dz++) {
				int d2 = dx * dx + dz * dz;
				for (int k = 0; k <= 4; k++) {
					int rk = R - 2 * k;
					if (d2 <= rk * rk) {
						BlockState s = k == 0 ? grass : (k <= 2 ? dirt : stone);
						level.setBlock(new BlockPos(dx, Y - k, dz), s, 2);
					}
				}
			}
		}

		// A small oak tree
		BlockState log = Blocks.OAK_LOG.defaultBlockState();
		BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		int tx = 3, tz = -5;
		for (int y = 1; y <= 4; y++) level.setBlock(new BlockPos(tx, Y + y, tz), log, 2);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 3; dy <= 5; dy++) {
					boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
					if ((dy == 5 && (Math.abs(dx) > 1 || Math.abs(dz) > 1)) || (corner && dy != 4)) continue;
					BlockPos p = new BlockPos(tx + dx, Y + dy, tz + dz);
					if (level.getBlockState(p).isAir()) level.setBlock(p, leaves, 2);
				}
			}
		}

		buildEndPortal(level);
		buildNetherPortal(level);
		level.setBlock(LUCKY_POS, ModContent.LUCKY_BLOCK.defaultBlockState(), 3);
	}

	private static void frame(ServerLevel level, int x, int z, Direction facing) {
		BlockState s = Blocks.END_PORTAL_FRAME.defaultBlockState()
				.setValue(EndPortalFrameBlock.FACING, facing)
				.setValue(EndPortalFrameBlock.HAS_EYE, true);
		level.setBlock(new BlockPos(x, Y, z), s, 2);
	}

	private static void buildEndPortal(ServerLevel level) {
		int cx = END_CX, cz = 0;
		for (int i = -1; i <= 1; i++) {
			frame(level, cx + i, cz - 2, Direction.SOUTH);
			frame(level, cx + i, cz + 2, Direction.NORTH);
			frame(level, cx - 2, cz + i, Direction.EAST);
			frame(level, cx + 2, cz + i, Direction.WEST);
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				level.setBlock(new BlockPos(cx + dx, Y, cz + dz), Blocks.END_PORTAL.defaultBlockState(), 2);
			}
		}
	}

	private static void buildNetherPortal(ServerLevel level) {
		BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
		BlockState portal = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, Direction.Axis.Z);
		for (int z = -1; z <= 2; z++) {
			for (int row = 1; row <= 5; row++) {
				boolean edge = z == -1 || z == 2 || row == 1 || row == 5;
				if (edge) level.setBlock(new BlockPos(NETHER_X, Y + row, z), obsidian, 2);
			}
		}
		for (int z = 0; z <= 1; z++) {
			for (int row = 2; row <= 4; row++) {
				level.setBlock(new BlockPos(NETHER_X, Y + row, z), portal, 2);
			}
		}
	}

	private Island() {}
}
