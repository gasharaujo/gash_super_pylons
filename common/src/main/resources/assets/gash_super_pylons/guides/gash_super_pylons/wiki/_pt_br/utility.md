---
navigation:
  title: Utilidades
  icon: gash_super_pylons:harvester_upgrade
  position: 2
  parent: upgrades.md
item_ids:
  - gash_super_pylons:fluid_storage_upgrade
  - gash_super_pylons:xp_tank_upgrade
  - gash_super_pylons:anvil_upgrade
  - gash_super_pylons:enchanting_upgrade
  - gash_super_pylons:clicker_upgrade
  - gash_super_pylons:fertilizer_upgrade
  - gash_super_pylons:harvester_upgrade
  - gash_super_pylons:break_upgrade
  - gash_super_pylons:filter_upgrade
  - gash_super_pylons:magnet_upgrade
  - gash_super_pylons:planter_upgrade
  - gash_super_pylons:placer_upgrade
  - gash_super_pylons:dropper_upgrade
  - gash_super_pylons:void_upgrade
  - gash_super_pylons:chunk_loader_upgrade
  - gash_super_pylons:super_upgrade
  - gash_super_pylons:smelt_upgrade
  - gash_super_pylons:pulverize_upgrade
---

# Utilidades

## Agricultura, coleta e construção

- **Fertilizer** aplica ticks aleatórios nas plantas. Velocidade controla o intervalo e as tentativas por planta. Quantidade define o número de plantas.
- **Harvester** colhe cultivos maduros e mantém a plantação. Também derruba árvores inteiras com copa natural. Os drops que não couberem caem no chão. A receita usa uma Base de Upgrade e um machado de diamante.
- **Planter** planta as sementes ou mudas guardadas em espaços válidos dentro do alcance.
- **Magnet** recolhe itens caídos. Começa com um item a cada 60 ticks numa área de 3×3×3 e respeita o filtro.
- **Placer** coloca um bloco do estoque no lado de trabalho.
- **Break** minera o bloco no lado de trabalho com uma picareta virtual de diamante. Agache e clique com uma ferramenta de mineração para equipá-la. Os drops iguais ao primeiro vão para o estoque, e os demais ficam no chão.
- **Clicker** clica com o botão direito no lado de trabalho com um item do estoque ou de mão vazia.
- **Dropper** solta itens no centro do bloco do lado de trabalho. Quantidade e Velocidade controlam o ciclo. Cada tier de Alcance soma um bloco à distância.

## Armazenamento e controle

- **Fluid Storage** guarda um tipo de fluido e começa com 32 baldes. Aceita baldes e automação de fluidos. Esvazie antes de retirar o upgrade. Quebrar o pylon descarta o fluido.
- **XP Tank** coleta orbes a até dois blocos, puxa XP de jogadores acima e entrega XP a jogadores abaixo. Começa com 32.000 pontos e usa Velocidade para a taxa entre jogadores. Um frasco de experiência rende 7 pontos e devolve um frasco de vidro. Um frasco vazio faz o inverso. Integra-se ao XP líquido de Just Dire Things, Ender IO e Mob Grinding Utils a 20 mB por ponto.
- **Filter** abre com botão direito e mãos vazias. Os 27 slots aceitam amostras do cursor ou do JEI. Use o botão do meio para escolher uma tag. Alterne entre a lista de permissão e a de bloqueio.
- **Void** descarta itens, fluidos, energia e XP que entrarem.
- **Chunk Loader** mantém um quadrado de chunks ativo ao redor do pylon. Começa com raio de um chunk. Alcance pode ampliá-lo.
- **Creative Storage** é instalado com um Command Block vanilla. Insira uma amostra no armazenamento correspondente para as retiradas gerarem cópias. Um estoque vazio não fornece recursos.
- **Super** adiciona quatro Nether Stars orbitando o núcleo e muda o comportamento dos upgrades listados abaixo.

## Processamento

**Smelt** usa receitas de fornalha. Instale um alto-forno ou defumador no pylon para especializar as receitas. **Pulverize** transforma pedregulho em cascalho, cascalho em areia com chance de sílex, terracota em areia vermelha e pedras compatíveis em pedregulho. Também processa blocos de minério e gera pós equivalentes quando existem no modpack. Quantidade altera alguns rendimentos e chances. Datapacks podem adicionar receitas.

Ambos enviam os resultados ao inventário no lado de trabalho e custam **1.000 FE por item processado**. Usam primeiro o FE do próprio pylon, depois o de um pylon vizinho e por fim combustível de fornalha de um inventário lateral. Um carvão rende oito usos. Geradores podem abastecer ambos quando houver receita para o produto.

## Enchanter e Anvil

Com a [Wrench](tools.md) no modo Vincular, agache e clique no pylon principal, depois clique num XP Tank e num auxiliar de itens. Os auxiliares podem ficar a 1–5 blocos em qualquer direção. O principal processa um item por vez. O resultado sai para o inventário do lado de trabalho ou fica no núcleo até haver espaço.

**Enchanter** recebe o item no principal e lápis-lazúli no auxiliar. Gasta um lápis e 27 pontos de XP por item. Capacidade define poder 1, 10, 20, 30, 40, 50 ou 100. Quantidade define o mínimo de 1 a 7 encantamentos compatíveis, conforme disponibilidade. Clique com o botão direito para instalar até 64 estantes compatíveis de Apotheosis ou Apothic Enchanting.

**Anvil** recebe o primeiro item no principal e o segundo item ou material de reparo no auxiliar. Encantamentos do mesmo nível sobem um nível. Cada melhoria custa o nível resultante multiplicado pela raridade. Acima do máximo normal, o preço é multiplicado pela raridade outra vez, com multiplicador mínimo de dois.

## Combinações com Super

- **Smelt e Pulverize** processam todos os stacks elegíveis no inventário oposto à saída enquanto houver espaço e energia ou combustível.
- **Fertilizer** avança os estágios das plantas diretamente. **Harvester** ataca até 1 + tier de Quantidade mobs por operação. Agache e clique com uma arma para equipá-la. A arma padrão é um machado de diamante.
- **Break e Fluid Storage** varrem o chunk a partir de três blocos abaixo do pylon. Cada tier de Alcance soma um chunk ao raio. Break envia drops ao inventário da saída, e Fluid Storage coleta fontes e pode enviá-las ao tanque adjacente.
- **Magnet** deposita diretamente no inventário de saída. **Placer** preenche espaços de um plano três blocos abaixo. **Dropper** funciona como ejetor e mira os projéteis na direção de trabalho.
- **Enchanter** pode usar os encantamentos do item de um terceiro auxiliar como blacklist. **Anvil** preserva o livro encantado usado como segundo item.
