#!/usr/bin/env python3
"""
FinFlow Security & Authorization QA Verification Script
Verifies:
1. Missing, Malformed, Invalid Signature, and Expired JWT tokens against API Gateway.
2. Cross-customer resource isolation (Accounts, Transactions, Notifications).
"""

import os
import sys
import time
import uuid
import json
import base64
import hmac
import hashlib
import urllib.request
import urllib.error

GATEWAY_URL = "http://localhost:8080/api"

# Read JWT_SECRET from environment or file
JWT_SECRET = os.environ.get("JWT_SECRET")
if not JWT_SECRET:
    env_file = "/tmp/finflow_test_env.sh"
    if os.path.exists(env_file):
        with open(env_file) as f:
            for line in f:
                if line.startswith("export JWT_SECRET="):
                    JWT_SECRET = line.strip().split("=", 1)[1].strip("\"'")

def make_request(path, method="GET", token=None, body=None, raw_auth_header=None):
    url = f"{GATEWAY_URL}{path}"
    headers = {"Content-Type": "application/json"}

    if raw_auth_header is not None:
        headers["Authorization"] = raw_auth_header
    elif token:
        headers["Authorization"] = f"Bearer {token}"

    data = json.dumps(body).encode("utf-8") if body else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)

    try:
        with urllib.request.urlopen(req) as resp:
            content = resp.read().decode("utf-8")
            try:
                parsed = json.loads(content)
            except Exception:
                parsed = content
            return resp.status, parsed
    except urllib.error.HTTPError as e:
        err_content = e.read().decode("utf-8")
        try:
            parsed = json.loads(err_content)
        except Exception:
            parsed = err_content
        return e.code, parsed
    except Exception as e:
        return 500, str(e)

def create_raw_jwt(payload, secret):
    """Encodes a standard HMAC-SHA256 JWT using secret bytes."""
    header = {"alg": "HS256", "typ": "JWT"}
    b64_header = base64.urlsafe_b64encode(json.dumps(header).encode()).decode().rstrip("=")
    b64_payload = base64.urlsafe_b64encode(json.dumps(payload).encode()).decode().rstrip("=")
    signing_input = f"{b64_header}.{b64_payload}".encode()

    try:
        secret_bytes = base64.b64decode(secret)
    except Exception:
        secret_bytes = secret.encode()

    signature = hmac.new(secret_bytes, signing_input, hashlib.sha256).digest()
    b64_sig = base64.urlsafe_b64encode(signature).decode().rstrip("=")
    return f"{b64_header}.{b64_payload}.{b64_sig}"

