# AI Book Study Companion — Database Schema & Data Architecture
## Production-Grade Firestore & Storage Architecture Specification
*Designed for Multi-Agent Orchestration, Offline-First Synchronization, and Million-User Scalability*

---

## 1. High-Level Entity-Relationship (ER) Diagram

```text
+------------------+         1:1         +------------------+
|      users       |-------------------->|     profiles     |
+------------------+                     +------------------+
         | 1:1                                    | 1:Many
         v                                        v
+------------------+                     +------------------+
|     settings     |                     |  study_sessions  |
+------------------+                     +------------------+
         | 1:1                                    |
         v                                        v
+------------------+                     +------------------+
| learning_pref    |                     |   achievements   |
+------------------+                     +------------------+
         |                                        | 1:Many
         |                                        v
         |                               +------------------+
         |                               |     badges       |
         |                               +------------------+
         |                                        | 1:Many
         |                                        v
         |                               +------------------+
         |                               |   certificates   |
         |                               +------------------+
         |
         | 1:Many
         v
+------------------+         1:Many      +------------------+
|      books       |-------------------->|   book_imports   |
+------------------+                     +------------------+
   | 1:1      | 1:Many                             | 1:1
   v          v                                    v
+-------+  +--------------+              +------------------+
| b_file|  |  chapters    |              |  ocr_jobs /      |
+-------+  +--------------+              |  parsing_jobs    |
                  |                      +------------------+
                  | 1:Many
                  v
           +--------------+
           |   sections   |
           +--------------+
                  |
                  | 1:Many
                  v
           +--------------+      1:Many  +------------------+
           |   concepts   |------------->|  gen_explanations|
           +--------------+              +------------------+
             | 1:1    | 1:1                       |
             v        v                           v
         +-------+ +--------+            +------------------+
         |diagram| |mind_map|            |  illustrations/  |
         +-------+ +--------+            |  infographics    |
                                         +------------------+
                                                  |
                                                  v
                                         +------------------+
                                         |    downloads     |
                                         +------------------+

                                         +------------------+
                                         |     quizzes      |
                                         +------------------+
                                           | 1:Many
                                           v
                                         +------------------+
                                         |    questions     |
                                         +------------------+
                                           | 1:Many
                                           v
                                         +------------------+
                                         |     answers      |
                                         +------------------+

                                         +------------------+
                                         |  bookmarks/notes |
                                         |  highlights      |
                                         +------------------+

                                         +------------------+
                                         |    ai_chats      |
                                         +------------------+
                                           | 1:Many
                                           v
                                         +------------------+
                                         |   ai_messages    |
                                         +------------------+

                                         +------------------+
                                         |  knowledge_graph |
                                         |  (nodes & edges) |
                                         +------------------+
```

---

## 2. Firestore Document Hierarchy

```text
/users/{userId} (document)
   |-- profiles (subcollection)
   |-- settings (subcollection)
   |-- learning_preferences (subcollection)
   |-- bookmarks (subcollection)
   |-- highlights (subcollection)
   |-- notes (subcollection)
   |-- study_sessions (subcollection)
   |-- achievements (subcollection)
   |-- badges (subcollection)
   |-- certificates (subcollection)
   |-- downloads (subcollection)
   |-- offline_cache_metadata (subcollection)
   |-- sync_queue (subcollection)
   |-- ai_chats (subcollection)
         |-- ai_messages (subcollection)

/books/{bookId} (document)
   |-- book_metadata (subcollection)
   |-- book_files (subcollection)
   |-- book_imports (subcollection)
   |-- ocr_jobs (subcollection)
   |-- parsing_jobs (subcollection)
   |-- chapters (subcollection)
         |-- sections (subcollection)
               |-- concepts (subcollection)
                     |-- gen_explanations (subcollection)
                     |-- illustrations (subcollection)
                     |-- diagrams (subcollection)
                     |-- mind_maps (subcollection)
                     |-- infographics (subcollection)
   |-- quizzes (subcollection)
         |-- questions (subcollection)
               |-- answers (subcollection)
   |-- flashcards (subcollection)
   |-- revision_plans (subcollection)
   |-- comments (subcollection)

/knowledge_graphs/{graphId} (document)
   |-- nodes (subcollection)
   |-- edges (subcollection)

/system_configs/{configId} (document)
   |-- feature_flags (subcollection)
   |-- experiments (subcollection)

/logs (collection)
   |-- crash_logs (subcollection)
   |-- error_logs (subcollection)
   |-- audit_logs (subcollection)
   |-- analytics_events (subcollection)

/admin_users/{adminId} (document)
   |-- admin_actions (subcollection)
```

