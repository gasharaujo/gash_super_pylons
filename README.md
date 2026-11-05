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

O Upgrade Base usa cinco pedregulhos em cruz. Nos upgrades de tier, o centro recebe o Upgrade Base no tier I e o tier anterior nos demais. Receitas de geradores devolvem um balde vazio por balde de água ou lava usado. Cada receita de Wire produz 16 cabos. Coloque um Insert ou Extract sozinho na bancada para convertê-lo na versão oposta do mesmo recurso.

- **Botão direito com um item:** deposita os itens da mão até o limite atual. O item aparece girando e flutuando no lugar do core.
- **Correspondência exata:** item, nome, encantamentos, dano e NBT/componentes precisam ser iguais. A quantidade da stack não faz parte dessa comparação.
- **Botão esquerdo:** retira 1 item. **Shift + botão esquerdo:** retira até uma stack normal do item (64, 16 ou 1, conforme o item).
- Os itens retirados vão para o inventário; se faltar espaço, o restante cai no chão.
- Ao esvaziar o armazenamento, o core branco reaparece e outro tipo de item pode ser inserido.
- Segurar o ataque minera o bloco normalmente em Sobrevivência. Na quebra, o conteúdo sai em stacks ou fica no próprio pylon quando o volume é grande. No Criativo, ataque com uma picareta para quebrar o pylon com conteúdo.

Itens com a tag **`#gsp:modifier_item`** ficam reservados para modificadores. A tag inclui os upgrades e pode ser estendida por datapacks. Inserir transfere os itens da mão também no Criativo.

Funis e cabos de itens podem inserir e extrair do estoque por qualquer face. Leitores de inventário que usam a interface de itens do loader veem o saldo completo até o limite da API. Comparadores leem o nível de ocupação do modo ativo. A entrada respeita o Filter, a capacidade, o modo do pylon e a correspondência exata do item.

### Upgrades

Os upgrades, ferramentas e cabos estão na aba **Gash's Super Pylons** do Criativo, identificada pelo ícone do Super Pylon. A tooltip azul indica a categoria. Clique com o upgrade na mão para instalar. Um tier superior do mesmo tipo substitui o anterior e o devolve ao inventário. Para retirar, use **Shift + botão direito** com a mão principal vazia. Quando há um gerador e um processador, o gerador sai primeiro.

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

Um gerador começa com produção de 1 a cada 60 ticks. Há um espaço exclusivo para gerador: ele só conflita com outro gerador e pode coexistir com uma especialidade/utilidade e um processador. A geração e a utilidade atuam no mesmo ciclo. Um estoque de outro item pausa a geração até haver espaço compatível, sem impedir a instalação. Geradores continuam saindo antes de processadores. O **Clicker** clica no bloco do lado de trabalho, inclusive de mão vazia. Se houver item armazenado, usa uma unidade por operação. O item volta ao pylon se o clique o deixar intacto. Um resultado alterado que não combine com o estoque sai como drop.

Fluid Storage exige o pylon sem itens e bloqueia depósitos de itens. Geradores de água e lava possuem inventário de itens independente do tanque, respeitando as regras da especialidade instalada. Anvil e Enchanter recebem um item por vez no núcleo para processá-lo; retire itens excedentes antes de instalar esses upgrades.

Fluid Storage e os geradores de água e lava aceitam baldes e automação de fluidos. A capacidade inicial é de **32 baldes (32.000 mB)**. Com Storage I–VI, passa a 64, 128, 256, 512, 1.024 baldes e capacidade ilimitada. Tanques salvos acima do novo limite mantêm todo o líquido e voltam a aceitar fluido quando houver espaço. Os geradores produzem **100 mB por unidade** de produção e aceitam o próprio fluido; com Fluid Storage, o tanque aceita qualquer fluido e a geração pausa quando o fluido armazenado for diferente. Com XP Tank e gerador de água/lava juntos, a automação enxerga dois reservatórios separados, um de fluido e outro de XP líquido. Fluid Storage só pode ser removido vazio. A remoção de um gerador com líquido pede um segundo clique em até 5 segundos para descartar seu conteúdo. Quebrar o pylon descarta o líquido.

