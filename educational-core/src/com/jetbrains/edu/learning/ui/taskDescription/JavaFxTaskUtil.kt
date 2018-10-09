@file:JvmName("JavaFxTaskUtil")
package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.ide.BrowserUtil
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduLanguageDecorator
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import kotlinx.css.*
import kotlinx.css.properties.lh
import org.apache.commons.lang.text.StrSubstitutor
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import java.util.*

private const val LEFT_INSET = 3.0
private const val RIGHT_INSET = 10.0
private const val TOP_INSET = 15.0
private const val BOTTOM_INSET = 10.0

const val MULTIPLE_CHOICE_LABEL = "Select one or more options from the list:"
const val SINGLE_CHOICE_LABEL = "Select one option from the list:"

fun Task.createScene(): Scene? {
  val choiceTask = this as? ChoiceTask ?: return null
  return choiceTask.createScene()
}

fun ChoiceTask.createScene(): Scene {
  val group = Group()
  val scene = Scene(group, getSceneBackground())

  val vBox = VBox()
  vBox.spacing = JBUI.scale(10).toDouble()
  vBox.padding = Insets(TOP_INSET, RIGHT_INSET, BOTTOM_INSET, LEFT_INSET)
  if (this.isMultipleChoice) {
    val text = createLabel(MULTIPLE_CHOICE_LABEL)

    vBox.children.add(text)
    for ((index, variant) in this.choiceVariants.withIndex()) {
      val checkBox = createCheckbox(variant, index, this)
      vBox.children.add(checkBox)
    }
  }
  else {
    val toggleGroup = ToggleGroup()
    val text = createLabel(SINGLE_CHOICE_LABEL)
    vBox.children.add(text)
    for ((index, variant) in this.choiceVariants.withIndex()) {
      val radioButton = createRadioButton(variant, index, toggleGroup, this)
      vBox.children.add(radioButton)
    }
  }
  group.children.add(vBox)

  LafManager.getInstance().addLafManagerListener(StudyLafManagerListener(scene))
  return scene
}

private fun createSelectionListener(task: ChoiceTask, index: Int): (ObservableValue<out Boolean>, Boolean, Boolean) -> Unit {
  return { _, _, isSelected ->
    if (isSelected) {
      task.selectedVariants.add(index)
    }
    else {
      task.selectedVariants.remove(index)
    }
  }
}

private fun createLabel(text: String): Label {
  val textLabel = Label(text)
  setUpLabelStyle(textLabel)
  return textLabel
}

private fun createCheckbox(variant: String, index: Int, task: ChoiceTask): CheckBox {
  val checkBox = CheckBox(variant)
  setUpButtonStyle(checkBox)
  checkBox.isMnemonicParsing = false
  checkBox.isSelected = task.selectedVariants.contains(index)
  checkBox.selectedProperty().addListener(createSelectionListener(task, index))
  return checkBox
}

private fun createRadioButton(variant: String, index: Int, toggleGroup: ToggleGroup, task: ChoiceTask): RadioButton {
  val isSelected = task.selectedVariants.contains(index)
  val radioButton = RadioButton(variant)
  setUpButtonStyle(radioButton)
  radioButton.toggleGroup = toggleGroup
  radioButton.isSelected = isSelected
  radioButton.selectedProperty().addListener(createSelectionListener(task, index))
  return radioButton
}

private fun getSceneBackground(): Color {
  val isDarcula = LafManager.getInstance().currentLookAndFeel is DarculaLookAndFeelInfo
  val panelBackground = if (isDarcula) UIUtil.getPanelBackground() else UIUtil.getTextFieldBackground()
  return Color.rgb(panelBackground.red, panelBackground.green, panelBackground.blue)
}

private fun setUpLabelStyle(node: Label) {
  node.stylesheets.add(StyleManager().baseStylesheet)
  node.font = Font(StyleManager().bodyFont, getFontSize())
  val labelForeground = UIUtil.getLabelForeground()
  node.textFill = Color.rgb(labelForeground.red, labelForeground.green, labelForeground.blue)
}

private fun getFontSize() = (EditorColorsManager.getInstance().globalScheme.editorFontSize + 1).toDouble()

private fun setUpButtonStyle(button: ButtonBase) {
  button.isWrapText = true
  button.font = Font.font(getFontSize())
  setButtonLaf(button)
}

fun Scene.updateLaf() {
  Platform.runLater {
    val panelBackground = UIUtil.getPanelBackground()
    val root = this.root
    this.fill = Color.rgb(panelBackground.red, panelBackground.green, panelBackground.blue)
    for (node in getAllNodes(root)) {
      (node as? ButtonBase)?.let { setButtonLaf(it) }
      (node as? Label)?.let { setUpLabelStyle(it) }
    }
  }
}

fun getAllNodes(root: Parent): ArrayList<Node> {
  val nodes = ArrayList<Node>()
  addAllDescendants(root, nodes)
  return nodes
}

private fun addAllDescendants(parent: Parent, nodes: ArrayList<Node>) {
  for (node in parent.childrenUnmodifiable) {
    nodes.add(node)
    (node as? Parent)?.let { addAllDescendants(it, nodes) }
  }
}

