package com.jetbrains.edu.learning.ui.taskDescription;

import com.intellij.ide.BrowserUtil
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduLanguageDecorator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduUsagesCollector
import com.sun.webkit.dom.ElementImpl
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.scene.Scene
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.annotations.TestOnly
import org.jsoup.select.Elements
import org.w3c.dom.Element
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.*

com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_URL;
import static com.jetbrains.edu.learning.ui.taskDescription.TaskFontPropertiesKt.*;

public class BrowserWindow {
  private static final Logger LOG = Logger.getInstance(TaskDescriptionToolWindow.class);
  private static final String EVENT_TYPE_CLICK = "click";
  private static final Pattern IN_COURSE_LINK = Pattern.compile("#(\\w+)#(\\w+)#((\\w+)#)?");

  public static final String SRC_ATTRIBUTE = "src";
  private JFXPanel myPanel;
  private WebView myWebComponent;
  private StackPane myPane;

  private WebEngine myEngine;
  private final Project myProject;
  private boolean myLinkInNewBrowser;

  public BrowserWindow(@NotNull final Project project, final boolean linkInNewWindow) {
    myProject = project;
    myLinkInNewBrowser = linkInNewWindow;
    setPanel(new JFXPanel());
    initComponents();
  }

  void updateLaf(boolean isDarcula) {
    if (isDarcula) {
      updateLafDarcula();
    }
    else {
      updateIntellijAndGTKLaf();
    }
  }

  private void updateIntellijAndGTKLaf() {
    Platform.runLater(() -> {
      final URL scrollBarStyleUrl = getClass().getResource(SystemInfo.isWindows ? "/style/javaFXBrowserScrollBar_win.css" : "/style/javaFXBrowserScrollBar.css");
      final URL engineStyleUrl = getClass().getResource(getBrowserStylesheet(false));
      myEngine.setUserStyleSheetLocation(engineStyleUrl.toExternalForm());
      myPanel.getScene().getStylesheets().clear();
      myPanel.getScene().getStylesheets().addAll(engineStyleUrl.toExternalForm(), scrollBarStyleUrl.toExternalForm());
      myEngine.reload();
    });
  }

  private void updateLafDarcula() {
    Platform.runLater(() -> {
      final URL engineStyleUrl = getClass().getResource(getBrowserStylesheet(true));
      final URL scrollBarStyleUrl = getClass().getResource(SystemInfo.isWindows ? "/style/javaFXBrowserDarculaScrollBar_win.css" : "/style/javaFXBrowserDarculaScrollBar.css");
      myEngine.setUserStyleSheetLocation(engineStyleUrl.toExternalForm());
      myPanel.getScene().getStylesheets().clear();
      myPanel.getScene().getStylesheets().addAll(engineStyleUrl.toExternalForm(), scrollBarStyleUrl.toExternalForm());
      myPane.setStyle("-fx-background-color: #3c3f41");
      myEngine.reload();
    });
  }

  @NotNull
  public static String getBrowserStylesheet(boolean isDarcula) {
    if (SystemInfo.isMac) {
      return isDarcula ? "/style/javaFXBrowserDarcula_mac.css" : "/style/javaFXBrowser_mac.css";
    }

    if (SystemInfo.isWindows) {
      return isDarcula ? "/style/javaFXBrowserDarcula_win.css" : "/style/javaFXBrowser_win.css";
    }

    return isDarcula ? "/style/javaFXBrowserDarcula_linux.css" : "/style/browser.css";
  }

  private void initComponents() {
    Platform.runLater(() -> {
      Platform.setImplicitExit(false);
      myPane = new StackPane();
      myWebComponent = new WebView();
      myWebComponent.setOnDragDetected(event -> {});
      myEngine = myWebComponent.getEngine();

      myPane.getChildren().add(myWebComponent);
      if (myLinkInNewBrowser) {
        initHyperlinkListener();
      }
      Scene scene = new Scene(myPane);
      myPanel.setScene(scene);
      myPanel.setVisible(true);
      updateLaf(LafManager.getInstance().getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo);
    });
  }

