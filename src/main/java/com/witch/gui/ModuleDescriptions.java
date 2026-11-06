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
			case "Hide Name" -> "Esconde a scoreboard da direita inteira (limpo pra print/video).";
			case "Jesus" -> "Andar sobre a agua.";
			case "Mood Filter" -> "Escurece a tela, tom frio e vinheta.";
			case "No Ghost Blocks" -> "Evita blocos fantasma ao quebrar/colocar.";
			case "No Hurt Cam" -> "Tira o tranco da camera ao tomar dano.";
			case "Particles" -> "Particulas extras.";
			case "Player ESP" -> "Destaca jogadores atraves das paredes.";
			case "Slow Hit Visual" -> "Visual de golpe lento.";
			case "Speed" -> "Aumenta a velocidade de movimento (0 = normal).";
			case "Storage ESP" -> "Destaca baus e containers proximos.";
			case "Sus Solo Finder" -> "Escaneia chunks de cima (bom voando), apita e marca o solo. Cor = chance de base (heuristica).";
			case "Waypoints" -> "Salva pontos (tecla Add waypoint), coluna visivel de longe e distancia na tela.";
			case "Coords HUD" -> "Coordenadas, direcao e velocidade na tela.";
			case "Armor HUD" -> "Durabilidade da armadura e do item na mao.";
			case "Chunk Borders" -> "Mostra as bordas das chunks ao redor.";
			case "Light Level" -> "Marca o chao com luz de bloco 0 (mobs nascem).";
			case "Weather Off" -> "Tira chuva e trovoada da sua tela.";
			case "Auto Sprint" -> "Corre sozinho ao andar pra frente.";
			case "Auto Reconnect" -> "Reconecta no servidor quando cai.";
			case "Chat Timestamps" -> "Poe a hora em cada mensagem do chat.";
			case "No Fall" -> "Sem dano de queda. Anticheat detecta.";
			case "Step" -> "Sobe blocos sem pular. Anticheat detecta.";
			case "Spider" -> "Sobe paredes andando contra elas. Anticheat detecta.";
			case "Crosshair" -> "Mira nova: 4 estilos, cor, tamanho e espaco.";
			case "Sky" -> "Cor do ceu e mais estrelas.";
			case "Fog" -> "Neblina: visao mais curta e nevoa colorida.";
			case "Glow Players" -> "Contorno brilhante nos outros jogadores.";
			case "Jump Circles" -> "Anel de luz no chao ao pular.";
			case "Kill Effect" -> "Som e efeito ao matar. Effect: 0 nenhum 1 cartas 2 faiscas 3 coracoes 4 totem. Sound type: 1 xp 2 level up 3 ding.";
			case "Card Particles" -> "Particulas ao acertar, pular ou flutuando. Shape: 1 cartas 2 dinheiro 3 chapeu de bruxa 4 pato de cartola.";
			case "Filter" -> "Filtro de cor: 1 quente 2 frio 3 neon 4 escuro 5 por do sol 6 menta.";
			case "Witch Theme" -> "Liga de uma vez o filtro de humor e a poeira ambiente.";
			case "Sus Chunk Finder" -> "Procura chunks suspeitas num raio proprio (1 a 40).";
			case "Sus Hole Finder" -> "Procura buracos suspeitos.";
			case "Tracers" -> "Linhas da mira ate Player/Farm/Storage ESP (estilo Meteor, bem slim).";
			case "Trigger Bot" -> "Ataca sozinho quando a mira esta num alvo.";
			case "X-Ray Hologram" -> "Mostra minerios atraves dos blocos.";
			case "Zoom" -> "Aproxima a visao com transicao suave.";
			case "GUI Settings" -> "Opacidade do ClickGUI e tooltips.";
			default -> com.witch.module.InfoModules.desc(n);
		};
	}
}
