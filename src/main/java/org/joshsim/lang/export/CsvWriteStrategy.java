package org.joshsim.lang.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.stream.Stream;


public class CsvWriteStrategy implements ExportWriteStrategy<Map<String, String>> {

  @Override
  public void write(Stream<Map<String, String>> records, OutputStream output) throws IOException {

  }

}