fun setButtonLaf(button: ButtonBase) {
  button.stylesheets.removeAll()
  button.stylesheets.addAll(StyleManager().buttonStylesheets)
}

private class StudyLafManagerListener(val scene: Scene) : LafManagerListener {
  override fun lookAndFeelChanged(manager: LafManager) {
    scene.updateLaf()
  }
}

class TaskDescriptionHtml(private val myProject: Project, course: Course, val taskText: String) {
  private val SRC_ATTRIBUTE = "src"
  private val LOG = Logger.getInstance(this::class.java)

  private val decorator: EduLanguageDecorator = EduLanguageDecorator.INSTANCE.forLanguage(course.languageById)

  // update style/template.html.ft in case of modifying
  private fun variables() = mapOf(
    "typography_color_style" to typographyAndColorStylesheet(),
    "language_script" to decorator.languageScriptUrl,
    "content" to taskText,
    "highlight_code" to highlightScript()
  )

  // update style/template.html.ft in case of modifying
  private fun resources() = mapOf(
    "codemirror" to getResourcePath("/code-mirror/codemirror.js"),
    "jquery" to getResourcePath("/style/hint/jquery-1.9.1.js"),
    "runmode" to getResourcePath("/code-mirror/runmode.js"),
    "colorize" to getResourcePath("/code-mirror/colorize.js"),
    "javascript" to getResourcePath("/code-mirror/javascript.js"),
    "hint_base" to getResourcePath("/style/hint/base.css"),
    "hint_laf_specific" to getResourcePath(
      if (UIUtil.isUnderDarcula()) "/style/hint/darcula.css" else "/style/hint/light.css"),
    "css_oldcodemirror" to getResourcePath(
      if (UIUtil.isUnderDarcula()) "/code-mirror/codemirror-old-darcula.css" else "/code-mirror/codemirror-old.css"),
    "css_codemirror" to getResourcePath(
      if (UIUtil.isUnderDarcula()) "/code-mirror/codemirror-darcula.css" else "/code-mirror/codemirror.css"),
    "toggle_hint_script" to getResourcePath("/style/hint/toggleHint.js"),
    "mathjax_script" to getResourcePath("/style/mathjaxConfigure.js"),
    "stepik_link" to getResourcePath("/style/stepikLink.css"),
    "highlight_code" to getResourcePath("/code-mirror/highlightCode.js.ft")
  )

  fun html(): String {
    val templateText = loadText("/style/template.html.ft")
    val templateWithVariables = StrSubstitutor(variables()).replace(templateText)

    val styledText = StrSubstitutor(resources()).replace(templateWithVariables)
    return absolutizeImgPaths(styledText)
  }

  private fun typographyAndColorStylesheet(): String {
    val styleManager = StyleManager()
    return CSSBuilder().apply {
      body {
        fontFamily = styleManager.bodyFont
        fontSize = styleManager.bodyFontSize.px
        lineHeight = styleManager.bodyLineHeight.px.lh
        color = styleManager.bodyColor
        backgroundColor = styleManager.bodyBackground
      }

      code {
        fontFamily = styleManager.codeFont
      }

      "pre code" {
        fontSize = styleManager.codeFontSize.px
        lineHeight = styleManager.codeLineHeight.px.lh
      }

      ".example" {
        backgroundColor = styleManager.exampleBackground
      }

      a {
        color = styleManager.linkColor
      }
    }.toString()
  }

  private fun highlightScript(): String {
    val loadText = loadText("/code-mirror/highlightCode.js.ft")
    return loadText?.replace("\${default_mode}", decorator.defaultHighlightingMode) ?: "";
  }

  private fun loadText(filePath: String): String? {
    var template: String? = null
    val classLoader = this::class.java.classLoader
    val stream = classLoader.getResourceAsStream(filePath)
    try {
      template = StreamUtil.readText(stream, "utf-8")
    }
    catch (e: IOException) {
      LOG.warn(e.message)
    }
    finally {
      try {
        stream.close()
      }
      catch (e: IOException) {
        LOG.warn(e.message)
      }
    }
    return template
  }

  private fun getResourcePath(recoursePath: String): String {
    val classLoader = this::class.java.classLoader
    val resource = classLoader.getResource(recoursePath)
    if (resource != null) {
      return resource.toExternalForm()
    }
    else {
      LOG.warn("Resource not found: $recoursePath")
    }
    return ""
  }

  private fun absolutizeImgPaths(content: String): String {
    val task = EduUtils.getCurrentTask(myProject)
    if (task == null) {
      return content
    }

    val taskDir = task.getTaskDir(myProject)
    if (taskDir == null) {
      return content
    }

    val document = Jsoup.parse(content)
    val imageElements = document.getElementsByTag("img")
    for (imageElement in imageElements) {
      val imagePath = imageElement.attr(SRC_ATTRIBUTE)
      if (!BrowserUtil.isAbsoluteURL(imagePath)) {
        val file = File(imagePath)
        val absolutePath = File(taskDir.path, file.path).toURI().toString()
        imageElement.attr("src", absolutePath)
      }
    }
    return document.outerHtml()
  }
}
