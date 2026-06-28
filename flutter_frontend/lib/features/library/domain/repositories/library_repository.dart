import 'package:dartz/dartz.dart';
import '../../../../core/error/failures.dart';
import '../entities/book_entity.dart';

abstract class LibraryRepository {
  Future<Either<Failure, List<BookEntity>>> getUserLibrary();
  Future<Either<Failure, BookEntity>> getBookById(String id);
  Future<Either<Failure, void>> updateReadingProgress(String id, double progress);
}
