# Gash's Super Pylons

Mod independente de Minecraft por **Gasharaujo**.

O Super Pylon armazena **2.048 itens exatamente iguais** na configuração inicial e é imune a explosões. A estrutura de ferro usa o modelo fornecido no Blockbench, e o item armazenado substitui visualmente o core branco.

| Minecraft | Loader de compilação | Java | Módulo |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.4.10 | 17 | `forge-1.20.1` |
| 1.21.1 | NeoForge 21.1.248 | 21 | `neoforge-1.21.1` |
| 26.1.2 | NeoForge 26.1.2.109 | 25 | `neoforge-26.1.2` |

Identificador do mod: `gash_super_pylons`

Pacote Java: `net.gasharaujo.superpylons`

## Super Pylon

Encontre o bloco na aba **Gash's Super Pylons** do Criativo ou use:

```text
/give @s gash_super_pylons:super_pylon
```

O frame permanece imóvel. O core mantém sua inclinação original, centralizado no vão, e gira no eixo vertical em sentido anti-horário visto de cima, completando uma volta a cada 8 segundos. A flutuação tem período de 4 segundos e percurso total de **1/8 da altura interna do frame**, o equivalente a 1,5 unidades do modelo. Um tanque cheio ou parcialmente cheio usa a textura do fluido no cubo animado. Ao esvaziar, o core volta a ficar branco.

A colisão e a área de seleção ocupam o bloco inteiro. O pylon tem a dureza e os requisitos de ferramenta do bloco de ferro. Use uma picareta de pedra ou superior para recuperá-lo.

### Receita e armazenamento

Fabrique com **8 barras de ferro** (`minecraft:iron_ingot`) ao redor de **1 bloco de tijolos de pedra talhados** (`minecraft:chiseled_stone_bricks`), produzindo 1 Super Pylon.

- **Botão direito com um item:** deposita os itens da mão até o limite atual. O item aparece girando e flutuando no lugar do core.
- **Correspondência exata:** item, nome, encantamentos, dano e NBT/componentes precisam ser iguais. A quantidade da stack não faz parte dessa comparação.
- **Botão esquerdo:** retira 1 item. **Shift + botão esquerdo:** retira até uma stack normal do item (64, 16 ou 1, conforme o item).
- Os itens retirados vão para o inventário; se faltar espaço, o restante cai no chão.
- Ao esvaziar o armazenamento, o core branco reaparece e outro tipo de item pode ser inserido.
- Segurar o ataque minera o bloco normalmente em Sobrevivência. Na quebra, o conteúdo sai em stacks ou fica no próprio pylon quando o volume é grande. No Criativo, enquanto houver conteúdo, o ataque retira itens em vez de destruir o pylon instantaneamente.

Itens com a tag **`#gsp:modifier_item`** ficam reservados para modificadores. A tag inclui os upgrades e pode ser estendida por datapacks. Inserir transfere os itens da mão também no Criativo.

Funis e cabos de itens podem inserir e extrair do estoque por qualquer face. Leitores de inventário que usam a interface de itens do loader veem o saldo completo até o limite da API. Comparadores leem o nível de ocupação do modo ativo. A entrada respeita o Filter, a capacidade, o modo do pylon e a correspondência exata do item.

### Upgrades

Os upgrades, ferramentas e cabos estão na aba **Gash's Super Pylons** do Criativo, identificada pelo ícone do Super Pylon. A tooltip azul indica a categoria. Clique com o upgrade na mão para instalar. Um tier superior do mesmo tipo substitui o anterior e o devolve ao inventário. Para retirar, use **Shift + botão direito** com a mão principal vazia. A prioridade é filtro, velocidade, quantidade, alcance, processamento, especialidade e armazenamento.

Os ícones aparecem na borda inferior de cada face lateral. O próximo upgrade a sair fica no canto direito, e os demais seguem para a esquerda na ordem de remoção.

| Tier | Intervalo em ticks | Produção por ciclo | Capacidade de armazenamento |
| --- | ---: | ---: | ---: |
| 0 | 60 | 1 | 1× |
| I | 40 | 2 | 2× |
| II | 30 | 4 | 4× |
| III | 20 | 8 | 8× |
| IV | 10 | 16 | 16× |
| V | 5 | 32 | 32× |
| VI | 1 | 64 | Ilimitada |

Um gerador começa com produção de 1 a cada 60 ticks. Os geradores de itens aceitam somente o produto que geram. O **Clicker** usa uma unidade do item armazenado para clicar com o botão direito na face superior do bloco abaixo, no intervalo de velocidade. O item volta ao pylon se o clique o deixar intacto. Um resultado alterado que não combine com o estoque sai como drop.

