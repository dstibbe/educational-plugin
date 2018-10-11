package com.jetbrains.edu.learning.stepik.hyperskill

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.authUtils.OAuthAccount

class HyperskillAccount : OAuthAccount<HyperskillUserInfo>()

@JsonIgnoreProperties(ignoreUnknown = true)
class HyperskillUserInfo {
  var id: Int = -1
  var email: String = ""
  var fullname: String = ""
  var stage: HyperskillStage? = null
  @JsonProperty("project")
  var hyperskillProject: HyperskillProject? = null

  override fun toString(): String {
    return fullname
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class HyperskillStage(var id: Int = -1, var title: String = "")

@JsonIgnoreProperties(ignoreUnknown = true)
data class HyperskillProject(var id: Int = -1, var title: String = "", var description: String = "",
                             var lesson: Int = -1, var repo_url: String = "")