// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.safari;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openqa.selenium.Platform.MAC;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.service.DriverService;

import java.io.File;
import java.io.IOException;

public class SafariDriverService extends DriverService {

  /**
   * System property that defines the location of the safaridriver executable that will be used by
   * the {@link #createDefaultService() default service}.
   */
  public static final String SAFARI_DRIVER_EXE_PROPERTY = "webdriver.safari.driver";

  private static final File SAFARI_DRIVER_EXECUTABLE = new File("/usr/bin/safaridriver");
  private static final File TP_SAFARI_DRIVER_EXECUTABLE =
      new File("/Applications/Safari Technology Preview.app/Contents/MacOS/safaridriver");

  public SafariDriverService(
      File executable,
      int port,
      ImmutableList<String> args,
      ImmutableMap<String, String> environment) throws IOException {
    super(executable, port, args, environment);
  }

  public static SafariDriverService createDefaultService() {
    return createDefaultService(new SafariOptions());
  }

  static SafariDriverService createDefaultService(SafariOptions options) {
    return new Builder().usingTechnologyPreview(options.getUseTechnologyPreview()).build();
  }

  static SafariDriverService createDefaultService(Capabilities caps) {
    return createDefaultService(new SafariOptions(caps));
  }

  @Override
  protected void waitUntilAvailable() {
    try {
      PortProber.waitForPortUp(getUrl().getPort(), 20, SECONDS);
    } catch (RuntimeException e) {
      throw new WebDriverException(e);
    }
  }

  @AutoService(DriverService.Builder.class)
  public static class Builder extends DriverService.Builder<
      SafariDriverService, SafariDriverService.Builder> {

    private boolean useTechnologyPreview = false;

    @Override
    public int score(Capabilities capabilites) {
      int score = 0;

      if (BrowserType.SAFARI.equals(capabilites.getBrowserName())) {
        score++;
      } else if ("Safari Technology Preview".equals(capabilites.getBrowserName())) {
        score++;
      }

      if (capabilites.getCapability(SafariOptions.CAPABILITY) != null) {
        score++;
      }

      if (capabilites.getCapability("se:safari:techPreview") != null) {
        score++;
      }

      return score;
    }

    public SafariDriverService.Builder usingTechnologyPreview(boolean useTechnologyPreview) {
      this.useTechnologyPreview = useTechnologyPreview;
      return this;
    }

    @Override
    protected File findDefaultExecutable() {
      File exe;
      if (System.getProperty(SAFARI_DRIVER_EXE_PROPERTY) != null) {
        exe = new File(System.getProperty(SAFARI_DRIVER_EXE_PROPERTY));
      } else if (useTechnologyPreview) {
        exe = TP_SAFARI_DRIVER_EXECUTABLE;
      } else {
        exe = SAFARI_DRIVER_EXECUTABLE;
      }

      if (!exe.isFile()) {
        StringBuilder message = new StringBuilder();
        message.append("Unable to find driver executable: ").append(exe);

        if (!isElCapitanOrLater()) {
          message.append("(SafariDriver requires Safari 10 running on OSX El Capitan or greater.)");
        }
        throw new WebDriverException(message.toString());
      }

      return SAFARI_DRIVER_EXECUTABLE;
    }

    private boolean isElCapitanOrLater() {
      if (!Platform.getCurrent().is(MAC)) {
        return false;
      }

      // If we're using a version of macOS later than 10, we're set.
      if (Platform.getCurrent().getMajorVersion() > 10) {
        return true;
      }

      // Check the El Capitan version
      return Platform.getCurrent().getMajorVersion() == 10 &&
             Platform.getCurrent().getMinorVersion() >= 11;
    }

    @Override
    protected ImmutableList<String> createArgs() {
      return ImmutableList.of("--port", String.valueOf(getPort()));
    }

    @Override
    protected SafariDriverService createDriverService(
        File exe,
        int port,
        ImmutableList<String> args,
        ImmutableMap<String, String> environment) {
      try {
        return new SafariDriverService(exe, port, args, environment);
      } catch (IOException e) {
        throw new WebDriverException(e);
      }
    }
  }
}
