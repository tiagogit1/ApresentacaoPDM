# Locais IPMA — demonstração do Tema 4

Programação de Dispositivos Móveis 2026/27 · Tema 4: APIs remotas e processamento assíncrono · Grupo 8

App Android muito simples: vai buscar à API aberta do IPMA a lista de locais
(capitais de distrito e ilhas), mostra-a numa lista e guarda no telemóvel
a última lista recebida e os favoritos (★). Depois de ter sido aberta uma vez
com rede, funciona sem rede com os dados guardados.

## Conteúdo do repositório
- `app/` — projeto Android (Kotlin + Jetpack Compose)
- `docs/` — protocolo experimental (PDF) e slides. **Ler primeiro:** `docs/Protocolo_Experimental.pdf`
- `excerto-java/` — LocaisViewModelJava.java`: equivalente em Java do LocaisViewModel.kt (ExecutorService + LiveData); não faz parte do build

## Versões usadas (não atualizar)
- Android Studio: 25.0.2
- AGP 8.13 · Kotlin 2.2 · compileSdk 36 · minSdk 26 (Android 8.0)
- Retrofit 3.0 · OkHttp 4.12 · kotlinx.serialization 1.9

## Pré-requisitos
- A API do IPMA é pública e não precisa de chave.
- A app usa a permissão `INTERNET` (já declarada no manifesto).
- O Gradle Sync precisa de internet na primeira vez.

## Abrir e correr
1. Clonar o repositório (ou Code → Download ZIP e descompactar).
2. Android Studio → **File → Open** → escolher a pasta do projeto.
3. Esperar pelo *Gradle Sync*. Se o Android Studio sugerir atualizar versões (AGP, Kotlin…),
   **não aceitar**: o protocolo foi feito com as versões acima.
4. Escolher um emulador ou telemóvel (Android 8.0 ou superior) e carregar em ▶ **Run**.

## Os 4 ficheiros (app/src/main/java/pt/ubi/locais/)
| Ficheiro | O que faz | Conteúdos do tema |
|---|---|---|
| `Rede.kt` | Formato do JSON, interface da API, cliente Retrofit/OkHttp | REST, HTTP, JSON, serialização, Retrofit, OkHttp, timeouts |
| `Armazenamento.kt` | Guarda a lista e os favoritos (SharedPreferences) | dados locais, cache, `withContext(Dispatchers.IO)` |
| `LocaisViewModel.kt` | Estado do ecrã, pedido à API, erros | coroutines, `viewModelScope`, StateFlow, tratamento de erros |
| `MainActivity.kt` | Ecrã em Compose | lista, indicadores de progresso, mensagens de erro |

## Fluxo
Abrir a app → mostra o que está guardado → pede à API → guarda a resposta → atualiza a lista.
Se o pedido falhar → a lista guardada fica no ecrã + mensagem de erro + "Tentar de novo".

## Roteiro da demonstração
1. Abrir a app com rede → indicador → lista de locais.
2. Marcar ★ em 2 locais → sobem para o topo e ficam guardados.
3. Fechar a app, ativar o **modo de avião**, abrir outra vez → a lista e os favoritos
   aparecem (vêm do armazenamento) + "Sem ligação à internet." + hora da última atualização.
4. Desligar o modo de avião → **Tentar de novo** → a lista atualiza e a hora muda.
5. Rodar o ecrã → nada se perde (o ViewModel sobrevive).

Antes da apresentação: *Definições → Apps → Locais IPMA → Armazenamento → Limpar dados*,
para o passo 1 mostrar o primeiro carregamento.

