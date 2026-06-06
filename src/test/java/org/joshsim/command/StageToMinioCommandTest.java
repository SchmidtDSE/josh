package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


/**
 * Unit tests for StageToMinioCommand.
 */
public class StageToMinioCommandTest {

  @Test
  void command_shouldRequirePrefix() {
    try {
      var field = StageToMinioCommand.class.getDeclaredField("prefix");
      var option = field.getAnnotation(picocli.CommandLine.Option.class);
      assertEquals(true, option.required());
    } catch (NoSuchFieldException e) {
      throw new AssertionError("Expected --prefix field", e);
    }
  }

  @Test
  void command_shouldRequireInputDir() {
    try {
      var field = StageToMinioCommand.class.getDeclaredField("inputDir");
      var option = field.getAnnotation(picocli.CommandLine.Option.class);
      assertEquals(true, option.required());
    } catch (NoSuchFieldException e) {
      throw new AssertionError("Expected --input-dir field", e);
    }
  }

  @Test
  void command_shouldBeRegisteredWithCorrectName() {
    var annotation = StageToMinioCommand.class.getAnnotation(picocli.CommandLine.Command.class);
    assertEquals("stageToMinio", annotation.name());
  }
}
