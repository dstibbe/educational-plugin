package com.jetbrains.edu.learning.serialization;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.hash.HashMap;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.authUtils.TokenInfo;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.*;
import com.jetbrains.edu.learning.serialization.converter.json.JsonLocalCourseConverter;
import com.jetbrains.edu.learning.serialization.converter.json.local.To8VersionLocalCourseConverter;
import com.jetbrains.edu.learning.serialization.converter.json.local.ToSeventhVersionLocalCourseConverter;
import com.jetbrains.edu.learning.serialization.converter.xml.*;
import com.jetbrains.edu.learning.stepik.StepikNames;
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SerializationUtils {
  private static final Logger LOG = Logger.getInstance(SerializationUtils.class);

  public static final String LINE = "line";
  public static final String START = "start";
  public static final String LENGTH = "length";
  public static final String HINT = "hint";
  public static final String ADDITIONAL_HINTS = "additional_hints";
  public static final String OFFSET = "offset";
  public static final String LESSONS = "lessons";
  public static final String ITEMS = "items";
  public static final String COURSE = "course";
  public static final String ID = "id";
  public static final String COURSE_TITLED = "Course";
  public static final String STATUS = "status";
  public static final String AUTHOR = "author";
  public static final String AUTHORS = "authors";
  public static final String MY_INITIAL_START = "myInitialStart";
  public static final String SUBTASK_MARKER = "_subtask";

  private SerializationUtils() {
  }

  public static class Xml {
    public static final List<Class<? extends Course>> COURSE_ELEMENT_TYPES = Lists.newArrayList(RemoteCourse.class, CheckiOCourse.class,
                                                                                                HyperskillCourse.class, Course.class);

    public final static String COURSE_ELEMENT = "courseElement";
    public final static String MAIN_ELEMENT = "StudyTaskManager";
    public final static String REMOTE_COURSE = "RemoteCourse";
    public static final String CHECKIO_COURSE = "CheckiOCourse";
    public static final String SECTION = "Section";
    public static final String LESSON = "Lesson";
    public static final String FRAMEWORK_LESSON = "FrameworkLesson";
    public static final String MAP = "map";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String NAME = "name";
    public static final String CUSTOM_NAME = "customPresentableName";
    public static final String LIST = "list";
    public static final String OPTION = "option";
    public static final String INDEX = "index";
    public static final String STUDY_STATUS_MAP = "myStudyStatusMap";
    public static final String TASK_STATUS_MAP = "myTaskStatusMap";
    public static final String LENGTH = "length";
    public static final String ANSWER_PLACEHOLDERS = "answerPlaceholders";
    public static final String TASK_LIST = "taskList";
    public static final String TASK_FILES = "taskFiles";
    public static final String INITIAL_STATE = "initialState";
    public static final String MY_INITIAL_STATE = "MyInitialState";
    public static final String MY_LINE = "myLine";
    public static final String MY_START = "myStart";
    public static final String MY_LENGTH = "myLength";
    public static final String HINT = "hint";
    public static final String AUTHOR_TITLED = "Author";
    public static final String FIRST_NAME = "first_name";
    public static final String SECOND_NAME = "second_name";
    public static final String MY_INITIAL_LINE = "myInitialLine";
    public static final String MY_INITIAL_LENGTH = "myInitialLength";
    public static final String ANSWER_PLACEHOLDER = "AnswerPlaceholder";
    public static final String TASK_WINDOWS = "taskWindows";
    public static final String RESOURCE_PATH = "resourcePath";
    public static final String COURSE_DIRECTORY = "courseDirectory";
    public static final String SUBTASK_INFO = "AnswerPlaceholderSubtaskInfo";
    public static final String SUBTASK_INFOS = "subtaskInfos";
    public static final String ADDITIONAL_HINTS = "additionalHints";
    public static final String POSSIBLE_ANSWER = "possibleAnswer";
    public static final String SELECTED = "selected";
    public static final String TASK_TEXT = "taskText";
    public static final String PLACEHOLDER_TEXT = "placeholderText";
    public static final String LAST_SUBTASK_INDEX = "lastSubtaskIndex";
    public static final String THEORY_TAG = "theoryTask";
    public static final String ADAPTIVE_TASK_PARAMETERS = "adaptiveTaskParameters";
    public static final String ADAPTIVE = "adaptive";
    public static final String PYCHARM_TASK = "PyCharmTask";
    public static final String EDU_TASK = "EduTask";
    public static final String TASK_WITH_SUBTASKS = "TaskWithSubtasks";
    public static final String THEORY_TASK = "TheoryTask";
    public static final String CHOICE_TASK = "ChoiceTask";
    public static final String CODE_TASK = "CodeTask";
    public static final String TASK_TEXTS = "taskTexts";
    public static final String HINTS = "hints";
    public static final String DESCRIPTION_TEXT = "descriptionText";
    public static final String DESCRIPTION_FORMAT = "descriptionFormat";
    public static final String ADDITIONAL_FILE = "AdditionalFile";
    public static final String ADDITIONAL_FILES = "additionalFiles";
    public static final String TEXT = "text";
    public static final String VISIBLE = "visible";
    public static final String TEST_FILES = "testsText";
    public static final String LANGUAGE = "language";
    public static final String PLACEHOLDER_DEPENDENCY = "placeholderDependency";
    public static final String DEPENDENCY_FILE_NAME = "fileName";
    public static final String SETTINGS_NAME = "EduSettings";
    public static final String USER = "user";
    public static final String LAST_TIME_CHECKED = "lastTimeChecked";
    public static final String USE_JAVA_FX = "shouldUseJavaFx";
    public static final String STEPIK_USER = "StepikUser";

    private Xml() {
    }

    public static int getVersion(Element element) throws StudyUnrecognizedFormatException {
      if (element.getChild(COURSE_ELEMENT) != null) {
        return 1;
      }

      final Element taskManager = element.getChild(MAIN_ELEMENT);

      Element versionElement = getChildWithName(taskManager, "VERSION");
      if (versionElement == null) {
        return -1;
      }

      return Integer.valueOf(versionElement.getAttributeValue(VALUE));
    }

    @NotNull
    public static Element convertToSecondVersion(@NotNull Project project,
                                                 @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToSecondVersionXmlConverter().convert(project, element);
    }

    @NotNull
    public static Element convertToThirdVersion(@NotNull Project project,
                                                @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToThirdVersionXmlConverter().convert(project, element);
    }

    @NotNull
    public static Element convertToFourthVersion(@NotNull Project project,
                                                 @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToFourthVersionXmlConverter().convert(project, element);
    }

    @NotNull
    public static Element convertToFifthVersion(@NotNull Project project,
                                                @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToFifthVersionXmlConverter().convert(project, element);
    }

    @NotNull
    public static Element convertToSixthVersion(@NotNull Project project,
                                                @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToSixthVersionXmlConverter().convert(project, element);
    }

    @NotNull
    public static Element convertToSeventhVersion(@NotNull Project project,
                                                  @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToSeventhVersionXmlConverter().convert(project, element);
    }

    @NotNull
    public static Element convertToEighthVersion(@NotNull Project project,
                                                 @NotNull Element state) throws StudyUnrecognizedFormatException {
      return new ToEighthVersionXmlConverter().convert(project, state);
    }

    @NotNull
    public static Element convertToNinthVersion(@NotNull Project project,
                                                @NotNull Element state) throws StudyUnrecognizedFormatException {
      return new ToNinthVersionXmlConverter().convert(project, state);
    }

    @NotNull
    public static Element convertToTenthVersion(@NotNull Project project,
                                                @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToTenthVersionXmlConverter().convert(project, element);
    }

    @NotNull
    public static Element convertToEleventhVersion(@NotNull Project project,
                                                   @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToEleventhVersionXmlConverter().convert(project, element);
    }

    @Nullable
    public static VirtualFile getTaskDir(@NotNull Project project, @NotNull Element lesson, @NotNull Element task)
      throws StudyUnrecognizedFormatException {
      final VirtualFile lessonDir = project.getBaseDir().findChild(EduNames.LESSON + getAsInt(lesson, INDEX));
      if (lessonDir == null) return null;
      VirtualFile taskDir = lessonDir.findChild(EduNames.TASK + getAsInt(task, INDEX));
      if (taskDir == null) {
        return null;
      }
      VirtualFile srcDir = taskDir.findChild(EduNames.SRC);
      if (srcDir != null) {
        taskDir = srcDir;
      }
      return taskDir;
    }

    public static String addStatus(XMLOutputter outputter,
                                   Map<String, String> placeholderTextToStatus,
                                   String taskStatus,
                                   Element placeholder) {
      String placeholderText = outputter.outputString(placeholder);
      String status = placeholderTextToStatus.get(placeholderText);
      if (status != null) {
        addChildWithName(placeholder, STATUS, status);
        if (taskStatus == null || status.equals(CheckStatus.Failed.toString())) {
          taskStatus = status;
        }
      }
      return taskStatus;
    }

    public static void addInitialState(Document document, Element placeholder) throws StudyUnrecognizedFormatException {
      Element initialState = getChildWithName(placeholder, INITIAL_STATE).getChild(MY_INITIAL_STATE);
      int initialLine = getAsInt(initialState, MY_LINE);
      int initialStart = getAsInt(initialState, MY_START);
      int initialOffset = document.getLineStartOffset(initialLine) + initialStart;
      addChildWithName(initialState, OFFSET, initialOffset);
      renameElement(getChildWithName(initialState, MY_LENGTH), LENGTH);
    }

    public static void addOffset(Document document, Element placeholder) throws StudyUnrecognizedFormatException {
      int line = getAsInt(placeholder, LINE);
      int start = getAsInt(placeholder, START);
      int offset = document.getLineStartOffset(line) + start;
      addChildWithName(placeholder, OFFSET, offset);
    }

    public static int getAsInt(Element element, String name) throws StudyUnrecognizedFormatException {
      return Integer.valueOf(getChildWithName(element, name).getAttributeValue(VALUE));
    }

    public static String getAsString(@NotNull Element element, @NotNull String name) throws StudyUnrecognizedFormatException {
      return getChildWithName(element, name).getAttributeValue(VALUE);
    }

    public static void incrementIndex(Element element) throws StudyUnrecognizedFormatException {
      Element index = getChildWithName(element, INDEX);
      int indexValue = Integer.parseInt(index.getAttributeValue(VALUE));
      changeValue(index, indexValue + 1);
    }

    public static void renameElement(Element element, String newName) {
      element.setAttribute(NAME, newName);
    }

    public static void changeValue(Element element, Object newValue) {
      element.setAttribute(VALUE, newValue.toString());
    }

    public static Element addChildWithName(Element parent, String name, Element value) {
      Element child = new Element(OPTION);
      child.setAttribute(NAME, name);
      child.addContent(value);
      parent.addContent(child);
      return value;
    }

    public static Element addChildWithName(Element parent, String name, Object value) {
      Element child = new Element(OPTION);
      child.setAttribute(NAME, name);
      child.setAttribute(VALUE, value.toString());
      parent.addContent(child);
      return child;
    }

    public static Element addChildList(Element parent, String name, List<Element> elements) {
      Element listElement = new Element(LIST);
      for (Element element : elements) {
        listElement.addContent(element);
      }
      return addChildWithName(parent, name, listElement);
    }

    public static Element addChildMap(Element parent, String name, Map<String, Element> value) {
      Element mapElement = new Element(MAP);
      for (Map.Entry<String, Element> entry : value.entrySet()) {
        Element entryElement = new Element("entry");
        mapElement.addContent(entryElement);
        String key = entry.getKey();
        entryElement.setAttribute("key", key);
        Element valueElement = new Element("value");
        valueElement.addContent(entry.getValue());
        entryElement.addContent(valueElement);
      }
      return addChildWithName(parent, name, mapElement);
    }

    public static Element addTextChildMap(Element parent, String name, Map<String, String> value) {
      Element mapElement = new Element(MAP);
      for (Map.Entry<String, String> entry : value.entrySet()) {
        Element entryElement = new Element("entry");
        mapElement.addContent(entryElement);
        String key = entry.getKey();
        entryElement.setAttribute("key", key);
        entryElement.setAttribute("value", entry.getValue());
      }
      return addChildWithName(parent, name, mapElement);
    }

    public static List<Element> getChildList(Element parent, String name) throws StudyUnrecognizedFormatException {
      return getChildList(parent, name, false);
    }

    public static List<Element> getChildList(Element parent, String name, boolean optional) throws StudyUnrecognizedFormatException {
      Element listParent = getChildWithName(parent, name, optional);
      if (listParent != null) {
        Element list = listParent.getChild(LIST);
        if (list != null) {
          return list.getChildren();
        }
      }
      return Collections.emptyList();
    }

    public static Element getChildWithName(Element parent, String name) throws StudyUnrecognizedFormatException {
      return getChildWithName(parent, name, false);
    }

    public static Element getChildWithName(Element parent, String name, boolean optional) throws StudyUnrecognizedFormatException {
      for (Element child : parent.getChildren()) {
        Attribute attribute = child.getAttribute(NAME);
        if (attribute == null) {
          continue;
        }
        if (name.equals(attribute.getValue())) {
          return child;
        }
      }
      if (optional) {
        return null;
      }
      throw new StudyUnrecognizedFormatException();
    }

    public static <K, V> Map<K, V> getChildMap(Element element, String name) throws StudyUnrecognizedFormatException {
      return getChildMap(element, name, false);
    }

    public static <K, V> Map<K, V> getChildMap(Element element, String name, boolean optional) throws StudyUnrecognizedFormatException {
      Element mapParent = getChildWithName(element, name, optional);
      if (mapParent != null) {
        Element map = mapParent.getChild(MAP);
        if (map != null) {
          HashMap result = new HashMap();
          for (Element entry : map.getChildren()) {
            Object key = entry.getAttribute(KEY) == null ? entry.getChild(KEY).getChildren().get(0) : entry.getAttributeValue(KEY);
            Object value = entry.getAttribute(VALUE) == null ? entry.getChild(VALUE).getChildren().get(0) : entry.getAttributeValue(VALUE);
            result.put(key, value);
          }
          return result;
        }
      }
      return Collections.emptyMap();
    }

    /**
     * Suitable for course xml with version 5 or above
     *
     * @param taskManagerElement element with {@link MAIN_ELEMENT} tag
     * @return course element
     * @throws StudyUnrecognizedFormatException if course element can not be found
     */
    @NotNull
    public static Element getCourseElement(@NotNull Element taskManagerElement) throws StudyUnrecognizedFormatException {
      Element courseHolder = getChildWithName(taskManagerElement, COURSE);
      for (Class<? extends Course> elementType : COURSE_ELEMENT_TYPES) {
        Element courseElement = courseHolder.getChild(elementType.getSimpleName());
        if (courseElement != null) {
          return courseElement;
        }
      }
      throw new StudyUnrecognizedFormatException("Failed to find course element type. CourseHolder is:\n" +
                                                 new XMLOutputter().outputString(courseHolder));
    }
  }

  public static class Json {

    public static final String TASK_LIST = "task_list";
    public static final String TASK_FILES = "task_files";
    public static final String TASK_TEXTS = "task_texts";
    public static final String FILES = "files";
    public static final String TESTS = "test";
    public static final String TEXTS = "text";
    public static final String HINTS = "hints";
    public static final String SUBTASK_INFOS = "subtask_infos";
    public static final String FORMAT_VERSION = "format_version";
    public static final String INDEX = "index";
    public static final String TASK_TYPE = "task_type";
    public static final String NAME = "name";
    public static final String TITLE = "title";
    public static final String LAST_SUBTASK = "last_subtask_index";
    public static final String ITEMS = "items";
    public static final String ITEM_TYPE = "type";
    public static final String FRAMEWORK_TYPE = "framework";
    public static final String PLACEHOLDERS = "placeholders";
    public static final String POSSIBLE_ANSWER = "possible_answer";
    public static final String PLACEHOLDER_TEXT = "placeholder_text";
    public static final String SELECTED = "selected";
    public static final String FILE_WRAPPER_TEXT = "text";
    public static final String DESCRIPTION_TEXT = "description_text";
    public static final String DESCRIPTION_FORMAT = "description_format";
    public static final String ADDITIONAL_FILES = "additional_files";
    public static final String TEXT = "text";
    public static final String IS_VISIBLE = "is_visible";
    public static final String DEPENDENCY = "dependency";
    public static final String DEPENDENCY_FILE = "file";
    public static final String TEST_FILES = "test_files";
    public static final String VERSION = "version";
    public static final String COURSE_TYPE = "course_type";
    public static final String PROGRAMMING_LANGUAGE = "programming_language";

    private Json() {
    }

    public static class LessonSectionAdapter implements JsonDeserializer<StudyItem>, JsonSerializer<StudyItem> {

      @Override
      public JsonElement serialize(StudyItem item, Type type, JsonSerializationContext context) {
        Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Task.class, new TaskAdapter())
          .excludeFieldsWithoutExposeAnnotation().create();
        JsonElement tree = gson.toJsonTree(item);
        final JsonObject jsonItem = tree.getAsJsonObject();
        String itemType = EduNames.LESSON;
        if (item instanceof FrameworkLesson) {
          itemType = FRAMEWORK_TYPE;
        }
        else if (item instanceof Section) {
          itemType = EduNames.SECTION;
        }
        jsonItem.add(ITEM_TYPE, new JsonPrimitive(itemType));
        return jsonItem;
      }

      @Override
      public StudyItem deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(Task.class, new TaskAdapter()).registerTypeAdapter(StudyItem.class, new LessonSectionAdapter()).create();
        final StudyItem item = deserializeItem(json, gson);
        final String name = item.getName();
        if (StepikNames.PYCHARM_ADDITIONAL.equals(name)) {
          item.setName(EduNames.ADDITIONAL_MATERIALS);
        }
        return item;
      }

      private static StudyItem deserializeItem(@NotNull JsonElement json, @NotNull Gson gson) {
        JsonObject object = json.getAsJsonObject();
        if (!object.has(ITEM_TYPE)) {
          return gson.fromJson(object, Lesson.class);
        } else {
          String itemType = object.get(ITEM_TYPE).getAsString();
          switch (itemType) {
            case EduNames.LESSON: return gson.fromJson(object, Lesson.class);
            case FRAMEWORK_TYPE: return gson.fromJson(object, FrameworkLesson.class);
            case EduNames.SECTION: return gson.fromJson(object, Section.class);
            default: throw new IllegalArgumentException("Unsupported lesson type: " + itemType);
          }
        }
      }
    }

    public static class CourseAdapter implements JsonSerializer<Course>, JsonDeserializer<Course> {

      @Override
      public JsonElement serialize(Course src, Type typeOfSrc, JsonSerializationContext context) {
        final Gson gson = new GsonBuilder()
          .setPrettyPrinting()
          .excludeFieldsWithoutExposeAnnotation()
          .registerTypeAdapter(StudyItem.class, new SerializationUtils.Json.LessonSectionAdapter())
          .registerTypeAdapter(Task.class, new SerializationUtils.Json.TaskAdapter())
          .create();
        JsonElement element = gson.toJsonTree(src, typeOfSrc);
        if (element.isJsonObject()) {
          element.getAsJsonObject().addProperty(VERSION, EduVersions.JSON_FORMAT_VERSION);
        }
        return element;
      }

      @Override
      public Course deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonPrimitive jsonVersion = jsonObject.getAsJsonPrimitive(VERSION);
        int version;
        if (jsonVersion == null) {
          version = 1;
        } else {
          version = jsonVersion.getAsInt();
        }

        while (version < EduVersions.JSON_FORMAT_VERSION) {
          JsonLocalCourseConverter converter = null;
          switch (version) {
            case 6: converter = new ToSeventhVersionLocalCourseConverter(); break;
            case 7: converter = new To8VersionLocalCourseConverter(); break;
          }
          if (converter != null) {
            jsonObject = converter.convert(jsonObject);
          }
          version++;
        }
        Gson gson = new GsonBuilder()
          .registerTypeAdapter(Task.class, new SerializationUtils.Json.TaskAdapter())
          .registerTypeAdapter(StudyItem.class, new SerializationUtils.Json.LessonSectionAdapter())
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .create();
        return gson.fromJson(jsonObject, typeOfT);
      }
    }

    public static class TaskAdapter implements JsonSerializer<Task>, JsonDeserializer<Task> {

      @Override
      public JsonElement serialize(Task src, Type typeOfSrc, JsonSerializationContext context) {
        Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
        return serializeWithTaskType(src, gson);
      }

      @Override
      public Task deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
        return doDeserialize(json, gson);
      }
    }

    @NotNull
    public static JsonObject serializeWithTaskType(Task src, Gson gson) {
      JsonElement tree = gson.toJsonTree(src);
      final JsonObject task = tree.getAsJsonObject();
      task.add(TASK_TYPE, new JsonPrimitive(src.getTaskType()));
      return task;
    }

    @Nullable
    public static Task doDeserialize(JsonElement json, Gson gson) {
      final JsonObject object = json.getAsJsonObject();
      if (object.has(NAME) && StepikNames.PYCHARM_ADDITIONAL.equals(object.get(NAME).getAsString())) {
        object.remove(NAME);
        object.add(NAME, new JsonPrimitive(EduNames.ADDITIONAL_MATERIALS));
      }
      if (object.has(TASK_TYPE)) {
        final String taskType = object.get(TASK_TYPE).getAsString();
        switch (taskType) {
          case "ide": return gson.fromJson(object, IdeTask.class);
          case "choice": return gson.fromJson(object, ChoiceTask.class);
          case "theory": return gson.fromJson(object, TheoryTask.class);
          case "code": return gson.fromJson(object, CodeTask.class);
          case "edu": return gson.fromJson(object, EduTask.class);
          case "output": return gson.fromJson(object, OutputTask.class);
          case "pycharm": return gson.fromJson(object, EduTask.class);     // deprecated: old courses have pycharm tasks
          default: {
            LOG.warn("Unsupported task type " + taskType);
            return null;
          }
        }
      }
      LOG.warn("No task type found in json " + json.toString());
      return null;
    }
  }

  public static class TokenInfoDeserializer implements JsonDeserializer<TokenInfo> {

    @Override
    public TokenInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      final JsonObject jsonObject = json.getAsJsonObject();

      final String accessToken = jsonObject.get("access_token").getAsString();
      // we don't have refresh token in tests
      final String refreshToken = jsonObject.get("refresh_token") == null ? "" : jsonObject.get("refresh_token").getAsString();
      final long expiresIn = jsonObject.get("expires_in").getAsLong();
      final long expiringTime = expiresIn + (System.currentTimeMillis() / 1000);

      TokenInfo tokenInfo = new TokenInfo();
      tokenInfo.setRefreshToken(refreshToken);
      tokenInfo.setAccessToken(accessToken);
      tokenInfo.setExpiresIn(expiringTime);
      return tokenInfo;
    }
  }
}
