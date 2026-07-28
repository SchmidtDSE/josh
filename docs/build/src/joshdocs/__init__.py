"""Builder for the Josh documentation library.

Authored Josh models under ``docs/src`` and the conformance suite under ``josh-tests`` are the only
sources of truth for the code that ships on joshsim.org. This package pairs each ``.josh`` with its
prose sidecar, validates the pair against the authoring contract in :mod:`joshdocs.schema`, and
emits ``docs-manifest.json`` for the conformance runner and the page renderer to consume.

The division of labour with the engine is deliberate and enforced in review: the jar owns every
question about Josh semantics (does this parse, which externals does it read), exposed as JSON
through the ``inspect-*`` commands and wrapped by :mod:`joshdocs.joshjar`. This package owns
front-matter, author-facing error messages, templating, and index generation. If anything here
starts parsing Josh, that logic belongs in the jar behind an ``inspect-*`` command instead.
"""

__all__ = ["SCHEMA_VERSION"]

#: Version of the manifest wire format. Bump when a consumer would need to change to read it.
SCHEMA_VERSION = 1
