package tech.gomeslab.superapp.core.di

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.gomeslab.superapp.core.command.CommandHandler
import tech.gomeslab.superapp.feature.alarm.command.AlarmCommandHandler

// Registra os handlers de comando de cada feature no set consumido pelo CommandDispatcher.
// Nova feature = adicionar um @Binds @IntoSet aqui.
@Module
@InstallIn(SingletonComponent::class)
abstract class CommandModule {

    @Binds
    @IntoSet
    abstract fun bindAlarmCommandHandler(handler: AlarmCommandHandler): CommandHandler
}
