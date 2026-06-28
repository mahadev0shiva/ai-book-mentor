class ChapterEntity {
  final String id;
  final String bookId;
  final int chapterIndex;
  final String title;
  final String contentMarkdown;
  final List<String> concepts;

  const ChapterEntity({
    required this.id,
    required this.bookId,
    required this.chapterIndex,
    required this.title,
    required this.contentMarkdown,
    required this.concepts,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ChapterEntity &&
          runtimeType == other.runtimeType &&
          id == other.id &&
          bookId == other.bookId &&
          chapterIndex == other.chapterIndex &&
          title == other.title &&
          contentMarkdown == other.contentMarkdown &&
          concepts == other.concepts;

  @override
  int get hashCode =>
      id.hashCode ^
      bookId.hashCode ^
      chapterIndex.hashCode ^
      title.hashCode ^
      contentMarkdown.hashCode ^
      concepts.hashCode;
}
