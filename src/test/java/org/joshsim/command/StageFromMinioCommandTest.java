package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


/**
 * Unit tests for StageFromMinioCommand.
 */
public class StageFromMinioCommandTest {

  @Test
  void command_shouldRequirePrefix() {
    try {
      var field = StageFromMinioCommand.class.getDeclaredField("prefix");
      var option = field.getAnnotation(picocli.CommandLine.Option.class);
      assertEquals(true, option.required());
    } catch (NoSuchFieldException e) {
      throw new AssertionError("Expected --prefix field", e);
    }
  }

  @Test
  void command_shouldRequireOutputDir() {
    try {
      var field = StageFromMinioCommand.class.getDeclaredField("outputDir");
      var option = field.getAnnotation(picocli.CommandLine.Option.class);
      assertEquals(true, option.required());
    } catch (NoSuchFieldException e) {
      throw new AssertionError("Expected --output-dir field", e);
    }
  }

  @Test
  void command_shouldBeRegisteredWithCorrectName() {
    var annotation = StageFromMinioCommand.class.getAnnotation(
        picocli.CommandLine.Command.class
    );
    assertEquals("stageFromMinio", annotation.name());
  }
}
