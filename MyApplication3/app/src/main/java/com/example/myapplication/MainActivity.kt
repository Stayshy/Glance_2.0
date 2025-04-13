package com.example.myapplication
import android.widget.Toast
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                CameraScreen(
                    onExitClick = {
                        // Завершаем приложение только по кнопке "Выход"
                        finish()
                    }
                )
            }
        }
    }
}
