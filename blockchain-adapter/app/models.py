from pydantic import BaseModel
from datetime import datetime
from typing import Optional
from uuid import UUID

class KafkaMessageEvent(BaseModel):
    messageId: UUID
    chatId: UUID
    senderId: UUID
    contentHash: str
    encryptedContent: str
    timestamp: int

class BlockchainRegistration(BaseModel):
    message_id: UUID
    content_hash: str
    blockchain_tx_id: str
    registered_at: datetime

class BlockchainVerification(BaseModel):
    hash: str
    is_registered: bool
    registered_at: Optional[datetime] = None
    registered_by: Optional[str] = None

class UpdateBlockchainTxRequest(BaseModel):
    blockchain_tx_id: str