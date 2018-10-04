package com.jetbrains.edu.learning.courseFormat.tasks;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.coursecreator.stepik.StepikChangeRetriever;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import icons.EducationalCoreIcons;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

/**
 * Implementation of task which contains task files, tests, input file for tests
 *
 * Update {@link StepikChangeRetriever#isEqualTo(Task, Task)} if you added new property that has to be compared
 *
 * To implement new task there are 5 steps to be done:
 * - Extend {@link Task} class
 * - Go to {@link Lesson#taskList} and update elementTypes in AbstractCollection annotation. Needed for proper xml serialization
 * - Update {@link SerializationUtils.Json.TaskAdapter#deserialize} to handle json serialization
 * - Update {@link TaskCheckerProvider#getTaskChecker} and provide default checker for new task
 * - Update {@link StepikTaskBuilder#pluginTaskTypes} for the tasks we do not have separately on stepik and {@link StepikTaskBuilder#stepikTaskTypes} otherwise
 */
public abstract class Task extends StudyItem {
  @Expose private String name;

  protected CheckStatus myStatus = CheckStatus.Unchecked;

  @SerializedName("stepic_id")
  @Expose private int myStepId;

  @SerializedName("task_files")
  @Expose private Map<String, TaskFile> myTaskFiles = new LinkedHashMap<>();

  @SerializedName("test_files")
  @Expose protected Map<String, String> testsText = new HashMap<>();

  @SerializedName("description_text")
  @Expose private String descriptionText;

  @SerializedName("description_format")
  @Expose private DescriptionFormat descriptionFormat = EduUtils.getDefaultTaskDescriptionFormat();

  @SerializedName("additional_files")
  @Expose protected Map<String, AdditionalFile> additionalFiles = new HashMap<>();

  @Transient private Lesson myLesson;
  @Expose @SerializedName("update_date") private Date myUpdateDate = new Date(0);

  @Expose
  @SerializedName("feedback_link")
  @NotNull
  private FeedbackLink myFeedbackLink = new FeedbackLink();

  public Task() {}

  public Task(@NotNull final String name) {
    this.name = name;
  }

  public void init(@Nullable Course course, @Nullable final StudyItem parentItem, boolean isRestarted) {
    setLesson(parentItem instanceof Lesson ? (Lesson)parentItem : null);
    for (TaskFile taskFile : getTaskFileValues()) {
      taskFile.initTaskFile(this, isRestarted);
    }
  }

  public Map<String, TaskFile> getTaskFiles() {
    return myTaskFiles;
  }

