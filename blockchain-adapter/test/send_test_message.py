#!/usr/bin/env python
# -*- coding: utf-8 -*-

import requests
import json
import base64
import time
import uuid

# Конфигурация
CHAT_ID = "ВАШ_CHAT_ID_ИЗ_ШАГА_1"  # Замените на реальный ID


def send_message_via_websocket():
    """Отправка сообщения через WebSocket (STOMP)"""

    # Для упрощения используем REST API напрямую (если есть эндпоинт)
    # Создаем тестовое сообщение напрямую в БД через chat-service

    message_data = {
        "chatId": CHAT_ID,
        "encryptedContent": base64.b64encode("Hello Blockchain! This is a test message from Python!".encode()).decode()
    }

    # Пробуем отправить через REST API (если эндпоинт существует)
    try:
        response = requests.post(
            "http://localhost:8082/api/v1/messages/test",
            json=message_data,
            headers={"Content-Type": "application/json"},
            timeout=5
        )

        if response.status_code == 200:
            print(f"✅ Message sent via REST API: {response.json()}")
            return response.json()
    except:
        print("⚠️ REST API endpoint not found, trying WebSocket...")

    # Если REST не работает, пробуем WebSocket
    try:
        import stomp

        class MyListener(stomp.PrintingListener):
            def on_message(self, frame):
                print(f"Received: {frame.body}")

        conn = stomp.Connection([('localhost', 8082)])
        conn.set_listener('', MyListener())
        conn.connect(wait=True)

        # Подписываемся на топик
        topic = f"/topic/chat/{CHAT_ID}"
        conn.subscribe(destination=topic, id=1, ack='auto')
        print(f"✅ Subscribed to {topic}")

        # Отправляем сообщение
        msg = {
            "chatId": CHAT_ID,
            "encryptedContent": base64.b64encode("Hello Blockchain!".encode()).decode()
        }

        conn.send(body=json.dumps(msg), destination="/app/chat.send")
        print("✅ Message sent via WebSocket (STOMP)")

        time.sleep(2)
        conn.disconnect()

        return True

    except Exception as e:
        print(f"❌ WebSocket error: {e}")
        return False


def check_blockchain_status():
    """Проверка статуса блокчейна"""
    try:
        response = requests.get("http://localhost:8083/api/v1/blockchain/stats")
        if response.status_code == 200:
            data = response.json()
            print(f"📊 Blockchain stats: {json.dumps(data, indent=2)}")
            return data
    except Exception as e:
        print(f"❌ Error checking blockchain: {e}")
        return None


def verify_hash(hash_value):
    """Проверка хэша в блокчейне"""
    try:
        response = requests.get(f"http://localhost:8083/api/v1/blockchain/verify/{hash_value}")
        if response.status_code == 200:
            data = response.json()
            print(f"🔍 Hash verification: {json.dumps(data, indent=2)}")
            return data
    except Exception as e:
        print(f"❌ Error verifying hash: {e}")
        return None


if __name__ == "__main__":
    print("=" * 60)
    print("📨 Blockchain Messenger Test")
    print("=" * 60)
    print(f"Chat ID: {CHAT_ID}")
    print()

    # Проверяем текущее состояние
    print("📊 Checking current blockchain state...")
    stats = check_blockchain_status()
    print()

    # Отправляем сообщение
    print("📨 Sending test message...")
    result = send_message_via_websocket()
    print()

    if result:
        print("⏳ Waiting for blockchain registration...")
        time.sleep(3)

        # Проверяем обновленное состояние
        print("📊 Checking updated blockchain state...")
        stats = check_blockchain_status()

        if stats and stats.get('total_registered', 0) > 0:
            print("✅ SUCCESS! Hash registered in blockchain!")
            print(f"   Total registered: {stats['total_registered']}")
            if stats.get('recent'):
                for h in stats['recent']:
                    print(f"   Hash: {h['hash'][:16]}... at {h['registered_at']}")
        else:
            print("⚠️ No new registrations found. Check Kafka and chat-service logs.")
    else:
        print("❌ Failed to send message. Please check chat-service logs.")

    print()
    print("=" * 60)
    print("🔍 Check logs for more details:")
    print("   docker compose logs -f chat-service")
    print("   docker compose logs -f blockchain-adapter")
    print("   docker compose logs -f kafka")
    print("=" * 60)