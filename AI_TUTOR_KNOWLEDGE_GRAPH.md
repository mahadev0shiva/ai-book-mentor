# AI Book Study Companion — AI Tutor & Knowledge Graph Engine
## Production-Grade Personalized Learning, Memory Architecture, and Graph Grounding

---

## 1. High-Level AI Tutor & Knowledge Graph Architecture

The AI Tutor and Knowledge Graph Engine provides a personalized, long-term learning experience. It combines a structured semantic graph of conceptual book nodes with an adaptive memory engine to tailor the AI's guidance to each student's current mastery level.

### 1.1 Architectural Design (ASCII Diagram)

```text
[ Student Natural Language Input ]
                 |
                 v
       +-------------------+
       | Intent Detection  | <--- Evaluates Q&A, Testing, or Exploration Modes
       +-------------------+
                 |
                 v
       +-------------------+
       | Context Retrieval | <--- Fetches Local Vector Embeddings & Graph Nodes
       +-------------------+
                 |
                 +-----------------------+-----------------------+
                 |                                               |
                 v                                               v
       +-------------------+                           +-------------------+
       |   Graph Engine    |                           |   Memory Engine   |
       +-------------------+                           +-------------------+
         - Fetches related concepts                      - Short-term context window
         - Follows "depends_on" edges                    - Long-term mastery stats
         - Identifies cross-book links                   - Tracks mistake patterns
                 |                                               |
                 +-----------------------+-----------------------+
                                         |
                                         v
                               +-------------------+
                               | Gemini Reasoning  | <--- Grounded Context Prompt
                               +-------------------+
                                         |
                                         v
                               +-------------------+
                               | Fact Checking &   | <--- Verifies Answer Integrity
                               | Consistency Gate  |
                               +-------------------+
                                         |
                                         v
                               +-------------------+
                               | Adaptive Response | ---> Updates Student Mastery
                               +-------------------+
```

### 1.2 Data Flow Control Loop

```text
Student        Tutor UI        Intent Detector     Graph Database     Gemini 3.5 API
   |              |                   |                  |                   |
   |-- Query ---->|                   |                  |                   |
   |              |-- Detect Intent ->|                  |                   |
   |              |                   |-- Query Graph ->|                   |
   |              |                   |<-- Nodes/Edges --|                   |
   |              |                                      |                   |
   |              |-- Retrieve Context & Memory ---------------------------->|
   |              |<-- Synthesized Grounded Response -------------------------|
   |              |                                                          |
   |              |-- Render Response & Update Mastery Metrics -------------->|
   |<-- Output ---|
```

---

## 2. Knowledge Graph Specification

The Knowledge Graph is represented as a network of nodes and directional edges, stored in Firestore under `/knowledge_graphs/{graphId}/nodes` and `/knowledge_graphs/{graphId}/edges`. This network is mirrored in the local client Room database for offline learning.

### 2.1 Node Entity Schema

```json
{
  "type": "OBJECT",
  "properties": {
    "nodeId": { "type": "STRING" },
    "label": { "type": "STRING" },
    "category": { "type": "STRING", "enum": ["CONCEPT", "CHAPTER", "BOOK", "FLASHCARD", "MISTAKE"] },
    "metadata": {
      "type": "OBJECT",
      "properties": {
        "associatedBookId": { "type": "STRING" },
        "difficultyScore": { "type": "NUMBER" },
        "conceptSummary": { "type": "STRING" }
      },
      "required": ["associatedBookId", "difficultyScore"]
    }
  },
  "required": ["nodeId", "label", "category", "metadata"]
}
```

### 2.2 Edge Relational Schema

```json
{
  "type": "OBJECT",
  "properties": {
    "edgeId": { "type": "STRING" },
    "sourceNodeId": { "type": "STRING" },
    "targetNodeId": { "type": "STRING" },
    "relationshipType": { "type": "STRING", "enum": ["PREREQUISITE_OF", "EXPANDS_ON", "RELATED_TO", "CONTRADICTS"] },
    "weight": { "type": "NUMBER" }
  },
  "required": ["edgeId", "sourceNodeId", "targetNodeId", "relationshipType", "weight"]
}
```

