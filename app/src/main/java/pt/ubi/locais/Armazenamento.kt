package pt.ubi.locais

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

// Guarda dados no telemóvel com SharedPreferences (ficheiro chave → valor).
// Ler e escrever no disco é I/O: por isso cada função muda para Dispatchers.IO.
class Armazenamento(context: Context) {

    private val prefs = context.getSharedPreferences("dados", Context.MODE_PRIVATE)
    private val serializador = ListSerializer(Local.serializer())

    // A última lista que veio da API, em JSON, + a hora em que chegou
    suspend fun guardarLocais(locais: List<Local>) = withContext(Dispatchers.IO) {
        prefs.edit {
            putString("locais", Rede.json.encodeToString(serializador, locais))
            putLong("atualizadoEm", System.currentTimeMillis())
        }
    }

    suspend fun lerLocais(): List<Local> = withContext(Dispatchers.IO) {
        val texto = prefs.getString("locais", null) ?: return@withContext emptyList()
        try {
            Rede.json.decodeFromString(serializador, texto)
        } catch (e: Exception) {
            emptyList()                          // dados guardados estragados → ignorar
        }
    }

    suspend fun lerAtualizadoEm(): Long = withContext(Dispatchers.IO) {
        prefs.getLong("atualizadoEm", 0L)
    }

    // Os favoritos são ids guardados como um conjunto de texto
    suspend fun guardarFavoritos(ids: Set<Int>) = withContext(Dispatchers.IO) {
        prefs.edit { putStringSet("favoritos", ids.map { it.toString() }.toSet()) }
    }

    suspend fun lerFavoritos(): Set<Int> = withContext(Dispatchers.IO) {
        prefs.getStringSet("favoritos", emptySet()).orEmpty().map { it.toInt() }.toSet()
    }
}
