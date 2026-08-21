"""End-to-end checks against the real ``docs/src`` tree, not a fixture.

The unit tests prove the renderer behaves; these prove the repository's actual content survives it.
They run the harvest with ``--skip-jar``, so they need no 116MB build and finish in about a second
-- the jar's opinion of a model is the conformance suite's job, not this file's.

The assertion that matters most is :func:`test_every_page_shows_its_own_source_file`. It is the
whole point of the pipeline stated as a test: if the code on a page ever stops being the file the
build validates, this fails.
"""

import html
import re
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urlsplit

import pytest

from joshdocs.harvest import HarvestOptions, harvest
from joshdocs.render import RENDERED_KINDS, RenderOptions, page_path, render

REPO_ROOT = Path(__file__).resolve().parents[3]
SRC = REPO_ROOT / "docs" / "src"
TESTS = REPO_ROOT / "josh-tests" / "conformance"

# Attribute-tolerant: the complete listing also carries an id, so the run button can read the model
# off the page instead of holding a second copy of it.
LISTING = re.compile(r'<pre><code class="language-joshlang"[^>]*>(.*?)</code></pre>', re.S)
HREF = re.compile(r'href="([^"]+)"')

#: Elements that never carry a closing tag, so they must not be pushed onto the nesting stack.
VOID_ELEMENTS = frozenset({
    "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source",
    "track", "wbr",
})


class TagBalance(HTMLParser):
    """Reports elements that are closed out of order or never closed at all.

    A Jinja template can produce mismatched markup without failing to render -- an ``{% if %}``
    that opens a tag inside the block and closes it outside is valid Jinja and invalid HTML.

    Attributes:
        errors: Descriptions of the mismatches found, each naming a line.
        stack: Elements still open when parsing finished.
    """

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.errors: list[str] = []
        self.stack: list[str] = []

    def handle_starttag(self, tag, attrs):
        if tag not in VOID_ELEMENTS:
            self.stack.append(tag)

    def handle_startendtag(self, tag, attrs):
        """Ignore a self-closing tag, which opens and closes in one go."""

    def handle_endtag(self, tag):
        if tag in VOID_ELEMENTS:
            return
        if not self.stack:
            self.errors.append(f"line {self.getpos()[0]}: </{tag}> with nothing open")
        elif self.stack[-1] != tag:
            self.errors.append(f"line {self.getpos()[0]}: </{tag}> closes <{self.stack[-1]}>")
            if tag in self.stack:
                while self.stack and self.stack.pop() != tag:
                    pass
        else:
            self.stack.pop()

    def faults(self) -> list[str]:
        """Return every mismatch, including elements left open.

        Returns:
            The descriptions, empty when the document is balanced.
        """
        unclosed = [f"never closed: <{tag}>" for tag in reversed(self.stack)]
        return self.errors + unclosed


@pytest.fixture(scope="module")
def manifest():
    """Harvest the real authored tree without the jar."""
    result = harvest(
        HarvestOptions(
            root=REPO_ROOT,
            src=SRC,
            tests=TESTS if TESTS.is_dir() else None,
            jar=None,
            runnable_dir=None,
        )
    )
    assert not result.log, result.log.report()
    return result.manifest


@pytest.fixture(scope="module")
def site(manifest, tmp_path_factory):
    """Render the real tree, failing the module if anything in it does not render."""
    out = tmp_path_factory.mktemp("library")
    result = render(RenderOptions(manifest=manifest, root=REPO_ROOT, out=out))
    assert not result.log, result.log.report()
    return out


def rendered_units(manifest):
    """Return the units that should have become pages."""
    return [unit for unit in manifest.units if unit.kind in RENDERED_KINDS]


def test_the_authored_tree_is_not_empty(manifest):
    assert len(rendered_units(manifest)) >= 30


def test_every_unit_has_a_page(manifest, site):
    missing = [
        unit.id for unit in rendered_units(manifest) if not (site / page_path(unit)).is_file()
    ]
    assert not missing


def test_every_page_shows_its_own_source_file(manifest, site):
    """The single-source promise: the listing on a page is the authored file, byte for byte."""
    for unit in rendered_units(manifest):
        page = (site / page_path(unit)).read_text(encoding="utf-8")
        blocks = LISTING.findall(page)
        assert blocks, f"{unit.id} has no joshlang block"
        shown = html.unescape(blocks[-1])
        source = (REPO_ROOT / unit.source).read_text(encoding="utf-8")
        assert shown == source, f"{unit.id}: the page's listing is not {unit.source}"


def test_every_download_is_the_source_file(manifest, site):
    """The download button hands over the same bytes the page shows.

    Only relative hrefs count. A site-absolute one would be served by the static site rather than
    from this tree, which is exactly the arrangement that let the old guide pages drift.
    """
    for unit in rendered_units(manifest):
        page_file = site / page_path(unit)
        hrefs = [
            href
            for href in HREF.findall(page_file.read_text(encoding="utf-8"))
            if href.endswith(f"/{unit.id}.josh") and not href.startswith("/")
        ]
        assert hrefs, f"{unit.id} has no download link within the library"
        target = page_file.parent / hrefs[0]
        assert target.read_text(encoding="utf-8") == (
            REPO_ROOT / unit.source
        ).read_text(encoding="utf-8")


