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
class JsStringJoinerTest {

  @Test
  void testAddMultipleStrings() {
    // Arrange
    String delimiter = ",";
    JsStringJoiner joiner = new JsStringJoiner(delimiter);

    // Act
    joiner.add("first");
    joiner.add("second");
    joiner.add("third");
    String result = joiner.compile();

    // Assert
    assertEquals("first,second,third", result);
  }

}
