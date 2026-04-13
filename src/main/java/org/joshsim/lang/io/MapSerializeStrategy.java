/**
 * Strategy for table-like serialization.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.io.strategy.MapExportSerializeStrategy;

/**
 * Strategy to serialize Entities into a flat table-like structure.
 */
public class MapSerializeStrategy implements MapExportSerializeStrategy {

  /** Sentinel value indicating no decimal place rounding should be applied. */
  public static final int UNLIMITED_PRECISION = -1;

  /** Default maximum number of decimal places for numeric values in CSV output. */
  public static final int DEFAULT_MAX_DECIMAL_PLACES = 10;

  private final int maxDecimalPlaces;

  /**
   * Create a new MapSerializeStrategy with the default precision.
   */
  public MapSerializeStrategy() {
    this(DEFAULT_MAX_DECIMAL_PLACES);
  }

  /**
   * Create a new MapSerializeStrategy with the specified maximum decimal places.
   *
   * @param maxDecimalPlaces Maximum number of decimal places for numeric values, or
   *     {@link #UNLIMITED_PRECISION} for no rounding.
   */
  public MapSerializeStrategy(int maxDecimalPlaces) {
    this.maxDecimalPlaces = maxDecimalPlaces;
  }

  /**
   * Get the maximum number of decimal places used for formatting.
   *
   * @return The max decimal places, or {@link #UNLIMITED_PRECISION} if unlimited.
   */
  public int getMaxDecimalPlaces() {
    return maxDecimalPlaces;
  }

  @Override
  public Map<String, String> getRecord(Entity entity) {
    int estimatedSize = entity.getAttributeNames().size() / 4 + 1;
    Map<String, String> result = new HashMap<>(estimatedSize);

    for (String name : entity.getAttributeNames()) {
      if (name.startsWith("export.")) {
        String key = name.replaceFirst("export\\.", "");
        Optional<EngineValue> value = entity.getAttributeValue(name);
        String valueStr = value.isPresent() ? value.get().getAsString() : "";
        result.put(key, formatNumeric(valueStr));
      }
    }

    if (entity.getGeometry().isPresent()) {
      EngineGeometry geometry = entity.getGeometry().get();
      result.put("position.x", formatDecimal(geometry.getCenterX()));
      result.put("position.y", formatDecimal(geometry.getCenterY()));
    }

    return result;
  }

  /**
   * Format a BigDecimal value respecting the max decimal places setting.
   *
   * @param value The BigDecimal to format.
   * @return The formatted string representation.
   */
  String formatDecimal(BigDecimal value) {
    if (maxDecimalPlaces == UNLIMITED_PRECISION) {
      return value.toString();
    }
    if (value.scale() <= 0 || value.scale() <= maxDecimalPlaces) {
      return value.toString();
    }
    return value.setScale(maxDecimalPlaces, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString();
  }

  /**
   * Format a string value, truncating if it represents a decimal number with excess precision.
   *
   * <p>Uses pure string operations to avoid expensive BigDecimal parsing. Values with fewer
   * decimal places than the limit are returned as-is. Values with excess precision are truncated
   * (not rounded) to the configured maximum decimal places.</p>
   *
   * @param valueStr The string to potentially format.
   * @return The original string if non-numeric or within precision, otherwise a truncated version.
   */
  String formatNumeric(String valueStr) {
    if (maxDecimalPlaces == UNLIMITED_PRECISION || valueStr.isEmpty()) {
      return valueStr;
    }

    int dotIndex = valueStr.indexOf('.');
    if (dotIndex < 0) {
      return valueStr;
    }

    int decimalDigits = valueStr.length() - dotIndex - 1;
    if (decimalDigits <= maxDecimalPlaces) {
      return valueStr;
    }

    return valueStr.substring(0, dotIndex + 1 + maxDecimalPlaces);
  }

}