def test_no_page_offers_a_download_from_outside_the_library(site):
    """A model served from anywhere but this tree is a second copy waiting to drift."""
    strays = [
        f"{page.relative_to(site)} -> {href}"
        for page in sorted(site.rglob("*.html"))
        for href in HREF.findall(page.read_text(encoding="utf-8"))
        if href.startswith("/") and href.endswith(".josh")
    ]
    assert not strays


def test_no_page_links_to_a_file_that_was_not_written(site):
    """Every relative href lands on something in the output tree.

    Site-absolute links are skipped: they point at the hand-written landing pages, which this tree
    knows nothing about. Relative links are entirely the renderer's own work, so a dangling one is
    always a bug here rather than a missing page elsewhere.
    """
    dangling = []
    for page in sorted(site.rglob("*.html")):
        for href in HREF.findall(page.read_text(encoding="utf-8")):
            path = urlsplit(href).path
            if not path or href.startswith(("#", "/", "http://", "https://", "//")):
                continue
            if not (page.parent / path).exists():
                dangling.append(f"{page.relative_to(site)} -> {href}")
    assert not dangling


def test_the_old_simulation_name_is_gone(site):
    """`TestSimpleSimulation` was the drift that motivated this pipeline.

    The guide's prose said it while the validated model said `Main`, and no check compared them.
    It must not come back through a page.
    """
    offenders = [
        page.relative_to(site)
        for page in site.rglob("*.html")
        if "TestSimpleSimulation" in page.read_text(encoding="utf-8")
    ]
    assert not offenders


def test_the_guides_are_all_published(manifest, site):
    guides = {unit.id for unit in rendered_units(manifest) if unit.kind.value == "guide"}
    assert guides == {"hello", "two_trees", "grass_shrub_fire"}
    index = (site / "guides" / "index.html").read_text(encoding="utf-8")
    for guide in guides:
        assert f"{guide}.html" in index


def test_an_index_exists_for_every_kind_present(manifest, site):
    kinds = {unit.kind for unit in rendered_units(manifest)}
    for kind in kinds:
        root = {"guide": "guides", "recipe": "recipes", "reference": "reference"}[kind.value]
        assert (site / root / "index.html").is_file()
    assert (site / "index.html").is_file()


def test_conformance_tests_get_no_pages(manifest, site):
    tests = [unit for unit in manifest.units if unit.kind.value == "test"]
    assert tests, "the conformance suite should have been harvested"
    written = {path.name for path in site.rglob("*.html")}
    assert not [unit for unit in tests if f"{unit.id}.html" in written]


def test_every_page_is_balanced_html(site):
    broken = {}
    for page in sorted(site.rglob("*.html")):
        parser = TagBalance()
        parser.feed(page.read_text(encoding="utf-8"))
        parser.close()
        faults = parser.faults()
        if faults:
            broken[str(page.relative_to(site))] = faults[:3]
    assert not broken


#: Third-party libraries the browser JavaScript reaches for as a bare global rather than importing.
#: Each maps the global's name to the script the page must load to provide it. They are invisible to
#: an import-graph check -- which is how `math` reached a reader as "math is not defined", reported
#: only after their simulation had already finished running.
GLOBAL_SCRIPTS = {
    "d3": "/d3.min.js",
    "math": "/math.min.js",
}


def test_a_run_page_loads_every_global_its_scripts_reach_for(manifest, tmp_path):
    """A page with a run box must load a script for each bare global the runner's code uses.

    Scanning the shipped JavaScript rather than asserting a fixed list, so that vendoring another
    file from the demo cannot quietly add a dependency the page does not provide.

    The harvest here runs without the jar, so nothing is marked browser-runnable; one unit is marked
    by hand to make the run box render. What is under test is the template's script tags against the
    scripts' actual needs, not how a unit earns a run box.
    """
    js_dir = REPO_ROOT / "landing" / "js"
    needed = set()
    for path in sorted(js_dir.glob("*.js")):
        text = path.read_text(encoding="utf-8")
        for name in GLOBAL_SCRIPTS:
            # Word-boundary on the left so `Math.round` never reads as the mathjs global.
            if re.search(rf"(?<![\w.]){name}\.", text):
                needed.add(name)

    assert needed, "expected the browser scripts to use at least one third-party global"

    runnable = next(unit for unit in manifest.units if unit.kind in RENDERED_KINDS
                    and unit.simulation is not None)
    runnable.browser_runnable = True
    try:
        out = tmp_path / "library"
        result = render(RenderOptions(manifest=manifest, root=REPO_ROOT, out=out))
        assert not result.log, result.log.report()
        page = (out / page_path(runnable)).read_text(encoding="utf-8")
    finally:
        runnable.browser_runnable = False

    for name in sorted(needed):
        script = GLOBAL_SCRIPTS[name]
        assert f'src="{script}"' in page, (
            f"the browser scripts use the `{name}` global but the run page does not load {script}"
        )
