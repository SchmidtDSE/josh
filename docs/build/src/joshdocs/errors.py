"""Author-facing problem reporting.

The harvester's contract with a docs author is that one run reports every problem, each located by
file and -- where the source makes it knowable -- by line. Fixing a docs tree should take one round
of edits, not one round per mistake, so nothing in this package raises on the first bad unit.
"""

from __future__ import annotations

import contextlib
from collections.abc import Iterable, Iterator
from pathlib import Path


class HarvestFailed(Exception):
    """Raised when a harvest finishes with problems recorded.

    Attributes:
        log: The problems collected during the run.
    """

    def __init__(self, log: ProblemLog) -> None:
        """Initialize the exception with the problems that caused the failure.

        Args:
            log: The accumulated problems.
        """
        super().__init__(f"{len(log)} problem(s) found")
        self.log = log


class Problem:
    """A single author-facing problem.

    Attributes:
        path: File the problem was found in.
        message: What is wrong, phrased for the author who has to fix it.
        line: One-based line number, or None when the source offers no useful position.
    """

    __slots__ = ("line", "message", "path")

    def __init__(self, path: Path, message: str, line: int | None = None) -> None:
        """Initialize a problem.

        Args:
            path: File the problem was found in.
            message: What is wrong.
            line: One-based line number, if known.
        """
        self.path = path
        self.message = message
        self.line = line

    def __eq__(self, other: object) -> bool:
        """Compare by path, message, and line."""
        if not isinstance(other, Problem):
            return NotImplemented
        return (self.path, self.message, self.line) == (other.path, other.message, other.line)

    def __hash__(self) -> int:
        """Hash by path, message, and line."""
        return hash((self.path, self.message, self.line))

    def __repr__(self) -> str:
        """Return a debugging representation."""
        return f"Problem(path={self.path!r}, message={self.message!r}, line={self.line!r})"

    def format(self, root: Path | None = None) -> str:
        """Render the problem as a two-line block an author can act on.

        Args:
            root: Directory to make the path relative to, when the path is inside it.

        Returns:
            The formatted problem, for example ``docs/src/a.md:4`` then ``  order: ...``.
        """
        location = self.path
        if root is not None:
            with contextlib.suppress(ValueError):
                location = self.path.relative_to(root)
        prefix = f"{location}:{self.line}" if self.line is not None else str(location)
        return f"{prefix}\n  {self.message}"


class ProblemLog:
    """Accumulates problems so that a single run can report all of them.

    Attributes:
        root: Directory that reported paths are made relative to.
    """

    def __init__(self, root: Path | None = None) -> None:
        """Initialize an empty log.

        Args:
            root: Directory that reported paths are made relative to.
        """
        self.root = root
        self._problems: list[Problem] = []

    def add(self, path: Path, message: str, line: int | None = None) -> None:
        """Record a problem.

        Args:
            path: File the problem was found in.
            message: What is wrong.
            line: One-based line number, if known.
        """
        self._problems.append(Problem(path, message, line))

    def extend(self, problems: Iterable[Problem]) -> None:
        """Record several problems.

        Args:
            problems: The problems to record.
        """
        self._problems.extend(problems)

    @property
    def problems(self) -> list[Problem]:
        """Return the recorded problems, ordered by file then line."""
        return sorted(self._problems, key=lambda p: (str(p.path), p.line or 0, p.message))

    def report(self) -> str:
        """Render every problem as a block of text for stderr.

        Returns:
            The formatted report, or an empty string when nothing was recorded.
        """
        if not self._problems:
            return ""
        blocks = [problem.format(self.root) for problem in self.problems]
        count = len(self._problems)
        noun = "problem" if count == 1 else "problems"
        return "\n".join(blocks) + f"\n\n{count} {noun} found."

    def raise_if_any(self) -> None:
        """Raise :class:`HarvestFailed` when any problem was recorded."""
        if self._problems:
            raise HarvestFailed(self)

    def __len__(self) -> int:
        """Return the number of recorded problems."""
        return len(self._problems)

    def __iter__(self) -> Iterator[Problem]:
        """Iterate over the recorded problems in report order."""
        return iter(self.problems)

    def __bool__(self) -> bool:
        """Return True when at least one problem was recorded."""
        return bool(self._problems)
