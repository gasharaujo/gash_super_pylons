---
navigation:
  title: Automação
  icon: gash_super_pylons:insert_item_upgrade
  position: 3
  parent: upgrades.md
item_ids:
  - gash_super_pylons:insert_item_upgrade
  - gash_super_pylons:insert_fluid_upgrade
  - gash_super_pylons:insert_energy_upgrade
  - gash_super_pylons:insert_xp_upgrade
  - gash_super_pylons:extract_item_upgrade
  - gash_super_pylons:extract_fluid_upgrade
  - gash_super_pylons:extract_energy_upgrade
  - gash_super_pylons:extract_xp_upgrade
---

# Automação

**Insert** puxa do bloco no lado de trabalho. **Extract** envia para esse bloco. Encoste um inventário, tanque, bateria ou porta de XP compatível nessa face. O padrão é embaixo. A [Wrench](tools.md) seleciona qualquer um dos seis lados.

| Variante | Recurso transferido |
| --- | --- |
| Item | Stacks de itens |
| Fluido | Líquido em mB |
| Energia | Forge Energy (FE) |
| XP | Pontos de XP ou XP líquido compatível |

Instale um sentido por recurso. Insert e Extract do mesmo recurso podem ser convertidos um no outro na bancada. O estoque também fica disponível para automação externa compatível.

Insert, Extract e [Cabos](wires.md) compartilham a taxa do pylon que faz a transferência.

| Capacidade | Itens | Fluidos |
| --- | --- | --- |
| 0 | 5/s | 250 mB/t |
| I | 10/s | 1 B/t |
| II | 1/t | 2 B/t |
| III | 16/t | 4 B/t |
| IV | 64/t | 16 B/t |
| V | 256/t | 64 B/t |
| VI | Bulk | Bulk |

B = balde (1.000 mB), t = tick e s = segundo de jogo (20 ticks). Energia e XP continuam com taxa de 10% da capacidade por tick, sujeita aos limites do recurso.

**Capacidade VI** ativa bulk nos dois sentidos. O pylon transfere o estoque disponível até o destino parar de aceitar. Capacidade VI em qualquer ponta ativa bulk numa conexão direta entre pylons. Os limites próprios do destino e o filtro de itens ainda se aplicam.
