# AI Book Study Companion — Book Processing Pipeline
## Production-Grade Multi-Stage Ingestion, OCR, Parsing & Structuring Architecture

---

## 1. End-to-End Ingestion & Processing Pipeline Architecture

### 1.1 Architectural Design (ASCII Diagram)

```text
[ Upload Source File (PDF, EPUB, DOCX, TXT) ]
                       |
                       v
       +-------------------------------+
       |    Stage 1: File Ingestion    | <--- Malware Scan & File Validation
       +-------------------------------+
                       |
                       v
       +-------------------------------+
       |    Stage 2: Format Parsing    | <--- EPUB / PDF / DOCX Extractor Engines
       +-------------------------------+
                       |
            /----------+----------\
           /                       \
   [ Scanned / Poor Text ]    [ Clean Structural Text ]
         |                                 |
         v                                 v
+------------------+              +------------------+
|  Stage 3: OCR    |              | Stage 4: Text    |
|  (GCP Vision)    |              | Normalization    |
+------------------+              +------------------+
         \                                 /
          \----------------/--------------/
                           |
                           v
       +-------------------------------+
       |  Stage 5: Outline Detection   | <--- Chapters, Sections & Headings
       +-------------------------------+
                       |
                       v
       +-------------------------------+
       | Stage 6: Semantic Structuring | <--- Table, Figure & Footnote Isolation
       +-------------------------------+
                       |
                       v
       +-------------------------------+
       |  Stage 7: Concept Extraction  | <--- Named Entities & Relational Map
       +-------------------------------+
                       |
                       v
       +-------------------------------+
       | Stage 8: Learning Mapping     | <--- Difficulty, Curriculum & Graph Prep
       +-------------------------------+
                       |
                       v
       +-------------------------------+
       | Stage 9: Context Chunking     | <--- Sliding Window Token Optimization
       +-------------------------------+
                       |
                       v
       +-------------------------------+
       |  Stage 10: Task Dispatching   | <--- Firestore Queue & Generation Jobs
       +-------------------------------+
```

### 1.2 Sequence & Data Flow Control Model

```text
Client (App)     GCS Bucket      Cloud Function     Firestore DB       Gemini 3.5 API
    |                 |                 |                 |                   |
    |-- Upload File ->|                 |                 |                   |
    |                 |-- Trigger Event->|                 |                   |
    |                 |                 |-- Init Job ---->|                   |
    |                 |                 |   State         |                   |
    |                 |                 |                 |                   |
    |                 |-- Read Bytes -->|                 |                   |
    |                 |                 |-- Parse/OCR --->|                   |
    |                 |                 |   Progress      |                   |
    |                 |                 |                 |                   |
    |                 |                 |-- Run Outline --------------------->|
    |                 |                 |   Detection                         |
    |                 |                 |<-- JSON Outline Model --------------|
    |                 |                 |                 |                   |
    |                 |                 |-- Map Concepts -------------------->|
    |                 |                 |<-- Concept Array -------------------|
    |                 |                 |                 |                   |
    |                 |                 |-- Write Chapter |                   |
    |                 |                 |   Trees ------->|                   |
    |<-- Sync State --------------------------------------|                   |
```

---

## 2. In-Depth Multi-Stage Pipeline Specification

### Stage 1: File Upload & Sanity Validation
* **Purpose**: Receive user documents securely, verify content signatures, reject malware, and confirm MIME integrity.
* **Inputs**: Dynamic byte streams or multipart file uploads.
* **Outputs**: Cleaned file reference registered under Firestore `book_files` subcollection.
* **Validation & Security**:
  - Enforce max size limit (Development: 50MB, Production: 250MB).
  - Run ClamAV scan trigger within Google Cloud Run before storage landing.
  - Reject spoofed headers by running magic-number byte validations (e.g., verifying `%PDF-` signature at byte offset 0).

### Stage 2: OCR Processing (Scanned Page Layouts)
* **Purpose**: Synthesize rich unicode text blocks from scanned or non-interactive image documents.
* **Algorithm**: Optical Character Recognition via Google Cloud Vision API with layout-boundary clustering.
* **Data Contract / JSON Schema**:
```json
{
  "type": "OBJECT",
  "properties": {
    "pageIndex": { "type": "INTEGER" },
    "boundingBoxes": {
      "type": "ARRAY",
      "items": {
        "type": "OBJECT",
        "properties": {
          "text": { "type": "STRING" },
          "vertices": { "type": "ARRAY", "items": { "type": "INTEGER" } }
        }
      }
    }
  },
  "required": ["pageIndex", "boundingBoxes"]
}
```

### Stage 3: EPUB, PDF & DOCX Native Parsing
* **Purpose**: Extract clean text runs, inline structures, embedded styles, and images.
* **Engines**: 
  - **EPUB**: Custom sax-based XML structural parsing to retain semantic XHTML layout trees.
  - **PDF**: Apache Tika / PDFBox pipeline extracting coordinates, fonts, and table layouts.
  - **DOCX**: XML stream parsing of document relationships.

