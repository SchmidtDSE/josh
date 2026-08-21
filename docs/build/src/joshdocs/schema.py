"""The authoring contract for a documentation unit.

A unit is one ``.josh`` model plus its same-stem ``.md`` sidecar. The sidecar's YAML front-matter is
modelled here with pydantic so that a malformed field produces a message an author can act on --
``order: expected an integer, got 'thirty'`` with a file and line -- rather than a traceback.

Every field has a default that suits the common case, so a minimal sidecar declares only ``kind``,
``title``, and ``description``. The rules enforced below exist because contradictory combinations
are silently
meaningless otherwise: a snippet that is not runnable cannot carry assertions, and a model expected
to fail parsing cannot be run at all.
"""

from __future__ import annotations

import re
from enum import StrEnum
from pathlib import Path

from pydantic import BaseModel, ConfigDict, Field, ValidationError, field_validator, model_validator

from .errors import Problem

#: Ids become URL segments and JUnit test names, so they are restricted to a portable alphabet.
#: Underscores are allowed because conformance ids are filename stems such as
#: ``test_entity_update_basic``, and that stem is the join key to the JUnit results.
ID_PATTERN = re.compile(r"^[a-z0-9][a-z0-9_-]*$")

#: Matches the seed the conformance runner passes today (JoshConformanceTest passes --seed 42).
DEFAULT_SEED = 42

#: Units sort by ``order`` within a destination; this default leaves room on both sides.
DEFAULT_ORDER = 100

#: How long a description may be. It is a subtitle, shown under the title on an index card and as
#: the tagline of the unit's own page, so past this length it stops summarizing the page and starts
#: being it. The prose below the front-matter is where the explanation goes.
MAX_DESCRIPTION = 200


class Kind(StrEnum):
    """What a unit is for, which decides how it is rendered and whether it runs."""

    GUIDE = "guide"
    RECIPE = "recipe"
    REFERENCE = "reference"
    TEST = "test"


class Expect(StrEnum):
    """Whether the model is expected to be valid Josh.

    ``PARSE_ERROR`` exists because the corpus contains deliberate syntax errors asserted by
    ``assert_not_ok`` in ``examples/validate.sh``; without it the harvester would report them as
    failures.
    """

    VALID = "valid"
    PARSE_ERROR = "parse-error"


class Status(StrEnum):
    """Whether a unit is live or a placeholder for unimplemented syntax.

    ``RESERVED`` records, reviewably, what is today an unexplained comment in
    ``examples/validate.sh``: syntax that is accepted by the docs as a forward reference but is not
    yet implemented by the engine. Reserved units are never validated or run.
    """

    ACTIVE = "active"
    RESERVED = "reserved"


class ExportSlot(StrEnum):
    """An ``exportFiles`` attribute that the harvester can retarget with an overlay."""

    PATCH = "patch"
    META = "meta"
    ENTITY = "entity"


#: Guides and recipes are runnable demonstrations; reference snippets are excerpts that usually are
#: not complete models. Authors override per unit with ``runnable:``.
RUNNABLE_BY_DEFAULT: dict[Kind, bool] = {
    Kind.GUIDE: True,
    Kind.RECIPE: True,
    Kind.REFERENCE: False,
    Kind.TEST: True,
}


