package com.jetbrains.edu.java.stepik.alt

import com.intellij.openapi.components.ApplicationComponent

@Suppress("ComponentNotRegistered")
class JHyperskillApplicationComponent : ApplicationComponent {
  override fun initComponent() {
    JHyperskill()
  }
}
