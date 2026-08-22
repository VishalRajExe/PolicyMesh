# PostgreSQL ownership

The database is persistent local infrastructure. Its named volume is intentionally retained by normal `stop`; `reset` deletes it after explicit confirmation. Application schema remains backend-owned.
