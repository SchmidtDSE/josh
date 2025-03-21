/**
 * Structures to describe language types.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringJoiner;


/**
 * Data structure desribing a language data type like decimal.
 */
public class LanguageType {

  private final Collection<String> distributionTypes;
  private final String rootType;

  public LanguageType(String rootType) {
    this.rootType = rootType;
    this.distributionTypes = new ArrayList<>();
  }

  public LanguageType(Collection<String> distributionTypes, String rootType) {
    this.distributionTypes = distributionTypes;
    this.rootType = rootType;
  }

  public boolean isDistribution() {
    return !distributionTypes.isEmpty();
  }

  public String getRootType() {
    return rootType;
  }

  public Iterable<String> getDistributionTypes() {
    return distributionTypes;
  }

  @Override
  public String toString() {
    if (isDistribution()) {
      StringJoiner joiner = new StringJoiner(" > ");
      for (String distributionType : getDistributionTypes()) {
        joiner.add(distributionType);
      }
      return joiner.toString();
    } else {
      return rootType;
    }
  }

  @Override
  public boolean equals(Object other) {
    return toString().equals(other.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
  
}
