package com.jetbrains.edu.python.learning;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NonNls;

import java.io.File;

@SuppressWarnings({"UtilityClassWithoutPrivateConstructor", "UtilityClassWithPublicConstructor", "ComponentNotRegistered"}) // Edu-Python.xml
public class PyInitialConfigurator implements ApplicationComponent {
  @NonNls private static final String CONFIGURED_V40 = "StudyPyCharm.InitialConfiguration40";

  /**
   * @noinspection UnusedParameters
   */
  public PyInitialConfigurator(MessageBus bus,
                               CodeInsightSettings codeInsightSettings,
                               final PropertiesComponent propertiesComponent,
                               FileTypeManager fileTypeManager,
                               final ProjectManagerEx projectManager) {
    if (!propertiesComponent.getBoolean(CONFIGURED_V40)) {
      final File courses = new File(PathManager.getConfigPath(), "courses");
      FileUtil.delete(courses);
      propertiesComponent.setValue(CONFIGURED_V40, "true");
    }
  }
}
