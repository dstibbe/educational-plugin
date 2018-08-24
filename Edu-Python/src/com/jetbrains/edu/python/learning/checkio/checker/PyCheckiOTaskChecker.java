package com.jetbrains.edu.python.learning.checkio.checker;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.CheckiOCourseUpdater;
import com.jetbrains.edu.learning.checkio.checker.CheckiOMissionCheck;
import com.jetbrains.edu.learning.checkio.checker.CheckiOTaskChecker;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOApiConnector;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import com.jetbrains.python.PythonFileType;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOTaskChecker extends CheckiOTaskChecker {
  protected PyCheckiOTaskChecker(@NotNull EduTask task, @NotNull Project project) {
    super(task, project, PyCheckiOOAuthConnector.getInstance());
  }

  @NotNull
  @Override
  protected CheckiOMissionCheck getMissionCheck() {
    return new PyCheckiOMissionCheck(project, task);
  }

  @NotNull
  @Override
  protected CheckiOCourseUpdater getCourseUpdater() {
    final CheckiOCourseContentGenerator contentGenerator =
      new CheckiOCourseContentGenerator(PythonFileType.INSTANCE, PyCheckiOApiConnector.getInstance());

    return new CheckiOCourseUpdater(
      (CheckiOCourse) task.getCourse(),
      project,
      contentGenerator
    );
  }
}
