package com.jetbrains.edu.learning.gradle

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_PROPERTIES
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_JAR
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_PROPERTIES
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_WIN
import com.jetbrains.edu.learning.gradle.GradleConstants.LOCAL_PROPERTIES
import com.jetbrains.edu.learning.gradle.GradleConstants.SETTINGS_GRADLE
import com.jetbrains.edu.learning.gradle.generation.EduGradleUtils
import java.io.File
import java.io.IOException

abstract class GradleConfiguratorBase : EduConfigurator<JdkProjectSettings> {

  abstract override fun getCourseBuilder(): GradleCourseBuilderBase

  override fun excludeFromArchive(project: Project, file: VirtualFile): Boolean {
    if (super.excludeFromArchive(project, file)) return true
    val name = file.name
    val path = file.path
    val pathSegments = path.split(VfsUtilCore.VFS_SEPARATOR_CHAR)
    if (SETTINGS_GRADLE == name) {
      try {
        val settingsDefaultText = EduGradleUtils.getInternalTemplateText(courseBuilder.settingGradleTemplateName,
                                                                         courseBuilder.templateVariables(project))
        val ioFile = File(path)
        return if (ioFile.exists()) FileUtil.loadFile(ioFile) == settingsDefaultText else true
      }
      catch (e: IOException) {
        LOG.error(e)
      }
    }
    return name in NAMES_TO_EXCLUDE || pathSegments.any { it in FOLDERS_TO_EXCLUDE }
  }

  override fun getSourceDir(): String = EduNames.SRC
  override fun getTestDirs(): List<String> = listOf(EduNames.TEST)

  override fun isTestFile(project: Project, file: VirtualFile): Boolean {
    if (file.isDirectory) return false
    val task = EduUtils.getTaskForFile(project, file) ?: return false
    val taskDir = task.getTaskDir(project) ?: return false
    return task.findTestDirs(taskDir).any { testDir -> VfsUtil.isAncestor(testDir, file, true) }
  }

  override fun pluginRequirements(): List<String> = listOf("org.jetbrains.plugins.gradle", "JUnit")

  companion object {
    private val NAMES_TO_EXCLUDE = ContainerUtil.newHashSet(
      ".idea", "EduTestRunner.java", GRADLE_WRAPPER_UNIX, GRADLE_WRAPPER_WIN, LOCAL_PROPERTIES, GRADLE_PROPERTIES,
      SETTINGS_GRADLE, GRADLE_WRAPPER_JAR, GRADLE_WRAPPER_PROPERTIES)

    private val FOLDERS_TO_EXCLUDE = ContainerUtil.newHashSet(EduNames.OUT, EduNames.BUILD)

    private val LOG = Logger.getInstance(GradleConfiguratorBase::class.java)
  }
}
