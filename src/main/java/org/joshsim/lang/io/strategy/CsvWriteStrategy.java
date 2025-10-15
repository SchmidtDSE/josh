/**
 * Logic to write CSV files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.joshsim.lang.io.AppendOutputStream;

/**
 * Implementation of the ExportWriteStrategy interface for writing CSV files.
 *
 * <p>Class  responsible for serializing a stream of records, where each record  is represented as a
 * Map (String, String), into a CSV format and writing it to an output stream. The first record in
 * the stream is used to extract and define the CSV headers. The headers are determined by the keys
 * of the map, and subsequent rows are written in the order of the corresponding values for the
 * keys.</p>
 */
public class CsvWriteStrategy implements StringMapWriteStrategy {

  private boolean onFirstRecord;
  private CSVPrinter printer;
  private OutputStreamWriter writer;
  private Optional<Iterable<String>> header;

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
    this.header = Optional.empty();
  }

  /**
   * Create a new CSV write strategy for Map inputs with a specified set of column names.
   *
   * @param header Iterable over the header columns to use. If a record is missing a value, it will
   *     be an empty string.
   */
  public CsvWriteStrategy(Iterable<String> header) {
    this.onFirstRecord = true;
    this.printer = null;
    this.writer = null;
    this.header = Optional.of(header);
  }

  @Override
  public void write(Map<String, String> record, OutputStream output) throws IOException {
    if (header.isEmpty()) {
      header = Optional.of(convertIterableToList(record.keySet()));
    }

    Iterable<String> headerVals = header.orElseThrow();

    if (onFirstRecord) {
      String[] headerValsArray = convertIterableToArray(headerVals);

      this.writer = new OutputStreamWriter(output);

      // Check if we're appending to an existing file
      boolean skipHeader = output instanceof AppendOutputStream
          && ((AppendOutputStream) output).isAppendingToExistingFile();

      if (skipHeader) {
        // Skip header when appending to existing file
        this.printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
      } else {
        // Write header for new files or when not appending
        this.printer = new CSVPrinter(writer, CSVFormat.DEFAULT
            .builder()
            .setHeader(headerValsArray)
            .build());
      }

      onFirstRecord = false;
    }

    String[] values = StreamSupport.stream(headerVals.spliterator(), false)
        .map((key) -> record.getOrDefault(key, ""))
        .toArray(String[]::new);

    printer.printRecord((Object[]) values);
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

  @Override
  public void close() {
    flush();
  }

  private static List<String> convertIterableToList(Iterable<String> target) {
    Stream<String> targetStream = StreamSupport.stream(target.spliterator(), false);
    return targetStream.collect(Collectors.toList());
  }

  private static String[] convertIterableToArray(Iterable<String> target) {
    Stream<String> targetStream = StreamSupport.stream(target.spliterator(), false);
    return targetStream.toArray(String[]::new);
  }
}
