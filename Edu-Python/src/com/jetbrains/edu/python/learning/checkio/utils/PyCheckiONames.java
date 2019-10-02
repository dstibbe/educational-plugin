package com.jetbrains.edu.python.learning.checkio.utils;

import static com.jetbrains.edu.learning.checkio.utils.CheckiONames.*;

public final class PyCheckiONames {
  private PyCheckiONames() {}

  public static final String PY_CHECKIO = "Py " + CHECKIO;

  public static final String PY_CHECKIO_URL = "py." + CHECKIO_URL;

  public static final String PY_CHECKIO_API_HOST = "https://" + PY_CHECKIO_URL;
  public static final String PY_CHECKIO_OAUTH_SERVICE_NAME = CHECKIO_OAUTH_SERVICE_NAME + "/py";
  public static final String PY_CHECKIO_OAUTH_SERVICE_PATH = CHECKIO_OAUTH_SERVICE_PATH + "/py";

  public static final String PY_CHECKIO_INTERPRETER = "python-3";

  public static final String PY_CHECKIO_TEST_FORM_TARGET_URL = PY_CHECKIO_API_HOST + CHECKIO_TEST_FORM_TARGET_PATH + "/";
}