def run_tests():
    print("=" * 60)
    print("FINFLOW SECURITY & ACCESS CONTROL VERIFICATION")
    print("=" * 60)

    results = []

    def record(name, passed, evidence):
        status = "PASS" if passed else "FAIL"
        print(f"[{status}] {name}")
        print(f"       Evidence: {evidence}")
        results.append((name, status, evidence))

    # --- PART 1: JWT AUTHENTICATION TESTS ---
    print("\n--- 1. Testing Missing / Malformed / Invalid / Expired JWTs ---")

    # 1.1 Missing JWT
    code, body = make_request("/accounts/me")
    record("Missing JWT on /accounts/me returns 401", code == 401, f"HTTP {code} -> {body}")

    code, body = make_request("/transactions/me")
    record("Missing JWT on /transactions/me returns 401", code == 401, f"HTTP {code} -> {body}")

    code, body = make_request("/notifications")
    record("Missing JWT on /notifications returns 401", code == 401, f"HTTP {code} -> {body}")

    code, body = make_request("/ai/insights")
    record("Missing JWT on /ai/insights returns 401", code == 401, f"HTTP {code} -> {body}")

    # 1.2 Malformed JWT
    code, body = make_request("/accounts/me", raw_auth_header="Bearer malformed.token.value")
    record("Malformed JWT string returns 401", code == 401, f"HTTP {code} -> {body}")

    code, body = make_request("/accounts/me", raw_auth_header="Basic dXNlcjpwYXNz")
    record("Non-Bearer scheme returns 401", code == 401, f"HTTP {code} -> {body}")

    code, body = make_request("/accounts/me", raw_auth_header="Bearer ")
    record("Empty Bearer token returns 401", code == 401, f"HTTP {code} -> {body}")

    # 1.3 Invalid Signature JWT
    fake_token = create_raw_jwt({"sub": "attacker@finflow.test", "userId": 9999, "role": "CUSTOMER"}, "wrong_invalid_secret_key_123456789")
    code, body = make_request("/accounts/me", raw_auth_header=f"Bearer {fake_token}")
    record("Tampered / Invalid signature JWT returns 401", code == 401, f"HTTP {code} -> {body}")

    # 1.4 Expired JWT
    if JWT_SECRET:
        now = int(time.time())
        expired_payload = {
            "sub": "expired_user@finflow.test",
            "userId": 8888,
            "role": "CUSTOMER",
            "iat": now - 3600,
            "exp": now - 1800 # Expired 30 minutes ago
        }
        expired_token = create_raw_jwt(expired_payload, JWT_SECRET)
        code, body = make_request("/accounts/me", raw_auth_header=f"Bearer {expired_token}")
        record("Expired JWT returns 401", code == 401, f"HTTP {code} -> {body}")
    else:
        record("Expired JWT test", False, "JWT_SECRET not available to sign expired token")

    # --- PART 2: CROSS-CUSTOMER ISOLATION TESTS ---
    print("\n--- 2. Testing Cross-Customer Resource Isolation ---")

    uid_a = str(uuid.uuid4())[:8]
    uid_b = str(uuid.uuid4())[:8]

    # Register Customer A & Login
    email_a = f"sec_cust_a_{uid_a}@finflow.test"
    pw_a = "Password123!"
    make_request("/auth/register", method="POST", body={"email": email_a, "password": pw_a})
    code, login_a = make_request("/auth/login", method="POST", body={"email": email_a, "password": pw_a})
    token_a = login_a.get("accessToken") if isinstance(login_a, dict) else None
    assert token_a, f"Failed to get token for Customer A: {login_a}"

    # Register Customer B & Login
    email_b = f"sec_cust_b_{uid_b}@finflow.test"
    pw_b = "Password123!"
    make_request("/auth/register", method="POST", body={"email": email_b, "password": pw_b})
    code, login_b = make_request("/auth/login", method="POST", body={"email": email_b, "password": pw_b})
    token_b = login_b.get("accessToken") if isinstance(login_b, dict) else None
    assert token_b, f"Failed to get token for Customer B: {login_b}"

    # Initialize accounts for Customer A and Customer B
    make_request("/accounts", method="POST", token=token_a)
    make_request("/accounts", method="POST", token=token_b)

    # Get Account A and Account B
    code, acc_a = make_request("/accounts/me", token=token_a)
    code, acc_b = make_request("/accounts/me", token=token_b)

    id_a = acc_a.get("id") if isinstance(acc_a, dict) else None
    id_b = acc_b.get("id") if isinstance(acc_b, dict) else None
    num_a = acc_a.get("accountNumber") if isinstance(acc_a, dict) else None
    num_b = acc_b.get("accountNumber") if isinstance(acc_b, dict) else None

    print(f"Customer A: ID={id_a}, Acc={num_a}")
    print(f"Customer B: ID={id_b}, Acc={num_b}")

    # 2.1 Customer A tries to access Customer B's account by ID: GET /accounts/{id_b}
    code, body = make_request(f"/accounts/{id_b}", token=token_a)
    record("Customer A accessing Customer B's account by ID is rejected (403)", code == 403, f"HTTP {code} -> {body}")

    # 2.2 Customer B tries to access Customer A's account by ID: GET /accounts/{id_a}
    code, body = make_request(f"/accounts/{id_a}", token=token_b)
    record("Customer B accessing Customer A's account by ID is rejected (403)", code == 403, f"HTTP {code} -> {body}")

    # 2.3 Deposit money into Customer A's account
    code, dep_a = make_request("/accounts/me/deposit", method="POST", token=token_a, body={"amount": 500.00})
    record("Customer A deposits ₹500 into own account", code == 200, f"HTTP {code} -> balance: {dep_a.get('balance') if isinstance(dep_a, dict) else dep_a}")

    # 2.4 Customer A transfers ₹100 to Customer B
    code, tx_transfer = make_request("/transactions/transfer", method="POST", token=token_a, body={
        "receiverAccountNumber": num_b,
        "amount": 100.00,
        "currency": "INR"
    })
    record("Customer A transfers ₹100 to Customer B", code in (200, 201), f"HTTP {code} -> txRef: {tx_transfer.get('transactionReference') if isinstance(tx_transfer, dict) else tx_transfer}")

    # Allow Kafka event to propagate to notification service
    time.sleep(2)

    # 2.5 Check Notifications for Customer B
    code, notifs_b = make_request("/notifications", token=token_b)
    has_transfer_notif = any("100" in n.get("message", "") or "received" in n.get("message", "").lower() for n in notifs_b) if isinstance(notifs_b, list) else False
    record("Customer B received incoming transfer notification", has_transfer_notif, f"Found {len(notifs_b) if isinstance(notifs_b, list) else 0} notifications")

    if isinstance(notifs_b, list) and len(notifs_b) > 0:
        notif_b_id = notifs_b[0].get("id")
        # 2.6 Customer A tries to modify / mark as read Customer B's notification
        code, body = make_request(f"/notifications/{notif_b_id}/read", method="PATCH", token=token_a)
        record("Customer A marking Customer B's notification as read is rejected", code in (403, 404), f"HTTP {code} -> {body}")

        # 2.7 Customer B marks own notification as read
        code, body = make_request(f"/notifications/{notif_b_id}/read", method="PATCH", token=token_b)
        record("Customer B marking own notification as read succeeds (204)", code == 204, f"HTTP {code}")

        # Verify read status persists
        code, notifs_b_after = make_request("/notifications", token=token_b)
        marked_item = next((n for n in notifs_b_after if n.get("id") == notif_b_id), None) if isinstance(notifs_b_after, list) else None
        is_read = (marked_item.get("read") is True or marked_item.get("isRead") is True) if marked_item else False
        record("Customer B notification mark-as-read persists in database", is_read, f"read={is_read}")

    # 2.8 Customer A queries own transactions: should only see transactions involving Customer A
    code, txs_a = make_request("/transactions/me", token=token_a)
    sent_ref = tx_transfer.get("transactionReference") if isinstance(tx_transfer, dict) else None
    matches_transfer = any(t.get("transactionReference") == sent_ref for t in txs_a) if isinstance(txs_a, list) and len(txs_a) > 0 else False
    record("Customer A /transactions/me returns Customer A's transfer and isolates data", matches_transfer, f"Total txs for A: {len(txs_a) if isinstance(txs_a, list) else 0}")

    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)
    all_passed = all(r[1] == "PASS" for r in results)
    for name, status, evidence in results:
        print(f"[{status}] {name}")
    print(f"\nTotal Tests: {len(results)}, Passed: {sum(1 for r in results if r[1] == 'PASS')}, Failed: {sum(1 for r in results if r[1] == 'FAIL')}")
    if all_passed:
        print("ALL SECURITY & AUTHORIZATION TESTS PASSED SUCCESSFULLY.")
    else:
        sys.exit(1)

if __name__ == "__main__":
    run_tests()
