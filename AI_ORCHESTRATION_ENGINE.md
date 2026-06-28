# AI Book Study Companion — AI Orchestration & Multi-Agent Architecture
## Enterprise Production-Grade Core Specifications for Gemini-Powered Cognitive Pipelines

---

## 1. High-Level AI Pipeline Flow & Orchestration Architecture

### 1.1 Architectural Design (ASCII Diagram)

```text
[ Book Upload (PDF / EPUB / OCR) ]
                 |
                 v
       +------------------+
       |   OCR / Parser   |
       +------------------+
                 |
                 v
       +------------------+
       |  Chapter Detec.  | <--- [Workflow Orchestrator (gemini-3.1-pro-preview)]
       +------------------+
                 |
                 v
       +------------------+
       |  Concept Extrac. | <--- [Fact Checking & Consistency Agent]
       +------------------+
                 |
                 v
       +------------------+
       | Learn Profile Ad.| <--- [Curriculum Planner (Adapts explanations)]
       +------------------+
                 |
                 +-----------------------+-----------------------+
                 |                       |                       |
                 v                       v                       v
       +------------------+    +------------------+    +------------------+
       | Deep Explanation |    |  Visual Planning |    | Evaluation Arch. |
       |     Generator    |    | (Diagrams, Maps) |    | (Quiz & Flash)   |
       +------------------+    +------------------+    +------------------+
                 |                       |                       |
                 +-----------------------+-----------------------+
                                         |
                                         v
                               +------------------+
                               | Knowledge Graph  |
                               |  Builder Agent   |
                               +------------------+
                                         |
                                         v
                               +------------------+
                               |  Quality Review  | <--- [Fact Checking & Consistency]
                               +------------------+
                                         |
                                         v
                               +------------------+
                               |   Final Ebook    |
                               |  Assembly Agent  |
                               +------------------+
                                         |
                                         v
                            [ Production Learning Portfolio ]
```

### 1.2 Multi-Agent Chaining Sequence

```text
Student      Orchestrator    ReaderAgent     TeacherAgent    ReviewerAgent    Database
   |              |               |               |               |              |
   |-- Upload -->|               |               |               |              |
   |   Book       |               |               |               |              |
   |              |-- Parse ----->|               |               |              |
   |              |   Chapters    |               |               |              |
   |              |<-- Content ---|               |               |              |
   |              |                               |               |              |
   |              |-- Plan Curriculum ------------>|               |              |
   |              |   Based on Learning Profile   |               |              |
   |              |<-- Curriculum Schema ---------|               |              |
   |              |                               |               |              |
   |              |-- Generate Deep Explanations ->|               |              |
   |              |<-- Drafted Explanations ------|               |              |
   |              |                                               |              |
   |              |-- Verify Schema & Fact Check ---------------->|              |
   |              |<-- Validated Payload & Graphics Data ----------|              |
   |              |                                                              |
   |              |-- Persist Portfolio ---------------------------------------->|
   |              |<-- Sync Complete --------------------------------------------|
   |<-- Ready ----|
```

---

## 2. Token & Context Management Strategy for Long Books

### 2.1 Sliding Window & Semantic Hierarchical Chunking
Handling massive textbooks (e.g., 1000+ pages) without overflowing the model context window or experiencing context dilution requires a specialized **Hierarchical Decomposition & MapReduce Strategy**:

1. **Phase 1 (Decomposition)**:
   - Chunk document contents using an abstract semantic-boundary parser. Boundaries are determined by a regex detecting markdown header sections or structural page breaks, target chunk sizing is kept at roughly 12,000 tokens (approx. 9,000 words).
   - Each chunk has an overlapping border window of exactly 1,000 tokens to preserve continuity of context across cut lines.

2. **Phase 2 (Individual Synthesis)**:
   - Run the **Concept Extraction Agent** and **Chapter Detection Agent** in parallel across each semantic chunk asynchronously using Kotlin Coroutines on `Dispatchers.IO`.

3. **Phase 3 (Reduce / Map-To-Global-Tree)**:
   - A global outline tree is built by combining all summaries. The **Workflow Orchestrator** reads this outline to create the central high-density **Knowledge Graph** index map, identifying overlapping themes across chapters.

