/**
 * Tests for MultiFormatExternalGetter routing and extension probing.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.lang.io.InputGetterStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Covers extension-based routing for suffixed names and the bare-name probing fallback.
 *
 * <p>Uses real {@code Jshd*ExternalGetter} instances wrapping a mocked
 * {@link InputGetterStrategy} so the test exercises the exact exception shape that
 * propagates from file-not-found to the dispatcher's {@code isFileNotFound} check.</p>
 */
@ExtendWith(MockitoExtension.class)
class MultiFormatExternalGetterTest {

  private MultiFormatExternalGetter getter;
  private JshdExternalGetter jshdGetter;
  private JshdzExternalGetter jshdzGetter;

  @Mock
  private InputGetterStrategy mockInputStrategy;

  @BeforeEach
  void setUp() {
    ValueSupportFactory factory = new ValueSupportFactory();
    jshdGetter = new JshdExternalGetter(mockInputStrategy, factory);
    jshdzGetter = new JshdzExternalGetter(mockInputStrategy, factory);
    getter = new MultiFormatExternalGetter(jshdGetter, jshdzGetter);
  }

  @Test
  void suffixedJshdz_routesDirectly() {
    // Arrange: jshdz getter throws a known error so we can confirm routing without
    // building real jshdz bytes.
    when(mockInputStrategy.open("foo.jshdz"))
        .thenThrow(new RuntimeException("routed", new FileNotFoundException("foo.jshdz")));

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> getter.getResource("foo.jshdz"));
    assertTrue(ex.getMessage().contains("routed"));
    // Direct route — should not have probed .jshd.
    verify(mockInputStrategy).open("foo.jshdz");
  }

  @Test
  void suffixedJshd_routesDirectly() {
    when(mockInputStrategy.open("foo.jshd"))
        .thenThrow(new RuntimeException("routed", new FileNotFoundException("foo.jshd")));

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> getter.getResource("foo.jshd"));
    assertTrue(ex.getMessage().contains("routed"));
    verify(mockInputStrategy).open("foo.jshd");
  }

  @Test
  void bareName_probesJshdzFirstThenJshd_bothMissingRaisesClearError() {
    when(mockInputStrategy.open("foo.jshdz"))
        .thenThrow(new RuntimeException("open failed", new FileNotFoundException("foo.jshdz")));
    when(mockInputStrategy.open("foo.jshd"))
        .thenThrow(new RuntimeException("open failed", new FileNotFoundException("foo.jshd")));

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> getter.getResource("foo"));

    assertTrue(ex.getMessage().contains("foo.jshdz"),
        "error should mention both probed names, got: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("foo.jshd"),
        "error should mention both probed names, got: " + ex.getMessage());
  }

  @Test
  void bareName_nonFileNotFoundError_isRethrownWithoutFallback() {
    // A non-FileNotFoundException failure (e.g. corrupt file, decompression error) on the
    // .jshdz attempt must not fall back to .jshd — surface the real error.
    RuntimeException realError = new RuntimeException("XZ decompression failed",
        new java.util.zip.ZipException("bad block"));
    when(mockInputStrategy.open("foo.jshdz")).thenThrow(realError);

    RuntimeException thrown = assertThrows(RuntimeException.class,
        () -> getter.getResource("foo"));

    assertSame(realError, thrown, "original error should propagate unchanged");
    // Critically, .jshd must not have been probed.
    verify(mockInputStrategy, never()).open("foo.jshd");
  }

  @Test
  void bareName_noSuchFileExceptionInCauseChain_triggersFallback() {
    // NoSuchFileException (java.nio) also counts as file-not-found.
    when(mockInputStrategy.open("foo.jshdz"))
        .thenThrow(new RuntimeException("open failed", new NoSuchFileException("foo.jshdz")));
    when(mockInputStrategy.open("foo.jshd"))
        .thenThrow(new RuntimeException("open failed", new NoSuchFileException("foo.jshd")));

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> getter.getResource("foo"));

    assertTrue(ex.getMessage().contains("not found"));
  }

  @Test
  void unknownSuffix_fallsThroughToProbing() {
    // A name like "foo.bar" is treated as a bare name — probes foo.bar.jshdz then foo.bar.jshd.
    when(mockInputStrategy.open("foo.bar.jshdz"))
        .thenThrow(new RuntimeException("missed", new FileNotFoundException("foo.bar.jshdz")));
    when(mockInputStrategy.open("foo.bar.jshd"))
        .thenThrow(new RuntimeException("missed", new FileNotFoundException("foo.bar.jshd")));

    assertThrows(RuntimeException.class, () -> getter.getResource("foo.bar"));
    verify(mockInputStrategy).open("foo.bar.jshdz");
    verify(mockInputStrategy).open("foo.bar.jshd");
  }

  @Test
  void suffixedJshdz_notMissingJshdProbed_whenRouteFails() {
    // Suffixed name routes directly; a not-found on that exact name must NOT probe the other.
    when(mockInputStrategy.open("foo.jshdz"))
        .thenThrow(new RuntimeException("missing", new FileNotFoundException("foo.jshdz")));

    assertThrows(RuntimeException.class, () -> getter.getResource("foo.jshdz"));

    verify(mockInputStrategy).open("foo.jshdz");
    verifyNoInteractionsWith(mockInputStrategy, "foo.jshd");
  }

  private static void verifyNoInteractionsWith(InputGetterStrategy strat, String filename) {
    verify(strat, never()).open(filename);
  }
}
