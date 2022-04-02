/*
 * Copyright 2020-? the original author or authors.
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
package com.stun4j.guid.core.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.stun4j.guid.core.utils.Execptions;

public class ExecptionsTest {
  @Test
  public void checkedExceptionTest() {
    // assertThatExceptionOfType(Exception.class).isThrownBy(() -> checkedException());
    try {
      raiseCheckedException();
    } catch (Throwable e) {
      assertThat(e).isNotExactlyInstanceOf(RuntimeException.class);
      assertThat(e).isExactlyInstanceOf(Exception.class);
      assertThat(e).hasMessage("ce");
    }
  }

  @Test
  public void runtimeExceptionTest() {
    try {
      raiseRuntimeException();
    } catch (Throwable e) {
      assertThat(e).isNotExactlyInstanceOf(Exception.class);
      assertThat(e).isExactlyInstanceOf(RuntimeException.class);
      assertThat(e).hasMessage("re");
    }
  }

  @Test
  public void errorTest() {
    // assertThatExceptionOfType(Exception.class).isThrownBy(() -> checkedException());
    try {
      raiseError();
    } catch (Throwable e) {
      assertThat(e).isNotExactlyInstanceOf(Exception.class);
      assertThat(e).isNotExactlyInstanceOf(RuntimeException.class);
      assertThat(e).isNotExactlyInstanceOf(Error.class);
      assertThat(e).isExactlyInstanceOf(OutOfMemoryError.class);
      assertThat(e).hasMessage("er");
    }
  }

  void raiseCheckedException() {
    try {
      throw new Exception("ce");
    } catch (Throwable t) {
      throw Execptions.sneakyThrow(t);
    }
  }

  void raiseRuntimeException() {
    try {
      throw new RuntimeException("re");
    } catch (Throwable t) {
      throw Execptions.sneakyThrow(t);
    }
  }

  void raiseError() {
    try {
      throw new OutOfMemoryError("er");
    } catch (Throwable t) {
      throw Execptions.sneakyThrow(t);
    }
  }
}
