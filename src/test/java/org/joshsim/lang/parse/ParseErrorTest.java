
package org.joshsim.lang.parse;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ParseErrorTest {

    @Test
    public void testParseErrorGetters() {
        int line = 42;
        String message = "Unexpected token";
        ParseError error = new ParseError(line, message);
        
        assertEquals(line, error.getLine(), "Line number should match constructor value");
        assertEquals(message, error.getMessage(), "Message should match constructor value");
    }
}
