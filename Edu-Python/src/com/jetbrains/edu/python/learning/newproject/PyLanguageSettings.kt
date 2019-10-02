package com.jetbrains.edu.python.learning.newproject

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModel
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator.getBaseSdk
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import java.awt.BorderLayout
import javax.swing.JComponent

internal open class PyLanguageSettings : LanguageSettings<PyNewProjectSettings>() {

  protected val mySettings: PyNewProjectSettings = PyNewProjectSettings()

  override fun getLanguageSettingsComponents(course: Course): List<LabeledComponent<JComponent>> {
    // by default we create new virtual env in project, we need to add this non-existing sdk to sdk list
    val fakeSdk = createFakeSdk(course)

    val combo = getInterpreterComboBox(fakeSdk)
    if (SystemInfo.isMac && !UIUtil.isUnderDarcula()) {
      combo.putClientProperty("JButton.buttonType", null)
    }
    combo.setButtonIcon(AllIcons.General.GearPlain)
    return listOf<LabeledComponent<JComponent>>(LabeledComponent.create(combo, "Interpreter", BorderLayout.WEST))
  }

  override fun getSettings(): PyNewProjectSettings = mySettings

  protected open fun getInterpreterComboBox(fakeSdk: Sdk?): ComboboxWithBrowseButton {
    val project = ProjectManager.getInstance().defaultProject
    val model = ProjectSdksModel()
    model.reset(project)
    if (fakeSdk != null) {
      model.addSdk(fakeSdk)
    }

    model.addListener(object : SdkModel.Listener {
      override fun sdkAdded(sdk: Sdk) = SdkConfigurationUtil.addSdk(sdk)
      override fun beforeSdkRemove(sdk: Sdk) {}
      override fun sdkChanged(sdk: Sdk, previousName: String) {}
      override fun sdkHomeSelected(sdk: Sdk, newSdkHome: String) {}
    })

    val sdkTypeIdFilter = Condition<SdkTypeId> { it == PythonSdkType.getInstance() || it == PyFakeSdkType }
    val sdkFilter = JdkComboBox.getSdkFilter(sdkTypeIdFilter)
    val comboBox = JdkComboBox(model, sdkTypeIdFilter, sdkFilter, sdkTypeIdFilter, true)
    comboBox.addActionListener { onSdkSelected(comboBox) }

    if (fakeSdk != null) {
      comboBox.selectedJdk = fakeSdk
    }

    val comboBoxWithBrowseButton = ComboboxWithBrowseButton(comboBox)
    val setupButton = comboBoxWithBrowseButton.button
    comboBox.setSetupButton(setupButton, null, model, comboBox.model.selectedItem as JdkComboBox.JdkComboBoxItem, null, false)
    return comboBoxWithBrowseButton

  }

  private fun onSdkSelected(comboBox: JdkComboBox) {
    var selectedSdk = comboBox.selectedJdk
    if (selectedSdk == null) {
      val selectedItem = comboBox.selectedItem
      if (selectedItem is JdkComboBox.SuggestedJdkItem) {
        selectedSdk = PyDetectedSdk(selectedItem.path)
      }
    }
    mySettings.sdk = selectedSdk
    notifyListeners()
  }

  companion object {

    private fun createFakeSdk(course: Course): ProjectJdkImpl? {
      val fakeSdkPath = getBaseSdk(course) ?: return null
      val flavor = PythonSdkFlavor.getApplicableFlavors(false)[0]
      val prefix = flavor.name + " "
      val versionString = flavor.getVersionString(fakeSdkPath)
      if (versionString == null || !versionString.contains(prefix)) {
        return null
      }
      val pythonVersion = versionString.substring(prefix.length)
      val name = "new virtual env $pythonVersion"
      return ProjectJdkImpl(name, PyFakeSdkType, "", pythonVersion)
    }
  }
}
