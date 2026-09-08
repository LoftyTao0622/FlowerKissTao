#!/usr/bin/env python3
"""Run the local demo's critical API flow.

Prerequisites: the Spring Boot backend is running, the database has been seeded,
and Redis is available (or disabled in application.yml). The script creates a
fresh user so it can be run repeatedly without depending on previous demo data.
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
import uuid


def call(base: str, method: str, path: str, body: object | None = None,
         token: str | None = None) -> object:
    data = None if body is None else json.dumps(body).encode("utf-8")
    request = urllib.request.Request(base.rstrip("/") + path, data=data, method=method)
    request.add_header("Accept", "application/json")
    if data is not None:
        request.add_header("Content-Type", "application/json")
    if token:
        request.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            status = response.status
            payload = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {path} returned HTTP {error.code}: {detail}") from error
    except urllib.error.URLError as error:
        raise RuntimeError(f"无法连接后端 {base}: {error.reason}") from error
    if status < 200 or status >= 300:
        raise RuntimeError(f"{method} {path} returned HTTP {status}: {payload}")
    if not isinstance(payload, dict) or payload.get("code") != 0:
        raise RuntimeError(f"{method} {path} returned business error: {payload}")
    return payload.get("data")


def require(value: object, label: str) -> object:
    if value is None:
        raise RuntimeError(f"{label} 为空")
    return value


def login(base: str, username: str, password: str) -> tuple[str, dict]:
    data = require(call(base, "POST", "/api/auth/login",
                        {"username": username, "password": password}), "登录结果")
    if not isinstance(data, dict) or not data.get("token"):
        raise RuntimeError(f"登录结果缺少 token: {data}")
    return str(data["token"]), data


def cleanup_user(user_id: int) -> None:
    """Remove the smoke user's records and restore stock consumed by its orders."""
    host = os.getenv("MYSQL_HOST", "127.0.0.1")
    port = os.getenv("MYSQL_PORT", "3306")
    username = os.getenv("MYSQL_USERNAME", "root")
    database = os.getenv("MYSQL_DATABASE", "plant")
    sql = f"""
SET FOREIGN_KEY_CHECKS = 0;
UPDATE catalog_sku s JOIN (
  SELECT oi.sku_id, SUM(oi.quantity) AS quantity
  FROM trade_order_item oi JOIN trade_order o ON o.id = oi.order_id
  WHERE o.user_id = {user_id} GROUP BY oi.sku_id
) x ON x.sku_id = s.id SET s.stock = s.stock + x.quantity;
DELETE n FROM care_note n JOIN care_archive a ON a.id = n.archive_id WHERE a.user_id = {user_id};
DELETE t FROM care_task t JOIN care_archive a ON a.id = t.archive_id WHERE a.user_id = {user_id};
DELETE n FROM care_notification n WHERE n.user_id = {user_id};
DELETE FROM care_archive WHERE user_id = {user_id};
DELETE i FROM rec_result_item i JOIN rec_result r ON r.id = i.result_id WHERE r.user_id = {user_id};
DELETE FROM rec_result WHERE user_id = {user_id};
DELETE FROM knowledge_feedback WHERE user_id = {user_id};
DELETE FROM trade_cart WHERE user_id = {user_id};
DELETE FROM user_address WHERE user_id = {user_id};
DELETE i FROM trade_order_item i JOIN trade_order o ON o.id = i.order_id WHERE o.user_id = {user_id};
DELETE FROM trade_order WHERE user_id = {user_id};
DELETE FROM user_scene_profile WHERE user_id = {user_id};
DELETE FROM sys_user_role WHERE user_id = {user_id};
DELETE FROM sys_user WHERE id = {user_id};
SET FOREIGN_KEY_CHECKS = 1;
"""
    env = os.environ.copy()
    env["MYSQL_PWD"] = os.getenv("MYSQL_PASSWORD", "123456")
    command = ["mysql", f"-h{host}", f"-P{port}", f"-u{username}",
               "--default-character-set=utf8mb4", database]
    result = subprocess.run(command, input=sql.encode("utf-8"), capture_output=True, env=env)
    if result.returncode:
        detail = result.stderr.decode("utf-8", errors="replace").strip()
        raise RuntimeError(f"清理冒烟用户失败: {detail}")


