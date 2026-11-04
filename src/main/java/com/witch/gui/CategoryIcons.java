package com.witch.gui;

import com.witch.module.Category;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Icones 7x7 por categoria (bitmaps simples). */
public final class CategoryIcons {
	private CategoryIcons() {}

	private static final String[] MOVEMENT = {"#..#...", ".#..#..", "..#..#.", "...#..#", "..#..#.", ".#..#..", "#..#..."};
	private static final String[] VISUALS = {".......", "..###..", ".#...#.", "#..#..#", ".#...#.", "..###..", "......."};
	private static final String[] UTILITY = {"..###..", ".#####.", "###.###", "##...##", "###.###", ".#####.", "..###.."};
	private static final String[] EFFECTS = {"...#...", "...#...", "..###..", "#######", "..###..", "...#...", "...#..."};

	public static void draw(GuiGraphicsExtractor g, Category c, int x, int y, int color) {
		String[] p = switch (c) {
			case MOVEMENT -> MOVEMENT;
			case VISUALS -> VISUALS;
			case UTILITY -> UTILITY;
			case EFFECTS -> EFFECTS;
		};
		for (int r = 0; r < p.length; r++) {
			for (int q = 0; q < p[r].length(); q++) {
				if (p[r].charAt(q) == '#') g.fill(x + q, y + r, x + q + 1, y + r + 1, color);
			}
		}
	}
}
