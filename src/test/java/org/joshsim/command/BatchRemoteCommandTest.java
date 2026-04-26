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

  @Test
  void customTag_shouldBeStringArrayOption() throws Exception {
    var field = BatchRemoteCommand.class.getDeclaredField("customTags");
    var option = field.getAnnotation(CommandLine.Option.class);
    assertEquals(false, option.required());
    assertEquals(String[].class, field.getType());
    assertTrue(java.util.Arrays.asList(option.names()).contains("--custom-tag"));
  }

  @Test
  void replicateStart_shouldBeIntOptionWithDefaultZero() throws Exception {
    var field = BatchRemoteCommand.class.getDeclaredField("replicateStart");
    var option = field.getAnnotation(CommandLine.Option.class);
    assertEquals(false, option.required());
    assertEquals(int.class, field.getType());
    assertTrue(java.util.Arrays.asList(option.names()).contains("--replicate-start"));
    assertEquals("0", option.defaultValue());
  }

  @Test
  void parseCustomTags_shouldRejectReservedNames() throws Exception {
    BatchRemoteCommand cmd = new BatchRemoteCommand();
    setCustomTags(cmd, new String[]{"replicate=42"});
    Exception ex = assertThrows("replicate", cmd);
    assertTrue(ex.getMessage().contains("reserved"),
        "expected 'reserved' in message: " + ex.getMessage());
  }

  @Test
  void parseCustomTags_shouldRejectMalformed() throws Exception {
    BatchRemoteCommand cmd = new BatchRemoteCommand();
    setCustomTags(cmd, new String[]{"no_equals_sign"});
    Exception ex = assertThrows("no_equals", cmd);
    assertTrue(ex.getMessage().contains("Invalid custom-tag"),
        "expected 'Invalid custom-tag' in message: " + ex.getMessage());
  }

  @Test
  void parseCustomTags_shouldParseValidPairsIntoMap() throws Exception {
    BatchRemoteCommand cmd = new BatchRemoteCommand();
    setCustomTags(cmd, new String[]{"run_hash=abc", "region=west"});
    var method = BatchRemoteCommand.class.getDeclaredMethod("parseCustomTags");
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    var parsed = (java.util.Map<String, String>) method.invoke(cmd);
    assertEquals(2, parsed.size());
    assertEquals("abc", parsed.get("run_hash"));
    assertEquals("west", parsed.get("region"));
  }

  private static void setCustomTags(BatchRemoteCommand cmd, String[] tags) throws Exception {
    java.lang.reflect.Field f = BatchRemoteCommand.class.getDeclaredField("customTags");
    f.setAccessible(true);
    f.set(cmd, tags);
  }

  private static Exception assertThrows(String description, BatchRemoteCommand cmd) {
    try {
      var method = BatchRemoteCommand.class.getDeclaredMethod("parseCustomTags");
      method.setAccessible(true);
      method.invoke(cmd);
    } catch (java.lang.reflect.InvocationTargetException ite) {
      Throwable cause = ite.getCause();
      if (cause instanceof IllegalArgumentException iae) {
        return iae;
      }
      throw new AssertionError(
          "Expected IllegalArgumentException for " + description + ", got: " + cause);
    } catch (Exception e) {
      throw new AssertionError("Reflection failure: " + e);
    }
    throw new AssertionError("Expected exception for " + description + " was not thrown");
  }

}