Remover armazenamento exige que o conteúdo caiba na capacidade restante. Na quebra, volumes acima de 64 stacks são guardados no próprio pylon junto com os upgrades. Coloque o bloco novamente para recuperar esse estoque.

#### XP, energia e agricultura

Os upgrades com receita também aparecem no JEI quando o mod está instalado.

- **XP Tank** coleta orbes a até 2 blocos da estrutura e drena jogadores logo acima. O Speed define até quantos níveis são drenados ou entregues por jogador por tick. Sem Speed, a taxa é de 1 nível. A taxa nos tiers I–VI é de 2, 4, 8, 16, 32 e 64 níveis. A capacidade inicial é de 32.000 pontos de XP, ampliada pelo Storage. Estoques antigos acima do limite ficam guardados e voltam a aceitar XP assim que houver espaço. O jogador logo abaixo pode receber progresso parcial quando sobrarem poucos pontos.
- O **XP Tank** exige o estoque de itens vazio na instalação. Um frasco de experiência vira sete pontos de XP e devolve o frasco de vidro. Um frasco de vidro consome sete pontos para virar um frasco de experiência. O tanque se conecta aos fluidos de XP líquido do Just Dire Things, Ender IO e Mob Grinding Utils presentes no modpack. Cada ponto equivale a 20 mB, com suporte a frações.
- **Energy** cabe em qualquer pylon e recebe FE por todas as faces. Puxa do lado oposto ao de trabalho, carrega um item compatível quando ele está sozinho no pedestal e envia o restante ao lado de trabalho. Esvazie XP ou FE antes de retirar o upgrade correspondente. Quebrar o pylon guarda o saldo no bloco junto com os upgrades.
- **Range** amplia o raio horizontal e vertical de Fertilizer, Harvester, Planter e Magnet. Sem Range, o raio é de um bloco. Fertilizer e Harvester exigem um pylon vazio na instalação.
- **Fertilizer** seleciona plantas ao acaso, sem repetir a mesma posição no ciclo. Speed I–VI define o intervalo de 40, 30, 20, 10, 5 ou 1 tick entre ciclos e aplica respectivamente 1, 2, 3, 4, 5 ou 6 random ticks por planta. Quantity I–VI seleciona até 2, 4, 8, 16, 32 ou 64 plantas por ciclo. Na configuração inicial, tenta um random tick em uma planta a cada 60 ticks.
- **Harvester** reinicia cultivos maduros, recolhe frutos e corta a ponta de cana, cacto e bambu. Usa o inventário no lado de trabalho quando disponível. Também derruba troncos com copa natural e recolhe os drops que couberem no pylon. O excedente fica no chão.
- **Planter** planta sementes e mudas do estoque em espaços válidos na área de Range. Speed define o intervalo e Quantity define quantas plantas cabem em cada ciclo.
- **Placer** coloca um bloco do estoque no lado de trabalho por ciclo de Speed.
- **Dropper** solta itens armazenados no centro do bloco do lado de trabalho, sem espalhar. Começa com um item a cada 60 ticks. Quantity define quantos itens saem por ciclo, Speed define o intervalo e cada tier de Range estende a distância em um bloco. Se o espaço de saída estiver bloqueado, os itens ficam no pylon.
- **Super** faz quatro Nether Stars orbitarem o núcleo. Tem slot próprio e sai primeiro ao remover upgrades.
- **Void** descarta itens, fluidos, FE e XP recebidos.
- **Chunk Loader** mantém os chunks ativos em um quadrado centrado no chunk do pylon. Começa com raio de um chunk e usa o raio do upgrade de Range quando instalado.

| Tier | Capacidade de Energy | Transferência por tick e sentido | Área de Range |
| --- | ---: | ---: | ---: |
| 0 | 0 FE | 0 FE | 3×3×3 |
| I | 1.000.000 FE | 1.000 FE | 7×7×7 |
| II | 10.000.000 FE | 10.000 FE | 9×9×9 |
| III | 40.000.000 FE | 40.000 FE | 11×11×11 |
| IV | 100.000.000 FE | 100.000 FE | 13×13×13 |
| V | 400.000.000 FE | 400.000 FE | 15×15×15 |
| VI | 2.000.000.000 FE | Ilimitada | 17×17×17 |

#### Filtro, ímã e processamento

