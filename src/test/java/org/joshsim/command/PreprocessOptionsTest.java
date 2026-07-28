/**
 * Tests for PreprocessOptions and its builder.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;


/**
 * Unit tests for {@link PreprocessOptions}.
 */
class PreprocessOptionsTest {

  @Test
  void defaultsMatchThePreprocessCommandsOwnDefaults() {
    // The builder is the single source of these defaults now, so it has to agree with what picocli
    // declares - otherwise a batch or MCP job diverges from the same job run locally.
    CommandSpec spec = new CommandLine(new PreprocessCommand()).getCommandSpec();
    PreprocessOptions options = PreprocessOptions.defaults();

    assertEquals(declaredDefault(spec, "--crs"), options.getCrsCode());
    assertEquals(declaredDefault(spec, "--x-coord"), options.getHorizCoordName());
    assertEquals(declaredDefault(spec, "--y-coord"), options.getVertCoordName());
    assertEquals(declaredDefault(spec, "--time-dim"), options.getTimeName());
    assertEquals(declaredDefault(spec, "--timestep"), options.getTimestep());
    assertEquals(declaredDefault(spec, "--parallel"), String.valueOf(options.isParallel()));
    assertEquals(declaredDefault(spec, "--amend"), String.valueOf(options.isAmend()));
  }

  private static String declaredDefault(CommandSpec spec, String flag) {
    return spec.findOption(flag).defaultValue();
  }

  @Test
  void defaultsLeaveDefaultValueUnsetAndTheAxisUndeclared() {
    PreprocessOptions options = PreprocessOptions.defaults();

    assertNull(options.getDefaultValue());
    assertFalse(options.getTimeAxis().isDeclared());
    assertFalse(options.isParallel());
    assertFalse(options.isAmend());
  }

  @Test
  void unsetValuesKeepTheirDefaults() {
    PreprocessOptions options = PreprocessOptions.builder().timestep("7").build();

    assertEquals("7", options.getTimestep());
    assertEquals("EPSG:4326", options.getCrsCode());
    assertEquals("calendar_year", options.getTimeName());
  }

  @Test
  void everyValueCanBeSet() {
    TimeAxisParams axis = TimeAxisParams.of("ISO", "2026-01-01", "", "900", "", "P1M", "");
    PreprocessOptions options = PreprocessOptions.builder()
        .crsCode("EPSG:3310")
        .horizCoordName("longitude")
        .vertCoordName("latitude")
        .timeName("time")
        .timestep("2020")
        .defaultValue("-999")
        .parallel(true)
        .amend(true)
        .timeAxis(axis)
        .build();

    assertEquals("EPSG:3310", options.getCrsCode());
    assertEquals("longitude", options.getHorizCoordName());
    assertEquals("latitude", options.getVertCoordName());
    assertEquals("time", options.getTimeName());
    assertEquals("2020", options.getTimestep());
    assertEquals("-999", options.getDefaultValue());
    assertTrue(options.isParallel());
    assertTrue(options.isAmend());
    assertEquals(axis, options.getTimeAxis());
  }

  @Test
  void noTimeDimClearsTheTimeDimensionName() {
    assertEquals("", PreprocessOptions.builder().noTimeDim().build().getTimeName());
    assertEquals("",
        PreprocessOptions.builder().timeName("time").noTimeDim().build().getTimeName());
  }

  @Test
  void emptyTimeNameIsEquivalentToNoTimeDim() {
    assertEquals(
        PreprocessOptions.builder().noTimeDim().build().getTimeName(),
        PreprocessOptions.builder().timeName("").build().getTimeName());
  }

  @Test
  void nullTimeNameIsRejectedRatherThanGuessedAt() {
    // The two classes this replaced disagreed about null here: one read it as the default dimension
    // name, the other as a source with no time dimension. Neither guess is safe to make.
    NullPointerException thrown = assertThrows(NullPointerException.class,
        () -> PreprocessOptions.builder().timeName(null));
    assertTrue(thrown.getMessage().contains("timeName"), thrown.getMessage());
  }

  @Test
  void otherRequiredValuesRejectNullToo() {
    assertThrows(NullPointerException.class,
        () -> PreprocessOptions.builder().crsCode(null));
    assertThrows(NullPointerException.class,
        () -> PreprocessOptions.builder().horizCoordName(null));
    assertThrows(NullPointerException.class,
        () -> PreprocessOptions.builder().vertCoordName(null));
    assertThrows(NullPointerException.class,
        () -> PreprocessOptions.builder().timeAxis(null));
  }

  @Test
  void nullTimestepMeansTheWholeStepRange() {
    // Dispatchers hand through a blank CLI flag as null; "" is what PreprocessUtil checks for.
    assertEquals("", PreprocessOptions.builder().timestep(null).build().getTimestep());
  }

  @Test
  void nullDefaultValueMeansNoFill() {
    assertNull(PreprocessOptions.builder().defaultValue(null).build().getDefaultValue());
  }
}
