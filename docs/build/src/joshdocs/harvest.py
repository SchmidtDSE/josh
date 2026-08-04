"""Harvesting: pair, validate, and record every documentation unit.

The harvest walks the authored tree and the conformance suite, pairs each model with its prose,
validates the pairing and the front-matter, asks the jar the questions only the engine can answer,
and writes ``docs-manifest.json``.

Two properties matter more than speed here. Nothing is skipped silently: a model without a sidecar,
a sidecar without a model, and a duplicate id are all failures, because the problem this pipeline
exists to solve is examples that no build step looks at. And a failing run reports every problem it
found, not the first.
"""

from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from pydantic import ValidationError

from .conformance import header_problems, read_header, title_from_stem
from .errors import Problem, ProblemLog
from .joshjar import JoshJar, first_error_line
from .manifest import Manifest, Unit, summarize
from .overlay import emit_runnable
from .schema import (
    ID_PATTERN,
    Expect,
    FrontMatter,
    Kind,
    Status,
    locate_key,
    problems_from_validation_error,
)
from .sidecar import SidecarError, load_sidecar

#: Authored content: one `.josh` plus one same-stem `.md` per unit.
DEFAULT_SRC = Path("docs/src")

#: The conformance suite, harvested for its `@`-tagged headers.
DEFAULT_TESTS = Path("josh-tests/conformance")

DEFAULT_MANIFEST = Path("build/docs/docs-manifest.json")
DEFAULT_RUNNABLE_DIR = Path("build/docs/runnable")
DEFAULT_EXPORT_DIR = Path("build/docs/exports")

#: Markdown that documents a directory rather than a unit, and so needs no model beside it.
SIDECAR_EXEMPT = frozenset({"README.md", "index.md"})

#: Directory under the authored tree to unit kind, so the tree describes itself and `kind:` in
#: front-matter is only needed to override the location.
KIND_BY_DIRECTORY: dict[str, Kind] = {
    "guides": Kind.GUIDE,
    "recipes": Kind.RECIPE,
    "reference": Kind.REFERENCE,
    "tests": Kind.TEST,
}


class HarvestOptions:
    """Inputs to a harvest.

    Attributes:
        root: Repo root that all recorded paths are relative to.
        src: Authored content tree, or None to harvest only the conformance suite.
        tests: Conformance suite root, or None to harvest only authored content.
        jar: Wrapper around the Josh CLI, or None to skip everything that needs the engine.
        require_src: Whether a missing ``src`` is a problem. False when ``src`` is the default, so
            that a checkout without the authored tree still harvests the conformance suite; True
            when a caller named a tree that is not there, since naming it says they expect it.
        validate_tests: Also run ``validate`` over conformance tests, which the conformance runner
            already covers more strictly.
        runnable_dir: Directory to write authored-plus-overlay models into.
        export_dir: Directory the emitted models write their exports into.
        jobs: How many jar invocations to run at once.
    """

    def __init__(
        self,
        root: Path,
        src: Path | None = None,
        tests: Path | None = None,
        jar: JoshJar | None = None,
        require_src: bool = True,
        validate_tests: bool = False,
        runnable_dir: Path | None = None,
        export_dir: Path | None = None,
        jobs: int = 4,
    ) -> None:
        """Initialize the options.

        Args:
            root: Repo root that all recorded paths are relative to.
            src: Authored content tree.
            tests: Conformance suite root.
            jar: Wrapper around the Josh CLI.
            require_src: Whether a missing ``src`` is a problem.
            validate_tests: Also validate conformance tests.
            runnable_dir: Directory for emitted runnable models.
            export_dir: Directory the emitted models export into.
            jobs: How many jar invocations to run at once.
        """
        self.root = root
        self.src = src
        self.tests = tests
        self.jar = jar
        self.require_src = require_src
        self.validate_tests = validate_tests
        self.runnable_dir = runnable_dir
        self.export_dir = export_dir
        self.jobs = max(1, jobs)


