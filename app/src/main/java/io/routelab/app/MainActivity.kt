package io.routelab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.routelab.app.presentation.RouteLabScreen
import io.routelab.app.presentation.RouteLabViewModel
import io.routelab.app.presentation.theme.RouteLabTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RouteLabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RouteLabTheme {
                RouteLabScreen(viewModel = viewModel)
            }
        }
    }
}
