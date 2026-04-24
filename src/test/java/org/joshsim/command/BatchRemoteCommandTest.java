/**
 * Tests for BatchRemoteCommand wiring.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;


/**
 * Schema and usage tests for {@link BatchRemoteCommand}.
 *
 * <p>Verifies the flag layout (simulation as positional; {@code --minio-prefix} required;
 * {@code --require-prestaged} optional).</p>
 */
public class BatchRemoteCommandTest {

  @Test
  void command_shouldBeRegisteredWithCorrectName() {
    var annotation = BatchRemoteCommand.class.getAnnotation(CommandLine.Command.class);
    assertEquals("batchRemote", annotation.name());
  }

  @Test
  void simulation_shouldBeIndexZeroPositional() throws Exception {
    var field = BatchRemoteCommand.class.getDeclaredField("simulation");
    var parameter = field.getAnnotation(CommandLine.Parameters.class);
    assertEquals("0", parameter.index());
  }

  @Test
  void minioPrefix_shouldBeRequired() throws Exception {
    var field = BatchRemoteCommand.class.getDeclaredField("minioPrefix");
    var option = field.getAnnotation(CommandLine.Option.class);
    assertEquals(true, option.required());
    assertTrue(java.util.Arrays.asList(option.names()).contains("--minio-prefix"));
  }

  @Test
  void requirePrestaged_shouldBeOptionalBoolean() throws Exception {
    var field = BatchRemoteCommand.class.getDeclaredField("requirePrestaged");
    var option = field.getAnnotation(CommandLine.Option.class);
    assertEquals(false, option.required());
    assertEquals(boolean.class, field.getType());
    assertTrue(java.util.Arrays.asList(option.names()).contains("--require-prestaged"));
  }

  @Test
  void input_shouldNoLongerExist() {
    try {
      BatchRemoteCommand.class.getDeclaredField("input");
      throw new AssertionError("positional <input> field should have been removed");
    } catch (NoSuchFieldException expected) {
      // ok
    }
  }

}
