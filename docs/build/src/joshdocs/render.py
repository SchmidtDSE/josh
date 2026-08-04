"""Rendering the harvested library into static pages.

The promise this module keeps is that **the code on a page is read from the authored ``.josh``**,
never transcribed. A unit page carries two kinds of code: the fenced excerpts the author wrote into
the prose, which teach the model a piece at a time, and one complete listing injected from disk. The
excerpts are prose and are allowed to differ; the listing is the file, byte for byte.

Rendering is a separate command from the harvest so that it reads only ``docs-manifest.json``. That
keeps the boundary the build depends on: the jar owns Josh semantics, the harvester owns the
contract, and this module owns HTML. Nothing here parses Josh -- it reads a file and escapes it.
"""

from __future__ import annotations

import posixpath
import shutil
from collections.abc import Iterable
from dataclasses import dataclass, field
from pathlib import Path
from urllib.parse import urlsplit

from jinja2 import Environment, FileSystemLoader, StrictUndefined, select_autoescape
from markdown_it import MarkdownIt
from markdown_it.token import Token
from markupsafe import Markup
from mdit_py_plugins.anchors import anchors_plugin

from .errors import ProblemLog
from .manifest import Manifest, Unit
from .schema import Kind, Status
from .sidecar import split_frontmatter

#: Where rendered pages land by default. Ignored by git: the tree is generated on every build.
DEFAULT_OUT = Path("landing/library")

#: Templates ship beside the package rather than inside it, so they can be edited without a
#: reinstall and so a designer never has to open Python to change markup.
TEMPLATE_DIR = Path(__file__).resolve().parents[2] / "templates"

#: Kinds that become pages. Conformance tests are in the manifest so the runner can find them, but
#: they carry no prose -- a page per test would be 156 stubs. Their home is the Phase 3 status page.
RENDERED_KINDS = frozenset({Kind.GUIDE, Kind.RECIPE, Kind.REFERENCE})

#: Where the downloadable copies of the authored models go, relative to the output root. One flat
#: directory keeps the download href short and stable regardless of where a unit's page sits.
MODEL_DIR = "models"

#: Data ships compressed in the repository. The site publishes it expanded, because the XZ decoder
#: behind this format is not compiled to WebAssembly and the browser editor cannot read it.
COMPRESSED_DATA_SUFFIX = ".jshdz"

#: Directory each kind's index lives in, matching the authored tree's top-level directories.
KIND_ROOT: dict[Kind, str] = {
    Kind.GUIDE: "guides",
    Kind.RECIPE: "recipes",
    Kind.REFERENCE: "reference",
}

#: Headings and blurbs for each kind's index, so the generated pages read as prose rather than as a
#: dump of the manifest.
KIND_INDEX: dict[Kind, tuple[str, str]] = {
    Kind.GUIDE: (
        "Guides",
        "Tutorials that build a working model from nothing, one idea at a time.",
    ),
    Kind.RECIPE: (
        "Recipes",
        "Self-contained models solving one problem, ready to lift into your own work.",
    ),
    Kind.REFERENCE: (
        "Reference",
        "One page per language feature, each backed by a model the build validates.",
    ),
}

#: Link targets that are already final: absolute URLs, site-absolute paths, and in-page anchors.
_UNTOUCHED_PREFIXES = ("#", "/", "//")


@dataclass
class RenderOptions:
    """Inputs to a render.

    Attributes:
        manifest: The harvested manifest.
        root: Repo root that manifest paths resolve against.
        out: Directory the pages are written into.
        clean: Whether to empty ``out`` first, so a deleted unit's page does not linger.
    """

    manifest: Manifest
    root: Path = field(default_factory=Path)
    out: Path = DEFAULT_OUT
    clean: bool = True


@dataclass
class RenderResult:
    """What a render produced.

    Attributes:
        pages: Paths written, in the order they were written.
        log: Problems found; a non-empty log means the render failed.
    """

    pages: list[Path] = field(default_factory=list)
    log: ProblemLog = field(default_factory=ProblemLog)


def page_path(unit: Unit) -> str:
    """Return a unit's page path within the output tree.

    The path is ``<destination>/<id>.html``, which is the prose file's own path with a new
    extension whenever the author let both fields default. Ids are unique across a harvest, so
    two units can never claim one page.

    Args:
        unit: The unit to place.

    Returns:
        A forward-slashed path relative to the output root.
    """
    destination = unit.destination.strip("/")
    if destination in {"", "."}:
        return f"{unit.id}.html"
    return f"{destination}/{unit.id}.html"


