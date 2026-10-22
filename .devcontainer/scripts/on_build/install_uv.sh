#!/bin/bash
# License: BSD-3-Clause
#
# SHA256 captured 2026-10-22 from this devcontainer (linux/amd64), and matches the
# uv-x86_64-unknown-linux-gnu.tar.gz.sha256 published alongside the release.
# Source: https://github.com/astral-sh/uv/releases/download/0.12.24/uv-x86_64-unknown-linux-gnu.tar.gz
# To update: download the new version, run sha256sum, and replace both
# the version and hash below.
#
# uv runs the docs builder in docs/build. It brings its own Python on first use, so no Python is
# installed here.
set -e

UV_VERSION="0.12.24"
UV_SHA256="b4dfaef47d491a7296981f8374a4595f55dbf84e8937c8ecd2983574d8bb3da6"

curl -fsSL "https://github.com/astral-sh/uv/releases/download/${UV_VERSION}/uv-x86_64-unknown-linux-gnu.tar.gz" -o /tmp/uv.tar.gz

if ! echo "${UV_SHA256}  /tmp/uv.tar.gz" | sha256sum --check --status; then
  echo "ERROR: SHA256 verification failed for uv ${UV_VERSION}." >&2
  echo "Expected: ${UV_SHA256}" >&2
  echo "Got:      $(sha256sum /tmp/uv.tar.gz | cut -d' ' -f1)" >&2
  echo "The upstream archive may have changed. Re-verify and update the hash." >&2
  rm /tmp/uv.tar.gz
  exit 1
fi

tar -xzf /tmp/uv.tar.gz -C /tmp
install -o root -g root -m 0755 /tmp/uv-x86_64-unknown-linux-gnu/uv /usr/local/bin/uv
install -o root -g root -m 0755 /tmp/uv-x86_64-unknown-linux-gnu/uvx /usr/local/bin/uvx
rm -r /tmp/uv.tar.gz /tmp/uv-x86_64-unknown-linux-gnu
