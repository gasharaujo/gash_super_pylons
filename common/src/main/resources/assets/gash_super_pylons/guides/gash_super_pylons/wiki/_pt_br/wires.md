---
navigation:
  title: Cabos
  icon: gash_super_pylons:item_wire
  position: 3
item_ids:
  - gash_super_pylons:item_wire
  - gash_super_pylons:fluid_wire
  - gash_super_pylons:energy_wire
  - gash_super_pylons:xp_wire
---

# Cabos

Clique primeiro no pylon de **origem**, depois no de **destino**. Eles precisam estar a até 32 blocos na mesma dimensão. A conexão gasta um cabo e transfere os recursos do primeiro pylon para o segundo.

| Cor | Recurso |
| --- | --- |
| Amarelo | Itens |
| Azul | Fluidos |
| Vermelho | Energia |
| Verde | XP |

Cada par de pylons aceita uma conexão. Clique no mesmo par em qualquer ordem para desconectar e recuperar o cabo. Agache e clique com o botão direito num pylon para cancelar uma seleção. Quebrar qualquer ponta também devolve o cabo uma única vez.

Os cabos compartilham a taxa de [automação](automation.md) de 10% da capacidade da origem por tick. Capacidade VI em qualquer ponta ativa bulk.