---

## 3. Firebase Storage Hierarchy

```text
/users/{userId}/
   |-- avatar.jpg                      # User profile avatar
   |-- uploads/
       |-- {uploadId}.pdf              # Original uploaded PDF
       |-- {uploadId}.epub             # Original uploaded EPUB
       |-- {uploadId}.docx             # Original uploaded DOCX
   |-- exports/
       |-- {exportId}.pdf              # Generated compiled PDF portfolio
       |-- {exportId}.epub             # Generated EPUB
       |-- {exportId}.docx             # Generated DOCX
   |-- offline_temp/                   # Dynamic background temp file assembly

/books/{bookId}/
   |-- system_assets/
       |-- cover.jpg                   # Extracted or AI-generated cover image
   |-- concepts/
       |-- illustrations/
           |-- {conceptId}_ill.png     # Stable-Diffusion or Imagen vector style art
       |-- diagrams/
           |-- {conceptId}_diag.svg    # Vector flowcharts and technical layouts
       |-- mind_maps/
           |-- {conceptId}_map.svg     # SVG mind-maps
       |-- infographics/
           |-- {conceptId}_info.png    # Summary graphic sheets
   |-- audio/
       |-- {chapterId}_lesson.mp3      # ElevenLabs synthesized voice curriculum
   |-- video/
       |-- {chapterId}_summary.mp4     # Dynamic slideshow synthesis (future)

/system/
   |-- default_templates/
       |-- certificate_template.pdf    # Cryptographic certificate vector layout
   |-- dynamic_assets/                 # Onboarding banners and illustration slides
```

---

## 4. End-to-End AI Data Flow

```text
[User Uploads PDF/EPUB]
         |
         v
[Storage: /users/{userId}/uploads/{uploadId}.pdf]
         |
         +--> Trigger Cloud Function: onBookUpload()
         |
         v
[OCR Node (Google Cloud Vision API)] <--- (If scanned image PDF)
         |
         +--> Writes: /books/{bookId}/ocr_jobs/{jobId}
         |
         v
[Document Parsing Engine]
         |
         +--> Writes: /books/{bookId}/parsing_jobs/{jobId}
         |
         v
[Chapter & Structure Detection Agent]
         |
         +--> Writes: /books/{bookId}/chapters/*
         |
         v
[Core Concept Extraction Agent]
         |
         +--> Writes: /books/{bookId}/chapters/{chId}/sections/*/concepts/*
         |
         +-----------------------------+-------------------------------+
         |                             |                               |
         v                             v                               v
[AI Deep Explanation Agent]  [Visual Model Generator]       [Evaluation Architect]
         |                             |                               |
         +-> Writes: gen_explanations  +-> Storage: diagrams/*.svg     +-> Writes: quizzes/*
         |                             +-> Storage: illustrations/*.png+-> Writes: flashcards/*
         v                             v                               v
[Knowledge Graph Syncer] ------------> [PDF/EPUB Ebook Generator] ----> [Offline Cache Sync]
  - Writes nodes & edges                 - Assembles chapters & images    - Client pulls via Flow
```

---

## 5. Comprehensive Collection Designs

### 5.1 Users Collection
* **Purpose**: Store root account lifecycle details and user verification vectors.
* **Relationships**: 1:1 with `profiles`, `settings`, `learning_preferences`. 1:Many with `ai_chats`, `study_sessions`.
* **JSON Example**:
```json
{
  "userId": "usr_9x82k371m",
  "email": "student@university.edu",
  "createdAt": "2026-06-28T05:40:00Z",
  "lastActiveAt": "2026-06-28T05:45:00Z",
  "status": "active",
  "authProvider": "google.com",
  "subscriptionTier": "premium",
  "stripeCustomerId": "cus_N7xH81k"
}
```
* **Indexes**: 
  * Single: `email` (Ascending)
  * Composite: `status` (Asc) + `createdAt` (Desc)
