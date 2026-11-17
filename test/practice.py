"""A practice database for trying SQL by hand. The tests do not use it.

From the project folder:
    python -m test.practice                    a psql prompt on the spring term data
    python -m test.practice src/waitlist.sql   run your file on that data first, then the prompt
Leave psql with \\q. The practice server and everything in it are then deleted, so every start is fresh.
"""

import shutil
import subprocess
import sys
from pathlib import Path

from test import support


def psql_command():
    """The psql client that ships with pgserver, or one on PATH."""
    try:
        from pgserver._commands import POSTGRES_BIN_PATH
    except ImportError:
        return shutil.which("psql")
    for name in ("psql", "psql.exe"):
        candidate = Path(POSTGRES_BIN_PATH) / name
        if candidate.exists():
            return str(candidate)
    return shutil.which("psql")


def prepare(db) -> None:
    """The registrar tables holding the spring term, as every test starts."""
    support.term_data(db)


def main(files) -> int:
    psql = psql_command()
    if psql is None:
        print("psql was not found. Run: python -m pip install -r requirements.txt")
        return 1
    server = support.start_server()
    try:
        with support.fresh_database(server) as db:
            prepare(db)
            uri = server.get_uri(database=db.info.dbname)
            for name in files:
                print(f"--- running {name}", flush=True)
                subprocess.run([psql, "-X", "-v", "ON_ERROR_STOP=1", "-f", name, uri], check=False)
            print("Practice database ready. Leave psql with \\q to delete it.", flush=True)
            subprocess.run([psql, "-X", uri], check=False)
    finally:
        server.cleanup()
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
