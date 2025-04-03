package org.joshsim.engine.external.cog;

import java.io.IOException;
import org.joshsim.engine.external.core.ExternalLayer;
import org.joshsim.engine.external.core.Request;
import org.joshsim.engine.value.type.RealizedDistribution;

/**
 * Concrete implementation of ExternalLayer that uses CogReader to read COG files.
 */
public class CogExternalLayer implements ExternalLayer {
  private final CogReader cogReader;
  
  /**
   * Creates a new COG external layer.
   *
   * @param cogReader The COG reader to use
   */
  public CogExternalLayer(CogReader cogReader) {
    this.cogReader = cogReader;
  }
  
  @Override
  public RealizedDistribution fulfill(Request request) {
    try {
      return cogReader.readValues(request.getPath(), request.getGeometry());
    } catch (IOException e) {
      throw new RuntimeException("Failed to read COG file: " + request.getPath(), e);
    }
  }
}