class HarvestResult:
    """What a harvest produced.

    Attributes:
        manifest: The harvested library.
        log: Problems found; a harvest with any problem is a failed harvest.
        notes: Remarks that are worth printing but are not failures.
    """

    __slots__ = ("log", "manifest", "notes")

    def __init__(self, manifest: Manifest, log: ProblemLog, notes: list[str] | None = None) -> None:
        """Initialize the result.

        Args:
            manifest: The harvested library.
            log: Problems found.
            notes: Remarks that are worth printing but are not failures.
        """
        self.manifest = manifest
        self.log = log
        self.notes = notes or []


def _relative(path: Path, root: Path) -> str:
    """Return a path relative to the root, with forward slashes."""
    try:
        return path.resolve().relative_to(root.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def _infer_kind(source: Path, src: Path) -> Kind | None:
    """Infer a unit's kind from the directory it lives in."""
    try:
        parts = source.resolve().relative_to(src.resolve()).parts
    except ValueError:
        return None
    return KIND_BY_DIRECTORY.get(parts[0]) if parts else None


def _default_destination(source: Path, src: Path) -> str:
    """Return the output directory for a unit that does not declare one."""
    try:
        relative = source.resolve().relative_to(src.resolve())
    except ValueError:
        return "."
    parent = relative.parent.as_posix()
    return "." if parent in {"", "."} else parent


def _collect_authored(src: Path, root: Path, log: ProblemLog) -> list[Unit]:
    """Pair and validate every authored unit under the source tree.

    Args:
        src: Authored content tree.
        root: Repo root that recorded paths are relative to.
        log: Problems are appended here.

    Returns:
        The units that were readable, whether or not the jar later rejects them.
    """
    units: list[Unit] = []
    models = sorted(src.rglob("*.josh"))
    prose_files = sorted(src.rglob("*.md"))

    stems = {model.with_suffix(""): model for model in models}
    for prose in prose_files:
        if prose.name in SIDECAR_EXEMPT or prose.name.startswith("_"):
            continue
        if prose.with_suffix("") not in stems:
            log.add(
                prose,
                f"has no model beside it: add {prose.with_suffix('.josh').name}, or rename this "
                "file to index.md if it documents the directory rather than a unit",
            )

    unpaired: list[Path] = []
    for model in models:
        prose = model.with_suffix(".md")
        if not prose.is_file():
            # It may be an overlay fragment rather than a unit. Only the sidecars know which, so
            # the decision waits until they have all been read.
            unpaired.append(model)
            continue
        unit = _authored_unit(model, prose, src, root, log)
        if unit is not None:
            units.append(unit)

    _check_overlays(units, unpaired, root, log)
    return units


def _check_overlays(
    units: list[Unit], unpaired: list[Path], root: Path, log: ProblemLog
) -> None:
    """Report fragments nothing claims, and overlays that are units in their own right.

    Args:
        units: The units built so far, each carrying the overlay it named.
        unpaired: Models with no sidecar beside them.
        root: Repo root that recorded paths are relative to.
        log: Problems are appended here.
    """
    sources = {unit.source: unit for unit in units}
    claimed = set()
    for unit in units:
        if unit.overlay is None:
            continue
        claimed.add(unit.overlay)
        if unit.overlay in sources:
            log.add(
                root / unit.prose if unit.prose else root / unit.source,
                f"names {Path(unit.overlay).name!r} as an overlay, but it is a unit of its own: an "
                "overlay holds `update` stanzas to append, not a model",
            )

    for model in unpaired:
        if _relative(model, root) in claimed:
            continue
        log.add(
            model,
            f"has no prose beside it: add {model.with_suffix('.md').name}, or name it as the "
            "'overlay:' of the model it updates",
        )


def _authored_unit(
    model: Path, prose: Path, src: Path, root: Path, log: ProblemLog
) -> Unit | None:
    """Build one authored unit, recording any problem that prevents it.

    Args:
        model: The ``.josh`` file.
        prose: The ``.md`` sidecar.
        src: Authored content tree.
        root: Repo root that recorded paths are relative to.
        log: Problems are appended here.

    Returns:
        The unit, or None when the sidecar could not be used.
    """
    try:
        sidecar = load_sidecar(prose)
    except SidecarError as exc:
        log.extend([exc.problem])
        return None

    metadata = dict(sidecar.metadata)
    inferred = _infer_kind(model, src)
    if "kind" not in metadata and inferred is not None:
        metadata["kind"] = inferred.value

    try:
        front = FrontMatter.model_validate(metadata)
    except ValidationError as exc:
        log.extend(problems_from_validation_error(exc, prose, sidecar.raw_frontmatter, 2))
        return None

    unit_id = front.id or model.stem
    if not ID_PATTERN.match(unit_id):
        log.add(
            prose,
            f"filename stem {model.stem!r} is not a usable id: rename the pair, or set an 'id:' "
            "field",
            locate_key(sidecar.raw_frontmatter, "id"),
        )
        return None

    overlay = None
    if front.overlay is not None:
        fragment = model.parent / front.overlay
        if fragment.is_file():
            overlay = _relative(fragment, root)
        else:
            log.add(
                prose,
                f"overlay: {front.overlay!r} is not beside {model.name}",
                locate_key(sidecar.raw_frontmatter, "overlay"),
            )

    return Unit(
        id=unit_id,
        kind=front.kind,
        title=front.title,
        description=None,
        source=_relative(model, root),
        prose=_relative(prose, root),
        overlay=overlay,
        destination=front.destination or _default_destination(model, src),
        order=front.order,
        runnable=bool(front.runnable),
        assertions=front.asserts,
        simulation=front.simulation,
        exports=front.exports,
        data=front.data,
        seed=front.seed,
        expect=front.expect,
        status=front.status,
        reason=front.reason,
        tags=front.tags,
    )


def _collect_conformance(tests: Path, root: Path, log: ProblemLog) -> list[Unit]:
    """Synthesize a unit from every conformance test's comment header.

    Args:
        tests: Conformance suite root.
        root: Repo root that recorded paths are relative to.
        log: Problems are appended here.

    Returns:
        One unit per discovered test.
    """
    units: list[Unit] = []
    for model in sorted(tests.rglob("test_*.josh")):
        try:
            header = read_header(model)
        except OSError as exc:
            log.add(model, f"could not be read: {exc.strerror or exc}")
            continue

        problems = header_problems(model, header)
        if problems:
            log.extend(problems)
            continue

        category = header.category or ""
        subcategory = header.subcategory or ""
        units.append(
            Unit(
                id=model.stem,
                kind=Kind.TEST,
                title=title_from_stem(model.stem),
                description=header.description,
                source=_relative(model, root),
                prose=None,
                destination=f"tests/{category}/{subcategory}",
                order=100,
                runnable=True,
                assertions=True,
                simulation=None,
                seed=42,
                expect=Expect.VALID,
                status=Status.ACTIVE,
                tags=[category, subcategory],
                priority=header.priority,
                issue=header.issue,
            )
        )
    return units


def _check_ids(units: list[Unit], log: ProblemLog, root: Path) -> list[Unit]:
    """Reject duplicate ids, naming both sources.

    Args:
        units: The harvested units.
        log: Problems are appended here.
        root: Repo root, used to resolve a unit's source for reporting.

    Returns:
        The units with duplicates removed, so the manifest stays well formed.
    """
    seen: dict[str, Unit] = {}
    kept: list[Unit] = []
    for unit in units:
        first = seen.get(unit.id)
        if first is not None:
            log.add(
                root / unit.source,
                f"id {unit.id!r} is already used by {first.source}: set a different 'id:' on one "
                "of them",
            )
            continue
        seen[unit.id] = unit
        kept.append(unit)
    return kept


def _needs_validation(unit: Unit, validate_tests: bool) -> bool:
    """Return True when the jar should be asked to validate this unit."""
    if unit.status is Status.RESERVED:
        return False
    # Conformance tests are executed by the conformance runner, which subsumes validation.
    return not (unit.kind is Kind.TEST and not validate_tests)


def _needs_externals(unit: Unit) -> bool:
    """Return True when the jar should be asked which externals this unit reads."""
    return (
        unit.status is not Status.RESERVED
        and unit.expect is Expect.VALID
        and unit.runnable
    )


def _undeclared_data(unit: Unit, externals: list[str]) -> list[str]:
    """Return the externals a unit reads that no declared data file provides.

    A `.jshd` external resolves by filename stem -- an external named `precipitation` is read from
    `precipitation.jshd` -- so that is what the match is on. A model naming its path inside a
    `start external` block would report a name no filename matches; none is runnable today, and
    declaring the file is the right answer there too, since the point is telling a reader what to
    fetch rather than proving the jar will find it.

    Conformance tests are exempt. Their fixtures are staged by the test harness, sometimes generated
    rather than shipped, so the author of a test does not own the file.

    Args:
        unit: The unit that was inspected.
        externals: External names the jar reported.

    Returns:
        The undeclared names, empty when every external has a declared file.
    """
    if unit.kind is Kind.TEST:
        return []
    declared = {Path(name).stem for name in unit.data}
    return [name for name in externals if name not in declared]


def _inspect(
    unit: Unit, jar: JoshJar, root: Path, validate_tests: bool
) -> tuple[Unit, list[Problem], list[str]]:
    """Ask the jar about one unit.

    Args:
        unit: The unit to inspect.
        jar: Wrapper around the Josh CLI.
        root: Repo root that the unit's recorded source is relative to.
        validate_tests: Whether conformance tests are validated too.

    Returns:
        The unit, the problems found, and the externals it reads.
    """
    path = root / unit.source
    problems: list[Problem] = []

    if _needs_validation(unit, validate_tests):
        result = jar.validate(path)
        if unit.expect is Expect.VALID and not result.success:
            problems.append(Problem(path, f"is not valid Josh: {first_error_line(result)}"))
        elif unit.expect is Expect.PARSE_ERROR and result.success:
            problems.append(
                Problem(
                    path,
                    "is declared 'expect: parse-error' but validates cleanly: drop the field, or "
                    "restore the error the docs describe",
                )
            )

        # The authored file validating does not mean the model that actually runs does: an overlay
        # is Josh an author wrote, and a bare `update` stanza cannot be validated on its own -- the
        # engine rejects it with "no prior definition exists" -- so the composed copy is the only
        # place a mistake in it can surface.
        if not problems and unit.runnable_file is not None:
            composed = root / unit.runnable_file
            composed_result = jar.validate(composed)
            if not composed_result.success:
                problems.append(
                    Problem(
                        root / unit.overlay if unit.overlay else path,
                        "produces a model that is not valid Josh once its overlay is applied: "
                        f"{first_error_line(composed_result)}",
                    )
                )

    externals: list[str] = []
    if not problems and _needs_externals(unit):
        externals = jar.inspect_externals(path)
        # `.jshd` files are gitignored and preprocessed by CI, so this list is the only record that
        # a run needs them. Without it a reader downloads the model and gets "External resource not
        # found" with nothing on the page saying which file was missing.
        missing = _undeclared_data(unit, externals)
        if missing:
            problems.append(
                Problem(
                    root / unit.prose if unit.prose else path,
                    f"reads the external {', '.join(missing)} but declares no data file for it: "
                    "add `data: [<name>.jshd]` naming what a run needs, since the preprocessed "
                    "files are not in the repository",
                )
            )

    return unit, problems, externals


def _inspect_all(
    scheduled: list[Unit], jar: JoshJar, root: Path, validate_tests: bool, jobs: int
) -> list[tuple[Unit, list[Problem], list[str]]]:
    """Inspect several units at once.

    Jar invocations are subprocesses, so threads are the right tool: they wait on a JVM rather than
    contend for the interpreter.

    Args:
        scheduled: Units to inspect.
        jar: Wrapper around the Josh CLI.
        root: Repo root that recorded sources are relative to.
        validate_tests: Whether conformance tests are validated too.
        jobs: How many invocations to run at once.

    Returns:
        One result per scheduled unit.
    """
    with ThreadPoolExecutor(max_workers=jobs) as pool:
        return list(
            pool.map(lambda unit: _inspect(unit, jar, root, validate_tests), scheduled)
        )


def _jar_pass(units: list[Unit], options: HarvestOptions, log: ProblemLog) -> int:
    """Validate units and record the externals they read.

    Args:
        units: The harvested units.
        options: The harvest options.
        log: Problems are appended here.

    Returns:
        How many units were validated against the jar.
    """
    if options.jar is None:
        return 0

    validated = sum(1 for unit in units if _needs_validation(unit, options.validate_tests))
    scheduled = [
        unit
        for unit in units
        if _needs_validation(unit, options.validate_tests) or _needs_externals(unit)
    ]
    if not scheduled:
        return 0

    results = _inspect_all(
        scheduled, options.jar, options.root, options.validate_tests, options.jobs
    )
    for unit, problems, externals in results:
        log.extend(problems)
        unit.externals = externals

    return validated


def _emit_runnables(units: list[Unit], options: HarvestOptions, log: ProblemLog) -> None:
    """Write authored-plus-overlay models for units that retarget their exports.

    Args:
        units: The harvested units.
        options: The harvest options.
        log: Problems are appended here.
    """
    if options.runnable_dir is None:
        return

    export_dir = options.export_dir or DEFAULT_EXPORT_DIR
    for unit in units:
        if not (unit.exports or unit.overlay) or unit.status is Status.RESERVED:
            continue
        try:
            written = emit_runnable(
                source=options.root / unit.source,
                destination=options.runnable_dir,
                simulation=unit.simulation or "",
                slots=list(unit.exports),
                export_dir=export_dir,
                unit_id=unit.id,
                overlay_file=None if unit.overlay is None else options.root / unit.overlay,
            )
        except OSError as exc:
            log.add(options.root / unit.source, f"runnable copy could not be written: {exc}")
            continue
        unit.runnable_file = _relative(written, options.root)


def harvest(options: HarvestOptions) -> HarvestResult:
    """Harvest the documentation library.

    Args:
        options: What to harvest and how.

    Returns:
        The manifest and every problem found. The caller decides whether to write the manifest;
        a run with problems should not be published.
    """
    log = ProblemLog(options.root)
    notes: list[str] = []
    units: list[Unit] = []

    if options.src is not None:
        if options.src.is_dir():
            units.extend(_collect_authored(options.src, options.root, log))
        elif options.require_src:
            log.add(options.src, "authored content directory does not exist")
        else:
            notes.append(f"{options.src} does not exist yet; harvested the conformance suite only")

    if options.tests is not None:
        if options.tests.is_dir():
            units.extend(_collect_conformance(options.tests, options.root, log))
        else:
            log.add(options.tests, "conformance directory does not exist")

    units = _check_ids(units, log, options.root)
    units.sort(key=lambda unit: (unit.kind.value, unit.destination, unit.order, unit.id))

    # Emitting comes first so that the jar pass can validate the composed model, not just the
    # authored half. An overlay is Josh an author wrote, so a typo in it has to fail the build
    # rather than wait for whatever eventually runs the emitted copy.
    _emit_runnables(units, options, log)
    validated = _jar_pass(units, options, log)

    manifest = Manifest(
        root=".",
        export_dir=options.export_dir.as_posix() if options.export_dir else None,
        counts=summarize(units, validated),
        units=units,
    )
    return HarvestResult(manifest, log, notes)