def main() -> int:
    parser = argparse.ArgumentParser(description="FlowerKissTao 本地演示冒烟测试")
    parser.add_argument("--base-url", default="http://127.0.0.1:8099",
                        help="Spring Boot API 地址，默认 http://127.0.0.1:8099")
    parser.add_argument("--keep-user", action="store_true",
                        help="保留冒烟用户及演示数据，便于失败后排查")
    args = parser.parse_args()
    base = args.base_url

    suffix = uuid.uuid4().hex[:10]
    username = "smoke_" + suffix
    password = "SmokePass123"
    print(f"[1/12] 注册演示用户 {username}")
    registered = require(call(base, "POST", "/api/auth/register", {
        "username": username, "password": password, "nickname": "冒烟用户"
    }), "注册结果")
    user_token = str(require(registered.get("token") if isinstance(registered, dict) else None, "注册 token"))
    user = registered.get("user") if isinstance(registered, dict) else None
    user_id = int(require(user.get("id") if isinstance(user, dict) else None, "冒烟用户 ID"))

    print("[2/12] 恢复登录态并读取公开目录")
    me = require(call(base, "GET", "/api/auth/me", token=user_token), "当前用户")
    if not isinstance(me, dict) or me.get("username") != username:
        raise RuntimeError(f"当前用户不匹配: {me}")
    plants = require(call(base, "GET", "/api/catalog/plants?current=1&size=12"), "植物列表")
    records = plants.get("records", []) if isinstance(plants, dict) else []
    first = records[0] if records else {}
    sku_value = first.get("defaultSkuId") or ((first.get("skus") or [{}])[0].get("id"))
    if not sku_value:
        raise RuntimeError("植物列表没有可购买的 SKU")
    sku_id = int(sku_value)

    print("[3/12] 创建场景画像")
    profile = require(call(base, "POST", "/api/profiles", {
        "sceneName": "冒烟演示场景", "placement": "living_room",
        "lightLevel": 3, "tempLevel": 2, "humidityLevel": 2,
        "spaceLevel": 2, "ventilation": 2, "budgetMin": 20,
        "budgetMax": 500, "preferOrnamental": "any", "hasChild": False,
        "hasCat": False, "hasDog": False, "experienceLevel": 2,
        "waterTimesWeek": 2, "travelFrequency": 1, "forgetful": False,
        "acceptRepot": True, "acceptFertilize": True, "acceptPrune": True,
    }, user_token), "画像结果")
    profile_id = int(require(profile, "画像 ID"))

    print("[4/12] 生成推荐")
    recommendation = require(call(base, "POST", "/api/recommendations",
                                  {"profileId": profile_id}, user_token), "推荐结果")
    if not isinstance(recommendation, dict) or "items" not in recommendation:
        raise RuntimeError(f"推荐结果格式不正确: {recommendation}")

    print("[5/12] 创建收货地址")
    address = require(call(base, "POST", "/api/addresses", {
        "receiver": "冒烟测试", "phone": "13800138000", "province": "广东省",
        "city": "深圳市", "district": "南山区", "detail": "演示路 1 号",
    }, user_token), "地址结果")
    address_id = int(require(address, "地址 ID"))

    print("[6/12] 加入购物车并读取购物车")
    call(base, "POST", "/api/cart", {"skuId": sku_id, "quantity": 1}, user_token)
    cart = require(call(base, "GET", "/api/cart", token=user_token), "购物车")
    if not isinstance(cart, dict) or not cart.get("items"):
        raise RuntimeError(f"购物车为空: {cart}")

    print("[7/12] 创建订单")
    order = require(call(base, "POST", "/api/orders", {
        "idemKey": "smoke-checkout-" + suffix, "addressId": address_id,
        "skuIds": [sku_id],
    }, user_token), "订单结果")
    order_id = int(require(order.get("id") if isinstance(order, dict) else None, "订单 ID"))

    print("[8/12] 模拟支付并验证幂等重试")
    pay_body = {"idemKey": "smoke-payment-" + suffix}
    call(base, "POST", f"/api/orders/{order_id}/pay", pay_body, user_token)
    call(base, "POST", f"/api/orders/{order_id}/pay", pay_body, user_token)

    print("[9/12] 管理员发货")
    admin_token, _ = login(base, "admin", "admin123")
    call(base, "PUT", f"/api/admin/orders/{order_id}/ship", token=admin_token)

    print("[10/12] 用户确认收货并读取订单")
    call(base, "PUT", f"/api/orders/{order_id}/receive", token=user_token)
    final_order = require(call(base, "GET", f"/api/orders/{order_id}", token=user_token), "订单详情")
    if not isinstance(final_order, dict) or final_order.get("status") != 3:
        raise RuntimeError(f"订单未完成: {final_order}")

    print("[11/12] 读取养护档案并手工触发维护")
    archives = require(call(base, "GET", "/api/care/archives", token=user_token), "养护档案")
    if not isinstance(archives, list) or not archives:
        raise RuntimeError(f"确认收货后没有养护档案: {archives}")
    call(base, "POST", "/api/care/maintenance/run", token=admin_token)

    print("[12/12] 验证后台可访问")
    call(base, "GET", "/api/admin/orders?current=1&size=1", token=admin_token)
    if not args.keep_user:
        print("清理冒烟用户及其演示数据")
        cleanup_user(user_id)
    print("SMOKE PASS: 注册、推荐、购物车、下单、支付、发货、收货、养护和后台流程均通过")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as error:
        print(f"SMOKE FAIL: {error}", file=sys.stderr)
        raise SystemExit(1)