* **Security Rules**:
```javascript
match /users/{userId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```
* **Scaling Strategy**: Single document per user prevents hotkeys. Max write throughput is 10k/sec globally.
* **Cost Optimization**: Exclude non-frequently-queried sub-fields from the root user document; push settings to a subcollection.

---

### 5.2 Settings Collection
* **Purpose**: Client application runtime parameters, sync controls, and high-fidelity themes.
* **Relationships**: Subcollection of `users`.
* **JSON Example**:
```json
{
  "themeStyle": "Cobalt Star",
  "isDarkTheme": true,
  "fontSizeSp": 16.0,
  "autoDownloadOnWifi": true,
  "hapticFeedbackEnabled": true,
  "syncIntervalSeconds": 300,
  "ttsVoiceId": "en-US-Neural2-F"
}
```
* **Indexes**: None required (queried strictly by direct document ID lookup).
* **Security Rules**: Only owner has read/write privileges.
* **Scaling Strategy**: Cached client-side indefinitely via offline cache. Saves Firestore reads.

---

### 5.3 Learning Preferences Collection
* **Purpose**: Tailor multi-agent prompt synthesis based on students' preferred curriculum style.
* **Relationships**: Subcollection of `users`.
* **JSON Example**:
```json
{
  "explanationDepth": "Intermediate",
  "readingLevel": "Undergraduate",
  "teachingStyle": "Socratic Method",
  "imageDensity": "Standard",
  "includeQuizzes": true,
  "includeFlashcards": true,
  "primaryDomainOfInterest": "Computer Science"
}
```
* **Indexes**: None required.
* **Security Rules**: Owner access only.
* **Cost Optimization**: Combined with AI prompt generation to reduce runtime database lookups.

---

### 5.4 Books Collection
* **Purpose**: Grounding catalog for student libraries, containing structural outlines and active metadata.
* **Relationships**: 1:Many with `chapters`, `quizzes`, `flashcards`.
* **JSON Example**:
```json
{
  "bookId": "bk_cs101_algo",
  "title": "Introduction to Algorithms",
  "author": "Thomas H. Cormen",
  "category": "Computer Science",
  "description": "Comprehensive reference text on sorting, graph structures, and algorithmic complexity.",
  "createdByUserId": "usr_9x82k371m",
  "isPublicDomain": false,
  "createdAt": "2026-06-28T05:41:00Z",
  "processingStatus": "completed",
  "pageCount": 1312
}
```
* **Indexes**:
  * Single: `createdByUserId` (Asc)
  * Composite: `createdByUserId` (Asc) + `createdAt` (Desc)
* **Security Rules**:
```javascript
match /books/{bookId} {
  allow read: if resource.data.isPublicDomain == true || resource.data.createdByUserId == request.auth.uid;
  allow create: if request.auth != null;
  allow update, delete: if resource.data.createdByUserId == request.auth.uid;
}
```
* **Scaling Strategy**: Book structure is divided into `chapters` and `sections` subcollections to avoid the 1MB Firestore document limit.

---

### 5.5 Chapters Collection
* **Purpose**: Logical segments of books acting as study milestones.
* **Relationships**: Child of `books`. 1:Many with `sections`.
* **JSON Example**:
```json
{
  "chapterId": "ch_01_foundations",
  "index": 1,
  "title": "The Role of Algorithms in Computing",
  "summary": "This chapter defines algorithms, examines resource efficiency, and details their essential role in modern systems.",
  "coreConcepts": "• **Algorithmic Complexity**: Analyzing processing bounds.\n• **Resource Trade-offs**: Speed vs. memory constraints.",
  "wordCount": 4200
}
```
* **Indexes**:
  * Composite: `__name__` (Asc) + `index` (Asc) [Internal paging]
* **Security Rules**: Read enabled for authorized book owners.

---

