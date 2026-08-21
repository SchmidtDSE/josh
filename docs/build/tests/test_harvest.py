"""Tests for the harvest itself: pairing, ids, defaults, and the manifest."""

import json
from pathlib import Path

from joshpy.cli import CLIResult

from joshdocs.harvest import DEFAULT_TESTS, HarvestOptions, harvest
from joshdocs.schema import Expect, Kind, Status
from support import with_description

REPO_ROOT = Path(__file__).resolve().parents[3]

MODEL = """start simulation Main

  grid.size = 10 m

end simulation
"""


class FakeJar:
    """Stands in for the Josh CLI so pairing and schema rules can be tested without a JVM."""

    def __init__(self, valid=True, externals=None, memory_only=True):
        self.valid = valid
        self.externals = externals or []
        self.memory_only = memory_only
        self.validated: list[Path] = []
        self.inspected: list[Path] = []
        self.export_checked: list[tuple[Path, str]] = []

    def validate(self, path):
        self.validated.append(path)
        if self.valid:
            return CLIResult(0, "Validated Josh code at " + str(path), "", ["validate"])
        return CLIResult(3, "", "line 1:0 no viable alternative at input 'start'", ["validate"])

    def inspect_externals(self, path):
        self.inspected.append(path)
        return list(self.externals)

    def writes_only_to_memory(self, path, simulation):
        self.export_checked.append((path, simulation))
        return self.memory_only


def write_unit(src, relative, front_matter, model=MODEL, data=()):
    """Write a `.josh` and its `.md` sidecar, returning the model's path.

    `data` names files to create beside the model. Their contents do not matter -- the harvest only
    checks that a declared data file is there, since reading one is the jar's job.
    """
    path = src / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.with_suffix(".josh").write_text(model, encoding="utf-8")
    path.with_suffix(".md").write_text(
        f"---\n{with_description(front_matter)}\n---\n\nProse.\n", encoding="utf-8"
    )
    for name in data:
        (path.parent / name).write_bytes(b"")
    return path.with_suffix(".josh")


def run(tmp_path, jar=None, **overrides):
    """Harvest a tmp tree, defaulting to authored content only and no jar."""
    fields = {
        "root": tmp_path,
        "src": tmp_path / "docs" / "src",
        "tests": None,
        "jar": jar,
        "runnable_dir": None,
    }
    fields.update(overrides)
    return harvest(HarvestOptions(**fields))


def src_of(tmp_path):
    path = tmp_path / "docs" / "src"
    path.mkdir(parents=True, exist_ok=True)
    return path


def test_harvests_a_single_unit(tmp_path):
    write_unit(src_of(tmp_path), "recipes/dispersal/wind", 'title: "Wind dispersal"')
    result = run(tmp_path)

    assert not result.log, result.log.report()
    unit = result.manifest.units[0]
    assert unit.id == "wind"
    assert unit.title == "Wind dispersal"
    assert unit.source == "docs/src/recipes/dispersal/wind.josh"
    assert unit.prose == "docs/src/recipes/dispersal/wind.md"


def test_kind_is_inferred_from_the_directory(tmp_path):
    src = src_of(tmp_path)
    write_unit(src, "guides/hello/hello", 'title: "Hello"')
    write_unit(src, "recipes/wind", 'title: "Wind"')
    write_unit(src, "reference/units/custom", 'title: "Units"')

    units = {unit.id: unit for unit in run(tmp_path).manifest.units}
    assert units["hello"].kind is Kind.GUIDE
    assert units["wind"].kind is Kind.RECIPE
    assert units["custom"].kind is Kind.REFERENCE
    assert units["custom"].runnable is False


def test_declared_kind_wins_over_the_directory(tmp_path):
    write_unit(src_of(tmp_path), "reference/wind", 'title: "Wind"\nkind: recipe')
    assert run(tmp_path).manifest.units[0].kind is Kind.RECIPE


