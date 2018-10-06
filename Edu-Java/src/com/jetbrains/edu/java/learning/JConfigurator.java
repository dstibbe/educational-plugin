package com.jetbrains.edu.java.learning;

import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.gradle.GradleConfiguratorBase;
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase;
import org.jetbrains.annotations.NotNull;

public class JConfigurator extends GradleConfiguratorBase {

  public static final String TEST_JAVA = "Test.java";
  public static final String TASK_JAVA = "Task.java";
  public static final String MOCK_JAVA = "Mock.java";

  private final JCourseBuilder myCourseBuilder = new JCourseBuilder();

  @NotNull
  @Override
  public GradleCourseBuilderBase getCourseBuilder() {
    return myCourseBuilder;
  }

  @NotNull
  @Override
  public String getTestFileName() {
    return TEST_JAVA;
  }

  @Override
  public boolean isEnabled() {
    return !EduUtils.isAndroidStudio();
  }

  @NotNull
  @Override
  public TaskCheckerProvider getTaskCheckerProvider() {
    return new JTaskCheckerProvider();
  }

  @Override
  public String getMockTemplate() {
    return FileTemplateManager.getDefaultInstance().getInternalTemplate(MOCK_JAVA).getText();
  }
}
