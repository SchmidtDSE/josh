"""The manifest: what the harvester produces and everything downstream consumes.

``docs-manifest.json`` is the contract between the harvester, the conformance runner, and the page
renderer. It is written with camelCase keys to match the JSON the ``inspect-*`` commands already
emit, sorted by id and free of timestamps so that two harvests of the same tree produce identical
bytes.

The authoring key ``assert`` becomes ``assertions`` here: ``assert`` is a keyword in both Python and
Java, and the manifest is read from Java.
"""

from __future__ import annotations

import json
from pathlib import Path

from pydantic import BaseModel, ConfigDict, Field

from . import SCHEMA_VERSION
from .schema import Expect, ExportSlot, Kind, Status


class Unit(BaseModel):
    """One documentation unit as recorded in the manifest.

    Paths are repo-relative and use forward slashes so the manifest is portable between the machine
    that harvests and the machine that renders.
    """

    model_config = ConfigDict(extra="forbid")

    id: str
    kind: Kind
    title: str
    description: str | None = None
    source: str
    prose: str | None = None
    destination: str
    order: int
    runnable: bool
    assertions: bool
    #: Simulation to run. Null means the consumer resolves it: only the engine knows what a file
    #: declares, and the conformance runner already looks it up. Authors set it to pin a specific
    #: simulation in a file that declares several, and must set it to use `exports`.
    simulation: str | None = None
    exports: list[ExportSlot] = Field(default_factory=list)
    externals: list[str] = Field(default_factory=list)
    data: list[str] = Field(default_factory=list)
    seed: int
    expect: Expect
    status: Status
    reason: str | None = None
    tags: list[str] = Field(default_factory=list)
    priority: str | None = None
    issue: str | None = None
    #: Path to the emitted authored-plus-overlay model, when an overlay was needed.
    runnable_file: str | None = Field(default=None, serialization_alias="runnableFile")


class Counts(BaseModel):
    """Summary counts, so a harvest's shape is visible without reading every unit."""

    model_config = ConfigDict(extra="forbid")

    total: int
    by_kind: dict[str, int] = Field(serialization_alias="byKind")
    runnable: int
    assertions: int
    reserved: int
    validated: int


class Manifest(BaseModel):
    """The harvested documentation library."""

    model_config = ConfigDict(extra="forbid")

    generator: str = "joshdocs"
    schema_version: int = Field(default=SCHEMA_VERSION, serialization_alias="schemaVersion")
    #: Directory that every path in the manifest is relative to, itself relative to the repo root.
    root: str = "."
    #: Directory the emitted runnable models write their exports into.
    export_dir: str | None = Field(default=None, serialization_alias="exportDir")
    counts: Counts
    units: list[Unit]

    def to_json(self) -> str:
        """Render the manifest as JSON.

        Returns:
            The JSON text, newline-terminated.
        """
        payload = self.model_dump(mode="json", by_alias=True)
        return json.dumps(payload, indent=2) + "\n"

    def write(self, path: Path) -> None:
        """Write the manifest, creating parent directories as needed.

        Args:
            path: Destination file.
        """
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(self.to_json(), encoding="utf-8")


def summarize(units: list[Unit], validated: int) -> Counts:
    """Compute the manifest's summary counts.

    Args:
        units: The harvested units.
        validated: How many units were checked against the jar.

    Returns:
        The counts.
    """
    by_kind: dict[str, int] = {}
    for kind in Kind:
        count = sum(1 for unit in units if unit.kind is kind)
        if count:
            by_kind[kind.value] = count
    return Counts(
        total=len(units),
        by_kind=by_kind,
        runnable=sum(1 for unit in units if unit.runnable),
        assertions=sum(1 for unit in units if unit.assertions),
        reserved=sum(1 for unit in units if unit.status is Status.RESERVED),
        validated=validated,
    )
