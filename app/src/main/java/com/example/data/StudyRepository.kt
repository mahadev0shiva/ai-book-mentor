package com.example.data

import kotlinx.coroutines.flow.Flow

class StudyRepository(private val studyDao: StudyDao) {
    val allBooks: Flow<List<Book>> = studyDao.getAllBooks()

    suspend fun getBookById(id: Int): Book? = studyDao.getBookById(id)

    suspend fun insertBook(book: Book): Long = studyDao.insertBook(book)

    suspend fun updateBook(book: Book) = studyDao.updateBook(book)

    suspend fun deleteBookById(id: Int) = studyDao.deleteBookById(id)

    fun getChaptersByBookId(bookId: Int): Flow<List<Chapter>> = studyDao.getChaptersByBookId(bookId)

    suspend fun getChapterById(id: Int): Chapter? = studyDao.getChapterById(id)

    suspend fun insertChapters(chapters: List<Chapter>) = studyDao.insertChapters(chapters)

    suspend fun updateChapter(chapter: Chapter) = studyDao.updateChapter(chapter)

    fun getHighlightsByBookId(bookId: Int): Flow<List<Highlight>> = studyDao.getHighlightsByBookId(bookId)

    suspend fun insertHighlight(highlight: Highlight) = studyDao.insertHighlight(highlight)

    suspend fun deleteHighlight(id: Int) = studyDao.deleteHighlight(id)

    fun getChatMessagesByBookId(bookId: Int): Flow<List<ChatMessage>> = studyDao.getChatMessagesByBookId(bookId)

    suspend fun insertChatMessage(message: ChatMessage) = studyDao.insertChatMessage(message)

    suspend fun clearChatMessages(bookId: Int) = studyDao.clearChatMessages(bookId)

    suspend fun getQuizByChapter(bookId: Int, chapterId: Int): List<QuizQuestion> =
        studyDao.getQuizByChapter(bookId, chapterId)

    suspend fun getQuizByBook(bookId: Int): List<QuizQuestion> =
        studyDao.getQuizByBook(bookId)

    suspend fun insertQuizQuestions(questions: List<QuizQuestion>) =
        studyDao.insertQuizQuestions(questions)

    fun getFlashcardsByChapter(bookId: Int, chapterId: Int): Flow<List<Flashcard>> =
        studyDao.getFlashcardsByChapter(bookId, chapterId)

    fun getFlashcardsByBook(bookId: Int): Flow<List<Flashcard>> =
        studyDao.getFlashcardsByBook(bookId)

    suspend fun insertFlashcards(flashcards: List<Flashcard>) =
        studyDao.insertFlashcards(flashcards)

    suspend fun updateFlashcard(flashcard: Flashcard) =
        studyDao.updateFlashcard(flashcard)
}
