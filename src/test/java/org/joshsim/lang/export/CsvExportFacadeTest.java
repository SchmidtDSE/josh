/**
 * Tests for CSV export logic.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joshsim.engine.entity.base.Entity;
import org.junit.jupiter.api.Test;


/**
 * Unit test suite for the CsvExportFacade.
 */
class CsvExportFacadeTest {

  @Test
  void testStartWhenAlreadyActiveNoActionPerformed() {
    // Arrange
    OutputStreamStrategy outputStrategyMock = mock(OutputStreamStrategy.class);
    CsvExportFacade csvExportFacade = new CsvExportFacade(outputStrategyMock);

    AtomicBoolean activeState = getActiveState(csvExportFacade);
    activeState.set(true); // Simulate active state

    // Act
    csvExportFacade.start();

    // Assert
    assertTrue(activeState.get(), "Facade should remain active");
    try {
      verify(outputStrategyMock, times(0)).open();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

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

  private AtomicBoolean getActiveState(CsvExportFacade csvExportFacade) {
    try {
      var activeField = CsvExportFacade.class.getDeclaredField("active");
      activeField.setAccessible(true);
      return (AtomicBoolean) activeField.get(csvExportFacade);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Could not access active field", e);
    }
  }

}
