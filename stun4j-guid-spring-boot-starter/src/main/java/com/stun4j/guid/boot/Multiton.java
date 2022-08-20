/*
 * Copyright 2022-? the original author or authors.
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
package com.stun4j.guid.boot;

/**
 * Multiton configuration
 * <p>
 * @author Jay Meng
 */
public class Multiton {
  /**
   * Is multiton mechanism enabled
   * <p>
   * If enabled, there will be more than one global guid-instance in the system
   * <p>
   * Default: false
   */
  private boolean enabled = false;
  /**
   * Is auto-register enabled
   * <p>
   * If enabled, a new guid-instance is automatically created when a new pattern(bit&digits etc.) is encountered
   * <p>
   * Default: true
   */

  private boolean autoRegisterEnabled = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isAutoRegisterEnabled() {
    return autoRegisterEnabled;
  }

  public void setAutoRegisterEnabled(boolean autoRegisterEnabled) {
    this.autoRegisterEnabled = autoRegisterEnabled;
  }

}
