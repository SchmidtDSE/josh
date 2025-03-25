/**
 * Structures to help build converters.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for Converters which can handle multiple conversions.
 */
public class ConverterBuilder {

  Map<EngineValueTuple.UnitsTuple, Conversion> conversions;
  Map<Units, List<Conversion>> conversionsByDestination;

  /**
   * Create a builder without any conversions.
   */
  public ConverterBuilder() {
    conversions = new HashMap<>();
    conversionsByDestination = new HashMap<>();
  }

  /**
   * Add a conversion rule to the builder.
   *
   * @param conversion the conversion rule to add
   * @return this builder for method chaining
   */
  public ConverterBuilder addConversion(Conversion conversion) {
    Units source = conversion.getSourceUnits();
    Units destination = conversion.getDestinationUnits();

    EngineValueTuple.UnitsTuple unitsTuple = new EngineValueTuple.UnitsTuple(source, destination);
    conversions.put(unitsTuple, conversion);

    if (!conversionsByDestination.containsKey(destination)) {
      conversionsByDestination.put(destination, new ArrayList<>());
    }
    conversionsByDestination.get(destination).add(conversion);

    extendTransitively(conversion);
    return this;
  }

  /**
   * Build and returns a Converter based on the added conversions.
   *
   * @return a new Converter instance
   */
  public Converter build() {
    return new Converter(conversions);
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

    List<Conversion> endingAtSource = conversionsByDestination.get(newSource);
    for (Conversion conversionToChain : endingAtSource) {
      Conversion chainedConversion = new TransitiveConversion(conversionToChain, newConversion);
      addConversion(chainedConversion);
    }
  }
}
