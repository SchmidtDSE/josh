/**
 * Tests for the JS compatibility string joiner.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


/**
 * Tests for the JS compatibility string joiner.
 */
class EmulatedStringJoinerTest {

  @Test
  void testAddMultipleStrings() {
    // Arrange
    String delimiter = ",";
    EmulatedStringJoiner joiner = new EmulatedStringJoiner(delimiter);

    // Act
    joiner.add("first");
    joiner.add("second");
    joiner.add("third");
    String result = joiner.toString();

    // Assert
    assertEquals("first,second,third", result);
  }

}