def test_destination_defaults_to_the_directory(tmp_path):
    write_unit(src_of(tmp_path), "recipes/dispersal/wind", 'title: "Wind"')
    assert run(tmp_path).manifest.units[0].destination == "recipes/dispersal"


def test_declared_destination_wins(tmp_path):
    write_unit(src_of(tmp_path), "recipes/wind", 'title: "Wind"\ndestination: recipes/spatial')
    assert run(tmp_path).manifest.units[0].destination == "recipes/spatial"


def test_a_model_without_prose_is_a_failure(tmp_path):
    src = src_of(tmp_path)
    (src / "recipes").mkdir(parents=True)
    (src / "recipes" / "orphan.josh").write_text(MODEL, encoding="utf-8")

    result = run(tmp_path)
    assert len(result.log) == 1
    assert "has no prose beside it: add orphan.md" in result.log.problems[0].message


def test_prose_without_a_model_is_a_failure(tmp_path):
    src = src_of(tmp_path)
    (src / "recipes").mkdir(parents=True)
    (src / "recipes" / "orphan.md").write_text("---\ntitle: t\n---\n", encoding="utf-8")

    result = run(tmp_path)
    assert len(result.log) == 1
    assert "has no model beside it: add orphan.josh" in result.log.problems[0].message


def test_directory_prose_needs_no_model(tmp_path):
    src = src_of(tmp_path)
    write_unit(src, "recipes/wind", 'title: "Wind"')
    (src / "recipes" / "index.md").write_text("# Recipes\n", encoding="utf-8")
    (src / "recipes" / "README.md").write_text("Notes.\n", encoding="utf-8")
    (src / "recipes" / "_draft.md").write_text("Draft.\n", encoding="utf-8")

    result = run(tmp_path)
    assert not result.log, result.log.report()
    assert len(result.manifest.units) == 1


def test_duplicate_ids_are_rejected_naming_both(tmp_path):
    src = src_of(tmp_path)
    write_unit(src, "recipes/a/wind", 'title: "One"')
    write_unit(src, "recipes/b/other", 'title: "Two"\nid: wind')

    result = run(tmp_path)
    assert len(result.log) == 1
    message = result.log.problems[0].message
    assert "id 'wind' is already used by" in message
    assert len(result.manifest.units) == 1


def test_bad_front_matter_reports_a_line_and_keeps_going(tmp_path):
    src = src_of(tmp_path)
    write_unit(src, "recipes/bad", 'title: "Bad"\norder: thirty')
    write_unit(src, "recipes/worse", "kind: tutorial")

    result = run(tmp_path)
    messages = [problem.message for problem in result.log]
    assert "order: expected an integer, got 'thirty'" in messages
    # Every problem in one run: the second unit's two failures are reported as well.
    assert any(message.startswith("title:") for message in messages)
    assert any(message.startswith("kind:") for message in messages)


def test_missing_source_directory_is_reported(tmp_path):
    result = run(tmp_path)
    assert len(result.log) == 1
    assert "does not exist" in result.log.problems[0].message


def test_reserved_units_are_recorded_with_their_reason(tmp_path):
    write_unit(
        src_of(tmp_path),
        "reference/config",
        'title: "Config"\nstatus: reserved\nreason: "config syntax is not implemented yet"',
    )
    unit = run(tmp_path).manifest.units[0]
    assert unit.status is Status.RESERVED
    assert unit.reason == "config syntax is not implemented yet"


def test_reserved_units_are_never_sent_to_the_jar(tmp_path):
    write_unit(
        src_of(tmp_path),
        "reference/config",
        'title: "Config"\nstatus: reserved\nreason: "not implemented"',
    )
    jar = FakeJar()
    result = run(tmp_path, jar=jar)
    assert jar.validated == []
    assert jar.inspected == []
    assert result.manifest.counts.validated == 0


def test_invalid_josh_is_reported_with_the_jars_message(tmp_path):
    write_unit(src_of(tmp_path), "recipes/wind", 'title: "Wind"')
    result = run(tmp_path, jar=FakeJar(valid=False))
    assert len(result.log) == 1
    assert "is not valid Josh: line 1:0 no viable alternative" in result.log.problems[0].message


