package com.example.data.provider

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ModrinthSearchResponse(
    val hits: List<ModrinthHit>,
    val total_hits: Int,
    val offset: Int,
    val limit: Int
)

@JsonClass(generateAdapter = true)
data class ModrinthHit(
    @Json(name = "project_id") val projectId: String,
    val slug: String,
    val title: String,
    val description: String,
    val author: String,
    @Json(name = "icon_url") val iconUrl: String?,
    val downloads: Long,
    val versions: List<String>?,
    val categories: List<String>?,
    val license: String?
)

@JsonClass(generateAdapter = true)
data class ModrinthProject(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    @Json(name = "icon_url") val iconUrl: String?,
    val downloads: Long,
    val license: ModrinthLicense?,
    @Json(name = "game_versions") val gameVersions: List<String>?,
    val loaders: List<String>?,
    @Json(name = "source_url") val sourceUrl: String?,
    @Json(name = "issues_url") val issuesUrl: String?,
    @Json(name = "wiki_url") val wikiUrl: String?
)

@JsonClass(generateAdapter = true)
data class ModrinthLicense(
    val id: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class ModrinthVersion(
    val id: String,
    @Json(name = "project_id") val projectId: String,
    @Json(name = "version_number") val versionNumber: String,
    val name: String,
    val changelog: String?,
    @Json(name = "game_versions") val gameVersions: List<String>,
    val loaders: List<String>,
    val files: List<ModrinthFile>,
    val dependencies: List<ModrinthDependency>?,
    @Json(name = "date_published") val datePublished: String
)

@JsonClass(generateAdapter = true)
data class ModrinthFile(
    val hashes: ModrinthHashes,
    val url: String,
    val filename: String,
    val primary: Boolean,
    val size: Long
)

@JsonClass(generateAdapter = true)
data class ModrinthHashes(
    val sha1: String,
    val sha512: String
)

@JsonClass(generateAdapter = true)
data class ModrinthDependency(
    @Json(name = "version_id") val versionId: String?,
    @Json(name = "project_id") val projectId: String?,
    @Json(name = "file_name") val fileName: String?,
    @Json(name = "dependency_type") val dependencyType: String
)

@JsonClass(generateAdapter = true)
data class ModrinthGameVersionTag(
    val version: String,
    @Json(name = "version_type") val versionType: String
)

interface ModrinthApiService {
    @GET("search")
    fun searchMods(
        @Query("query") query: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("facets") facets: String?
    ): Call<ModrinthSearchResponse>

    @GET("project/{id}")
    fun getProject(@Path("id") idOrSlug: String): Call<ModrinthProject>

    @GET("project/{id}/version")
    fun getVersions(
        @Path("id") modId: String,
        @Query("loaders") loaders: String?,
        @Query("game_versions") gameVersions: String?
    ): Call<List<ModrinthVersion>>

    @GET("version/{id}")
    fun getVersion(@Path("id") versionId: String): Call<ModrinthVersion>

    @GET("tag/game_version")
    fun getGameVersions(): Call<List<ModrinthGameVersionTag>>
}

object ModrinthRetrofitClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "ModManage-Android/1.0.0 (contact@modmanage.app)")
                .build()
            chain.proceed(request)
        }
        .build()

    val api: ModrinthApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.modrinth.com/v2/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ModrinthApiService::class.java)
    }
}
