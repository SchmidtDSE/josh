
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
    // Should allow valid variable names
    assertDoesNotThrow(() -> ReservedWordChecker.checkVariableDeclaration("validName"));
    assertDoesNotThrow(() -> ReservedWordChecker.checkVariableDeclaration("anotherValid.name"));
    assertDoesNotThrow(() -> ReservedWordChecker.checkVariableDeclaration("valid.prior.name"));
  }

  @Test
  void checkVariableDeclarationFail() {
    // Should throw for reserved words
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

}
