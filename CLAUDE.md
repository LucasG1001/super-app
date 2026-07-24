# CLAUDE.md

Este arquivo orienta o Claude Code (claude.ai/code) ao trabalhar neste repositório.

## Visão geral

**SuperApp** é um app Android nativo que reúne **vários "apps" próprios num só** (multi-feature)
e que também funciona **como uma API do aparelho**: o backend (`https://media.gomeslab.tech`)
envia comandos e o app os executa no celular. O transporte dos comandos será o **Firebase Cloud
Messaging (FCM)** — fase 2. Hoje a **camada de comandos já existe e é funcional localmente**
(sem Firebase).

Segue a filosofia do projeto irmão `stash` (web): camadas por feature, registries para cortar
boilerplate, **design tokens centralizados** (tema dark, accent roxo `#8B5CF6`), código em
**inglês** e textos de UI em **português**.

Features ("apps") atuais:
- **Despertador** (`feature/alarm`) — alarmes locais completos: criar/editar/excluir, recorrência
  por dia da semana, tela de toque sobre o lockscreen, soneca, persistência e reagendamento após
  reboot.

## Comandos de desenvolvimento

Pré-requisitos: JDK 17+ (o toolchain do Gradle usa 21), Android SDK (`sdk.dir` em
`local.properties`). O wrapper usa Gradle 9.5.

```bash
./gradlew assembleDebug        # build do APK debug
./gradlew testDebugUnitTest    # testes unitários (JVM)
./gradlew lint                 # análise estática do Android
./gradlew installDebug         # instala no device/emulador conectado
```

Antes de considerar uma mudança pronta: `assembleDebug` e `lintDebug` passam, e
`testDebugUnitTest` quando tocar em algo coberto.

### Restrições de build (AGP 9 — não óbvias, não quebrar)

- **AGP 9 tem Kotlin embutido** (KGP 2.2.10). **Não** aplique `org.jetbrains.kotlin.android` — ele
  colide ("extension 'kotlin' already registered"). O `jvmTarget` é configurado no bloco
  `kotlin { compilerOptions { … } }`, não em `android { kotlinOptions { … } }`.
- **Plugin do Compose e KSP devem casar a versão do Kotlin embutido** (2.2.10 → KSP `2.2.10-2.0.2`).
- **KSP roda em KSP2** (KSP1 é incompatível com o Kotlin embutido). Funções `suspend` de DAO que
  retornam `Unit` disparam o bug do KSP2 "unexpected jvm signature V" — por isso os DAOs retornam
  `Int`/`Long`.
- **Room 2.8.x** (codegen Kotlin por padrão no KSP). Room 2.6.x gera Java incompatível com as
  assinaturas `suspend` do Kotlin 2.2 e falha a compilação.
- **Hilt 2.60.x** — versões < 2.59 não suportam AGP 9 ("Android BaseExtension not found").
- `gradle.properties` tem `android.disallowKotlinSourceSets=false` (necessário para o KSP com
  Kotlin embutido).
- O tema XML (`themes.xml`) usa parent de plataforma `@android:style/Theme.Material.NoActionBar` —
  não há a lib Material Components (Views); as cores reais vêm do Compose.

## Arquitetura

### Stack

Kotlin + **Jetpack Compose** (Material 3), single-Activity + **Navigation Compose**. **MVVM**
(ViewModel + StateFlow) por feature; camadas `data / domain / ui`. **Hilt** para DI, **Room** para
persistência, **Coroutines/Flow** para async. Java 17. `minSdk 24`, `targetSdk 36`.
`applicationId`/namespace: `tech.gomeslab.superapp`.

### Camada de comandos ("app como API") — `core/command`

```
Backend (fase 2: FCM data message) → CommandDispatcher.dispatch(Command)
    → CommandHandler do namespace (ex.: AlarmCommandHandler) → executa a ação no aparelho
```

- **`Command`** (`namespace`, `action`, `payload: Map<String,String>`) — o `payload` em strings é
  compatível com data messages do FCM.
- **`CommandHandler`** — cada feature implementa um, com seu `namespace`. Registrados via
  **multibinding do Hilt** (`@Binds @IntoSet` em `core/di/CommandModule`) — para adicionar uma
  feature ao "modo API", basta registrar seu handler ali.
- **`CommandDispatcher`** — roteia por `namespace`. Ponto único de entrada do backend; hoje
  exercitável por teste e por gatilhos locais. Na fase 2, um `FirebaseMessagingService`
  desserializa o data message em `Command` e chama `dispatch()`.

### Estrutura de pacotes (`app/src/main/java/tech/gomeslab/superapp/`)

