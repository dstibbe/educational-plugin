package com.jetbrains.edu.learning.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.stepik.StepikCourseUpdater;
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader;
import com.jetbrains.edu.learning.stepik.StepikUpdateDateExt;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SyncCourseAction extends DumbAwareAction {

  public SyncCourseAction() {
    super("Synchronize Course", "Synchronize Course", EducationalCoreIcons.StepikRefresh);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project != null) {
      doUpdate(project);
    }
  }

  public static void doUpdate(@NotNull Project project) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    if (course instanceof RemoteCourse) {
      ProgressManager.getInstance().run(new Task.Backgroundable(project, "Updating Course", true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);

          if (StepikUpdateDateExt.isUpToDate((RemoteCourse)course)) {
            ApplicationManager.getApplication().invokeLater(() -> {
              Notification notification = new Notification("Update.course", "Course is up to date", "", NotificationType.INFORMATION);
              notification.notify(project);
            });
          }
          else {
            new StepikCourseUpdater((RemoteCourse)course, project).updateCourse();
            StepikUpdateDateExt.setUpdated((RemoteCourse)course);
          }
        }
      });
    }

    if (CCUtils.isCourseCreator(project)) {
      return;
    }

    StepikSolutionsLoader courseSynchronizer = StepikSolutionsLoader.getInstance(project);
    courseSynchronizer.loadSolutionsInBackground();
  }

  public static boolean isAvailable(@Nullable Project project) {
    if (project == null) {
      return false;
    }

    if (!EduUtils.isStudyProject(project)) {
      return false;
    }

    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof RemoteCourse) || !((RemoteCourse) course).isLoadSolutions()) {
      return false;
    }

    return true;
  }

  @Override
  public void update(AnActionEvent e) {
    boolean visible = isAvailable(e.getProject());
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(visible);
  }
}