def test_expected_parse_error_is_not_a_failure(tmp_path):
    write_unit(src_of(tmp_path), "reference/error", 'title: "A mistake"\nexpect: parse-error')
    result = run(tmp_path, jar=FakeJar(valid=False))
    assert not result.log, result.log.report()
    assert result.manifest.units[0].expect is Expect.PARSE_ERROR


def test_a_parse_error_that_stopped_erroring_is_a_failure(tmp_path):
    write_unit(src_of(tmp_path), "reference/error", 'title: "A mistake"\nexpect: parse-error')
    result = run(tmp_path, jar=FakeJar(valid=True))
    assert len(result.log) == 1
    assert "validates cleanly" in result.log.problems[0].message


def test_externals_come_from_the_jar(tmp_path):
    write_unit(
        src_of(tmp_path),
        "guides/two_trees",
        'title: "Two trees"\ndata: [precipitationTulare.jshdz, temperatureTulare.jshdz]',
        data=("precipitationTulare.jshdz", "temperatureTulare.jshdz"),
    )
    jar = FakeJar(externals=["precipitationTulare", "temperatureTulare"])
    result = run(tmp_path, jar=jar)
    assert not result.log, result.log.report()
    assert result.manifest.units[0].externals == ["precipitationTulare", "temperatureTulare"]


def test_an_external_with_no_declared_data_is_a_failure(tmp_path):
    """`.jshd` files are gitignored, so the declaration is the only record a run needs one."""
    write_unit(src_of(tmp_path), "guides/two_trees", 'title: "Two trees"')
    result = run(tmp_path, jar=FakeJar(externals=["precipitationTulare"]))

    assert len(result.log) == 1
    problem = result.log.problems[0]
    assert "precipitationTulare" in problem.message
    # The fix goes in the sidecar, so that is where the author is sent.
    assert problem.path.name == "two_trees.md"


def test_only_the_externals_that_are_missing_are_reported(tmp_path):
    write_unit(
        src_of(tmp_path),
        "guides/two_trees",
        'title: "Two trees"\ndata: [rain.jshdz]',
        data=("rain.jshdz",),
    )
    result = run(tmp_path, jar=FakeJar(externals=["rain", "heat"]))

    assert len(result.log) == 1
    assert "heat" in result.log.problems[0].message
    assert "rain" not in result.log.problems[0].message


def test_a_declared_data_file_that_is_not_there_is_a_failure(tmp_path):
    """The data is committed beside the model, so a name matching no file is a typo."""
    write_unit(src_of(tmp_path), "guides/two_trees", 'title: "Two trees"\ndata: [rain.jshdz]')
    result = run(tmp_path)

    assert len(result.log) == 1
    assert "rain.jshdz" in result.log.problems[0].message
    assert "not beside" in result.log.problems[0].message


def test_a_compressed_external_satisfies_the_declaration(tmp_path):
    """`.jshdz` is XZ-compressed `.jshd`, and the engine resolves a bare name to either."""
    write_unit(
        src_of(tmp_path),
        "guides/grass_shrub_fire",
        'title: "Fire"\ndata: [precipitation.jshdz]',
        data=("precipitation.jshdz",),
    )
    result = run(tmp_path, jar=FakeJar(externals=["precipitation"]))
    assert not result.log, result.log.report()


def test_a_conformance_test_may_read_an_undeclared_external(tmp_path):
    """A test's fixtures are staged by the harness, sometimes generated, so its author owns none."""
    tests = tmp_path / "josh-tests" / "conformance" / "io"
    tests.mkdir(parents=True)
    (tests / "test_external.josh").write_text(
        "# @category: io\n# @subcategory: s\n# @priority: high\n# @description: d\n\n" + MODEL,
        encoding="utf-8",
    )
    result = harvest(
        HarvestOptions(
            root=tmp_path,
            src=None,
            tests=tmp_path / "josh-tests" / "conformance",
            jar=FakeJar(externals=["CheckerboardData"]),
            runnable_dir=None,
        )
    )
    assert not result.log, result.log.report()


