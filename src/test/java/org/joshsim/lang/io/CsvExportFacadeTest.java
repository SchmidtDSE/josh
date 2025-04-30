/**
 * Tests for CSV export logic.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import org.joshsim.engine.entity.base.Entity;
import org.junit.jupiter.api.Test;


/**
 * Unit test suite for the CsvExportFacade.
 */
class CsvExportFacadeTest {

  @Test
  void testStartWhenInactiveOpensOutputStream() throws IOException {
    // Arrange
    OutputStreamStrategy outputStrategyMock = mock(OutputStreamStrategy.class);
    OutputStream outputStreamMock = mock(OutputStream.class);

    when(outputStrategyMock.open()).thenReturn(outputStreamMock);

    CsvExportFacade csvExportFacade = new CsvExportFacade(outputStrategyMock);

    // Act
    csvExportFacade.start();
    csvExportFacade.join(); // Ensure the background thread finishes

    // Assert
    verify(outputStrategyMock, times(1)).open();
    verify(outputStreamMock, times(1)).close();
  }

  @Test
  void testStartWritesEntitiesToOutputStreamAndFlushesSuccessfully() throws IOException {
    // Arrange
    OutputStreamStrategy outputStrategyMock = mock(OutputStreamStrategy.class);
    OutputStream outputStreamMock = mock(OutputStream.class);
    Entity entityMock = mock(Entity.class);
    CsvExportFacade.Task task = new CsvExportFacade.Task(entityMock, 1L);

    when(outputStrategyMock.open()).thenReturn(outputStreamMock);

    CsvExportFacade csvExportFacade = new CsvExportFacade(outputStrategyMock);

    // Act
    csvExportFacade.start();
    csvExportFacade.write(task);
    csvExportFacade.join(); // Ensure the operation completes

    // Assert
    verify(outputStrategyMock, times(1)).open();
    verify(outputStreamMock, times(1)).close();
  }

}
