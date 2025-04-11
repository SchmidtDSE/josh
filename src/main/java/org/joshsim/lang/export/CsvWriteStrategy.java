
package org.joshsim.lang.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;


public class CsvWriteStrategy implements ExportWriteStrategy<Map<String, String>> {

  @Override
  public void write(Stream<Map<String, String>> records, OutputStream output) throws IOException {
    try (OutputStreamWriter writer = new OutputStreamWriter(output);
         CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
      
      // Get the first record to determine headers
      Map<String, String> firstRecord = records.findFirst().orElse(null);
      if (firstRecord == null) {
        return;
      }
      
      // Write headers
      printer.printRecord(firstRecord.keySet());
      
      // Write first record
      printer.printRecord(firstRecord.values());
      
      // Write remaining records
      records.forEach(record -> {
        try {
          printer.printRecord(record.values());
        } catch (IOException e) {
          throw new RuntimeException("Failed to write CSV record due to: " + e);
        }
      });
    }
  }
}
