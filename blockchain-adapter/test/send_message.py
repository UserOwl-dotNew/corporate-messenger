import websocket
import json
import base64

CHAT_ID = "ВАШ_CHAT_ID"


def on_message(ws, message):
    print(f"Received: {message}")


def on_error(ws, error):
    print(f"Error: {error}")


def on_close(ws, close_status_code, close_msg):
    print("Connection closed")


def on_open(ws):
    # Подписка на топик
    subscribe_frame = {
        "destination": f"/topic/chat/{CHAT_ID}",
        "id": "sub-0",
        "ack": "auto"
    }
    ws.send(json.dumps(subscribe_frame))

    # Отправка сообщения
    message = {
        "destination": "/app/chat.send",
        "body": json.dumps({
            "chatId": CHAT_ID,
            "encryptedContent": base64.b64encode("Hello WebSocket!".encode()).decode()
        })
    }
    ws.send(json.dumps(message))
    print("Message sent!")


if __name__ == "__main__":
    ws = websocket.WebSocketApp("ws://localhost:8082/ws",
                                on_open=on_open,
                                on_message=on_message,
                                on_error=on_error,
                                on_close=on_close)
    ws.run_forever()