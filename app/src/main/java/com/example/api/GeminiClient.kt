package com.example.api

import com.example.BuildConfig
import com.example.data.Chapter
import com.example.data.Flashcard
import com.example.data.QuizQuestion
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

// --- Request/Response Schemas for Gemini API ---

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

// --- Retrofit API Service Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    /**
     * Gemini Multi-Agent Prompt Library
     * Each AI agent has a single responsibility and is built upon the core rule:
     * "Teach for understanding, not just information."
     */
    object AgentLibrary {
        // 1. Master Orchestrator Agent
        const val MASTER_ORCHESTRATOR_ROLE = """
            [Master Orchestrator Agent]
            Role: Receives the user's request, plans the academic curation workflow, coordinates specialized educational agents, and formats the output into a premium unified learning package.
            Rule: Coordinate logical sequence, ensure consistency of learning level across all sub-agents, and monitor the overall pedagogical flow.
        """

        // 2. Book Analysis Agent
        const val BOOK_ANALYSIS_ROLE = """
            [Book Analysis Agent]
            Role: Analyzes source material text or topic to identify structural hierarchy, recurring key themes, core complex concepts, and required prerequisite knowledge.
            Rule: Extract exact conceptual terms and structures, highlighting structural prerequisites needed for a clean onboarding experience.
        """

        // 3. Curriculum Designer Agent
        const val CURRICULUM_DESIGNER_ROLE = """
            [Curriculum Designer Agent]
            Role: Converts academic concepts and structures identified by the Analysis Agent into a highly structured, progressive 3-stage learning pathway (chapters).
            Rule: Group topics logically, ensure a smooth learning curve from introductory foundations to advanced applications.
        """

        // 4. Master Teacher Agent
        const val MASTER_TEACHER_ROLE = """
            [Master Teacher Agent]
            Role: Explains core terms and abstract theories like an exceptional, highly supportive teacher.
            Rule: Teach for understanding, not just information. Use clear, original, and intuitive analogies, real-world examples, and friendly pedagogical dialogue appropriate to the reader's chosen level.
        """

        // 5. Deep Explanation Agent
        const val DEEP_EXPLANATION_ROLE = """
            [Deep Explanation Agent]
            Role: Produces comprehensive, original, and detailed educational explanations of the core themes, concepts, and relationships without verbatim reproduction of external source materials.
            Rule: Deep dive into the mechanics ("how" and "why"), not just the "what". Provide rich conceptual contexts.
        """

        // 6. Visual Learning Agent
        const val VISUAL_LEARNING_ROLE = """
            [Visual Learning Agent]
            Role: Identifies critical junctions in the text where a diagram, flowchart, timeline, map, or infographic would dramatically improve concept retention.
            Rule: Formulate precise descriptions and image generation prompts for those visual concepts, embedding them clearly into the study text.
        """

        // 7. Memory Science Agent
        const val MEMORY_SCIENCE_ROLE = """
            [Memory Science Agent]
            Role: Uses cognitive science (active recall, spaced repetition, mnemonics) to design high-yield study flashcards, memory palaces, and key cues.
            Rule: Formulate concise question-and-answer pairs that target core relational and conceptual links rather than trivial facts.
        """

        // 8. Assessment Agent
        const val ASSESSMENT_ROLE = """
            [Assessment Agent]
            Role: Designs comprehensive, non-trivial, diagnostic multiple-choice quizzes and exercises to test structural understanding and active application.
            Rule: Create realistic scenario questions, clear distractor choices, and exceptionally supportive explanations for each option.
        """

        // 9. Fact-Checking & Quality Agent
        const val FACT_CHECKING_ROLE = """
            [Fact-Checking & Quality Agent]
            Role: Reviews all compiled educational text for clarity, pedagogical consistency, academic accuracy, and ensures there are no false or unsupported claims.
            Rule: Enforce absolute truth, flag or revise any ambiguous or uncertain claims, and verify that all concepts are explained accurately.
        """

        // 10. Ebook Designer Agent
        const val EBOOK_DESIGNER_ROLE = """
            [Ebook Designer Agent]
            Role: Styles and formats the generated educational content using premium editorial typography, structured headings, bulleted lists, information tables, and highlighted key boxes (callouts).
            Rule: Elevate the reading experience visually using styled Markdown structures, quote boxes, and distinct logical dividers.
        """

        // 11. AI Tutor Agent
        const val AI_TUTOR_ROLE = """
            [AI Tutor Agent]
            Role: Engages in a helpful, warm, Socratic follow-up dialogue with the student, checking understanding, and tailoring responses to the student's unique pace and questions.
            Rule: Avoid feeding direct answers; use guidance, follow-up questions, and real-world analogies to guide the student toward independent realization.
        """

        // 12. Export Agent
        const val EXPORT_AGENT_ROLE = """
            [Export Agent]
            Role: Prepares and compiles the curated modules, flashcards, and study progress into beautifully formatted printable/exportable Markdown layouts.
            Rule: Organize all sections into a comprehensive, high-quality, professional educational portfolio.
        """
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "MY_GEMINI_API_KEY" || key.isEmpty()) {
            // Return empty or placeholder so we can handle gracefully
            ""
        } else {
            key
        }
    }

    /**
     * General content generation
     */
    suspend fun generateText(prompt: String, systemPrompt: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext "Error: API Key is missing. Please set your GEMINI_API_KEY in the Secrets panel."
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemPrompt?.let { Content(parts = listOf(Part(text = it))) }
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from AI."
        } catch (e: Exception) {
            "Error calling Gemini: ${e.localizedMessage}"
        }
    }

    /**
     * 1. Generate Custom Book Study Syllabus (Chapters)
     * Coordinated by Master Orchestrator, Book Analysis, Curriculum Designer, Master Teacher, Deep Explanation, Visual Learning, Fact-Checking, and Ebook Designer Agents.
     */
    suspend fun generateBookSyllabus(
        title: String,
        author: String,
        category: String,
        content: String = "",
        depth: String = "Intermediate",
        readLevel: String = "Undergraduate",
        teachingStyle: String = "Socratic Method",
        imgDensity: String = "Standard"
    ): List<SyllabusChapterDto> = withContext(Dispatchers.IO) {
        val prompt = """
            ${AgentLibrary.MASTER_ORCHESTRATOR_ROLE}
            ${AgentLibrary.BOOK_ANALYSIS_ROLE}
            ${AgentLibrary.CURRICULUM_DESIGNER_ROLE}
            ${AgentLibrary.MASTER_TEACHER_ROLE}
            ${AgentLibrary.DEEP_EXPLANATION_ROLE}
            ${AgentLibrary.VISUAL_LEARNING_ROLE}
            ${AgentLibrary.FACT_CHECKING_ROLE}
            ${AgentLibrary.EBOOK_DESIGNER_ROLE}

            The Master Orchestrator coordinates the Book Analysis, Curriculum Designer, Master Teacher, Deep Explanation, Visual Learning, Fact-Checking, and Ebook Designer Agents to construct a highly informative study syllabus for the book/topic: "$title" by "$author" (Category: $category).
            
            COORDINATION GUIDELINES:
            - Target Reading Level: $readLevel
            - Target Depth: $depth
            - Teaching Style: $teachingStyle
            - Visual Illustration Density: $imgDensity
            
            Source Material / Context:
            $content

            CORE RULE:
            Teach for understanding, not just information.
            
            Divide the syllabus into exactly 3 chapters representing the core stages of learning this book or subject.
            For each chapter, provide:
            1. 'title': A descriptive chapter title.
            2. 'summary': A comprehensive, beautifully formatted educational summary using teaching style "$teachingStyle" explaining the core thesis, insights, and lessons (approx 150-200 words). Include Markdown styled sections or bullet points where useful.
            3. 'coreConcepts': Detailed Markdown formatted text explaining the core terms and concepts of this chapter, with real-world examples, analogies, and a specific conceptual visual description (with image generation prompt from Visual Learning Agent).
               Format each concept nicely as:
               • **Concept Name**: Description.
               • **Analogy**: Real-world analogy.
               • **[Visual Illustration]**: (Prompt and description for image generation of the concept diagram)
               • **Self-Check**: A quick self-check question at the end for the learner.

            Return your output as a strict JSON array of objects.
            Format:
            [
              {
                "title": "Chapter Title",
                "summary": "Detailed chapter summary text...",
                "coreConcepts": "• Concept 1: Description\n• Analogy: Analogy...\n• [Visual Illustration]: Prompt...\n• Self-Check: Question..."
              },
              ...
            ]
            Do not include markdown wrappers (like ```json). Respond with only the raw JSON.
        """.trimIndent()

        val responseText = generateJsonText(prompt)
        parseSyllabusChapters(responseText)
    }

    /**
     * 2. Generate Interactive Quizzes
     * Coordinated by Assessment and Master Teacher Agents.
     */
    suspend fun generateChapterQuiz(
        bookTitle: String,
        chapterTitle: String,
        chapterContent: String,
        readLevel: String = "Undergraduate",
        depth: String = "Intermediate"
    ): List<QuizQuestionDto> = withContext(Dispatchers.IO) {
        val prompt = """
            ${AgentLibrary.ASSESSMENT_ROLE}
            ${AgentLibrary.MASTER_TEACHER_ROLE}

            The Assessment Agent is designing a customized multiple-choice quiz based on:
            Book: "$bookTitle"
            Chapter: "$chapterTitle"
            Chapter Content:
            $chapterContent
            
            Target Level: $readLevel ($depth)
            
            CORE RULE:
            Design questions that test for deep structural understanding and active recall, not just rote memorization.
            
            Generate exactly 3 multiple choice questions.
            Each question must have:
            1. 'questionText': The question, testing for deep concept understanding.
            2. 'optionA', 'optionB', 'optionC', 'optionD': Four clear, non-trivial options.
            3. 'correctAnswer': Must be exactly "A", "B", "C", or "D".
            4. 'explanation': A supportive, pedagogical explanation of why that option is correct, incorporating insights from [Master Teacher Agent].

            Return your output as a strict JSON array of objects.
            Format:
            [
              {
                "questionText": "Question text here?",
                "optionA": "First choice",
                "optionB": "Second choice",
                "optionC": "Third choice",
                "optionD": "Fourth choice",
                "correctAnswer": "A",
                "explanation": "Because..."
              },
              ...
            ]
            Do not include markdown wrappers. Respond with only the raw JSON.
        """.trimIndent()

        val responseText = generateJsonText(prompt)
        parseQuizQuestions(responseText)
    }

    /**
     * 3. Generate Revision Flashcards
     * Coordinated by Memory Science Agent.
     */
    suspend fun generateChapterFlashcards(
        bookTitle: String,
        chapterTitle: String,
        chapterContent: String,
        readLevel: String = "Undergraduate",
        depth: String = "Intermediate"
    ): List<FlashcardDto> = withContext(Dispatchers.IO) {
        val prompt = """
            ${AgentLibrary.MEMORY_SCIENCE_ROLE}

            The Memory Science Agent is synthesizing revision flashcards for the chapter "$chapterTitle" from the book "$bookTitle".
            Chapter summary and concepts:
            $chapterContent
            
            Target reading level: $readLevel
            Target depth: $depth

            Generate exactly 4 educational active-recall flashcards.
            Each flashcard has:
            1. 'front': A key term, question, or concept testing active recall.
            2. 'back': A brief concise definition or explanation (1-2 sentences) containing a helpful mnemonic device or memorable analogy if appropriate.

            Return your output as a strict JSON array of objects.
            Format:
            [
              {
                "front": "Term / Question",
                "back": "Definition / Answer"
              },
              ...
            ]
            Do not include markdown wrappers. Respond with only the raw JSON.
        """.trimIndent()

        val responseText = generateJsonText(prompt)
        parseFlashcards(responseText)
    }

    /**
     * 4. Explain highlighted text contextually
     * Coordinated by Master Teacher, Deep Explanation, and Visual Learning Agents.
     */
    suspend fun explainHighlight(
        bookTitle: String,
        chapterTitle: String,
        highlightText: String,
        readLevel: String = "Undergraduate",
        depth: String = "Intermediate"
    ): String = withContext(Dispatchers.IO) {
        val prompt = """
            ${AgentLibrary.MASTER_TEACHER_ROLE}
            ${AgentLibrary.DEEP_EXPLANATION_ROLE}
            ${AgentLibrary.VISUAL_LEARNING_ROLE}

            In the book "$bookTitle", within the chapter "$chapterTitle", the user highlighted this text:
            "$highlightText"
            
            Provide a deep, engaging explanation of this highlighted statement tailored to reading level: $readLevel and depth: $depth.
            Explain:
            1. **Simplification**: The context and real-world significance in simpler terms.
            2. **Deep Context**: What it actually means structurally in deep detail.
            3. **Analogy & Example**: A concrete real-world analogy and example.
            4. **[Visual Illustration]**: A description of a concept diagram/illustration that would help, with a detailed prompt for generating that image.
            5. **Self-Check**: A quick self-check question at the end.
            
            Keep your answer highly engaging, concise, and structured with clear paragraphs and beautiful markdown styling.
        """.trimIndent()

        generateText(prompt)
    }

    /**
     * Helper to enforce JSON return format
     */
    private suspend fun generateJsonText(prompt: String): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return ""

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // --- Parsing Functions (using simple, ultra-resilient manual JSON parsing) ---

    private fun parseSyllabusChapters(jsonStr: String): List<SyllabusChapterDto> {
        val list = mutableListOf<SyllabusChapterDto>()
        if (jsonStr.isEmpty()) return list
        try {
            val cleaned = cleanJsonString(jsonStr)
            val jsonArray = JSONArray(cleaned)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SyllabusChapterDto(
                        title = obj.optString("title", "Untitled Chapter"),
                        summary = obj.optString("summary", "No summary available."),
                        coreConcepts = obj.optString("coreConcepts", "No concepts provided.")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseQuizQuestions(jsonStr: String): List<QuizQuestionDto> {
        val list = mutableListOf<QuizQuestionDto>()
        if (jsonStr.isEmpty()) return list
        try {
            val cleaned = cleanJsonString(jsonStr)
            val jsonArray = JSONArray(cleaned)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    QuizQuestionDto(
                        questionText = obj.optString("questionText", "Review Question"),
                        optionA = obj.optString("optionA", "Option A"),
                        optionB = obj.optString("optionB", "Option B"),
                        optionC = obj.optString("optionC", "Option C"),
                        optionD = obj.optString("optionD", "Option D"),
                        correctAnswer = obj.optString("correctAnswer", "A").trim().uppercase(),
                        explanation = obj.optString("explanation", "Correct Answer.")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseFlashcards(jsonStr: String): List<FlashcardDto> {
        val list = mutableListOf<FlashcardDto>()
        if (jsonStr.isEmpty()) return list
        try {
            val cleaned = cleanJsonString(jsonStr)
            val jsonArray = JSONArray(cleaned)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    FlashcardDto(
                        front = obj.optString("front", "Term"),
                        back = obj.optString("back", "Definition")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun cleanJsonString(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```")) {
            str = str.replace(Regex("^```[a-zA-Z]*\\s*"), "")
            str = str.replace(Regex("\\s*```$"), "")
        }
        return str.trim()
    }
}

// --- Intermediate DTOs for Parsing ---

data class SyllabusChapterDto(
    val title: String,
    val summary: String,
    val coreConcepts: String
)

data class QuizQuestionDto(
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String,
    val explanation: String
)

data class FlashcardDto(
    val front: String,
    val back: String
)
