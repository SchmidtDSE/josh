"""Loading of a unit's prose sidecar.

A sidecar is the ``.md`` half of a unit: YAML front-matter carrying the authoring contract, then the
prose that surrounds the model on the rendered page. Parsing is separated from validation so that a
broken YAML block is reported with the line the parser objected to, rather than as a missing field.

The block is split here rather than by a front-matter library because the libraries coerce a
non-mapping block to an empty dict. That turns ``- a list`` where a mapping belongs into a pile of
"required field is missing" errors, when the useful message is that the block is the wrong shape.
"""

from __future__ import annotations

from pathlib import Path

import yaml

from .errors import Problem

#: Reported when the front-matter block never opened, so the message names the fix.
_NO_FRONTMATTER = (
    "no YAML front-matter found: the file must open with a '---' line, then the unit's fields, "
    "then a closing '---'"
)

#: Lines that close a front-matter block, following YAML's document markers.
_CLOSING = frozenset({"---", "..."})


class SidecarError(Exception):
    """Raised when a sidecar cannot be read far enough to validate it.

    Attributes:
        problem: The author-facing problem describing what went wrong.
    """

    def __init__(self, problem: Problem) -> None:
        """Initialize the error.

        Args:
            problem: The author-facing problem.
        """
        super().__init__(problem.message)
        self.problem = problem


class Sidecar:
    """A parsed sidecar.

    Attributes:
        path: The sidecar's path.
        metadata: The front-matter mapping, before schema validation.
        raw_frontmatter: The front-matter text without its delimiters, used to locate keys by line.
        body: The prose following the front-matter.
    """

    __slots__ = ("body", "metadata", "path", "raw_frontmatter")

    def __init__(self, path: Path, metadata: dict, raw_frontmatter: str, body: str) -> None:
        """Initialize a sidecar.

        Args:
            path: The sidecar's path.
            metadata: The front-matter mapping.
            raw_frontmatter: The front-matter text without delimiters.
            body: The prose following the front-matter.
        """
        self.path = path
        self.metadata = metadata
        self.raw_frontmatter = raw_frontmatter
        self.body = body


def split_frontmatter(text: str) -> tuple[str, str]:
    """Split a sidecar into its front-matter block and its prose.

    Args:
        text: The full sidecar text.

    Returns:
        The front-matter text without delimiters, and the prose after the closing delimiter. Both
        are empty when the file opens no block.
    """
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        return "", ""
    for offset, line in enumerate(lines[1:], start=1):
        if line.strip() in _CLOSING:
            body = "\n".join(lines[offset + 1 :])
            return "\n".join(lines[1:offset]), body.lstrip("\n")
    return "", ""


def extract_raw_frontmatter(text: str) -> str:
    """Return only the front-matter block of a sidecar.

    Args:
        text: The full sidecar text.

    Returns:
        The front-matter text, or an empty string when the file opens no block.
    """
    return split_frontmatter(text)[0]


def load_sidecar(path: Path) -> Sidecar:
    """Read and parse a sidecar.

    Args:
        path: The ``.md`` file to read.

    Returns:
        The parsed sidecar.

    Raises:
        SidecarError: If the file cannot be read, opens no front-matter block, or the block is not
            a YAML mapping.
    """
    try:
        text = path.read_text(encoding="utf-8")
    except OSError as exc:
        raise SidecarError(Problem(path, f"could not be read: {exc.strerror or exc}")) from exc

    raw, body = split_frontmatter(text)
    if not raw.strip():
        raise SidecarError(Problem(path, _NO_FRONTMATTER, 1))

    try:
        metadata = yaml.safe_load(raw)
    except yaml.YAMLError as exc:
        mark = getattr(exc, "problem_mark", None)
        # The block starts on line 2 of the file, and yaml marks are zero-based.
        line = mark.line + 2 if mark is not None else 2
        detail = getattr(exc, "problem", None) or str(exc).splitlines()[0]
        message = f"front-matter is not valid YAML: {detail}"
        raise SidecarError(Problem(path, message, line)) from exc

    if not isinstance(metadata, dict):
        raise SidecarError(
            Problem(path, "front-matter must be a mapping of fields to values, one per line", 2)
        )

    return Sidecar(path=path, metadata=metadata, raw_frontmatter=raw, body=body)
