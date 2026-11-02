package pt.ubi.locais

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

// ---------- 1. O formato do JSON (DTO) ----------
// Resposta de https://api.ipma.pt/open-data/distrits-islands.json
// {"owner":"IPMA","country":"PT","data":[{"globalIdLocal":1050200,"local":"Castelo Branco", ...}]}

@Serializable
data class RespostaLocais(
    val data: List<Local>,
)

@Serializable
data class Local(
    val globalIdLocal: Int,
    val local: String,
    val latitude: String,    // a API envia as coordenadas como texto
    val longitude: String,
)

// ---------- 2. A API descrita como uma interface (Retrofit) ----------

interface IpmaApi {
    @GET("open-data/distrits-islands.json")
    suspend fun obterLocais(): RespostaLocais     // suspend → não bloqueia a UI
}

// ---------- 3. Criar o cliente uma única vez ----------

object Rede {

    // ignoreUnknownKeys: a API envia campos que não declarámos (owner, idRegiao…)
    val json = Json { ignoreUnknownKeys = true }

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)   // tempo para estabelecer a ligação
        .readTimeout(15, TimeUnit.SECONDS)      // tempo máximo sem receber dados
        .callTimeout(20, TimeUnit.SECONDS)      // tempo máximo para o pedido inteiro
        .build()

    val api: IpmaApi = Retrofit.Builder()
        .baseUrl("https://api.ipma.pt/")        // tem de terminar em "/"
        .client(cliente)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(IpmaApi::class.java)
}