- **Filter** abre com botão direito e as mãos vazias. Use um item no cursor ou arraste do JEI para preencher um dos 27 slots. Botão direito limpa o slot e botão do meio permite selecionar uma tag do item. O filtro começa em modo de bloqueio e pode alternar para permissão.
- **Magnet** coleta itens caídos em uma área inicial de 3×3×3, com o pylon no centro. Começa com um item a cada 60 ticks e segue os valores de Speed, Quantity e Range. Os itens entram no armazenamento e seguem o filtro e a regra de correspondência exata.
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
- **Energy Wire, Item Wire, Fluid Wire e XP Wire** empilham até 64. Clique primeiro na origem e depois no destino, a até 32 blocos na mesma dimensão. Uma conexão concluída gasta um cabo. Cada par aceita uma conexão, em um sentido. Para desconectar, selecione os dois pylons em qualquer ordem com qualquer tipo de Wire. O cabo instalado volta ao inventário ou cai no chão se estiver cheio. Shift + botão direito no pylon cancela a seleção. O limite por tick é de 10% da capacidade da origem por tipo, compartilhado entre os cabos e o upgrade de transferência desse tipo. Energia também respeita os limites de entrada e saída, e itens seguem o filtro e a regra de correspondência exata do destino.
- **Insert** puxa do bloco no lado de trabalho. **Extract** envia para ele. As portas funcionam com outros pylons e com blocos que expõem a interface compatível. Capacidade VI libera o limite de 10% para Wire, Insert e Extract. Os cabos são amarelos para itens, azuis para fluidos, vermelhos para energia e verdes para XP.
- **Wrench** alterna entre os modos Vincular e Direção com Shift + rolagem. No modo Vincular, selecione Enchanter ou Anvil com Shift + botão direito. Clique em cada auxiliar a uma distância de 1 a 5 blocos entre centros, inclusive na vertical e na diagonal. Os dois auxiliares principais do Enchanter mostram livros e partículas; o terceiro auxiliar de blacklist mantém o item ou livro no núcleo. Anvil mostra oito orbes que circulam os núcleos auxiliares.
- No modo Direção, Shift + botão direito no ar alterna entre os seis lados. Botão direito no pylon aplica o lado selecionado. O padrão é baixo.
- Shift + botão direito no ar limpa os dados da Copy Paste Tool, da Paint Tool e da Wrench no modo Vincular.
- **Break** minera o bloco no lado de trabalho no intervalo do Speed. Usa uma picareta virtual de diamante por padrão. Shift + botão direito com uma ferramenta de mineração na mão principal instala a ferramenta real, com seus encantamentos e desgaste. Ela aparece na barra na ordem de remoção, logo depois do Super quando instalado. Quando ela é retirada ou quebra, a mineração volta a usar a picareta virtual. O Break guarda os drops iguais ao primeiro e deixa os demais no chão. Com o armazenamento cheio, a mineração pausa.

### Wiki dentro do jogo

Com GuideME instalado, abra a wiki pelo atalho de guia ao apontar para um Super Pylon no inventário. Também é possível pegar o livro com `/guideme give @s gash_super_pylons:wiki`. A wiki inclui páginas em português e inglês nas três versões. Ponder é opcional e oferece uma cena de introdução no Forge 1.20.1 e no NeoForge 1.21.1.

### Overlays opcionais

Jade, WTHIT (fork do Waila) e The One Probe mostram o conteúdo, a quantidade, a capacidade e os upgrades instalados. Quando há combustível armazenado no processador, mostram o saldo em usos. As três integrações estão disponíveis em Forge 1.20.1, NeoForge 1.21.1 e NeoForge 26.1.2.

Jade e WTHIT leem o conteúdo que o pylon sincroniza com o cliente. The One Probe usa sua consulta ao servidor, onde também precisa estar instalado em multijogador. Cada integração é ativada pelo overlay correspondente.

Cada módulo gera um JAR próprio. Todas as dependências declaradas no mod usam apenas versão mínima, sem limite máximo. Isso não substitui a validação em outras versões de Minecraft ou do loader.


## Creative Storage

Use um **Command Block vanilla** com botão direito no pylon para instalar Creative Storage em um espaço próprio. Ele aparece na barra de upgrades e nas informações de Jade/WTHIT/The One Probe. Com permissão de operador, o Command Block também aparece na aba criativa do mod, com tooltip azul explicando a função.

