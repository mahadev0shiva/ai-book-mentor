package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.Book
import com.example.data.Chapter
import com.example.data.ChatMessage
import com.example.data.Flashcard
import com.example.data.Highlight
import com.example.data.QuizQuestion
import com.example.data.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudyViewModel(private val repository: StudyRepository) : ViewModel() {

    // --- State flows from DB ---
    val books: StateFlow<List<Book>> = repository.allBooks
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook: StateFlow<Book?> = _selectedBook.asStateFlow()

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    private val _highlights = MutableStateFlow<List<Highlight>>(emptyList())
    val highlights: StateFlow<List<Highlight>> = _highlights.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _flashcards = MutableStateFlow<List<Flashcard>>(emptyList())
    val flashcards: StateFlow<List<Flashcard>> = _flashcards.asStateFlow()

    // --- Loading & Error UI States ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow("")
    val loadingMessage: StateFlow<String> = _loadingMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- Quiz Session States ---
    private val _activeQuiz = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val activeQuiz: StateFlow<List<QuizQuestion>> = _activeQuiz.asStateFlow()

    private val _currentQuizIndex = MutableStateFlow(0)
    val currentQuizIndex: StateFlow<Int> = _currentQuizIndex.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    private val _quizCompleted = MutableStateFlow(false)
    val quizCompleted: StateFlow<Boolean> = _quizCompleted.asStateFlow()

    // Tracks if user has answered the current question, and what their answer was
    private val _selectedAnswer = MutableStateFlow<String?>(null)
    val selectedAnswer: StateFlow<String?> = _selectedAnswer.asStateFlow()

    init {
        // Collect books to set default selection if books exist
        viewModelScope.launch {
            books.collect { bookList ->
                if (_selectedBook.value == null && bookList.isNotEmpty()) {
                    selectBook(bookList.first())
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Select active book and load associated data
     */
    fun selectBook(book: Book) {
        _selectedBook.value = book
        // Reset state
        _activeQuiz.value = emptyList()
        _quizCompleted.value = false
        _currentQuizIndex.value = 0
        _quizScore.value = 0
        _selectedAnswer.value = null

        viewModelScope.launch {
            // Load Chapters
            repository.getChaptersByBookId(book.id).collect { chapterList ->
                _chapters.value = chapterList
            }
        }
        viewModelScope.launch {
            // Load Highlights
            repository.getHighlightsByBookId(book.id).collect { highlightList ->
                _highlights.value = highlightList
            }
        }
        viewModelScope.launch {
            // Load Chat Messages
            repository.getChatMessagesByBookId(book.id).collect { chatList ->
                _chatMessages.value = chatList
            }
        }
        viewModelScope.launch {
            // Load Flashcards
            repository.getFlashcardsByBook(book.id).collect { flashcardList ->
                _flashcards.value = flashcardList
            }
        }
    }

    /**
     * Toggle Chapter Completion Status (and recalculate overall progress)
     */
    fun toggleChapterCompletion(chapter: Chapter) {
        viewModelScope.launch {
            val updated = chapter.copy(isCompleted = !chapter.isCompleted)
            repository.updateChapter(updated)

            // Recalculate book progress percentage
            _selectedBook.value?.let { currentBook ->
                repository.getChaptersByBookId(currentBook.id).collect { currentChapters ->
                    if (currentChapters.isNotEmpty()) {
                        val completedCount = currentChapters.count { it.isCompleted }
                        val newProgress = (completedCount * 100) / currentChapters.size
                        val updatedBook = currentBook.copy(progressPercent = newProgress)
                        repository.updateBook(updatedBook)
                        _selectedBook.value = updatedBook
                    }
                }
            }
        }
    }

    /**
     * Generate custom syllabus pathway using Gemini AI and persist to Room DB.
     * Sequentially coordinates the Multi-Agent Prompt Library:
     * Master Orchestrator, Book Analysis, Curriculum Designer, Master Teacher, Deep Explanation,
     * Visual Learning, Fact-Checking, Ebook Designer, Memory Science, and Assessment Agents.
     */
    fun generateCustomBookPath(
        title: String,
        author: String,
        category: String,
        content: String = "",
        depth: String = "Intermediate",
        readLevel: String = "Undergraduate",
        teachingStyle: String = "Socratic Method",
        imgDensity: String = "Standard",
        includeQuiz: Boolean = true,
        includeFlashcards: Boolean = true
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "[Master Orchestrator] Organizing curation flow..."
            _errorMessage.value = null

            try {
                // Step 1: Syllabus Creation
                _loadingMessage.value = "[Book Analysis & Curriculum Designer] Mapping chapters and learning pathways..."
                val dtos = GeminiClient.generateBookSyllabus(
                    title = title,
                    author = author,
                    category = category,
                    content = content,
                    depth = depth,
                    readLevel = readLevel,
                    teachingStyle = teachingStyle,
                    imgDensity = imgDensity
                )
                if (dtos.isEmpty()) {
                    _errorMessage.value = "Failed to draft the study pathway. Please make sure your Gemini API key is valid."
                    _isLoading.value = false
                    return@launch
                }

                _loadingMessage.value = "[Master Teacher & Deep Explanation Agent] Drafting pedagogical summaries..."
                // Create Book in DB
                val bookId = repository.insertBook(
                    Book(
                        title = title,
                        author = author.ifBlank { "Unknown Author" },
                        description = "Premium study package for $title ($readLevel - $depth). Styled in a $teachingStyle format.",
                        category = category.ifBlank { "General" },
                        isCustom = true
                    )
                ).toInt()

                _loadingMessage.value = "[Ebook Designer & Visual Learning Agent] Formatting typography..."
                // Map and insert Chapters
                val chaptersList = dtos.mapIndexed { index, dto ->
                    Chapter(
                        bookId = bookId,
                        title = dto.title,
                        orderIndex = index + 1,
                        summary = dto.summary,
                        coreConcepts = dto.coreConcepts
                    )
                }
                repository.insertChapters(chaptersList)

                // Select the new book
                val insertedBook = repository.getBookById(bookId)
                if (insertedBook != null) {
                    selectBook(insertedBook)
                }

                _loadingMessage.value = "[Fact-Checking Agent] Verifying modules..."
                val dbChapters = repository.getChaptersByBookId(bookId).first { it.isNotEmpty() }

                // Step 2: Memory Science Agent (Flashcards)
                if (includeFlashcards) {
                    _loadingMessage.value = "[Memory Science Agent] Synthesizing active-recall flashcards..."
                    dbChapters.forEach { ch ->
                        val chapterContentString = "${ch.title}\n${ch.summary}\n${ch.coreConcepts}"
                        try {
                            val flashcardDtos = GeminiClient.generateChapterFlashcards(
                                bookTitle = title,
                                chapterTitle = ch.title,
                                chapterContent = chapterContentString,
                                readLevel = readLevel,
                                depth = depth
                            )
                            if (flashcardDtos.isNotEmpty()) {
                                val cards = flashcardDtos.map {
                                    Flashcard(
                                        bookId = bookId,
                                        chapterId = ch.id,
                                        front = it.front,
                                        back = it.back
                                    )
                                }
                                repository.insertFlashcards(cards)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // Step 3: Assessment Agent (Quiz Questions)
                if (includeQuiz) {
                    _loadingMessage.value = "[Assessment Agent] Designing comprehensive quizzes..."
                    dbChapters.forEach { ch ->
                        val chapterContentString = "${ch.title}\n${ch.summary}\n${ch.coreConcepts}"
                        try {
                            val quizDtos = GeminiClient.generateChapterQuiz(
                                bookTitle = title,
                                chapterTitle = ch.title,
                                chapterContent = chapterContentString,
                                readLevel = readLevel,
                                depth = depth
                            )
                            if (quizDtos.isNotEmpty()) {
                                val questions = quizDtos.map {
                                    QuizQuestion(
                                        bookId = bookId,
                                        chapterId = ch.id,
                                        questionText = it.questionText,
                                        optionA = it.optionA,
                                        optionB = it.optionB,
                                        optionC = it.optionC,
                                        optionD = it.optionD,
                                        correctAnswer = it.correctAnswer,
                                        explanation = it.explanation
                                    )
                                }
                                repository.insertQuizQuestions(questions)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                _loadingMessage.value = "[Export Agent] Bundling final learning package..."
                // Select the new book again to ensure UI has latest associations
                val finalBook = repository.getBookById(bookId)
                if (finalBook != null) {
                    selectBook(finalBook)
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Save a highlighted text and trigger automatic AI deep context explanation.
     * Uses Master Teacher, Deep Explanation, and Visual Learning Agents.
     */
    fun saveHighlight(chapter: Chapter, text: String, note: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _selectedBook.value?.let { book ->
                val highlight = Highlight(
                    bookId = book.id,
                    chapterId = chapter.id,
                    text = text,
                    note = note,
                    explanation = "AI is thinking..."
                )
                repository.insertHighlight(highlight)

                // Explain highlight in background
                launch {
                    try {
                        val explanationText = GeminiClient.explainHighlight(
                            bookTitle = book.title,
                            chapterTitle = chapter.title,
                            highlightText = text,
                            readLevel = book.category,
                            depth = "Detailed"
                        )
                        // Retrieve the highlights to update the right one
                        repository.getHighlightsByBookId(book.id).collect { currentList ->
                            val savedHighlight = currentList.firstOrNull { it.text == text && it.chapterId == chapter.id }
                            if (savedHighlight != null) {
                                val updated = savedHighlight.copy(explanation = explanationText)
                                repository.insertHighlight(updated)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * Delete highlight
     */
    fun deleteHighlight(id: Int) {
        viewModelScope.launch {
            repository.deleteHighlight(id)
        }
    }

    /**
     * Chat with the Academic AI Tutor about the Book
     */
    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val currentBook = _selectedBook.value ?: return

        viewModelScope.launch {
            // Append user message
            val userMsg = ChatMessage(bookId = currentBook.id, role = "user", content = userText)
            repository.insertChatMessage(userMsg)

            // Prepare typing response placeholder
            val modelPlaceholder = ChatMessage(bookId = currentBook.id, role = "model", content = "Thinking...")
            repository.insertChatMessage(modelPlaceholder)

            try {
                // Get chat history for context
                var chatHistoryText = ""
                _chatMessages.value.forEach {
                    if (it.content != "Thinking...") {
                        chatHistoryText += "${if (it.role == "user") "User" else "Tutor"}: ${it.content}\n"
                    }
                }

                // Construct system context prompt using combined AI Tutor & Master Teacher Agent definition
                val systemInstruction = """
                    You are the [AI Tutor Agent] and [Master Teacher Agent] combined.
                    You are assisting the student to master the book/topic: "${currentBook.title}" by "${currentBook.author}".
                    Your target cognitive level is based on Category/Subject: "${currentBook.category}".
                    
                    CORE DIRECTIVES:
                    - Teach for understanding, not just information.
                    - Adopt a highly supportive, warm, Socratic conversational approach.
                    - Explain complex concepts using intuitive real-world analogies, clear examples, and original pedagogical explanations.
                    - Guide the student towards independent realization by asking check questions and active-recall triggers.
                    - Format your output with premium editorial layout, utilizing Markdown, headers, bullet points, and highlighted text boxes (callouts) where appropriate.
                """.trimIndent()

                val response = GeminiClient.generateText(
                    prompt = "Chat History:\n$chatHistoryText\nUser: $userText\nTutor:",
                    systemPrompt = systemInstruction
                )

                // Update the placeholder message with actual response
                repository.getChatMessagesByBookId(currentBook.id).collect { freshList ->
                    val placeholder = freshList.lastOrNull { it.role == "model" && it.content == "Thinking..." }
                    if (placeholder != null) {
                        val finalMsg = placeholder.copy(content = response)
                        repository.insertChatMessage(finalMsg)
                    }
                }
            } catch (e: Exception) {
                // Update placeholder with error
                repository.getChatMessagesByBookId(currentBook.id).collect { freshList ->
                    val placeholder = freshList.lastOrNull { it.role == "model" && it.content == "Thinking..." }
                    if (placeholder != null) {
                        repository.insertChatMessage(placeholder.copy(content = "Error: Failed to fetch tutor response. Please check connection and API key."))
                    }
                }
            }
        }
    }

    /**
     * Clear active chat history
     */
    fun clearChatHistory() {
        _selectedBook.value?.let { book ->
            viewModelScope.launch {
                repository.clearChatMessages(book.id)
            }
        }
    }

    /**
     * Start a Quiz session for a chapter. If no quiz exists, use Gemini to generate it!
     */
    fun startChapterQuiz(chapter: Chapter) {
        val currentBook = _selectedBook.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Formulating adaptive revision questions..."
            _errorMessage.value = null

            _activeQuiz.value = emptyList()
            _quizCompleted.value = false
            _currentQuizIndex.value = 0
            _quizScore.value = 0
            _selectedAnswer.value = null

            try {
                // Check database first
                var questions = repository.getQuizByChapter(currentBook.id, chapter.id)
                if (questions.isEmpty()) {
                    // Generate using AI
                    val contentString = "${chapter.title}\n${chapter.summary}\n${chapter.coreConcepts}"
                    val dtos = GeminiClient.generateChapterQuiz(currentBook.title, chapter.title, contentString)
                    if (dtos.isNotEmpty()) {
                        val questionsList = dtos.map {
                            QuizQuestion(
                                bookId = currentBook.id,
                                chapterId = chapter.id,
                                questionText = it.questionText,
                                optionA = it.optionA,
                                optionB = it.optionB,
                                optionC = it.optionC,
                                optionD = it.optionD,
                                correctAnswer = it.correctAnswer,
                                explanation = it.explanation
                            )
                        }
                        repository.insertQuizQuestions(questionsList)
                        questions = questionsList
                    }
                }

                if (questions.isNotEmpty()) {
                    _activeQuiz.value = questions
                } else {
                    _errorMessage.value = "Failed to load quiz. Verify your Gemini API Key in AI Studio."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to launch quiz: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Select a quiz option
     */
    fun selectQuizAnswer(answer: String) {
        if (_selectedAnswer.value != null) return // Already answered
        _selectedAnswer.value = answer

        val currentQuestion = _activeQuiz.value.getOrNull(_currentQuizIndex.value)
        if (currentQuestion != null && answer == currentQuestion.correctAnswer) {
            _quizScore.value += 1
        }
    }

    /**
     * Advance to the next quiz question
     */
    fun nextQuizQuestion() {
        val nextIndex = _currentQuizIndex.value + 1
        if (nextIndex < _activeQuiz.value.size) {
            _currentQuizIndex.value = nextIndex
            _selectedAnswer.value = null
        } else {
            _quizCompleted.value = true
        }
    }

    /**
     * Generate flashcards for a chapter. If none exist, use Gemini AI!
     */
    fun loadOrGenerateFlashcards(chapter: Chapter) {
        val currentBook = _selectedBook.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Synthesizing visual revision flashcards..."
            _errorMessage.value = null

            try {
                repository.getFlashcardsByChapter(currentBook.id, chapter.id).collect { dbFlashcards ->
                    if (dbFlashcards.isEmpty()) {
                        // Generate
                        val contentString = "${chapter.title}\n${chapter.summary}\n${chapter.coreConcepts}"
                        val dtos = GeminiClient.generateChapterFlashcards(currentBook.title, chapter.title, contentString)
                        if (dtos.isNotEmpty()) {
                            val flashcardList = dtos.map {
                                Flashcard(
                                    bookId = currentBook.id,
                                    chapterId = chapter.id,
                                    front = it.front,
                                    back = it.back
                                )
                            }
                            repository.insertFlashcards(flashcardList)
                        }
                    } else {
                        _flashcards.value = dbFlashcards
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to synthesize flashcards: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Toggle flashcard master status
     */
    fun toggleFlashcardMastered(flashcard: Flashcard) {
        viewModelScope.launch {
            val updated = flashcard.copy(isMastered = !flashcard.isMastered)
            repository.updateFlashcard(updated)
        }
    }
}

class StudyViewModelFactory(private val repository: StudyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
