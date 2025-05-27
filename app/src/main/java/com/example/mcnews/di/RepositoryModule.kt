package com.example.mcnews.di

import com.example.mcnews.domain.repo.ArticleRepository
import com.example.mcnews.data.repo.ArticleRepositoryImpl
import com.example.mcnews.domain.repo.AuthRepository
import com.example.mcnews.data.repo.AuthRepositoryImpl
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindArticleRepository(
        impl: ArticleRepositoryImpl
    ): ArticleRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository
}