class FrontMatter(BaseModel):
    """The YAML front-matter of a unit's ``.md`` sidecar.

    Unknown keys are rejected rather than ignored, so a typo in a field name is a build failure
    instead of a setting that silently never took effect.
    """

    model_config = ConfigDict(extra="forbid", populate_by_name=True, str_strip_whitespace=True)

    id: str | None = None
    kind: Kind
    title: str
    #: One sentence saying what the page is for. Required, because a title alone is not an answer:
    #: an index of links reading "evalDuration cannot be assigned" tells a reader nothing about
    #: what is on the other end of one.
    description: str
    destination: str | None = None
    order: int = DEFAULT_ORDER
    runnable: bool | None = None
    # `assert` is a Python keyword, so the field is named `asserts` and aliased to the YAML key.
    asserts: bool = Field(default=False, alias="assert")
    simulation: str | None = None
    exports: list[ExportSlot] = Field(default_factory=list)
    #: A `.josh` file beside this unit holding `update` stanzas to append for runs. Naming a file
    #: here is also what marks it as a fragment rather than a unit of its own, since a bare
    #: `update` stanza does not validate on its own -- the engine rejects it with "no prior
    #: definition exists" -- and so cannot be a documentation unit.
    overlay: str | None = None
    #: Preprocessed `.jshd` inputs the model needs in its working directory to run. Declared rather
    #: than derived: `externals` names what the model reaches, but the file behind a name is not
    #: implied by it -- a geotiff or netcdf external carries its path inside a `start external`
    #: block. These files are also not in the repository, since `.gitignore` excludes `*.jshd` and
    #: CI preprocesses them into an artifact, so this list is the only record that a run needs them.
    data: list[str] = Field(default_factory=list)
    seed: int = DEFAULT_SEED
    expect: Expect = Expect.VALID
    status: Status = Status.ACTIVE
    reason: str | None = None
    tags: list[str] = Field(default_factory=list)

    @field_validator("id")
    @classmethod
    def _check_id(cls, value: str | None) -> str | None:
        """Reject ids that would not survive a URL or a JUnit test name."""
        if value is not None and not ID_PATTERN.match(value):
            raise ValueError(
                f"{value!r} is not a valid id: use lowercase letters, digits, '-' and '_', "
                "starting with a letter or digit"
            )
        return value

    @field_validator("title")
    @classmethod
    def _check_title(cls, value: str) -> str:
        """Reject a blank title, which would render as an empty heading."""
        if not value.strip():
            raise ValueError("title must not be empty")
        return value

    @field_validator("description")
    @classmethod
    def _check_description(cls, value: str) -> str:
        """Reject a description that is empty or long enough to stop being a subtitle."""
        collapsed = " ".join(value.split())
        if not collapsed:
            raise ValueError("description must not be empty")
        if len(collapsed) > MAX_DESCRIPTION:
            raise ValueError(
                f"description must be at most {MAX_DESCRIPTION} characters, got {len(collapsed)}: "
                "it is the one-line subtitle under the title, not the page's opening paragraph"
            )
        if "`" in collapsed:
            # The description is shown as text, so a backtick reaches the reader as a backtick.
            # Caught here because the alternative is noticing it on the published page.
            raise ValueError(
                "description is shown as plain text, not rendered as Markdown, so drop the "
                "backticks; the prose below the front-matter is where formatting works"
            )
        return collapsed

    @field_validator("destination")
    @classmethod
    def _check_destination(cls, value: str | None) -> str | None:
        """Reject destinations that would escape the output tree."""
        if value is None:
            return None
        cleaned = value.strip().strip("/")
        if not cleaned:
            raise ValueError("destination must name a directory, or be omitted")
        parts = Path(cleaned).parts
        if ".." in parts or Path(cleaned).is_absolute():
            raise ValueError(f"{value!r} must be a relative path without '..'")
        return cleaned

    @field_validator("overlay")
    @classmethod
    def _check_overlay(cls, value: str | None) -> str | None:
        """Reject an overlay that is not a plain `.josh` filename beside the unit."""
        if value is None:
            return None
        cleaned = value.strip()
        if not cleaned.endswith(".josh"):
            raise ValueError(f"{value!r} must name a .josh file")
        if len(Path(cleaned).parts) != 1 or cleaned in {".", ".."}:
            raise ValueError(
                f"{value!r} must be a bare filename: an overlay lives beside the model it updates"
            )
        return cleaned

    @field_validator("seed")
    @classmethod
    def _check_seed(cls, value: int) -> int:
        """Reject a negative seed, which the run command does not accept."""
        if value < 0:
            raise ValueError(f"seed must not be negative, got {value}")
        return value

    @field_validator("data", "tags")
    @classmethod
    def _check_no_blanks(cls, value: list[str]) -> list[str]:
        """Reject blank list entries, which are almost always a stray dash."""
        if any(not entry.strip() for entry in value):
            raise ValueError("entries must not be empty")
        return [entry.strip() for entry in value]

    @model_validator(mode="after")
    def _check_combination(self) -> FrontMatter:
        """Apply the defaults that depend on other fields, then reject contradictions."""
        if self.runnable is None:
            self.runnable = RUNNABLE_BY_DEFAULT[self.kind]

        if self.status is Status.RESERVED and not self.reason:
            raise ValueError(
                "status: reserved requires reason: <why this syntax is not implemented yet>"
            )
        if self.reason and self.status is not Status.RESERVED:
            raise ValueError("reason: only applies to status: reserved")

        if self.expect is Expect.PARSE_ERROR and self.runnable:
            raise ValueError(
                "expect: parse-error cannot be runnable: a model that does not parse cannot run"
            )
        if self.expect is Expect.PARSE_ERROR and self.asserts:
            raise ValueError("expect: parse-error cannot carry assert: true")

        if self.asserts and not self.runnable:
            raise ValueError("assert: true requires runnable: true")
        if self.exports and not self.runnable:
            raise ValueError("exports: requires runnable: true")
        # An overlay is `update <type> <Name>` stanzas the author wrote, so it names its own
        # targets; unlike `exports`, it does not need `simulation` to know what to update. A
        # parse-error unit is caught by the runnable rule above, since it cannot be runnable.
        if self.overlay and not self.runnable:
            raise ValueError("overlay: requires runnable: true")

        # The overlay is `update simulation <Name>`, so it has to know which stanza to update. The
        # name is not inferred here on purpose: only the engine knows what a file declares, and
        # asking it needs a jar-side simulation lookup that does not exist yet (see joshjar).
        if self.exports and not self.simulation:
            raise ValueError(
                "exports: requires simulation: <Name> so the overlay knows which stanza to update"
            )

        return self


