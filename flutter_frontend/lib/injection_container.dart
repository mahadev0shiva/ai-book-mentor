import 'package:get_it/get_it.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'features/auth/data/datasources/auth_remote_data_source.dart';
import 'features/auth/data/repositories/auth_repository_impl.dart';
import 'features/auth/domain/repositories/auth_repository.dart';
import 'features/auth/domain/usecases/login_usecase.dart';
import 'features/auth/presentation/bloc/auth_bloc.dart';

final sl = GetIt.instance;

Future<void> init() async {
  // --- Presentation Layer (BLoCs / Cubits) ---
  sl.registerFactory(() => AuthBloc(loginUseCase: sl()));

  // --- Domain Layer (Use Cases) ---
  sl.registerLazySingleton(() => LoginUseCase(sl()));

  // --- Data Layer (Repositories & Data Sources) ---
  sl.registerLazySingleton<AuthRepository>(
    () => AuthRepositoryImpl(remoteDataSource: sl()),
  );

  sl.registerLazySingleton<AuthRemoteDataSource>(
    () => AuthRemoteDataSourceImpl(firebaseAuth: sl()),
  );

  // --- External Services & Drivers ---
  final firebaseAuth = firebase_auth.FirebaseAuth.instance;
  sl.registerLazySingleton(() => firebaseAuth);
}
