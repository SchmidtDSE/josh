package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


/**
 * Unit tests for PreprocessFromMinioCommand.
 */
public class PreprocessFromMinioCommandTest {

  @Test
  void getTimestepIndex_shouldDefaultToZeroWhenEnvNotSet() {
    PreprocessFromMinioCommand command = new PreprocessFromMinioCommand();
    int index = command.getTimestepIndex();
    assertEquals(0, index);
  }

  @Test
  void command_shouldRequireJobId() {
    try {
      var field = PreprocessFromMinioCommand.class.getDeclaredField("jobId");
      var option = field.getAnnotation(picocli.CommandLine.Option.class);
      assertEquals(true, option.required());
    } catch (NoSuchFieldException e) {
      throw new AssertionError("Expected --job-id field on PreprocessFromMinioCommand", e);
    }
  }

  @Test
  void command_shouldBeRegisteredWithCorrectName() {
    var annotation = PreprocessFromMinioCommand.class.getAnnotation(
        picocli.CommandLine.Command.class
    );
    assertEquals("preprocessFromMinio", annotation.name());
  }
}