```
SuperApp.kt              # Application (@HiltAndroidApp) + canais de notificação
MainActivity.kt          # única Activity Compose; hospeda o AppNavHost
core/
  command/               # Command, CommandResult, CommandHandler, CommandDispatcher
  data/                  # AppDatabase (Room)
  di/                    # DatabaseModule, CommandModule
  navigation/            # Routes, AppNavHost
  ui/theme/              # Color, Type, Shape, Dimens, Theme (design tokens do stash)
  ui/home/               # HomeScreen (launcher em grade dos "apps")
feature/alarm/
  data/                  # AlarmEntity, AlarmDao, AlarmRepository
  domain/                # Alarm, mappers, NextTrigger (puro/testável), AlarmScheduler
  ui/list · ui/edit · ui/ring   # telas + ViewModels; AlarmRingActivity (full-screen)
  receiver/              # AlarmReceiver (dispara), BootReceiver (reagenda pós-boot)
  command/               # AlarmCommandHandler (namespace "alarm")
  AlarmIntents / AlarmNotifier   # actions/extras e a notificação de alarme tocando
```

### Despertador — invariantes

- **Agendamento** via `AlarmScheduler` usando `AlarmManager.setAlarmClock` — é exato e **não
  exige** a permissão `SCHEDULE_EXACT_ALARM`, além de mostrar o ícone de alarme do sistema.
- **`daysOfWeek`** é um bitmask: bit = `Calendar.DAY_OF_WEEK - 1` (Dom=0 … Sáb=6). `0` = alarme
  único, que dispara uma vez e se **auto-desativa** (`enabled=false`).
- **`NextTrigger.compute(now, hour, minute, daysOfWeek)`** é uma função **pura** (recebe `now`),
  isolada para ser testável sem framework Android.
- Ao disparar, `AlarmReceiver` inicia o **`AlarmService`** (foreground service, que toca o som e
  vibra via `AlarmPlayer` e publica a notificação com **full-screen intent** + ações
  Soneca/Desligar) e também tenta `startActivity(AlarmRingActivity)` direto — o exact alarm dá uma
  janela de background-activity-launch, o que abre a tela mesmo desbloqueado (quando o SO permite;
  senão o full-screen intent cobre o caso de tela bloqueada). Repetentes são **reagendados na hora
  do disparo**.
- **Som/vibração ficam no service, não na Activity** — o full-screen intent só abre a Activity com
  a tela bloqueada; desbloqueada, o sistema mostra só a notificação. Por isso o alarme precisa
  tocar independentemente da tela abrir, e a notificação leva os botões Soneca/Desligar.
- A `AlarmRingActivity` é só a UI de dispensa (aparece sobre o lockscreen); seus botões
  encaminham Soneca/Desligar ao `AlarmService`. Exige a permissão **full-screen intent**
  (Android 14+ não concede automaticamente; pedimos na tela de lista).

## Convenções

- **Idioma**: código (variáveis, tipos, arquivos) em inglês; textos ao usuário (UI, mensagens de
  comando) em português.
- **Kotlin**: sem `!!` desnecessário; use `data class` e sealed types para modelos/resultados.
- **UI/estilo**: Compose Material 3, tema **dark-only**. Sempre consuma os tokens de
  `core/ui/theme` (cores, `Spacing`, `Shapes`, `Typography`) — **nunca** hardcode de cor/tamanho
  nos composables.
- **Estado**: ViewModel + `StateFlow`; nada de estado global mutável fora disso.
- **DI**: Hilt. Features se plugam ao "modo API" registrando um `CommandHandler` via `@IntoSet`.
- **Sem comentários no código**, exceto para registrar uma restrição não óbvia.

## Pendências / fase 2

- **Firebase/FCM**: adicionar `google-services.json` (projeto Firebase a criar), o plugin
  `google-services`, deps do FCM e um `FirebaseMessagingService` que converta o data message em
  `Command` e chame `CommandDispatcher`. A arquitetura já está pronta para isso.

## Manutenção deste arquivo

- Quando uma mudança tornar algo aqui factualmente incorreto (arquivo/pacote renomeado ou
  removido, comando alterado, novo invariante ou feature), atualize a linha afetada na mesma
  tarefa.
- Edite no lugar e remova o obsoleto — este arquivo não cresce sem contrapartida. Prefira
  descrever padrões/invariantes estáveis a listar arquivos.
- Documente fatos, não preferências. Não adicione convenções novas por conta própria: proponha e
  deixe a decisão de estilo comigo.
- Mantenha conciso e em português.
