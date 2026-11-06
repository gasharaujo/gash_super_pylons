---
navigation:
  title: Modificadores
  icon: gash_super_pylons:speed_upgrade_1
  position: 1
  parent: upgrades.md
item_ids:
  - gash_super_pylons:speed_upgrade_1
  - gash_super_pylons:speed_upgrade_2
  - gash_super_pylons:speed_upgrade_3
  - gash_super_pylons:speed_upgrade_4
  - gash_super_pylons:speed_upgrade_5
  - gash_super_pylons:speed_upgrade_6
  - gash_super_pylons:quantity_upgrade_1
  - gash_super_pylons:quantity_upgrade_2
  - gash_super_pylons:quantity_upgrade_3
  - gash_super_pylons:quantity_upgrade_4
  - gash_super_pylons:quantity_upgrade_5
  - gash_super_pylons:quantity_upgrade_6
  - gash_super_pylons:range_upgrade_1
  - gash_super_pylons:range_upgrade_2
  - gash_super_pylons:range_upgrade_3
  - gash_super_pylons:range_upgrade_4
  - gash_super_pylons:range_upgrade_5
  - gash_super_pylons:range_upgrade_6
  - gash_super_pylons:storage_upgrade_1
  - gash_super_pylons:storage_upgrade_2
  - gash_super_pylons:storage_upgrade_3
  - gash_super_pylons:storage_upgrade_4
  - gash_super_pylons:storage_upgrade_5
  - gash_super_pylons:storage_upgrade_6
  - gash_super_pylons:energy_upgrade_1
  - gash_super_pylons:energy_upgrade_2
  - gash_super_pylons:energy_upgrade_3
  - gash_super_pylons:energy_upgrade_4
  - gash_super_pylons:energy_upgrade_5
  - gash_super_pylons:energy_upgrade_6
---

# Modificadores

- **Velocidade (Speed)** define o intervalo entre operações. O Fertilizer também recebe mais tentativas de crescimento, e o XP Tank transfere mais níveis por tick.
- **Quantidade (Quantity)** define quantas unidades entram em cada operação. Algumas máquinas usam o próprio tier, como o mínimo de encantamentos no Enchanter e o número de alvos no Super Harvester.
- **Alcance (Range)** amplia a área horizontal e vertical. No Dropper, cada tier soma um bloco à distância. No Chunk Loader, define o raio em chunks.
- **Capacidade (Capacity)** aumenta o estoque de itens, fluidos e XP. O tier VI ativa [transferências bulk](automation.md). No [Encantador](enchanter.md), Capacidade só aumenta o poder base com Super instalado no mesmo pylon.
- **Energia (Energy)** adiciona armazenamento de Forge Energy a qualquer pylon. Puxa do lado oposto ao de trabalho, carrega um item compatível no pedestal e envia FE para o lado de trabalho.

| Tier | Velocidade (ticks) | Quantidade | Alcance | Capacidade |
| --- | --- | --- | --- | --- |
| 0 | 60 | 1 | 3×3×3 | 1× |
| I | 40 | 2 | 7×7×7 | 2× |
| II | 30 | 4 | 9×9×9 | 4× |
| III | 20 | 8 | 11×11×11 | 8× |
| IV | 10 | 16 | 13×13×13 | 16× |
| V | 5 | 32 | 15×15×15 | 32× |
| VI | 1 | 64 | 17×17×17 | Ilimitada |

| Tier de Energia | Estoque | FE por tick e por sentido |
| --- | --- | --- |
| I | 1.000.000 FE | 1.000 |
| II | 10.000.000 FE | 10.000 |
| III | 40.000.000 FE | 40.000 |
| IV | 100.000.000 FE | 100.000 |
| V | 400.000.000 FE | 400.000 |
| VI | 2.000.000.000 FE | Ilimitada |
