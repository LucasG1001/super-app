package tech.gomeslab.superapp.command

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.gomeslab.superapp.core.command.Command
import tech.gomeslab.superapp.core.command.CommandDispatcher
import tech.gomeslab.superapp.core.command.CommandHandler
import tech.gomeslab.superapp.core.command.CommandResult

private class FakeHandler(
    override val namespace: String,
    val onHandle: (Command) -> CommandResult = { CommandResult.Success() },
) : CommandHandler {
    var received: Command? = null
    override suspend fun handle(command: Command): CommandResult {
        received = command
        return onHandle(command)
    }
}

class CommandDispatcherTest {

    @Test
    fun `routes command to handler matching namespace`() = runTest {
        val alarm = FakeHandler("alarm") { CommandResult.Success("ok") }
        val other = FakeHandler("other")
        val dispatcher = CommandDispatcher(setOf(alarm, other))

        val result = dispatcher.dispatch(Command("alarm", "create", mapOf("hour" to "7")))

        assertEquals(CommandResult.Success("ok"), result)
        assertEquals("create", alarm.received?.action)
        assertEquals(null, other.received)
    }

    @Test
    fun `returns failure for unknown namespace`() = runTest {
        val dispatcher = CommandDispatcher(setOf(FakeHandler("alarm")))

        val result = dispatcher.dispatch(Command("unknown", "x"))

        assertTrue(result is CommandResult.Failure)
    }
}