Fluid Storage, geradores de água e lava, Anvil e Enchanter exigem o pylon sem itens e bloqueiam depósitos de itens.

Fluid Storage e os geradores de água e lava aceitam baldes e automação de fluidos. A capacidade inicial é de **32 baldes (32.000 mB)**. Com Storage I–VI, passa a 64, 128, 256, 512, 1.024 baldes e capacidade ilimitada. Tanques salvos acima do novo limite mantêm todo o líquido e voltam a aceitar fluido quando houver espaço. Os geradores produzem **100 mB por unidade** de produção e aceitam o próprio fluido. Fluid Storage só pode ser removido vazio. A remoção de um gerador com líquido pede um segundo clique em até 5 segundos para descartar seu conteúdo. Quebrar o pylon descarta o líquido.

Remover armazenamento exige que o conteúdo caiba na capacidade restante. Na quebra, volumes acima de 64 stacks são guardados no próprio pylon junto com os upgrades. Coloque o bloco novamente para recuperar esse estoque.

#### XP, energia e agricultura

Os upgrades estão disponíveis no Criativo e por comando.

- **XP Tank** coleta orbes a até 2 blocos da estrutura e drena jogadores logo acima. O Speed define até quantos níveis são drenados ou entregues por jogador por tick. Sem Speed, a taxa é de 1 nível. A taxa nos tiers I–VI é de 2, 4, 8, 16, 32 e 64 níveis. A capacidade inicial é de 32.000 pontos de XP, ampliada pelo Storage. Estoques antigos acima do limite ficam guardados e voltam a aceitar XP assim que houver espaço. O jogador logo abaixo pode receber progresso parcial quando sobrarem poucos pontos.
- O **XP Tank** exige o estoque de itens vazio na instalação e bloqueia novos depósitos de itens. Itens guardados em versões anteriores podem ser retirados normalmente. O tanque se conecta a tubos e tanques de XP líquido do Just Dire Things, Ender IO e Mob Grinding Utils quando os fluidos estão presentes no modpack. Cada ponto equivale a 20 mB, e o pylon guarda frações ao transferir volumes menores.
- **Energy** puxa FE pela face superior, carrega um item compatível no pedestal e passa a energia restante pela face inferior. A carga do item usa o mesmo orçamento de saída da face inferior. No modo Energy o pedestal comporta um item. Esvazie o XP Tank ou o Energy para retirar o upgrade. Quebrar o pylon com XP ou FE guarda o saldo no bloco junto com os upgrades.
- **Range** define o raio horizontal de Fertilizer, Harvester e Magnet. Eles procuram alvos no nível do pylon e um bloco acima e abaixo. Sem Range, o raio é de 1 bloco. Fertilizer e Harvester exigem um pylon vazio e bloqueiam depósitos manuais.
- **Fertilizer** seleciona plantas ao acaso, sem repetir a mesma posição no ciclo. Speed I–VI define o intervalo de 40, 30, 20, 10, 5 ou 1 tick entre ciclos e aplica respectivamente 1, 2, 3, 4, 5 ou 6 random ticks por planta. Quantity I–VI seleciona até 2, 4, 8, 16, 32 ou 64 plantas por ciclo. Na configuração inicial, tenta um random tick em uma planta a cada 60 ticks.
- **Harvester** colhe até uma posição por ciclo de Speed, no mesmo intervalo dos geradores. Reinicia cultivos maduros, recolhe frutos de plantas como cacau e bagas e corta só a ponta de cana, cacto e bambu. Melões e abóboras com caule ligado são recolhidos, mantendo o caule. Troncos e copas ficam intactos. A colheita vai pela face superior do inventário abaixo. Se ele só aceitar parte, o pylon guarda o restante e pausa novas colheitas até entregá-lo.

| Tier | Capacidade de Energy | Transferência por tick e sentido | Área de Range |
| --- | ---: | ---: | ---: |
| 0 | 0 FE | 0 FE | 3×3 |
| I | 1.000.000 FE | 1.000 FE | 7×7 |
| II | 10.000.000 FE | 10.000 FE | 9×9 |
| III | 40.000.000 FE | 40.000 FE | 11×11 |
| IV | 100.000.000 FE | 100.000 FE | 13×13 |
| V | 400.000.000 FE | 400.000 FE | 15×15 |
| VI | 2.000.000.000 FE | Ilimitada | 17×17 |

#### Filtro, ímã e processamento

