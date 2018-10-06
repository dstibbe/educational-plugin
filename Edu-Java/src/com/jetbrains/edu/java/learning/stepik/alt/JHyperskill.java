package com.jetbrains.edu.java.learning.stepik.alt;

import com.intellij.lang.DependentLanguage;
import com.intellij.lang.Language;
import com.jetbrains.edu.learning.EduNames;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.java.learning.stepik.alt.JHyperskillNamesKt.JHYPERSKILL_LANGUAGE;

public class JHyperskill extends Language implements DependentLanguage {
  protected JHyperskill() {
    super(JHYPERSKILL_LANGUAGE);
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return EduNames.JAVA;
  }
}
