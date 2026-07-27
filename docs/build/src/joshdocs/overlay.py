"""Retargeting a model's exports without editing what the author wrote.

An authored model that writes to ``memory://editor/patches`` runs in the browser but throws
``Unsupported protocol: memory`` on the JVM, and one that writes to ``file:///tmp/...`` throws
``Only in-memory targets supported on WASM``. That conflict is why ``examples/guide`` carries four
``_cli`` twins of otherwise identical models.

Rather than rewrite the authored source, the harvester appends an ``update simulation <Name>``
stanza that redeclares only the ``exportFiles`` handlers. ``update`` merges onto the entity already
declared, replacing the handlers it names and leaving everything else intact, so appending is enough
and the view-source box on a rendered page still shows the authored file byte for byte. The pattern
is already in production in ``paper/management/management.josh``.

Emitted models carry no comments: Josh sources in this repo are comment-free by convention, and the
provenance of a generated model lives in the manifest instead.
"""

from __future__ import annotations

from pathlib import Path

from .schema import ExportSlot

#: Exports are written as CSV, the format the run command produces for a `.csv` target.
EXPORT_SUFFIX = "csv"


def export_target(export_dir: Path, unit_id: str, slot: ExportSlot) -> str:
    """Build the ``file://`` target for one export slot.

    Args:
        export_dir: Directory the exports are written into.
        unit_id: Id of the unit being retargeted.
        slot: Which ``exportFiles`` attribute is being retargeted.

    Returns:
        A ``file://`` URL with an absolute path.
    """
    destination = export_dir.resolve() / f"{unit_id}_{slot.value}.{EXPORT_SUFFIX}"
    return f"file://{destination}"


def render_overlay(simulation: str, targets: dict[ExportSlot, str]) -> str:
    """Render an ``update simulation`` stanza that retargets export files.

    Args:
        simulation: Name of the simulation stanza to update.
        targets: Export slot to target URL.

    Returns:
        The stanza, newline-terminated. Empty when there is nothing to retarget.
    """
    if not targets:
        return ""

    lines = [f"update simulation {simulation}", ""]
    for slot in ExportSlot:
        if slot in targets:
            lines.append(f'  exportFiles.{slot.value} = "{targets[slot]}"')
    lines.extend(["", "end simulation", ""])
    return "\n".join(lines)


def compose(source_text: str, overlay: str) -> str:
    """Append an overlay to an authored model.

    The overlay has to follow the stanza it updates, which appending guarantees.

    Args:
        source_text: The authored model, verbatim.
        overlay: The stanza to append, or an empty string.

    Returns:
        The composed model.
    """
    if not overlay:
        return source_text
    if source_text.endswith("\n\n"):
        separator = ""
    elif source_text.endswith("\n"):
        separator = "\n"
    else:
        separator = "\n\n"
    return f"{source_text}{separator}{overlay}"


def emit_runnable(
    source: Path,
    destination: Path,
    simulation: str,
    slots: list[ExportSlot],
    export_dir: Path,
    unit_id: str,
) -> Path:
    """Write a runnable copy of an authored model with its exports retargeted.

    Args:
        source: The authored ``.josh`` file.
        destination: Directory to write the runnable model into.
        simulation: Name of the simulation stanza to update.
        slots: Export slots to retarget.
        export_dir: Directory the exports should be written into.
        unit_id: Id of the unit, used for both filenames.

    Returns:
        The path of the written model.
    """
    targets = {slot: export_target(export_dir, unit_id, slot) for slot in slots}
    composed = compose(source.read_text(encoding="utf-8"), render_overlay(simulation, targets))
    destination.mkdir(parents=True, exist_ok=True)
    written = destination / f"{unit_id}.josh"
    written.write_text(composed, encoding="utf-8")
    return written
