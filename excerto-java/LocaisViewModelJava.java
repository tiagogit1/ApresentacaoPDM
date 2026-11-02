// Equivalente em Java do LocaisViewModel.kt 
// Excerto ilustrativo: não faz parte do build da app.
// Depende de classes que não estão neste repositório 
// Armazenamento, EstadoUi, Rede, Local, RespostaLocais.
// Dependência extra: com.squareup.retrofit2:converter-gson

package pt.ubi.locais;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;

import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Response;

// AndroidViewModel: sobrevive à rotação do ecrã e dá acesso ao Context (para o armazenamento)
public class LocaisViewModel extends AndroidViewModel {

    private final Armazenamento armazenamento;

    private final MutableLiveData<EstadoUi> estado = new MutableLiveData<>(new EstadoUi());

    // Threads de fundo: uma para a rede, outra para o disco (a UI nunca fica bloqueada)
    private final ExecutorService rede = Executors.newSingleThreadExecutor();
    private final ExecutorService disco = Executors.newSingleThreadExecutor();
    private final Handler threadPrincipal = new Handler(Looper.getMainLooper());

    // Ordem alfabética portuguesa ("Évora" junto de "Faro", não no fim)
    private final Collator ordemPt = Collator.getInstance(Locale.forLanguageTag("pt-PT"));

    private volatile Call<RespostaLocais> pedidoEmCurso;
    private volatile boolean terminado = false;

    public LocaisViewModel(@NonNull Application app) {
        super(app);
        armazenamento = new Armazenamento(app);

        disco.execute(() -> {
            // 1. Mostrar logo o que ficou guardado da última vez (funciona sem rede)
            List<Local> guardados = armazenamento.lerLocais();
            Set<Integer> favoritos = armazenamento.lerFavoritos();
            long atualizadoEm = armazenamento.lerAtualizadoEm();
            naThreadPrincipal(() -> {
                atualizar(e -> e.comLocais(guardados).comFavoritos(favoritos).comAtualizadoEm(atualizadoEm));
                // 2. Pedir dados novos à API
                carregar();
            });
        });
    }

    // O ecrã só lê (LiveData), não altera
    public LiveData<EstadoUi> getEstado() {
        return estado;
    }

    public void carregar() {
        if (atual().aCarregar) return;                          // já há um pedido em curso
        atualizar(e -> e.comACarregar(true).comErro(null));
        rede.execute(() -> {
            try {
                Call<RespostaLocais> chamada = Rede.api.obterLocais();
                pedidoEmCurso = chamada;                        // para poder cancelar se o ecrã fechar
                Response<RespostaLocais> resposta = chamada.execute();   // bloqueia esta thread, não a UI
                if (!resposta.isSuccessful()) throw new HttpException(resposta);
                RespostaLocais corpo = resposta.body();
                if (corpo == null || corpo.data == null) {
                    throw new JsonParseException("Resposta sem o campo 'data'");
                }

                List<Local> ordenados = new ArrayList<>(corpo.data);
                ordenados.sort(Comparator.comparing((Local l) -> l.local, ordemPt));
                armazenamento.guardarLocais(ordenados);         // guardar para a próxima vez
                long agora = System.currentTimeMillis();
                naThreadPrincipal(() -> atualizar(e -> e.comLocais(ordenados).comAtualizadoEm(agora)));
            } catch (Exception e) {
                if (!terminado) {                               // cancelamento não é erro
                    String mensagem = mensagemDeErro(e);
                    naThreadPrincipal(() -> atualizar(est -> est.comErro(mensagem)));   // a lista guardada fica
                }
            } finally {
                pedidoEmCurso = null;
                naThreadPrincipal(() -> atualizar(e -> e.comACarregar(false)));
            }
        });
    }

    public void alternarFavorito(int id) {
        Set<Integer> novos = new HashSet<>(atual().favoritos);
        if (!novos.remove(id)) novos.add(id);
        Set<Integer> favoritos = Collections.unmodifiableSet(novos);
        atualizar(e -> e.comFavoritos(favoritos));
        disco.execute(() -> armazenamento.guardarFavoritos(favoritos));
    }

    // Chamado quando o ecrã fecha de vez (não na rotação)
    @Override
    protected void onCleared() {
        terminado = true;
        Call<RespostaLocais> pedido = pedidoEmCurso;
        if (pedido != null) pedido.cancel();
        rede.shutdownNow();
        disco.shutdown();                                       // deixa terminar as escritas pendentes
        threadPrincipal.removeCallbacksAndMessages(null);
    }

    // ---------- auxiliares ----------

    private EstadoUi atual() {
        return estado.getValue();
    }

    // Só se altera o estado na thread principal
    private void atualizar(UnaryOperator<EstadoUi> mudanca) {
        estado.setValue(mudanca.apply(atual()));
    }

    private void naThreadPrincipal(Runnable acao) {
        threadPrincipal.post(() -> {
            if (!terminado) acao.run();
        });
    }

    // Cada tipo de falha → uma mensagem que o utilizador percebe
    public static String mensagemDeErro(Exception e) {
        if (e instanceof InterruptedIOException) return "O servidor demorou demasiado a responder.";  // timeout
        if (e instanceof IOException) return "Sem ligação à internet.";                               // sem rede, DNS…
        if (e instanceof HttpException) {
            return "O servidor respondeu com erro (HTTP " + ((HttpException) e).code() + ").";        // 4xx / 5xx
        }
        if (e instanceof JsonParseException) return "A resposta tem um formato inesperado.";          // JSON diferente
        return "Ocorreu um erro inesperado.";
    }
}