def test_unrunnable_units_are_not_inspected_for_externals(tmp_path):
    write_unit(src_of(tmp_path), "reference/snippet", 'title: "Snippet"')
    jar = FakeJar()
    run(tmp_path, jar=jar)
    assert jar.validated  # a reference snippet is still checked...
    assert jar.inspected == []  # ...but it is not a whole model, so it reads no externals


def test_exports_emit_a_runnable_copy(tmp_path):
    write_unit(
        src_of(tmp_path),
        "guides/hello",
        'title: "Hello"\nsimulation: Main\nexports: [patch]',
    )
    options = HarvestOptions(
        root=tmp_path,
        src=tmp_path / "docs" / "src",
        tests=None,
        runnable_dir=tmp_path / "build" / "docs" / "runnable",
        export_dir=tmp_path / "build" / "docs" / "exports",
    )
    result = harvest(options)

    assert not result.log, result.log.report()
    unit = result.manifest.units[0]
    assert unit.runnable_file == "build/docs/runnable/hello.josh"
    emitted = (tmp_path / unit.runnable_file).read_text(encoding="utf-8")
    assert emitted.startswith(MODEL)
    assert "update simulation Main" in emitted


def test_units_without_exports_emit_nothing(tmp_path):
    write_unit(src_of(tmp_path), "guides/hello", 'title: "Hello"')
    options = HarvestOptions(
        root=tmp_path,
        src=tmp_path / "docs" / "src",
        tests=None,
        runnable_dir=tmp_path / "build" / "docs" / "runnable",
    )
    result = harvest(options)
    assert result.manifest.units[0].runnable_file is None
    assert not (tmp_path / "build" / "docs" / "runnable").exists()


def test_manifest_is_deterministic_and_uses_the_documented_keys(tmp_path):
    src = src_of(tmp_path)
    write_unit(src, "recipes/b", 'title: "B"\norder: 20')
    write_unit(src, "recipes/a", 'title: "A"\norder: 10\nassert: true')

    first = run(tmp_path).manifest.to_json()
    second = run(tmp_path).manifest.to_json()
    assert first == second

    payload = json.loads(first)
    assert payload["schemaVersion"] == 1
    assert payload["counts"]["total"] == 2
    assert payload["counts"]["byKind"] == {"recipe": 2}
    assert payload["counts"]["assertions"] == 1
    # `assert` is a keyword in both Python and Java, so the manifest says `assertions`.
    assert payload["units"][0]["assertions"] is True
    assert "assert" not in payload["units"][0]
    # Ordered by kind, destination, order, id -- not by filesystem order.
    assert [unit["id"] for unit in payload["units"]] == ["a", "b"]


def test_manifest_writes_to_disk(tmp_path):
    write_unit(src_of(tmp_path), "recipes/wind", 'title: "Wind"')
    destination = tmp_path / "build" / "docs" / "docs-manifest.json"
    run(tmp_path).manifest.write(destination)
    assert json.loads(destination.read_text(encoding="utf-8"))["units"][0]["id"] == "wind"


def test_conformance_suite_is_harvested_from_its_headers(tmp_path):
    tests = tmp_path / "josh-tests" / "conformance" / "core" / "phases"
    tests.mkdir(parents=True)
    (tests / "test_phases_basic.josh").write_text(
        "# @category: core\n"
        "# @subcategory: phases\n"
        "# @priority: critical\n"
        "# @description: phases advance\n\n" + MODEL,
        encoding="utf-8",
    )
    options = HarvestOptions(
        root=tmp_path, src=None, tests=tmp_path / "josh-tests" / "conformance", runnable_dir=None
    )
    result = harvest(options)

    assert not result.log, result.log.report()
    unit = result.manifest.units[0]
    # The id is the filename stem, which is what JoshConformanceTest already emits into JUnit XML.
    assert unit.id == "test_phases_basic"
    assert unit.title == "Phases basic"
    assert unit.description == "phases advance"
    assert unit.kind is Kind.TEST
    assert unit.assertions is True
    assert unit.priority == "critical"
    assert unit.destination == "tests/core/phases"
    assert unit.tags == ["core", "phases"]
    assert unit.prose is None