### 5.6 Concepts Collection
* **Purpose**: Extracted conceptual building blocks from chapter contents.
* **Relationships**: Subcollection of `sections` or `chapters`. Parent of `gen_explanations`.
* **JSON Example**:
```json
{
  "conceptId": "concept_complexity_01",
  "name": "Asymptotic Big-O Notation",
  "description": "Mathematical framework representing the precise upper bound of execution duration.",
  "analogy": "Like planning a journey's worst-case traffic delay to guarantee you arrive on schedule.",
  "visualIllustration": "A plot diagram illustrating f(n) staying strictly underneath c*g(n) for all n >= n0.",
  "selfCheck": "Explain why constant multipliers are discarded when evaluating asymptotic bounds."
}
```
* **Indexes**:
  * Single: `name` (Asc)
* **Security Rules**: Inherited from parent book credentials.

---

### 5.7 Quizzes Collection
* **Purpose**: Interactive assessment packages evaluating students' comprehension of chapter concepts.
* **Relationships**: Subcollection of `books` or `chapters`. Parent of `questions`.
* **JSON Example**:
```json
{
  "quizId": "qz_ch1_foundations",
  "title": "Foundational Asymptotic Evaluation",
  "conceptCount": 5,
  "difficulty": "Intermediate",
  "createdAt": "2026-06-28T05:43:00Z"
}
```
* **Security Rules**: Read enabled for book owners.

---

### 5.8 Questions Collection
* **Purpose**: Direct assessment item configurations.
* **Relationships**: Subcollection of `quizzes`.
* **JSON Example**:
```json
{
  "questionId": "q_01",
  "text": "Which of the following functions grows asymptotically fastest?",
  "options": ["O(n log n)", "O(n^2)", "O(2^n)", "O(n!)"],
  "correctAnswerIndex": 3,
  "explanation": "Factorial complexity O(n!) grows exponentially faster than standard exponential growth O(2^n).",
  "associatedConceptId": "concept_complexity_01"
}
```

---

### 5.9 AI Chats Collection
* **Purpose**: Active interactive dialog context sessions between the Student and the AI Academic Tutor.
* **Relationships**: Subcollection of `users`. Parent of `ai_messages`.
* **JSON Example**:
```json
{
  "chatId": "chat_algorithms_101",
  "title": "Algorithmic Complexity Clarification",
  "associatedBookId": "bk_cs101_algo",
  "associatedChapterId": "ch_01_foundations",
  "createdAt": "2026-06-28T05:44:00Z",
  "lastMessageAt": "2026-06-28T05:45:10Z"
}
```
* **Indexes**:
  * Composite: `lastMessageAt` (Desc)
* **Security Rules**: Strictly owner user-accessible.

---

### 5.10 AI Messages Collection
* **Purpose**: Detailed conversation transcript steps.
* **Relationships**: Subcollection of `ai_chats`.
* **JSON Example**:
```json
{
  "messageId": "msg_001",
  "sender": "user",
  "text": "Why does quicksort have a worst-case complexity of O(n^2)?",
  "timestamp": "2026-06-28T05:44:45Z",
  "suggestedPrompts": ["How do randomized pivots prevent this?", "Show me a comparison matrix."]
}
```

---

### 5.11 Sync Queue Collection
* **Purpose**: Buffers transactional client edits in offline scenarios, performing atomic replays during reconnections.
* **Relationships**: Subcollection of `users`.
* **JSON Example**:
```json
{
  "eventId": "evt_9a87f1k3",
  "operation": "CREATE_HIGHLIGHT",
  "targetPath": "users/usr_9x82k371m/highlights/hl_new_001",
  "payload": {
    "bookId": "bk_cs101_algo",
    "chapterId": "ch_01_foundations",
    "selectedText": "Algorithms must possess a termination condition.",
    "timestamp": "2026-06-28T05:42:15Z"
  },
  "retryCount": 0,
  "status": "pending"
}
```

---

## 6. Security Matrix & Rules

### 6.1 Authentication Strategy
- Identity verification via **Firebase Authentication** supporting OAuth2 (Google Workspace, Apple Sign-in, Custom JWT).
- Session tokens utilize SHA-256 validation rotated every 3600 seconds.

