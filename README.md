# Gash's Super Pylons

Mod independente de Minecraft por **Gasharaujo**.

O Super Pylon armazena até **2.048 itens exatamente iguais** e é imune a explosões. A estrutura de ferro usa o modelo fornecido no Blockbench, e o item armazenado substitui visualmente o core branco. A versão **0.4.0** adiciona integrações opcionais para inspecionar o armazenamento.

| Minecraft | Loader de compilação | Java | Módulo |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.4.10 | 17 | `forge-1.20.1` |
| 1.21.1 | NeoForge 21.1.248 | 21 | `neoforge-1.21.1` |
| 26.1.2 | NeoForge 26.1.2.109 | 25 | `neoforge-26.1.2` |

Identificador do mod: `gash_super_pylons`

Pacote Java: `net.gasharaujo.superpylons`

## Super Pylon

Encontre o bloco na aba **Blocos funcionais** do Criativo ou use:

```text
/give @s gash_super_pylons:super_pylon
```

O frame permanece imóvel. O core mantém sua inclinação original, centralizado no vão, e gira no eixo vertical em sentido anti-horário visto de cima, completando uma volta a cada 8 segundos. A flutuação tem período de 4 segundos e percurso total de **1/8 da altura interna do frame**: 1,25 unidades do modelo, entre os extremos inferior e superior. O cubo permanece dentro do frame durante todo o ciclo.

A colisão segue as 12 barras do frame. O core não tem colisão; a área de seleção e clique cobre todo o frame. O bloco tem a dureza e os requisitos de ferramenta do bloco de ferro: use uma picareta de pedra ou superior para recuperá-lo. É imune a explosões.

### Receita e armazenamento

Fabrique com **8 barras de ferro** (`minecraft:iron_ingot`) ao redor de **1 bloco de tijolos de pedra talhados** (`minecraft:chiseled_stone_bricks`), produzindo 1 Super Pylon.

- **Botão direito com um item:** deposita os itens da mão até o limite de 2.048. O item aparece girando e flutuando no lugar do core.
- **Correspondência exata:** item, nome, encantamentos, dano e NBT/componentes precisam ser iguais. A quantidade da stack não faz parte dessa comparação.
- **Botão esquerdo:** retira 1 item. **Shift + botão esquerdo:** retira até uma stack normal do item (64, 16 ou 1, conforme o item).
- Os itens retirados vão para o inventário; se faltar espaço, o restante cai no chão.
- Ao esvaziar o armazenamento, o core branco reaparece e outro tipo de item pode ser inserido.
- Segurar o ataque minera o bloco normalmente em Sobrevivência. Ao quebrar, o conteúdo restante é derrubado em stacks normais. No Criativo, enquanto houver conteúdo, o ataque retira itens em vez de destruir o pylon instantaneamente.

Itens com a tag **`#gsp:modifier_item`** não entram no armazenamento e não substituem o core. A tag é fornecida vazia para receber os modificadores futuros ou itens adicionados por datapacks. Inserir transfere os itens da mão também no Criativo.

### Overlays opcionais

Jade, WTHIT (fork do Waila) e The One Probe mostram o nome do item armazenado e a quantidade de **0 a 2.048** ao inspecionar o Super Pylon. As três integrações estão disponíveis em Forge 1.20.1, NeoForge 1.21.1 e NeoForge 26.1.2.

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
