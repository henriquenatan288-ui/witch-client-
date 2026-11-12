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