### 2.2 Resume, Recovery, and Transactional Retry Architecture
Each processing pipeline stage is run as an idempotent database transaction. If an API request fails, is rate-limited, or times out:
- **Checkpointing**: State-saving coordinates are written to Firestore under `books/{bookId}/parsing_jobs/{jobId}` specifying the current chapter number, last processed block, and chunk index.
- **Backoff Protocol**: Calls use an exponential backoff formula: $RetryDelay = BaseDelay \times 2^{AttemptCount}$ (with jitter up to 10 seconds).
- **Graceful Resumption**: On restart, the orchestrator pulls the last `completed` checkpoint from Firestore and continues processing only the remaining queue elements.

---

## 3. Specialized Multi-Agent Frameworks

### 3.1 Workflow Orchestrator
* **Purpose**: Act as the master control loop, dispatching chunks to parsing, planning, and evaluation agents.
* **Responsibilities**: Coordinate pipeline checkpoints, aggregate agent JSON models, handle circuit breaking, and format final payloads.
* **Model Designation**: `gemini-3.1-pro-preview` (Highly robust logical planner)
* **Inputs**: Book upload files, current student learning profile metadata.
* **Outputs**: Sequential task assignment matrix, pipeline checkpoint logs.
* **System Prompt**:
```text
You are the Lead Master Academic Orchestrator. Your task is to process incoming books, breakdown their chapter structures, adapt their explanations based on target student preferences, and coordinate visual and evaluation pipelines.
You MUST enforce the following constraints:
1. Always generate output strictly following the JSON Schema contract.
2. Maintain checkpoint records to enable process resuming.
```
* **User Prompt**:
```text
Process the following book upload content and student learning preferences to build the core curriculum processing matrix.
Student Level: ${readingLevel}
Preferred Style: ${teachingStyle}
Book Raw Structure Sample: ${rawTextSample}
```
* **Structured JSON Output Schema**:
```json
{
  "type": "OBJECT",
  "properties": {
    "bookId": { "type": "STRING" },
    "pipelineSteps": {
      "type": "ARRAY",
      "items": {
        "type": "OBJECT",
        "properties": {
          "stepIndex": { "type": "INTEGER" },
          "agentName": { "type": "STRING" },
          "targetChapterIndex": { "type": "INTEGER" },
          "status": { "type": "STRING" }
        }
      }
    }
  },
  "required": ["bookId", "pipelineSteps"]
}
```
* **Validation Rules**: Must contain at least one step for each detected chapter index.
* **Cost Optimization**: Cache global book outlines in memory; do not pass the entire full-text back to the orchestrator once chunk boundaries are set.

---

### 3.2 Book Analysis Agent
* **Purpose**: Evaluate the overall subject matter, target audience, technical difficulty, and central thesis.
* **Responsibilities**: Define the pedagogical scope, prerequisite requirements, and baseline vocabulary map.
* **Model Designation**: `gemini-3.1-pro-preview`
* **Inputs**: Dynamic raw text samples from the introduction, preface, and selected body sections.
* **Outputs**: Full conceptual index catalog, difficulty ranking, core topic tags.
* **System Prompt**:
```text
You are an expert Academic Reviewer. Assess the general book contents, identify the targeted technical difficulty level, outline required prerequisite skills, and list core concept namespaces.
```
* **User Prompt**:
```text
Review this book overview segment and extract the global context profile:
Raw text sample: ${bookIntroText}
```
* **Structured JSON Output Schema**:
```json
{
  "type": "OBJECT",
  "properties": {
    "difficultyLevel": { "type": "STRING" },
    "prerequisites": { "type": "ARRAY", "items": { "type": "STRING" } },
    "coreTopics": { "type": "ARRAY", "items": { "type": "STRING" } }
  },
  "required": ["difficultyLevel", "prerequisites", "coreTopics"]
}
```

---

