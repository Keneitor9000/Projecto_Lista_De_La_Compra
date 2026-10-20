package org.insbaixcamp.lista_de_la_compra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.analytics.FirebaseAnalytics
import org.insbaixcamp.lista_de_la_compra.ui.theme.Lista_De_La_CompraTheme

class MainActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener la instancia de FirebaseAnalytics.
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Registrar un evento SELECT_ITEM en Firebase Analytics
        val itemBundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, "id_1")
            putString(FirebaseAnalytics.Param.ITEM_NAME, "ejemplo")
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image")
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM, itemBundle)

        enableEdgeToEdge()
        setContent {
            Lista_De_La_CompraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Lista_De_La_CompraTheme {
        Greeting("Android")
    }
}