def kind_index_path(kind: Kind) -> str:
    """Return the path of a kind's index page.

    Args:
        kind: The kind to index.

    Returns:
        A forward-slashed path relative to the output root.
    """
    return f"{KIND_ROOT[kind]}/index.html"


def relative_href(from_page: str, to_page: str) -> str:
    """Return a link from one page to another, as a relative path.

    Relative links keep the tree movable: the same output works at ``/library/`` and at the root of
    a preview server, with no base URL to configure.

    Args:
        from_page: Path of the page carrying the link.
        to_page: Path of the page being linked to.

    Returns:
        The href to emit.
    """
    return posixpath.relpath(to_page, posixpath.dirname(from_page) or ".")


class Library:
    """The manifest indexed the ways the renderer needs to look units up.

    Attributes:
        units: The units that become pages, sorted for display.
        by_id: Every renderable unit by id.
        by_path: Every renderable unit by repo-relative prose and source path.
        assets: Repo-relative path of every publishable file that is not a page, mapped to its
            path within the output tree. Models and overlay fragments live here.
    """

    def __init__(self, manifest: Manifest) -> None:
        """Index a manifest.

        Args:
            manifest: The harvested manifest.
        """
        self.units = sorted(
            (unit for unit in manifest.units if unit.kind in RENDERED_KINDS),
            key=lambda unit: (KIND_ROOT[unit.kind], unit.destination, unit.order, unit.id),
        )
        self.by_id = {unit.id: unit for unit in self.units}
        self.by_path: dict[str, Unit] = {}
        self.assets: dict[str, str] = {}
        for unit in self.units:
            self.by_path[unit.source] = unit
            if unit.prose is not None:
                self.by_path[unit.prose] = unit
            self.assets[unit.source] = f"{MODEL_DIR}/{unit.id}.josh"
            if unit.overlay is not None:
                # A fragment is not a unit -- a bare `update` stanza does not validate on its own --
                # but the page names it, so a reader has to be able to open it.
                self.assets[unit.overlay] = f"{MODEL_DIR}/{posixpath.basename(unit.overlay)}"

    def kinds(self) -> list[Kind]:
        """Return the kinds that have at least one unit, in reading order.

        Returns:
            The kinds present, ordered guides then recipes then reference.
        """
        present = {unit.kind for unit in self.units}
        return [kind for kind in (Kind.GUIDE, Kind.RECIPE, Kind.REFERENCE) if kind in present]

    def of_kind(self, kind: Kind) -> list[Unit]:
        """Return the units of one kind, in display order.

        Args:
            kind: The kind to select.

        Returns:
            The matching units.
        """
        return [unit for unit in self.units if unit.kind is kind]

    def grouped(self, kind: Kind) -> list[tuple[str, list[Unit]]]:
        """Return one kind's units grouped by destination.

        Args:
            kind: The kind to group.

        Returns:
            Pairs of destination and the units in it, both in display order.
        """
        groups: dict[str, list[Unit]] = {}
        for unit in self.of_kind(kind):
            groups.setdefault(unit.destination, []).append(unit)
        return list(groups.items())

    def resolve(self, target: str, source_prose: str, from_page: str) -> str | None:
        """Return the href a relative link should become.

        Three spellings are honored, because all three appear in the authored tree. A path that
        resolves against the linking file wins; failing that, a bare filename is matched against
        unit ids, which is how a tutorial cites its neighbour without knowing the neighbour's
        directory; failing that, a path naming a publishable file that is not a page -- a model or
        an overlay fragment -- becomes a link to the copy served beside the pages.

        Args:
            target: The link target, without any query or fragment.
            source_prose: Repo-relative path of the file carrying the link.
            from_page: Path of the page the link is being written into.

        Returns:
            The href, or None when nothing in the library claims that target.
        """
        # Normalizing is lexical on purpose: these are repo-relative paths recorded in the
        # manifest, not paths on the machine rendering, so the filesystem must not be consulted.
        resolved = posixpath.normpath(posixpath.join(posixpath.dirname(source_prose), target))
        unit = self.by_path.get(resolved)
        if unit is None and Path(target).suffix in {".md", ".josh"}:
            unit = self.by_id.get(Path(target).stem)
        if unit is not None:
            return relative_href(from_page, page_path(unit))
        asset = self.assets.get(resolved)
        return relative_href(from_page, asset) if asset is not None else None