### 3.3 Chapter Detection Agent
* **Purpose**: Segment raw parsed book text into individual semantic chapters.
* **Responsibilities**: Identify start and end character positions, chapter indices, titles, and structural metadata.
* **Model Designation**: `gemini-3.5-flash` (High processing volume)
* **Inputs**: Raw parsed text streams.
* **Outputs**: Array of chapter index objects.
* **System Prompt**:
```text
You are an expert Document Parsing Engineer. Identify and partition raw book text streams into clear, clean semantic chapters based on heading indexes, page intervals, and logical section breaks.
```
* **Structured JSON Output Schema**:
```json
{
  "type": "OBJECT",
  "properties": {
    "detectedChapters": {
      "type": "ARRAY",
      "items": {
        "type": "OBJECT",
        "properties": {
          "index": { "type": "INTEGER" },
          "title": { "type": "STRING" },
          "summaryOverview": { "type": "STRING" },
          "approximateStartOffset": { "type": "INTEGER" },
          "approximateEndOffset": { "type": "INTEGER" }
        }
      }
    }
  },
  "required": ["detectedChapters"]
}
```

---

### 3.4 Concept Extraction Agent
* **Purpose**: Isolate core scientific, mathematical, or literary concepts from a single chapter.
* **Responsibilities**: Map relationships, define core theoretical foundations, and generate initial context prompts.
* **Model Designation**: `gemini-3.5-flash`
* **Inputs**: Cleaned chapter content segment.
* **Outputs**: Array of concept nodes matching the Database Schema.
* **System Prompt**:
```text
You are a Cognitive Science Extraction Agent. Extract high-density core concepts from the chapter segment, mapping their core theoretical rules, analogies, and self-check challenges.
```
* **Structured JSON Output Schema**:
```json
{
  "type": "OBJECT",
  "properties": {
    "concepts": {
      "type": "ARRAY",
      "items": {
        "type": "OBJECT",
        "properties": {
          "conceptId": { "type": "STRING" },
          "name": { "type": "STRING" },
          "description": { "type": "STRING" },
          "analogy": { "type": "STRING" },
          "visualIllustration": { "type": "STRING" },
          "selfCheck": { "type": "STRING" }
        }
      }
    }
  },
  "required": ["concepts"]
}
```

---

### 3.5 Curriculum Planning Agent
* **Purpose**: Align the book's chapter progression with the student's background knowledge.
* **Responsibilities**: Build a custom pedagogical syllabus path, dynamically adding structural bridging nodes for challenging areas.
* **Model Designation**: `gemini-3.1-pro-preview`
* **Inputs**: User's learning preferences profile, book analysis concepts catalog.
* **Outputs**: Tailored curriculum map with custom study milestones.
* **System Prompt**:
```text
You are a Principal Curriculum Architect. Design a customized milestone learning path by matching the user's specific background preferences with the extracted book index catalog.
```
* **Structured JSON Output Schema**:
```json
{
  "type": "OBJECT",
  "properties": {
    "personalizedMilestones": {
      "type": "ARRAY",
      "items": {
        "type": "OBJECT",
        "properties": {
          "milestoneId": { "type": "STRING" },
          "targetTopic": { "type": "STRING" },
          "associatedChapterIndices": { "type": "ARRAY", "items": { "type": "INTEGER" } },
          "customPedagogicalFocus": { "type": "STRING" }
        }
      }
    }
  },
  "required": ["personalizedMilestones"]
}
```

---

### 3.6 Master Teacher Agent
* **Purpose**: Deliver core academic explanations of extracted concepts.
* **Responsibilities**: Answer open-ended questions using custom instructional styles (e.g., Socratic, Analogical, Case-Study).
* **Model Designation**: `gemini-3.1-pro-preview`
* **Inputs**: Selected concept, student background profile, current chapter context.
* **Outputs**: Tailored, deep educational explanations.
* **System Prompt**:
```text
You are the World's Best Academic Teacher. Explain the target educational concept clearly, using the student's selected teaching style, maintaining high conceptual rigor while making it intuitive.
```

---

### 3.7 Educational Writer Agent
* **Purpose**: Format drafted explanations into highly polished textbook prose.
* **Responsibilities**: Refine text structure, maintain elegant styling, structure content with Markdown, and format key callout sections.
* **Model Designation**: `gemini-3.5-flash`
* **Inputs**: Draft teacher explanations.
* **Outputs**: Clean Markdown text with structured headings.

---

