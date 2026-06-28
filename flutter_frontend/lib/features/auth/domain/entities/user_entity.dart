class UserEntity {
  final String uid;
  final String email;
  final String displayName;
  final String? photoUrl;

  const UserEntity({
    required this.uid,
    required this.email,
    required this.displayName,
    this.photoUrl,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is UserEntity &&
          runtimeType == other.runtimeType &&
          uid == other.uid &&
          email == other.email &&
          displayName == other.displayName &&
          photoUrl == other.photoUrl;

  @override
  int get hashCode =>
      uid.hashCode ^ email.hashCode ^ displayName.hashCode ^ photoUrl.hashCode;
}
