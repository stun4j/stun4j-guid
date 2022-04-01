package com.stun4j.guid.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

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
