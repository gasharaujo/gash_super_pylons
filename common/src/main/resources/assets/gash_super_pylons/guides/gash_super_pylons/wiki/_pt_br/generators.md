---
navigation:
  title: Geradores
  icon: gash_super_pylons:cobblestone_generator_upgrade
  position: 0
  parent: upgrades.md
---

# Geradores

Um gerador produz **uma unidade a cada 60 ticks** na configuração inicial. [Velocidade](modifiers.md) diminui o intervalo, e Quantidade aumenta a produção por ciclo. Cada pylon tem um slot de gerador, separado da utilidade e do processador.

| Gerador | Produção |
| --- | --- |
| Pedregulho | Pedregulho |
| Ardósia | Ardósia |
| Pedra-negra | Pedra-negra |
| Netherrack | Netherrack |
| Areia | Areia |
| Vidro | Vidro |
| Água | 100 mB de água por unidade |
| Lava | 100 mB de lava por unidade |

A geração de itens pausa quando o pylon está cheio ou guarda outro item. Ela pode alimentar Smelt ou Crush quando existir uma receita para o produto.

Água e Lava incluem um **tanque de 32 baldes**, ampliado por Capacidade. O tanque enche baldes e alimenta a automação de fluidos. A textura do líquido aparece no núcleo. Remover um gerador com líquido pede confirmação antes de descartá-lo. Quebrar o pylon descarta o líquido.