### Stage 4: Chapter & Outline Detection
* **Purpose**: Identify table of contents entries, chapter dividers, and hierarchical page splits.
* **Model Designation**: `gemini-3.5-flash` for initial structural evaluation.
* **Prompt Strategy**:
```text
Analyze this compiled page index outline. Extract the table of contents and locate the precise structural dividers. Return a strictly valid JSON listing containing the title, index, and start-end page boundaries for every chapter.
Outline sample: ${tocText}
```

### Stage 5: Table & Structural Figure Extraction
* **Purpose**: Isolate visual and tabular assets to prevent raw code injection or data corruption.
* **Approach**: Table layouts are converted to Markdown tables, and mathematical formulas are standardized into KaTeX notation. Visual assets are cropped and saved to GCS `/books/{id}/concepts/diagrams/`.

### Stage 6: Concept & Named Entity Extraction
* **Purpose**: Parse raw chapter bodies to identify definitions, technical theorems, and foundational concepts.
* **Model**: `gemini-3.5-flash` combined with a local regex dictionary.
* **Data Contract / JSON Schema**:
```json
{
  "type": "OBJECT",
  "properties": {
    "extractedConcepts": {
      "type": "ARRAY",
      "items": {
        "type": "OBJECT",
        "properties": {
          "name": { "type": "STRING" },
          "definition": { "type": "STRING" },
          "relatedTerms": { "type": "ARRAY", "items": { "type": "STRING" } }
        }
      }
    }
  },
  "required": ["extractedConcepts"]
}
```

### Stage 7: Learning Mapping & Curriculum Planning
* **Purpose**: Evaluate subject matter depth and structure concepts into a sequential learning path.
* **Model**: `gemini-3.1-pro-preview` for deep semantic relationship mapping.
* **Parameters**: 
  - Difficulty range: $1.0 \rightarrow 10.0$ (Primary school to Academic research).
  - Prerequisites tracing: Edge connection weights representing concept dependency links.

### Stage 8: Token Budgeting & Sliding Window Chunking
* **Purpose**: Divide long textbook structures into optimized context chunks for target models.
* **Sliding Window Parameters**:
  - Target Chunk Size: 16,000 tokens (approx. 12,000 words).
  - Overlap Size: 1,500 tokens (approx. 1,125 words) to maintain continuity.
  - Encoding Engine: `cl100k_base` (tiktoken) for precise token estimation.

---

## 3. High-Volume Processing Queue & Recovery Model

### 3.1 Google Cloud Tasks Orchestration
All long-running generation jobs are dispatched to **Google Cloud Tasks**, offering precise rate-limiting, deduping, and exponential retries:

```text
[Pipeline Dispatcher]
         |
         v
[Google Cloud Tasks Queue] -- rate: 50 tasks/sec
         |
         +--> [Worker 1: OCR Processing] (Cloud Run)
         |
         +--> [Worker 2: Concept Extraction] (Cloud Run)
         |
         +--> [Worker 3: Explanation Generator] (Cloud Run)
```

### 3.2 Error Code Matrix & Mitigation Policies

| Exception Class | Trigger Condition | Backoff Trigger | Recovery Strategy |
| :--- | :--- | :--- | :--- |
| `API_RATE_LIMIT` | HTTP 429 from Gemini | Yes (30s baseline) | Route through secondary API endpoints or fall back to staging API queues. |
| `CORRUPTED_ZIP` | Invalid EPUB structural container | No (Instant Fail) | Fail the current ingestion job and request that the user re-upload the document. |
| `TIMEOUT` | Cloud Run execution > 540s | Yes (10s baseline) | Slice target chapters into smaller sub-paragraphs and restart the processing step. |
| `INVALID_SCHEMA`| Output JSON model parsing error | Yes (3s baseline) | Reparse output text or execute a recovery run with a high-temperature schema model. |

---

## 4. Cost, Performance & Security Optimization Profiles

### 4.1 Ingestion Cost Projections (For 500-Page Textbook)

| Processing Step | Engine / Model | Volume Metrics | Unit Rate | Est. Cost (USD) |
| :--- | :--- | :--- | :--- | :--- |
| **Malware Checking** | Cloud Run (ClamAV) | 500 Pages | \$0.00001 / page | \$0.005 |
| **OCR Ingestion** | Cloud Vision API | 150 Scanned Pages | \$1.50 / 1,000 pages| \$0.225 |
| **Structure Tracing** | `gemini-3.5-flash` | 400,000 tokens | \$0.075 / 1M tokens | \$0.030 |
| **Concept Mapping** | `gemini-3.5-flash` | 400,000 tokens | \$0.075 / 1M tokens | \$0.030 |
| **Explanations** | `gemini-3.1-pro-preview` | 600,000 tokens | \$1.25 / 1M tokens | \$0.750 |
| **Quiz Creation** | `gemini-3.5-flash` | 200,000 tokens | \$0.075 / 1M tokens | \$0.015 |
| **Total Ingestion Cost**| — | — | — | **\$1.055** |

### 4.2 Quality Gate & Validation Playbook
1. **Pre-Processing Validation**: Confirm checksum match against registered book archives in Firestore.
2. **Pedagogical Consistency Validation**: The `Fact Checking Agent` compares the raw text of each chapter against generated explanations. If accuracy drops below 98%, the job is flagged for review.
3. **Format Integrity Gate**: Ensure generated JSON strings precisely match their target JSON schema structures, rejecting corrupted responses.
