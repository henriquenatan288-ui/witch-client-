package com.witch.gui;

import com.witch.module.Category;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Icones de categoria: PNGs brancos suavizados (tools/gen_category_icons.py), pintados pela cor recebida. */
public final class CategoryIcons {
	private CategoryIcons() {}

	private static final int TEX = 48;
	private static final int SIZE = 9;
	private static final Map<Category, Identifier> IDS = new EnumMap<>(Category.class);

	static {
		for (Category c : Category.values()) {
			IDS.put(c, Identifier.fromNamespaceAndPath("witch", "textures/gui/cat_" + c.name().toLowerCase(Locale.ROOT) + ".png"));
		}
	}

	public static final Identifier MUSIC = Identifier.fromNamespaceAndPath("witch", "textures/gui/cat_music.png");
	public static final Identifier SEARCH = Identifier.fromNamespaceAndPath("witch", "textures/gui/ico_search.png");
	public static final Identifier SLIDERS = Identifier.fromNamespaceAndPath("witch", "textures/gui/ico_sliders.png");
	public static final Identifier CHEV_DOWN = Identifier.fromNamespaceAndPath("witch", "textures/gui/chev_down.png");
	public static final Identifier CHEV_UP = Identifier.fromNamespaceAndPath("witch", "textures/gui/chev_up.png");

	/** Icone avulso (PNG 48x48 branco) desenhado em size x size, pintado pela cor. */
	public static void blit(GuiGraphicsExtractor g, Identifier id, int x, int y, int size, int color) {
		g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, size, size, TEX, TEX, TEX, TEX, color);
	}

	/** (x, y) e o canto da area antiga 7x7; o icone novo (9x9) fica centrado nela. */
	public static void draw(GuiGraphicsExtractor g, Category c, int x, int y, int color) {
		g.blit(RenderPipelines.GUI_TEXTURED, IDS.get(c), x - 1, y - 1, 0f, 0f, SIZE, SIZE, TEX, TEX, TEX, TEX, color);
	}
}
