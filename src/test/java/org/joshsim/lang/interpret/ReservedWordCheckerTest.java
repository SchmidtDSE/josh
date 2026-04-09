/**
 * Tests for ReservedWordChecker.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the ReservedWordChecker which validates variable names against reserved words.
 */
public class ReservedWordCheckerTest {

  @Test
  void checkVariableDeclarationPass() {
    assertDoesNotThrow(() -> ReservedWordChecker.checkVariableDeclaration("validName"));
    assertDoesNotThrow(() -> ReservedWordChecker.checkVariableDeclaration("anotherValid.name"));
    assertDoesNotThrow(() -> ReservedWordChecker.checkVariableDeclaration("valid.prior.name"));
  }

  @Test
  void checkVariableDeclarationFail() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ReservedWordChecker.checkVariableDeclaration("prior")
    );
    assertTrue(exception.getMessage().contains("Cannot shadow prior"));

    exception = assertThrows(
        IllegalArgumentException.class,
        () -> ReservedWordChecker.checkVariableDeclaration("current.meta")
    );
    assertTrue(exception.getMessage().contains("Cannot shadow current.meta"));
  }

  @Test
  void checkEvalDurationDirectFail() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ReservedWordChecker.checkVariableDeclaration("evalDuration")
    );
    assertTrue(exception.getMessage().contains("Cannot use reserved attribute evalDuration"));
  }

  @Test
  void checkEvalDurationSuffixFail() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ReservedWordChecker.checkVariableDeclaration("height.evalDuration")
    );
    assertTrue(exception.getMessage().contains("Cannot use reserved attribute"));
  }

  @Test
  void checkEvalDurationPrefixOk() {
    assertDoesNotThrow(
        () -> ReservedWordChecker.checkVariableDeclaration("evalDurationExtra")
    );
    assertDoesNotThrow(
        () -> ReservedWordChecker.checkVariableDeclaration("evalDurationExtra.step")
    );
  }

}
