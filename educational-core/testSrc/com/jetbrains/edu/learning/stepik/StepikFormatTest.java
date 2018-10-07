package com.jetbrains.edu.learning.stepik;

import com.google.gson.*;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduTestCase;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.serialization.converter.TaskRoots;
import com.jetbrains.edu.learning.serialization.converter.TaskRootsKt;
import com.jetbrains.edu.learning.stepik.serialization.StepikSubmissionTaskAdapter;
import kotlin.collections.CollectionsKt;
import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.jetbrains.edu.learning.stepik.StepikNames.PYCHARM_PREFIX;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class StepikFormatTest extends EduTestCase {

  @NotNull
  @Override
  protected String getTestDataPath() {
    return "testData/stepik";
  }

  public void testFirstVersion() throws IOException {
    doStepOptionsCreationTest();
  }

  public void testSecondVersion() throws IOException {
    doStepOptionsCreationTest();
  }

  public void testThirdVersion() throws IOException {
    doStepOptionsCreationTest();
  }

  public void testFifthVersion() throws IOException {
    StepikWrappers.StepOptions options = getStepOptions();
    assertEquals(1, options.additionalFiles.size());
    AdditionalFile file = options.additionalFiles.get("additional_file.txt");
    assertNotNull(file);
    assertEquals("some text", file.getText());
    assertEquals(true, file.isVisible());
  }

  public void testSixthVersion() throws IOException {
    for (Map.Entry<String, TaskRoots> entry : TaskRootsKt.LANGUAGE_TASK_ROOTS.entrySet()) {
      checkSixthVersion(entry.getKey(), startsWith(entry.getValue().getTaskFilesRoot()), startsWith(entry.getValue().getTestFilesRoot()));
    }
    Matcher<String> pathMatcher = not(containsString("/"));
    checkSixthVersion(EduNames.PYTHON, pathMatcher, pathMatcher);
  }

  private void checkSixthVersion(@NotNull String language,
                                 @NotNull Matcher<String> srcPathMatcher,
                                 @NotNull Matcher<String> testPathMatcher) throws IOException {
    StepikWrappers.StepOptions options = getStepOptions(language);
    assertEquals(3, options.files.size());
    TaskFile taskFile = options.files.get(0);
    assertThat(taskFile.getName(), srcPathMatcher);
    assertEquals(1, taskFile.getAnswerPlaceholders().size());
    AnswerPlaceholderDependency dependency = taskFile.getAnswerPlaceholders().get(0).getPlaceholderDependency();
    assertThat(dependency.getFileName(), srcPathMatcher);
    assertEquals(1, options.test.size());
    assertThat(options.test.get(0).name, testPathMatcher);
  }

  public void testAdditionalMaterialsLesson() throws IOException {
    String responseString = loadJsonText();
    Lesson lesson =
        StepikClient.deserializeStepikResponse(StepikWrappers.LessonContainer.class, responseString, null).lessons.get(0);
    assertEquals(EduNames.ADDITIONAL_MATERIALS, lesson.getName());
  }

  public void testAdditionalMaterialsStep() throws IOException {
    String responseString = loadJsonText();
    for (String language : Arrays.asList(EduNames.KOTLIN, EduNames.PYTHON)) {
      StepikWrappers.StepSource step = StepikClient.deserializeStepikResponse(StepikWrappers.StepContainer.class,
                                                                              responseString,
                                                                              createParams(language)).steps.get(0);
      assertEquals(EduNames.ADDITIONAL_MATERIALS, step.block.options.title);
      assertEquals("task_file.py", step.block.options.files.get(0).getName());
      assertEquals("test_helperq.py", step.block.options.test.get(0).name);
    }
  }

  public void testAvailableCourses() throws IOException {
    String responseString = loadJsonText();
    StepikWrappers.CoursesContainer container =
      StepikClient.deserializeStepikResponse(StepikWrappers.CoursesContainer.class, responseString, null);
    assertNotNull(container.courses);
    assertEquals("Incorrect number of courses", 4, container.courses.size());
  }

  public void testPlaceholderSerialization() throws IOException {
    final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    AnswerPlaceholder answerPlaceholder = new AnswerPlaceholder();
    answerPlaceholder.setOffset(1);
    answerPlaceholder.setLength(10);
    answerPlaceholder.setPlaceholderText("type here");
    answerPlaceholder.setPossibleAnswer("answer1");
    answerPlaceholder.setHints(ContainerUtil.list("hint 1", "hint 2"));
    final String placeholderSerialization = gson.toJson(answerPlaceholder);
    String expected  = loadJsonText();
    JsonObject object = new JsonParser().parse(expected).getAsJsonObject();
    assertEquals(gson.toJson(gson.fromJson(object, AnswerPlaceholder.class)), placeholderSerialization);

  }

  public void testTokenUptoDate() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.AuthorWrapper wrapper = gson.fromJson(jsonText, StepikWrappers.AuthorWrapper.class);
    assertNotNull(wrapper);
    assertFalse(wrapper.users.isEmpty());
    StepicUserInfo user = wrapper.users.get(0);
    assertNotNull(user);
    assertFalse(user.isGuest());
  }

  public void testCourseAuthor() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.AuthorWrapper wrapper = gson.fromJson(jsonText, StepikWrappers.AuthorWrapper.class);
    assertNotNull(wrapper);
    assertFalse(wrapper.users.isEmpty());
    StepicUserInfo user = wrapper.users.get(0);
    assertNotNull(user);
    assertFalse(user.isGuest());
  }

  public void testSections() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.SectionContainer sectionContainer = gson.fromJson(jsonText, StepikWrappers.SectionContainer.class);
    assertNotNull(sectionContainer);
    assertEquals(1, sectionContainer.sections.size());
    List<Integer> unitIds = sectionContainer.sections.get(0).units;
    assertEquals(10, unitIds.size());
  }

  public void testUnit() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.UnitContainer unit = gson.fromJson(jsonText, StepikWrappers.UnitContainer.class);
    assertNotNull(unit);
    assertEquals(1, unit.units.size());
    final int lesson = unit.units.get(0).lesson;
    assertEquals(13416, lesson);
  }

  public void testLesson() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.LessonContainer lessonContainer = gson.fromJson(jsonText, StepikWrappers.LessonContainer.class);
    assertNotNull(lessonContainer);
    assertEquals(1, lessonContainer.lessons.size());
    final Lesson lesson = lessonContainer.lessons.get(0);
    assertNotNull(lesson);
  }

  public void testStep() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.StepContainer stepContainer = gson.fromJson(jsonText, StepikWrappers.StepContainer.class);
    assertNotNull(stepContainer);
    final StepikWrappers.StepSource step = stepContainer.steps.get(0);
    assertNotNull(step);
  }

  public void testStepBlock() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.StepContainer stepContainer = gson.fromJson(jsonText, StepikWrappers.StepContainer.class);
    final StepikWrappers.StepSource step = stepContainer.steps.get(0);
    final StepikWrappers.Step block = step.block;
    assertNotNull(block);
    assertNotNull(block.options);
    assertTrue(block.name.startsWith(PYCHARM_PREFIX));
  }

  public void testStepBlockOptions() throws IOException {
    final StepikWrappers.StepOptions options = getStepOptions();
    assertNotNull(options);
  }

  public void testUpdateDate() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.StepContainer stepContainer = gson.fromJson(jsonText, StepikWrappers.StepContainer.class);
    final StepikWrappers.StepSource step = stepContainer.steps.get(0);
    assertNotNull(step.update_date);
  }

  public void testOptionsTitle() throws IOException {
    final StepikWrappers.StepOptions options = getStepOptions();
    assertEquals("Our first program", options.title);
  }

  public void testOptionsTest() throws IOException {
    final StepikWrappers.StepOptions options = getStepOptions();
    final List<StepikWrappers.FileWrapper> testWrapper = options.test;
    assertNotNull(testWrapper);
    assertEquals(1, testWrapper.size());
    assertEquals("tests.py", testWrapper.get(0).name);
    assertNotNull(testWrapper.get(0).text);
  }

  public void testOptionsDescription() throws IOException {
    final StepikWrappers.StepOptions options = getStepOptions();

    assertEquals("\n" +
        "Traditionally the first program you write in any programming language is <code>\"Hello World!\"</code>.\n" +
        "<br><br>\n" +
        "Introduce yourself to the World.\n" +
        "<br><br>\n" +
        "Hint: To run a script сhoose 'Run &lt;name&gt;' on the context menu. <br>\n" +
        "For more information visit <a href=\"https://www.jetbrains.com/help/pycharm/running-and-rerunning-applications.html\">our help</a>.\n" +
        "\n" +
        "<br>\n", options.descriptionText);
  }

  public void testOptionsFeedbackLinks() throws IOException {
    StepikWrappers.StepOptions stepOptions = getStepOptions();
    assertEquals(FeedbackLink.LinkType.CUSTOM, stepOptions.myFeedbackLink.getType());
  }

  public void testOptionsFiles() throws IOException {
    final StepikWrappers.StepOptions options = getStepOptions();

    final List<TaskFile> files = options.files;
    assertEquals(1, files.size());
    final TaskFile taskFile = files.get(0);
    assertEquals("hello_world.py", taskFile.getName());
    assertEquals("print(\"Hello, world! My name is type your name\")\n", taskFile.getText());
  }

  private StepikWrappers.StepOptions getStepOptions() throws IOException {
    return getStepOptions(null);
  }

  private StepikWrappers.StepOptions getStepOptions(@Nullable String language) throws IOException {
    Gson gson = getGson(createParams(language));
    String jsonText = loadJsonText();
    final StepikWrappers.StepContainer stepContainer = gson.fromJson(jsonText, StepikWrappers.StepContainer.class);
    final StepikWrappers.StepSource step = stepContainer.steps.get(0);
    final StepikWrappers.Step block = step.block;
    return block.options;
  }

  public void testOptionsPlaceholder() throws IOException {
    final StepikWrappers.StepOptions options = getStepOptions();
    final List<TaskFile> files = options.files;
    final TaskFile taskFile = files.get(0);

    final List<AnswerPlaceholder> placeholders = taskFile.getAnswerPlaceholders();
    assertEquals(1, placeholders.size());
    final int offset = placeholders.get(0).getOffset();
    assertEquals(32, offset);
    final int length = placeholders.get(0).getLength();
    assertEquals(14, length);
    assertEquals("type your name", taskFile.getText().substring(offset, offset + length));
  }

  public void testTaskStatuses() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    StepikWrappers.ProgressContainer progressContainer = gson.fromJson(jsonText, StepikWrappers.ProgressContainer.class);
    assertNotNull(progressContainer);
    List<StepikWrappers.ProgressContainer.Progress> progressList = progressContainer.progresses;
    assertNotNull(progressList);
    final Boolean[] statuses = progressList.stream().map(progress -> progress.isPassed).toArray(Boolean[]::new);
    assertNotNull(statuses);
    assertEquals(50, statuses.length);
  }

  public void testLastSubmission() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    StepikWrappers.SubmissionsWrapper submissionsWrapper = gson.fromJson(jsonText, StepikWrappers.SubmissionsWrapper.class);
    assertNotNull(submissionsWrapper);
    assertNotNull(submissionsWrapper.submissions);
    assertEquals(20, submissionsWrapper.submissions.length);
    final StepikWrappers.Reply reply = submissionsWrapper.submissions[0].reply;
    assertNotNull(reply);
    List<StepikWrappers.SolutionFile> solutionFiles = reply.solution;
    assertEquals(1, solutionFiles.size());
    assertEquals("hello_world.py", solutionFiles.get(0).name);
    assertEquals("print(\"Hello, world! My name is type your name\")\n", solutionFiles.get(0).text);
  }

  public void testReplyMigration() throws IOException {
    String jsonText = loadJsonText();
    for (Map.Entry<String, TaskRoots> entry : TaskRootsKt.LANGUAGE_TASK_ROOTS.entrySet()) {
      Matcher<String> pathMatcher = startsWith(entry.getValue().getTaskFilesRoot());
      checkReply(jsonText, entry.getKey(), pathMatcher);
    }

    checkReply(jsonText, EduNames.PYTHON, not(containsString("/")));
  }

  private static void checkReply(@NotNull String jsonText, @NotNull String language, @NotNull Matcher<String> pathMatcher) {
    Gson gson = getGson(createParams(language));
    StepikWrappers.Reply reply = gson.fromJson(jsonText, StepikWrappers.Reply.class);
    assertEquals(1, reply.solution.size());
    assertThat(reply.solution.get(0).name, pathMatcher);

    Gson taskGson = new GsonBuilder()
      .registerTypeAdapter(Task.class, new StepikSubmissionTaskAdapter(reply.version, language))
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create();

    StepikWrappers.TaskWrapper taskWrapper = taskGson.fromJson(reply.edu_task, StepikWrappers.TaskWrapper.class);
    Map.Entry<String, TaskFile> taskFileEntry = CollectionsKt.first(taskWrapper.task.getTaskFiles().entrySet());
    assertThat(taskFileEntry.getKey(), pathMatcher);
    TaskFile taskFile = taskFileEntry.getValue();
    assertThat(taskFile.getName(), pathMatcher);
    AnswerPlaceholderDependency dependency = taskFile.getAnswerPlaceholders().get(0).getPlaceholderDependency();
    assertThat(dependency.getFileName(), pathMatcher);
  }

  public void testNonEduTasks() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepikWrappers.StepContainer stepContainer = gson.fromJson(jsonText, StepikWrappers.StepContainer.class);
    assertNotNull(stepContainer);
    assertNotNull(stepContainer.steps);
    assertEquals(3, stepContainer.steps.size());
  }

  @NotNull
  private static Gson getGson() {
    return getGson(null);
  }

  @NotNull
  private static Gson getGson(@Nullable Map<Key, Object> params) {
    return StepikClient.createGson(params);
  }

  @NotNull
  private String loadJsonText() throws IOException {
    return FileUtil.loadFile(new File(getTestDataPath(), getTestFile()));
  }

  private StepikWrappers.StepOptions doStepOptionsCreationTest() throws IOException {
    String responseString = loadJsonText();
    StepikWrappers.StepSource stepSource =
        StepikClient.deserializeStepikResponse(StepikWrappers.StepContainer.class, responseString, null).steps.get(0);
    StepikWrappers.StepOptions options = stepSource.block.options;
    List<TaskFile> files = options.files;
    assertEquals("Wrong number of task files", 1, files.size());
    List<AnswerPlaceholder> placeholders = files.get(0).getAnswerPlaceholders();
    assertEquals("Wrong number of placeholders", 1, placeholders.size());
    AnswerPlaceholder placeholder = placeholders.get(0);
    assertEquals(Collections.singletonList("Type your name here."), placeholder.getHints());
    assertEquals("Liana", placeholder.getPossibleAnswer());
    assertEquals("Description", options.descriptionText);
    return options;
  }

  @NotNull
  private String getTestFile() {
    return getTestName(true) + ".json";
  }

  private static Map<Key, Object> createParams(@Nullable String language) {
    return language == null ? null : Collections.singletonMap(StepikConnector.COURSE_LANGUAGE, language);
  }
}
