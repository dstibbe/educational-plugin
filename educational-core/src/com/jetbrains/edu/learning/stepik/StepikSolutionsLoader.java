package com.jetbrains.edu.learning.stepik;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.stepik.serialization.StepikSubmissionTaskAdapter;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView;
import com.jetbrains.edu.learning.update.UpdateNotification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jetbrains.edu.learning.stepik.StepikCheckerConnector.EDU_TOOLS_COMMENT;
import static com.jetbrains.edu.learning.stepik.StepikConnector.*;

public class StepikSolutionsLoader implements Disposable {
  public static final String PROGRESS_ID_PREFIX = "77-";

  private static final String NOTIFICATION_TITLE = "Outdated EduTools Plugin";
  private static final String NOTIFICATION_CONTENT = "<html>Your version of EduTools plugin is outdated to apply all solutions.\n" +
                                                     "<a href=\"\">Update plugin</a> to avoid compatibility problems.\n";

  private static final Logger LOG = Logger.getInstance(StepikSolutionsLoader.class);
  private final HashMap<Integer, Future<Boolean>> myFutures = new HashMap<>();
  private final Project myProject;
  private MessageBusConnection myBusConnection;
  private Task mySelectedTask;

  protected StepikSolutionsLoader(@NotNull final Project project) {
    this.myProject = project;
  }

  public static StepikSolutionsLoader getInstance(@NotNull Project project) {
    StepikSolutionsLoader service = ServiceManager.getService(project, StepikSolutionsLoader.class);
    if (service != null) {
      service.init();
    }
    return service;
  }

  private void init() {
    EduEditor selectedEduEditor = EduUtils.getSelectedEduEditor(myProject);
    if (selectedEduEditor != null && selectedEduEditor.getTaskFile() != null) {
      mySelectedTask = selectedEduEditor.getTaskFile().getTask();
    }
    addFileOpenListener();
  }

  public void loadSolutionsInBackground() {
    ProgressManager.getInstance().run(new Backgroundable(myProject, "Getting Tasks to Update") {
      @Override
      public void run(@NotNull ProgressIndicator progressIndicator) {
        Course course = StudyTaskManager.getInstance(myProject).getCourse();
        if (course != null) {
          loadSolutions(progressIndicator, course);
        }
      }
    });
  }

  private void loadSolutions(@Nullable ProgressIndicator progressIndicator, @NotNull Course course) {
    List<Task> tasksToUpdate = EduUtils.execCancelable(() -> tasksToUpdate(course));
    if (tasksToUpdate != null) {
      updateTasks(tasksToUpdate, progressIndicator);
    }
    else {
      LOG.warn("Can't get a list of tasks to update");
    }
  }

  private void updateTasks(@NotNull List<Task> tasks, @Nullable ProgressIndicator progressIndicator) {
    cancelUnfinishedTasks();
    myFutures.clear();

    List<Task> tasksToUpdate = tasks.stream()
      .filter(task -> !(task instanceof TheoryTask))
      .collect(Collectors.toList());

    CountDownLatch countDownLatch = new CountDownLatch(tasksToUpdate.size());
    for (int i = 0; i < tasksToUpdate.size(); i++) {
      final Task task = tasksToUpdate.get(i);
      final int progressIndex = i + 1;
      if (progressIndicator == null || !progressIndicator.isCanceled()) {
        Future<Boolean> future = ApplicationManager.getApplication().executeOnPooledThread(() -> {
          try {
            boolean isSolved = task.getStatus() == CheckStatus.Solved;
            if (progressIndicator != null) {
              progressIndicator.setFraction((double)progressIndex / tasksToUpdate.size());
              progressIndicator.setText(String.format("Loading solution %d from %d", progressIndex, tasksToUpdate.size()));
            }
            return loadSolution(myProject, task, isSolved);
          }
          finally {
            countDownLatch.countDown();
          }
        });
        myFutures.put(task.getStepId(), future);
      }
      else {
        countDownLatch.countDown();
      }
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      if (mySelectedTask != null && tasksToUpdate.contains(mySelectedTask)) {
        EduEditor selectedEduEditor = EduUtils.getSelectedEduEditor(myProject);
        if (selectedEduEditor != null) {
          selectedEduEditor.showLoadingPanel();
          enableEditorWhenFutureDone(myFutures.get(mySelectedTask.getStepId()));
        }
      }
    });

