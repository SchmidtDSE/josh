/**
 * Tests for GridInfoExtractor, including geo-canonical grid corner aliases.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests that GridInfoExtractor reads grid corners, including the {@code grid.top_left} /
 * {@code grid.bottom_right} aliases for {@code grid.low} / {@code grid.high}.
 */
@ExtendWith(MockitoExtension.class)
class GridInfoExtractorTest {

  private static final String TOP_LEFT = "36.73 degrees latitude, -119.52 degrees longitude";
  private static final String BOTTOM_RIGHT = "35.80 degrees latitude, -117.98 degrees longitude";

  @Mock(lenient = true) private MutableEntity mockSimulation;

  private ValueSupportFactory valueFactory;

  /**
   * Sets up the test environment before each test.
   */
  @BeforeEach
  void setUp() {
    valueFactory = new ValueSupportFactory();
  }

  private EngineValue str(String value) {
    return valueFactory.build(value, Units.EMPTY);
  }

  /**
   * The top_left / bottom_right aliases resolve to the same start / end corners as low / high.
   */
  @Test
  void readsTopLeftAndBottomRightAliases() {
    when(mockSimulation.getAttributeValue("grid.low")).thenReturn(Optional.empty());
    when(mockSimulation.getAttributeValue("grid.high")).thenReturn(Optional.empty());
    when(mockSimulation.getAttributeValue("grid.top_left")).thenReturn(Optional.of(str(TOP_LEFT)));
    when(mockSimulation.getAttributeValue("grid.bottom_right"))
        .thenReturn(Optional.of(str(BOTTOM_RIGHT)));

    GridInfoExtractor extractor = new GridInfoExtractor(mockSimulation, valueFactory);

    assertEquals(TOP_LEFT, extractor.getStartStr());
    assertEquals(BOTTOM_RIGHT, extractor.getEndStr());
  }

  /**
   * When both the canonical and alias names are present, the canonical name wins.
   */
  @Test
  void canonicalNamesTakePrecedenceOverAliases() {
    when(mockSimulation.getAttributeValue("grid.low")).thenReturn(Optional.of(str(TOP_LEFT)));
    when(mockSimulation.getAttributeValue("grid.high")).thenReturn(Optional.of(str(BOTTOM_RIGHT)));
    when(mockSimulation.getAttributeValue("grid.top_left"))
        .thenReturn(Optional.of(str("0 degrees latitude, 0 degrees longitude")));
    when(mockSimulation.getAttributeValue("grid.bottom_right"))
        .thenReturn(Optional.of(str("0 degrees latitude, 0 degrees longitude")));

    GridInfoExtractor extractor = new GridInfoExtractor(mockSimulation, valueFactory);

    assertEquals(TOP_LEFT, extractor.getStartStr());
    assertEquals(BOTTOM_RIGHT, extractor.getEndStr());
  }
}