### 3.8 Visual Planning Agent
* **Purpose**: Plan where to insert diagrams, illustrations, or infographics in chapter texts.
* **Responsibilities**: Define visual specifications, determine layout styles, and output generator prompt blueprints.
* **Model Designation**: `gemini-3.1-pro-preview`
* **Inputs**: Complete polished chapter text.
* **Outputs**: Structured layout guide specifying visual asset anchor coordinates.

---

### 3.9 Diagram & Infographic Planner
* **Purpose**: Generate vector code representations (SVG, Mermaid.js) for technical flowcharts and mind-maps.
* **Responsibilities**: Output valid XML layouts or structural script parameters.
* **Model Designation**: `gemini-3.5-flash`
* **Inputs**: Concept metadata, relational outline.
* **Outputs**: SVG string code payload.

---

### 3.10 Quiz Generator
* **Purpose**: Generate high-fidelity interactive multiple-choice quizzes for students.
* **Responsibilities**: Create accurate questions, design plausible distractor choices, and draft detailed rationales.
* **Model Designation**: `gemini-3.5-flash`
* **Inputs**: Chapter concept descriptions.
* **Outputs**: Quiz data structures matching the Database Schema.
* **System Prompt**:
```text
You are an expert Evaluation Architect. Generate interactive multiple-choice questions matching the provided chapter concepts. Ensure distractor options are realistic, and write logical, comprehensive correct-choice explanations.
```
* **Structured JSON Output Schema**:
```json
{
  "type": "OBJECT",
  "properties": {
    "quizItems": {
      "type": "ARRAY",
      "items": {
        "type": "OBJECT",
        "properties": {
          "questionText": { "type": "STRING" },
          "options": { "type": "ARRAY", "items": { "type": "STRING" } },
          "correctOptionIndex": { "type": "INTEGER" },
          "explanation": { "type": "STRING" }
        }
      }
    }
  },
  "required": ["quizItems"]
}
```

---

### 3.11 Flashcard Generator
* **Purpose**: Synthesize active-recall flashcard packages.
* **Responsibilities**: Create balanced question-answer pairings utilizing spaced repetition rules.
* **Model Designation**: `gemini-3.5-flash`
* **Inputs**: Core concept extraction map.
* **Outputs**: Array of flashcard cards.

---

### 3.12 Revision Planner
* **Purpose**: Build custom calendar revision maps.
* **Responsibilities**: Design custom repetition intervals based on student quiz performance profiles.
* **Model Designation**: `gemini-3.1-pro-preview`
* **Inputs**: Quiz performance logs, milestone progression data.
* **Outputs**: Structured date schedule map.

---

### 3.13 Memory Techniques Agent
* **Purpose**: Enhance recall pathways by drafting mnemonics, loci-map coordinates, or narrative stories.
* **Responsibilities**: Deliver creative learning hacks.
* **Model Designation**: `gemini-3.5-flash`
* **Inputs**: Highly abstract formulas or definitions.
* **Outputs**: Mnemonic schemas.

---

### 3.14 AI Tutor Agent
* **Purpose**: Host real-time conversational study sessions with the student.
* **Responsibilities**: Answer follow-up questions, guide discussions Socratically, and maintain conversational context limits.
* **Model Designation**: `gemini-3.1-pro-preview` (Highly robust reasoning and contextualization)
* **Inputs**: Conversation transcript history, book baseline knowledge nodes.
* **Outputs**: Conversational responses and recommended next-step study questions.

---

### 3.15 Knowledge Graph Builder
* **Purpose**: Convert raw concepts and cross-chapter dependencies into a structured node-and-edge mathematical network.
* **Responsibilities**: Generate nodes, trace edges, assign relational coefficients, and update coordinates.
* **Model Designation**: `gemini-3.1-pro-preview`
* **Inputs**: Complete multi-chapter concept indices.
* **Outputs**: Edge relational network.
* **Structured JSON Output Schema**:
```json
{
  "type": "OBJECT",
  "properties": {
    "nodes": {
      "type": "ARRAY",
      "items": {
        "type": "OBJECT",
        "properties": {
          "id": { "type": "STRING" },
          "label": { "type": "STRING" },
          "category": { "type": "STRING" }
        }
      }
    },
    "edges": {
      "type": "ARRAY",
      "items": {
        "type": "OBJECT",
        "properties": {
          "sourceId": { "type": "STRING" },
          "targetId": { "type": "STRING" },
          "relationshipType": { "type": "STRING" },
          "weight": { "type": "NUMBER" }
        }
      }
    }
  },
  "required": ["nodes", "edges"]
}
```

