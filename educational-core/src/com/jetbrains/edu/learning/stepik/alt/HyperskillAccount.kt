package com.jetbrains.edu.learning.stepik.alt

import java.util.*

class HyperskillAccount {
  var tokenInfo: TokenInfo? = null
  var userInfo: HyperskillUserInfo? = null

  // used for deserialization
  private constructor() {}

  constructor(userInfo: HyperskillUserInfo, tokens: TokenInfo) {
    this.userInfo = userInfo
    tokenInfo = tokens
  }

  fun updateTokens(newTokens: TokenInfo) {
    tokenInfo = newTokens
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val account = other as HyperskillAccount?
    return userInfo == account!!.userInfo
  }

  override fun hashCode(): Int {
    return Objects.hash(userInfo)
  }
}

data class HyperskillUserInfo(var id: Int = -1, var email: String = "")