package tech.gomeslab.superapp.core.command

// Um comando vindo do backend (via FCM na fase 2) ou de um gatilho local.
// `namespace` seleciona a feature (ex.: "alarm"); `action` a operação (ex.: "create").
// `payload` carrega os parâmetros como strings (compatível com data messages do FCM).
data class Command(
    val namespace: String,
    val action: String,
    val payload: Map<String, String> = emptyMap(),
)
