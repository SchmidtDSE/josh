/**
 * Tests for BatchArgUtil.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;


/**
 * Unit tests for {@link BatchArgUtil}.
 */
class BatchArgUtilTest {

  @Test
  void encodeCustomTags_emptyMap_returnsEmptyString() {
    assertEquals("", BatchArgUtil.encodeCustomTags(Map.of()));
  }

  @Test
  void encodeCustomTags_singleEntry_returnsKeyEqualsValue() {
    assertEquals("run_hash=abc123",
        BatchArgUtil.encodeCustomTags(Map.of("run_hash", "abc123")));
  }

  @Test
  void encodeCustomTags_multipleEntries_areNewlineDelimited() {
    Map<String, String> tags = new LinkedHashMap<>();
    tags.put("run_hash", "abc123");
    tags.put("region", "west");
    assertEquals("run_hash=abc123\nregion=west", BatchArgUtil.encodeCustomTags(tags));
  }

  @Test
  void encodeCustomTags_preservesLinkedHashMapInsertionOrder() {
    Map<String, String> tags = new LinkedHashMap<>();
    tags.put("z", "1");
    tags.put("a", "2");
    tags.put("m", "3");
    assertEquals("z=1\na=2\nm=3", BatchArgUtil.encodeCustomTags(tags));
  }

  @Test
  void encodeCustomTags_valueWithEqualsSign_isPreservedVerbatim() {
    // Decoder splits on the first '=' so values may contain '='
    assertEquals("expr=a=b+c",
        BatchArgUtil.encodeCustomTags(Map.of("expr", "a=b+c")));
  }
}
