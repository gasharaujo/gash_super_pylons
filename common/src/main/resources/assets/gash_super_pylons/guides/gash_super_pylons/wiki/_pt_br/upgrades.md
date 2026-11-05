---
navigation:
  title: Upgrades
  icon: planter_upgrade
  parent: index.md
  position: 2
item_ids:
  - gash_super_pylons:planter_upgrade
  - gash_super_pylons:harvester_upgrade
  - gash_super_pylons:placer_upgrade
  - gash_super_pylons:dropper_upgrade
  - gash_super_pylons:super_upgrade
  - gash_super_pylons:void_upgrade
  - gash_super_pylons:chunk_loader_upgrade
  - gash_super_pylons:xp_tank_upgrade
  - gash_super_pylons:energy_upgrade_1
---

# Upgrades

Faça o Upgrade Base com cinco pedregulhos em cruz. Use a base no centro para o tier I e o upgrade anterior nos tiers seguintes. Com o JEI instalado, consulte as receitas nele.

**Speed** controla o intervalo. **Quantity** controla a quantidade por operação. **Range** expande a área na horizontal e na vertical. **Capacidade** aumenta o armazenamento. **Energia** adiciona um estoque de FE independente da especialidade e aceita energia por todas as faces.

- **Planter** planta as sementes ou mudas armazenadas. **Fertilizer** acelera o crescimento.
- **Harvester** replanta cultivos e derruba árvores. O que couber dos drops das árvores fica no pylon. O excedente cai no mundo.
- **Placer** coloca um bloco armazenado no lado de trabalho. **Break** minera esse lado. **Clicker** interage com ele, inclusive de mão vazia.
- **Dropper** solta itens armazenados no centro do bloco adjacente no lado de trabalho. Speed e Quantity controlam o ciclo e cada tier de Range estende a ejeção em um bloco.
- **Super** adiciona quatro Nether Stars que orbitam o núcleo e sai primeiro ao remover upgrades.
- **Void** destrói itens, fluidos, FE e XP recebidos. **Chunk Loader** mantém chunks ativos em um quadrado com raio inicial de um chunk.
- **XP Tank** converte um frasco de experiência em sete pontos de XP e um frasco de vidro. Use um frasco de vidro no tanque para fazer a conversão inversa.
- **Fluid Storage** começa com 32 baldes de capacidade. Os geradores de água e lava também funcionam como tanques.

Geradores de itens podem abastecer Smelt ou Pulverize. O processamento gasta 1.000 FE ou combustível equivalente a um uso de fornalha.

## Enchanter e Anvil

Com a Wrench no modo Vincular, use Shift + botão direito no principal e botão direito num XP Tank e num auxiliar de itens. A ordem desses dois auxiliares é livre. Eles podem ficar entre 1 e 5 blocos do principal, medidos entre centros, inclusive na vertical e na diagonal. As duas máquinas trabalham com um item por vez; o resultado substitui a entrada no núcleo e pode ser retirado por automação. Speed controla o intervalo. Uma operação incompleta não consome recursos.

### Enchanter

Coloque o item no principal e lápis-lazúli no auxiliar de itens. Cada operação gasta um lápis e **27 pontos de XP**, equivalentes a três níveis contados a partir de zero, em qualquer poder. Capacidade 0/I/II/III/IV/V/VI define poder base **1/10/20/30/40/50/100**. Quantity no principal define mínimo de **1/2/3/4/5/6/7 encantamentos**, respeitando compatibilidade e disponibilidade. Itens já encantados não são reencantados.

Com Super, vincule um terceiro pylon opcional contendo um item ou livro encantado. Os encantamentos dele formam uma blacklist. Ele não é consumido e continua aparecendo no núcleo normal. Só os dois primeiros auxiliares mostram livros de mesa. Remover Super libera o terceiro vínculo.

Use botão direito com estantes do Apotheosis/Apothic Enchanting para instalar até 64 estantes. A integração usa os valores e a seleção de encantamentos do próprio mod, incluindo Eterna, Quanta, Arcana, tesouros, estabilidade/retificação e blacklist de estantes, conforme a versão. Pistas da interface não afetam o processo automático. As estantes são salvas, aparecem na barra e saem antes do Enchanter quando ele for o próximo na ordem de remoção.

### Anvil

Coloque o primeiro item no principal e o segundo item ou material de reparo no auxiliar de itens. Encantamentos do mesmo nível sobem um nível, inclusive acima do máximo normal; níveis diferentes preservam o maior. Os encantamentos devem ser compatíveis. O limite técnico é 255.

O preço soma **nível resultante × raridade**: comum 1, incomum 2, raro 3 e muito raro 4. Para cada encantamento acima do máximo normal, multiplique seu preço novamente pela raridade, com multiplicador mínimo dois. Reparos custam ao menos um nível. O total é convertido em pontos de XP pela curva vanilla a partir do zero e pago pelo auxiliar de XP.

Super preserva um livro encantado usado como segundo item. Outros itens e materiais de reparo continuam sendo consumidos. Só há gasto quando a operação melhora encantamentos ou repara dano. Jade, WTHIT e The One Probe mostram o estado, o custo e as estantes instaladas.