    try {
      countDownLatch.await();
      final boolean needToShowNotification = needToShowUpdateNotification();
      ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
        if (needToShowNotification) {
          new UpdateNotification(NOTIFICATION_TITLE, NOTIFICATION_CONTENT).notify(myProject);
        }
        EduUtils.synchronize();
        if (mySelectedTask != null) {
          updateUI(myProject, mySelectedTask);
        }
      }));
      myBusConnection.disconnect();
    }
    catch (InterruptedException e) {
      LOG.warn(e);
    }
  }

  private boolean needToShowUpdateNotification() {
    return myFutures.values().stream().anyMatch(future -> {
      try {
        Boolean result = future.get();
        return result == Boolean.TRUE;
      } catch (InterruptedException | ExecutionException e) {
        LOG.warn(e);
        return false;
      }
    });
  }

  private void cancelUnfinishedTasks() {
    for (Future future : myFutures.values()) {
      if (!future.isDone()) {
        future.cancel(true);
      }
    }
  }

  public List<Task> tasksToUpdate(@NotNull Course course) {
    List<Task> tasksToUpdate = new ArrayList<>();
    Stream<Lesson> lessonsFromSection = course.getSections().stream().flatMap(section -> section.getLessons().stream());
    Stream<Lesson> allLessons = Stream.concat(lessonsFromSection, course.getLessons().stream());
    Task[] allTasks = allLessons.flatMap(lesson -> lesson.getTaskList().stream()).toArray(Task[]::new);

    String[] progresses = Arrays.stream(allTasks).map(task -> PROGRESS_ID_PREFIX + String.valueOf(task.getStepId())).toArray(String[]::new);
    Boolean[] taskStatuses = taskStatuses(progresses);
    if (taskStatuses == null) return tasksToUpdate;
    for (int j = 0; j < allTasks.length; j++) {
      Boolean isSolved = taskStatuses[j];
      Task task = allTasks[j];
      boolean toUpdate = false;
      if (isSolved != null && !(task instanceof TheoryTask)) {
        toUpdate = isToUpdate(task, isSolved, task.getStatus(), task.getStepId());
      }
      if (toUpdate) {
        task.setStatus(checkStatus(isSolved));
        tasksToUpdate.add(task);
      }
    }
    return tasksToUpdate;
  }

  private static CheckStatus checkStatus(boolean solved) {
    return solved ? CheckStatus.Solved : CheckStatus.Failed;
  }

  private void addFileOpenListener() {
    myBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
    myBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        EduEditor eduEditor = EduUtils.getSelectedEduEditor(myProject);
        TaskFile taskFile = EduUtils.getTaskFile(myProject, file);
        if (eduEditor != null && taskFile != null) {
          mySelectedTask = taskFile.getTask();
          Task task = taskFile.getTask();
          if (myFutures.containsKey(task.getStepId())) {
            eduEditor.showLoadingPanel();
            Future future = myFutures.get(task.getStepId());
            if (!future.isDone() || !future.isCancelled()) {
              enableEditorWhenFutureDone(future);
            }
          }
        }
      }
    });
  }

  private void enableEditorWhenFutureDone(@NotNull Future future) {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        future.get();
        ApplicationManager.getApplication().invokeLater(() -> {
          EduEditor selectedEditor = EduUtils.getSelectedEduEditor(myProject);
          if (selectedEditor != null && mySelectedTask.getTaskFiles().containsKey(selectedEditor.getTaskFile().getName())) {
            JBLoadingPanel component = selectedEditor.getComponent();
            component.stopLoading();
            ((EditorImpl)selectedEditor.getEditor()).setViewer(false);
            selectedEditor.validateTaskFile();
          }
        });
      }
      catch (InterruptedException | ExecutionException e) {
        LOG.warn(e.getCause());
      }
    });
  }

  private static boolean isToUpdate(Task task, @NotNull Boolean isSolved, @NotNull CheckStatus currentStatus, int stepId) {
    if (isSolved && currentStatus != CheckStatus.Solved) {
      return true;
    }
    else if (!isSolved) {
      try {
        if (task instanceof EduTask) {
          String language = task.getCourse().getLanguageID();
          StepikWrappers.Reply reply = getLastSubmission(String.valueOf(stepId), isSolved, language);
          if (reply != null && !reply.solution.isEmpty()) {
            return true;
          }
        }
        else {
          HashMap<String, String> solution = getSolutionForStepikAssignment(task, isSolved);
          if (!solution.isEmpty()) {
            return true;
          }
        }
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
    }

    return false;
  }

  /**
   * @return true if solutions for given task are incompatible with current plugin version, false otherwise
   */
  private static boolean loadSolution(@NotNull Project project, @NotNull Task task, boolean isSolved) {
    try {
      TaskSolutions taskSolutions = loadSolutionTexts(task, isSolved);
      if (!taskSolutions.hasIncompatibleSolutions && !taskSolutions.solutions.isEmpty()) {
        updateFiles(project, task, taskSolutions.solutions);
      }
      return taskSolutions.hasIncompatibleSolutions;
    }
    catch (IOException e) {
      LOG.warn("Failed to load task solutions", e);
      return false;
    }
  }

  private static TaskSolutions loadSolutionTexts(@NotNull Task task, boolean isSolved) throws IOException {
    if (task.isToSubmitToStepik()) {
      return getEduTaskSolution(task, isSolved);
    }
    else {
      return new TaskSolutions(getStepikTaskSolution(task, isSolved));
    }
  }

  private static HashMap<String, String> getStepikTaskSolution(@NotNull Task task, boolean isSolved) throws IOException {
    HashMap<String, String> solutions = getSolutionForStepikAssignment(task, isSolved);
    if (!solutions.isEmpty()) {
      for (Map.Entry<String, String> entry : solutions.entrySet()) {
        String solutionWithoutEduPrefix = removeEduPrefix(task, entry.getValue());
        solutions.put(entry.getKey(), solutionWithoutEduPrefix);
      }
      task.setStatus(isSolved ? CheckStatus.Solved : CheckStatus.Failed);
    }
    return solutions;
  }

  private static TaskSolutions getEduTaskSolution(@NotNull Task task, boolean isSolved) throws IOException {
    String language = task.getCourse().getLanguageID();
    StepikWrappers.Reply reply = getLastSubmission(String.valueOf(task.getStepId()), isSolved, language);
    if (reply == null || reply.solution == null || reply.solution.isEmpty()) {
      // https://youtrack.jetbrains.com/issue/EDU-1449
      if (reply != null && reply.solution == null) {
        LOG.warn(String.format("`solution` field of reply object is null for task `%s`", task.getName()));
      }
      task.setStatus(CheckStatus.Unchecked);
      return TaskSolutions.EMPTY;
    }

    if (reply.version > EduVersions.JSON_FORMAT_VERSION) {
      // TODO: show notification with suggestion to update plugin
      LOG.warn(String.format("The plugin supports versions of submission reply not greater than %d. The current version is `%d`",
                             EduVersions.JSON_FORMAT_VERSION, reply.version));
      return TaskSolutions.INCOMPATIBLE;
    }

    String serializedTask = reply.edu_task;
    if (serializedTask == null) {
      task.setStatus(checkStatus(isSolved));
      return new TaskSolutions(loadSolutionTheOldWay(task, reply));
    }

    StepikWrappers.TaskWrapper updatedTask = new GsonBuilder()
      .registerTypeAdapter(Task.class, new StepikSubmissionTaskAdapter(reply.version, language))
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create()
      .fromJson(serializedTask, StepikWrappers.TaskWrapper.class);

    if (updatedTask == null || updatedTask.task == null) {
      return TaskSolutions.EMPTY;
    }

    task.setStatus(checkStatus(isSolved));

    Map<String, String> taskFileToText = new HashMap<>();
    for (StepikWrappers.SolutionFile file : reply.solution) {
      TaskFile taskFile = task.getTaskFile(file.name);
      TaskFile updatedTaskFile = updatedTask.task.getTaskFile(file.name);
      if (taskFile != null && updatedTaskFile != null) {
        setPlaceholders(taskFile, updatedTaskFile);
        taskFileToText.put(file.name, removeAllTags(file.text));
      }
    }
    return new TaskSolutions(taskFileToText);
  }

  private static void setPlaceholders(@NotNull TaskFile taskFile, @NotNull TaskFile updatedTaskFile) {
    List<AnswerPlaceholder> answerPlaceholders = taskFile.getAnswerPlaceholders();
    List<AnswerPlaceholder> updatedPlaceholders = updatedTaskFile.getAnswerPlaceholders();
    for (int i = 0; i < answerPlaceholders.size(); i++) {
      AnswerPlaceholder answerPlaceholder = answerPlaceholders.get(i);
      AnswerPlaceholder updatedPlaceholder = updatedPlaceholders.get(i);
      answerPlaceholder.setHints(updatedPlaceholder.getHints());
      answerPlaceholder.setPossibleAnswer(updatedPlaceholder.getPossibleAnswer());
      answerPlaceholder.setPlaceholderText(updatedPlaceholder.getPlaceholderText());
      answerPlaceholder.setStatus(updatedPlaceholder.getStatus());
      answerPlaceholder.setOffset(updatedPlaceholder.getOffset());
      answerPlaceholder.setLength(updatedPlaceholder.getLength());
      answerPlaceholder.setSelected(updatedPlaceholder.getSelected());
    }
  }

  /**
   * Before we decided to store the information about placeholders as a separate field of Stepik reply{@link StepikWrappers.Reply#edu_task},
   * we used to pass full text of task file marking placeholders with <placeholder> </placeholder> tags
   */
  private static Map<String, String> loadSolutionTheOldWay(@NotNull Task task, @NotNull StepikWrappers.Reply reply) {
    HashMap<String, String> taskFileToText = new HashMap<>();
    List<StepikWrappers.SolutionFile> solutionFiles = reply.solution;
    if (solutionFiles.isEmpty()) {
      task.setStatus(CheckStatus.Unchecked);
      return taskFileToText;
    }

    for (StepikWrappers.SolutionFile file : solutionFiles) {
      TaskFile taskFile = task.getTaskFile(file.name);
      if (taskFile != null) {
        if (setPlaceholdersFromTags(taskFile, file)) {
          taskFileToText.put(file.name, removeAllTags(file.text));
        }
        else {
          taskFileToText.put(file.name, file.text);
        }
      }
    }

    return taskFileToText;
  }

  private static String removeEduPrefix(@NotNull Task task, String solution) {
    Language language = task.getLesson().getCourse().getLanguageById();
    String commentPrefix = LanguageCommenters.INSTANCE.forLanguage(language).getLineCommentPrefix();
    if (solution.contains(commentPrefix + EDU_TOOLS_COMMENT)) {
      return solution.replace(commentPrefix + EDU_TOOLS_COMMENT, "");
    }
    return solution;
  }

  private static void updateFiles(@NotNull Project project, @NotNull Task task, Map<String, String> solutionText) {
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return;
    }
    ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
      for (TaskFile taskFile : task.getTaskFiles().values()) {
        VirtualFile vFile = EduUtils.findTaskFileInDir(taskFile, taskDir);
        if (vFile != null) {
          try {
            taskFile.setTrackChanges(false);
            VfsUtil.saveText(vFile, solutionText.get(taskFile.getName()));
            SaveAndSyncHandler.getInstance().refreshOpenFiles();
            taskFile.setTrackChanges(true);
          }
          catch (IOException e) {
            LOG.warn(e.getMessage());
          }
        }
      }
    }));
  }

  @Override
  public void dispose() {
    myBusConnection.disconnect();
    cancelUnfinishedTasks();
  }

  @TestOnly
  public void doLoadSolution(Task task, boolean isSolved) {
    loadSolution(myProject, task, isSolved);
  }

  private static void updateUI(@NotNull Project project, @NotNull Task task) {
    ProjectView.getInstance(project).refresh();
    TaskDescriptionView.getInstance(project).setCurrentTask(task);
    NavigationUtils.navigateToTask(project, task);
  }

  private static class TaskSolutions {

    public static final TaskSolutions EMPTY = new TaskSolutions(Collections.emptyMap());
    public static final TaskSolutions INCOMPATIBLE = new TaskSolutions(Collections.emptyMap(), true);

    public final Map<String, String> solutions;
    public final boolean hasIncompatibleSolutions;

    public TaskSolutions(@NotNull Map<String, String> solutions, boolean hasIncompatibleSolutions) {
      this.solutions = solutions;
      this.hasIncompatibleSolutions = hasIncompatibleSolutions;
    }

    public TaskSolutions(@NotNull Map<String, String> solutions) {
      this(solutions, false);
    }
  }
}
