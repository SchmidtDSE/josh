/**
 * Structures describing a conversion between two units through a single callable.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import org.joshsim.engine.func.CompiledCallable;


/**
 * A conversion rule between two unit types using a single conversion callable.
 */
public class DirectConversion implements Conversion {

  private final String sourceUnits;
  private final String destinationUnits;
  private final CompiledCallable conversionCallable

  @Override
  public String getSourceUnits() {
    
  }

  @Override
  public String getDestinationUnits() {
    
  }

  @Override
  public CompiledCallable getConversionCallable() {
    
  }

}
