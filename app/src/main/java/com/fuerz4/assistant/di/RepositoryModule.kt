package com.fuerz4.assistant.di

import com.fuerz4.assistant.data.repository.AuthRepositoryImpl
import com.fuerz4.assistant.data.repository.ChatRepositoryImpl
import com.fuerz4.assistant.data.repository.DeviceRepositoryImpl
import com.fuerz4.assistant.data.repository.ProfileRepositoryImpl
import com.fuerz4.assistant.domain.repository.AuthRepository
import com.fuerz4.assistant.domain.repository.ChatRepository
import com.fuerz4.assistant.domain.repository.DeviceRepository
import com.fuerz4.assistant.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}
