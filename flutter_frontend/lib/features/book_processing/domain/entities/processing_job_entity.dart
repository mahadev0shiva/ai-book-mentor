enum ProcessingStatus { pending, analyzing, parsing, ocr, structured, complete, failed }

class ProcessingJobEntity {
  final String jobId;
  final String bookId;
  final String bookTitle;
  final ProcessingStatus status;
  final double progressPercent;
  final String? errorMessage;
  final DateTime updatedAt;

  const ProcessingJobEntity({
    required this.jobId,
    required this.bookId,
    required this.bookTitle,
    required this.status,
    required this.progressPercent,
    this.errorMessage,
    required this.updatedAt,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ProcessingJobEntity &&
          runtimeType == other.runtimeType &&
          jobId == other.jobId &&
          bookId == other.bookId &&
          bookTitle == other.bookTitle &&
          status == other.status &&
          progressPercent == other.progressPercent &&
          errorMessage == other.errorMessage &&
          updatedAt == other.updatedAt;

  @override
  int get hashCode =>
      jobId.hashCode ^
      bookId.hashCode ^
      bookTitle.hashCode ^
      status.hashCode ^
      progressPercent.hashCode ^
      errorMessage.hashCode ^
      updatedAt.hashCode;
}
