/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stun4j.guid.boot.support;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;

import org.apache.curator.framework.CuratorFramework;

/**
 * Class that exposes the Curator version. Fetches the
 * {@link Name#IMPLEMENTATION_VERSION Implementation-Version} manifest attribute from the
 * jar file via {@link Package#getImplementationVersion()}, falling back to locating the
 * jar file that contains this class and reading the {@code Implementation-Version}
 * attribute from its manifest.
 * <p>
 * This class might not be able to determine the Curator version in all environments.
 * Consider using a reflection-based check instead: For example, checking for the presence
 * of a specific Curator method that you intend to call.
 * @author Drummond Dawson
 * @author Hendrig Sellik
 * @author Andy Wilkinson
 * @author Jay Meng
 *         <p>
 *         From spring-boot:2.6.5,with name changed and the way of fetching version
 * @since 1.3.0
 */
public final class CuratorVersion {

  private CuratorVersion() {
  }

  /**
   * Return the full version string of the present Curator codebase, or {@code null}
   * if it cannot be determined.
   * @return the version of Curator or {@code null}
   * @see Package#getImplementationVersion()
   */
  public static String getVersion() {
    return determineCuratorVersion();
  }

  private static String determineCuratorVersion() {
    String implementationVersion = null;
    try {
      implementationVersion = CuratorFramework.class.getPackage().getImplementationVersion();
    } catch (Throwable e) {
      return null;
    }
    if (implementationVersion != null) {
      return implementationVersion;
    }
    CodeSource codeSource = CuratorFramework.class.getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      return null;
    }
    URL codeSourceLocation = codeSource.getLocation();
    try {
      URLConnection connection = codeSourceLocation.openConnection();
      if (connection instanceof JarURLConnection) {
        return getImplementationVersion(((JarURLConnection)connection).getJarFile());
      }
      try (JarFile jarFile = new JarFile(new File(codeSourceLocation.toURI()))) {
        return getImplementationVersion(jarFile);
      }
    } catch (Exception ex) {
      return null;
    }
  }

  private static String getImplementationVersion(JarFile jarFile) throws IOException {
    String ver = jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
    if (ver == null) {
      ver = jarFile.getManifest().getMainAttributes().getValue("Bundle-Version");
    }
    return ver;
  }

}
