import sqlite3
import datetime
import os

DB_PATH = "timelord.db"

def register_account():
    print("--- Timelord Inbox Registration ---")
    email = input("Enter Gmail Address: ").strip()
    name = input("Enter Account Nickname (e.g., Personal, Work): ").strip()
    app_password = input("Enter Google App Password (16 chars, no spaces): ").strip()

    # Lookback logic: set to 1 day ago (Space format for SQLite JDBC)
    one_day_ago = (datetime.datetime.now() - datetime.timedelta(days=1)).strftime("%Y-%m-%d %H:%M:%S.%f")

    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()

        # Insert attempt
        cursor.execute("""
            INSERT INTO sync_state (email_address, account_name, app_password, last_successful_sync_at, total_processed_count)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(email_address) DO UPDATE SET 
                app_password=excluded.app_password,
                account_name=excluded.account_name
        """, (email, name, app_password, one_day_ago, 0))

        conn.commit()
        print(f"\nSUCCESS: Account '{email}' registered with a 1-day lookback window ({one_day_ago}).")
        conn.close()

    except sqlite3.OperationalError as e:
        print(f"\nERROR: Could not open database '{DB_PATH}'. Have you run the Java application at least once to create the schema?")
        print(f"Details: {e}")
    except Exception as e:
        print(f"\nAN ERROR OCCURRED: {e}")

if __name__ == "__main__":
    register_account()
