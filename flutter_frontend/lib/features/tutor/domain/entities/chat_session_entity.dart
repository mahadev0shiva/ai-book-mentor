class MessageEntity {
  final String messageId;
  final String sender; // 'user' or 'tutor'
  final String text;
  final DateTime timestamp;

  const MessageEntity({
    required this.messageId,
    required this.sender,
    required this.text,
    required this.timestamp,
  });
}

class ChatSessionEntity {
  final String sessionId;
  final String bookId;
  final List<MessageEntity> messages;
  final DateTime lastActive;

  const ChatSessionEntity({
    required this.sessionId,
    required this.bookId,
    required this.messages,
    required this.lastActive,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ChatSessionEntity &&
          runtimeType == other.runtimeType &&
          sessionId == other.sessionId &&
          bookId == other.bookId &&
          messages == other.messages &&
          lastActive == other.lastActive;

  @override
  int get hashCode =>
      sessionId.hashCode ^
      bookId.hashCode ^
      messages.hashCode ^
      lastActive.hashCode;
}