### 6.2 Security Policy Rules (`firestore.rules`)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Core check helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    function isAdmin() {
      return isAuthenticated() && exists(/databases/$(database)/documents/admin_users/$(request.auth.uid));
    }

    // --- Users Root Route ---
    match /users/{userId} {
      allow read, write: if isOwner(userId) || isAdmin();
      
      match /settings/{settingId} {
        allow read, write: if isOwner(userId);
      }
      
      match /learning_preferences/{preferenceId} {
        allow read, write: if isOwner(userId);
      }
      
      match /bookmarks/{bookmarkId} {
        allow read, write: if isOwner(userId);
      }
      
      match /highlights/{highlightId} {
        allow read, write: if isOwner(userId);
      }
      
      match /notes/{noteId} {
        allow read, write: if isOwner(userId);
      }
      
      match /ai_chats/{chatId} {
        allow read, write: if isOwner(userId);
        
        match /ai_messages/{msgId} {
          allow read, write: if isOwner(userId);
        }
      }
      
      match /sync_queue/{eventId} {
        allow read, write: if isOwner(userId);
      }
    }

    // --- Books Master Catalog Route ---
    match /books/{bookId} {
      allow read: if isAuthenticated() && (resource.data.isPublicDomain == true || resource.data.createdByUserId == request.auth.uid || isAdmin());
      allow create: if isAuthenticated();
      allow update, delete: if isAuthenticated() && (resource.data.createdByUserId == request.auth.uid || isAdmin());
      
      match /chapters/{chapterId} {
        allow read: if isAuthenticated();
        allow write: if isAdmin() || (get(/databases/$(database)/documents/books/$(bookId)).data.createdByUserId == request.auth.uid);
      }
      
      match /quizzes/{quizId} {
        allow read: if isAuthenticated();
        allow write: if isAdmin() || (get(/databases/$(database)/documents/books/$(bookId)).data.createdByUserId == request.auth.uid);
      }
    }
    
    // --- Admin Control Route ---
    match /admin_users/{adminId} {
      allow read: if isAdmin();
      allow write: if false; // System provisioned only
    }
  }
}
```

### 6.3 Storage Access Protection (`storage.rules`)

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    // User personal storage folder
    match /users/{userId}/{allPaths=**} {
      allow read, write: if isOwner(userId);
    }

    // System wide public books assets
    match /books/{bookId}/{allPaths=**} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && request.auth.token.admin == true;
    }
  }
}
```

---

## 7. Data Access Matrices

### 7.1 Read/Write Frequency Matrix

| Collection Name | Typical Read Event | Typical Write Event | Read Frequency | Write Frequency | Caching Policy |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **users** | App Login | Account Sign-up | Extremely Low | Extremely Low | Persisted Local State |
| **settings** | UI Launch | Option Toggle | Low | Low | Permanent Offline |
| **books** | Library Drawer | Upload Success | Medium | Low | Memory & DB Cache |
| **chapters** | Reader Section | AI Pipeline | Medium | Low | Active Session Memory |
| **concepts** | Mind Map Tab | Generation Agent | High (on view) | Low | Local Storage |
| **bookmarks** | Bookmark Sync | Tap Icon | Medium | Medium | Immediate Sync |
| **ai_chats** | Chat Thread | Message Sent | High | High | Dynamic Sync Flow |
| **sync_queue** | Net Reconnect | Offline Mode Active | Low | Burst on Net Drop | Client Memory Store |

---

### 7.2 Security Capability Matrix

| Actor Role | read:users | write:users | read:books (Private) | read:books (Public) | write:books | read:admin_actions | write:admin_actions |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Anonymous** | ❌ Denied | ❌ Denied | ❌ Denied | ❌ Denied | ❌ Denied | ❌ Denied | ❌ Denied |
| **Basic Scholar**| 🟢 Allowed (Self) | 🟢 Allowed (Self) | 🟢 Allowed (Self Owned) | 🟢 Allowed | 🟢 Allowed (Own Assets) | ❌ Denied | ❌ Denied |
| **Premium Scholar**| 🟢 Allowed (Self) | 🟢 Allowed (Self) | 🟢 Allowed (Self Owned) | 🟢 Allowed | 🟢 Allowed (Own Assets) | ❌ Denied | ❌ Denied |
| **Platform Admin**| 🟢 Allowed (All) | 🟢 Allowed (All) | 🟢 Allowed (All) | 🟢 Allowed | 🟢 Allowed (All) | 🟢 Allowed | 🟢 Allowed |