  // Use carefully. taskFiles is supposed to be ordered so use LinkedHashMap
  public void setTaskFiles(Map<String, TaskFile> taskFiles) {
    this.myTaskFiles = taskFiles;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  public String getDescriptionText() {
    return descriptionText;
  }

  public void setDescriptionText(String descriptionText) {
    this.descriptionText = descriptionText;
  }

  public DescriptionFormat getDescriptionFormat() {
    return descriptionFormat;
  }

  public void setDescriptionFormat(DescriptionFormat descriptionFormat) {
    this.descriptionFormat = descriptionFormat;
  }

  public Map<String, String> getTestsText() {
    return testsText;
  }

  @SuppressWarnings("unused")
  //used for deserialization
  public void setTestsText(Map<String, String> testsText) {
    this.testsText = testsText;
  }

  @NotNull
  public Map<String, AdditionalFile> getAdditionalFiles() {
    return additionalFiles;
  }

  @SuppressWarnings("unused")
  //used for deserialization
  public void setAdditionalFiles(@NotNull Map<String, AdditionalFile> additionalFiles) {
    this.additionalFiles = additionalFiles;
  }

  public void addTestsTexts(String name, String text) {
    testsText.put(name, text);
  }

  public void addAdditionalFile(@NotNull String name, @NotNull String text) {
    additionalFiles.put(name, new AdditionalFile(text, true));
  }

  public void addAdditionalFile(@NotNull String name, @NotNull AdditionalFile file) {
    additionalFiles.put(name, file);
  }

  @Nullable
  public TaskFile getTaskFile(final String name) {
    return name != null ? myTaskFiles.get(name) : null;
  }

  public TaskFile addTaskFile(@NotNull final String name) {
    TaskFile taskFile = new TaskFile();
    taskFile.setTask(this);
    taskFile.setName(name);
    myTaskFiles.put(name, taskFile);
    return taskFile;
  }

  public void addTaskFile(@NotNull final TaskFile taskFile) {
    taskFile.setTask(this);
    myTaskFiles.put(taskFile.getName(), taskFile);
  }

  @Nullable
  public TaskFile getFile(@NotNull final String fileName) {
    return myTaskFiles.get(fileName);
  }

  @Transient
  public Lesson getLesson() {
    return myLesson;
  }

  @Transient
  public void setLesson(Lesson lesson) {
    myLesson = lesson;
  }

  @Nullable
  public VirtualFile getTaskDir(@NotNull final Project project) {
    final VirtualFile lessonDir = myLesson.getLessonDir(project);

    return lessonDir == null ? null : lessonDir.findChild(TaskExt.getDirName(this));
  }

  /**
   * @param wrap if true, text will be wrapped with ancillary information (e.g. to display latex)
   */
  public String getTaskDescription(boolean wrap, @Nullable VirtualFile taskDir) {
    String taskText = descriptionText;
    if (!wrap) {
      return taskText;
    }
    if (taskDir != null) {
      StringBuffer text = new StringBuffer(taskText);
      EduUtils.replaceActionIDsWithShortcuts(text);
      addPlaceholderHints(text);
      taskText = text.toString();
      if (descriptionFormat == DescriptionFormat.MD) {
        taskText = EduUtils.convertToHtml(taskText, taskDir);
      }
    }
    return taskText;
  }

  private void addPlaceholderHints(StringBuffer text) {
    List<String> hints = new ArrayList<>();
    for (TaskFile value : getTaskFiles().values()) {
      for (AnswerPlaceholder placeholder : value.getAnswerPlaceholders()) {
        for (String hint : placeholder.getHints()) {
          if (!hint.isEmpty()) {
            hints.add(hint);
          }
        }
      }
    }

    if (hints.isEmpty()) {
      return;
    }

    text.append("<br>");
    for (String hint : hints) {
      text.append("<div class='hint'>").append(hint).append("</div>");
    }
    text.append("<br>");
  }

  @Nullable
  public String getTaskDescription(@Nullable VirtualFile taskDir) {
    if (taskDir == null) {
      return null;
    }
    return getTaskDescription(true, taskDir);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Task task = (Task)o;

    if (getIndex() != task.getIndex()) return false;
    if (name != null ? !name.equals(task.name) : task.name != null) return false;
    if (myTaskFiles != null ? !myTaskFiles.equals(task.myTaskFiles) : task.myTaskFiles != null) return false;
    if (descriptionText != null ? !descriptionText.equals(task.descriptionText) : task.descriptionText != null) return false;
    if (descriptionFormat != null ? !descriptionFormat.equals(task.descriptionFormat) : task.descriptionFormat != null) return false;
    if (testsText != null ? !testsText.equals(task.testsText) : task.testsText != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + getIndex();
    result = 31 * result + (myTaskFiles != null ? myTaskFiles.hashCode() : 0);
    result = 31 * result + (descriptionText != null ? descriptionText.hashCode() : 0);
    result = 31 * result + (descriptionFormat != null ? descriptionFormat.hashCode() : 0);
    result = 31 * result + (testsText != null ? testsText.hashCode() : 0);
    return result;
  }

  public void setStepId(int stepId) {
    myStepId = stepId;
  }

  public int getStepId() {
    return myStepId;
  }

  public CheckStatus getStatus() {
    return myStatus;
  }

  public void setStatus(CheckStatus status) {
    for (TaskFile taskFile : myTaskFiles.values()) {
      for (AnswerPlaceholder placeholder : taskFile.getAnswerPlaceholders()) {
        placeholder.setStatus(status);
      }
    }
    myStatus = status;
  }

  public Task copy() {
    Element element = XmlSerializer.serialize(this);
    Task copy = XmlSerializer.deserialize(element, getClass());
    copy.init(null, null, true);
    return copy;
  }

  public void setUpdateDate(Date date) {
    myUpdateDate = date;
  }

  public Date getUpdateDate() {
    return myUpdateDate;
  }

  // used in json serialization/deserialization
  public abstract String getTaskType();

  public int getPosition() {
    final Lesson lesson = getLesson();
    return lesson.getTaskList().indexOf(this) + 1;
  }

  public boolean isValid(@NotNull Project project) {
    VirtualFile taskDir = getTaskDir(project);
    if (taskDir == null) return false;
    for (TaskFile taskFile : getTaskFileValues()) {
      VirtualFile file = EduUtils.findTaskFileInDir(taskFile, taskDir);
      if (file == null) continue;
      try {
        String text = VfsUtilCore.loadText(file);
        if (!taskFile.isValid(text)) return false;
      }
      catch (IOException e) {
        return false;
      }
    }
    return true;
  }

  public boolean isToSubmitToStepik() {
    return false;
  }

  public Icon getIcon() {
    if (myStatus == CheckStatus.Unchecked) {
      return EducationalCoreIcons.Task;
    }
    return myStatus == CheckStatus.Solved ? EducationalCoreIcons.TaskSolved : EducationalCoreIcons.TaskFailed;
  }

  @Override
  public int getId() {
    return myStepId;
  }

  @NotNull
  public FeedbackLink getFeedbackLink() {
    return myFeedbackLink;
  }

  public void setFeedbackLink(@NotNull FeedbackLink feedbackLink) {
    myFeedbackLink = feedbackLink;
  }

  @Override
  @Nullable
  public VirtualFile getDir(@NotNull Project project) {
    return getTaskDir(project);
  }

  @NotNull
  @Override
  public Course getCourse() {
    return myLesson.getCourse();
  }

  @NotNull
  private Collection<TaskFile> getTaskFileValues() {
    return getTaskFiles().values();
  }

  @SuppressWarnings("unused") //used for yaml deserialization
  private void setTaskFileValues(List<TaskFile> taskFiles) {
    this.myTaskFiles.clear();
    for (TaskFile taskFile : taskFiles) {
      this.myTaskFiles.put(taskFile.getName(), taskFile);
    }
  }

  @NotNull
  @Override
  public StudyItem getParent() {
    return myLesson;
  }
}
