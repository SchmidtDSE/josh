"""Command line entry point for the docs builder.

Run from the repo root:

    uv run --frozen --project docs/build joshdocs harvest

Exit codes: 0 when the harvest is clean, 1 when any unit has a problem, and 2 when the builder
itself could not run -- a missing jar, an unreadable tree. Rendering arrives with the page
templates; this command is the half that produces the manifest.
"""

from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

from .errors import HarvestFailed
from .harvest import (
    DEFAULT_EXPORT_DIR,
    DEFAULT_MANIFEST,
    DEFAULT_RUNNABLE_DIR,
    DEFAULT_SRC,
    DEFAULT_TESTS,
    HarvestOptions,
    harvest,
)
from .joshjar import DEFAULT_JAR, JarUnavailable, JoshJar

EXIT_OK = 0
EXIT_PROBLEMS = 1
EXIT_UNUSABLE = 2

#: Default parallelism for jar invocations, which are subprocess-bound rather than CPU-bound.
DEFAULT_JOBS = min(8, (os.cpu_count() or 2))


def _build_parser() -> argparse.ArgumentParser:
    """Construct the argument parser.

    Returns:
        The parser, with the ``harvest`` subcommand registered.
    """
    parser = argparse.ArgumentParser(
        prog="joshdocs",
        description="Build the Josh documentation library from authored models and tests.",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    harvest_parser = subparsers.add_parser(
        "harvest",
        help="pair models with prose, validate them, and write docs-manifest.json",
    )
    harvest_parser.add_argument(
        "--root",
        type=Path,
        default=Path(),
        help="repo root that manifest paths are relative to (default: the current directory)",
    )
    harvest_parser.add_argument(
        "--src",
        type=Path,
        default=None,
        help=(
            f"authored content tree (default: {DEFAULT_SRC}, which is skipped with a note when it "
            "does not exist; naming a tree that is missing is an error)"
        ),
    )
    harvest_parser.add_argument(
        "--tests",
        type=Path,
        default=DEFAULT_TESTS,
        help=f"conformance suite to harvest headers from (default: {DEFAULT_TESTS})",
    )
    harvest_parser.add_argument(
        "--no-tests",
        action="store_true",
        help="harvest only authored content, skipping the conformance suite",
    )
    harvest_parser.add_argument(
        "--jar",
        type=Path,
        default=DEFAULT_JAR,
        help=f"Josh fat jar used to validate models (default: {DEFAULT_JAR})",
    )
    harvest_parser.add_argument(
        "--skip-jar",
        action="store_true",
        help="do not validate or inspect models, for previewing prose without building the jar",
    )
    harvest_parser.add_argument(
        "--validate-tests",
        action="store_true",
        help="also validate conformance tests, which the conformance runner already executes",
    )
    harvest_parser.add_argument(
        "--manifest",
        type=Path,
        default=DEFAULT_MANIFEST,
        help=f"where to write the manifest (default: {DEFAULT_MANIFEST})",
    )
    harvest_parser.add_argument(
        "--emit-runnable",
        type=Path,
        default=DEFAULT_RUNNABLE_DIR,
        help=f"where to write authored-plus-overlay models (default: {DEFAULT_RUNNABLE_DIR})",
    )
    harvest_parser.add_argument(
        "--export-dir",
        type=Path,
        default=DEFAULT_EXPORT_DIR,
        help=f"where emitted models write their exports (default: {DEFAULT_EXPORT_DIR})",
    )
    harvest_parser.add_argument(
        "--jobs",
        type=int,
        default=DEFAULT_JOBS,
        help=f"how many jar invocations to run at once (default: {DEFAULT_JOBS})",
    )
    harvest_parser.add_argument(
        "--quiet",
        action="store_true",
        help="print nothing on success",
    )
    return parser


def _run_harvest(args: argparse.Namespace) -> int:
    """Run the harvest subcommand.

    Args:
        args: Parsed arguments.

    Returns:
        A process exit code.
    """
    jar = None
    if not args.skip_jar:
        try:
            jar = JoshJar(args.jar)
        except JarUnavailable as exc:
            print(f"joshdocs: {exc}", file=sys.stderr)
            return EXIT_UNUSABLE

    options = HarvestOptions(
        root=args.root,
        src=args.src or DEFAULT_SRC,
        tests=None if args.no_tests else args.tests,
        jar=jar,
        require_src=args.src is not None,
        validate_tests=args.validate_tests,
        runnable_dir=args.emit_runnable,
        export_dir=args.export_dir,
        jobs=args.jobs,
    )

    try:
        result = harvest(options)
    except JarUnavailable as exc:
        print(f"joshdocs: {exc}", file=sys.stderr)
        return EXIT_UNUSABLE

    for note in result.notes:
        print(f"joshdocs: note: {note}", file=sys.stderr)

    if result.log:
        print(result.log.report(), file=sys.stderr)
        return EXIT_PROBLEMS

    result.manifest.write(args.manifest)
    if not args.quiet:
        counts = result.manifest.counts
        print(
            f"joshdocs: {counts.total} units "
            f"({counts.runnable} runnable, {counts.assertions} asserting, "
            f"{counts.reserved} reserved, {counts.validated} validated) -> {args.manifest}"
        )
    return EXIT_OK


def main(argv: list[str] | None = None) -> int:
    """Run the builder.

    Args:
        argv: Arguments to parse, or None to read ``sys.argv``.

    Returns:
        A process exit code.
    """
    args = _build_parser().parse_args(argv)
    try:
        if args.command == "harvest":
            return _run_harvest(args)
    except HarvestFailed as exc:
        print(exc.log.report(), file=sys.stderr)
        return EXIT_PROBLEMS
    return EXIT_UNUSABLE


if __name__ == "__main__":
    sys.exit(main())
