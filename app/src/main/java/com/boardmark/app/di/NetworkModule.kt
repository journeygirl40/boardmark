package com.boardmark.app.di

import com.boardmark.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

/**
 * User-Agentを個別に指定していないリクエスト(主にCoil経由のサムネイル画像の読み込み)に付ける値。
 * OkHttpの既定値("okhttp/x.y.z")のままだと、Wikimediaのように「どのアプリからの
 * アクセスか分かるUser-Agent」を必須にしている画像サーバーから403で拒否され、
 * サムネイルのURLは取得できているのに画像が表示されなかった。
 */
private val APP_USER_AGENT =
    "Boardmark/${BuildConfig.VERSION_NAME} (Android; +https://journeygirl40.github.io/boardmark/) okhttp"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        // ページ取得(OgpFetcher)のように呼び出し側がUser-Agentを指定している場合はそれを優先する。
        .addInterceptor { chain ->
            val request = chain.request()
            if (request.header("User-Agent") != null) {
                chain.proceed(request)
            } else {
                chain.proceed(request.newBuilder().header("User-Agent", APP_USER_AGENT).build())
            }
        }
        .build()
}
