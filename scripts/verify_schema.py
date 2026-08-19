#!/usr/bin/env python3
"""Compare schema.sql with a live MySQL database without modifying either side.

Usage:
    python scripts/verify_schema.py [database]

Connection defaults mirror application.yml and may be overridden with
MYSQL_HOST / MYSQL_PORT / MYSQL_USERNAME / MYSQL_PASSWORD.
"""
from __future__ import annotations

import os
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / "src/main/resources/db/schema.sql"
DEFAULT_DB = "plant"


def mysql_args(database: str | None = None) -> tuple[list[str], dict[str, str]]:
    host = os.getenv("MYSQL_HOST", "127.0.0.1")
    port = os.getenv("MYSQL_PORT", "3306")
    user = os.getenv("MYSQL_USERNAME", "root")
    password = os.getenv("MYSQL_PASSWORD", "123456")
    args = ["mysql", f"-h{host}", f"-P{port}", f"-u{user}", "-N", "-B", "--default-character-set=utf8mb4"]
    if database:
        args.append(database)
    env = os.environ.copy()
    env["MYSQL_PWD"] = password
    return args, env


def query(sql: str, database: str | None = None) -> str:
    args, env = mysql_args(database)
    result = subprocess.run(args + ["-e", sql], capture_output=True, env=env)
    if result.returncode:
        raise RuntimeError(result.stderr.decode("utf-8", errors="replace").strip())
    return result.stdout.decode("utf-8", errors="replace")


def expected_tables() -> dict[str, dict[str, object]]:
    text = SCHEMA.read_text(encoding="utf-8")
    result: dict[str, dict[str, object]] = {}
    pattern = re.compile(r"CREATE TABLE `([^`]+)` \((.*?)\) ENGINE=", re.S)
    for table, body in pattern.findall(text):
        columns: dict[str, tuple[str, str]] = {}
        # Only column-definition lines begin with whitespace + backtick. This avoids matching
        # constraint expressions such as `w_light` + `w_temp`, and captures DECIMAL(10,2)
        # without splitting at the comma inside parentheses.
        column_pattern = re.compile(
            r"^\s*`([^`]+)`\s+([A-Z]+(?:\([^)]*\))?)(.*)$", re.I
        )
        for line in body.splitlines():
            match = column_pattern.match(line)
            if match:
                name, type_name, rest = match.groups()
                nullable = "YES" if re.search(r"\bNULL\b", rest) and not re.search(r"\bNOT NULL\b", rest) else "NO"
                columns[name] = (type_name.lower(), nullable)

        indexes: dict[str, tuple[bool, tuple[str, ...]]] = {}
        primary = re.search(r"PRIMARY KEY \(([^)]+)\)", body)
        if primary:
            indexes["PRIMARY"] = (True, tuple(re.findall(r"`([^`]+)`", primary.group(1))))
        for unique, name, cols in re.findall(r"\s*(UNIQUE )?KEY `([^`]+)` \(([^)]+)\)", body):
            indexes[name] = (bool(unique), tuple(re.findall(r"`([^`]+)`", cols)))
        result[table] = {"columns": columns, "indexes": indexes}
    return result


def actual_tables(database: str) -> dict[str, dict[str, object]]:
    columns: dict[str, dict[str, tuple[str, str]]] = defaultdict(dict)
    sql = (
        "SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE "
        "FROM information_schema.COLUMNS "
        f"WHERE TABLE_SCHEMA='{database}' ORDER BY TABLE_NAME,ORDINAL_POSITION"
    )
    for line in query(sql).splitlines():
        if not line:
            continue
        table, column, type_name, nullable = line.split("\t")
        columns[table][column] = (type_name.lower(), nullable)

    index_rows: dict[str, dict[str, list[tuple[int, str, int]]]] = defaultdict(lambda: defaultdict(list))
    sql = (
        "SELECT TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX,COLUMN_NAME,NON_UNIQUE "
        "FROM information_schema.STATISTICS "
        f"WHERE TABLE_SCHEMA='{database}' ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX"
    )
    for line in query(sql).splitlines():
        if not line:
            continue
        table, name, seq, column, non_unique = line.split("\t")
        index_rows[table][name].append((int(seq), column, int(non_unique)))

    result: dict[str, dict[str, object]] = {}
    for table in set(columns) | set(index_rows):
        indexes: dict[str, tuple[bool, tuple[str, ...]]] = {}
        for name, rows in index_rows[table].items():
            rows.sort()
            indexes[name] = (rows[0][2] == 0, tuple(row[1] for row in rows))
        result[table] = {"columns": columns.get(table, {}), "indexes": indexes}
    return result


def compare(expected: dict[str, dict[str, object]], actual: dict[str, dict[str, object]]) -> list[str]:
    errors: list[str] = []
    expected_names, actual_names = set(expected), set(actual)
    for table in sorted(expected_names - actual_names):
        errors.append(f"missing table: {table}")
    for table in sorted(actual_names - expected_names):
        errors.append(f"extra table: {table}")

    for table in sorted(expected_names & actual_names):
        exp_cols = expected[table]["columns"]
        act_cols = actual[table]["columns"]
        assert isinstance(exp_cols, dict) and isinstance(act_cols, dict)
        for column in sorted(set(exp_cols) - set(act_cols)):
            errors.append(f"{table}: missing column {column}")
        for column in sorted(set(act_cols) - set(exp_cols)):
            errors.append(f"{table}: extra column {column}")
        for column in sorted(set(exp_cols) & set(act_cols)):
            exp_type, exp_null = exp_cols[column]
            act_type, act_null = act_cols[column]
            # Schema uses INT while information_schema reports int; exact length differences are significant.
            if str(exp_type).lower() != str(act_type).lower():
                errors.append(f"{table}.{column}: type schema={exp_type}, db={act_type}")
            if exp_null != act_null:
                errors.append(f"{table}.{column}: nullable schema={exp_null}, db={act_null}")

        exp_idx = expected[table]["indexes"]
        act_idx = actual[table]["indexes"]
        assert isinstance(exp_idx, dict) and isinstance(act_idx, dict)
        for name in sorted(set(exp_idx) - set(act_idx)):
            errors.append(f"{table}: missing index {name} {exp_idx[name]}")
        for name in sorted(set(act_idx) - set(exp_idx)):
            errors.append(f"{table}: extra index {name} {act_idx[name]}")
        for name in sorted(set(exp_idx) & set(act_idx)):
            if exp_idx[name] != act_idx[name]:
                errors.append(f"{table}.{name}: index schema={exp_idx[name]}, db={act_idx[name]}")
    return errors


def main() -> int:
    database = sys.argv[1] if len(sys.argv) > 1 else os.getenv("MYSQL_DATABASE", DEFAULT_DB)
    expected = expected_tables()
    actual = actual_tables(database)
    errors = compare(expected, actual)
    print(f"schema tables={len(expected)}, database tables={len(actual)}")
    print(f"schema columns={sum(len(v['columns']) for v in expected.values())}, "
          f"database columns={sum(len(v['columns']) for v in actual.values())}")
    if errors:
        print(f"DRIFT FOUND ({len(errors)}):")
        for error in errors:
            print("  -", error)
        return 1
    print("PASS: schema.sql matches the live database tables, columns and indexes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
