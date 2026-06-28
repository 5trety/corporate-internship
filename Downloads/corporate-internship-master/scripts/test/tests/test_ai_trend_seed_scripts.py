from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]
FULL_SEED_SQL = ROOT / "scripts" / "seed-ai-full-test-data.sql"
FULL_CLEAR_SQL = ROOT / "scripts" / "clear-ai-full-test-data.sql"
FULL_MARKER = "AI-FULL-TEST"
FULL_PART_CODES = ("AITEST-001", "AITEST-002", "AITEST-003", "AITEST-004", "AITEST-005")


class AiTrendSeedScriptTest(unittest.TestCase):
    def test_full_seed_and_clear_scripts_exist(self):
        self.assertTrue(FULL_SEED_SQL.exists())
        self.assertTrue(FULL_CLEAR_SQL.exists())

    def test_full_seed_script_creates_complete_random_test_data(self):
        sql = FULL_SEED_SQL.read_text(encoding="utf-8")

        self.assertIn(FULL_MARKER, sql)
        self.assertIn("RAND()", sql)
        for part_code in FULL_PART_CODES:
            self.assertIn(part_code, sql)

        for table in (
            "part",
            "inbound_order",
            "inbound_order_detail",
            "outbound_order",
            "outbound_order_detail",
            "kanban",
            "current_inventory",
            "inventory_trace",
        ):
            self.assertRegex(
                sql,
                re.compile(rf"INSERT\s+INTO\s+{table}\b", re.IGNORECASE),
                msg=f"seed script should insert {table}",
            )

    def test_full_seed_script_covers_all_ai_warning_types(self):
        sql = FULL_SEED_SQL.read_text(encoding="utf-8")

        expected_cases = {
            "AITEST-001": ("AI Full Test Shortage", "SHORTAGE"),
            "AITEST-002": ("AI Full Test Obsolete", "OBSOLETE"),
            "AITEST-003": ("AI Full Test Fluctuation", "FLUCTUATION"),
            "AITEST-004": ("AI Full Test Rising Demand", "RISING"),
            "AITEST-005": ("AI Full Test Falling Demand", "FALLING"),
        }

        for part_code, (part_name, warning_target) in expected_cases.items():
            self.assertIn(part_code, sql)
            self.assertIn(part_name, sql)
            self.assertIn(warning_target, sql)

        self.assertIn("warning_target", sql)
        self.assertIn("CASE warning_target", sql)

    def test_full_seed_fluctuation_case_uses_up_and_down_series(self):
        sql = FULL_SEED_SQL.read_text(encoding="utf-8")

        self.assertIn("WHEN 5 THEN 20 + FLOOR(RAND() * 5)", sql)
        self.assertIn("WHEN 4 THEN 4 + FLOOR(RAND() * 2)", sql)
        self.assertIn("WHEN 3 THEN 22 + FLOOR(RAND() * 5)", sql)
        self.assertIn("WHEN 2 THEN 5 + FLOOR(RAND() * 2)", sql)
        self.assertIn("WHEN 1 THEN 18 + FLOOR(RAND() * 5)", sql)
        self.assertNotIn("CASE WHEN d.day_offset = 0 THEN 25", sql)

    def test_full_seed_contains_rising_and_falling_prediction_cases(self):
        sql = FULL_SEED_SQL.read_text(encoding="utf-8")

        self.assertIn("WHEN 'RISING' THEN 4 + ((6 - d.day_offset) * 2) + FLOOR(RAND() * 2)", sql)
        self.assertIn("WHEN 'FALLING' THEN 18 - ((6 - d.day_offset) * 2) + FLOOR(RAND() * 2)", sql)

    def test_full_clear_script_only_deletes_scoped_test_data(self):
        sql = FULL_CLEAR_SQL.read_text(encoding="utf-8")

        self.assertIn(FULL_MARKER, sql)
        for part_code in FULL_PART_CODES:
            self.assertIn(part_code, sql)

        for part_name in (
            "AI Full Test Stable",
            "AI Full Test Rising",
            "AI Full Test Volatile",
            "AI Full Test Shortage",
            "AI Full Test Obsolete",
            "AI Full Test Fluctuation",
            "AI Full Test Rising Demand",
            "AI Full Test Falling Demand",
        ):
            self.assertIn(part_name, sql)

        for table in (
            "inventory_trace",
            "current_inventory",
            "kanban",
            "inbound_order_detail",
            "inbound_order",
            "outbound_order_detail",
            "outbound_order",
            "part",
        ):
            self.assertRegex(
                sql,
                re.compile(rf"DELETE\s+(?:\w+\s+)?FROM\s+{table}\b[\s\S]*?WHERE", re.IGNORECASE),
                msg=f"clear script should delete {table} with a WHERE clause",
            )
            self.assertNotRegex(
                sql,
                re.compile(rf"DELETE\s+FROM\s+{table}\s*;", re.IGNORECASE),
                msg=f"clear script must not delete all rows from {table}",
            )


if __name__ == "__main__":
    unittest.main()
