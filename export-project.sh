#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
cd "$(git rev-parse --show-toplevel)"

python3 - <<'PY'
import os
import subprocess
import zipfile
from datetime import datetime
from pathlib import Path

root = Path.cwd()
output = root.parent / f"{root.name}-snapshot-{datetime.now():%Y%m%d-%H%M%S}.zip"

# Include current tracked files and new, nonignored files.
raw = subprocess.check_output([
    'git', 'ls-files', '-z', '--cached', '--others', '--exclude-standard'
])
paths = sorted(set(p for p in raw.split(b'\0') if p))

# Also exclude ignored files that were accidentally tracked in the past.
ignored = subprocess.run(
    ['git', 'check-ignore', '--no-index', '-z', '--stdin'],
    input=b'\0'.join(paths) + (b'\0' if paths else b''),
    stdout=subprocess.PIPE, check=False
)
if ignored.returncode not in (0, 1):
    raise SystemExit('Unable to check Git ignore rules; export cancelled.')
excluded = set(ignored.stdout.split(b'\0'))
count = 0
with zipfile.ZipFile(output, 'x', compression=zipfile.ZIP_DEFLATED) as archive:
    for raw_path in paths:
        if raw_path in excluded:
            continue
        relative = Path(os.fsdecode(raw_path))
        source = root / relative
        # Skip deleted files, symlinks and paths outside the repository.
        if not source.is_file() or source.is_symlink() or not source.resolve().is_relative_to(root):
            continue
        archive.write(source, arcname=relative.as_posix())
        count += 1
print(f'Exported {count} files to {output}')
PY
