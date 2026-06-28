@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Helper Data Classes defined at top-level to ensure clean compilation
data class OnboardingData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

data class MockCatalog(
    val title: String,
    val author: String,
    val cat: String,
    val color: Color
)

data class ThemePalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyApp(viewModel: StudyViewModel) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val selectedBook by viewModel.selectedBook.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val flashcards by viewModel.flashcards.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val loadingMessage by viewModel.loadingMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    // --- State variables for unified 17-screen flow ---
    var appWorkflowState by remember { mutableStateOf("splash") } // "splash", "onboarding", "login", "main", "reader"
    var onboardingPage by remember { mutableStateOf(0) }
    var nickname by remember { mutableStateOf("Scholar") }
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // Bottom Navigation Bar state inside "main"
    var mainNavigationTab by remember { mutableStateOf("home") } // "home", "library", "create", "learn", "profile"

    // Custom UI settings
    var appThemeStyle by remember { mutableStateOf("Cobalt Star") } // "Cobalt Star", "Emerald Forest", "Rose Gold"
    var isDarkTheme by remember { mutableStateOf(true) }
    var readerFontSize by remember { mutableStateOf(16.sp) }

    // Onboarding page content list
    val onboardingPages = listOf(
        OnboardingData(
            title = "Synthesize Custom Books",
            description = "Transform any textbook, literature piece, or academic topic into comprehensive, AI-drafted syllabi instantly.",
            icon = Icons.Default.AutoStories,
            gradientColors = listOf(Color(0xFF3F51B5), Color(0xFF00BCD4))
        ),
        OnboardingData(
            title = "Deep Visual Context",
            description = "Explore custom-designed diagrams, charts, and interactive concept mind maps created dynamically to optimize memory.",
            icon = Icons.Default.Analytics,
            gradientColors = listOf(Color(0xFF009688), Color(0xFF4CAF50))
        ),
        OnboardingData(
            title = "Conversational AI Tutor",
            description = "Get instant clarification on difficult concepts, explore real-world analogies, or translate reading selections anytime.",
            icon = Icons.Default.Psychology,
            gradientColors = listOf(Color(0xFF673AB7), Color(0xFFE91E63))
        ),
        OnboardingData(
            title = "Smart Active Recall",
            description = "Gauge knowledge retention with adaptive multiple-choice revisions, true/false scenarios, and interactive flashcards.",
            icon = Icons.Default.Quiz,
            gradientColors = listOf(Color(0xFFFF9800), Color(0xFFE91E63))
        )
    )

    // User progress stats (interactive mockup states)
    var studyMinutesToday by remember { mutableStateOf(18) }
    val studyGoalMinutes = 30
    var selectedCertificateToView by remember { mutableStateOf<String?>(null) }

    // Library tab and query state
    var libraryTab by remember { mutableStateOf("My Books") }
    var librarySearch by remember { mutableStateOf("") }
    var libraryFilter by remember { mutableStateOf("All") }

    // Create Screen States
    var createImportType by remember { mutableStateOf("paste") } // "paste", "pdf", "ocr", "public"
    var pasteTitle by remember { mutableStateOf("") }
    var pasteAuthor by remember { mutableStateOf("") }
    var pasteCategory by remember { mutableStateOf("") }
    var pasteContentText by remember { mutableStateOf("") }
    var ocrPhotoCaptured by remember { mutableStateOf(false) }
    var ocrScanningAnimation by remember { mutableStateOf(false) }
    var selectedPublicBookId by remember { mutableStateOf<Int?>(null) }

    // AI Generation settings state
    var expDepth by remember { mutableStateOf("Intermediate") }
    var readLevel by remember { mutableStateOf("Undergraduate") }
    var teachingStyle by remember { mutableStateOf("Socratic Method") }
    var imgDensity by remember { mutableStateOf("Standard") }
    var toggleIncludeQuizzes by remember { mutableStateOf(true) }
    var toggleIncludeFlashcards by remember { mutableStateOf(true) }

    // Reader Screen States
    var activeReaderTab by remember { mutableStateOf("content") } // "content", "tutor", "quiz", "flashcards", "mindmap", "export"
    var selectedReaderChapterId by remember { mutableStateOf<Int?>(null) }
    var selectedHighlightText by remember { mutableStateOf("") }
    var highlightNoteText by remember { mutableStateOf("") }
    var showHighlightDialog by remember { mutableStateOf<Chapter?>(null) }
    var bookmarkedChapters by remember { mutableStateOf(setOf<Int>()) }

    // Mindmap state
    var mindMapScale by remember { mutableStateOf(1.0f) }
    var expandedConceptNode by remember { mutableStateOf<String?>(null) }

    // Dynamic Color Palette mapping
    val currentThemeColors = when (appThemeStyle) {
        "Emerald Forest" -> ThemePalette(
            primary = Color(0xFF2E7D32),
            secondary = Color(0xFF00796B),
            tertiary = Color(0xFF558B2F),
            background = if (isDarkTheme) Color(0xFF0C130D) else Color(0xFFF1F8E9),
            surface = if (isDarkTheme) Color(0xFF142016) else Color(0xFFFFFFFF),
            onPrimary = Color.White
        )
        "Rose Gold" -> ThemePalette(
            primary = Color(0xFFAD1457),
            secondary = Color(0xFF880E4F),
            tertiary = Color(0xFFD81B60),
            background = if (isDarkTheme) Color(0xFF1A0E13) else Color(0xFFFCE4EC),
            surface = if (isDarkTheme) Color(0xFF26151C) else Color(0xFFFFFFFF),
            onPrimary = Color.White
        )
        else -> ThemePalette( // "Cobalt Star" (Default)
            primary = Color(0xFF1E88E5),
            secondary = Color(0xFF0D47A1),
            tertiary = Color(0xFF00ACC1),
            background = if (isDarkTheme) Color(0xFF0D141F) else Color(0xFFF0F4F8),
            surface = if (isDarkTheme) Color(0xFF152238) else Color(0xFFFFFFFF),
            onPrimary = Color.White
        )
    }

    MaterialTheme(
        colorScheme = if (isDarkTheme) {
            darkColorScheme(
                primary = currentThemeColors.primary,
                secondary = currentThemeColors.secondary,
                tertiary = currentThemeColors.tertiary,
                background = currentThemeColors.background,
                surface = currentThemeColors.surface
            )
        } else {
            lightColorScheme(
                primary = currentThemeColors.primary,
                secondary = currentThemeColors.secondary,
                tertiary = currentThemeColors.tertiary,
                background = currentThemeColors.background,
                surface = currentThemeColors.surface
            )
        },
        typography = androidx.compose.material3.Typography()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // MAIN CONTENT ROUTER
                when (appWorkflowState) {
                    "splash" -> {
                        SplashScreenView(
                            onContinue = { appWorkflowState = "onboarding" },
                            onSkip = { appWorkflowState = "main" }
                        )
                    }

                    "onboarding" -> {
                        OnboardingScreenView(
                            pages = onboardingPages,
                            currentPage = onboardingPage,
                            onPageChange = { onboardingPage = it },
                            onSkip = { appWorkflowState = "login" },
                            onFinish = { appWorkflowState = "login" }
                        )
                    }

                    "login" -> {
                        LoginScreenView(
                            email = loginEmail,
                            onEmailChange = { loginEmail = it },
                            password = loginPassword,
                            onPasswordChange = { loginPassword = it },
                            onForgotPasswordClick = { showForgotPasswordDialog = true },
                            onLoginSuccess = { user ->
                                nickname = if (user.isNotBlank()) user else "Scholar"
                                appWorkflowState = "main"
                            }
                        )
                    }

                    "main" -> {
                        Scaffold(
                            bottomBar = {
                                NavigationBar(
                                    modifier = Modifier.testTag("bottom_nav_bar"),
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp
                                ) {
                                    NavigationBarItem(
                                        selected = mainNavigationTab == "home",
                                        onClick = { mainNavigationTab = "home" },
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                        label = { Text("Home", fontSize = 11.sp) },
                                        modifier = Modifier.testTag("nav_tab_home")
                                    )
                                    NavigationBarItem(
                                        selected = mainNavigationTab == "library",
                                        onClick = { mainNavigationTab = "library" },
                                        icon = { Icon(Icons.Default.LocalLibrary, contentDescription = "Library") },
                                        label = { Text("Library", fontSize = 11.sp) },
                                        modifier = Modifier.testTag("nav_tab_library")
                                    )
                                    NavigationBarItem(
                                        selected = mainNavigationTab == "create",
                                        onClick = { mainNavigationTab = "create" },
                                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Create") },
                                        label = { Text("Create", fontSize = 11.sp) },
                                        modifier = Modifier.testTag("nav_tab_create")
                                    )
                                    NavigationBarItem(
                                        selected = mainNavigationTab == "learn",
                                        onClick = { mainNavigationTab = "learn" },
                                        icon = { Icon(Icons.Default.School, contentDescription = "Learn") },
                                        label = { Text("Learn", fontSize = 11.sp) },
                                        modifier = Modifier.testTag("nav_tab_learn")
                                    )
                                    NavigationBarItem(
                                        selected = mainNavigationTab == "profile",
                                        onClick = { mainNavigationTab = "profile" },
                                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                        label = { Text("Profile", fontSize = 11.sp) },
                                        modifier = Modifier.testTag("nav_tab_profile")
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (mainNavigationTab) {
                                    "home" -> {
                                        HomeDashboardView(
                                            books = books,
                                            nickname = nickname,
                                            studyMinutes = studyMinutesToday,
                                            studyGoal = studyGoalMinutes,
                                            onLogMinute = { studyMinutesToday = (studyMinutesToday + 5).coerceAtMost(60) },
                                            onBookSelect = { book ->
                                                viewModel.selectBook(book)
                                                selectedReaderChapterId = null
                                                activeReaderTab = "content"
                                                appWorkflowState = "reader"
                                            },
                                            onSyllabusBuilderClick = { mainNavigationTab = "create" }
                                        )
                                    }

                                    "library" -> {
                                        LibraryView(
                                            books = books,
                                            selectedTab = libraryTab,
                                            onTabChange = { libraryTab = it },
                                            searchQuery = librarySearch,
                                            onSearchChange = { librarySearch = it },
                                            filterState = libraryFilter,
                                            onFilterChange = { libraryFilter = it },
                                            onBookSelect = { book ->
                                                viewModel.selectBook(book)
                                                selectedReaderChapterId = null
                                                activeReaderTab = "content"
                                                appWorkflowState = "reader"
                                            }
                                        )
                                    }

                                    "create" -> {
                                        CreateBookView(
                                            importType = createImportType,
                                            onImportTypeChange = { createImportType = it },
                                            title = pasteTitle,
                                            onTitleChange = { pasteTitle = it },
                                            author = pasteAuthor,
                                            onAuthorChange = { pasteAuthor = it },
                                            category = pasteCategory,
                                            onCategoryChange = { pasteCategory = it },
                                            content = pasteContentText,
                                            onContentChange = { pasteContentText = it },
                                            ocrCaptured = ocrPhotoCaptured,
                                            onOcrCapture = {
                                                coroutineScope.launch {
                                                    ocrScanningAnimation = true
                                                    delay(2500)
                                                    ocrPhotoCaptured = true
                                                    ocrScanningAnimation = false
                                                    pasteTitle = "Scientific Article Scan"
                                                    pasteAuthor = "Camera Scanner OCR"
                                                    pasteCategory = "Science"
                                                    pasteContentText = "Recent breakthroughs in astronomical astrophysics point to dark matter structures behaving as cosmic lattice webs across clusters."
                                                }
                                            },
                                            ocrScanning = ocrScanningAnimation,
                                            onOcrReset = {
                                                ocrPhotoCaptured = false
                                                pasteContentText = ""
                                            },
                                            selectedPublicId = selectedPublicBookId,
                                            onSelectPublicId = { id, title, author, cat ->
                                                selectedPublicBookId = id
                                                pasteTitle = title
                                                pasteAuthor = author
                                                pasteCategory = cat
                                                pasteContentText = "Synthesis outline for classic public domain work: $title by $author."
                                            },
                                            expDepth = expDepth,
                                            onDepthChange = { expDepth = it },
                                            readLevel = readLevel,
                                            onReadLevelChange = { readLevel = it },
                                            teachingStyle = teachingStyle,
                                            onTeachingStyleChange = { teachingStyle = it },
                                            imgDensity = imgDensity,
                                            onImgDensityChange = { imgDensity = it },
                                            includeQuiz = toggleIncludeQuizzes,
                                            onIncludeQuizChange = { toggleIncludeQuizzes = it },
                                            includeFlashcards = toggleIncludeFlashcards,
                                            onIncludeFlashcardsChange = { toggleIncludeFlashcards = it },
                                            onGenerate = {
                                                viewModel.generateCustomBookPath(
                                                    title = pasteTitle,
                                                    author = pasteAuthor,
                                                    category = pasteCategory,
                                                    content = pasteContentText,
                                                    depth = expDepth,
                                                    readLevel = readLevel,
                                                    teachingStyle = teachingStyle,
                                                    imgDensity = imgDensity,
                                                    includeQuiz = toggleIncludeQuizzes,
                                                    includeFlashcards = toggleIncludeFlashcards
                                                )
                                                // Reset fields
                                                pasteTitle = ""
                                                pasteAuthor = ""
                                                pasteCategory = ""
                                                pasteContentText = ""
                                                ocrPhotoCaptured = false
                                                selectedPublicBookId = null
                                                // Navigate to active generation
                                                selectedReaderChapterId = null
                                                activeReaderTab = "content"
                                                appWorkflowState = "reader"
                                            }
                                        )
                                    }

                                    "learn" -> {
                                        LearnHubView(
                                            books = books,
                                            onOpenQuiz = { book ->
                                                viewModel.selectBook(book)
                                                selectedReaderChapterId = null
                                                activeReaderTab = "quiz"
                                                appWorkflowState = "reader"
                                            },
                                            onOpenFlashcards = { book ->
                                                viewModel.selectBook(book)
                                                selectedReaderChapterId = null
                                                activeReaderTab = "flashcards"
                                                appWorkflowState = "reader"
                                            },
                                            onOpenMindMap = { book ->
                                                viewModel.selectBook(book)
                                                selectedReaderChapterId = null
                                                activeReaderTab = "mindmap"
                                                appWorkflowState = "reader"
                                            }
                                        )
                                    }

                                    "profile" -> {
                                        ProfileView(
                                            nickname = nickname,
                                            studyMinutes = studyMinutesToday,
                                            studyGoal = studyGoalMinutes,
                                            themeName = appThemeStyle,
                                            onThemeChange = { appThemeStyle = it },
                                            isDark = isDarkTheme,
                                            onDarkChange = { isDarkTheme = it },
                                            onViewCertificate = { selectedCertificateToView = it }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "reader" -> {
                        selectedBook?.let { book ->
                            ReaderScreenView(
                                book = book,
                                chapters = chapters,
                                highlights = highlights,
                                chatMessages = chatMessages,
                                flashcards = flashcards,
                                viewModel = viewModel,
                                activeTab = activeReaderTab,
                                onTabChange = { activeReaderTab = it },
                                selectedChapterId = selectedReaderChapterId,
                                onSelectChapter = { selectedReaderChapterId = it },
                                onBackClick = { appWorkflowState = "main" },
                                fontSize = readerFontSize,
                                onFontSizeChange = { readerFontSize = it },
                                bookmarkedChapters = bookmarkedChapters,
                                onToggleBookmark = { id ->
                                    bookmarkedChapters = if (bookmarkedChapters.contains(id)) {
                                        bookmarkedChapters - id
                                    } else {
                                        bookmarkedChapters + id
                                    }
                                },
                                showHighlightDialog = showHighlightDialog,
                                onShowHighlightDialogChange = { showHighlightDialog = it },
                                highlightText = selectedHighlightText,
                                onHighlightTextChange = { selectedHighlightText = it },
                                highlightNote = highlightNoteText,
                                onHighlightNoteChange = { highlightNoteText = it },
                                mindMapScale = mindMapScale,
                                onMindMapScaleChange = { mindMapScale = it },
                                expandedConcept = expandedConceptNode,
                                onExpandedConceptChange = { expandedConceptNode = it },
                                expDepth = expDepth
                            )
                        } ?: run {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No syllabus module loaded.")
                            }
                        }
                    }
                }

                // AI SYSTEM DIALOGS, OVERLAYS & ERRORS
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(16.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Synergizing Deep Knowledge",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = loadingMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Real-time Simulated Terminal Output Stage
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .background(Color.Black, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "[AI Core]: Analyzing literature structures...\n[Synthesizer]: Generating chapter revisions and flashcards...\n[Progress]: Stage active. Computing standard node maps...",
                                        color = Color.Green,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                errorMessage?.let { error ->
                    AlertDialog(
                        onDismissRequest = { viewModel.clearError() },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Model Response Warning")
                            }
                        },
                        text = {
                            Text(
                                text = if (error.contains("API Key", ignoreCase = true)) {
                                    "Your GEMINI_API_KEY is currently empty.\n\nPlease define your API Key securely in AI Studio Secrets panel so deep syllabus generation, tutor dialogues, and quizzes work perfectly."
                                } else {
                                    error
                                }
                            )
                        },
                        confirmButton = {
                            Button(onClick = { viewModel.clearError() }) {
                                Text("Acknowledge")
                            }
                        }
                    )
                }

                if (showForgotPasswordDialog) {
                    AlertDialog(
                        onDismissRequest = { showForgotPasswordDialog = false },
                        title = { Text("Transmitting Recovery Key") },
                        text = {
                            Text("A dynamic deep-link verification token has been dispatched to $loginEmail for instant secure verification.")
                        },
                        confirmButton = {
                            Button(onClick = { showForgotPasswordDialog = false }) {
                                Text("Done")
                            }
                        }
                    )
                }

                selectedCertificateToView?.let { certTitle ->
                    CertificateDialog(
                        userName = nickname,
                        bookTitle = certTitle,
                        onDismiss = { selectedCertificateToView = null }
                    )
                }
            }
        }
    }
}

// ======================== SUB-VIEW COMPOSABLES ========================

// --- 1. SPLASH SCREEN ---
@Composable
fun SplashScreenView(onContinue: () -> Unit, onSkip: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative cosmic orbits
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.Cyan.copy(alpha = 0.05f),
                radius = size.width * 0.35f,
                center = Offset(size.width * 0.5f, size.height * 0.45f)
            )
            drawCircle(
                color = Color.Magenta.copy(alpha = 0.03f),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.5f, size.height * 0.45f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Cyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Cosmic Mind Logo",
                    tint = Color.Cyan,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "DeepStudy AI",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "The World's Best AI Deep Explainer",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.Cyan,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color.Cyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp)
            ) {
                Text("Begin Exploration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onSkip) {
                Text("Skip to Dashboard", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
    }
}

// --- 2. ONBOARDING SCREEN ---
@Composable
fun OnboardingScreenView(
    pages: List<OnboardingData>,
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    val page = pages[currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSkip) {
                    Text("Skip", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            // Interactive illustration panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(colors = page.gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(96.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(
                    text = page.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = page.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Dot indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed { idx, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (idx == currentPage) 16.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (idx == currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Bottom controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    TextButton(onClick = { onPageChange(currentPage - 1) }) {
                        Text("Back", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Button(
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            onPageChange(currentPage + 1)
                        } else {
                            onFinish()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (currentPage == pages.size - 1) "Get Started" else "Next",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// --- 3. LOGIN & SIGN-UP SCREEN ---
@Composable
fun LoginScreenView(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isSignUpMode) "Establish Deep Portal" else "Syllabus Explorer Entry",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isSignUpMode) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Your Academic Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Educational Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("login_email_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Vault Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("login_password_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!isSignUpMode) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onForgotPasswordClick) {
                        Text("Forgot Key?", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onLoginSuccess(if (isSignUpMode) displayName else email.substringBefore("@")) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("login_action_button")
            ) {
                Text(
                    text = if (isSignUpMode) "Synthesize Profile" else "Authorize Secure Entry",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Login Button (Simulated)
            OutlinedButton(
                onClick = { onLoginSuccess("Academic Google Scholar") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Google",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Sync with Google Workspace", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Anonymous Guest Mode
            TextButton(onClick = { onLoginSuccess("Anonymous Guest") }) {
                Text("Proceed in Anonymous Guest Mode", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isSignUpMode) "Already registered?" else "New scholar?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { isSignUpMode = !isSignUpMode }) {
                    Text(
                        text = if (isSignUpMode) "Sign In instead" else "Create account",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// --- 4. HOME DASHBOARD ---
@Composable
fun HomeDashboardView(
    books: List<Book>,
    nickname: String,
    studyMinutes: Int,
    studyGoal: Int,
    onLogMinute: () -> Unit,
    onBookSelect: (Book) -> Unit,
    onSyllabusBuilderClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Welcome Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Good day, $nickname!",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Continuous mastery yields ultimate retention.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = nickname.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Daily Progress & Goals with Streak calendar
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Mastery Goal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "$studyMinutes / $studyGoal min active focus",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Spark Streak Icon
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("7 Days Streak", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFFFF9800))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { studyMinutes.toFloat() / studyGoal.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Streak Calendar Days
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val days = listOf("M", "T", "W", "T", "F", "S", "S")
                        days.forEachIndexed { index, d ->
                            val active = index < 6 || studyMinutes >= studyGoal
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = d,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onLogMinute,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log 5 Mins Interactive Study", fontSize = 12.sp)
                    }
                }
            }
        }

        // Weekly Study Hours custom chart drawn with Canvas
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Weekly Knowledge Curve", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        val chartHeight = size.height
                        val totalBars = 7
                        val barWidth = size.width / (totalBars * 2)
                        val barSpacing = size.width / (totalBars * 2)

                        val dataPoints = listOf(0.4f, 0.6f, 0.3f, 0.8f, 0.5f, 0.9f, 0.7f) // % of height

                        dataPoints.forEachIndexed { i, pt ->
                            val x = i * (barWidth + barSpacing) + barSpacing / 2
                            val h = pt * chartHeight
                            val y = chartHeight - h

                            drawRoundRect(
                                color = Color(0xFF1E88E5),
                                topLeft = Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(barWidth, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                            Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Continue Learning Header & Card
        item {
            Text("Resume Study Program", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        item {
            if (books.isNotEmpty()) {
                val latestBook = books.first()
                Card(
                    onClick = { onBookSelect(latestBook) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(latestBook.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Category: ${latestBook.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { latestBook.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("${latestBook.progressPercent}%", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onSyllabusBuilderClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("No modules drafted. Tap to build custom syllabus now!", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Recommended Books & Trending Public Classics
        item {
            Text("Classic Public Domain Catalogs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val mockClassics = listOf(
                    MockCatalog("The Republic", "Plato", "Philosophy", Color(0xFF673AB7)),
                    MockCatalog("Sapiens: A Brief History", "Yuval Noah Harari", "Anthropology", Color(0xFFE91E63)),
                    MockCatalog("Cosmos", "Carl Sagan", "Science", Color(0xFF3F51B5)),
                    MockCatalog("The Art of War", "Sun Tzu", "Strategy", Color(0xFF009688))
                )

                mockClassics.forEach { cls ->
                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .height(180.dp),
                        shape = RoundedCornerShape(14.dp),
                        onClick = onSyllabusBuilderClick
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(colors = listOf(cls.color, cls.color.copy(alpha = 0.6f))))
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(cls.cat, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(cls.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }

                                Text("by ${cls.author}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 5. LIBRARY VIEW ---
@Composable
fun LibraryView(
    books: List<Book>,
    selectedTab: String,
    onTabChange: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterState: String,
    onFilterChange: (String) -> Unit,
    onBookSelect: (Book) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search title, author, topic...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("library_search_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs Row: My Books, Favorites, Downloads, Public, Collections
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("My Books", "Favorites", "Downloads", "Public Library", "Collections")
            tabs.forEach { tb ->
                FilterChip(
                    selected = selectedTab == tb,
                    onClick = { onTabChange(tb) },
                    label = { Text(tb) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Filter Chips: All, In Progress, Completed
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val filters = listOf("All", "In Progress", "Completed")
            filters.forEach { fl ->
                FilterChip(
                    selected = filterState == fl,
                    onClick = { onFilterChange(fl) },
                    label = { Text(fl) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic filtered book list
        val filteredList = books.filter {
            (it.title.contains(searchQuery, ignoreCase = true) || it.author.contains(searchQuery, ignoreCase = true)) &&
                    (filterState == "All" ||
                            (filterState == "Completed" && it.progressPercent >= 100) ||
                            (filterState == "In Progress" && it.progressPercent in 1..99))
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No syllabi fit the query.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList) { book ->
                    Card(
                        onClick = { onBookSelect(book) },
                        modifier = Modifier.fillMaxWidth().testTag("library_book_card_${book.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Book, contentDescription = null, tint = Color.White)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("by ${book.author}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = { book.progressPercent / 100f },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(5.dp)
                                            .clip(CircleShape),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${book.progressPercent}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Actions",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 6. CREATE BOOK VIEW & AI SETTINGS ---
@Composable
fun CreateBookView(
    importType: String,
    onImportTypeChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    author: String,
    onAuthorChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    ocrCaptured: Boolean,
    onOcrCapture: () -> Unit,
    ocrScanning: Boolean,
    onOcrReset: () -> Unit,
    selectedPublicId: Int?,
    onSelectPublicId: (id: Int, title: String, author: String, cat: String) -> Unit,
    // AI configuration parameters
    expDepth: String,
    onDepthChange: (String) -> Unit,
    readLevel: String,
    onReadLevelChange: (String) -> Unit,
    teachingStyle: String,
    onTeachingStyleChange: (String) -> Unit,
    imgDensity: String,
    onImgDensityChange: (String) -> Unit,
    includeQuiz: Boolean,
    onIncludeQuizChange: (Boolean) -> Unit,
    includeFlashcards: Boolean,
    onIncludeFlashcardsChange: (Boolean) -> Unit,
    onGenerate: () -> Unit
) {
    var screenStage by remember { mutableStateOf("input") } // "input" or "settings"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (screenStage == "input") {
            Text(
                text = "Draft Knowledge Module",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Select literature source or specify parameters for synthesis.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val imports = listOf("paste" to "Paste Text", "ocr" to "Camera Scan", "public" to "Public Classics")
                imports.forEach { (key, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (importType == key) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onImportTypeChange(key) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (importType == key) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (importType) {
                "paste" -> {
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text("Academic Title *") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("create_title_field")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = author,
                        onValueChange = onAuthorChange,
                        label = { Text("Author / Source Organization") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = category,
                        onValueChange = onCategoryChange,
                        label = { Text("Discipline (e.g., Neuroscience, Physics)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = onContentChange,
                        label = { Text("Paste core text syllabus blueprint (optional)") },
                        minLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "ocr" -> {
                    if (!ocrCaptured) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                if (ocrScanning) {
                                    CircularProgressIndicator(color = Color.Green)
                                    Text(
                                        "Scanning text layers...",
                                        color = Color.Green,
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Focus camera on book page or document", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onOcrCapture,
                            enabled = !ocrScanning,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simulate Camera Snapshot (OCR Scan)")
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("OCR Scan Completed", fontWeight = FontWeight.Bold, color = Color.Green)
                                    TextButton(onClick = onOcrReset) { Text("Reset") }
                                }
                                Text("Captured Text Output:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                "public" -> {
                    val publicBooks = listOf(
                        101 to Triple("The Republic", "Plato", "Philosophy"),
                        102 to Triple("Sapiens", "Yuval Noah Harari", "Science"),
                        103 to Triple("Cosmos", "Carl Sagan", "Astronomy"),
                        104 to Triple("The Art of War", "Sun Tzu", "History")
                    )

                    publicBooks.forEach { (id, info) ->
                        val selected = selectedPublicId == id
                        Card(
                            onClick = { onSelectPublicId(id, info.first, info.second, info.third) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(info.first, fontWeight = FontWeight.Bold)
                                    Text("by ${info.second} • ${info.third}", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { screenStage = "settings" },
                enabled = title.isNotBlank() || selectedPublicId != null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("next_to_settings_button")
            ) {
                Text("Proceed to Generation Settings", fontWeight = FontWeight.Bold)
            }
        } else {
            // "settings" STAGE
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { screenStage = "input" }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Generation Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explanation Depth
            Text("Explanation Depth Level", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Beginner", "Intermediate", "Advanced", "Researcher").forEach { dp ->
                    FilterChip(
                        selected = expDepth == dp,
                        onClick = { onDepthChange(dp) },
                        label = { Text(dp, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reading Level
            Text("Target Reading Level", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("High School", "Undergraduate", "Ph.D.").forEach { rl ->
                    FilterChip(
                        selected = readLevel == rl,
                        onClick = { onReadLevelChange(rl) },
                        label = { Text(rl, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Teaching Style
            Text("Teaching Methodology Style", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Socratic Method", "Analogical", "Case-Study").forEach { ts ->
                    FilterChip(
                        selected = teachingStyle == ts,
                        onClick = { onTeachingStyleChange(ts) },
                        label = { Text(ts, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Include Comprehensive Quizzes", fontSize = 13.sp)
                Switch(checked = includeQuiz, onCheckedChange = onIncludeQuizChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pre-compile Revision Flashcards", fontSize = 13.sp)
                Switch(checked = includeFlashcards, onCheckedChange = onIncludeFlashcardsChange)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    screenStage = "input"
                    onGenerate()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("trigger_synthesis_button")
            ) {
                Text("Synthesize & Launch Course", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- 7. CENTRAL LEARN HUB ---
@Composable
fun LearnHubView(
    books: List<Book>,
    onOpenQuiz: (Book) -> Unit,
    onOpenFlashcards: (Book) -> Unit,
    onOpenMindMap: (Book) -> Unit
) {
    var activeSubView by remember { mutableStateOf("directory") } // "directory", "analytics"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Central Learn Directory",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge
            )

            TextButton(
                onClick = {
                    activeSubView = if (activeSubView == "directory") "analytics" else "directory"
                }
            ) {
                Text(if (activeSubView == "directory") "View Analytics" else "Back to Core Hub")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeSubView == "directory") {
            if (books.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Draft custom books first to active active study training hubs.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Active Syllabus Hubs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    items(books) { book ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(book.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("by ${book.author}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { onOpenQuiz(book) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Quiz Hub", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { onOpenFlashcards(book) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                    ) {
                                        Icon(Icons.Default.AmpStories, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Flashcards", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { onOpenMindMap(book) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                                    ) {
                                        Icon(Icons.Default.DeviceHub, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mind Map", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // STUDY ANALYTICS SCREEN
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Academics Deep Diagnostics", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Retention Coefficient", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("84.5%", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Calculated from MCQ revisions and flipped card spacing.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Revision Mastery Ratio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("12 / 15 chapters mastered", fontSize = 14.sp)
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Target Weak Disciplines", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("• Plato's Republic: Allegory of Cave nuance questions failed.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("• Astronomy: Cosmic distance scale computations require review.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// --- 8. PROFILE VIEW ---
@Composable
fun ProfileView(
    nickname: String,
    studyMinutes: Int,
    studyGoal: Int,
    themeName: String,
    onThemeChange: (String) -> Unit,
    isDark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onViewCertificate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // User Profile Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        nickname.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(nickname, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("Elite Explainer Level 5 Scholar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Goals Tracker
        Text("Academics Learning Objectives", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Syllabus Mastery Goal: Complete 3 Books", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                LinearProgressIndicator(
                    progress = { 0.67f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary
                )
                Text("2 of 3 Books completed.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Certificates List
        Text("Certificates of Mastery", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        val sampleCertificates = listOf("Sapiens Master Program", "Classical Philosophy Synthesis")
        sampleCertificates.forEach { cert ->
            Card(
                onClick = { onViewCertificate(cert) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CardMembership, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(cert, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Issued with Distinction by AI Explainer Core.", fontSize = 10.sp)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Settings Block
        Text("App Configurations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Theme Toggle
                Text("Active Color Palette", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Cobalt Star", "Emerald Forest", "Rose Gold").forEach { th ->
                        FilterChip(
                            selected = themeName == th,
                            onClick = { onThemeChange(th) },
                            label = { Text(th) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Deep Space Dark Theme", fontSize = 13.sp)
                    Switch(checked = isDark, onCheckedChange = onDarkChange)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Interactive Study Push Notifications", fontSize = 13.sp)
                    Switch(checked = true, onCheckedChange = {})
                }
            }
        }
    }
}

// --- 9. CERTIFICATE DISPLAY DIALOG ---
@Composable
fun CertificateDialog(userName: String, bookTitle: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(3.dp, Color(0xFFD4AF37)), // Gold border
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CardMembership,
                    contentDescription = null,
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "CERTIFICATE OF MASTERY",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFFD4AF37),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "This is officially academic certification that",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "has successfully completed and mastered the deep syllabus curriculum for",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "\"$bookTitle\"",
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    color = Color.Cyan,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Verified by DeepStudy AI Explainer System",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Download & Export PDF", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- 10. READER SCREEN VIEW & CORRESPONDING TOOLS ---
@Composable
fun ReaderScreenView(
    book: Book,
    chapters: List<Chapter>,
    highlights: List<Highlight>,
    chatMessages: List<ChatMessage>,
    flashcards: List<Flashcard>,
    viewModel: StudyViewModel,
    activeTab: String,
    onTabChange: (String) -> Unit,
    selectedChapterId: Int?,
    onSelectChapter: (Int?) -> Unit,
    onBackClick: () -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onFontSizeChange: (androidx.compose.ui.unit.TextUnit) -> Unit,
    bookmarkedChapters: Set<Int>,
    onToggleBookmark: (Int) -> Unit,
    showHighlightDialog: Chapter?,
    onShowHighlightDialogChange: (Chapter?) -> Unit,
    highlightText: String,
    onHighlightTextChange: (String) -> Unit,
    highlightNote: String,
    onHighlightNoteChange: (String) -> Unit,
    mindMapScale: Float,
    onMindMapScaleChange: (Float) -> Unit,
    expandedConcept: String?,
    onExpandedConceptChange: (String?) -> Unit,
    expDepth: String
) {
    val activeChapter = chapters.firstOrNull { it.id == selectedChapterId } ?: chapters.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        // Upper Top Bar with standard metadata info
        TopAppBar(
            title = {
                Text(
                    text = book.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Font Size Selector Action button
                IconButton(
                    onClick = {
                        val currentSize = fontSize.value
                        val newSize = if (currentSize >= 22f) 14f else currentSize + 2f
                        onFontSizeChange(newSize.sp)
                    }
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = "Font size")
                }

                activeChapter?.let { ch ->
                    val isBookmarked = bookmarkedChapters.contains(ch.id)
                    IconButton(onClick = { onToggleBookmark(ch.id) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        )

        // Navigation sub-tabs inside Reader
        ScrollableTabRow(
            selectedTabIndex = when (activeTab) {
                "content" -> 0
                "tutor" -> 1
                "quiz" -> 2
                "flashcards" -> 3
                "mindmap" -> 4
                else -> 5
            },
            divider = {},
            edgePadding = 8.dp
        ) {
            val readerTabs = listOf(
                "content" to "Syllabus",
                "tutor" to "AI Tutor",
                "quiz" to "Quizzes",
                "flashcards" to "Revision Cards",
                "mindmap" to "Mind Map",
                "export" to "Export Cert"
            )

            readerTabs.forEachIndexed { idx, (key, label) ->
                Tab(
                    selected = activeTab == key,
                    onClick = {
                        onTabChange(key)
                        if (key == "quiz" && activeChapter != null) {
                            viewModel.startChapterQuiz(activeChapter)
                        } else if (key == "flashcards" && activeChapter != null) {
                            viewModel.loadOrGenerateFlashcards(activeChapter)
                        }
                    },
                    text = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // Outer Content router based on active tab
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                "content" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Chapter selector top header
                        if (chapters.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chapters.forEach { ch ->
                                    val isSelected = (activeChapter?.id == ch.id)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSelectChapter(ch.id) },
                                        label = { Text("Ch ${ch.orderIndex}") },
                                        modifier = Modifier.testTag("chapter_selector_chip_${ch.id}")
                                    )
                                }
                            }
                        }

                        activeChapter?.let { ch ->
                            ChapterReaderContentView(
                                chapter = ch,
                                fontSize = fontSize,
                                highlights = highlights.filter { it.chapterId == ch.id },
                                onAddHighlightClick = { onShowHighlightDialogChange(ch) },
                                onTextHighlightSelected = { onHighlightTextChange(it) },
                                onToggleCompleted = { viewModel.toggleChapterCompletion(ch) },
                                viewModel = viewModel,
                                expDepth = expDepth
                            )
                        }
                    }
                }

                "tutor" -> {
                    ChatTab(chatMessages = chatMessages, viewModel = viewModel)
                }

                "quiz" -> {
                    activeChapter?.let {
                        QuizTab(viewModel = viewModel)
                    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active chapter loaded for Quiz.")
                    }
                }

                "flashcards" -> {
                    activeChapter?.let {
                        FlashcardsTab(flashcards = flashcards, viewModel = viewModel)
                    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active chapter loaded for Flashcards.")
                    }
                }

                "mindmap" -> {
                    activeChapter?.let { ch ->
                        MindMapTab(
                            chapter = ch,
                            scale = mindMapScale,
                            onScaleChange = onMindMapScaleChange,
                            expandedConcept = expandedConcept,
                            onExpandedConceptChange = onExpandedConceptChange
                        )
                    }
                }

                "export" -> {
                    ExportOptionsView(
                        book = book,
                        chapters = chapters,
                        onGenerateCertificate = { onBackClick() }
                    )
                }
            }
        }
    }

    // Annotation highlight dialogue overlay
    if (showHighlightDialog != null) {
        AlertDialog(
            onDismissRequest = { onShowHighlightDialogChange(null) },
            title = { Text("Annotate Learning Selection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Highlighted Selection:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                            .padding(8.dp)
                    ) {
                        Text(highlightText, fontStyle = FontStyle.Italic, fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = highlightNote,
                        onValueChange = onHighlightNoteChange,
                        label = { Text("Personal Learning Annotation / Note") },
                        modifier = Modifier.fillMaxWidth().testTag("highlight_note_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveHighlight(showHighlightDialog, highlightText, highlightNote)
                        onShowHighlightDialogChange(null)
                        onHighlightTextChange("")
                        onHighlightNoteChange("")
                    },
                    modifier = Modifier.testTag("submit_highlight_button")
                ) {
                    Text("Save Highlight")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowHighlightDialogChange(null) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- 11. SUB-VIEW: CHAPTER READING VIEW ---
@Composable
fun ChapterReaderContentView(
    chapter: Chapter,
    fontSize: androidx.compose.ui.unit.TextUnit,
    highlights: List<Highlight>,
    onAddHighlightClick: () -> Unit,
    onTextHighlightSelected: (String) -> Unit,
    onToggleCompleted: () -> Unit,
    viewModel: StudyViewModel,
    expDepth: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Chapter Heading Info with checked completed state indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Chapter ${chapter.orderIndex}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = chapter.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Checkbox(
                checked = chapter.isCompleted,
                onCheckedChange = { onToggleCompleted() },
                modifier = Modifier.testTag("chapter_completed_checkbox")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Generated Custom Diagram drawn on Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                        )
                    ),
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Drawing nodes representation diagram
                drawCircle(color = Color(0xFF1E88E5), radius = 24f, center = Offset(size.width * 0.25f, size.height * 0.5f))
                drawCircle(color = Color(0xFF00ACC1), radius = 24f, center = Offset(size.width * 0.5f, size.height * 0.5f))
                drawCircle(color = Color(0xFF673AB7), radius = 24f, center = Offset(size.width * 0.75f, size.height * 0.5f))

                drawLine(
                    color = Color.Gray.copy(alpha = 0.6f),
                    start = Offset(size.width * 0.25f + 24f, size.height * 0.5f),
                    end = Offset(size.width * 0.5f - 24f, size.height * 0.5f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color.Gray.copy(alpha = 0.6f),
                    start = Offset(size.width * 0.5f + 24f, size.height * 0.5f),
                    end = Offset(size.width * 0.75f - 24f, size.height * 0.5f),
                    strokeWidth = 3f
                )
            }

            Text(
                "Dynamic Structural Node Matrix: $expDepth Synthesis",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Premium Highlight & Annotate Simulator Tools
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Review Texts", fontWeight = FontWeight.Bold, fontSize = 13.sp)

            TextButton(
                onClick = {
                    onTextHighlightSelected("Active Core Theory")
                    onAddHighlightClick()
                },
                modifier = Modifier.testTag("simulate_highlight_trigger")
            ) {
                Icon(Icons.Default.BorderColor, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Highlight 'Active Core Theory'", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom summary callout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Deep Summary", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(
                        chapter.summary,
                        fontSize = fontSize,
                        lineHeight = (fontSize.value * 1.5).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chapter Core concepts display
        Text("Syllabus Terminology Matrix", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = chapter.coreConcepts,
            fontSize = fontSize,
            lineHeight = (fontSize.value * 1.5).sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (highlights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Your Highlights & Tutor Explanations", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            highlights.forEach { h ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Selection: \"${h.text}\"", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            IconButton(onClick = { viewModel.deleteHighlight(h.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                            }
                        }
                        if (h.note.isNotBlank()) {
                            Text("Your Note: ${h.note}", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("AI Tutor Explanation: ${h.explanation}", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// --- 12. SUB-VIEW: AI TUTOR CHAT ---
@Composable
fun ChatTab(
    chatMessages: List<ChatMessage>,
    viewModel: StudyViewModel
) {
    var userText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Ask the Academic Tutor any questions about concept definitions, analogies, or translations.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(chatMessages) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 0.dp,
                                        bottomEnd = if (isUser) 0.dp else 16.dp
                                    )
                                )
                                .background(
                                    if (isUser) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg.content,
                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        Divider()

        // Quick tutor prompts suggestions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tutorPrompts = listOf(
                "Analogy?" to "Give me an intuitive analogy of the concepts discussed.",
                "Simplify?" to "Explain this chapter to me like I am 10 years old.",
                "Translate?" to "Translate the core concept summary into simple French."
            )

            tutorPrompts.forEach { (label, promptText) ->
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.sendChatMessage(promptText)
                            scope.launch {
                                delay(100)
                                if (chatMessages.isNotEmpty()) {
                                    listState.animateScrollToItem(chatMessages.size - 1)
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        // Input send box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userText,
                onValueChange = { userText = it },
                placeholder = { Text("Ask deep questions...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (userText.isNotBlank()) {
                        viewModel.sendChatMessage(userText)
                        userText = ""
                        scope.launch {
                            delay(200)
                            if (chatMessages.isNotEmpty()) {
                                listState.animateScrollToItem(chatMessages.size - 1)
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("chat_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// --- 13. SUB-VIEW: QUIZ CENTER ---
@Composable
fun QuizTab(
    viewModel: StudyViewModel
) {
    val quizQuestions by viewModel.activeQuiz.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentQuizIndex.collectAsStateWithLifecycle()
    val score by viewModel.quizScore.collectAsStateWithLifecycle()
    val quizCompleted by viewModel.quizCompleted.collectAsStateWithLifecycle()
    val selectedAnswer by viewModel.selectedAnswer.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (quizQuestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Preparing multiple choice quizzes...")
                }
            }
        } else if (quizCompleted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Success",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Evaluation Completed!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Score: $score / ${quizQuestions.size}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            val q = quizQuestions[currentIndex]

            Text(
                text = "Question ${currentIndex + 1} of ${quizQuestions.size}",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = q.questionText,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            val choices = listOf(
                "A" to q.optionA,
                "B" to q.optionB,
                "C" to q.optionC,
                "D" to q.optionD
            )

            choices.forEach { (key, label) ->
                val isSelected = selectedAnswer == key
                val isCorrect = key == q.correctAnswer
                val highlightColor = when {
                    selectedAnswer == null -> MaterialTheme.colorScheme.surface
                    isSelected && isCorrect -> Color.Green.copy(alpha = 0.2f)
                    isSelected -> Color.Red.copy(alpha = 0.2f)
                    isCorrect -> Color.Green.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surface
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("quiz_choice_$key"),
                    onClick = { viewModel.selectQuizAnswer(key) },
                    colors = CardDefaults.cardColors(containerColor = highlightColor),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "$key. $label",
                        modifier = Modifier.padding(14.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (selectedAnswer != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("AI Revision Explanation:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(q.explanation, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.nextQuizQuestion() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("next_quiz_question_button")
                ) {
                    Text("Advance")
                }
            }
        }
    }
}

// --- 14. SUB-VIEW: FLASHCARDS TRAINER ---
@Composable
fun FlashcardsTab(
    flashcards: List<Flashcard>,
    viewModel: StudyViewModel
) {
    var cardIndex by remember { mutableStateOf(0) }
    var flipped by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (flashcards.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val card = flashcards[cardIndex]

            Text(
                "Card ${cardIndex + 1} of ${flashcards.size}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Card Flipper Panel
            Card(
                onClick = { flipped = !flipped },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(240.dp)
                    .testTag("flashcard_body"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (flipped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (flipped) "ANSWER" else "TERM CONCEPT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (flipped) card.back else card.front,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Spaced Repetition Buttons
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        flipped = false
                        cardIndex = (cardIndex + 1) % flashcards.size
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Text("Hard")
                }

                Button(
                    onClick = {
                        flipped = false
                        cardIndex = (cardIndex + 1) % flashcards.size
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow.copy(alpha = 0.8f), contentColor = Color.Black)
                ) {
                    Text("Medium")
                }

                Button(
                    onClick = {
                        flipped = false
                        viewModel.toggleFlashcardMastered(card)
                        cardIndex = (cardIndex + 1) % flashcards.size
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.8f))
                ) {
                    Text("Easy (Mastered)")
                }
            }
        }
    }
}

// --- 15. SUB-VIEW: INTERACTIVE MIND MAP ---
data class ParsedConcept(
    val name: String,
    val description: String,
    val analogy: String = "",
    val visualIllustration: String = "",
    val selfCheck: String = ""
)

fun parseConcepts(coreConceptsText: String): List<ParsedConcept> {
    if (coreConceptsText.isBlank()) return emptyList()
    
    // Split by bullet points or asterisks
    val segments = coreConceptsText.split(Regex("•|\\*\\s+")).map { it.trim() }.filter { it.isNotBlank() }
    
    val list = mutableListOf<ParsedConcept>()
    for (segment in segments) {
        val lines = segment.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) continue
        
        val firstLine = lines[0]
        var name = ""
        var mainDesc = ""
        
        // Attempt to extract name from bold markup: **Concept Name**: Description
        val boldRegex = """\*\*(.*?)\*\*(.*)""".toRegex()
        val matchResult = boldRegex.find(firstLine)
        if (matchResult != null) {
            name = matchResult.groupValues[1].trim()
            mainDesc = matchResult.groupValues[2].trim().removePrefix(":").trim()
        } else {
            val parts = firstLine.split(":", limit = 2)
            if (parts.size == 2) {
                name = parts[0].trim().replace("**", "").replace("*", "")
                mainDesc = parts[1].trim()
            } else {
                name = "Key Concept"
                mainDesc = firstLine
            }
        }
        
        var analogy = ""
        var visual = ""
        var selfCheck = ""
        
        // Parse secondary fields from subsequent lines
        for (i in 1 until lines.size) {
            val line = lines[i]
            when {
                line.contains("Analogy", ignoreCase = true) -> {
                    analogy = line.substringAfter(":").trim().removePrefix("**").removeSuffix("**")
                }
                line.contains("Visual Illustration", ignoreCase = true) || line.contains("[Visual", ignoreCase = true) -> {
                    visual = line.substringAfter("]:").substringAfter(":").trim().removePrefix("**").removeSuffix("**")
                }
                line.contains("Self-Check", ignoreCase = true) || line.contains("Self Check", ignoreCase = true) -> {
                    selfCheck = line.substringAfter(":").trim().removePrefix("**").removeSuffix("**")
                }
                else -> {
                    mainDesc += "\n" + line
                }
            }
        }
        
        name = name.removePrefix("**").removeSuffix("**").trim()
        
        list.add(
            ParsedConcept(
                name = name,
                description = mainDesc,
                analogy = analogy,
                visualIllustration = visual,
                selfCheck = selfCheck
            )
        )
    }
    return list
}

@Composable
fun MindMapTab(
    chapter: Chapter,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    expandedConcept: String?,
    onExpandedConceptChange: (String?) -> Unit
) {
    val concepts = remember(chapter.coreConcepts) { parseConcepts(chapter.coreConcepts) }
    var selfCheckAnswers by remember { mutableStateOf(mapOf<String, String>()) }
    var revealedAnswers by remember { mutableStateOf(setOf<String>()) }

    // Animation for flow indicator dots
    val infiniteTransition = rememberInfiniteTransition(label = "mindmap_flow")
    val flowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot_flow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Interactive Concept Map",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Teach for understanding • Dynamic cognitive flow",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onScaleChange((scale - 0.1f).coerceAtLeast(0.6f)) }) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Zoom out")
                }
                Text(
                    text = "${(scale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { onScaleChange((scale + 0.1f).coerceAtMost(1.6f)) }) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Zoom in")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            if (concepts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Concept extraction is processing or not available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                ) {
                    // Custom Draw Canvas Background for Node Connections
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Root center is at top center of canvas
                        val rootX = canvasWidth / 2f
                        val rootY = 60f * scale

                        // Draw branch lines to all concepts
                        val conceptCount = concepts.size
                        val startY = rootY + 40f
                        val endY = canvasHeight - 100f
                        
                        // For a vertical tree, we draw a central spine from Root down
                        val spineEndX = rootX
                        val spineEndY = canvasHeight - 50f
                        
                        // Main Spine Line
                        drawLine(
                            color = outlineVariantColor,
                            start = Offset(rootX, rootY),
                            end = Offset(spineEndX, spineEndY),
                            strokeWidth = 3f * scale,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )

                        // Animated flow dot along the main spine
                        val spineDotY = rootY + (spineEndY - rootY) * flowProgress
                        drawCircle(
                            color = primaryColor,
                            radius = 6f * scale,
                            center = Offset(rootX, spineDotY)
                        )

                        // Branch connectors
                        concepts.forEachIndexed { index, _ ->
                            // Calculate approximate vertical position of each concept node card
                            val cardY = 220f * scale + (index * 260f * scale)
                            
                            // Horizontal branch from central spine to the left/right cards
                            // If index is even, card is left, else right (or all on right for mobile layout)
                            // In this mobile friendly design, all cards are right-aligned, spine is on the left
                            val branchStartX = rootX - 100f * scale
                            val branchEndX = rootX - 30f * scale
                            
                            // Horizontal dash connection
                            drawLine(
                                color = secondaryColor.copy(alpha = 0.6f),
                                start = Offset(rootX - 120f * scale, cardY),
                                end = Offset(rootX - 145f * scale, cardY),
                                strokeWidth = 2f * scale
                            )
                        }
                    }

                    // Interactive UI Nodes Layer
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Central Chapter Root Node Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .scale(scale)
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Hub,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "CENTRAL THEME",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Child Concept Nodes List
                        concepts.forEachIndexed { index, concept ->
                            val isExpanded = expandedConcept == concept.name
                            
                            Card(
                                onClick = { onExpandedConceptChange(if (isExpanded) null else concept.name) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isExpanded) {
                                        MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    width = if (isExpanded) 2.dp else 1.dp,
                                    color = if (isExpanded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (index + 1).toString(),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = concept.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isExpanded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = concept.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        lineHeight = 16.sp,
                                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (isExpanded) {
                                        // Extended Pedagogical Sections
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // 1. Analogy Callout Box
                                        if (concept.analogy.isNotBlank()) {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color(0xFFFFFBEB).copy(alpha = 0.9f) // Warm amber light background
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lightbulb,
                                                        contentDescription = "Analogy",
                                                        tint = Color(0xFFD97706),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = "CONCEPT ANALOGY",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFFB45309),
                                                            letterSpacing = 0.5.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = concept.analogy,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color(0xFF78350F),
                                                            lineHeight = 14.sp
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }

                                        // 2. Visual Illustration Blueprint Idea
                                        if (concept.visualIllustration.isNotBlank()) {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Brush,
                                                        contentDescription = "Visual illustration prompt",
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = "VISUAL MODEL CONCEPT",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            letterSpacing = 0.5.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = concept.visualIllustration,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            lineHeight = 14.sp
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }

                                        // 3. Cognitive Science Active Recall Self-Check
                                        if (concept.selfCheck.isNotBlank()) {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Quiz,
                                                            contentDescription = "Self Check",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "ACTIVE RECALL SELF-CHECK",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            letterSpacing = 0.5.sp
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = concept.selfCheck,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        lineHeight = 14.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    // Interactive answer input
                                                    val answerText = selfCheckAnswers[concept.name] ?: ""
                                                    val isRevealed = revealedAnswers.contains(concept.name)

                                                    if (!isRevealed) {
                                                        OutlinedTextField(
                                                            value = answerText,
                                                            onValueChange = { newVal ->
                                                                selfCheckAnswers = selfCheckAnswers.toMutableMap().apply {
                                                                    put(concept.name, newVal)
                                                                }
                                                            },
                                                            placeholder = { Text("Draft your active recall answer...", fontSize = 11.sp) },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            maxLines = 2,
                                                            shape = RoundedCornerShape(8.dp),
                                                            textStyle = MaterialTheme.typography.bodySmall
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.End
                                                        ) {
                                                            Button(
                                                                onClick = {
                                                                    revealedAnswers = revealedAnswers.toMutableSet().apply {
                                                                        add(concept.name)
                                                                    }
                                                                },
                                                                shape = RoundedCornerShape(8.dp),
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                            ) {
                                                                Text("Submit & Verify", fontSize = 10.sp)
                                                            }
                                                        }
                                                    } else {
                                                        // Response feedback
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(
                                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                                                    shape = RoundedCornerShape(8.dp)
                                                                )
                                                                .border(
                                                                    width = 1.dp,
                                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                                    shape = RoundedCornerShape(8.dp)
                                                                )
                                                                .padding(10.dp)
                                                        ) {
                                                            Column {
                                                                Text(
                                                                    text = "Your drafted answer:",
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 9.sp,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                                Text(
                                                                    text = answerText.ifBlank { "(No answer entered)" },
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    fontStyle = FontStyle.Italic
                                                                )
                                                                Spacer(modifier = Modifier.height(6.dp))
                                                                Text(
                                                                    text = "Pedagogical Tip:",
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 9.sp,
                                                                    color = MaterialTheme.colorScheme.secondary
                                                                )
                                                                Text(
                                                                    text = "Compare your response to the concept description above. If you remembered the core relational rules, your recall pathway is strengthening!",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    fontSize = 11.sp,
                                                                    lineHeight = 14.sp
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        TextButton(
                                                            onClick = {
                                                                revealedAnswers = revealedAnswers.toMutableSet().apply {
                                                                    remove(concept.name)
                                                                }
                                                            },
                                                            modifier = Modifier.align(Alignment.End)
                                                        ) {
                                                            Text("Try Again", fontSize = 10.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 16. SUB-VIEW: EXPORT OPTIONS & CERTIFICATE ---
@Composable
fun ExportOptionsView(
    book: Book,
    chapters: List<Chapter>,
    onGenerateCertificate: () -> Unit
) {
    var exportFormat by remember { mutableStateOf("PDF") }
    var fontStyleChoice by remember { mutableStateOf("Editorial Serif") }
    var studentName by remember { mutableStateOf("Scholar") }
    var isCompiling by remember { mutableStateOf(false) }
    var compileStep by remember { mutableStateOf("") }
    var isCompiled by remember { mutableStateOf(false) }

    LaunchedEffect(isCompiling) {
        if (isCompiling) {
            compileStep = "[Export Agent] Bundling curated modules..."
            delay(1000)
            compileStep = "[Ebook Designer] Formatting $fontStyleChoice typography rules..."
            delay(1000)
            compileStep = "[Fact-Checking Agent] Cryptographically signing academic progress..."
            delay(1000)
            compileStep = "[Master Orchestrator] Generating Certificate of Completion..."
            delay(1000)
            isCompiling = false
            isCompiled = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isCompiled) {
            Text(
                "Export Curriculum Portfolio",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Student Name Input
            OutlinedTextField(
                value = studentName,
                onValueChange = { studentName = it },
                label = { Text("Student Name for Certificate") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Formats Select
            Text(
                "Choose Target Format",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("PDF", "EPUB", "Markdown").forEach { fmt ->
                    FilterChip(
                        selected = exportFormat == fmt,
                        onClick = { exportFormat = fmt },
                        label = { Text(fmt) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Typography Selection
            Text(
                "Reader Editorial Typography",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Editorial Serif", "Grotesque Clean", "Developer Monospace").forEach { fst ->
                    FilterChip(
                        selected = fontStyleChoice == fst,
                        onClick = { fontStyleChoice = fst },
                        label = { Text(fst) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isCompiling) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    compileStep,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic
                )
            } else {
                Button(
                    onClick = { isCompiling = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compile & Export Study Package")
                }
            }
        } else {
            // Certificate design
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Certificate Badge/Seal
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Gold Medal",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "CERTIFICATE OF MASTERY",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "ACADEMIC CURRICULUM PORTFOLIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "This credential certifies that",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        studentName.uppercase(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "has successfully mastered the custom compiled curriculum program of",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        book.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        "by ${book.author}",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Spacer(modifier = Modifier.height(12.dp))

                    // Verification details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("DATE OF ISSUANCE", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("June 28, 2026", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("AI STUDY PLATFORM ID", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("SHA256: 8F${book.id}A9B${chapters.size}CD", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Study package exported successfully as a pristine $exportFormat document using $fontStyleChoice styles.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { isCompiled = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Design New Export Portfolio")
            }
        }
    }
}
