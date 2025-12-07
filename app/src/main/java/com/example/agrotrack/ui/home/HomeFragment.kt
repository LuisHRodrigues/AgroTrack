package com.example.agrotrack.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.agrotrack.databinding.FragmentTelaInicialBinding
// Importe as data classes necessárias
import com.example.agrotrack.ui.cadastro_rebanho.RebanhoDataClass
import com.example.agrotrack.ui.custos.CustoDataClass
import com.example.agrotrack.ui.vendas.VendaDataClass
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment: Fragment() {
    private var _binding: FragmentTelaInicialBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTelaInicialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        db = Firebase.firestore

        // Inicia a busca dos dados quando a tela é criada
        carregarDadosResumo()
    }

    // --- LÓGICA PRINCIPAL ---
    private fun carregarDadosResumo() {
        val email = auth.currentUser?.email
        if (email == null) {
            Toast.makeText(requireContext(), "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Busca todos os dados necessários
                val todosOsRebanhos = buscarTodosOsRebanhos(email)
                val todasAsVendas = mutableListOf<VendaDataClass>()
                val todosOsCustos = mutableListOf<CustoDataClass>()

                todosOsRebanhos.forEach { rebanho ->
                    todasAsVendas.addAll(buscarVendasPorRebanho(email, rebanho.nome))
                    todosOsCustos.addAll(buscarCustosPorRebanho(email, rebanho.nome))
                }

                // 2. Com os dados em mãos, atualiza a UI na thread principal
                withContext(Dispatchers.Main) {
                    popularResumo(todosOsRebanhos, todasAsVendas, todosOsCustos)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("HomeFragment", "Falha ao carregar dados do resumo", e)
                    Toast.makeText(requireContext(), "Erro ao carregar resumo: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- FUNÇÕES DE BUSCA NO FIREBASE ---

    private suspend fun buscarTodosOsRebanhos(email: String): List<RebanhoDataClass> {
        val snapshot = db.collection("Usuarios").document(email).collection("Rebanhos").get().await()
        return snapshot.toObjects(RebanhoDataClass::class.java)
    }

    private suspend fun buscarVendasPorRebanho(email: String, rebanhoId: String): List<VendaDataClass> {
        val snapshot = db.collection("Usuarios").document(email)
            .collection("Rebanhos").document(rebanhoId)
            .collection("Vendas").get().await()
        return snapshot.toObjects(VendaDataClass::class.java)
    }

    private suspend fun buscarCustosPorRebanho(email: String, rebanhoId: String): List<CustoDataClass> {
        val snapshot = db.collection("Usuarios").document(email)
            .collection("Rebanhos").document(rebanhoId)
            .collection("Custos").get().await()
        return snapshot.toObjects(CustoDataClass::class.java)
    }

    // --- FUNÇÃO PARA POPULAR A UI---

    private fun popularResumo(rebanhos: List<RebanhoDataClass>, vendas: List<VendaDataClass>, custos: List<CustoDataClass>) {
        // --- Cálculos ---
        val receitaTotal = vendas.sumOf { it.valorTotal }
        val totalAnimais = rebanhos.sumOf { it.quantidadeInicial }
        val nomeUsuario = auth.currentUser?.displayName?.split(" ")?.get(0) ?: "Usuário"

        // --- LÓGICA PARA FILTRAR CUSTOS DO MÊS ATUAL ---
        val calendario = Calendar.getInstance()
        val mesAtual = calendario.get(Calendar.MONTH) + 1 // Calendar.MONTH é baseado em 0 (Janeiro = 0)
        val anoAtual = calendario.get(Calendar.YEAR)
        val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val custosDoMes = custos.filter { custo ->
            try {
                val dataCusto = formatoData.parse(custo.dataCusto)
                if (dataCusto != null) {
                    val calCusto = Calendar.getInstance().apply { time = dataCusto }
                    val mesCusto = calCusto.get(Calendar.MONTH) + 1
                    val anoCusto = calCusto.get(Calendar.YEAR)
                    mesCusto == mesAtual && anoCusto == anoAtual
                } else {
                    false
                }
            } catch (e: Exception) {
                // Trata datas mal formatadas, se houver
                false
            }
        }.sumOf { it.valorTotal }
        // ----------------------------------------------------

        // Formato de moeda para R$
        val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        // --- Atualiza os TextViews
        binding.tvWelcome.text = "Bem vindo, $nomeUsuario!"
        binding.tvGanhosTotais.text = formatoMoeda.format(receitaTotal)
        binding.tvCustosMes.text = formatoMoeda.format(custosDoMes)
        binding.tvTotalAnimais.text = totalAnimais.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
