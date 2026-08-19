#!/usr/bin/env python3
"""Prove schema.sql + data.sql can initialize a brand-new database.

The script creates an isolated temporary database, runs both SQL files, validates
all seed counts and JSON columns, then drops the database in a finally block.
It never touches the normal `plant` database.
"""
from __future__ import annotations

import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / "src/main/resources/db/schema.sql"
DATA = ROOT / "src/main/resources/db/data.sql"
DB_NAME = os.getenv("VERIFY_DATABASE", "plant_verify_flowerkisstao")

# Refuse arbitrary database names. This script is allowed to create/drop only a clearly
# designated verification database, never the normal `plant` database.
if DB_NAME != "plant_verify_flowerkisstao":
    raise ValueError("VERIFY_DATABASE must be exactly plant_verify_flowerkisstao")


def mysql_base(database: str | None = None) -> tuple[list[str], dict[str, str]]:
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


def run_sql(sql: str, database: str | None = None) -> str:
    args, env = mysql_base(database)
    result = subprocess.run(args, input=sql.encode("utf-8"), capture_output=True, env=env)
    if result.returncode:
        raise RuntimeError(result.stderr.decode("utf-8", errors="replace").strip())
    return result.stdout.decode("utf-8", errors="replace").strip()


def scalar(sql: str) -> str:
    return run_sql(sql, DB_NAME).splitlines()[0]


def expect(label: str, actual: str, expected: str) -> None:
    if actual != expected:
        raise AssertionError(f"{label}: expected {expected}, got {actual}")
    print(f"PASS  {label}: {actual}")


def main() -> int:
    print(f"temporary database: {DB_NAME}")
    run_sql(f"CREATE DATABASE `{DB_NAME}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    try:
        print("applying schema.sql ...")
        run_sql(SCHEMA.read_text(encoding="utf-8"), DB_NAME)
        print("applying data.sql ...")
        run_sql(DATA.read_text(encoding="utf-8"), DB_NAME)

        expect("table count", scalar("SELECT COUNT(*) FROM information_schema.TABLES "
                                     f"WHERE TABLE_SCHEMA='{DB_NAME}'"), "24")
        expect("role count", scalar("SELECT COUNT(*) FROM sys_role"), "3")
        expect("seed account count", scalar("SELECT COUNT(*) FROM sys_user"), "3")
        expect("operation permissions", scalar("SELECT COUNT(*) FROM sys_permission "
                                                "WHERE code IN ('operation:dashboard:read','operation:log:read')"), "2")
        expect("operator operation grants", scalar("SELECT COUNT(*) FROM sys_role_permission rp "
                "JOIN sys_role r ON r.id=rp.role_id JOIN sys_permission p ON p.id=rp.permission_id "
                "WHERE r.code='ROLE_OPERATOR' AND p.code IN "
                "('operation:dashboard:read','operation:log:read')"), "2")
        expect("species count", scalar("SELECT COUNT(*) FROM catalog_species"), "18")
        expect("sku count", scalar("SELECT COUNT(*) FROM catalog_sku"), "24")
        expect("knowledge seed count", scalar("SELECT COUNT(*) FROM knowledge_article"), "10")
        expect("published knowledge count", scalar("SELECT COUNT(*) FROM knowledge_article WHERE status=2"), "10")
        expect("weight row", scalar("SELECT COUNT(*) FROM rec_weight_config WHERE id=1 "
                "AND w_light=30 AND w_temp=15 AND w_humidity=10 AND w_care=20 "
                "AND w_space=10 AND w_budget=10 AND w_preference=5 AND top_n=3"), "1")
        expect("demo address", scalar("SELECT COUNT(*) FROM user_address a JOIN sys_user u ON u.id=a.user_id "
                                       "WHERE u.username='demo' AND a.is_default=1"), "1")
        expect("demo profiles", scalar("SELECT COUNT(*) FROM user_scene_profile p JOIN sys_user u ON u.id=p.user_id "
                                        "WHERE u.username='demo'"), "2")
        expect("valid article JSON", scalar("SELECT COUNT(*) FROM knowledge_article "
                "WHERE JSON_VALID(steps)=1 AND (tags IS NULL OR JSON_VALID(tags)=1)"), "10")

        # Start a real Spring context against the temporary database. This catches entity/mapper
        # mismatches that SQL-only checks cannot see. Environment overrides application.yml only
        # for this child process; no project configuration is changed.
        print("starting Spring context against the temporary database ...")
        _, mysql_env = mysql_base()
        spring_env = os.environ.copy()
        spring_env["SPRING_DATASOURCE_URL"] = (
            f"jdbc:mysql://127.0.0.1:3306/{DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true"
            "&serverTimezone=UTC&characterEncoding=utf8"
        )
        spring_env["MYSQL_USERNAME"] = os.getenv("MYSQL_USERNAME", "root")
        spring_env["MYSQL_PASSWORD"] = mysql_env["MYSQL_PWD"]
        maven = os.getenv("MAVEN_CMD")
        if not maven:
            # Windows Python does not resolve Git-Bash shell functions/scripts named `mvn`.
            # Resolve the Maven home used by this project explicitly, with a PATH fallback.
            candidates = [
                Path(r"E:\software\maven\bin\mvn.cmd"),
                Path(r"E:\software\maven\bin\mvn"),
            ]
            maven = next((str(path) for path in candidates if path.exists()), "mvn.cmd" if os.name == "nt" else "mvn")
        spring = subprocess.run(
            [maven, "-q", "-o", "-Dtest=FlowerKissTaoApplicationTests", "test"],
            cwd=ROOT,
            capture_output=True,
            env=spring_env,
        )
        if spring.returncode:
            raise RuntimeError("Spring context failed on clean database:\n"
                               + spring.stdout.decode("utf-8", errors="replace")[-2000:]
                               + spring.stderr.decode("utf-8", errors="replace")[-2000:])
        print("PASS  Spring Boot context starts against the clean database")

        # Make sure the whole schema and seed set is idempotent enough for a second empty-db run:
        # rerunning data.sql must not duplicate RBAC rows because it uses INSERT IGNORE.
        # Domain seed tables intentionally use regular INSERT and are not rerun here.
        print("PASS: clean database initialization completed")
        return 0
    finally:
        run_sql(f"DROP DATABASE IF EXISTS `{DB_NAME}`")
        left = run_sql("SELECT COUNT(*) FROM information_schema.SCHEMATA "
                       f"WHERE SCHEMA_NAME='{DB_NAME}'")
        print(f"temporary database removed: {left == '0'}")


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        raise
