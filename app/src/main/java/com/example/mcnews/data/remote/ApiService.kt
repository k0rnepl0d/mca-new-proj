package com.example.mcnews.data.remote

import com.example.mcnews.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("articles")
    suspend fun getArticles(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100,
        @Query("status") status: Int? = null,
        @Query("search") search: String? = null,
        @Query("tag_id") tagId: Int? = null
    ): List<ArticleDto>

    @GET("articles/{id}")
    suspend fun getArticle(@Path("id") id: Int): ArticleDto

    @POST("articles/")
    suspend fun createArticle(@Body dto: ArticleCreateDto): ArticleDto

    @Multipart
    @POST("articles/")
    suspend fun createArticleMultipart(
        @Part("title") title: RequestBody,
        @Part("body") body: RequestBody,
        @Part("author_id") authorId: RequestBody,
        @Part("status_id") statusId: RequestBody,
        @Part("tag_ids") tagIds: RequestBody,
        @Part image: MultipartBody.Part? = null
    ): ArticleDto

    @PUT("articles/{id}")
    suspend fun updateArticle(@Path("id") id: Int, @Body dto: ArticleUpdateDto): ArticleDto

    @Multipart
    @PUT("articles/{id}")
    suspend fun updateArticleMultipart(
        @Path("id") id: Int,
        @Part("title") title: RequestBody? = null,
        @Part("body") body: RequestBody? = null,
        @Part("status_id") statusId: RequestBody? = null,
        @Part("tag_ids") tagIds: RequestBody? = null,
        @Part image: MultipartBody.Part? = null
    ): ArticleDto

    @DELETE("articles/{id}")
    suspend fun deleteArticle(@Path("id") id: Int): Response<Unit>

    @GET("tags/")
    suspend fun getTags(): List<TagDto>

    @GET("users/")
    suspend fun getAuthors(): List<AuthorDto>

    // User profile endpoints
    @GET("users/me")
    suspend fun getCurrentUser(): UserDto

    @Multipart
    @PUT("users/me")
    suspend fun updateProfile(
        @Part("first_name") firstName: RequestBody? = null,
        @Part("last_name") lastName: RequestBody? = null,
        @Part("middle_name") middleName: RequestBody? = null,
        @Part("email") email: RequestBody? = null,
        @Part photo: MultipartBody.Part? = null
    ): UserDto

    @FormUrlEncoded
    @PUT("users/me/password")
    suspend fun changePassword(
        @Field("old") oldPassword: String,
        @Field("new") newPassword: String
    ): Response<Unit>

    @GET("users/me/pdf")
    suspend fun getProfilePdf(): Response<ResponseBody>
}