def locate_key(raw_frontmatter: str, key: str) -> int | None:
    """Find the line a front-matter key is declared on.

    YAML parsers hand back a dict without positions, so the key is located by scanning the raw
    block. This is good enough for the common case of a top-level key and it degrades to None
    rather than guessing.

    Args:
        raw_frontmatter: The front-matter text, excluding the ``---`` delimiters.
        key: The key to locate.

    Returns:
        The one-based line number in the file, or None when the key cannot be found.
    """
    pattern = re.compile(rf"^\s*{re.escape(key)}\s*:")
    for offset, line in enumerate(raw_frontmatter.splitlines()):
        if pattern.match(line):
            # Line 1 of the file is the opening `---`, so the block's first line is line 2.
            return offset + 2
    return None


def _humanize(error: dict) -> str:
    """Turn one pydantic error into a sentence about the author's YAML.

    Args:
        error: An entry from :meth:`ValidationError.errors`.

    Returns:
        The message, without the field name prefix.
    """
    kind = error.get("type", "")
    given = error.get("input")

    if kind == "missing":
        return "required field is missing"
    if kind == "extra_forbidden":
        return "unknown field (see docs/build/README.md for the fields a sidecar may declare)"
    if kind in {"int_parsing", "int_type", "int_from_float"}:
        return f"expected an integer, got {given!r}"
    if kind in {"bool_parsing", "bool_type"}:
        return f"expected true or false, got {given!r}"
    if kind in {"string_type", "string_too_short"}:
        return f"expected text, got {given!r}"
    if kind in {"list_type", "iterable_type"}:
        return f"expected a list, got {given!r}"
    if kind == "enum":
        expected = error.get("ctx", {}).get("expected", "")
        return f"expected {expected}, got {given!r}"
    return error.get("msg", "is invalid")


def _field_name(error: dict) -> str | None:
    """Return the author-visible YAML key an error refers to, if it names one."""
    location = error.get("loc") or ()
    for part in location:
        if isinstance(part, str):
            # The model field is `asserts`; the author wrote `assert`.
            return "assert" if part == "asserts" else part
    return None


def problems_from_validation_error(
    error: ValidationError,
    path: Path,
    raw_frontmatter: str,
    fallback_line: int | None = None,
) -> list[Problem]:
    """Translate a pydantic failure into author-facing problems.

    Args:
        error: The validation error raised while parsing front-matter.
        path: The sidecar the front-matter came from.
        raw_frontmatter: The front-matter text, used to locate keys by line.
        fallback_line: Line to report when a key cannot be located.

    Returns:
        One problem per validation error, in the order pydantic reported them.
    """
    problems: list[Problem] = []
    for entry in error.errors():
        name = _field_name(entry)
        message = _humanize(entry)
        if name is None:
            problems.append(Problem(path, message, fallback_line))
            continue
        line = locate_key(raw_frontmatter, name) or fallback_line
        problems.append(Problem(path, f"{name}: {message}", line))
    return problems
