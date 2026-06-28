# AI Book Study Companion — Universal AI Content Generation Engine
## Production-Grade Specifications for Multi-Style Explanations, Graphic Planning, and Assessment Generation

---

## 1. High-Level Content Generation Architecture

The Universal AI Content Generation Engine converts structured book knowledge (chapters, sections, and concept nodes) into a comprehensive learning experience. It balances cognitive theory with Material Design 3 guidelines and cost-efficient API usage.

### 1.1 Architectural Design (ASCII Diagram)

```text
[ Core Concept Nodes (from Ingestion Pipeline) ]
                       |
                       v
       +-------------------------------+
       |   Content Generation Engine   | <--- Inputs: Student Profile, Domain Theme
       +-------------------------------+
                       |
       +---------------+---------------+---------------+
       |                               |               |
       v                               v               v
+--------------+               +--------------+ +--------------+
| Module 1:    |               | Module 2:    | | Module 3:    |
| Pedagogical  |               | Interactive  | | Educational  |
| Explanations |               | Assessments  | | Visualization|
+--------------+               +--------------+ +--------------+
  - Socratic                     - MCQ Quizzes    - SVG Diagrams
  - Case-Study                   - Flashcards     - Mind Maps
  - Analogies                    - Rubrics        - Timelines
       |                               |               |
       +---------------+---------------+---------------+
                       |
                       v
       +-------------------------------+
       |     Stage 4: Validation       | <--- Fact Checking & Schema Integrity Gate
       +-------------------------------+
                       |
                       v
       +-------------------------------+
       |  Stage 5: Local Database Sync | <--- Room / Firestore Multi-Tier Storage
       +-------------------------------+
```

---

## 2. Explanation Generators & Prompt Library

### 2.1 Masterclass Deep Explanation Generator
* **Purpose**: Provide high-density, rigorous academic explanations of complex core concepts.
* **Responsibilities**: Explain concepts step-by-step, connect ideas across chapters, and include real-world applications.
* **Model**: `gemini-3.1-pro-preview` (Highly robust reasoning and complex technical synthesis)
* **Inputs**: Target Concept Object, Chapter Summary context, Student Level.
* **System Prompt**:
```text
You are a Distinguished Professor. Explain the target educational concept with absolute academic rigor, following a structured pedagogical layout:
1. Core Definition: Precise mathematical, scientific, or logical statement.
2. Conceptual Foundation: Break down the underlying rules step-by-step.
3. Real-World Case Study: Illustrate the concept in action within an industry or historical scenario.
4. Active Reflection Query: Pose a challenging question that requires critical thinking.
Keep explanations highly clear, accessible, and structured with clean Markdown formatting.
```
* **User Prompt**:
```text
Generate a Masterclass explanation for:
Concept: ${conceptName}
Context Description: ${conceptDescription}
Reading Level: ${readingLevel}
```
* **Structured JSON Output Schema**:
```json
{
  "type": "OBJECT",
  "properties": {
    "conceptName": { "type": "STRING" },
    "coreDefinition": { "type": "STRING" },
    "foundationalBreakdown": { "type": "ARRAY", "items": { "type": "STRING" } },
    "caseStudy": {
      "type": "OBJECT",
      "properties": {
        "title": { "type": "STRING" },
        "scenario": { "type": "STRING" },
        "solution": { "type": "STRING" }
      },
      "required": ["title", "scenario", "solution"]
    },
    "reflectionQuery": { "type": "STRING" }
  },
  "required": ["conceptName", "coreDefinition", "foundationalBreakdown", "caseStudy", "reflectionQuery"]
}
```

---

### 2.2 Socratic Tutor Explainer
* **Purpose**: Prompt intellectual discovery by guiding students to conclusions using targeted, progressive questioning.
* **Model**: `gemini-3.1-pro-preview`
* **System Prompt**:
```text
You are Socrates, the famous ancient philosopher. You teach not by lecture, but through questioning. Guide the student to discover the target concept on their own. Break down the concept into three logical steps, and write a leading question for each step that guides the student to the correct conclusion.
```
* **Structured JSON Output Schema**:
```json
{
  "type": "OBJECT",
  "properties": {
    "conceptName": { "type": "STRING" },
    "socraticDialogue": {
      "type": "ARRAY",
      "items": {
        "type": "OBJECT",
        "properties": {
          "stepIndex": { "type": "INTEGER" },
          "guidingPrinciple": { "type": "STRING" },
          "leadingQuestion": { "type": "STRING" },
          "hints": { "type": "ARRAY", "items": { "type": "STRING" } }
        },
        "required": ["stepIndex", "guidingPrinciple", "leadingQuestion", "hints"]
      }
    }
  },
  "required": ["conceptName", "socraticDialogue"]
}
```

