/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.ui.taskDescription;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class JavaFxToolWindow extends TaskDescriptionToolWindow {
  private BrowserWindow myBrowserWindow;
  private static final String HINT_BLOCK_TEMPLATE = "<div class='hint_header'>Hint %d</div>" +
                                                    "  <div class='hint_content'>" +
                                                    " %s" +
                                                    "  </div>";
  private static final String HINT_EXPANDED_BLOCK_TEMPLATE = "<div class='hint_header checked'>Hint %d</div>" +
                                                             "  <div class='hint_content'>" +
                                                             " %s" +
                                                             "  </div>";

  private Project myProject;
  private JFXPanel taskSpecificPanel;

  public JavaFxToolWindow() {
    super();
  }

  @Override
  public JComponent createTaskInfoPanel(@NotNull Project project) {
    myProject = project;
    myBrowserWindow = new BrowserWindow(project, true);
    return myBrowserWindow.getPanel();
  }

  public JComponent createTaskSpecificPanel(Task task) {
    taskSpecificPanel = new JFXPanel();
    return taskSpecificPanel;
  }

  public void updateTaskSpecificPanel(@Nullable Task task) {
    if (taskSpecificPanel == null) return;
    Platform.runLater(() -> {
      final Scene scene = task != null ? JavaFxTaskUtil.createScene(task) : null;
      taskSpecificPanel.setScene(scene);
      taskSpecificPanel.setVisible(scene != null);
    });
  }

  @Override
  protected String wrapHint(@NotNull String hintText, int hintNumber) {
    Course course = StudyTaskManager.getInstance(myProject).getCourse();
    if (course == null) {
      return String.format(HINT_BLOCK_TEMPLATE, hintNumber, hintText);
    }

    boolean study = course.isStudy();
    if (study) {
      return String.format(HINT_BLOCK_TEMPLATE, hintNumber, hintText);
    }
    else {
      return String.format(HINT_EXPANDED_BLOCK_TEMPLATE, hintNumber, hintText);
    }
  }

  @Override
  protected void updateLaf() {
    myBrowserWindow.updateLaf();
  }

  @Override
  public void setText(@NotNull String text) {
    String wrappedText = wrapHints(text);
    myBrowserWindow.loadContent(wrappedText);
  }
}
