package com.jetbrains.edu.coursecreator.configuration

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask


class YamlDeserializationTest: EduTestCase() {
  fun `test course`() {
    val name = "Test Course"
    val language = "Russian"
    val programmingLanguage = "Plain text"
    val firstLesson = "the first lesson"
    val secondLesson = "the second lesson"
    val yamlContent = """
      |title: $name
      |language: $language
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: $programmingLanguage
      |content:
      |- $firstLesson
      |- $secondLesson
      |""".trimMargin("|")
    val course = YamlFormatSynchronizer.MAPPER.readValue(yamlContent, Course::class.java)
    assertEquals(name, course.name)
    assertEquals(language, course.humanLanguage)
    assertEquals(programmingLanguage, course.languageById.displayName)
    assertNotNull(course.description)
    assertEquals(listOf(firstLesson, secondLesson), course.items.map { it.name })
  }

  fun `test section`() {
    val firstLesson = "Introduction Lesson"
    val secondLesson = "Advanced Lesson"
    val yamlContent = """
      |content:
      |- $firstLesson
      |- $secondLesson
    """.trimMargin("|")
    val section = YamlFormatSynchronizer.MAPPER.readValue(yamlContent, Section::class.java)
    assertEquals(listOf(firstLesson, secondLesson), section.items.map { it.name })
  }

  fun `test lesson`() {
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val yamlContent = """
      |content:
      |- $firstTask
      |- $secondTask
    """.trimMargin("|")
    val lesson = YamlFormatSynchronizer.MAPPER.readValue(yamlContent, Lesson::class.java)
    assertEquals(listOf(firstTask, secondTask), lesson.getTaskList().map { it.name })
  }

  fun `test output task`() {
    val yamlContent = """
    |type: output
    |task_files:
    |- name: Test.java
    |""".trimMargin("|")
    val task = YamlFormatSynchronizer.deserializeTask(yamlContent)
    assertTrue(task is OutputTask)
    assertEquals(listOf("Test.java"), task.taskFiles.map { it.key })
  }

  fun `test edu task`() {
    val yamlContent = """
    |type: edu
    |task_files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |    dependency:
    |      lesson: lesson1
    |      task: task1
    |      file: Test.java
    |      placeholder: 1
    |      is_visible: true
    |""".trimMargin("|")
    val task = YamlFormatSynchronizer.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val answerPlaceholder = task.taskFiles["Test.java"]!!.answerPlaceholders[0]
    assertEquals(9, answerPlaceholder.length)
    assertEquals(3, answerPlaceholder.possibleAnswer.length)
    assertEquals("lesson1#task1#Test.java#1", answerPlaceholder.placeholderDependency.toString())
  }

  fun `test edu task without dependency`() {
    val yamlContent = """
    |type: edu
    |task_files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |""".trimMargin("|")
    val task = YamlFormatSynchronizer.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val answerPlaceholder = task.taskFiles["Test.java"]!!.answerPlaceholders[0]
    assertEquals(9, answerPlaceholder.length)
    assertEquals(3, answerPlaceholder.possibleAnswer.length)
  }

  fun `test feedback link`() {
    val yamlContent = """
    |type: edu
    |feedback_link: http://example.com
    |task_files:
    |- name: Test.java
    |""".trimMargin("|")
    val task = YamlFormatSynchronizer.deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    assertEquals("http://example.com", task.feedbackLink.link)
    assertEquals(FeedbackLink.LinkType.CUSTOM, task.feedbackLink.type)
  }
}