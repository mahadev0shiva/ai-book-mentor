class BookEntity {
  final String id;
  final String title;
  final String author;
  final String coverUrl;
  final double progressPercent;
  final String category;
  final DateTime createdAt;

  const BookEntity({
    required this.id,
    required this.title,
    required this.author,
    required this.coverUrl,
    required this.progressPercent,
    required this.category,
    required this.createdAt,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is BookEntity &&
          runtimeType == other.runtimeType &&
          id == other.id &&
          title == other.title &&
          author == other.author &&
          coverUrl == other.coverUrl &&
          progressPercent == other.progressPercent &&
          category == other.category &&
          createdAt == other.createdAt;

  @override
  int get hashCode =>
      id.hashCode ^
      title.hashCode ^
      author.hashCode ^
      coverUrl.hashCode ^
      progressPercent.hashCode ^
      category.hashCode ^
      createdAt.hashCode;
}
