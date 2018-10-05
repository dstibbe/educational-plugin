package com.jetbrains.edu.learning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.util.ReflectionUtil
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element

abstract class OauthAccount<T : Any> {
  var tokenInfo: TokenInfo = TokenInfo()
  lateinit var userInfo: T

  constructor()

  constructor(tokenInfo: TokenInfo) {
    this.tokenInfo = tokenInfo
  }

  fun updateTokens(tokenInfo: TokenInfo) {
    this.tokenInfo = tokenInfo
  }

  fun serialize() : Element {
    val accountClass = this.javaClass
    val accountElement = Element(accountClass.simpleName)

    XmlSerializer.serializeInto(tokenInfo, accountElement)
    XmlSerializer.serializeInto(userInfo, accountElement)

    return accountElement
  }
}

fun <Account: OauthAccount<UserInfo>, UserInfo: Any> deserializeAccount(
  xmlAccount: Element,
  accountClass: Class<Account>,
  userInfoClass: Class<UserInfo> ) : Account {

  val account = ReflectionUtil.newInstance(accountClass)

  val tokenInfo = TokenInfo()
  XmlSerializer.deserializeInto(tokenInfo, xmlAccount)

  val userInfo = ReflectionUtil.newInstance(userInfoClass)
  XmlSerializer.deserializeInto(userInfo, xmlAccount)

  account.userInfo = userInfo
  account.tokenInfo = tokenInfo
  return account
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TokenInfo {
  @JsonProperty("access_token")
  var accessToken: String = ""
  @JsonProperty("refresh_token")
  var refreshToken: String = ""
  @JsonProperty("expires_in")
  var expiresIn: Long = -1
}