- **Filter** abre com botão direito e as mãos vazias. Use um item no cursor para marcar um dos 27 slots e botão direito para limpar a marca. O filtro começa no modo de bloqueio e pode alternar para a lista de permissão. Ele compara o item e seus dados exatos tanto nos depósitos manuais quanto na coleta do Magnet.
- **Magnet** coleta itens caídos em uma área inicial de 3×3, com o pylon no centro. Começa com um item a cada 60 ticks e segue os valores de Speed, Quantity e Range. Os itens entram no armazenamento e seguem o filtro e a regra de correspondência exata.
- **Smelt** usa receitas de fornalha. **Pulverize** tritura cobbles em cascalho, cascalho em areia e sílex, terracota em areia vermelha e pedras nas variantes quebradas disponíveis. Também processa blocos de minério e usa os pós equivalentes de `c:dust` quando disponíveis.
- No Pulverize, blocos de minério rendem 2, 3, 4 ou 5 unidades de minério bruto ou gema nos tiers Quantity 0, II, IV e VI. Redstone e lápis-lazúli rendem seis vezes esse valor. Lingotes e gemas rendem um pó. Minério bruto rende 2, 4 ou 6 pós nos tiers 0, IV e VI. O tier intermediário mantém o rendimento do tier anterior.
- O processamento envia os resultados para o inventário abaixo e funciona com geradores de itens quando existe uma receita para o produto gerado. Speed define o intervalo e Quantity define o máximo de itens processados por ciclo. Cada item custa um uso do saldo de combustível, 1.000 FE de um pylon Energy lateral ou combustível de um inventário lateral. Um carvão rende 8 usos. O saldo só diminui ao processar.

O Pulverize aceita arquivos de datapack em `data/<namespace>/pulverizing/<id>.json`. Uma receita de datapack tem prioridade sobre as regras padrão. Use IDs de itens ou tags com prefixo `#`. `count` e `chance` aceitam um número ou uma lista na ordem Quantity 0 a VI. A chance é percentual e os valores padrão são 1 item e 100%.

```json
{
  "ingredient": "minecraft:gravel",
  "results": [
    {"item": "minecraft:sand"},
    {"item": "minecraft:flint", "chance": [30, 40, 50, 60, 80, 90, 100]}
  ]
}
```

#### Ferramentas e cabos

- **Paint Tool** aplica ao frame a textura do bloco da mão secundária sem gastar o bloco. Sem um bloco na mão secundária, Shift + botão direito copia a pintura e botão direito aplica a pintura salva. Uma textura indisponível usa o ferro padrão.
- **Copy Paste Tool** copia os tipos e tiers de upgrades com Shift + botão direito. Botão direito aplica o setup completo se todos os upgrades faltantes estiverem no inventário. Os upgrades retirados voltam ao jogador. Se faltar algum, o chat lista o que falta. Paint Tool e Copy Paste Tool ganham brilho e tooltip quando guardam dados.
- **Energy Wire, Item Wire, Fluid Wire e XP Wire** empilham até 64. Clique primeiro na origem e depois no destino, a até 32 blocos na mesma dimensão. Uma conexão concluída gasta um cabo. Cada par aceita uma conexão, em um sentido. Para desconectar, selecione os dois pylons em qualquer ordem com qualquer tipo de Wire, sem gastar outro cabo. Shift + botão direito no pylon cancela a seleção. O limite por tick é de 10% da capacidade da origem por tipo, compartilhado entre os cabos e o upgrade de transferência desse tipo. Energia também respeita os limites de entrada e saída, e itens seguem o filtro e a regra de correspondência exata do destino.
- **Insert** puxa do bloco diretamente abaixo. **Extract** envia ao bloco diretamente abaixo. Há uma versão de cada para itens, fluidos, energia e XP. Cada recurso tem um slot para definir o sentido, e o limite por tick usa 10% da capacidade do pylon equipado. As portas funcionam com outros pylons e com blocos que expõem a interface compatível. Os upgrades de energia assumem a transferência ativa da face inferior quando instalados.
- **Wrench** seleciona um pylon com Enchanter ou Anvil usando Shift + botão direito. Clique com o botão direito em cada um dos dois auxiliares, com um bloco de ar entre cada auxiliar e o principal. No Enchanter, os auxiliares exibem livros. Com lápis-lazúli em um e XP no outro, enviam partículas ao principal. Na Anvil, orbes de XP giram em torno dos cores auxiliares. Os efeitos são visuais.
- Shift + botão direito no ar limpa os dados salvos na Copy Paste Tool, na Paint Tool ou na Wrench.
- **Break** minera o bloco diretamente abaixo no intervalo do Speed. Usa uma picareta virtual de diamante por padrão. Shift + botão direito com uma ferramenta de mineração na mão principal instala a ferramenta real, com seus encantamentos e desgaste. Ela aparece na ponta direita da barra e sai antes dos upgrades com Shift + botão direito de mão vazia. Quando ela é retirada ou quebra, a mineração volta a usar a picareta virtual. O Break guarda os drops iguais ao primeiro e deixa os demais no chão. Com o armazenamento cheio, a mineração pausa.

