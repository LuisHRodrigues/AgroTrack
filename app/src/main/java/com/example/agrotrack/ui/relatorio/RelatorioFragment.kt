package com.example.agrotrack.ui.relatorio

import android.graphics.Color
import android.icu.text.NumberFormat
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.agrotrack.R
import com.example.agrotrack.databinding.FragmentRelatorioBinding
import com.example.agrotrack.ui.cadastro_rebanho.RebanhoDataClass
import com.example.agrotrack.ui.custos.CustoDataClass
import com.example.agrotrack.ui.vendas.VendaDataClass
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.google.android.material.textfield.TextInputEditText
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
import java.util.Date
import java.util.Locale

class RelatorioFragment : Fragment() {

    private var _binding: FragmentRelatorioBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Variáveis para armazenar os filtros selecionados
    private var filtroDataInicio: Date? = null
    private var filtroDataFim: Date? = null
    private var filtroRebanho: String? = null // Pode ser nulo para "Todos"

    private val formatoData =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRelatorioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        db = Firebase.firestore

        // Configura o listener para o botão de filtro manual
        binding.btnFiltrar.setOnClickListener {
            mostrarDialogoFiltro()
        }

        // --- NOVO: CONFIGURA LISTENERS PARA OS FILTROS PREDEFINIDOS ---
        setupFiltrosPredefinidosListeners()

