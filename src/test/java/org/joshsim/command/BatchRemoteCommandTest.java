/**
 * Tests for BatchRemoteCommand wiring.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;


/**
 * Schema and usage tests for {@link BatchRemoteCommand}.
 *
 * <p>Verifies the flag layout (simulation as positional; {@code --minio-prefix} required;
 * {@code --stage-from-local-dir} and {@code --require-prestaged} optional) and exercises
 * the mutex check via a real command invocation.</p>
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
  void stageFromLocalDir_shouldBeOptional() throws Exception {
    var field = BatchRemoteCommand.class.getDeclaredField("stageFromLocalDir");
    var option = field.getAnnotation(CommandLine.Option.class);
    assertEquals(false, option.required());
    assertEquals(File.class, field.getType());
    assertTrue(java.util.Arrays.asList(option.names()).contains("--stage-from-local-dir"));
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
  void mutuallyExclusiveFlags_returnUsageError() {
    BatchRemoteCommand command = new BatchRemoteCommand();
    CommandLine cli = new CommandLine(command);
    int exit = cli.execute(
        "MySim",
        "--target=nonexistent-profile",
        "--minio-prefix=batch-jobs/foo/inputs/",
        "--stage-from-local-dir=/tmp",
        "--require-prestaged"
    );
    // 102 is USAGE_ERROR_CODE in BatchRemoteCommand.
    assertEquals(102, exit);
  }
}
