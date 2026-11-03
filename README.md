# Gash's Super Pylons

Mod independente de Minecraft por **Gasharaujo**.

A versão **0.1.0** é a base vazia do projeto: carrega o mod e registra sua inicialização no log. Os pylons, blocos, itens, receitas e efeitos serão desenvolvidos depois. Não exige outros mods além do loader correspondente.

| Minecraft | Loader de compilação | Java | Módulo |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.4.10 | 17 | `forge-1.20.1` |
| 1.21.1 | NeoForge 21.1.248 | 21 | `neoforge-1.21.1` |
| 26.1.2 | NeoForge 26.1.2.109 | 25 | `neoforge-26.1.2` |

Identificador do mod: `gash_super_pylons`

Pacote Java: `net.gasharaujo.superpylons`

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

Os módulos são independentes para acomodar as diferenças entre as APIs do Forge e NeoForge. Não há conteúdo de gameplay nesta etapa. O código de inicialização usa apenas APIs comuns a cliente e servidor.

Integração contínua: `.github/workflows/build.yml` compila as três versões e guarda os JARs como artefatos.

## Referências

- [Configuração do NeoForge 1.21.1](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)
- [Configuração do NeoForge 26.1](https://docs.neoforged.net/docs/gettingstarted/)
- [Metadados do Forge](https://docs.minecraftforge.net/en/1.20.x/gettingstarted/modfiles/)

## Licença

All Rights Reserved. Copyright (c) 2026 Gasharaujo.
