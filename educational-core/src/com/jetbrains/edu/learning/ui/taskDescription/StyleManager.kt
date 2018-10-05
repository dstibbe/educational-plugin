package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.CommonBundle
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.FontPreferences
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionBundle.getFloatParameter
import kotlinx.css.Color
import org.jetbrains.annotations.PropertyKey
import java.util.*


class StyleManager {
  private val lafPrefix = if (UIUtil.isUnderDarcula()) "darcula" else "light"
  private val typographyManager = TypographyManager()

  val bodyFontSize = typographyManager.bodyFontSize
  val codeFontSize = typographyManager.codeFontSize
  val bodyLineHeight = typographyManager.bodyLineHeight
  val codeLineHeight = typographyManager.codeLineHeight
  val bodyFont = typographyManager.bodyFont
  val codeFont = typographyManager.codeFont

  val bodyColor = getCSSColor("$lafPrefix.body.color")
  val exampleBackground = getCSSColor("$lafPrefix.example.background")
  val linkColor = getCSSColor("$lafPrefix.link.color")
  val bodyBackground = getCSSColor("$lafPrefix.body.background")

  private fun getCSSColor(s: String): Color {
    return Color((TaskDescriptionBundle.message(s)))
  }
}

private class TypographyManager {
  private val editorFontSize = EditorColorsManager.getInstance().globalScheme.editorFontSize

  val bodyFontSize = (editorFontSize * fontScaleFactor("body.font.size")).toInt()
  val codeFontSize = (editorFontSize * fontScaleFactor("code.font.size")).toInt()
  val bodyLineHeight = (bodyFontSize * lineHeightScaleFactor("body.line.height")).toInt()
  val codeLineHeight = (codeFontSize * lineHeightScaleFactor("code.line.height")).toInt()

  val bodyFont = TaskDescriptionBundle.getOsDependentParameter("body.font")
  val codeFont = TaskDescriptionBundle.getOsDependentParameter("code.font")

  private fun fontScaleFactor(parameterName: String): Float {
    val fontSize = getFloatParameter(parameterName)
    return fontSize / FontPreferences.DEFAULT_FONT_SIZE
  }

  private fun lineHeightScaleFactor(parameterName: String): Float {
    val lineHeight = getFloatParameter(parameterName)
    val defaultValueParameterName = if (parameterName.startsWith("body")) "body.font.size" else "code.font.size"
    val fontSize = getFloatParameter(defaultValueParameterName)
    return (lineHeight / fontSize)
  }
}

private object TaskDescriptionBundle {
  const val BUNDLE_NAME = "style.browser"
  private val BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME)

  fun message(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String, vararg params: Any): String {
    return CommonBundle.message(BUNDLE, key, *params)
  }

  fun getFloatParameter(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String) = TaskDescriptionBundle.message(
    if (SystemInfo.isMac) "mac.$key" else key).toFloat()

  fun getOsDependentParameter(key: String) = TaskDescriptionBundle.message(
    parameterNameWithOSPrefix(key))

  private fun parameterNameWithOSPrefix(name: String): String {
    return when {
      SystemInfo.isMac -> "mac.$name"
      SystemInfo.isWindows -> "win.$name"
      else -> "linux.$name"
    }
  }
}