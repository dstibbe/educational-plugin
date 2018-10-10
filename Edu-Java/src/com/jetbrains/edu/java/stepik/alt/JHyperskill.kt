package com.jetbrains.edu.java.stepik.alt

import com.intellij.lang.DependentLanguage
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage

class JHyperskill : Language(JavaLanguage.INSTANCE, JHYPERSKILL_LANGUAGE), DependentLanguage {
  override fun getDisplayName() = baseLanguage?.displayName ?:
                                  throw IllegalStateException("No base language found for hyperskill language")
}