Depois de inserir um item, fluido, FE ou XP, as retiradas passam a fornecer cópias sem diminuir o conteúdo inicial. A função vale para retirada manual, funis, cabos, wires, Insert/Extract, outros pylons e consumo pelas máquinas do mod. Um armazenamento vazio não produz recursos. Fluidos, FE e XP ainda precisam dos respectivos upgrades para ter armazenamento, e os limites de capacidade e transferência continuam aplicáveis. A configuração de item e fluido mantém a correspondência exata existente.

Shift + botão direito com a mão vazia devolve o Command Block na ordem normal de remoção, após Super e a ferramenta de Break quando presentes. Ao removê-lo, o conteúdo inicial volta a ser finito. A Copy Paste Tool inclui o Command Block no setup e exige um no inventário para aplicá-lo. Ao quebrar um pylon com Creative Storage, ele guarda seu setup, itens, FE e XP num pylon com dados; os fluidos continuam sendo descartados ao quebrar, como nos outros pylons.

## Enchanter e Anvil

Os dois upgrades trabalham com um item de entrada por vez no pylon principal e exigem dois auxiliares vinculados com a Wrench: um com **XP Tank** e outro com os materiais. Os dois primeiros vínculos definem esses auxiliares, em qualquer ordem. O alcance é de 1 a 5 blocos entre centros, em qualquer direção. Speed define o intervalo de operação: 60/40/30/20/10/5/1 ticks. O resultado substitui o item no núcleo e fica disponível para retirada manual ou por automação. Se faltar XP, material ou uma combinação válida, nada é consumido. Jade, WTHIT e The One Probe mostram o estado e o custo em pontos de XP.

### Enchanter

- O item a encantar fica no principal; lápis-lazúli fica no auxiliar de itens. Cada operação consome **um lápis-lazúli e 27 pontos de XP**, equivalentes a três níveis contados a partir de zero, independentemente do poder.
- O poder base é **1/10/20/30/40/50/100** para Capacidade 0/I/II/III/IV/V/VI. Capacidade modifica o poder de encantamento dessa máquina; seu espaço de trabalho continua sendo um item.
- Quantity no principal define o mínimo de **1/2/3/4/5/6/7 encantamentos**. A máquina respeita compatibilidade e disponibilidade: se não houver encantamentos suficientes, aplica os possíveis. Um item já encantado não é reencantado automaticamente.
- Com **Super**, um terceiro auxiliar pode conter um item ou livro encantado. Os encantamentos nele formam uma blacklist; esse item não é consumido e continua visível no núcleo. Remover Super libera esse terceiro vínculo.
- Estantes do Apotheosis/Apothic Enchanting podem ser instaladas com botão direito, até 64 estantes. A integração consulta os valores e a seleção de encantamentos do próprio mod para aplicar Eterna, Quanta, Arcana, tesouros, estabilidade/retificação e blacklist, conforme a versão. Pistas de interface não alteram o processamento automático. As estantes são salvas, aparecem na barra e saem antes do Enchanter, quando ele for o próximo upgrade a remover.

### Anvil

- O item principal vai na Anvil e o segundo item, livro ou material de reparo fica no auxiliar de itens. Um item do auxiliar é usado por operação. A máquina mantém as regras de compatibilidade dos encantamentos e também aceita reparos válidos de bigorna.
- Encantamentos de mesmo nível combinam para o próximo nível, inclusive acima do máximo normal. Níveis diferentes preservam o maior. O limite técnico de gravação usado é 255.
- O preço soma `nível × raridade` dos encantamentos resultantes: comum 1, incomum 2, raro 3 e muito raro 4. Para cada encantamento acima do máximo normal, seu custo é multiplicado novamente pela raridade, com multiplicador mínimo 2. Reparos sem encantamentos custam ao menos um nível.
- O total em níveis é convertido em pontos de XP pela curva vanilla a partir do nível zero, debitados do XP Tank auxiliar.
- Com **Super**, um **livro encantado** usado como segundo item não é consumido. Ferramentas e materiais de reparo continuam sendo consumidos. Se a combinação não melhorar os encantamentos nem reparar o item, a máquina não gasta recursos.

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
