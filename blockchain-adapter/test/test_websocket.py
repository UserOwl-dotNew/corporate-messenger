#!/usr/bin/env python
# -*- coding: utf-8 -*-

import asyncio
import json
import base64
import uuid
import websockets
import stomp
import time

# Конфигурация
CHAT_ID = "ВАШ_CHAT_ID_ЗДЕСЬ"  # Подставьте ID из шага 1
WS_URL = "ws://localhost:8082/ws"


class WebSocketTester:
    def __init__(self, chat_id):
        self.chat_id = chat_id
        self.message_count = 0
        self.websocket = None

    async def connect(self):
        """Подключение к WebSocket через STOMP"""
        # Используем SockJS через веб-сокет напрямую
        # Для простоты используем websocket-client с STOMP протоколом
        pass

    async def test_send_message(self):
        """Отправка тестового сообщения"""
        try:
            # Простой WebSocket без STOMP - для демонстрации
            print(f"Sending test message to chat {self.chat_id}")

            # Симулируем сообщение через REST API (альтернатива WebSocket)
            import requests
            message_data = {
                "chatId": self.chat_id,
                "encryptedContent": base64.b64encode("Hello Blockchain!".encode()).decode()
            }

            response = requests.post(
                "http://localhost:8082/api/v1/messages/test",
                json=message_data,
                headers={"Content-Type": "application/json"}
            )

            if response.status_code == 200:
                print(f"✅ Message sent successfully: {response.json()}")
                return response.json()
            else:
                print(f"❌ Failed to send message: {response.status_code}")
                return None

        except Exception as e:
            print(f"❌ Error: {e}")
            return None


# Для прямого теста через WebSocket STOMP
async def stomp_test():
    """Тест через STOMP WebSocket"""
    try:
        # Используем websockets для STOMP
        uri = "ws://localhost:8082/ws"

        # Это упрощенная версия, для реального теста используйте библиотеку stomp.py
        print("Testing WebSocket connection...")

        # Импортируем библиотеку stomp
        import stomp

        # Создаем соединение
        conn = stomp.Connection([('localhost', 8082)])
        conn.set_listener('', stomp.PrintingListener())
        conn.connect(wait=True)

        # Подписываемся на топик
        topic = f"/topic/chat/{CHAT_ID}"
        conn.subscribe(destination=topic, id=1, ack='auto')
        print(f"Subscribed to {topic}")

        # Отправляем сообщение
        message = {
            "chatId": CHAT_ID,
            "encryptedContent": base64.b64encode("Hello via STOMP!".encode()).decode()
        }

        conn.send(body=json.dumps(message), destination="/app/chat.send")
        print("Message sent!")

        time.sleep(2)
        conn.disconnect()

    except Exception as e:
        print(f"STOMP test error: {e}")


if __name__ == "__main__":
    print("=== WebSocket Test ===")
    print(f"Chat ID: {CHAT_ID}")

    # Запускаем тест
    asyncio.run(stomp_test())