def _markdown() -> MarkdownIt:
    """Build the Markdown parser.

    Returns:
        A parser with typographic replacements and heading anchors, so the tables of contents the
        guides already write by hand keep working.
    """
    parser = MarkdownIt("commonmark", {"typographer": True})
    parser.enable(["table", "strikethrough", "replacements", "smartquotes"])
    parser.use(anchors_plugin, max_level=4, permalink=False)
    return parser


class _Locator:
    """Turns a link target into the line of the sidecar it was written on.

    The prose is searched rather than tracked through the parser because markdown-it reports a
    token's line only for block-level tokens, and a link is inline. Repeated targets are handed out
    in order, so four links to the same missing page name four different lines instead of pointing
    an author at the first one four times.

    Attributes:
        lines: The sidecar's lines, front-matter included, so numbers match the file.
    """

    def __init__(self, text: str, body: str) -> None:
        """Index a sidecar.

        Args:
            text: The whole sidecar, front-matter included.
            body: The prose alone, used only to measure the front-matter's height.
        """
        self.lines = text.splitlines()
        self._offset = len(self.lines) - len(body.splitlines())
        self._seen: dict[str, int] = {}

    def locate(self, needle: str) -> int | None:
        """Return the line of the next unreported occurrence of a target.

        Args:
            needle: Text to locate.

        Returns:
            A one-based line number in the sidecar, or None when no further occurrence exists.
        """
        start = self._seen.get(needle, self._offset)
        for index in range(start, len(self.lines)):
            if needle in self.lines[index]:
                self._seen[needle] = index + 1
                return index + 1
        return None


def _rewrite_links(tokens: Iterable[Token], unit: Unit, library: Library, log: ProblemLog,
                   prose_path: Path, locator: _Locator) -> None:
    """Point every relative link in a unit's prose at the page it means.

    Rewriting happens on the token stream rather than on rendered HTML: a regex over ``href="..."``
    would also match hrefs inside a fenced code block, and the guides fence real Josh that contains
    quotes and slashes.

    Args:
        tokens: The parsed token stream, edited in place.
        unit: The unit being rendered.
        library: The indexed manifest.
        log: Problems are appended here.
        prose_path: Path of the prose, for problem reporting.
        locator: Finds the line a bad link was written on.
    """
    from_page = page_path(unit)
    source_path = unit.prose or unit.source
    for token in tokens:
        if token.children:
            _rewrite_links(token.children, unit, library, log, prose_path, locator)
        if token.type != "link_open":
            continue
        href = token.attrGet("href")
        if not href or href.startswith(_UNTOUCHED_PREFIXES):
            continue
        split = urlsplit(href)
        if split.scheme or split.netloc:
            continue
        rewritten = library.resolve(split.path, source_path, from_page)
        if rewritten is None:
            log.add(
                prose_path,
                f"links to {href!r}, which is neither a unit in the library nor a file beside "
                "the model; a page that is not published cannot be linked to",
                locator.locate(href),
            )
            continue
        token.attrSet("href", f"{rewritten}#{split.fragment}" if split.fragment else rewritten)


def _render_prose(unit: Unit, options: RenderOptions, library: Library, parser: MarkdownIt,
                  log: ProblemLog) -> Markup:
    """Render a unit's prose to HTML, with its links repointed.

    The result is marked safe because this function is what produced the markup. Everything the
    templates receive as a plain string -- titles, reasons, and the model listing above all -- goes
    through Jinja's autoescaping instead.

    Args:
        unit: The unit being rendered.
        options: The render inputs.
        library: The indexed manifest.
        parser: The Markdown parser.
        log: Problems are appended here.

    Returns:
        The rendered HTML, or empty markup when the unit has no prose.
    """
    if unit.prose is None:
        return Markup()
    path = options.root / unit.prose
    try:
        text = path.read_text(encoding="utf-8")
    except OSError as exc:
        log.add(path, f"could not be read: {exc.strerror or exc}")
        return Markup()
    _, body = split_frontmatter(text)
    tokens = _without_leading_title(parser.parse(body))
    _rewrite_links(tokens, unit, library, log, path, _Locator(text, body))
    return Markup(parser.renderer.render(tokens, parser.options, {}))


