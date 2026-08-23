
/**
 * Data structures describing a set of conversions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.converter;

import java.util.Map;
import org.joshsim.engine.value.engine.EngineValueTuple;
import org.joshsim.engine.value.engine.EngineValueTuple.PairCreator;
import org.joshsim.engine.value.engine.EngineValueTuple.PairTable;
import org.joshsim.util.CompositeKeyUtil;


/**
 * Store of available conversion operations between different units.
 */
public class MapConverter implements Converter {

  private final Map<EngineValueTuple.UnitsTuple, Conversion> conversions;
  private final ThreadLocal<PairTable<Units, Units, Conversion>> conversionCache;
  private final PairCreator<Units, Units, Conversion> conversionCreator;

  /**
   * Constructs a new Converter with the specified conversion mappings.
   *
   * @param conversions a map of unit tuples to their corresponding conversion operations
   */
  public MapConverter(Map<EngineValueTuple.UnitsTuple, Conversion> conversions) {
    this.conversions = conversions;
    this.conversionCache = ThreadLocal.withInitial(PairTable::new);

    // Held in a field rather than written as this::computeConversion at the call site: a bound
    // method reference allocates a fresh object on each evaluation, which would defeat the
    // allocation-free lookup the cache exists to provide.
    this.conversionCreator = this::computeConversion;
  }

  /**
   * Get a conversion between two unit types.
   *
   * <p>Results are cached per thread keyed on the dense identities of the units pair, so the
   * steady-state lookup path allocates nothing. On a cache miss, the content-based
   * UnitsTuple lookup below runs as before, including its error behavior.</p>
   *
   * <p>The two identities are packed into one long by
   * {@link CompositeKeyUtil#packIds(int, int)}, which places the source units in the high 32 bits
   * and the destination in the low 32 bits. That keeps the key order sensitive, so converting
   * from metres to feet is cached separately from feet to metres, and lets the cache be keyed on
   * a primitive rather than on a boxed pair.</p>
   *
   * @param oldUnits the source units
   * @param newUnits the destination units
   * @return a Conversion that can convert between the specified units
   * @throws IllegalArgumentException if no conversion exists between the units
   */
  public Conversion getConversion(Units oldUnits, Units newUnits) {
    long key = CompositeKeyUtil.packIds(oldUnits.getId(), newUnits.getId());
    return conversionCache.get().getOrPut(key, oldUnits, newUnits, conversionCreator);
  }

  /**
   * Compute a conversion between two unit types without any caching.
   *
   * @param oldUnits the source units
   * @param newUnits the destination units
   * @return a Conversion that can convert between the specified units
   * @throws IllegalArgumentException if no conversion exists between the units
   */
  private Conversion computeConversion(Units oldUnits, Units newUnits) {
    EngineValueTuple.UnitsTuple tuple = new EngineValueTuple.UnitsTuple(oldUnits, newUnits);

    // First check if there's an explicit conversion (includes aliases via NoopConversion)
    if (conversions.containsKey(tuple)) {
      return conversions.get(tuple);
    }

    // If no explicit conversion exists, check if units are inherently compatible
    if (tuple.getAreCompatible()) {
      return new NoopConversion(newUnits);
    }

    // No conversion found
    String message = String.format(
        "No conversion exists between \"%s\" and \"%s\".",
        oldUnits,
        newUnits
    );
    throw new IllegalArgumentException(message);
  }

}
