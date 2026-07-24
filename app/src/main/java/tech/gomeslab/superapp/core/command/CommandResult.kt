package tech.gomeslab.superapp.core.command

// Resultado do processamento de um comando. Mensagens destinadas ao usuário em português.
sealed interface CommandResult {
    data class Success(val message: String? = null) : CommandResult
    data class Failure(val message: String) : CommandResult
}