        // Carrega os dados uma vez sem filtro ao iniciar
        carregarDadosDoRelatorio()
    }

    // --- NOVA FUNÇÃO PARA OS CHIPS ---
    private fun setupFiltrosPredefinidosListeners() {
        binding.chipFiltroSemanal.setOnClickListener { aplicarFiltroPredefinido("semanal") }
        binding.chipFiltroMensal.setOnClickListener { aplicarFiltroPredefinido("mensal") }
        binding.chipFiltroBimestral.setOnClickListener { aplicarFiltroPredefinido("bimestral") }
        binding.chipFiltroAnual.setOnClickListener { aplicarFiltroPredefinido("anual") }
    }

    // --- NOVA FUNÇÃO PARA CALCULAR AS DATAS E APLICAR O FILTRO ---
    private fun aplicarFiltroPredefinido(tipo: String) {
        val calendar = Calendar.getInstance()
        filtroDataFim = calendar.time // A data final é sempre "hoje"

        when (tipo) {
            "semanal" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                filtroDataInicio = calendar.time
            }
            "mensal" -> {
                calendar.add(Calendar.MONTH, -1)
                filtroDataInicio = calendar.time
            }
            "bimestral" -> {
                calendar.add(Calendar.MONTH, -2)
                filtroDataInicio = calendar.time
            }
            "anual" -> {
                calendar.add(Calendar.YEAR, -1)
                filtroDataInicio = calendar.time
            }
        }
        // Após calcular as datas, recarrega o relatório
        carregarDadosDoRelatorio()
    }


    private fun mostrarDialogoFiltro() {
        // ... (código da função mostrarDialogoFiltro continua EXATAMENTE IGUAL)
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_filtro_relatorio, null)

        val spinnerFiltroRebanho = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerFiltroRebanho)
        val etDataInicio = dialogView.findViewById<TextInputEditText>(R.id.etFiltroDataInicio)
        val etDataFim = dialogView.findViewById<TextInputEditText>(R.id.etFiltroDataFim)

        // Preenche o spinner de rebanhos
        CoroutineScope(Dispatchers.Main).launch {
            auth.currentUser?.email?.let { email ->
                val nomesRebanhos = buscarTodosOsRebanhos(email).map { it.nome }
                val listaComTodos = mutableListOf("Todos os Rebanhos").apply { addAll(nomesRebanhos) }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    listaComTodos
                )
                spinnerFiltroRebanho.setAdapter(adapter)
            }
        }

        // Listeners para abrir o DatePickerDialog
        etDataInicio.setOnClickListener { showDatePickerDialog(etDataInicio) }
        etDataFim.setOnClickListener { showDatePickerDialog(etDataFim) }

        builder.setView(dialogView)
            .setPositiveButton("Aplicar") { _, _ ->
                // Desmarca os chips para indicar que um filtro manual está ativo
                binding.chipGroupFiltros.clearCheck()

                // Captura os valores do filtro
                val rebanhoSelecionado = spinnerFiltroRebanho.text.toString()
                filtroRebanho = if (rebanhoSelecionado.isBlank() || rebanhoSelecionado == "Todos os Rebanhos") null else rebanhoSelecionado

                try {
                    filtroDataInicio = if (etDataInicio.text.toString().isNotEmpty()) formatoData.parse(etDataInicio.text.toString()) else null
                    filtroDataFim = if (etDataFim.text.toString().isNotEmpty()) formatoData.parse(etDataFim.text.toString()) else null
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Formato de data inválido.", Toast.LENGTH_SHORT).show()
                }
                carregarDadosDoRelatorio()
            }
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Limpar") { _, _ ->
                // Limpa os filtros e os chips, depois recarrega os dados
                binding.chipGroupFiltros.clearCheck()
                filtroRebanho = null
                filtroDataInicio = null
                filtroDataFim = null
                carregarDadosDoRelatorio()
            }
            .show()
    }


    private fun carregarDadosDoRelatorio() {
        // ... (código da função carregarDadosDoRelatorio continua EXATAMENTE IGUAL)
        val email = auth.currentUser?.email ?: return
        binding.progressBar.visibility = View.VISIBLE
        limparUI()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Decide quais rebanhos buscar com base no filtro
                val rebanhosParaBuscar = if (filtroRebanho != null) {
                    listOfNotNull(buscarUmRebanho(email, filtroRebanho!!))
                } else {
                    buscarTodosOsRebanhos(email)
                }

                if (rebanhosParaBuscar.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Nenhum rebanho encontrado.", Toast.LENGTH_LONG).show()
                        binding.progressBar.visibility = View.GONE
                    }
                    return@launch
                }

                // 2. Busca vendas e custos para os rebanhos selecionados
                val todasAsVendas = mutableListOf<VendaDataClass>()
                val todosOsCustos = mutableListOf<CustoDataClass>()

                rebanhosParaBuscar.forEach { rebanho ->
                    todasAsVendas.addAll(buscarVendasPorRebanho(email, rebanho.nome))
                    todosOsCustos.addAll(buscarCustosPorRebanho(email, rebanho.nome))
                }

                // 3. APLICA O FILTRO DE DATA
                val vendasFiltradas = filtrarListaPorData(todasAsVendas) { it.dataVenda }
                val custosFiltrados = filtrarListaPorData(todosOsCustos) { it.dataCusto }

                // 4. CHAMA A FUNÇÃO DE PROCESSAMENTO COM OS DADOS CORRETOS
                withContext(Dispatchers.Main) {
                    processarEExibirResultados(rebanhosParaBuscar, vendasFiltradas, custosFiltrados)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("RelatorioFragment", "Falha ao carregar dados", e)
                    Toast.makeText(requireContext(), "Erro ao gerar relatório: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    // --- FUNÇÕES DE BUSCA ---
    private suspend fun buscarTodosOsRebanhos(email: String): List<RebanhoDataClass> {
        val snapshot = db.collection("Usuarios").document(email).collection("Rebanhos").get().await()
        return snapshot.toObjects(RebanhoDataClass::class.java)
    }

    private suspend fun buscarUmRebanho(email: String, rebanhoId: String): RebanhoDataClass? {
        val snapshot = db.collection("Usuarios").document(email).collection("Rebanhos").document(rebanhoId).get().await()
        return snapshot.toObject(RebanhoDataClass::class.java)
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

    // --- FUNÇÃO DE FILTRO GENÉRICA ---
    private fun <T> filtrarListaPorData(lista: List<T>, extrairDataString: (T) -> String): List<T> {
        if (filtroDataInicio == null && filtroDataFim == null) {
            return lista // Sem filtro de data, retorna a lista inteira
        }
        return lista.filter { item ->
            try {
                val dataString = extrairDataString(item)
                if (dataString.isBlank()) return@filter false

                val dataItem = formatoData.parse(dataString) ?: return@filter false

                // Compara a data do item com os filtros (se existirem)
                val depoisDoInicio = filtroDataInicio?.let { dataItem.after(it) || dataItem == it } ?: true
                val antesDoFim = filtroDataFim?.let { dataItem.before(it) || dataItem == it } ?: true

                depoisDoInicio && antesDoFim
            } catch (e: Exception) {
                Log.w("FiltrarData", "Não foi possível analisar a data: ${extrairDataString(item)}")
                false // Ignora itens com data mal formatada
            }
        }
    }

    // --- FUNÇÃO DE PROCESSAMENTO (RECEBENDO OS PARÂMETROS CORRETOS) ---
    private fun processarEExibirResultados(
        rebanhos: List<RebanhoDataClass>,
        vendas: List<VendaDataClass>,
        custos: List<CustoDataClass>
    ) {
        // --- Cálculos ---
        val receitaTotal = vendas.sumOf { it.valorTotal }
        val custoTotal = custos.sumOf { it.valorTotal }
        val totalAnimais = rebanhos.sumOf { it.quantidadeInicial }
        val totalAnimaisVendidos = vendas.sumOf { it.quantidadeAnimais }
        val despesasVacinas = custos.filter { it.subcategoria.equals("Vacinas", ignoreCase = true) }.sumOf { it.valorTotal }
        val despesasRacao = custos.filter { it.subcategoria.equals("Ração", ignoreCase = true) }.sumOf { it.valorTotal }

        // Investimento em Compra de Animais ---
        // Filtra a lista de rebanhos baseada na data de compra
        val rebanhosNoPeriodo = filtrarListaPorData(rebanhos) { it.dataCompra }
        //Soma o valor de compra (trata nulos como 0.0)
        val investimentoCompraAnimais = rebanhosNoPeriodo.sumOf { it.valorCompra ?: 0.0 }
        val despesaTotalGeral = custoTotal + investimentoCompraAnimais
        val lucroPrejuizo = receitaTotal - despesaTotalGeral

        //Agrupar despesas totais por categoria
        val despesasPorCategoria = custos
            .groupBy {custo: CustoDataClass -> custo.tipoCusto } // Agrupa a lista de custos pela propriedade "tipoCusto"
            .mapValues { entry ->
                entry.value.sumOf {custo: CustoDataClass -> custo.valorTotal } // Para cada grupo, soma os valorTotal
            }
        // --- FIM Cálculos ---

        // PADRONIZAÇÃO DE FORMATAÇÃO
        val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        // --- Atualização dos TextViews ---
        binding.tvReceitaTotal.text = formatoMoeda.format( receitaTotal)
        binding.tvCustoTotal.text = formatoMoeda.format( despesaTotalGeral)
        binding.tvLucroPrejuizo.text = formatoMoeda.format( lucroPrejuizo)
        binding.tvTotalAnimais.text = totalAnimais.toString()
        binding.tvTotalVendidos.text = totalAnimaisVendidos.toString()
        binding.tvDespesasVacinas.text = formatoMoeda.format( despesasVacinas)
        binding.tvDespesasRacao.text = formatoMoeda.format( despesasRacao)
        binding.tvDespesasCompraAnimais.text = formatoMoeda.format(investimentoCompraAnimais)
        binding.tvDespesasVacinas.text = formatoMoeda.format(despesasVacinas)

        val cor = if (lucroPrejuizo >= 0) ContextCompat.getColor(requireContext(), R.color.lucro) else ContextCompat.getColor(requireContext(), R.color.prejuizo)
        binding.tvLucroPrejuizo.setTextColor(cor)

        // --- Gráficos ---
        val receitasPorRebanho = vendas.groupBy { it.rebanhoEnvolvido }.mapValues { entry -> entry.value.sumOf { it.valorTotal } }
        val animaisPorRebanho = rebanhos.associate { it.nome to it.quantidadeInicial.toDouble() }

        setupPieChart(binding.pieChartRebanho, "Receita por Rebanho", receitasPorRebanho)
        setupPieChart(binding.pieChartDistribuicao, "Animais por Rebanho", animaisPorRebanho)
        setupBarChart(receitaTotal.toFloat(), despesaTotalGeral.toFloat())
        setupCategoryBarChart(despesasPorCategoria)

        binding.progressBar.visibility = View.GONE
    }

    private fun limparUI() {

        // PADRONIZAÇÃO DE FORMATAÇÃO
        val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val zeroReais = formatoMoeda.format(0.0)

        binding.tvReceitaTotal.text = zeroReais
        binding.tvCustoTotal.text = zeroReais
        binding.tvLucroPrejuizo.text = zeroReais
        binding.tvLucroPrejuizo.setTextColor(Color.BLACK)
        binding.tvTotalAnimais.text = "0"
        binding.tvTotalVendidos.text = "0"
        binding.tvDespesasVacinas.text = zeroReais
        binding.tvDespesasRacao.text = zeroReais
        binding.tvDespesasCompraAnimais.text = zeroReais
        binding.pieChartRebanho.clear()
        binding.pieChartDistribuicao.clear()
        binding.barChart.clear()
        //Não limpa o progress bar aqui para evitar piscar
    }

    private fun setupPieChart(pieChart: com.github.mikephil.charting.charts.PieChart, title: String, data: Map<String, Double>) {
        if (data.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = title
            pieChart.setCenterTextSize(14f)
            pieChart.invalidate()
            return
        }

        val entries = ArrayList<PieEntry>()
        data.forEach { (label, value) ->
            entries.add(PieEntry(value.toFloat(), label))
        }
        val dataSet = PieDataSet(entries, "")
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.chart_color_1),
            ContextCompat.getColor(requireContext(), R.color.chart_color_2),
            ContextCompat.getColor(requireContext(), R.color.chart_color_3),
            ContextCompat.getColor(requireContext(), R.color.chart_color_4),
            ContextCompat.getColor(requireContext(), R.color.chart_color_5)
        )
        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(pieChart))
        pieData.setValueTextSize(12f)
        pieData.setValueTextColor(Color.WHITE)

        pieChart.apply {
            this.data = pieData
            this.setUsePercentValues(true)
            this.description.isEnabled = false
            this.legend.isEnabled = true
            this.setEntryLabelColor(Color.BLACK)
            this.setEntryLabelTextSize(12f)
            this.centerText = title
            this.setCenterTextSize(14f)
            this.animateY(1400, Easing.EaseInOutQuad)
            this.invalidate()
        }
    }

    private fun setupBarChart(receita: Float, custo: Float) {
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, receita, "Receita"))
        entries.add(BarEntry(1f, custo, "Custo"))

        val receitaDataSet = BarDataSet(listOf(BarEntry(0f, receita)), "Receita")
        receitaDataSet.color = ContextCompat.getColor(requireContext(), R.color.lucro)

        val custoDataSet = BarDataSet(listOf(BarEntry(1f, custo)), "Custo")
        custoDataSet.color = ContextCompat.getColor(requireContext(), R.color.prejuizo)

        val dataSets = ArrayList<IBarDataSet>()
        dataSets.add(receitaDataSet)
        dataSets.add(custoDataSet)

        val data = BarData(dataSets)
        data.setValueTextSize(12f)
        data.barWidth = 0.4f

        binding.barChart.data = data
        binding.barChart.description.isEnabled = false
        binding.barChart.legend.isEnabled = true
        binding.barChart.setFitBars(true)

        // Eixo X
        val xAxis = binding.barChart.xAxis
        xAxis.setDrawLabels(false)
        xAxis.setDrawGridLines(false)

        // Eixo Y
        binding.barChart.axisLeft.axisMinimum = 0f
        binding.barChart.axisRight.isEnabled = false

        binding.barChart.animateY(1500)
        binding.barChart.invalidate()
    }

    private fun showDatePickerDialog(editText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = android.app.DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                editText.setText(selectedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    /**
     * Configura um gráfico de barras e uma legenda customizada para as despesas por categoria.
     * @param data Um mapa onde a chave é o nome da categoria e o valor é a soma das despesas.
     */
    private fun setupCategoryBarChart(data: Map<String, Double>) {
        val barChart = binding.barChartDespesasCategoria
        val legendContainer = binding.legendContainer

        // 1. Limpa o gráfico e a legenda antiga antes de adicionar novos dados
        barChart.clear()
        legendContainer.removeAllViews()

        if (data.isEmpty()) {
            barChart.invalidate()
            return
        }

        val entries = ArrayList<BarEntry>()
        // Define a paleta de cores que será usada tanto no gráfico quanto na legenda
        val colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.chart_color_1),
            ContextCompat.getColor(requireContext(), R.color.chart_color_2),
            ContextCompat.getColor(requireContext(), R.color.chart_color_3),
            ContextCompat.getColor(requireContext(), R.color.chart_color_4),
            ContextCompat.getColor(requireContext(), R.color.chart_color_5)
        )

        // 2. Itera sobre os dados para criar as barras e a legenda
        var index = 0
        data.entries.forEach { (tipoCusto, valor) ->
            // Adiciona a barra ao gráfico
            entries.add(BarEntry(index.toFloat(), valor.toFloat()))

            // Infla o layout do item da legenda
            val legendItemView = LayoutInflater.from(requireContext()).inflate(R.layout.legenda_item, legendContainer, false)
            val colorView = legendItemView.findViewById<View>(R.id.legendColorView)
            val textView = legendItemView.findViewById<TextView>(R.id.legendTextView)

            // Pega a cor correspondente (e repete as cores se houver mais categorias que cores)
            val color = colors[index % colors.size]

            // Configura o item da legenda
            colorView.setBackgroundColor(color)
            textView.text = tipoCusto

            // Adiciona o item configurado ao contêiner da legenda
            legendContainer.addView(legendItemView)

            index++
        }

        val dataSet = BarDataSet(entries, "")
        dataSet.colors = colors
        dataSet.setDrawValues(true)

        val barData = BarData(dataSet)
        barData.setValueTextSize(10f)
        barData.setValueTextColor(Color.BLACK)
        barData.barWidth = 0.6f

        barChart.apply {
            this.data = barData
            description.isEnabled = false
            legend.isEnabled = false

            // 3. REMOVE as legendas do eixo X
            xAxis.setDrawLabels(false)
            xAxis.setDrawGridLines(false)
            xAxis.setDrawAxisLine(false)

            // Configurações dos outros eixos
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false

            animateY(1400, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
