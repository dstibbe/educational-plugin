package com.jetbrains.edu.learning.authUtils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.IntNode
import com.intellij.util.ReflectionUtil
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element

// Base class for oauth-based accounts
// All user-specific information should be stored in userInfo
abstract class OAuthAccount<UserInfo : Any> {
  var tokenInfo: TokenInfo = TokenInfo()
  lateinit var userInfo: UserInfo

  constructor()

  constructor(tokenInfo: TokenInfo) {
    this.tokenInfo = tokenInfo
  }

  fun updateTokens(tokenInfo: TokenInfo) {
    this.tokenInfo = tokenInfo
  }

  fun serialize(): Element {
    val accountClass = this.javaClass
    val accountElement = Element(accountClass.simpleName)

    XmlSerializer.serializeInto(tokenInfo, accountElement)
    XmlSerializer.serializeInto(userInfo, accountElement)

    return accountElement
  }
}

fun <Account : OAuthAccount<UserInfo>, UserInfo : Any> deserializeAccount(
  xmlAccount: Element,
  accountClass: Class<Account>,
  userInfoClass: Class<UserInfo>): Account {

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
  @JsonDeserialize(using = ExpiresDeserializer::class)
  var expiresIn: Long = -1

  fun isUpToDate(): Boolean {
    return currentTimeSeconds() < expiresIn - 600 // subtract 10 minutes to avoid boundary case
  }

  private fun currentTimeSeconds(): Long {
    return System.currentTimeMillis() / 1000
  }
}

class ExpiresDeserializer : StdDeserializer<Int>(Int::class.java) {
  override fun deserialize(parser: JsonParser, ctxt: DeserializationContext?): Int {
    val expiresInNode: IntNode = parser.codec.readTree(parser)
    val currentTime: Int = (System.currentTimeMillis() / 1000).toInt()
    val expiresIn = expiresInNode.numberValue() as Int
    return expiresIn + currentTime
  }
}