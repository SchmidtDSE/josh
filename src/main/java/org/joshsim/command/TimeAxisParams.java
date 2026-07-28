/**
 * Declared JSHD time axis parameters, carried as unparsed strings.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * The temporal metadata declared for a preprocessed JSHD file, before parsing.
 *
 * <p>Holds the values behind the {@code --time-*} preprocess flags. Each value's name is declared
 * once in {@link Field}, which derives both the CLI flag and the {@code /preprocessBatch} form
 * field from it. Layers that move these values across a boundary iterate {@link Field} rather than
 * restating the seven names: {@link org.joshsim.pipeline.target.HttpPreprocessTarget} encodes them
 * as form fields, {@link org.joshsim.pipeline.target.KubernetesPreprocessTarget} encodes them as a
 * single pre-built flag string for the pod entrypoint, and
 * {@link org.joshsim.cloud.JoshSimPreprocessBatchHandler} decodes them back.</p>
 *
 * <p>Values are kept as strings because validation depends on {@code type} and on the number of
 * output slices, neither of which is known at dispatch time. {@link PreprocessUtil} parses and
 * validates them into a {@link org.joshsim.precompute.TimeAxis}, and only does so when
 * {@link #isDeclared()} is true.</p>
 */
public final class TimeAxisParams {

  /**
   * The declared time axis values, and the names each one goes by.
   *
   * <p>The enum constant is the single source of truth: {@code START} yields {@code --time-start}
   * on the command line and {@code timeStart} as a form field.</p>
   */
  public enum Field {
    /** Axis type: {@code count} or {@code ISO}. */
    TYPE,
    /** First coordinate of a range: a count coordinate or an ISO date. */
    START,
    /** Unit of a count axis, such as {@code year}. */
    UNIT,
    /** Number of declared coordinates, which must equal the number of output slices. */
    COUNT,
    /** Spacing between count coordinates; defaults to 1. */
    INCREMENT,
    /** Spacing between ISO coordinates as an ISO-8601 period, such as {@code P1M}. */
    INTERVAL,
    /** Single coordinate for a one-slice axis, in place of a range. */
    INSTANT;

    /**
     * Returns the preprocess CLI flag for this value, such as {@code --time-start}.
     *
     * @return The long-form flag name, including leading dashes.
     */
    public String getCliFlag() {
      return "--time-" + name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the camel-cased name for this value, such as {@code timeStart}.
     *
     * <p>Shared by the {@code /preprocessBatch} form field and the {@code preprocess_data} MCP
     * argument, so a client naming one has named the other.</p>
     *
     * @return The camel-cased field name.
     */
    public String getFieldName() {
      return "time" + name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }
  }

  /** An axis with nothing declared, which leaves a JSHD file without temporal metadata. */
  public static final TimeAxisParams EMPTY = new TimeAxisParams(new EnumMap<>(Field.class));

  private static final String DEFAULT_TYPE = "count";

  private final Map<Field, String> values;

  private TimeAxisParams(Map<Field, String> values) {
    Map<Field, String> normalized = new EnumMap<>(Field.class);
    for (Field field : Field.values()) {
      String value = values.get(field);
      normalized.put(field, value == null ? "" : value);
    }
    this.values = normalized;
  }

  /**
   * Constructs params from the seven values, in preprocess flag declaration order.
   *
   * @param type Axis type: {@code count} or {@code ISO}; blank means {@code count}.
   * @param start First coordinate of a range, or blank.
   * @param unit Count-axis unit, or blank.
   * @param count Number of declared coordinates, or blank.
   * @param increment Count-axis coordinate spacing, or blank for 1.
   * @param interval ISO-8601 period between coordinates, or blank.
   * @param instant Single coordinate for a one-slice axis, or blank.
   * @return The declared params; nulls are treated as blank.
   */
  public static TimeAxisParams of(String type, String start, String unit, String count,
      String increment, String interval, String instant) {
    Map<Field, String> values = new EnumMap<>(Field.class);
    values.put(Field.TYPE, type);
    values.put(Field.START, start);
    values.put(Field.UNIT, unit);
    values.put(Field.COUNT, count);
    values.put(Field.INCREMENT, increment);
    values.put(Field.INTERVAL, interval);
    values.put(Field.INSTANT, instant);
    return new TimeAxisParams(values);
  }

  /**
   * Constructs params by asking a lookup for each field in turn.
   *
   * <p>Lets a decoder name its own source - a form field, an env var, a JSON key - without
   * restating the set of fields. A lookup may return null for a value it does not have.</p>
   *
   * @param lookup Resolves a field to its declared value, or null/blank if absent.
   * @return The declared params.
   */
  public static TimeAxisParams fromLookup(Function<Field, String> lookup) {
    Map<Field, String> values = new EnumMap<>(Field.class);
    for (Field field : Field.values()) {
      values.put(field, lookup.apply(field));
    }
    return new TimeAxisParams(values);
  }

  /**
   * Returns the raw declared value for a field.
   *
   * @param field The field to read.
   * @return The declared value, or the empty string if it was not declared.
   */
  public String get(Field field) {
    return values.get(field);
  }

  /**
   * Returns the axis type, defaulting to {@code count} when it was not declared.
   *
   * @return The axis type as declared, or {@code count}.
   */
  public String getType() {
    String declared = values.get(Field.TYPE);
    return declared.isBlank() ? DEFAULT_TYPE : declared;
  }

  /**
   * Reports whether enough was declared to write a time axis.
   *
   * <p>{@link Field#TYPE} and {@link Field#INCREMENT} only shape an axis that something else
   * establishes, so neither one alone counts as a declaration.</p>
   *
   * @return True if a time axis should be written, false to leave the JSHD file timeless.
   */
  public boolean isDeclared() {
    return !get(Field.START).isBlank()
        || !get(Field.UNIT).isBlank()
        || !get(Field.COUNT).isBlank()
        || !get(Field.INTERVAL).isBlank()
        || !get(Field.INSTANT).isBlank();
  }

  /**
   * Encodes the declared values as {@code /preprocessBatch} form fields.
   *
   * @return Form field name to value, in field declaration order, omitting undeclared values.
   */
  public Map<String, String> toFormFields() {
    return encode(Field::getFieldName);
  }

  /**
   * Encodes the declared values as preprocess CLI flags.
   *
   * <p>Used to hand a whole time axis to the pod entrypoint as one env var, so the shell does not
   * have to restate the field names. Values are single tokens - counts, units, ISO dates and
   * periods - so the result is safe to word-split.</p>
   *
   * @return Space-separated {@code --time-x=value} flags, or the empty string if nothing was
   *     declared.
   */
  public String toCliFlags() {
    return encode(Field::getCliFlag).entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining(" "));
  }

  private Map<String, String> encode(Function<Field, String> namer) {
    Map<String, String> encoded = new LinkedHashMap<>();
    for (Field field : Field.values()) {
      String value = get(field);
      if (!value.isBlank()) {
        encoded.put(namer.apply(field), value);
      }
    }
    return encoded;
  }
}
