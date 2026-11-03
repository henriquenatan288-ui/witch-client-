package com.witch.gui;

/** Tooltips / descrições dos módulos do ClickGUI. */
public final class ModuleDescriptions {
	private ModuleDescriptions() {}

	public static String of(String n) {
		return switch (n) {
			case "Ambient Dust" -> "Particulas brancas flutuando no ar em volta de voce.";
			case "Ancient Finder" -> "Procura blocos/estruturas antigas nas chunks carregadas.";
			case "Anti Vanish" -> "Mostra jogadores que ficaram invisiveis por vanish.";
			case "Auto Eat" -> "Come sozinho quando a fome baixa.";
			case "Auto Tool" -> "Troca para a melhor ferramenta ao quebrar um bloco.";
			case "Auto Totem" -> "Mantem um totem na mao secundaria.";
			case "Break Delay" -> "Ajusta o atraso entre quebrar blocos.";
			case "Break Wave" -> "Efeito visual ao quebrar blocos.";
			case "Farm ESP" -> "Destaca plantacoes proximas.";
			case "Fly" -> "Voo livre.";
			case "FreeLook" -> "Gira a camera em volta do personagem sem mudar para onde ele olha.";
			case "Freecam" -> "Camera livre fora do corpo, sem limite de distancia.";
			case "Fullbright" -> "Clareia o mundo (visao noturna no seu cliente).";
			case "Jesus" -> "Andar sobre a agua.";
			case "Mood Filter" -> "Escurece a tela, tom frio e vinheta.";
			case "No Ghost Blocks" -> "Evita blocos fantasma ao quebrar/colocar.";
			case "No Hurt Cam" -> "Tira o tranco da camera ao tomar dano.";
			case "Particles" -> "Particulas extras.";
			case "Player ESP" -> "Destaca jogadores atraves das paredes.";
			case "Slow Hit Visual" -> "Visual de golpe lento.";
			case "Speed" -> "Aumenta a velocidade de movimento (0 = normal).";
			case "Storage ESP" -> "Destaca baus e containers proximos.";
			case "Sus Chunk Finder" -> "Procura chunks suspeitas num raio proprio (1 a 30).";
			case "Sus Hole Finder" -> "Procura buracos suspeitos.";
			case "Tracers" -> "Linhas da mira ate Player/Farm/Storage ESP (estilo Meteor).";
			case "Trigger Bot" -> "Ataca sozinho quando a mira esta num alvo.";
			case "Witch Particles" -> "Chapeus de bruxa girando em volta de voce.";
			case "X-Ray Hologram" -> "Mostra minerios atraves dos blocos.";
			case "Zoom" -> "Aproxima a visao com transicao suave.";
			case "GUI Settings" -> "Opacidade do ClickGUI e tooltips.";
			default -> "";
		};
	}
}