def test_conformance_tests_are_not_validated_by_default(tmp_path):
    tests = tmp_path / "josh-tests" / "conformance" / "core"
    tests.mkdir(parents=True)
    (tests / "test_a.josh").write_text(
        "# @category: core\n# @subcategory: s\n# @priority: high\n# @description: d\n\n" + MODEL,
        encoding="utf-8",
    )
    jar = FakeJar()
    options = HarvestOptions(
        root=tmp_path,
        src=None,
        tests=tmp_path / "josh-tests" / "conformance",
        jar=jar,
        runnable_dir=None,
    )
    result = harvest(options)
    # The conformance runner executes these, which subsumes validating them.
    assert jar.validated == []
    assert result.manifest.counts.validated == 0
    # Externals are still resolved, because nothing else records what data a test needs.
    assert len(jar.inspected) == 1

    jar = FakeJar()
    options.jar = jar
    options.validate_tests = True
    harvest(options)
    assert len(jar.validated) == 1


def test_the_real_conformance_suite_harvests_cleanly():
    """The suite in this repo must satisfy the contract, not just a fixture."""
    options = HarvestOptions(
        root=REPO_ROOT, src=None, tests=REPO_ROOT / DEFAULT_TESTS, runnable_dir=None
    )
    result = harvest(options)

    assert not result.log, result.log.report()
    units = result.manifest.units
    assert len(units) > 100
    assert len({unit.id for unit in units}) == len(units)
    assert all(unit.kind is Kind.TEST for unit in units)
    assert all(unit.description for unit in units)
    assert any(unit.priority == "critical" for unit in units)


FRAGMENT = """update simulation Main

  steps.high = 5 count

end simulation
"""


def write_fragment(src, relative, text=FRAGMENT):
    """Write an overlay fragment, which has no sidecar of its own."""
    path = src / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    return path


def test_a_named_overlay_is_not_a_unit(tmp_path):
    # A bare `update` stanza cannot be a unit: it does not validate on its own, and it has no prose
    # of its own to carry. Naming it as an overlay is what says so.
    src = src_of(tmp_path)
    write_unit(src, "guides/two_trees", "kind: guide\ntitle: Two trees\noverlay: two_trees_ci.josh")
    write_fragment(src, "guides/two_trees_ci.josh")

    result = run(tmp_path)

    assert not result.log
    assert len(result.manifest.units) == 1
    assert result.manifest.units[0].id == "two_trees"


def test_an_unclaimed_fragment_is_a_failure(tmp_path):
    src = src_of(tmp_path)
    write_unit(src, "guides/two_trees", "kind: guide\ntitle: Two trees")
    write_fragment(src, "guides/two_trees_ci.josh")

    result = run(tmp_path)

    assert len(result.log) == 1
    message = result.log.problems[0].message
    assert "has no prose beside it: add two_trees_ci.md" in message
    assert "'overlay:'" in message


def test_an_overlay_that_does_not_exist_is_a_failure(tmp_path):
    src = src_of(tmp_path)
    write_unit(src, "guides/two_trees", "kind: guide\ntitle: Two trees\noverlay: absent.josh")

    result = run(tmp_path)

    assert len(result.log) == 1
    assert "overlay: 'absent.josh' is not beside two_trees.josh" in result.log.problems[0].message


def test_an_overlay_that_is_a_unit_is_a_failure(tmp_path):
    src = src_of(tmp_path)
    write_unit(src, "guides/two_trees", "kind: guide\ntitle: Two trees\noverlay: hello.josh")
    write_unit(src, "guides/hello", "kind: guide\ntitle: Hello")

    result = run(tmp_path)

    assert len(result.log) == 1
    assert "is a unit of its own" in result.log.problems[0].message


