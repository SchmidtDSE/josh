
package org.joshsim.engine.value.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.value.type.Distribution;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.RealizedDistribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlicerTest {

    private Slicer slicer;
    @Mock private EngineValue mockSubject;
    @Mock private EngineValue mockSelections;
    @Mock private Distribution mockSubjectDistribution;
    @Mock private Distribution mockSelectionsDistribution;
    @Mock private EngineValueCaster mockCaster;

    @BeforeEach
    void setUp() {
        slicer = new Slicer();
        when(mockSubject.getAsDistribution()).thenReturn(mockSubjectDistribution);
        when(mockSelections.getAsDistribution()).thenReturn(mockSelectionsDistribution);
        when(mockSubject.getCaster()).thenReturn(mockCaster);
    }

    @Test
    void testSliceWithEqualSizes() {
        // Setup
        when(mockSubjectDistribution.getSize()).thenReturn(Optional.of(3));
        when(mockSelectionsDistribution.getSize()).thenReturn(Optional.of(3));

        List<EngineValue> subjectValues = new ArrayList<>();
        EngineValue value1 = mock(EngineValue.class);
        EngineValue value2 = mock(EngineValue.class);
        EngineValue value3 = mock(EngineValue.class);
        subjectValues.add(value1);
        subjectValues.add(value2);
        subjectValues.add(value3);

        List<EngineValue> selectionValues = new ArrayList<>();
        EngineValue selection1 = mock(EngineValue.class);
        EngineValue selection2 = mock(EngineValue.class);
        EngineValue selection3 = mock(EngineValue.class);
        selectionValues.add(selection1);
        selectionValues.add(selection2);
        selectionValues.add(selection3);

        when(mockSubjectDistribution.getContents(3, false))
            .thenReturn(subjectValues);
        when(mockSelectionsDistribution.getContents(3, false))
            .thenReturn(selectionValues);

        when(selection1.getAsBoolean()).thenReturn(true);
        when(selection2.getAsBoolean()).thenReturn(false);
        when(selection3.getAsBoolean()).thenReturn(true);

        // Execute
        EngineValue result = slicer.slice(mockSubject, mockSelections);

        // Verify
        Distribution resultDist = result.getAsDistribution();
        List<EngineValue> resultValues = new ArrayList<>();
        resultDist.getContents(2, false).forEach(resultValues::add);

        assertEquals(2, resultValues.size());
        assertEquals(value1, resultValues.get(0));
        assertEquals(value3, resultValues.get(1));
    }

    @Test
    void testSliceWithUnequalSizes() {
        // Setup
        when(mockSubjectDistribution.getSize()).thenReturn(Optional.of(3));
        when(mockSelectionsDistribution.getSize()).thenReturn(Optional.of(2));

        // Execute & Verify
        assertThrows(IllegalArgumentException.class,
            () -> slicer.slice(mockSubject, mockSelections));
    }

    @Test
    void testSliceWithEmptyDistributions() {
        // Setup
        when(mockSubjectDistribution.getSize()).thenReturn(Optional.of(0));
        when(mockSelectionsDistribution.getSize()).thenReturn(Optional.of(0));

        when(mockSubjectDistribution.getContents(0, false))
            .thenReturn(new ArrayList<>());
        when(mockSelectionsDistribution.getContents(0, false))
            .thenReturn(new ArrayList<>());

        // Execute
        EngineValue result = slicer.slice(mockSubject, mockSelections);

        // Verify
        Distribution resultDist = result.getAsDistribution();
        List<EngineValue> resultValues = new ArrayList<>();
        resultDist.getContents(0, false).forEach(resultValues::add);

        assertEquals(0, resultValues.size());
    }
}