---

### 2.3 Analogy explainer
* **Purpose**: Translate highly abstract topics into intuitive, everyday real-world experiences.
* **Model**: `gemini-3.5-flash`
* **System Prompt**:
```text
You are an expert communicator. Your task is to explain complex academic concepts using simple, everyday analogies. Avoid jargon, and make the comparison intuitive and memorable.
```
* **User Prompt**:
```text
Explain this concept using a clear analogy:
Concept: ${conceptName}
Definition: ${conceptDescription}
```

---

## 3. Visual Planning & Layout Architecture

To support learning styles that benefit from visual representations, the pipeline plans and drafts specifications for visual aids, inserting them directly into the study materials.

### 3.1 Interactive Diagram Planning Specification

Every generated visual asset must contain:
- **Purpose**: Clear educational goal.
- **Prompt**: Standard prompt structure for the visual generator (SVG, Mermaid.js, or Imagen).
- **Placement**: Anchor point relative to chapter sections.
- **Caption**: Short description of the graphic.
- **Alt Text**: Detailed accessibility description for screen readers.

### 3.2 Diagram Generator (SVG-Native Vector Layouts)
* **Purpose**: Generate scalable, high-fidelity vector flowcharts or technical diagrams dynamically.
* **Model**: `gemini-3.5-flash`
* **Inputs**: Target Concept, Flow Relationship Data.
* **Outputs**: Valid inline SVG XML.
* **Constraint Checklist**:
  - [x] Must be fully responsive (uses `viewBox` instead of hardcoded `width` and `height` coordinates).
  - [x] Follows Material Design 3 color schemes (uses primary, secondary, and surface-variant values).
  - [x] Touch targets for interactive hotkeys must be at least $48\text{dp} \times 48\text{dp}$.

---

## 4. Evaluation & Assessment Generation Engine

### 4.1 Multiple-Choice Quiz (MCQ) Generator
* **Purpose**: Generate high-fidelity quizzes to evaluate conceptual understanding.
* **Model**: `gemini-3.5-flash`
* **System Prompt**:
```text
You are an expert Evaluator. Your task is to write challenging, pedagogically sound multiple-choice questions for the target concept.
Ensure distractor options are plausible misconceptions rather than obviously wrong choices. Provide a comprehensive explanation detailing why the correct answer is right and why the other options are incorrect.
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
          "correctIndex": { "type": "INTEGER" },
          "rationales": { "type": "ARRAY", "items": { "type": "STRING" } }
        },
        "required": ["questionText", "options", "correctIndex", "rationales"]
      }
    }
  },
  "required": ["quizItems"]
}
```

---

### 4.2 Spaced Repetition Flashcard Engine
* **Purpose**: Synthesize balanced question-answer pairings designed for long-term active recall.
* **Model**: `gemini-3.5-flash`
* **JSON Schema**:
```json
{
  "type": "OBJECT",
  "properties": {
    "flashcards": {
      "type": "ARRAY",
      "items": {
        "type": "OBJECT",
        "properties": {
          "front": { "type": "STRING" },
          "back": { "type": "STRING" },
          "clue": { "type": "STRING" }
        },
        "required": ["front", "back", "clue"]
      }
    }
  },
  "required": ["flashcards"]
}
```

---

## 5. Spaced Repetition Scheduling Algorithm (SuperMemo SM-2)

Study intervals are scheduled using the SuperMemo-2 (SM-2) algorithm, tracking student response scores ($q$) from $0 \rightarrow 5$:

$$I(1) := 1$$
$$I(2) := 6$$
$$I(n) := I(n-1) \times EF \quad \text{for } n > 2$$

Where the Easiness Factor ($EF$) is updated dynamically:

$$EF' := EF + (0.1 - (5 - q) \times (0.08 + (5 - q) \times 0.02))$$

If $q < 3$, reset the repetition interval to $1$ day to reinforce retention.

---

## 6. Content Quality Rubric & Validation Gates

To guarantee accuracy and clarity, all AI-generated content must pass rigorous automated quality gates:

```text
                  [ Generated Content Draft ]
                               |
                               v
         [ Gate 1: JSON Schema Integrity Check ]
                               |
            /------------------+------------------\
           /                                       \
     [Passes Schema]                        [Fails Schema]
           |                                       |
           v                                       v
[ Gate 2: Fact-Check Engine ]             [ Trigger Regenerate ]
  - Compare with raw chapter sources                 - Attempt up to 3 times
  - Minimum overlap score: 98%
           |
            \------------------+------------------/
                               |
                        [Passes Both]
                               |
                               v
                  [ Write to Firestore DB ]
```