---

## 3. Memory & Personalization System

The memory engine tracks student comprehension across three main layers: Short-term Session Context, Long-term Historical Mastery, and Spaced Repetition logs.

### 3.1 Personalization Mastery Algorithm (Bayesian Knowledge Tracing)
The system calculates a student's probability of mastering a concept, $P(L_n)$, after answering an evaluation quiz question $n$:

$$P(L_n) = P(L_{n-1} \mid \text{Response}) + (1 - P(L_{n-1} \mid \text{Response})) \times P(T)$$

Where $P(T)$ is the transition probability of moving from an unlearned state to a learned state, and the conditional probability based on response accuracy is:

* **For Correct Responses**:
$$P(L_{n-1} \mid \text{Correct}) = \frac{P(L_{n-1}) \times (1 - P(S))}{P(L_{n-1}) \times (1 - P(S)) + (1 - P(L_{n-1})) \times P(G)}$$

* **For Incorrect Responses**:
$$P(L_{n-1} \mid \text{Incorrect}) = \frac{P(L_{n-1}) \times P(S)}{P(L_{n-1}) \times P(S) + (1 - P(L_{n-1})) \times (1 - P(G))}$$

Where parameters are initialized as:
- **Slip Probability ($P(S)$)**: $0.1$ (Accidentally getting a known concept wrong)
- **Guess Probability ($P(G)$)**: $0.2$ (Correctly guessing an unknown concept)
- **Transition Rate ($P(T)$)**: $0.15$

---

## 4. AI Tutor Workflows & Grounded Prompt Library

### 4.1 Conversational Q&A Grounded Query Agent
* **Purpose**: Provide accurate, context-grounded answers to student queries using the compiled Knowledge Graph.
* **Model**: `gemini-3.1-pro-preview` (Highly robust logical reasoning and context mapping)
* **Inputs**: Student Query, Retrieve Concept Nodes, Student Mastery History, Related Book Snippets.
* **System Prompt**:
```text
You are the Lead AI Academic Tutor. Your goal is to help the student master their textbooks.
You MUST follow these strict guidelines:
1. Base your explanations entirely on the verified text snippets from the attached book. Do not introduce outside academic facts.
2. If the user's query cannot be answered using the provided context, state: "I cannot find this information in your library." Do not guess or hallucinate.
3. Match your technical terminology and pacing to the student's current mastery level.
```
* **User Prompt**:
```text
Student Profile Mastery: ${masteryLevel}
Retrieve Concept Snippets: ${conceptContext}
Student Question: ${studentQuestion}
```

---

### 4.2 Socratic Diagnostic Assessment Agent
* **Purpose**: Evaluate a student's conceptual understanding using Socratic, progressive questioning rather than direct multiple-choice tests.
* **Model**: `gemini-3.1-pro-preview`
* **System Prompt**:
```text
You are a Socratic Academic Evaluator. Do not give direct explanations. Instead, ask the student a single, clear, conceptual question that tests their understanding of the target concept.
Review the student's response, evaluate their comprehension, and ask a follow-up question that guides them toward a complete understanding of the topic.
```
* **User Prompt**:
```text
Target Concept to Diagnostic Test: ${conceptName}
Definition Context: ${conceptDescription}
Current Conversation History: ${dialogueHistory}
```

---

## 5. Performance, Security & Cost Optimization Checklists

- [x] **Graph Decoupling for Fast Client Queries**: Store complex node relationships in flat Firestore collections, allowing the client to query specific conceptual sub-graphs quickly.
- [x] **Short-Term Context Truncation**: Maintain active chat transcripts within a sliding window of $10$ messages. Compress earlier logs into conversational summaries to optimize token usage.
- [x] **Cosine Similarity Threshold Checks**: Filter retrieved vector embeddings against a $0.82$ similarity threshold, ignoring irrelevant matches to minimize API payload size.
- [x] **Offline Graph Synchronization**: Mirror critical concept maps and prerequisite links locally in the student's Room database, allowing offline visual study and flashcard review.
