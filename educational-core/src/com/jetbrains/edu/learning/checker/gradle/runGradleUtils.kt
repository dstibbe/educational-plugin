package com.jetbrains.edu.learning.checker.gradle

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.checker.CheckUtils.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.dirName
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_WIN
import com.jetbrains.edu.learning.gradle.generation.EduGradleUtils

const val MAIN_CLASS_PROPERTY_PREFIX = "-PmainClass="

const val ASSEMBLE_TASK_NAME = "assemble"
const val TEST_TASK_NAME = "test"

const val TESTS_ARG = "--tests"

fun getGradleProjectName(task: Task) =
  if (task.lesson.section != null)
    ":${EduGradleUtils.sanitizeName(task.lesson.section!!.name)}-${EduGradleUtils.sanitizeName(task.lesson.name)}-${EduGradleUtils.sanitizeName(task.dirName)}"
  else
    ":${EduGradleUtils.sanitizeName(task.lesson.name)}-${EduGradleUtils.sanitizeName(task.dirName)}"

class GradleCommandLine private constructor(
  private val cmd: GeneralCommandLine,
  val taskName: String
) {

  fun launchAndCheck(): CheckResult {
    val output = launch() ?: return CheckResult.FAILED_TO_CHECK
    if (!output.isSuccess) return CheckResult(CheckStatus.Failed, output.firstMessage, output.messages.joinToString("\n"))

    return TestsOutputParser.getCheckResult(output.messages)
  }

  fun launch(): GradleOutput? {
    val output = try {
      val handler = CapturingProcessHandler(cmd)
      if (ProgressManager.getInstance().hasProgressIndicator()) {
        handler.runProcessWithProgressIndicator(ProgressManager.getInstance().progressIndicator)
      } else {
        handler.runProcess()
      }
    } catch (e: ExecutionException) {
      LOG.info(CheckUtils.FAILED_TO_CHECK_MESSAGE, e)
      return null
    }

    val stderr = output.stderr
    if (!stderr.isEmpty() && output.stdout.isEmpty()) {
      return GradleOutput(false, listOf(stderr))
    }

    //gradle prints compilation failures to error stream
    if (hasCompilationErrors(output)) {
      return GradleOutput(false, listOf(COMPILATION_FAILED_MESSAGE, output.stderr))
    }

    if (!output.stdout.contains(taskName)) {
      LOG.warn("#educational: executing $taskName fails: \n" + output.stdout)
      return GradleOutput(false, listOf("$FAILED_TO_CHECK_MESSAGE. See idea.log for more details."))
    }

    return GradleOutput(true, collectMessages(output))
  }

  private fun collectMessages(output: ProcessOutput): List<String> {
    var currentMessage: StringBuilder? = null
    val allMessages = mutableListOf<String>()

    fun addCurrentMessageIfNeeded() {
      if (currentMessage != null) {
        allMessages += currentMessage.toString()
      }
    }

    for (line in output.stdoutLines) {
      if (line.startsWith(STUDY_PREFIX)) {
        val messageLine = line.removePrefix(STUDY_PREFIX)
        if (currentMessage != null) {
          currentMessage.appendln(messageLine)
        } else {
          currentMessage = StringBuilder(messageLine).append("\n")
        }
      } else {
        addCurrentMessageIfNeeded()
        currentMessage = null
      }
    }

    addCurrentMessageIfNeeded()
    return allMessages
  }

  companion object {

    private val LOG: Logger = Logger.getInstance(GradleCommandLine::class.java  )

    fun create(project: Project, command: String, vararg additionalParams: String): GradleCommandLine? {
      val basePath = project.basePath ?: return null
      val projectJdkPath = ProjectRootManager.getInstance(project).projectSdk?.homePath ?: return null
      val projectPath = FileUtil.toSystemDependentName(basePath)
      val cmd = GeneralCommandLine()
        .withEnvironment("JAVA_HOME", projectJdkPath)
        .withWorkDirectory(FileUtil.toSystemDependentName(basePath))
        .withExePath(if (SystemInfo.isWindows) FileUtil.join(projectPath, GRADLE_WRAPPER_WIN) else "./$GRADLE_WRAPPER_UNIX")
        .withParameters(command)
        .withParameters(*additionalParams)

      return GradleCommandLine(cmd, command)
    }
  }
}

class GradleOutput(val isSuccess: Boolean, _messages: List<String>) {
  val messages = _messages.map { it.postProcessOutput() }

  val firstMessage: String get() = messages.firstOrNull { it.isNotBlank() } ?: "<no output>"
}

fun String.postProcessOutput(): String = replace(System.getProperty("line.separator"), "\n").removeSuffix("\n")

/**
 * Run gradle 'run' task.
 * Returns gradle output if task was successfully executed, otherwise returns CheckResult.
 */
fun runGradleRunTask(
  project: Project,
  task: Task,
  mainClassForFile: (Project, VirtualFile) -> String?
): ExecutionResult<String, CheckResult> {
  val mainClassName = findMainClass(project, task, mainClassForFile)
                      ?: return Err(CheckResult(CheckStatus.Unchecked, "Unable to execute task ${task.name}"))
  val taskName = if (task.hasSeparateModule(project)) "${getGradleProjectName(task)}:run" else "run"

  val gradleOutput = GradleCommandLine.create(project, taskName, "${MAIN_CLASS_PROPERTY_PREFIX}$mainClassName")
    ?.launch()
    ?: return Err(CheckResult.FAILED_TO_CHECK)

  if (!gradleOutput.isSuccess) {
    return Err(CheckResult(CheckStatus.Failed, gradleOutput.firstMessage, gradleOutput.messages.joinToString("\n")))
  }

  return Ok(gradleOutput.firstMessage)
}

private fun findMainClass(project: Project, task: Task, mainClassForFile: (Project, VirtualFile) -> String?): String? =
  runReadAction {
    val selectedFile = getSelectedFile(project)
    if (selectedFile != null) {
      val fileTask = EduUtils.getTaskForFile(project, selectedFile)
      if (fileTask == task) {
        val mainClass = mainClassForFile(project, selectedFile)
        if (mainClass != null) return@runReadAction mainClass
      }
    }

    for ((_, taskFile) in task.taskFiles) {
      val file = taskFile.getVirtualFile(project) ?: continue
      return@runReadAction mainClassForFile(project, file) ?: continue
    }
    null
  }

private fun getSelectedFile(project: Project): VirtualFile? {
  val editor = EduUtils.getSelectedEditor(project) ?: return null
  return FileDocumentManager.getInstance().getFile(editor.document)
}

/**
 * There are two types of supported gradle projects: module-per-task and one module for the whole course
 */
fun Task.hasSeparateModule(project: Project): Boolean {
  val taskDir = getTaskDir(project) ?: error("Dir for task $name not found")
  val taskModule = ModuleUtil.findModuleForFile(taskDir, project) ?: error("Module for task $name not found")
  val courseModule = ModuleUtil.findModuleForFile(EduUtils.getCourseDir(project), project)
  return taskModule != courseModule
}
