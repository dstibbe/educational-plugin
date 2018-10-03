package com.jetbrains.edu.learning.stepik.alt

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import retrofit2.Call
import retrofit2.http.*
import java.util.*

@Suppress("unused")
interface HyperskillService {

  @POST("login/")
  fun login(@Body body: LoginBody): Call<TokenInfo>

  @GET("recommendations/")
  fun recommendations(): Call<Recommendation>

  @FormUrlEncoded
  @POST("oauth/token/")
  fun getTokens(
    @Field("grant_type") grantType: String,
    @Field("client_secret") clientSecret: String,
    @Field("client_id") clientId: String,
    @Field("code") code: String,
    @Field("redirect_uri") redirectUri: String
  ): Call<TokenInfo>

  @FormUrlEncoded
  @POST("oauth/token/")
  fun refreshTokens(
    @Field("grant_type") grantType: String,
    @Field("client_secret") clientSecret: String,
    @Field("client_id") clientId: String,
    @Field("refresh_token") refreshToken: String
  ): Call<TokenInfo>

  @GET("oauth/information/")
  fun getUserInfo(@Query("access_token") accessToken: String): Call<HyperskillUserInfo>
}

class LoginBody(account: HyperskillAccount) {
  @JsonProperty("stepik_id")
  var stepikId: Int = 0

  @JsonProperty("access_token")
  var accessToken: String

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  @JsonProperty("expires_in")
  var expiresIn: Date

  @JsonProperty("refresh_token")
  var refreshToken: String

  @JsonProperty("client_id")
  var clientId = "jcboczaSZYHmmCewusCNrE172yHkOONV7JY1ECh4"

  init {
    stepikId = account.userInfo?.id  ?: -1
    accessToken = account.tokenInfo?.accessToken ?: ""
    refreshToken = account.tokenInfo?.refreshToken ?: ""
    expiresIn = account.tokenInfo?.expiresIn ?: Date(0) //"2018-10-11T10:00"
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TokenInfo {
  @JsonProperty("access_token")
  var accessToken: String = ""
  @JsonProperty("refresh_token")
  var refreshToken: String = ""
  @JsonProperty("expires_in")
  var expiresIn: Date = Date(0)
}


@JsonIgnoreProperties(ignoreUnknown = true)
class Recommendation {
  var lesson: Lesson? = null
}

class Lesson {
  @JsonProperty("stepik_id")
  var stepikId: Int = 0
}
