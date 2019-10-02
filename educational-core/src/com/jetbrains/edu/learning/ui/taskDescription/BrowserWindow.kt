package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduUsagesCollector
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_URL
import com.jetbrains.edu.learning.ui.taskDescription.styleManagers.StyleManager
import com.sun.webkit.dom.ElementImpl
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import org.jetbrains.annotations.TestOnly
import org.w3c.dom.Element
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.EventTarget
import java.util.*
import java.util.regex.Pattern

class BrowserWindow(private val myProject: Project, private val myLinkInNewBrowser: Boolean) {
  var panel: JFXPanel = JFXPanel()
  private lateinit var myWebComponent: WebView
  private lateinit var myPane: StackPane
  private lateinit var myEngine: WebEngine

  val engine: WebEngine
    get() = myWebComponent.engine

  init {
    Platform.runLater {
      Platform.setImplicitExit(false)
      myPane = StackPane()
      myWebComponent = WebView()
      myWebComponent.setOnDragDetected { }
      myEngine = myWebComponent.engine

      myPane.children.add(myWebComponent)
      if (myLinkInNewBrowser) {
        initHyperlinkListener()
      }
      val scene = Scene(myPane)
      panel.scene = scene
      panel.isVisible = true
      updateLaf()
    }
  }

  fun updateLaf() {
    Platform.runLater {
      val styleManager = StyleManager()
      val baseStylesheet = styleManager.baseStylesheet
      myEngine.userStyleSheetLocation = baseStylesheet
      panel.scene.stylesheets.clear()
      panel.scene.stylesheets.add(baseStylesheet)
      panel.scene.stylesheets.addAll(styleManager.scrollBarStylesheets)
      myPane.style = "-fx-background-color: ${StyleManager().bodyBackground};"
      myEngine.reload()
    }
  }

  fun loadContent(content: String) {
    StudyTaskManager.getInstance(myProject).course ?: return
    Platform.runLater { myEngine.loadContent(htmlWithResources(myProject, content)) }
  }

  private fun initHyperlinkListener() {
    myEngine.loadWorker.stateProperty().addListener { _, _, newState ->
      if (newState === Worker.State.SUCCEEDED) {
        val listener = makeHyperLinkListener()

        addListenerToAllHyperlinkItems(listener)
      }
    }
  }

  private fun addListenerToAllHyperlinkItems(listener: EventListener) {
    val doc = myEngine.document
    if (doc != null) {
      val nodeList = doc.getElementsByTagName("a")
      for (i in 0 until nodeList.length) {
        (nodeList.item(i) as EventTarget).addEventListener(EVENT_TYPE_CLICK, listener, false)
      }
    }
  }

  private fun makeHyperLinkListener(): EventListener {
    return object : EventListener {
      override fun handleEvent(ev: Event) {
        val domEventType = ev.type
        if (domEventType == EVENT_TYPE_CLICK) {
          ev.preventDefault()
          val target = ev.target as Element
          val hrefAttribute = getElementWithATag(target).getAttribute("href")

          if (hrefAttribute != null) {
            val matcher = IN_COURSE_LINK.matcher(hrefAttribute)
            if (matcher.matches()) {
              EduUsagesCollector.inCourseLinkClicked()
              var sectionName: String? = null
              val lessonName: String
              val taskName: String
              if (matcher.group(3) != null) {
                sectionName = matcher.group(1)
                lessonName = matcher.group(2)
                taskName = matcher.group(4)
              }
              else {
                lessonName = matcher.group(1)
                taskName = matcher.group(2)
              }
              NavigationUtils.navigateToTask(myProject, sectionName, lessonName, taskName)
            }
            else {
              if (hrefAttribute.startsWith(TaskDescriptionToolWindow.PSI_ELEMENT_PROTOCOL)) {
                TaskDescriptionToolWindow.navigateToPsiElement(myProject, hrefAttribute)
              }
              else {
                EduUsagesCollector.externalLinkClicked()
                myEngine.isJavaScriptEnabled = true
                myEngine.loadWorker.cancel()
                var href: String? = getLink(target) ?: return
                if (isRelativeLink(href!!)) {
                  href = STEPIK_URL + href
                }
                BrowserUtil.browse(href)
                if (href.startsWith(STEPIK_URL)) {
                  EduUsagesCollector.stepikLinkClicked()
                }
              }
            }
          }
        }
      }

      private fun isRelativeLink(href: String): Boolean {
        return !href.startsWith("http")
      }

      private fun getElementWithATag(element: Element): Element {
        var currentElement = element
        while (currentElement.tagName.toLowerCase(Locale.ENGLISH) != "a") {
          currentElement = (currentElement as ElementImpl).parentElement
        }
        return currentElement
      }

      private fun getLink(element: Element): String? {
        val href = element.getAttribute("href")
        return href ?: getLinkFromNodeWithCodeTag(element)
      }

      private fun getLinkFromNodeWithCodeTag(element: Element): String? {
        var parentNode = element.parentNode
        var attributes = parentNode.attributes
        while (attributes.length > 0 && attributes.getNamedItem("class") != null) {
          parentNode = parentNode.parentNode
          attributes = parentNode.attributes
        }
        return attributes.getNamedItem("href").nodeValue
      }
    }
  }

  companion object {
    private const val EVENT_TYPE_CLICK = "click"
    private val IN_COURSE_LINK = Pattern.compile("#(\\w+)#(\\w+)#((\\w+)#)?")

    @TestOnly
    fun processContent(content: String, project: Project): String {
      return htmlWithResources(project, content)
    }
  }
}
