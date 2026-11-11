package com.hhpc.smarttask.data.api

import com.hhpc.smarttask.data.model.UserPreferenceDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserPreferenceApi {
    @GET("preferences")
    suspend fun getPreferences(): Response<UserPreferenceDto>

    @PUT("preferences")
    suspend fun updatePreferences(
        @Body preference: UserPreferenceDto
    ): Response<UserPreferenceDto>
}