---

## 8. Offline-First Synchronization & Partition Strategy

### 8.1 Offline Cache Strategy
1. **Firestore Persistence Engine**: Enforced client-side persistence configured dynamically:
   ```kotlin
   val settings = firestoreSettings {
       isPersistenceEnabled = true
       cacheSizeBytes = FirestoreSettings.CACHE_SIZE_UNLIMITED
   }
   db.firestoreSettings = settings
   ```
2. **Deterministic Partitioning**:
   - Primary user settings, highlights, and local notes use Firestore's local SQLite offline backing.
   - Chapter reading text and generated illustrations are saved locally in the Android app directory as plain text chunks and offline WebP assets.

### 8.2 Conflict Resolution Design
The system utilizes a structured **Last-Write-Wins (LWW)** vector clock for highlights and note logs, coupled with a linear retry queue for state-changing activities (like completing quizzes):

```text
               [Offline Edit Performed]
                          |
                          v
        [Write Event added to Client Queue]
                          |
                          v
         [Reconnection Signal Received]
                          |
                          v
        [Push Event Payload to Sync Queue]
                          |
            /-------------+-------------\
           /                             \
    [No Remote Modifications]    [Remote Conflict Detected]
         |                                 |
         v                                 v
  [Atomic Write Applied]         [Evaluate Event Clocks]
                                           |
                                  /--------+--------\
                                 /                   \
                   [Client-Side Newer]          [Server-Side Newer]
                          |                             |
                          v                             v
             [Overwrite Server Document]     [Discard Client Event, Pull]
```

---

## 9. Performance & Cost Optimization Checklists

### 9.1 Performance Acceleration Checklist
- [x] **Subcollection Decoupling**: Avoid nesting bloated lists inside root document arrays. Store individual highlights and quiz questions in individual documents inside subcollections.
- [x] **Client-Side Query Pagination**: Implement Firestore query limit boundaries (`limit(30)`) along with query cursors (`startAfter(lastDocument)`) when viewing the public domain library.
- [x] **Static Document Cache Maps**: Build memory-resident mappings in the Android ViewModel cache layer to skip reloading identical structures on UI recomposition.
- [x] **Pre-Synthesized Cover Previews**: Always load 150px compressed WebP thumbnails of covers in grid list formats instead of full resolution uploaded files.

### 9.2 Cost Mitigation Checklist
- [x] **No Multi-Wildcard Queries**: Eliminate query requests requiring costly full collection group scans where possible. Ensure compound lookups use strict composite indexes.
- [x] **Aggressive Value Denormalization**: Store basic author names and total section counts inside the root `books` collection to completely bypass subcollection reads on dashboard loads.
- [x] **Selective Sync Engine**: Exclude real-time listeners on static elements. Only run `addSnapshotListener` on messaging chats; query details and settings using simple one-time `get()` actions.
- [x] **Client Side Diffing**: Match existing data checksums before performing write actions to avoid unnecessary write operations.

---

## 10. Enterprise Backup & Migration Playbook

### 10.1 Multi-Region Cloud Backup Strategy
1. **Automated Daily Snapshot Exports**: Set up a Cloud Scheduler cron task that invokes an Cloud Function daily to perform a complete partition backup of firestore databases to a multi-region cold bucket:
   ```bash
   gcloud firestore export gs://my-platform-backups/firestore-daily-snapshots
   ```
2. **Point-In-Time Recovery (PITR)**: Enable PITR in production to allow instant restoration of documents to any precise timestamp down to the microsecond for up to 7 consecutive days.

### 10.2 Database Version Migration Protocol
To modify schemas dynamically without risking downtime:
1. **Phase 1: Dual-Write**: Introduce new schema parameters alongside old structures. Build the app codebase to read both values, writing new records using the new standard.
2. **Phase 2: Database Scan**: Execute a background Dataflow batch job to migrate historical documents, populating fallback values in old assets.
3. **Phase 3: Clean-Up**: Deprecate legacy parsing logic and drop redundant parameters from the active codebase.
