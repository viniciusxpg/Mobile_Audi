# Vinil

![Ícone do Vinil](docs/icone-vinil.png)

Tocador de música para Android, nativo, em **Kotlin + Jetpack Compose + Media3 (ExoPlayer)**.

## Funcionalidades

- **Menu inferior com ícones**: Músicas, Playlists, Álbuns, Artistas e Pastas, com o mini player logo acima
- **Playlists**: criar, renomear, excluir, adicionar músicas (menu ⋮ ou um álbum/artista/pasta inteiro) e remover
- **Busca** por músicas, álbuns, artistas e playlists, sem diferenciar acentos ("acucar" encontra "Açúcar")
- **Tocar em seguida**: coloca uma música logo depois da atual
- **Fila de reprodução**: veja o que vem a seguir (na ordem real, inclusive no aleatório), pule para uma música, remova, limpe ou arraste para reordenar
- Telas de álbum, artista e pasta com botões **Tocar** e **Aleatório**; álbuns em ordem de faixa
- **Retoma de onde parou**: ao reabrir o app (ou apertar play no fone com o app fechado) volta à última música, na mesma posição, com a mesma fila
- Tela "Tocando agora" (também com o celular deitado) com barra de progresso, aleatório e repetir (desligado / tudo / uma)
- **Equalizador** com ajustes prontos do aparelho, uma barra por faixa de frequência e reforço de graves (vale só para o Vinil e fica salvo)
- Mini player na parte de baixo da biblioteca
- **Biblioteca sempre atualizada**: músicas copiadas ou apagadas aparecem/somem sozinhas, sem tocar em atualizar
- Capas em cache na memória para rolagem leve em bibliotecas grandes
- **Disco girando** em "Tocando agora", com a capa do álbum no selo (para ao pausar; respeita "remover animações")
- **Widget** na tela inicial com capa e botões anterior / tocar-pausar / próxima (funciona com o app fechado)
- Ícone da notificação com a silhueta do disco
- **Reprodução em segundo plano** com notificação, controles na tela de bloqueio e botões de fones Bluetooth
- Pausa automaticamente em ligações (foco de áudio) e ao desconectar o fone
- Busca por voz ("Ok Google, tocar ... no Vinil"), inclusive no Android Auto
- **Android Auto**: navegação por músicas, playlists, álbuns, artistas e pastas direto na central do carro
- Ignora áudios e mensagens de voz do WhatsApp (e WhatsApp Business) e do Telegram, e gravações do rádio FM
- **Cores da logo** (laranja, preto vinil e creme) com **modo claro, escuro ou automático**; opção de usar as cores do papel de parede (Android 12+)
- **Configurações** (⚙️ no topo): tema, cores, equalizador, atualizar biblioteca e **pastas ocultas** — escolha quais pastas aparecem no app

## Requisitos

- Android 7.0 (API 24) ou superior no aparelho
- Android Studio (Ladybug ou mais recente) com JDK 17

## Como rodar

1. Abra a pasta do projeto no Android Studio e aguarde a sincronização do Gradle.
2. Conecte um aparelho (ou crie um emulador) e clique em **Run**.

Pela linha de comando:

```bash
./gradlew assembleDebug        # gera app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # instala no aparelho conectado
./gradlew testDebugUnitTest    # testes unitários
```

O GitHub Actions (`.github/workflows/android.yml`) compila o projeto a cada push e publica o APK de debug como artefato.

### Testando no Android Auto

Instale o **Desktop Head Unit (DHU)** pelo SDK Manager, ative o modo desenvolvedor no app Android Auto
do celular, habilite "Fontes desconhecidas" e inicie o DHU. Como o app ainda não está na Play Store,
o Android Auto só o exibe com "Fontes desconhecidas" habilitado.

## Estrutura

```
app/src/main/java/io/github/viniciusxpg/vinil/
├── MainActivity.kt              # ponto de entrada (Compose)
├── data/
│   ├── Song.kt                  # modelos Song, Album, Artist e Folder
│   ├── Library.kt               # agrupamento por álbum, artista e pasta
│   ├── Search.kt                # busca sem acentos
│   ├── Playlist.kt              # modelo e JSON das playlists
│   ├── PlaylistRepository.kt    # playlists salvas em playlists.json
│   ├── LibrarySettings.kt       # pastas ocultas escolhidas pelo usuário
│   └── MusicRepository.kt       # leitura do MediaStore + cache
├── playback/
│   ├── PlaybackService.kt       # MediaLibraryService: ExoPlayer, notificação, Android Auto
│   ├── PlaybackStateStore.kt    # salva fila/posição para retomar de onde parou
│   ├── EqualizerController.kt   # equalizador e reforço de graves na sessão de áudio do player
│   ├── MediaIds.kt              # IDs da árvore de navegação (songs / albums / artists / folders)
│   └── MediaItems.kt            # conversão Song/Album -> MediaItem
├── ui/
│   ├── VinilApp.kt              # permissões e navegação entre telas
│   ├── PlayerViewModel.kt       # MVVM: MediaController -> StateFlow
│   ├── components/              # AlbumArt, SpinningVinyl, MiniPlayer, SongRow (menu ⋮), diálogos de playlist
│   ├── screens/                 # Library (menu inferior), Collection, Search, NowPlaying, Queue, Equalizer, Settings, Permission
│   └── theme/                   # paletas clara/escura da marca e escolha de tema
├── widget/                      # widget da tela inicial
└── util/                        # permissões, capas, formatação de tempo e de frequência
```

### Arquitetura

```
Compose UI ──> PlayerViewModel ──MediaController──> PlaybackService (MediaLibrarySession + ExoPlayer)
                     │                                       │
                     └──────────> MusicRepository <──────────┘
                                  (MediaStore)
```

A interface nunca toca o ExoPlayer diretamente: ela fala com o serviço pelo `MediaController`,
da mesma forma que a notificação, o relógio e o Android Auto. Por isso a música continua tocando
com o app fechado e todos os controles ficam sincronizados.

## Próximos passos

- targetSdk 37 (Android 17), depois de testar as mudanças de comportamento
- Reordenar músicas das playlists
- Versão de release assinada para a Play Store
