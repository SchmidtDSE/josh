package org.joshsim.engine.entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventHandlerGroupBuilderTest {
    private EventHandlerGroupBuilder builder;
    private EventHandler mockHandler1;
    private EventHandler mockHandler2;
    private static final String TEST_STATE = "testState";
    private static final String TEST_ATTRIBUTE = "testAttribute";
    private static final String TEST_EVENT = "testEvent";

    @BeforeEach
    public void setUp() {
        builder = new EventHandlerGroupBuilder();
        mockHandler1 = mock(EventHandler.class);
        mockHandler2 = mock(EventHandler.class);
    }

    @Test
    public void testBuildCompleteEventHandlerGroup() {
        builder.setState(TEST_STATE);
        builder.setAttribute(TEST_ATTRIBUTE);
        builder.setEvent(TEST_EVENT);
        builder.addEventHandler(mockHandler1);

        EventHandlerGroup group = builder.build();

        assertNotNull(group);
        assertTrue(group.getEventHandlers().iterator().hasNext());
        assertEquals(mockHandler1, group.getEventHandlers().iterator().next());
        assertEquals(TEST_STATE, group.getEventKey().getState());
        assertEquals(TEST_ATTRIBUTE, group.getEventKey().getAttribute());
        assertEquals(TEST_EVENT, group.getEventKey().getEvent());
    }

    @Test
    public void testStateValidation() {
        assertThrows(IllegalStateException.class, () -> builder.getState(),
                "State not set should throw exception");

        builder.setState(TEST_STATE);
        assertEquals(TEST_STATE, builder.getState());
    }

    @Test
    public void testAttributeValidation() {
        assertThrows(IllegalStateException.class, () -> builder.getAttribute(),
                "Attribute not set should throw exception");

        builder.setAttribute(TEST_ATTRIBUTE);
        assertEquals(TEST_ATTRIBUTE, builder.getAttribute());
    }

    @Test
    public void testEventValidation() {
        assertThrows(IllegalStateException.class, () -> builder.getEvent(),
                "Event not set should throw exception");

        builder.setEvent(TEST_EVENT);
        assertEquals(TEST_EVENT, builder.getEvent());
    }

    @Test
    public void testAddSingleEventHandler() {
        builder.addEventHandler(mockHandler1);
        builder.setState(TEST_STATE);
        builder.setAttribute(TEST_ATTRIBUTE);
        builder.setEvent(TEST_EVENT);

        EventHandlerGroup group = builder.build();
        assertTrue(group.getEventHandlers().iterator().hasNext());
        assertEquals(mockHandler1, group.getEventHandlers().iterator().next());
    }

    @Test
    public void testAddMultipleEventHandlers() {
        List<EventHandler> handlers = Arrays.asList(mockHandler1, mockHandler2);
        builder.addEventHandler(handlers);
        builder.setState(TEST_STATE);
        builder.setAttribute(TEST_ATTRIBUTE);
        builder.setEvent(TEST_EVENT);

        EventHandlerGroup group = builder.build();
        int count = 0;
        for (EventHandler handler : group.getEventHandlers()) {
            assertTrue(handlers.contains(handler));
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testClear() {
        builder.setState(TEST_STATE);
        builder.setAttribute(TEST_ATTRIBUTE);
        builder.setEvent(TEST_EVENT);
        builder.addEventHandler(mockHandler1);

        builder.clear();

        builder.setState(TEST_STATE);
        builder.setAttribute(TEST_ATTRIBUTE);
        builder.setEvent(TEST_EVENT);
        EventHandlerGroup group = builder.build();
        assertFalse(group.getEventHandlers().iterator().hasNext(), "Event handlers should be cleared");
    }

    @Test
    public void testBuildWithRequiredFieldsOnly() {
        builder.setState(TEST_STATE);
        builder.setAttribute(TEST_ATTRIBUTE);
        builder.setEvent(TEST_EVENT);

        EventHandlerGroup group = builder.build();
        assertNotNull(group);
        assertEquals(TEST_STATE, group.getEventKey().getState());
        assertEquals(TEST_ATTRIBUTE, group.getEventKey().getAttribute());
        assertEquals(TEST_EVENT, group.getEventKey().getEvent());
        assertFalse(group.getEventHandlers().iterator().hasNext());
    }
}