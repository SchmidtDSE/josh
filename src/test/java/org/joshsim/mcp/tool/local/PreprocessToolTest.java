/**
 * Tests for PreprocessTool argument handling.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp.tool.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.joshsim.command.PreprocessOptions;
import org.joshsim.command.TimeAxisParams;
import org.junit.jupiter.api.Test;


/**
 * Unit tests for {@link PreprocessTool#buildOptions(Map)}.
 *
 * <p>The MCP tool is expected to behave like the {@code preprocess} CLI command given the same
 * arguments, so these tests pin the defaults as well as the mappings.</p>
 */
class PreprocessToolTest {

  @Test
  void defaultsMatchTheCliWhenNoOptionalArgumentsAreGiven() {
    PreprocessOptions options = PreprocessTool.buildOptions(new HashMap<>());

    assertEquals("EPSG:4326", options.getCrsCode());
    assertEquals("lon", options.getHorizCoordName());
    assertEquals("lat", options.getVertCoordName());
    assertEquals("calendar_year", options.getTimeName());
    assertEquals("", options.getTimestep());
    assertNull(options.getDefaultValue());
    assertFalse(options.isParallel());
    assertFalse(options.isAmend());
    assertFalse(options.getTimeAxis().isDeclared());
  }

  @Test
  void spatialAndModeArgumentsAreRead() {
    Map<String, Object> args = new HashMap<>();
    args.put("crs", "EPSG:3310");
    args.put("xCoord", "longitude");
    args.put("yCoord", "latitude");
    args.put("timeDim", "time");
    args.put("timestep", "2020");
    args.put("defaultValue", "-999");
    args.put("parallel", true);
    args.put("amend", true);

    PreprocessOptions options = PreprocessTool.buildOptions(args);

    assertEquals("EPSG:3310", options.getCrsCode());
    assertEquals("longitude", options.getHorizCoordName());
    assertEquals("latitude", options.getVertCoordName());
    assertEquals("time", options.getTimeName());
    assertEquals("2020", options.getTimestep());
    assertEquals("-999", options.getDefaultValue());
    assertTrue(options.isParallel());
    assertTrue(options.isAmend());
  }

  @Test
  void noTimeDimOverridesTimeDim() {
    Map<String, Object> args = new HashMap<>();
    args.put("timeDim", "time");
    args.put("noTimeDim", true);

    assertEquals("", PreprocessTool.buildOptions(args).getTimeName());
  }

  @Test
  void countTimeAxisArgumentsAreRead() {
    Map<String, Object> args = new HashMap<>();
    args.put("timeType", "count");
    args.put("timeStart", "2015");
    args.put("timeUnit", "year");
    args.put("timeCount", "86");
    args.put("timeIncrement", "1");

    TimeAxisParams axis = PreprocessTool.buildOptions(args).getTimeAxis();

    assertTrue(axis.isDeclared());
    assertEquals("count", axis.getType());
    assertEquals("2015", axis.get(TimeAxisParams.Field.START));
    assertEquals("year", axis.get(TimeAxisParams.Field.UNIT));
    assertEquals("86", axis.get(TimeAxisParams.Field.COUNT));
    assertEquals("1", axis.get(TimeAxisParams.Field.INCREMENT));
  }

  @Test
  void isoTimeAxisArgumentsAreRead() {
    Map<String, Object> args = new HashMap<>();
    args.put("timeType", "ISO");
    args.put("timeStart", "2026-01-01");
    args.put("timeCount", "900");
    args.put("timeInterval", "P1M");

    TimeAxisParams axis = PreprocessTool.buildOptions(args).getTimeAxis();

    assertEquals("ISO", axis.getType());
    assertEquals("2026-01-01", axis.get(TimeAxisParams.Field.START));
    assertEquals("P1M", axis.get(TimeAxisParams.Field.INTERVAL));
    assertEquals("", axis.get(TimeAxisParams.Field.UNIT));
  }

  @Test
  void numericTemporalCoordinatesSentAsNumbersAreCoerced() {
    // Clients that infer types from the value send 2015 rather than "2015" for a count coordinate.
    Map<String, Object> args = new HashMap<>();
    args.put("timeStart", 2015);
    args.put("timeUnit", "year");
    args.put("timeCount", 86);

    TimeAxisParams axis = PreprocessTool.buildOptions(args).getTimeAxis();

    assertEquals("2015", axis.get(TimeAxisParams.Field.START));
    assertEquals("86", axis.get(TimeAxisParams.Field.COUNT));
  }

  @Test
  void stringifiedBooleansAreAccepted() {
    Map<String, Object> args = new HashMap<>();
    args.put("parallel", "true");
    args.put("noTimeDim", "true");

    PreprocessOptions options = PreprocessTool.buildOptions(args);

    assertTrue(options.isParallel());
    assertEquals("", options.getTimeName());
  }

  @Test
  void everySchemaTimeArgumentIsNamedAfterItsField() {
    // The schema is hand-written JSON, so nothing but this checks that its property names still
    // match the names buildOptions looks up.
    String schema = ToolSchemas.load("preprocess_data");
    for (TimeAxisParams.Field field : TimeAxisParams.Field.values()) {
      assertTrue(schema.contains("\"" + field.getFieldName() + "\""), field.getFieldName());
    }
  }
}
