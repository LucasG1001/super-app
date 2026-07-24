package tech.gomeslab.superapp.core.command

import javax.inject.Inject
import javax.inject.Singleton

// Roteia um Command para o handler do namespace correspondente. Ponto único de entrada
// para o backend: na fase 2, o FirebaseMessagingService desserializa o data message em
// Command e chama dispatch(). Hoje é exercitável por teste e por gatilhos locais.
@Singleton
class CommandDispatcher @Inject constructor(
    handlers: Set<@JvmSuppressWildcards CommandHandler>,
) {
    private val byNamespace: Map<String, CommandHandler> = handlers.associateBy { it.namespace }

    suspend fun dispatch(command: Command): CommandResult {
        val handler = byNamespace[command.namespace]
            ?: return CommandResult.Failure("Comando desconhecido: ${command.namespace}")
        return handler.handle(command)
    }
}