def test_the_manifest_records_the_overlay(tmp_path):
    src = src_of(tmp_path)
    write_unit(src, "guides/two_trees", "kind: guide\ntitle: Two trees\noverlay: two_trees_ci.josh")
    write_fragment(src, "guides/two_trees_ci.josh")

    unit = run(tmp_path).manifest.units[0]

    assert unit.overlay == "docs/src/guides/two_trees_ci.josh"


def test_the_composed_model_is_validated(tmp_path):
    # The authored half validating says nothing about the emitted copy, which is what runs.
    class FailsOnlyTheComposedCopy(FakeJar):
        def validate(self, path):
            self.validated.append(path)
            if "runnable" in str(path):
                return CLIResult(3, "", "line 9:0 mismatched input", ["validate"])
            return CLIResult(0, "Validated Josh code at " + str(path), "", ["validate"])

    src = src_of(tmp_path)
    write_unit(src, "guides/two_trees", "kind: guide\ntitle: Two trees\noverlay: two_trees_ci.josh")
    write_fragment(src, "guides/two_trees_ci.josh")

    result = run(tmp_path, jar=FailsOnlyTheComposedCopy(), runnable_dir=tmp_path / "runnable")

    assert len(result.log) == 1
    problem = result.log.problems[0]
    assert "once its overlay is applied" in problem.message
    # The author has to be sent to the file they can edit, not to the generated copy.
    assert problem.path.name == "two_trees_ci.josh"


def test_a_memory_only_unit_is_marked_browser_runnable(tmp_path):
    src = src_of(tmp_path)
    write_unit(src, "guides/hello", "kind: guide\ntitle: Hello\nsimulation: Main")

    unit = run(tmp_path, jar=FakeJar()).manifest.units[0]

    assert unit.browser_runnable


def test_a_unit_writing_to_a_file_is_not_browser_runnable(tmp_path):
    # WebAssembly has no filesystem, so a `file://` target aborts the run rather than writing.
    src = src_of(tmp_path)
    write_unit(src, "guides/hello", "kind: guide\ntitle: Hello\nsimulation: Main")

    unit = run(tmp_path, jar=FakeJar(memory_only=False)).manifest.units[0]

    assert not unit.browser_runnable


def test_a_unit_that_names_no_simulation_is_not_browser_runnable(tmp_path):
    # The browser has to name a simulation to run one, and the manifest leaves that lookup to its
    # consumers rather than guessing here.
    src = src_of(tmp_path)
    write_unit(src, "guides/hello", "kind: guide\ntitle: Hello")

    jar = FakeJar()
    unit = run(tmp_path, jar=jar).manifest.units[0]

    assert not unit.browser_runnable
    assert jar.export_checked == []


def test_an_invalid_unit_is_never_offered_as_browser_runnable(tmp_path):
    # A model the engine rejected must not carry a run button, whatever its targets say.
    src = src_of(tmp_path)
    write_unit(src, "guides/hello", "kind: guide\ntitle: Hello\nsimulation: Main")

    result = run(tmp_path, jar=FakeJar(valid=False))

    assert result.log
    assert not result.manifest.units[0].browser_runnable


def test_a_conformance_test_is_not_checked_for_browser_runnability(tmp_path):
    # Tests carry no page, so nothing would use the answer and the JVM start would be wasted.
    tests = tmp_path / "josh-tests" / "conformance" / "core"
    tests.mkdir(parents=True)
    (tests / "test_thing.josh").write_text(
        "# @category: core\n"
        "# @subcategory: things\n"
        "# @priority: high\n"
        "# @description: checks a thing\n\n" + MODEL,
        encoding="utf-8",
    )

    jar = FakeJar()
    result = harvest(
        HarvestOptions(
            root=tmp_path,
            src=None,
            tests=tmp_path / "josh-tests" / "conformance",
            jar=jar,
            runnable_dir=None,
        )
    )

    assert not result.log, result.log.report()
    assert jar.export_checked == []
