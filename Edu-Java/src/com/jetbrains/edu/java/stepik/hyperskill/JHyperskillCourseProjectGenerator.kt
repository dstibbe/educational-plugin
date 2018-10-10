package com.jetbrains.edu.java.stepik.hyperskill

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillSettings
import com.jetbrains.edu.learning.stepik.hyperskill.getLesson

class JHyperskillCourseProjectGenerator(builder: GradleCourseBuilderBase,
                                        course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun beforeProjectGenerated(): Boolean {
    return try {
      val language = myCourse.languageById
      val userInfo = HyperskillSettings.INSTANCE.account?.userInfo ?: return false
      val lessonId = userInfo.hyperskillProject?.lesson ?: return false
      val projectId = myCourse.id

      val stages = HyperskillConnector.getStages(projectId) ?: return false
      val lesson = getLesson(lessonId, language, stages) ?: return false
      lesson.name = userInfo.hyperskillProject?.title?.removePrefix(PROJECT_PREFIX)

      myCourse.addLesson(FrameworkLesson(lesson))
      true
    }
    catch (e: Exception) {
      LOG.warn(e)
      false
    }
  }

  companion object {
    private val LOG = Logger.getInstance(JHyperskillCourseProjectGenerator::class.java)
  }
}
