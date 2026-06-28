package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Dao
interface StudyDao {
    // Books
    @Query("SELECT * FROM books ORDER BY createdAt DESC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Int): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Update
    suspend fun updateBook(book: Book)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Int)

    // Chapters
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY orderIndex ASC")
    fun getChaptersByBookId(bookId: Int): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Int): Chapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<Chapter>)

    @Update
    suspend fun updateChapter(chapter: Chapter)

    // Highlights
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getHighlightsByBookId(bookId: Int): Flow<List<Highlight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: Int)

    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE bookId = :bookId ORDER BY timestamp ASC")
    fun getChatMessagesByBookId(bookId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE bookId = :bookId")
    suspend fun clearChatMessages(bookId: Int)

    // Quiz Questions
    @Query("SELECT * FROM quiz_questions WHERE bookId = :bookId AND chapterId = :chapterId")
    suspend fun getQuizByChapter(bookId: Int, chapterId: Int): List<QuizQuestion>

    @Query("SELECT * FROM quiz_questions WHERE bookId = :bookId")
    suspend fun getQuizByBook(bookId: Int): List<QuizQuestion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizQuestions(questions: List<QuizQuestion>)

    // Flashcards
    @Query("SELECT * FROM flashcards WHERE bookId = :bookId AND chapterId = :chapterId")
    fun getFlashcardsByChapter(bookId: Int, chapterId: Int): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE bookId = :bookId")
    fun getFlashcardsByBook(bookId: Int): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<Flashcard>)

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)
}

