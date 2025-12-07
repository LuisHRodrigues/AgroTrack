package com.example.agrotrack.ui.custos

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.agrotrack.R
import com.example.agrotrack.databinding.FragmentCustosEDespesasBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Custos_e_DespesasFragment : Fragment() {

    private var _binding: FragmentCustosEDespesasBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustosEDespesasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Firebase
        auth = Firebase.auth
        db = Firebase.firestore

        setupSpinners()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnConcluir.setOnClickListener {
            if (validarCampos()) {
                exibirDialogoConfirmacao()
            }
        }
        binding.layoutDataCusto.setEndIconOnClickListener {
            showDatePickerDialog()
        }
        // ADICIONA UM LISTENER PARA O TIPO DE CUSTO
        // Quando o tipo de custo mudar, atualiza as subcategorias.
        binding.spinnerTipoCusto.setOnItemClickListener { parent, view, position, id ->
            val tipoSelecionado = parent.getItemAtPosition(position).toString()
            atualizarSpinnerSubcategoria(tipoSelecionado)
        }
    }

    private fun setupSpinners() {
        // Popula o spinner de rebanhos
        buscarRebanhosDoUsuario { listaRebanhos ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listaRebanhos)
            binding.spinnerRebanhoAssociado.setAdapter(adapter)
        }

        // Popula o spinner de tipo de custo
        val tiposCusto = resources.getStringArray(R.array.tipos_custo)
        val tiposCustoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tiposCusto)
        binding.spinnerTipoCusto.setAdapter(tiposCustoAdapter)

        binding.layoutSubcategoriaCusto.isEnabled = false
    }

    private fun atualizarSpinnerSubcategoria(tipoCusto: String) {
        val arrayId = when (tipoCusto) {
            "Alimentação" -> R.array.subcategorias_alimentacao
            "Medicamentos" -> R.array.subcategorias_medicamentos
            "Mão de Obra" -> R.array.subcategorias_mao_de_obra
            "Manutenção" -> R.array.subcategorias_manutencao
            else -> R.array.subcategorias_outros
        }

        val subcategorias = resources.getStringArray(arrayId)
        val subcategoriaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, subcategorias)
        binding.spinnerSubcategoriaCusto.setAdapter(subcategoriaAdapter)

        // Limpa a seleção anterior e habilita o campo
        binding.spinnerSubcategoriaCusto.setText("", false)
        binding.layoutSubcategoriaCusto.isEnabled = true
    }


    private fun buscarRebanhosDoUsuario(onResult: (List<String>) -> Unit) {
        val email = auth.currentUser?.email
        if (email == null) {
            onResult(emptyList())
            return
        }

        db.collection("Usuarios").document(email).collection("Rebanhos")
            .get()
            .addOnSuccessListener { documents ->
                val nomesRebanhos = documents.map { it.id }
                onResult(nomesRebanhos)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao buscar rebanhos.", Toast.LENGTH_SHORT).show()
                onResult(emptyList())
            }
    }

    private fun validarCampos(): Boolean {
        var isValid = true

        // Valida Rebanho
        if (binding.spinnerRebanhoAssociado.text.isNullOrBlank()) {
            binding.layoutRebanhoAssociado.error = "Selecione um rebanho"
            isValid = false
        } else {
            binding.layoutRebanhoAssociado.error = null
        }

        // Valida Valor (R$)
        val valor = binding.etValorTotal.text.toString().toDoubleOrNull()
        if (valor == null || valor <= 0) {
            binding.etValorTotal.error = "Valor inválido" // Ou no layout se tiver ID
            isValid = false
        } else {
            binding.etValorTotal.error = null
        }

        // Valida Data
        if (binding.dataCusto.text.isNullOrBlank()) {
            binding.layoutDataCusto.error = "Data obrigatória"
            isValid = false
        } else {
            binding.layoutDataCusto.error = null
        }

        // Valida Tipo e Subcategoria
        if (binding.spinnerTipoCusto.text.isNullOrBlank()) {
            binding.spinnerTipoCusto.error = "Selecione o tipo"
            isValid = false
        }

        if (binding.layoutSubcategoriaCusto.isEnabled && binding.spinnerSubcategoriaCusto.text.isNullOrBlank()) {
            binding.layoutSubcategoriaCusto.error = "Selecione a subcategoria"
            isValid = false
        } else {
            binding.layoutSubcategoriaCusto.error = null
        }

        return isValid
    }

    private fun exibirDialogoConfirmacao() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Custo")
            .setMessage("Deseja registrar este custo/despesa?")
            .setPositiveButton("Confirmar") { _, _ ->
                salvarCusto()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarCusto() {
        val email = auth.currentUser?.email ?: return
        val nomeRebanho = binding.spinnerRebanhoAssociado.text.toString()

        // 1. Criar o objeto Custo
        val novoCusto = CustoDataClass(
            rebanhoAssociado = nomeRebanho,
            tipoCusto = binding.spinnerTipoCusto.text.toString(),
            descricao = binding.etDescricao.text.toString(),
            subcategoria = binding.spinnerSubcategoriaCusto.text.toString(),
            dataCusto = binding.dataCusto.text.toString(),
            valorTotal = binding.etValorTotal.text.toString().toDoubleOrNull() ?: 0.0
        )

        // 2. Salvar o documento do custo na subcoleção correta
        db.collection("Usuarios").document(email)
            .collection("Rebanhos").document(nomeRebanho)
            .collection("Custos").add(novoCusto) // .add() cria um ID automático
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Custo registrado com sucesso!", Toast.LENGTH_SHORT).show()
                limparCampos()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Falha ao registrar custo: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun limparCampos() {
        binding.spinnerRebanhoAssociado.setText("", false)
        binding.layoutRebanhoAssociado.error = null


        binding.spinnerTipoCusto.setText("", false)
        binding.etDescricao.text?.clear()
        binding.dataCusto.text?.clear()
        binding.etValorTotal.text?.clear()
        binding.etValorTotal.error = null

        binding.spinnerSubcategoriaCusto.setText("", false)
        binding.layoutSubcategoriaCusto.isEnabled = false // Desabilita novamente

    }

    private fun showDatePickerDialog() {
        val calendario = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, ano, mes, dia ->
                val dataSelecionada = Calendar.getInstance()
                dataSelecionada.set(ano, mes, dia)
                val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.dataCusto.setText(formatoData.format(dataSelecionada.time))
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
