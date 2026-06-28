package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AppDatabase
import com.example.data.StudyRepository
import com.example.ui.StudyApp
import com.example.ui.StudyViewModel
import com.example.ui.StudyViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Local DB and Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = StudyRepository(database.studyDao())

        // Instantiate Viewmodel with custom Factory
        val viewModel: StudyViewModel by viewModels {
            StudyViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                StudyApp(viewModel = viewModel)
            }
        }
    }
}
