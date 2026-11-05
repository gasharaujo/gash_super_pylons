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

A taxa normal é **10% da capacidade do pylon para aquele recurso por tick**, compartilhada com os [cabos](wires.md) desse recurso. **Capacidade VI** ativa bulk nos dois sentidos. O pylon transfere o estoque disponível até o destino parar de aceitar. Capacidade VI em qualquer ponta ativa bulk numa conexão direta entre pylons. Os limites próprios do destino e o filtro de itens ainda se aplicam.
