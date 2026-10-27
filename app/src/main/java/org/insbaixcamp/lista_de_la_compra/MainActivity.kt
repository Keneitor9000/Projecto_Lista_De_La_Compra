package org.insbaixcamp.lista_de_la_compra

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import org.insbaixcamp.lista_de_la_compra.ui.ShoppingAppScreen
import org.insbaixcamp.lista_de_la_compra.ui.theme.Lista_De_La_CompraTheme

class MainActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private var currentUserState by mutableStateOf<FirebaseUser?>(null)
    private var demoUserEmailState by mutableStateOf<String?>(null)

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Inicializar Firebase Auth
        auth = Firebase.auth

        // Inicializar Credential Manager
        credentialManager = CredentialManager.create(this)

        // Registrar evento inicial de prueba en Analytics
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
                    MainScreen(
                        user = currentUserState,
                        demoUserEmail = demoUserEmailState,
                        onSignInClick = { signInWithGoogle() },
                        onDemoSignInClick = { demoUserEmailState = "usuario_demo@ejemplo.com" },
                        onSignOutClick = { signOut() },
                        onLogAnalyticsEvent = { logSampleAnalyticsEvent() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Verificar si el usuario ya inició sesión en Firebase
        val user = auth.currentUser
        updateUI(user)
    }

    private fun updateUI(user: FirebaseUser?) {
        currentUserState = user
        if (user != null) {
            demoUserEmailState = null
        }
    }

    // --- Autenticación con Google usando Credential Manager ---

    private fun signInWithGoogle() {
        val webClientId = try {
            getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            ""
        }

        if (webClientId.isBlank() || webClientId.contains("placeholder")) {
            Toast.makeText(
                this,
                "Para usar Google Sign-In real debes configurar tu Web Client ID en Firebase Console.",
                Toast.LENGTH_LONG
            ).show()
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity
                )
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Error al obtener credenciales", e)
                Toast.makeText(
                    this@MainActivity,
                    "No hay credenciales disponibles. Habilita Google Sign-In en Firebase Console y añade tu SHA-1.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun handleSignIn(credential: Credential) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w(TAG, "Las credenciales no son de tipo Google ID")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                    Toast.makeText(this, "¡Sesión iniciada con éxito!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                    Toast.makeText(this, "Error de autenticación con Firebase", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signOut() {
        auth.signOut()
        demoUserEmailState = null
        lifecycleScope.launch {
            try {
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)
                updateUI(null)
                Toast.makeText(this@MainActivity, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            } catch (e: ClearCredentialException) {
                Log.e(TAG, "No se pudieron borrar las credenciales: ${e.localizedMessage}")
            }
        }
    }

    private fun logSampleAnalyticsEvent() {
        val eventBundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, "id_demo")
            putString(FirebaseAnalytics.Param.ITEM_NAME, "Demostración")
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Boton")
        }
        firebaseAnalytics.logEvent("click_boton_demo", eventBundle)
        Toast.makeText(this, "¡Evento enviado a Firebase Analytics!", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MainScreen(
    user: FirebaseUser?,
    demoUserEmail: String?,
    onSignInClick: () -> Unit,
    onDemoSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onLogAnalyticsEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeEmail = user?.email ?: demoUserEmail

    if (activeEmail != null) {
        // Cuando el usuario ha iniciado sesión (por Google o Demo), muestra las listas
        ShoppingAppScreen(
            currentUserEmail = activeEmail,
            onSignOut = onSignOutClick
        )
    } else {
        // Pantalla de Inicio de Sesión
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Lista de la Compra",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Inicia sesión para gestionar tus listas de la compra",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onSignInClick) {
                Text("Iniciar sesión con Google")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(onClick = onDemoSignInClick) {
                Text("Probar sin Google (Modo Demo)")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenSignedOutPreview() {
    Lista_De_La_CompraTheme {
        MainScreen(
            user = null,
            demoUserEmail = null,
            onSignInClick = {},
            onDemoSignInClick = {},
            onSignOutClick = {},
            onLogAnalyticsEvent = {}
        )
    }
}