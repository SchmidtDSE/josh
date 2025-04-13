/**
 * Logic to write CSV files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * Implementation of the ExportWriteStrategy interface for writing CSV files.
 *
 * <p>Class  responsible for serializing a stream of records, where each record  is represented as a
 * Map (String, String), into a CSV format and writing it to an output stream. The first record in
 * the stream is used to extract and define the CSV headers. The headers are determined by the keys
 * of the map, and subsequent rows are written in the order of the corresponding values for the
 * keys.</p>
 */
public class CsvWriteStrategy implements ExportWriteStrategy<Map<String, String>> {

  private boolean onFirstRecord;
  private CSVPrinter printer;
  private OutputStreamWriter writer;

  /**
   * Create a new CSV write strategy for Map inputs.
   *
   * <p>Constructs a CsvWriteStrategy instance which will use the first record provided to sniff and
   * write a header row.</p>
   */
  public CsvWriteStrategy() {
    this.onFirstRecord = true;
    this.printer = null;
    this.writer = null;
  }

  @Override
  public void write(Map<String, String> record, OutputStream output) throws IOException {
    if (onFirstRecord) {
      this.writer = new OutputStreamWriter(output);
      this.printer = new CSVPrinter(writer, CSVFormat.DEFAULT
          .builder()
          .setHeader(record.keySet().toArray(new String[0]))
          .build());
      onFirstRecord = false;
    }
    printer.printRecord(record.values());
  }

  @Override
  public void flush() {
    if (printer != null) {
      try {
        printer.flush();
      } catch (IOException e) {
        throw new RuntimeException("Failed to flush CSVPrinter", e);
      }
    }

    if (writer != null) {
      try {
        writer.flush();
      } catch (IOException e) {
        throw new RuntimeException("Failed to flush OutputStreamWriter", e);
      }
    }
  }
}
