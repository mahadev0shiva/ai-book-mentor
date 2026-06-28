# AI Book Study Companion — Ebook Assembly & Export Engine
## Production-Grade Specifications for Document Rendering, Multi-Format Packaging, and Signed Access Control

---

## 1. High-Level Export Pipeline Architecture

The Ebook Assembly & Export Engine aggregates all generated study assets (chapters, multi-style deep explanations, vector diagrams, interactive quizzes, flashcards, and knowledge graphs) and compiles them into standard reflowable and print-ready document formats.

### 1.1 Architectural Design (ASCII Diagram)

```text
[ Study Database Assets (Firestore / Room) ]
                    |
                    v
    +-------------------------------+
    |   Stage 1: Asset Aggregation  | <--- Resolves Text and GCS Image Coordinates
    +-------------------------------+
                    |
                    v
    +-------------------------------+
    |  Stage 2: Layout Processing   | <--- Translates Markdown to CSS/Layout Trees
    +-------------------------------+
                    |
          /---------+---------\
         /          |          \
        v           v           v
  +-----------+ +-----------+ +-----------+
  |  PDF-Print| |Reflowable | | DOCX-XML  |
  |  (Weasy)  | | EPUB v3   | | Generator |
  +-----------+ +-----------+ +-----------+
        \           |           /
         \----------+----------/
                    |
                    v
    +-------------------------------+
    | Stage 4: Package Compression  | <--- ZIP, WebP Asset Inlining
    +-------------------------------+
                    |
                    v
    +-------------------------------+
    | Stage 5: GCS Storage Landing  | <--- Under /users/{userId}/exports/{id}
    +-------------------------------+
                    |
                    v
   [ Secure Signed URL Issued to App ]
```

---

## 2. Multi-Format Rendering & Compilation Pipelines

### 2.1 PDF Rendering Pipeline (Print & High-Fidelity Layouts)
* **Purpose**: Generate elegant, print-ready PDF ebooks with consistent typography, headers, footers, and page numbers.
* **Engine**: Headless HTML-to-PDF compilation via **WeasyPrint** running in isolated Cloud Run containers. This supports full CSS Paged Media standards.
* **Layout Specifications**:
  - **Margins**: $2.0\text{ cm}$ top/bottom, $2.5\text{ cm}$ inside gutter, $2.0\text{ cm}$ outside.
  - **Typography**: "Lora" serif body font paired with "Inter" sans-serif display headings.
  - **Visual Containers**:
    - *Analogies*: Left border accented with a dynamic light blue line (`#0284C7`), light background padding.
    - *Definitions*: Filled, light gray background container (`#F3F4F6`), with subtle rounded corners ($4\text{dp}$).

### 2.2 Reflowable EPUB v3 Packaging Engine
* **Purpose**: Package study content into reflowable, standard-compliant e-reader documents compatible with mobile devices.
* **Structure & Assembly**:
  - Core book chapters are compiled as isolated XHTML documents (`chapter_001.xhtml`).
  - Style rules are bundled into a single `stylesheet.css` using relative spacing metrics (`em` or `%`) to allow font scaling on various device sizes.
  - Metadata is packaged into `package.opf` along with `toc.ncx` for system navigation maps.

---

## 3. Storage & Access Security Architecture

All compiled ebook portfolios are stored securely in Google Cloud Storage using short-lived signed access controls.

### 3.1 Signed URL Allocation Strategy
1. Ebooks are written to `gs://aistudio-book-study-prod/users/{userId}/exports/{exportId}.{ext}`.
2. Direct access to GCS buckets is restricted. The Flutter client requests download links from a secure Cloud Function.
3. The Cloud Function verifies the user's active session token, checks if the user owns the export ID, and generates a time-limited signed URL valid for exactly $900$ seconds:

```kotlin
val blobInfo = BlobInfo.newBuilder(bucketName, blobName).build()
val signedUrl = storage.signUrl(
    blobInfo,
    15,
    TimeUnit.MINUTES,
    Storage.SignUrlOption.withV4Signature()
)
```

---

## 4. Offline Package Format (Client Sync Layout)

For active studying in poor network conditions, the system bundles study data into a lightweight **Offline Package Schema (JSON)** stored in the local Room database, paired with local WebP visual assets.

### 4.1 Local Bundle JSON Schema

```json
{
  "type": "OBJECT",
  "properties": {
    "bundleId": { "type": "STRING" },
    "associatedBookId": { "type": "STRING" },
    "chaptersCount": { "type": "INTEGER" },
    "chapters": {
      "type": "ARRAY",
      "items": {
        "type": "OBJECT",
        "properties": {
          "chapterIndex": { "type": "INTEGER" },
          "title": { "type": "STRING" },
          "contentMarkdown": { "type": "STRING" },
          "localImagePaths": { "type": "ARRAY", "items": { "type": "STRING" } }
        },
        "required": ["chapterIndex", "title", "contentMarkdown"]
      }
    }
  },
  "required": ["bundleId", "associatedBookId", "chaptersCount", "chapters"]
}
```

---

## 5. Performance & Resource Optimization Checklists

- [x] **In-Memory ZIP Assembly**: EPUB packaging is compiled inside temporary container RAM streams (`MemoryStream`) to prevent high disk write latency.
- [x] **Lossless WebP Illustration Conversion**: Original high-resolution PNG illustration drafts are downsampled to $1024\text{px}$ width WebP format, reducing asset file sizes by up to 75% without compromising clarity.
- [x] **Chunked Database Queries**: Do not load all chapter sub-nodes simultaneously when aggregating assets. Pull and compile chapters sequentially to avoid memory pressure.
- [x] **Dynamic CSS Media Checks**: Print media stylesheets ignore interactive buttons, navigation rails, and chat text fields, automatically excluding them from PDF compilation.
