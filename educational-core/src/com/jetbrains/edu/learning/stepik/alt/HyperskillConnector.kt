package com.jetbrains.edu.learning.stepik.alt

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.checkio.notifications.CheckiONotification
import org.jetbrains.ide.BuiltInServerManager
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.IOException
import java.net.URISyntaxException

object HyperskillConnector {
  private val LOG = Logger.getInstance(HyperskillConnector::class.java.name)
  private const val HYPERSKILL_URL = "https://hyperskill.org/api/"
  private val port = BuiltInServerManager.getInstance().port
  private val redirectUri = "http://localhost:$port/hyperskill/oauth"

  var clientId = "jcboczaSZYHmmCewusCNrE172yHkOONV7JY1ECh4"
  private val authorizationCodeUrl = "https://hyperskill.org/oauth2/authorize/?" +
                                     "client_id=$clientId&redirect_uri=$redirectUri&response_type=code"

  private val service: HyperskillService
    get() {
//      val okHttpClient = OkHttpClient.Builder()
//        .readTimeout(60, TimeUnit.SECONDS)
//        .connectTimeout(60, TimeUnit.SECONDS)
//        .build()

      val retrofit = Retrofit.Builder()
        .baseUrl(HYPERSKILL_URL)
        .addConverterFactory(JacksonConverterFactory.create())
//        .client(okHttpClient)
        .build()

      return retrofit.create(HyperskillService::class.java)
    }

  fun login(account: HyperskillAccount) {
    try {
      val loginBody = LoginBody(account)
      val response = service.login(loginBody).execute()
      val tokenInfo = response.body()
      if (tokenInfo != null) {
        account.tokenInfo = tokenInfo
      }
      if (response.errorBody() != null) {
        LOG.warn("Failed to login to hyperskill.org. " + response.errorBody()!!.string())
      }
    }
    catch (e: IOException) {
      LOG.warn("Failed to login to hyperskill.org. " + e.message)
    }

  }

  fun doAuthorize(vararg postLoginActions: Runnable) {
//    requireClientPropertiesExist()

    try {
//      createAuthorizationListener(*postLoginActions)
      BrowserUtil.browse(authorizationCodeUrl)
    }
    catch (e: URISyntaxException) {
      // IOException is thrown when there're no available ports, in some cases restarting can fix this
      Notifications.Bus.notify(CheckiONotification.Error(
        "Authorization failed",
        null,
        "Try to restart IDE and log in again", null
      ))
    }
    catch (e: IOException) {
      Notifications.Bus.notify(CheckiONotification.Error("Authorization failed", null, "Try to restart IDE and log in again", null))
    }

  }
}