---

### 3.16 Recommendation Engine
* **Purpose**: Suggest supplementary readings and personalized focus areas based on learning performance.
* **Responsibilities**: Recommend targeted sub-topics to address student weak points.
* **Model Designation**: `gemini-3.5-flash`
* **Inputs**: Progress tracking statistics, quiz answers history.
* **Outputs**: Array of study recommendations.

---

### 3.17 Fact Checking & Consistency Agent
* **Purpose**: Guarantee exact accuracy, validating AI-generated content against raw source files.
* **Responsibilities**: Block hallucinations, correct factual errors in drafts, and flag potential semantic conflicts.
* **Model Designation**: `gemini-3.1-pro-preview` (Critical reasoning and accuracy verification)
* **Inputs**: Raw parsed original text, draft explanations or quizzes.
* **Outputs**: Validation results, along with proposed edits for any flagged errors.
* **System Prompt**:
```text
You are the Lead Fact Checking Specialist. Compare the draft generated educational explanations with the raw original book source. Flag any factual inconsistencies, mathematical errors, or historical hallucinations, returning precise remediation edits.
```

---

### 3.18 Quality Review Agent
* **Purpose**: Enforce clean prose style, Material 3 visual standards, and consistent design layouts.
* **Responsibilities**: Verify typography choices, review visual flow setups, and evaluate grammar and styling.
* **Model Designation**: `gemini-3.5-flash`
* **Inputs**: Fully compiled chapters.
* **Outputs**: Quality pass status along with layout polish logs.

---

### 3.19 Export Preparation Agent
* **Purpose**: Compile structured book content, diagrams, and formatting styles into standard export payloads.
* **Responsibilities**: Adapt document styles, structure chapters, and output standard EPUB/PDF compile schemas.
* **Model Designation**: `gemini-3.5-flash`
* **Inputs**: Validated chapters and graphic arrays.
* **Outputs**: CSS layouts and section structural indexes.

---

### 3.20 Final Assembly Agent
* **Purpose**: Complete the pipeline, validating the final generated learning portfolio.
* **Responsibilities**: Ensure all structural checks pass, verify cryptographic signature assets, and mark jobs as complete in Firestore.
* **Model Designation**: `gemini-3.1-pro-preview`
* **Inputs**: Complete pipeline checklists.
* **Outputs**: Success confirmation record and status update payload.

---

## 4. Performance & Cost Optimization Playbook

### 4.1 Token Budget Planning
To manage processing costs effectively, each book generation task is run within strict token budget limits:

| Generation Task | Model Used | Approx. Input Tokens | Approx. Output Tokens | Input Cost (per 1M) | Output Cost (per 1M) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Chapter Detection** | `gemini-3.5-flash` | 80,000 | 2,000 | \$0.075 | \$0.30 |
| **Concept Extraction**| `gemini-3.5-flash` | 25,000 | 4,000 | \$0.075 | \$0.30 |
| **Deep Explanation** | `gemini-3.1-pro-preview`| 35,000 | 12,000 | \$1.25 | \$5.00 |
| **Quiz Generation** | `gemini-3.5-flash` | 15,000 | 3,000 | \$0.075 | \$0.30 |
| **Fact Checking** | `gemini-3.1-pro-preview`| 45,000 | 4,000 | \$1.25 | \$5.00 |

### 4.2 Hallucination Reduction Protocols
1. **Direct Context Grounding (RAG)**: System prompts explicitly forbid external reasoning outside of the attached original source text contents block.
2. **Double-Pass Critique Logic**: Draft outputs from the `Master Teacher Agent` are routed through the `Fact Checking & Consistency Agent` before they are written to Firestore. If the factual similarity score drops below 0.98, the content is rejected and regenerated automatically.
3. **Structured Schema Enforcements**: Forcing structured JSON schema structures prevents models from generating unstructured text, lowering the rate of structural format errors to less than 0.2% in production.
