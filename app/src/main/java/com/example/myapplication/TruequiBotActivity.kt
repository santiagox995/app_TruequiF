package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.ChatAdapter
import com.example.myapplication.model.Mensaje
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

// ────────────────────────────────────────────
// 1️⃣ Definición de datos (Modelos)
// ────────────────────────────────────────────
data class AssistantMessage(
    val role: String,
    val content: String
)

data class AssistantRequest(
    val model: String,
    val messages: List<AssistantMessage>
)

data class AssistantResponse(
    val choices: List<AssistantChoice>
)

data class AssistantChoice(
    val message: AssistantMessage
)

// ────────────────────────────────────────────
// 2️⃣ Servicio Retrofit para OpenAI
// ────────────────────────────────────────────
interface TruequiBotService {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun getAssistantCompletion(
        @Header("Authorization") authorization: String,
        @Body requestBody: AssistantRequest
    ): Response<AssistantResponse>
}

// ────────────────────────────────────────────
// 3️⃣ TruequiBotActivity - Interfaz
// ────────────────────────────────────────────
class TruequiBotActivity : AppCompatActivity() {

    // 🔑 Clave de API y ID del Asistente
    private val apiKey = "Bearer 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"
    private val assistantId = "asst_5u2moiA1fnBy48B4mLTN7fUT"

    // UI
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var inputMessage: EditText
    private lateinit var sendButton: Button
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<Mensaje>()


    // Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(OkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(TruequiBotService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_truequibot)

        // Inicialización de UI
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        inputMessage = findViewById(R.id.input_message)
        sendButton = findViewById(R.id.send_button)

        chatAdapter = ChatAdapter(messageList)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

        // Evento al enviar un mensaje
        sendButton.setOnClickListener {
            val userText = inputMessage.text.toString().trim()
            if (userText.isNotEmpty()) {
                addUserMessage(userText)
                inputMessage.text.clear()

                // Revisar si es una pregunta frecuente
                val respuestaFaq = esPreguntaFrecuente(userText)
                if (respuestaFaq != null) {
                    addBotMessage(respuestaFaq)
                } else {
                    callAssistant(userText)
                }
            }
        }

    }

    // Agregar el mensaje del usuario al RecyclerView
    private fun addUserMessage(text: String) {
        messageList.add(Mensaje(text, "user"))
        chatAdapter.notifyItemInserted(messageList.size - 1)
        chatRecyclerView.scrollToPosition(messageList.size - 1)
    }

    // Agregar el mensaje del bot al RecyclerView
    private fun addBotMessage(text: String) {
        messageList.add(Mensaje(text, "bot"))
        chatAdapter.notifyItemInserted(messageList.size - 1)
        chatRecyclerView.scrollToPosition(messageList.size - 1)
    }
    private fun esPreguntaFrecuente(mensaje: String): String? {
        return when {
            mensaje.contains("ayuda", true) -> "Para ayudarte puedes utilizar estos atajos para comunicarte conmigo: \n -agregar producto \n -ver productos \n -dar like \n -economia circular \n -mis productos \n -cómo funciona \n -servicios \n -cómo funciona Truequi"
            mensaje.contains("agregar producto", true) -> "Para agregar un producto, dirígete al menú de 'Agregar Producto' el cual es un + en la mitad de la barra de tareas. Una vez allí dale a continuar , sube 4 fotos, título, descripción, selecciona la categoría y el estado del producto. ¡Es fácil y rápido!"
            mensaje.contains("ver productos", true) -> "Puedes ver los productos que has agregado en tu perfil. Ahí te aparecerán todos tus productos."
            mensaje.contains("dar like", true) -> "Solo presiona el botón de 'Like' (el cual es un corazón) en cualquier producto para agregarlo a tu lista de favoritos."
            mensaje.contains("economía circular", true) -> "Truequi promueve la reutilización de productos en buen estado, ayudando a reducir los residuos y promoviendo un consumo más responsable."
            mensaje.contains("mis productos", true) -> "Puedes ver tus productos subidos en la sección 'Mi Perfil'. Ahí verás todos los artículos que has publicado."
            mensaje.contains("cómo funciona", true) -> "La aplicación Truequi te permite intercambiar productos en buen estado con otros usuarios, fomentando la economía circular. ¡Un lugar donde puedes encontrar lo que necesitas o dar una nueva vida a lo que ya no usas!"
            mensaje.contains("servicios", true) -> "Truequi también permite intercambiar o solicitar servicios. ¡Puedes ofrecer tus habilidades y encontrar lo que necesitas!"
            mensaje.contains("cómo funciona Truequi", true) -> "Truequi permite a los usuarios publicar, buscar y cambiar productos o servicios en buen estado. Fomentamos la economía circular para que aprovechemos mejor los recursos."
            else -> null
        }
    }
    // Llamada al asistente
    private fun callAssistant(userInput: String) {
        val body = AssistantRequest(
            model = "gpt-4o-mini",  // 🔍 Ajustado al modelo del Playground
            messages = listOf(
                AssistantMessage("system", "TruequiBot es un asistente especializado en facilitar trueques entre usuarios. Su tarea principal es identificar oportunidades de intercambio basadas en similitudes y necesidades. ¿En qué puedo ayudarte hoy?"),
                AssistantMessage("user", userInput)
            )
        )

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val resp: Response<AssistantResponse> = service.getAssistantCompletion(apiKey, body)
                if (resp.isSuccessful) {
                    val content = resp.body()?.choices
                        ?.firstOrNull()
                        ?.message
                        ?.content
                    addBotMessage(content ?: "❌ Respuesta vacía")
                } else {
                    val errorBody = resp.errorBody()?.string()
                    Log.e("TruequiBot", "Error en la llamada: $errorBody")
                    addBotMessage("❌ Error ${resp.code()} al llamar al asistente")
                }
            } catch (e: Exception) {
                Log.e("TruequiBot", "API error: ${e.message}")
                addBotMessage("❌ No se pudo procesar la solicitud. Intenta de nuevo.")
            }
        }


    }
}
