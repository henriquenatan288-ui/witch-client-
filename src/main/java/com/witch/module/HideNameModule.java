package com.witch.module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Objective;

/**
 * Remove o teu nome da scoreboard da direita (sidebar).
 * Util pra prints e videos — a linha com teu nick some.
 */
public class HideNameModule extends Module {
	private static HideNameModule instance;

	public HideNameModule() {
		super("Hide Name", Category.VISUALS);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	@Override
	public void onTick() {
		if (!isEnabled()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;

		Scoreboard scoreboard = mc.level.getScoreboard();
		String name = mc.player.getScoreboardName();

		// Remove de todas as teams (evita aparecer com prefixo/rank)
		for (PlayerTeam team : scoreboard.getPlayerTeams()) {
			if (team.getPlayers().contains(name)) {
				scoreboard.removePlayerFromTeam(name);
			}
		}

		// Remove scores do jogador em todos os objectives da sidebar
		for (Objective objective : scoreboard.getObjectives()) {
			scoreboard.resetSinglePlayerScore(name, objective);
		}
	}
}
