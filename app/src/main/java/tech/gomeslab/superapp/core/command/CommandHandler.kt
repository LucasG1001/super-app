package tech.gomeslab.superapp.core.command

// Cada feature registra um handler para o seu namespace. Handlers são reunidos via
// multibinding do Hilt (@IntoSet) — o análogo Android das factories/registries do stash.
interface CommandHandler {
    val namespace: String
    suspend fun handle(command: Command): CommandResult
}
