/**
 * Structures to help build converters.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.converter;

import java.util.HashMap;
import java.util.Map;
import org.joshsim.engine.value.engine.EngineValueTuple;

/**
 * Builder for Converters which can handle multiple conversions.
 */
public class ConverterBuilder {

  Map<EngineValueTuple.UnitsTuple, Conversion> conversions;
  Map<Units, Map<Units, Conversion>> conversionsByDestination;

  /**
   * Create a builder without any conversions.
   */
  public ConverterBuilder() {
    conversions = new HashMap<>();
    conversionsByDestination = new HashMap<>();
    addConversion(new NoopConversion(Units.of(""), Units.of("count")));
    addConversion(new NoopConversion(Units.of("count"), Units.of("counts")));
    addConversion(new NoopConversion(Units.of("m"), Units.of("meter")));
    addConversion(new NoopConversion(Units.of("meter"), Units.of("meters")));
  }

  /**
   * Add a conversion rule to the builder.
   *
   * @param conversion the conversion rule to add
   * @return this builder for method chaining
   */
  public ConverterBuilder addConversion(Conversion conversion) {
    addConversionNoCommunicative(conversion);
    if (conversion.isCommunicativeSafe()) {
      addConversionNoCommunicative(new InverseConversion(conversion));
    }
    return this;
  }


  private void addConversionNoCommunicative(Conversion conversion) {
    Units source = conversion.getSourceUnits();
    Units destination = conversion.getDestinationUnits();

    if (conversionsByDestination.containsKey(destination)) {
      if (conversionsByDestination.get(destination).containsKey(source)) {
        return;
      }
    }

    EngineValueTuple.UnitsTuple unitsTuple = new EngineValueTuple.UnitsTuple(source, destination);
    conversions.put(unitsTuple, conversion);

    if (!conversionsByDestination.containsKey(destination)) {
      conversionsByDestination.put(destination, new HashMap<>());
    }
    conversionsByDestination.get(destination).put(source, conversion);

    extendTransitively(conversion);
  }

  /**
   * Build and returns a Converter based on the added conversions.
   *
   * @return a new Converter instance
   */
  public Converter build() {
    return new MapConverter(conversions);
  }

  /**
   * Add transitive conversions for any strategies which end in the source of the new conversion.
   *
   * @param newConversion for which to add new transitive conversions.
   */
  private void extendTransitively(Conversion newConversion) {
    Units newSource = newConversion.getSourceUnits();

    if (!conversionsByDestination.containsKey(newSource)) {
      return;
    }

    Map<Units, Conversion> endingAtSource = conversionsByDestination.get(newSource);
    for (Conversion conversionToChain : endingAtSource.values()) {
      Conversion chainedConversion = new TransitiveConversion(conversionToChain, newConversion);
      addConversion(chainedConversion);
    }
  }
}
