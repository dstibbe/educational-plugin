package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.CommonBundle
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.FontPreferences
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduLanguageDecorator
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionBundle.getFloatParameter
import kotlinx.css.*
import kotlinx.css.properties.lh
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
  val linkColor = getCSSColor("$lafPrefix.link.color")
  val bodyBackground = getCSSColor("$lafPrefix.body.background")
  val codeBackground = if (EduSettings.getInstance().shouldUseJavaFx()) bodyBackground
  else Color("#${ColorUtil.toHex(ColorUtil.dimmer(UIUtil.getPanelBackground()))}")

  val scrollBarStylesheets = getScrollBarStylesheetsUrls()
  val baseStylesheet = resourceUrl("/style/browser.css")
  val buttonStylesheets = listOfNotNull(baseStylesheet,
                                        resourceUrl("/style/javafxButtons/buttonsBase.css"),
                                        resourceUrl("/style/javafxButtons/buttonsDarcula.css").takeIf { UIUtil.isUnderDarcula() })

  fun resources(project: Project, content: String) = StyleResourcesManager(project, content).resources

  private fun getScrollBarStylesheetsUrls(): List<String> {
    return listOf(resourceUrl("/style/scrollbars/base.css"),
                  if (SystemInfo.isWindows) resourceUrl("/style/scrollbars/winShape.css")
                  else resourceUrl("/style/scrollbars/macLinuxShape.css"),
                  if (UIUtil.isUnderDarcula()) resourceUrl("/style/scrollbars/darcula.css")
                  else resourceUrl("/style/scrollbars/light.css"))
  }

  private fun getCSSColor(s: String): Color {
    return Color((TaskDescriptionBundle.message(s)))
  }

  companion object {
    internal val LOG = Logger.getInstance(this::class.java)
  }
}

private class TypographyManager {
  private val editorFontSize = EditorColorsManager.getInstance().globalScheme.editorFontSize

  val bodyFontSize = (editorFontSize * fontScaleFactor("body.font.size")).toInt()
  val codeFontSize = (editorFontSize * fontScaleFactor("code.font.size")).toInt()
  val bodyLineHeight = (bodyFontSize * lineHeightScaleFactor("body.line.height")).toInt()
  val codeLineHeight = (codeFontSize * lineHeightScaleFactor("code.line.height")).toInt()

  val bodyFont = TaskDescriptionBundle.getOsDependentParameter(if (EduSettings.getInstance().shouldUseJavaFx()) "body.font" else "swing.body.font")
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

  fun getOsDependentParameter(key: String) = TaskDescriptionBundle.message(parameterNameWithOSPrefix(key))

  private fun parameterNameWithOSPrefix(name: String): String {
    return when {
      SystemInfo.isMac -> "mac.$name"
      SystemInfo.isWindows -> "win.$name"
      else -> "linux.$name"
    }
  }
}

class StyleResourcesManager(project: Project, taskText: String) {
  private fun decorator(project: Project): EduLanguageDecorator = EduLanguageDecorator.INSTANCE.forLanguage(
    StudyTaskManager.getInstance(project).course?.languageById ?: PlainTextLanguage.INSTANCE)

  // update style/template.html.ft in case of changing key names
  val resources = mapOf(
    "typography_color_style" to typographyAndColorStylesheet(),
    "language_script" to decorator(project).languageScriptUrl,
    "content" to taskText,
    "highlight_code" to highlightScript(project),
    "base_css" to loadText("/style/browser.css"),
    resourcePair("codemirror", "/code-mirror/codemirror.js"),
    resourcePair("jquery", ("/style/hint/jquery-1.9.1.js")),
    resourcePair("runmode", "/code-mirror/runmode.js"),
    resourcePair("colorize", "/code-mirror/colorize.js"),
    resourcePair("javascript", "/code-mirror/javascript.js"),
    resourcePair("hint_base", "/style/hint/base.css"),
    resourcePair("hint_laf_specific", if (UIUtil.isUnderDarcula()) "/style/hint/darcula.css" else "/style/hint/light.css"),
    resourcePair("css_oldcodemirror", if (UIUtil.isUnderDarcula()) "/code-mirror/codemirror-old-darcula.css" else "/code-mirror/codemirror-old.css"),
    resourcePair("css_codemirror", if (UIUtil.isUnderDarcula()) "/code-mirror/codemirror-darcula.css" else "/code-mirror/codemirror.css"),
    resourcePair("toggle_hint_script", "/style/hint/toggleHint.js"),
    resourcePair("mathjax_script", "/style/mathjaxConfigure.js"),
    resourcePair("stepik_link", "/style/stepikLink.css")
  )

  private fun resourcePair(name: String, path: String) = name to resourceUrl(path)

  private fun typographyAndColorStylesheet(): String {
    val styleManager = StyleManager()
    return CSSBuilder().apply {
      body {
        fontFamily = styleManager.bodyFont
        fontSize = if (EduSettings.getInstance().shouldUseJavaFx()) styleManager.bodyFontSize.px else styleManager.bodyFontSize.pt
        lineHeight = styleManager.bodyLineHeight.px.lh
        color = styleManager.bodyColor
        backgroundColor = styleManager.bodyBackground
      }

      code {
        fontFamily = styleManager.codeFont
        backgroundColor = styleManager.codeBackground
      }

      "pre code" {
        fontSize = if (EduSettings.getInstance().shouldUseJavaFx()) styleManager.codeFontSize.px else styleManager.codeFontSize.pt
        lineHeight = styleManager.codeLineHeight.px.lh
      }

      a {
        color = styleManager.linkColor
      }
    }.toString()
  }

  private fun highlightScript(project: Project): String {
    val loadText = loadText("/code-mirror/highlightCode.js.ft")
    return loadText?.replace("\${default_mode}", decorator(project).defaultHighlightingMode) ?: ""
  }
}

private fun resourceUrl(name: String): String {
  val resource = object {}.javaClass.getResource(name)?.toExternalForm()
  return if (resource != null) {
    resource
  }
  else {
    StyleManager.LOG.warn("Cannot find resource: $name")
    ""
  }
}