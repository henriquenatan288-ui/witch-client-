# Retomar (salvo em 2026-11-12) - Witch Client Java

Contexto: limite do usuario quase acabando. Usuario: Brasil, celular fraco (0-1 FPS), responder em PT, curto, direto, critico.
Regras: sem simulacao/CI visual sem pedir; 1 build por vez; poll minimo; nao copiar texturas/arquivos de outros; sem bypass de anticheat/panic.
Fluxo git: commit em work46 -> push HEAD:work46 -> esperar CI verde -> merge-base -> push work46:main (nunca force).

## Pendente (ordem sugerida)
1. HALO: anel azul pulsante acima da cabeca do jogador (perguntar: so em si ou nos outros tambem) + brilho suave no chao.
   Referencia (video Vamp Visuals): anel fino achatado com brilho leve, rastro de quadrados translucidos no chao.
2. PALETA UNICA AZUL em todos os efeitos visuais (Vamp usa 1 cor so; disciplina > quantidade).
3. REDESIGN DA GUI conforme os 2 prints desta pasta:
   - ref-gui-layout.png: janela escura arredondada; sidebar esquerda (logo "CLIENT", CATEGORIES: Combat/Movement/Player/Render/World/Misc, Settings embaixo, item ativo com barra branca a esquerda e fundo levemente claro); header com titulo da categoria + busca "Search modules"; area de modulos com cantos em colchete; rodape "RSHIFT toggle GUI". Fonte mono pequena e espacada para rotulos.
   - ref-gui-componentes.png: cartao de modulo OFF / OFF+keybind (chip "R") / ON (borda clara + brilho + switch branco) / EXPANDED (toggle, slider com valor, dropdown "Single", keybind). Controles: switch, checkbox, slider, dropdown, keybind ("..." ao gravar).
   Tema: preto/cinza muito escuro, branco como destaque. Usuario pediu azul antes: confirmar se troca branco por azul.
   Nota: nao copiar fontes/icones de terceiros; desenhar icones simples via g.fill.
4. Nao testado em jogo: otimizacao SectionScan (Storage/Farm ESP).

## Limites conhecidos
- Contorno 3D do item na mao nao funcionou (precisa shader). Nao prometer silhueta/chams.
- API 26.x: mc.gui.screen()/setScreen, GuiGraphicsExtractor.extractRenderState, g.fill int, g.pose() Matrix3x2fStack, MouseButtonEvent.
- Lambdas: variaveis efetivamente final.


---
# ATUALIZACAO 2026-11-13 06:10 (retomar as 12:59 ou 13:20 - usuario so tem limite depois disso)

Estado: main = work46 = 81409a5 (ClickGUI com janela PNG + cartoes PNG, tela WitchEnterScreen, titulo com logo centralizado).
Primeiro passo na proxima sessao: ler este arquivo, `git fetch origin main`, trabalhar em work46 (merge de main antes de empurrar).
NAO rodar simulacao. 1 build por rodada. Responder em PT, curto, direto.

## Feedback do usuario (ja testou no celular) - fazer nesta ordem
1. BRILHO COM FAIXAS: `WitchLoading.glow(...)` desenha aneis concentricos visiveis ("camadas se juntando"), tanto na tela de entrada
   quanto no titulo (logo no topo). Trocar por brilho suave: textura radial pre-suavizada (gerar com Pillow, PNG branco com alpha gaussiano)
   desenhada com blit tintado; nunca empilhar muitos fills. Vale tambem para as outras telas que chamam glow.
2. SKIN/BONECO DO TITULO ("boneco vindo", WitchTitleScreen.walker/wave/model3d/TitleSkin): ele anda de costas (moonwalk), nao chega na posicao,
   e depois fica ~2s travado olhando para baixo. Corrigir: movimento de entrada com a perna/passo sincronizado com a direcao, parar ao chegar
   e voltar a pose normal (cabeca reta) sem travar. Conferir `wave(t, arrived)` e a rotacao da cabeca (pitch) ao chegar.
3. CLICKGUI - CARTOES: os recortes PNG dos cartoes (ui_card_off/on, 235x42 reduzidos) ficaram ruins (borrados, borda grossa).
   VOLTAR a desenhar os cartoes por codigo (roundRect), mais finos e nitidos como na referencia (borda 1px, switch pequeno, chevron fino);
   manter a janela PNG (ui_menu.png). Texto esta grande demais para a tela do celular: usar fonte menor/escala menor nos cartoes e na lista.
   Barra de busca: texto cortado ("arch modul.."): usar placeholder curto ou ajustar largura. (Os overlays UI/FREE/XRAY/VOICE/FPS/E nas capturas
   sao dos controles do launcher do celular, nao do client.)
4. LOGO WT: `textures/gui/logo.png` parece mal recortada/serrilhada (cauda do T cortada). Regenerar a textura com Pillow a partir da fonte/maior
   resolucao com antialias e margem transparente; testar com blit em escalas pequenas. Se a imagem original nao existir no repo, pedir ao usuario.
5. FUNDO DO LARP: recriar POR CODIGO (nao copiar arquivo): fundo preto-grafite com faixas verticais onduladas cinza escuras que se movem devagar,
   pontinhos/circulos pequenos espalhados, fade in quando o logo chega. Usar no WitchTitleScreen no lugar/em cima de `UiTex.titleBg`
   (e na WitchEnterScreen so depois do arrasto). Ondas: varias colunas com offset senoidal, alpha baixo, desenhadas com poucos blits/fills.
6. Arrasto do logo na WitchEnterScreen: usuario gostou ("ficou bom"). Nao mexer.

## Referencias
- Video do Larp Client (sequencia): preto -> logo+nome+"Press to enter" -> arrasto com esticar -> fundo de ondas -> menu centralizado -> skin gira a direita.
- Cartoes e janela da GUI: docs/retomar/ref-gui-*.png (cartoes devem ser desenhados por codigo, janela usa ui_menu.png).
