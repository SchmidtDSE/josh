"""Synthesis of documentation units from the conformance suite.

The 155 tests under ``josh-tests`` already carry metadata in an ``@``-tagged comment header, so they
become documentation units without gaining a sidecar. Their ids are filename stems, which is what
``JoshConformanceTest.TestInfo.toString`` already emits into the JUnit XML -- so the join key from a
rendered page back to a CI result exists without touching any test file.

The header grammar mirrors ``TestMetadata.parse`` on the Java side, with one deliberate difference
noted on :func:`parse_header`.
"""

from __future__ import annotations

import re
from pathlib import Path

from .errors import Problem

#: Same pattern as TestMetadata.parse: a comment, an @tag, a colon, then the value.
TAG_PATTERN = re.compile(r"^#\s*@(\w+):\s*(.+)$")

#: Tags the Java parser recognizes. Anything else in a header is ignored there and here.
KNOWN_TAGS = ("category", "subcategory", "priority", "issue", "description")

#: Tags every conformance test in the suite carries today, so absence is an authoring mistake.
REQUIRED_TAGS = ("category", "subcategory", "priority", "description")

#: Priorities in use. Only "critical" carries behaviour (the conformanceTestCritical gate);
#: "normal" is a legacy synonym for "medium" surviving in three files.
KNOWN_PRIORITIES = frozenset({"critical", "high", "medium", "low", "normal"})


class TestHeader:
    """Metadata parsed from a conformance test's comment header.

    Attributes:
        category: Top-level grouping, matching the directory under ``josh-tests/conformance``.
        subcategory: Second-level grouping.
        priority: One of :data:`KNOWN_PRIORITIES`.
        issue: Related issue reference, when the test tracks one.
        description: What the test proves, joined across continuation lines.
    """

    __slots__ = ("category", "description", "issue", "priority", "subcategory")

    def __init__(
        self,
        category: str | None = None,
        subcategory: str | None = None,
        priority: str | None = None,
        issue: str | None = None,
        description: str | None = None,
    ) -> None:
        """Initialize a header.

        Args:
            category: Top-level grouping.
            subcategory: Second-level grouping.
            priority: Test priority.
            issue: Related issue reference.
            description: What the test proves.
        """
        self.category = category
        self.subcategory = subcategory
        self.priority = priority
        self.issue = issue
        self.description = description


def parse_header(text: str) -> TestHeader:
    """Parse the ``@``-tagged header at the top of a conformance test.

    Reading stops at the first line that is neither a comment nor blank, matching
    ``TestMetadata.parse``.

    One difference from the Java parser: a comment line that continues the previous tag is appended
    to that tag's value rather than dropped. Sixteen of the 155 tests wrap their description, and
    Java truncates those mid-sentence -- harmless for a test name, but the description is body text
    on a rendered page.

    Args:
        text: The test's source.

    Returns:
        The parsed header, with None for any tag the file does not carry.
    """
    values: dict[str, str] = {}
    current: str | None = None

    for line in text.splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        if not stripped.startswith("#"):
            break

        match = TAG_PATTERN.match(stripped)
        if match:
            key = match.group(1)
            current = key if key in KNOWN_TAGS else None
            if current is not None:
                values[current] = match.group(2).strip()
            continue

        if current is not None:
            continuation = stripped.lstrip("#").strip()
            if continuation:
                values[current] = f"{values[current]} {continuation}"

    return TestHeader(**{tag: values.get(tag) for tag in KNOWN_TAGS})


def title_from_stem(stem: str) -> str:
    """Turn a test's filename stem into a title for an index or a page heading.

    The description is a sentence about what the test proves, which is body text rather than a
    heading, so the stem is the better title: ``test_entity_update_basic`` reads as "Entity update
    basic".

    Args:
        stem: The filename stem, with or without the ``test_`` prefix.

    Returns:
        The title.
    """
    words = stem.removeprefix("test_").replace("_", " ").strip()
    return words[:1].upper() + words[1:] if words else stem


def read_header(path: Path) -> TestHeader:
    """Parse the header of a conformance test file.

    Args:
        path: The test file to read.

    Returns:
        The parsed header.

    Raises:
        OSError: If the file cannot be read.
    """
    return parse_header(path.read_text(encoding="utf-8"))


def header_problems(path: Path, header: TestHeader) -> list[Problem]:
    """Check a header for the tags a documentation unit needs.

    Args:
        path: The test file the header came from.
        header: The parsed header.

    Returns:
        One problem per missing or unrecognized tag.
    """
    problems: list[Problem] = []
    for tag in REQUIRED_TAGS:
        if not getattr(header, tag):
            problems.append(Problem(path, f"conformance header is missing '# @{tag}:'"))
    if header.priority and header.priority not in KNOWN_PRIORITIES:
        expected = ", ".join(sorted(KNOWN_PRIORITIES))
        problems.append(
            Problem(path, f"@priority: {header.priority!r} is not one of {expected}")
        )
    return problems
