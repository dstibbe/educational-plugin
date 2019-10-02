package com.jetbrains.edu.java.stepik.hyperskill

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.java.JTaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.gradle.GradleConfiguratorBase
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillTopic
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView
import com.jetbrains.edu.learning.ui.taskDescription.check.CheckDetailsPanel
import com.jetbrains.edu.learning.ui.taskDescription.createTextPane
import javax.swing.JPanel

class JHyperskillConfigurator : GradleConfiguratorBase() {

  override fun getCourseBuilder() = JHyperskillCourseBuilder()
  override fun getTestFileName() = ""
  override fun getTaskCheckerProvider() = JTaskCheckerProvider()
  override fun isCourseCreatorEnabled() = false

  override fun additionalTaskTab(currentTask: Task?, project: Project): Pair<JPanel, String>? {
    if (currentTask == null) return null

    val course = currentTask.lesson.course
    if (course is HyperskillCourse) {
      val topicsPanel = JPanel(VerticalFlowLayout())
      topicsPanel.background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
      topicsPanel.border = JBUI.Borders.empty(8, 16, 0, 0)
      val textPane = createTextPane()
      textPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
      val topics = course.taskToTopics[currentTask.index]
      var descriptionText = "<h3>Topics for current stage :</h3>"
      if (topics != null) {
        for (topic in topics) {
          descriptionText += topicLink(topic)
          descriptionText += "<br>"
        }
      }
      else {
        descriptionText += "No topics found for current stage."
      }
      textPane.text = descriptionText
      topicsPanel.add(textPane)
      topicsPanel.add(CheckDetailsPanel.LightColoredActionLink(
        "Back to the task description",
        CheckDetailsPanel.SwitchTaskTabAction(project, 0),
        AllIcons.Actions.Back))

      return Pair(topicsPanel, "Topics")
    }
    return null
  }

  private fun topicLink(topic: HyperskillTopic): String = "<a style=\"color:$linkColor\" href=\"https://hyperskill.org/learn/topic/${topic.id}/\">${topic.title}</a>"
  private val linkColor: String = if (UIUtil.isUnderDarcula()) "#6894C6" else "#5C84C9"
}
