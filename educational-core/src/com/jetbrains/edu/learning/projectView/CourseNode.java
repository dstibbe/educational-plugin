package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.SimpleTextAttributes;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import icons.EducationalCoreIcons;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.EduNames.PROJECT_PLAYGROUND;


public class CourseNode extends EduNode {
  protected final Course myCourse;

  public CourseNode(@NotNull Project project,
                    PsiDirectory value,
                    ViewSettings viewSettings,
                    @NotNull Course course) {
    super(project, value, viewSettings);
    myCourse = course;
  }

  @Override
  protected void updateImpl(PresentationData data) {
    final Pair<Integer, Integer> progress = ProgressUtil.countProgress(myCourse);
    final Integer tasksSolved = progress.getFirst();
    final Integer tasksTotal = progress.getSecond();
    data.clearText();
    data.addText(myCourse.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    data.setIcon(EducationalCoreIcons.CourseTree);
    data.addText("  " + tasksSolved.toString() + "/" + tasksTotal.toString(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
  }

  @Override
  protected AbstractTreeNode modifyChildNode(AbstractTreeNode child) {
    Object value = child.getValue();
    if (value instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory)value;
      final Section section = myCourse.getSection(directory.getName());
      if (section != null) {
        return createSectionNode(directory, section);
      }
      final Lesson lesson = myCourse.getLesson(directory.getName());
      if (lesson != null) {
        return createLessonNode(directory, lesson);
      }
      if (directory.getName().equals(PROJECT_PLAYGROUND)) {
        return new ProjectNode(myProject, directory, getSettings());
      }
    }
    return null;
  }

  @NotNull
  protected SectionNode createSectionNode(@NotNull PsiDirectory directory, @NotNull Section section) {
    return new SectionNode(myProject, getSettings(), section, directory);
  }

  @NotNull
  protected LessonNode createLessonNode(@NotNull PsiDirectory directory, @NotNull Lesson lesson) {
    if (lesson instanceof FrameworkLesson) {
      return new FrameworkLessonNode(myProject, directory, getSettings(), (FrameworkLesson) lesson);
    } else {
      return new LessonNode(myProject, directory, getSettings(), lesson);
    }
  }
}