def _without_leading_title(tokens: list[Token]) -> list[Token]:
    """Drop a prose file's opening ``# Title``, which the page header already shows.

    The guides open with an ``h1`` because they read as documents on their own in the repository.
    Keeping it would print the title twice, so the rendered page uses the front-matter ``title``
    and the sidecar keeps its heading for whoever reads the file on GitHub.

    Args:
        tokens: The parsed token stream.

    Returns:
        The tokens, without a leading level-one heading if there was one.
    """
    if len(tokens) >= 3 and tokens[0].type == "heading_open" and tokens[0].tag == "h1":
        return tokens[3:]
    return tokens


def _read(relative: str, options: RenderOptions, log: ProblemLog) -> str | None:
    """Read an authored file that the output tree will carry a copy of.

    Args:
        relative: Repo-relative path of the file.
        options: The render inputs.
        log: Problems are appended here.

    Returns:
        The file's text, or None when it could not be read.
    """
    path = options.root / relative
    try:
        return path.read_text(encoding="utf-8")
    except OSError as exc:
        log.add(path, f"could not be read: {exc.strerror or exc}")
        return None


def _environment() -> Environment:
    """Build the Jinja environment.

    Returns:
        An environment that autoescapes HTML and treats an undefined variable as an error, so a
        renamed field fails the build instead of rendering an empty page.
    """
    return Environment(
        loader=FileSystemLoader(TEMPLATE_DIR),
        autoescape=select_autoescape(default_for_string=True, default=True),
        undefined=StrictUndefined,
        trim_blocks=True,
        lstrip_blocks=True,
        keep_trailing_newline=True,
    )


def _write(out: Path, page: str, markup: str, result: RenderResult) -> None:
    """Write one page, creating its directories.

    Args:
        out: The output root.
        page: Path of the page within the output root.
        markup: The rendered HTML.
        result: Written paths are appended here.
    """
    destination = out / page
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(markup, encoding="utf-8")
    result.pages.append(destination)


def render(options: RenderOptions) -> RenderResult:
    """Render every page in the library.

    Args:
        options: The render inputs.

    Returns:
        The pages written and any problems found. A non-empty log means the output is incomplete
        and the caller should fail rather than publish it.
    """
    result = RenderResult()
    library = Library(options.manifest)
    parser = _markdown()
    environment = _environment()
    unit_template = environment.get_template("unit.html")
    index_template = environment.get_template("index.html")
    library_template = environment.get_template("library.html")

    if options.clean and options.out.exists():
        shutil.rmtree(options.out)

    for unit in library.units:
        page = page_path(unit)
        markup = unit_template.render(
            unit=unit,
            page=page,
            prose=_render_prose(unit, options, library, parser, result.log),
            model=_read(unit.source, options, result.log),
            model_href=relative_href(page, library.assets[unit.source]),
            overlay_href=(
                None if unit.overlay is None
                else relative_href(page, library.assets[unit.overlay])
            ),
            reserved=unit.status is Status.RESERVED,
            # The page names the file as the repository stores it, but the prose links the expanded
            # copy the site publishes, so a reader who compares the two needs the difference said
            # out loud rather than left as a puzzle about a filename they cannot find.
            data_compressed=any(name.endswith(COMPRESSED_DATA_SUFFIX) for name in unit.data),
            kind_root=KIND_ROOT[unit.kind],
            kind_title=KIND_INDEX[unit.kind][0],
            kind_index=relative_href(page, kind_index_path(unit.kind)),
            home=relative_href(page, "index.html"),
        )
        _write(options.out, page, markup, result)

    # The authored files are copied verbatim so that "Download Complete Code" hands over the same
    # bytes the page shows and the build validates -- one file, three consumers.
    for authored, asset in sorted(library.assets.items()):
        text = _read(authored, options, result.log)
        if text is not None:
            _write(options.out, asset, text, result)

    for kind in library.kinds():
        page = kind_index_path(kind)
        title, blurb = KIND_INDEX[kind]
        groups = [
            (destination, destination.split("/")[-1].replace("_", " "),
             [(unit, relative_href(page, page_path(unit))) for unit in units])
            for destination, units in library.grouped(kind)
        ]
        markup = index_template.render(
            title=title,
            blurb=blurb,
            groups=groups,
            single_group=len(groups) == 1,
            page=page,
            home=relative_href(page, "index.html"),
        )
        _write(options.out, page, markup, result)

    sections = [
        (KIND_INDEX[kind][0], KIND_INDEX[kind][1], kind_index_path(kind),
         len(library.of_kind(kind)))
        for kind in library.kinds()
    ]
    _write(options.out, "index.html",
           library_template.render(sections=sections, page="index.html",
                                   counts=options.manifest.counts),
           result)
    return result
