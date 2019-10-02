package com.jetbrains.edu.learning.checker.gradle

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.Err
import com.jetbrains.edu.learning.checker.Ok
import com.jetbrains.edu.learning.checker.TheoryTaskChecker
import com.jetbrains.edu.learning.checker.details.CheckDetailsView
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class GradleTheoryTaskChecker(
  task: TheoryTask,
  project: Project,
  private val mainClassForFile: (Project, VirtualFile) -> String?
) : TheoryTaskChecker(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val result = runGradleRunTask(project, task, mainClassForFile)
    val output = when (result) {
      is Err -> return result.error
      is Ok -> result.value
    }

    CheckDetailsView.getInstance(project).showOutput(output)
    return CheckResult(CheckStatus.Solved, "")
  }
}
