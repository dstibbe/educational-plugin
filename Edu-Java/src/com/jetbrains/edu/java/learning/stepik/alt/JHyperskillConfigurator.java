package com.jetbrains.edu.java.learning.stepik.alt;

import com.jetbrains.edu.java.learning.JTaskCheckerProvider;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.gradle.GradleConfiguratorBase;
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase;
import org.jetbrains.annotations.NotNull;

public class JHyperskillConfigurator extends GradleConfiguratorBase {
  private final JHyperskillCourseBuilder myCourseBuilder = new JHyperskillCourseBuilder();

  @NotNull
  @Override
  public GradleCourseBuilderBase getCourseBuilder() {
    return myCourseBuilder;
  }

  @NotNull
  @Override
  public String getTestFileName() {
    return "";
  }

  @NotNull
  @Override
  public TaskCheckerProvider getTaskCheckerProvider() {
    return new JTaskCheckerProvider();
  }
}
