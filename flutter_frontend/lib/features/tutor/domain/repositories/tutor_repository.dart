import 'package:dartz/dartz.dart';
import '../../../../core/error/failures.dart';
import '../entities/chat_session_entity.dart';

abstract class TutorRepository {
  Future<Either<Failure, ChatSessionEntity>> createTutorSession(String bookId);
  Future<Either<Failure, MessageEntity>> sendMessageToTutor({
    required String sessionId,
    required String userQuery,
  });
  Future<Either<Failure, Map<String, dynamic>>> queryKnowledgeGraph(String bookId);
  Future<Either<Failure, void>> updateStudentMastery({
    required String conceptId,
    required double quizScore,
  });
}
