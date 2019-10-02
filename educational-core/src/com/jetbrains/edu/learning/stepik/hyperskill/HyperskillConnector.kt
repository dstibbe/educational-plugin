package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit

object HyperskillConnector {
  private val LOG = Logger.getInstance(HyperskillConnector::class.java)

  private var authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()
  private val authorizationTopic = com.intellij.util.messages.Topic.create<HyperskillLoggedIn>("Edu.hyperskillLoggedIn",
                                                                                               HyperskillLoggedIn::class.java)

  private val authorizationService: HyperskillService
    get() {
      val retrofit = Retrofit.Builder()
        .baseUrl(HYPERSKILL_URL)
        .addConverterFactory(JacksonConverterFactory.create())
        .build()

      return retrofit.create(HyperskillService::class.java)
    }

  private val service: HyperskillService
    get() {
      val account = HyperskillSettings.INSTANCE.account
      if (account != null && !account.tokenInfo.isUpToDate()) {
        account.refreshTokens()
      }

      val dispatcher = Dispatcher()
      dispatcher.maxRequests = 10

      val okHttpClient = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
          val tokenInfo = account?.tokenInfo
          if (tokenInfo == null) return@addInterceptor chain.proceed(chain.request())

          val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${tokenInfo.accessToken}")
            .build()
          chain.proceed(newRequest)
        }
        .dispatcher(dispatcher)
        .build()
      val retrofit = Retrofit.Builder()
        .baseUrl(HYPERSKILL_URL)
        .addConverterFactory(JacksonConverterFactory.create())
        .client(okHttpClient)
        .build()

      return retrofit.create(HyperskillService::class.java)
    }

  fun doAuthorize(vararg postLoginActions: Runnable) {
    createAuthorizationListener(*postLoginActions)
    BrowserUtil.browse(AUTHORISATION_CODE_URL)
  }

  fun login(code: String): Boolean {
    val tokenInfo = authorizationService.getTokens(CLIENT_ID, REDIRECT_URI, code, "authorization_code").execute().body() ?: return false
    HyperskillSettings.INSTANCE.account = HyperskillAccount()
    HyperskillSettings.INSTANCE.account!!.tokenInfo = tokenInfo
    val currentUser = getCurrentUser() ?: return false
    HyperskillSettings.INSTANCE.account!!.userInfo = currentUser
    ApplicationManager.getApplication().messageBus.syncPublisher<HyperskillLoggedIn>(authorizationTopic).userLoggedIn()
    return true
  }

  private fun HyperskillAccount.refreshTokens() {
    val refreshToken = tokenInfo.refreshToken
    val tokens = authorizationService.refreshTokens("refresh_token", CLIENT_ID, refreshToken).execute().body()
    if (tokens != null) {
      updateTokens(tokens)
    }
  }

  fun getCurrentUser(): HyperskillUserInfo? {
    return service.getUserInfo(0).execute().body()?.users?.first() ?: return null
  }

  fun getStages(projectId: Int): List<HyperskillStage>? {
    return service.stages(projectId).execute().body()?.stages
  }

  fun fillTopics(course: HyperskillCourse, project: Project) {
    for ((taskIndex, stage) in course.stages.withIndex()) {
      val call = service.topics(stage.id)
      call.enqueue(object: Callback<TopicsData> {
        override fun onFailure(call: Call<TopicsData>, t: Throwable) {
          LOG.warn("Failed to get topics for stage ${stage.id}")
        }

        override fun onResponse(call: Call<TopicsData>, response: Response<TopicsData>) {
          val topics = response.body()?.topics?.filter { it.children.isEmpty() }
          if (topics != null && topics.isNotEmpty()) {
            course.taskToTopics[taskIndex] = topics
            runInEdt {
              TaskDescriptionView.getInstance(project).updateAdditionalTaskTab()
            }
          }
        }
      })
    }
  }

  private fun createAuthorizationListener(vararg postLoginActions: Runnable) {
    authorizationBusConnection.disconnect()
    authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()
    authorizationBusConnection.subscribe(authorizationTopic, object : HyperskillLoggedIn {
      override fun userLoggedIn() {
        for (action in postLoginActions) {
          action.run()
        }
      }
    })
  }

  private interface HyperskillLoggedIn {
    fun userLoggedIn()
  }
}