  public void loadContent(@NotNull final String content) {
    Course course = StudyTaskManager.getInstance(myProject).getCourse();
    if (course == null) {
      return;
    }

    Task task = EduUtils.getCurrentTask(myProject);
    if (task == null) {
      Platform.runLater(() -> myEngine.loadContent(createHtmlWithCodeHighlighting(content, course)));
      return;
    }

    VirtualFile taskDir = task.getTaskDir(myProject);
    if (taskDir == null) {
      Platform.runLater(() -> myEngine.loadContent(createHtmlWithCodeHighlighting(content, course)));
      return;
    }

    Platform.runLater(() -> myEngine.loadContent(doProcessContent(content, taskDir, myProject)));
  }

  @TestOnly
  public static String processContent(@NotNull String content, @NotNull VirtualFile taskDir, Project project) {
    return doProcessContent(content, taskDir, project);
  }

  private static String doProcessContent(@NotNull String content, @NotNull VirtualFile taskDir, Project project) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return content;
    }

    String text = createHtmlWithCodeHighlighting(content, course);

    return absolutizeImgPaths(text, taskDir);
  }

  @NotNull
  private static String absolutizeImgPaths(@NotNull String withCodeHighlighting, @NotNull VirtualFile taskDir) {
    org.jsoup.nodes.Document document = Jsoup.parse(withCodeHighlighting);
    Elements imageElements = document.getElementsByTag("img");
    for (org.jsoup.nodes.Element imageElement : imageElements) {
      String imagePath = imageElement.attr(SRC_ATTRIBUTE);
      if (!BrowserUtil.isAbsoluteURL(imagePath)) {
        File file = new File(imagePath);
        String absolutePath = new File(taskDir.getPath(), file.getPath()).toURI().toString();
        imageElement.attr("src", absolutePath);
      }
    }
    return document.outerHtml();
  }

  @NotNull
  private static String createHtmlWithCodeHighlighting(@NotNull final String content, @NotNull Course course) {
    EduLanguageDecorator decorator = EduLanguageDecorator.INSTANCE.forLanguage(course.getLanguageById());
    if (decorator == null) return content;

    String template = null;
    ClassLoader classLoader = BrowserWindow.class.getClassLoader();
    InputStream stream = classLoader.getResourceAsStream("/style/template.html");
    try {
      template = StreamUtil.readText(stream, "utf-8");
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
    finally {
      try {
        stream.close();
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
    }

    if (template == null) {
      LOG.warn("Code mirror template is null");
      return content;
    }

    int bodyFontSize = bodyFontSize();
    int codeFontSize = codeFontSize();

    int bodyLineHeight = bodyLineHeight();
    int codeLineHeight = codeLineHeight();

    template = template.replace("${body_font_size}", String.valueOf(bodyFontSize));
    template = template.replace("${code_font_size}", String.valueOf(codeFontSize));
    template = template.replace("${body_line_height}", String.valueOf(bodyLineHeight));
    template = template.replace("${code_line_height}", String.valueOf(codeLineHeight));
    template = setResourcePath(template, "${codemirror}", "/code-mirror/codemirror.js");
    template = setResourcePath(template, "${jquery}", "/style/hint/jquery-1.9.1.js");
    template = template.replace("${language_script}", decorator.getLanguageScriptUrl());
    template = template.replace("${default_mode}", decorator.getDefaultHighlightingMode());
    template = setResourcePath(template, "${runmode}", "/code-mirror/runmode.js");
    template = setResourcePath(template, "${colorize}", "/code-mirror/colorize.js");
    template = setResourcePath(template, "${javascript}", "/code-mirror/javascript.js");
    if (LafManager.getInstance().getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo) {
      template = setResourcePath(template, "${css_oldcodemirror}", "/code-mirror/codemirror-old-darcula.css");
      template = setResourcePath(template, "${css_codemirror}", "/code-mirror/codemirror-darcula.css");
      template = setResourcePath(template, "${hint_base}", "/style/hint/base_darcula.css");
    }
    else {
      template = setResourcePath(template, "${hint_base}", "/style/hint/base.css");
      template = setResourcePath(template, "${css_oldcodemirror}", "/code-mirror/codemirror-old.css");
      template = setResourcePath(template, "${css_codemirror}", "/code-mirror/codemirror.css");
    }
    template = template.replace("${code}", content);

    return template;
  }

  private static String setResourcePath(@NotNull String template, @NotNull String name, @NotNull String recoursePath) {
    ClassLoader classLoader = BrowserWindow.class.getClassLoader();
    URL codemirrorScript = classLoader.getResource(recoursePath);
    if (codemirrorScript != null) {
      template = template.replace(name, codemirrorScript.toExternalForm());
    }
    else {
      LOG.warn("Resource not found: " + recoursePath);
    }
    return template;
  }

  private void initHyperlinkListener() {
    myEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED) {
        final EventListener listener = makeHyperLinkListener();

        addListenerToAllHyperlinkItems(listener);
      }
    });
  }

  private void addListenerToAllHyperlinkItems(EventListener listener) {
    final Document doc = myEngine.getDocument();
    if (doc != null) {
      final NodeList nodeList = doc.getElementsByTagName("a");
      for (int i = 0; i < nodeList.getLength(); i++) {
        ((EventTarget)nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, false);
      }
    }
  }

  @NotNull
  private EventListener makeHyperLinkListener() {
    return new EventListener() {
      @Override
      public void handleEvent(Event ev) {
        String domEventType = ev.getType();
        if (domEventType.equals(EVENT_TYPE_CLICK)) {
          ev.preventDefault();
          Element target = (Element)ev.getTarget();
          String hrefAttribute = getElementWithATag(target).getAttribute("href");

          if (hrefAttribute != null) {
            final Matcher matcher = IN_COURSE_LINK.matcher(hrefAttribute);
            if (matcher.matches()) {
              EduUsagesCollector.inCourseLinkClicked();
              String sectionName = null;
              String lessonName;
              String taskName;
              if (matcher.group(3) != null) {
                sectionName = matcher.group(1);
                lessonName = matcher.group(2);
                taskName = matcher.group(4);
              }
              else {
                lessonName = matcher.group(1);
                taskName = matcher.group(2);
              }
              NavigationUtils.navigateToTask(myProject, sectionName, lessonName, taskName);
            }
            else {
              if (hrefAttribute.startsWith(TaskDescriptionToolWindow.PSI_ELEMENT_PROTOCOL)) {
                TaskDescriptionToolWindow.navigateToPsiElement(myProject, hrefAttribute);
              } else {
                EduUsagesCollector.externalLinkClicked();
                myEngine.setJavaScriptEnabled(true);
                myEngine.getLoadWorker().cancel();
                String href = getLink(target);
                if (href == null) return;
                if (isRelativeLink(href)) {
                  href = STEPIK_URL + href;
                }
                BrowserUtil.browse(href);
                if (href.startsWith(STEPIK_URL)) {
                  EduUsagesCollector.stepikLinkClicked();
                }
              }
            }
          }
        }
      }

      private boolean isRelativeLink(@NotNull String href) {
        return !href.startsWith("http");
      }

      private Element getElementWithATag(Element element) {
        Element currentElement = element;
        while (!currentElement.getTagName().toLowerCase(Locale.ENGLISH).equals("a")) {
          currentElement = ((ElementImpl)currentElement).getParentElement();
        }
        return currentElement;
      }

      @Nullable
      private String getLink(@NotNull Element element) {
        final String href = element.getAttribute("href");
        return href == null ? getLinkFromNodeWithCodeTag(element) : href;
      }

      @Nullable
      private String getLinkFromNodeWithCodeTag(@NotNull Element element) {
        Node parentNode = element.getParentNode();
        NamedNodeMap attributes = parentNode.getAttributes();
        while (attributes.getLength() > 0 && attributes.getNamedItem("class") != null) {
          parentNode = parentNode.getParentNode();
          attributes = parentNode.getAttributes();
        }
        return attributes.getNamedItem("href").getNodeValue();
      }
    };
  }

  @NotNull
  public WebEngine getEngine() {
    return myWebComponent.getEngine();
  }

  public JFXPanel getPanel() {
    return myPanel;
  }

  private void setPanel(JFXPanel panel) {
    myPanel = panel;
  }
}
