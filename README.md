# SuperApp

App Android nativo que reúne **vários "apps" próprios num só** e funciona também **como uma API do
aparelho**: um backend envia comandos e o app os executa no celular (transporte via Firebase Cloud
Messaging na fase 2 — a arquitetura de comandos já está pronta).

## Features

- **Despertador** — alarmes locais completos: criar/editar/excluir, recorrência por dia da semana,
  som + tela de alarme sobre o lockscreen, soneca, persistência e reagendamento após reboot.

## Stack

Kotlin · Jetpack Compose (Material 3) · Navigation Compose · MVVM (ViewModel + StateFlow) · Hilt ·
Room · Coroutines/Flow · AlarmManager. `minSdk 24`, `targetSdk 36`, Java 17.

## Como rodar

Pré-requisitos: JDK 17+ e Android SDK (`sdk.dir` em `local.properties`).

```bash
./gradlew assembleDebug        # build do APK debug
./gradlew installDebug         # instala no device/emulador conectado
./gradlew testDebugUnitTest    # testes unitários
./gradlew lintDebug            # análise estática
```

## Arquitetura

Detalhes de arquitetura, convenções e restrições de build estão em [`CLAUDE.md`](CLAUDE.md) — em
especial a **camada de comandos** (`core/command`), que é o coração do "app como API", e as notas
sobre o AGP 9 (Kotlin embutido, Compose/KSP/Hilt/Room).
