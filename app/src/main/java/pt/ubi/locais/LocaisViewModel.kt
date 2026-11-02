package pt.ubi.locais

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.delay
// Tudo o que o ecrã precisa de saber, num só objeto
data class EstadoUi(
    val locais: List<Local> = emptyList(),
    val favoritos: Set<Int> = emptySet(),
    val aCarregar: Boolean = false,
    val erro: String? = null,
    val atualizadoEm: Long = 0L,
)

// AndroidViewModel: sobrevive à rotação do ecrã e dá acesso ao Context (para o armazenamento)
class LocaisViewModel(app: Application) : AndroidViewModel(app) {

    private val armazenamento = Armazenamento(app)

    private val _estado = MutableStateFlow(EstadoUi())
    val estado: StateFlow<EstadoUi> = _estado.asStateFlow()   // o ecrã só lê

    // Ordem alfabética portuguesa ("Évora" junto de "Faro", não no fim)
    private val ordemPt = Collator.getInstance(Locale.forLanguageTag("pt-PT"))

    init {
        viewModelScope.launch {
            // 1. Mostrar logo o que ficou guardado da última vez (funciona sem rede)
            val guardados = armazenamento.lerLocais()
            val favoritos = armazenamento.lerFavoritos()
            val atualizadoEm = armazenamento.lerAtualizadoEm()
            _estado.update {
                it.copy(locais = guardados, favoritos = favoritos, atualizadoEm = atualizadoEm)
            }
            // 2. Pedir dados novos à API
            carregar()
        }
    }

    fun carregar() {
        if (_estado.value.aCarregar) return                    // já há um pedido em curso
        viewModelScope.launch {                                 // cancelada se o ecrã fechar
            _estado.update { it.copy(aCarregar = true, erro = null) }
            try {
                delay(1500)
                val resposta = Rede.api.obterLocais()           // suspende aqui; a UI continua fluida
                val ordenados = resposta.data.sortedWith(compareBy(ordemPt) { it.local })
                armazenamento.guardarLocais(ordenados)          // guardar para a próxima vez
                _estado.update {
                    it.copy(locais = ordenados, atualizadoEm = System.currentTimeMillis())
                }
            } catch (e: CancellationException) {
                throw e                                         // cancelamento não é erro
            } catch (e: Exception) {
                _estado.update { it.copy(erro = mensagemDeErro(e)) }   // a lista guardada fica
            } finally {
                _estado.update { it.copy(aCarregar = false) }
            }
        }
    }

    fun alternarFavorito(id: Int) {
        val atuais = _estado.value.favoritos
        val novos = if (id in atuais) atuais - id else atuais + id
        _estado.update { it.copy(favoritos = novos) }
        viewModelScope.launch { armazenamento.guardarFavoritos(novos) }
    }
}

// Cada tipo de falha → uma mensagem que o utilizador percebe
fun mensagemDeErro(e: Exception): String = when (e) {
    is InterruptedIOException -> "O servidor demorou demasiado a responder."  // timeout
    is IOException -> "Sem ligação à internet."                               // sem rede, DNS…
    is HttpException -> "O servidor respondeu com erro (HTTP ${e.code()})."   // 4xx / 5xx
    is SerializationException -> "A resposta tem um formato inesperado."      // JSON diferente
    else -> "Ocorreu um erro inesperado."
}
