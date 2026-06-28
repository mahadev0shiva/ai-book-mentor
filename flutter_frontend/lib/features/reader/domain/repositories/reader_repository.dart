import 'package:dartz/dartz.dart';
import '../../../../core/error/failures.dart';
import '../entities/chapter_entity.dart';

abstract class ReaderRepository {
  Future<Either<Failure, List<ChapterEntity>>> getBookChapters(String bookId);
  Future<Either<Failure, ChapterEntity>> getChapterDetail(String bookId, int chapterIndex);
  Future<Either<Failure, void>> createHighlight({
    required String bookId,
    required int chapterIndex,
    required String textSegment,
    required int startCharIndex,
    required int endCharIndex,
    required String highlightColor,
  });
}