@Database(
    entities = [
        Book::class,
        Chapter::class,
        Highlight::class,
        ChatMessage::class,
        QuizQuestion::class,
        Flashcard::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_companion_db"
                )
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed database on a background thread
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = getDatabase(context).studyDao()
                    seedInitialBooks(dao)
                }
            }

            private suspend fun seedInitialBooks(dao: StudyDao) {
                // Book 1: Sapiens
                val sapiensId = dao.insertBook(
                    Book(
                        title = "Sapiens: A Brief History of Humankind",
                        author = "Yuval Noah Harari",
                        description = "Explores how history has shaped human societies, from the cognitive revolution 70,000 years ago to modern days.",
                        category = "History & Science",
                        progressPercent = 0,
                        isCustom = false
                    )
                ).toInt()

                dao.insertChapters(
                    listOf(
                        Chapter(
                            bookId = sapiensId,
                            title = "The Cognitive Revolution",
                            orderIndex = 1,
                            summary = "About 70,000 years ago, Homo sapiens underwent a Cognitive Revolution. This allowed Sapiens to develop flexible language, abstract thinking, and shared myths (such as religions, nations, and money). These shared constructs enabled sapiens to cooperate in exceptionally large groups, eventually outcompeting other human species like Neanderthals.",
                            coreConcepts = "• Cognitive Revolution: The sudden cognitive leap enabling complex language.\n• Shared Myths: Fictional beliefs (laws, corporations, currency) that enable massive collective coordination.\n• Flexible Cooperation: Sapiens' unique ability to coordinate adaptably with countless strangers."
                        ),
                        Chapter(
                            bookId = sapiensId,
                            title = "The Agricultural Revolution",
                            orderIndex = 2,
                            summary = "Around 12,000 years ago, humans transitioned from hunter-gatherer foraging to agriculture. While it boosted total food supply and allowed human populations to grow exponentially, it locked individuals into grueling manual routines, social hierarchy, and dietary deficiencies. Harari describes it as 'history's biggest fraud' where wheat domesticated Sapiens.",
                            coreConcepts = "• Domestication Trap: How crops coerced humans into sedentary, high-labor lives.\n• Demographic Expansion: Population size exploded while individual quality of life often declined.\n• Social Stratification: Surplus food enabled the rise of rulers, elites, and heavy class divides."
                        ),
                        Chapter(
                            bookId = sapiensId,
                            title = "The Unification of Humankind",
                            orderIndex = 3,
                            summary = "Historically, human cultures were fragmented. However, three main forces unified the globe into a cohesive network: the monetary order (unifying through trade), the imperial order (unifying through political power), and the universal religious order (unifying through spiritual codes). Today, we live in a single globalized culture.",
                            coreConcepts = "• Monetary Order: Money as the ultimate universal trust mechanism.\n• Imperial Order: Empires absorbing diverse cultures, homogenizing laws and customs.\n• Universal Religions: Belief systems appealing to all humanity, standardizing behavioral norms globally."
                        )
                    )
                )

                // Seed some quizzes for Sapiens Chapter 1
                dao.insertQuizQuestions(
                    listOf(
                        QuizQuestion(
                            bookId = sapiensId,
                            chapterId = 1,
                            questionText = "What is the primary cognitive change that enabled Homo sapiens to dominate other human species?",
                            optionA = "The discovery of fire and metalwork",
                            optionB = "The Cognitive Revolution, enabling complex language and shared myths",
                            optionC = "Direct evolutionary physical superiority over Neanderthals",
                            optionD = "The transition to settled crop farming",
                            correctAnswer = "B",
                            explanation = "Homo sapiens' Cognitive Revolution enabled complex language and the creation of shared myths, allowing flexible cooperation in huge groups."
                        ),
                        QuizQuestion(
                            bookId = sapiensId,
                            chapterId = 1,
                            questionText = "According to Harari, what are 'Shared Myths'?",
                            optionA = "Fictional ideas (money, laws, states) that enable large-scale cooperation",
                            optionB = "Ancient campfire stories with no real-world social impact",
                            optionC = "Biological impulses hardcoded in human DNA",
                            optionD = "Scientific truths verified by empirical evidence",
                            correctAnswer = "A",
                            explanation = "Shared myths are fictional constructs like money, laws, and nations that exist only in our collective imagination but facilitate trust and cooperation."
                        ),
                        QuizQuestion(
                            bookId = sapiensId,
                            chapterId = 2,
                            questionText = "Why does Harari refer to the Agricultural Revolution as 'history's biggest fraud'?",
                            optionA = "Crops did not actually grow as expected",
                            optionB = "It reduced the average individual's variety of diet and increased labor hours",
                            optionC = "All lands were stolen by extraterrestrials",
                            optionD = "It immediately resulted in a massive decline of the global population",
                            correctAnswer = "B",
                            explanation = "The Agricultural Revolution led to longer working hours, higher risk of disease, and a narrower diet for individuals, despite supporting a larger overall population."
                        )
                    )
                )

                // Seed some flashcards for Sapiens
                dao.insertFlashcards(
                    listOf(
                        Flashcard(
                            bookId = sapiensId,
                            chapterId = 1,
                            front = "Cognitive Revolution",
                            back = "The evolutionary leap around 70,000 years ago that gave Sapiens advanced language and abstract thinking."
                        ),
                        Flashcard(
                            bookId = sapiensId,
                            chapterId = 1,
                            front = "Shared Myths",
                            back = "Imagined realities (like currency, states, and corporations) that allow thousands of strangers to cooperate smoothly."
                        ),
                        Flashcard(
                            bookId = sapiensId,
                            chapterId = 2,
                            front = "Domestication Trap",
                            back = "The idea that humans did not domesticate wheat, but rather wheat domesticated humans, locking them into grueling farm work."
                        )
                    )
                )

                // Book 2: Thinking, Fast and Slow
                val thinkingId = dao.insertBook(
                    Book(
                        title = "Thinking, Fast and Slow",
                        author = "Daniel Kahneman",
                        description = "A deep dive into the two cognitive systems that drive our decisions: the intuitive, emotional System 1, and the deliberate, logical System 2.",
                        category = "Psychology",
                        progressPercent = 0,
                        isCustom = false
                    )
                ).toInt()

                dao.insertChapters(
                    listOf(
                        Chapter(
                            bookId = thinkingId,
                            title = "Two Systems of the Mind",
                            orderIndex = 1,
                            summary = "Nobel laureate Daniel Kahneman maps human thinking into two systems. System 1 operates automatically and quickly, with little or no effort and no sense of voluntary control (intuitive). System 2 allocates attention to the effortful mental operations that demand it, including complex computations (deliberative). Conflict arises because System 1 is prone to systematic biases.",
                            coreConcepts = "• System 1: Rapid, emotional, unconscious, associative mental processor.\n• System 2: Slow, logical, skeptical, energy-demanding mental supervisor.\n• Cognitive Ease: The state of comfort where System 1 runs smoothly, making us gullible and less analytical."
                        ),
                        Chapter(
                            bookId = thinkingId,
                            title = "Heuristics and Biases",
                            orderIndex = 2,
                            summary = "System 1 relies heavily on heuristics—mental shortcuts that simplify complex decision-making. However, these shortcuts frequently lead to predictable errors (biases). Key traps include the anchoring effect (relying too much on initial info), availability heuristic (overestimating the likelihood of things easy to recall), and substitution (answering an easier question instead of the hard one).",
                            coreConcepts = "• Anchoring: How initial random numbers strongly pull subsequent estimates.\n• Availability Bias: Confusing ease of recall with statistical frequency.\n• Substitution: Simplifying a tough problem by replacing it with a simpler intuitive judgment."
                        )
                    )
                )

                // Book 3: Atomic Habits
                val habitsId = dao.insertBook(
                    Book(
                        title = "Atomic Habits",
                        author = "James Clear",
                        description = "An extremely practical guide to building great habits and breaking bad ones by focusing on 1% daily compounding gains.",
                        category = "Self-Improvement",
                        progressPercent = 0,
                        isCustom = false
                    )
                ).toInt()

                dao.insertChapters(
                    listOf(
                        Chapter(
                            bookId = habitsId,
                            title = "The Power of 1% Compounding",
                            orderIndex = 1,
                            summary = "Small daily adjustments seem insignificant at first, but compound over months and years into monumental shifts. James Clear argues that you do not rise to the level of your goals, but rather fall to the level of your systems. Sustainable habit change must be built on identity-level shifts, changing who you are, rather than what you want to achieve.",
                            coreConcepts = "• Marginal Gains: Compounding 1% adjustments daily results in a 37x improvement in a year.\n• Systems over Goals: Designing workflows and routines that make progress inevitable.\n• Identity Habit Change: Aligning habits with self-image ('I am a writer' vs 'I want to write')."
                        )
                    )
                )
            }
        }
    }
}
