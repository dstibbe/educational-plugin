package com.jetbrains.edu.learning.taskDescription

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.keymap.impl.KeymapManagerImpl
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.ui.taskDescription.BrowserWindow
import org.jsoup.Jsoup
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.File
import java.io.IOException
import javax.swing.KeyStroke

class TaskDescriptionTest : EduTestCase() {
  companion object {
    private val overrideMethodShortcut: String = getKeystrokeText(KeyEvent.VK_O, InputEvent.CTRL_MASK)
    private val goToActionShortcut: String = getKeystrokeText(KeyEvent.VK_A, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK)

    private fun getKeystrokeText(keyChar: Int, modifiers: Int) = KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(keyChar, modifiers))
  }

  fun testSimpleImg() {
    doTestImage()
  }

  fun testImgInsideOtherTags() {
    doTestImage()
  }

  fun testWebImage() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    val name = getTestName(true) + ".html"
    val fileText = FileUtil.loadFile(File(testDataPath, name))
    val initialDocument = Jsoup.parse(fileText)

    val processedText = BrowserWindow.processContent(fileText, project)
    val processedDocument = Jsoup.parse(processedText)

    val initialImgElements = initialDocument.getElementsByTag("img")
    val processedImageElements = processedDocument.getElementsByTag("img")
    processedImageElements.zip(initialImgElements)
    initialImgElements.zip(processedImageElements).map { assert(it.first.attr("src") == it.second.attr("src") ) }
  }

  private fun doTestImage() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    val name = getTestName(true) + ".html"
    val fileText = FileUtil.loadFile(File(testDataPath, name))
    val processedText = BrowserWindow.processContent(fileText, project)
    val document = Jsoup.parse(processedText)
    val imageElements = document.getElementsByTag("img")
    imageElements
      .map { assert(BrowserUtil.isAbsoluteURL(it.attr("src"))) }
  }

  fun testShortcutRendering() {
    val taskText = "You can use &shortcut:OverrideMethods; to override methods"
    val taskTextWithShortcuts = "You can use $overrideMethodShortcut to override methods"
    doTestShortcut(taskText, taskTextWithShortcuts)
  }

  fun testSeveralShortcutsRendering() {
    val taskText = "You can use &shortcut:OverrideMethods; to override methods. One more useful shortcut: &shortcut:GotoAction;"
    val taskTextWithShortcuts = "You can use $overrideMethodShortcut to override methods. " +
                                "One more useful shortcut: $goToActionShortcut"
    doTestShortcut(taskText, taskTextWithShortcuts)
  }

  fun testShortcutInsideTag() {
    val taskText = "You can use <code>&shortcut:OverrideMethods;</code> to override methods. One more useful shortcut: &shortcut:GotoAction;"
    val taskTextWithShortcuts = "You can use $overrideMethodShortcut to override methods. One more useful shortcut: $goToActionShortcut"
    doTestShortcut(taskText, taskTextWithShortcuts)
  }

  private fun doTestShortcut(taskText: String, taskTextWithShortcuts: String, keymapName: String = "Default for XWin") {
    configureByTaskFile(1, 1, "taskFile1.txt")
    val task = EduUtils.getCurrentTask(project)!!
    task.descriptionText = taskText
    val oldActiveKeymap = KeymapManager.getInstance().activeKeymap
    val keymapManager = KeymapManager.getInstance() as KeymapManagerImpl
    keymapManager.activeKeymap = keymapManager.getKeymap(keymapName)
    val taskDescription = task.getTaskDescription(task.getTaskDir(project))
    assertEquals(taskTextWithShortcuts, taskDescription!!.getBody())
    keymapManager.activeKeymap = oldActiveKeymap
  }

  @Throws(IOException::class)
  override fun createCourse() {
    myFixture.copyDirectoryToProject("lesson1", "lesson1")
    val course = Course()
    course.name = "Edu test course"
    course.language = PlainTextLanguage.INSTANCE.id
    StudyTaskManager.getInstance(myFixture.project).course = course

    val lesson1 = createLesson(1, 1)
    course.addLesson(lesson1)
    course.init(null, null, false)

  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/toolWindow"
  }

  private fun String.getBody() = Jsoup.parse(this).getElementsByTag("body").text()

}