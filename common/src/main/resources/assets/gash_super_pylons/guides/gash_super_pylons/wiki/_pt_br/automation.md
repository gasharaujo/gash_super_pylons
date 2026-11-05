---
navigation:
  title: Automação
  icon: item_wire
  parent: index.md
  position: 1
item_ids:
  - gash_super_pylons:item_wire
  - gash_super_pylons:fluid_wire
  - gash_super_pylons:energy_wire
  - gash_super_pylons:xp_wire
  - gash_super_pylons:filter_upgrade
  - gash_super_pylons:wrench
---

# Automação

Use um Wire primeiro na origem e depois no destino, a até 32 blocos. Amarelo transfere itens, azul transfere fluidos, vermelho transfere energia e verde transfere XP. Selecione o mesmo par em qualquer ordem para desconectar e recuperar o cabo instalado.

Insert puxa do bloco adjacente. Extract envia para ele. Os cabos e upgrades de transferência compartilham um limite de 10% da capacidade por recurso a cada tick. Capacidade VI remove esse limite. O envio usa o saldo e o espaço disponíveis, respeitando os limites dos equipamentos conectados.

Instale Filter e clique com as mãos vazias para abrir os 27 espaços. Use um item no cursor ou arraste do JEI. Clique com o botão do meio em um espaço preenchido para escolher uma tag ou voltar à correspondência exata. O botão no topo alterna entre permitir e bloquear.

Segure a Wrench e use Shift + rolagem para alternar os modos. **Vincular** configura os pylons auxiliares. No modo **Direção**, use Shift + botão direito no ar para escolher um dos seis lados. Clique no pylon para aplicar. O lado inicial é baixo.

## Bulk e devolução dos cabos

Capacidade VI ativa bulk para itens, fluidos, FE e XP nos dois sentidos. Wires, Insert e Extract movem tudo que o destino aceita, até esvaziar uma origem finita ou parar a aceitação. Basta uma ponta da ligação entre pylons ter Capacidade VI para ativar bulk. O limite de FE do próprio pylon é liberado nesse modo; as regras de aceitação dos outros mods e os filtros continuam valendo. Pedidos grandes usam os lotes aceitos pela API externa. O Matter Condenser do AE2 recebe quantidades longas diretamente. Transferências entre origem e destino inesgotáveis usam o maior pedido aceito pela API para preservar o término de cada ciclo.

Quebrar qualquer pylon conectado derruba o cabo uma única vez e remove a conexão. Os vínculos de entrada ficam salvos para devolver o cabo também quando o chunk da origem estiver inativo. Vínculos antigos inválidos devolvem o cabo quando detectados.