### Overlays opcionais

Jade, WTHIT (fork do Waila) e The One Probe mostram o conteúdo, a quantidade, a capacidade e os upgrades instalados. Quando há combustível armazenado no processador, mostram o saldo em usos. As três integrações estão disponíveis em Forge 1.20.1, NeoForge 1.21.1 e NeoForge 26.1.2.

Jade e WTHIT leem o conteúdo que o pylon sincroniza com o cliente. The One Probe usa sua consulta ao servidor, onde também precisa estar instalado em multijogador. Cada integração é ativada pelo overlay correspondente.

Cada módulo gera um JAR próprio. Todas as dependências declaradas no mod usam apenas versão mínima, sem limite máximo. Isso não substitui a validação em outras versões de Minecraft ou do loader.

## Compilar

Na raiz do projeto, com PowerShell e os JDKs 17, 21 e 25 instalados:

```powershell
.\tools\build.ps1
```

O script encontra JDKs em `JAVA_HOME`, `.jdks`, `.gradle/jdks`, Program Files e nos runtimes do CurseForge. Também aceita `JAVA17_HOME`, `JAVA21_HOME` e `JAVA25_HOME`. Os três JARs e seus hashes SHA-256 ficam em `artifacts/`.

Para uma versão específica:

```powershell
.\tools\build.ps1 -Target neoforge-1.21.1
```

Use `-Offline` quando todas as dependências já estiverem no cache.

Alternativamente, entre na pasta do módulo e execute `./gradlew build` (Linux/macOS) ou `.\gradlew.bat build` (Windows), com o JDK correspondente em `JAVA_HOME`.

## Testar

Para compilar e instalar nas instâncias de teste:

```powershell
.\tools\build.ps1 -Install
```

| Módulo | Instância de teste |
| --- | --- |
| `forge-1.20.1` | `C:\Users\pichau\curseforge\minecraft\Instances\20.1` |
| `neoforge-1.21.1` | `C:\Users\pichau\curseforge\minecraft\Instances\21` |
| `neoforge-26.1.2` | `C:\Users\pichau\curseforge\minecraft\Instances\26.1.2` |

O script instala somente o JAR correspondente e preserva versões antigas deste mod com a extensão `.disabled`. Feche a instância antes de atualizar o arquivo. As instâncias ficam fora do repositório.

Depois, abra a instância pelo CurseForge e confira o mod na lista de mods. No arquivo `logs/latest.log`, procure:

```text
Gash's Super Pylons loaded (gash_super_pylons)
```

Para um cliente de desenvolvimento isolado, execute `.\gradlew.bat runClient` dentro do módulo. Para servidor de desenvolvimento, use `.\gradlew.bat runServer`; a aceitação da EULA é feita pelo responsável pelo servidor.

## Desenvolvimento

Os módulos usam registros, persistência e renderizadores específicos para as APIs do Forge e NeoForge. O código e os recursos compartilhados ficam em `common/src/main/`. A animação é visual e não exige ticker de servidor. Alterações de conteúdo são salvas e sincronizadas com o pacote vanilla de atualização de block entity.

Os builds NeoForge executam testes JUnit com um servidor temporário do framework oficial, cobrindo capacidade, conservação dos itens, metadados, modificadores, persistência e propriedades de quebra/explosão. Os testes e sua tag de exemplo não são incluídos nos JARs distribuídos.

O arquivo original foi movido de `Pylon.json` para `model-source/Pylon.json`. O modelo de frame usado pelo jogo fica em `common/src/main/resources/assets/gash_super_pylons/models/block/super_pylon.json`. O core é separado do modelo estático para permitir sua rotação em todas as versões.

Após editar o modelo original, execute `python tools/generate_model.py` para atualizar os recursos. Mudanças de tamanho ou inclinação do core também precisam ser aplicadas em `PylonCoreModel.java` e `PylonAnimation.java`. Os recursos gerados são versionados; compilar não exige Python.

Integração contínua: `.github/workflows/build.yml` compila as três versões e guarda os JARs como artefatos.

## Referências

- [Configuração do NeoForge 1.21.1](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)
- [Configuração do NeoForge 26.1](https://docs.neoforged.net/docs/gettingstarted/)
- [Metadados do Forge](https://docs.minecraftforge.net/en/1.20.x/gettingstarted/modfiles/)

## Licença

All Rights Reserved. Copyright (c) 2026 Gasharaujo.
