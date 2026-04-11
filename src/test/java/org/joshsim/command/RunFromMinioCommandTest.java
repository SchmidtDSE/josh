package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


/**
 * Unit tests for RunFromMinioCommand.
 */
public class RunFromMinioCommandTest {

  @Test
  void getReplicateNumber_shouldDefaultToZeroWhenEnvNotSet() {
    RunFromMinioCommand command = new RunFromMinioCommand();
    // When JOB_COMPLETION_INDEX is not set, should return 0
    int replicate = command.getReplicateNumber();
    assertEquals(0, replicate);
  }

  @Test
  void command_shouldRequireJobId() {
    // Verify the command has a --job-id option that is required
    // This is validated by picocli at runtime; we verify the annotation exists
    try {
      var field = RunFromMinioCommand.class.getDeclaredField("jobId");
      var option = field.getAnnotation(picocli.CommandLine.Option.class);
      assertEquals(true, option.required());
    } catch (NoSuchFieldException e) {
      throw new AssertionError("Expected --job-id field on RunFromMinioCommand", e);
    }
  }

  @Test
  void command_shouldBeRegisteredWithCorrectName() {
    var annotation = RunFromMinioCommand.class.getAnnotation(picocli.CommandLine.Command.class);
    assertEquals("runFromMinio", annotation.name());
  }
}
