package pt.ubi.locais

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                EcraLocais()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcraLocais(vm: LocaisViewModel = viewModel()) {
    // Recolhe o estado do ViewModel; o ecrã redesenha-se sempre que ele muda
    val estado by vm.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Locais IPMA") },
                actions = {
                    TextButton(onClick = vm::carregar, enabled = !estado.aCarregar) {
                        Text("Atualizar")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // Indicador de progresso quando já há lista no ecrã
            if (estado.aCarregar && estado.locais.isNotEmpty()) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            // Mensagem de erro (a lista guardada continua visível por baixo)
            estado.erro?.let { erro ->
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(erro, Modifier.weight(1f))
                        TextButton(onClick = vm::carregar) { Text("Tentar de novo") }
                    }
                }
            }

            // Frescura dos dados
            if (estado.atualizadoEm > 0) {
                Text(
                    "Atualizado em ${formatarHora(estado.atualizadoEm)}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            when {
                // 1.º arranque, ainda sem nada guardado: indicador ao centro
                estado.locais.isEmpty() && estado.aCarregar ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                // Sem dados e sem rede
                estado.locais.isEmpty() ->
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Ainda não há dados guardados.")
                        Button(onClick = vm::carregar, Modifier.padding(top = 12.dp)) {
                            Text("Tentar de novo")
                        }
                    }

                // A lista: favoritos primeiro, depois por ordem alfabética
                else -> {
                    val ordenados = estado.locais.sortedByDescending { it.globalIdLocal in estado.favoritos }
                    LazyColumn {
                        items(ordenados, key = { it.globalIdLocal }) { local ->
                            val favorito = local.globalIdLocal in estado.favoritos
                            ListItem(
                                headlineContent = { Text(local.local) },
                                supportingContent = { Text("${local.latitude}, ${local.longitude}") },
                                trailingContent = {
                                    IconButton(onClick = { vm.alternarFavorito(local.globalIdLocal) }) {
                                        Text(
                                            if (favorito) "★" else "☆",
                                            style = MaterialTheme.typography.titleLarge,
                                        )
                                    }
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

private val formatoHora = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm").withZone(ZoneId.systemDefault())

private fun formatarHora(epochMs: Long): String = formatoHora.format(Instant.ofEpochMilli(epochMs))
