import 'package:dartz/dartz.dart';
import '../../../../core/error/failures.dart';
import '../entities/processing_job_entity.dart';

abstract class BookProcessingRepository {
  Future<Either<Failure, ProcessingJobEntity>> uploadAndProcessBook({
    required String filePath,
    required String title,
    required String author,
    required String category,
  });

  Future<Either<Failure, ProcessingJobEntity>> getProcessingJobStatus(String jobId);
}
