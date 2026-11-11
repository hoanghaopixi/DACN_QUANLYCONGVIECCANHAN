package com.hhpc.smarttask.data.api

import com.hhpc.smarttask.data.model.Category
import retrofit2.Response
import retrofit2.http.*

interface CategoryApi {
    @GET("categories")
    suspend fun getCategories(@Query("workspaceId") workspaceId: Long? = null): Response<List<Category>>

    @POST("categories")
    suspend fun createCategory(
        @Body category: Category,
        @Query("workspaceId") workspaceId: Long? = null
    ): Response<Category>

    @PUT("categories/{id}")
    suspend fun updateCategory(@Path("id") id: Long, @Body category: Category): Response<Category>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long): Response<Unit>
}
