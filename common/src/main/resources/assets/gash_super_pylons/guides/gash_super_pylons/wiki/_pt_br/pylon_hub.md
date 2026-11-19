---
navigation:
  title: Pylon HUB
  icon: gash_super_pylons:pylon_hub
  position: 0
  parent: pylon.md
item_ids:
  - gash_super_pylons:pylon_hub
---

# Pylon HUB

O HUB fornece acesso aos itens, fluidos, XP e energia dos pylons vinculados. Clique com a mão vazia para ver os totais e quantos vínculos estão ativos.

1. Coloque a Chave no modo **Vincular**. Agache e clique com o botão direito no HUB.
2. Solte o Shift e clique nos pylons de destino. Repita o clique para remover um vínculo.
3. Conecte o cabo ou leitor ao HUB. As portas de itens, fluidos, FE e XP operam diretamente no pylon de destino.

O alcance base é **16 blocos**. Cada nível de [Alcance](range.md) acrescenta 16. Com [Super](super.md), o vínculo pode atravessar qualquer distância e dimensão. O HUB e o destino devem estar em chunks carregados. O HUB aceita Alcance e Super e grava apenas a configuração dos vínculos. As integrações são opcionais.

No NeoForge, o Barramento de Armazenamento do AE2 acessa diretamente os itens e fluidos do HUB. O External Storage do Refined Storage e leitores de outros mods usam as portas compatíveis de cada versão. O SCC pode usar a porta de fluido para XP líquido.

<RecipeFor id="gash_super_pylons:pylon_hub" />
