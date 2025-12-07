package com.example.agrotrack.ui.vendas

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.agrotrack.R
import com.example.agrotrack.databinding.FragmentVendasEReceitasBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Vendas_e_ReceitasFragment : Fragment() {

    private var _binding: FragmentVendasEReceitasBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    //Armazena quantos animais o rebanho selecionado tem atualmente
    private var quantidadeDisponivelNoRebanho: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVendasEReceitasBinding.inflate(inflater, container, false)
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

        binding.layoutDataVenda.setEndIconOnClickListener {
            showDatePickerDialog()
        }


    }

    private fun setupSpinners() {
// 1. Configura a lista de rebanhos
        buscarRebanhosDoUsuario { listaRebanhos ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listaRebanhos)
            binding.spinnerRebanhosEnvolvidos.setAdapter(adapter)
        }

        // 2. LISTENER IMPORTANTE: Quando selecionar um rebanho, busca a quantidade dele no banco
        binding.spinnerRebanhosEnvolvidos.setOnItemClickListener { parent, _, position, _ ->
            val nomeRebanho = parent.getItemAtPosition(position).toString()
            buscarQuantidadeAtualDoRebanho(nomeRebanho)
            binding.layoutRebanhosEnvolvidos.error = null // Limpa erro se tiver
        }

        val compradores = resources.getStringArray(R.array.compradores)
        val metodosPagamento = resources.getStringArray(R.array.metodos_pagamento)

        val compradorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, compradores)
        binding.autoComprador.setAdapter(compradorAdapter)

        val pagamentoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, metodosPagamento)
        binding.autoMetodoDePagamento.setAdapter(pagamentoAdapter)
    }

    //Busca quantos animais existem para evitar venda maior que o estoque
    private fun buscarQuantidadeAtualDoRebanho(nomeRebanho: String) {
        val email = auth.currentUser?.email ?: return

        db.collection("Usuarios").document(email)
            .collection("Rebanhos").document(nomeRebanho)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    quantidadeDisponivelNoRebanho = document.getLong("quantidadeInicial")?.toInt() ?: 0
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao verificar estoque do rebanho", Toast.LENGTH_SHORT).show()
            }
    }
    private fun validarCampos(): Boolean {
        var isValid = true
        val rebanho = binding.spinnerRebanhosEnvolvidos.text.toString()
        val valor = binding.etValorTotal.text.toString().toDoubleOrNull()
        val qtdVenda = binding.etQuantidadeAnimais.text.toString().toIntOrNull()
        val data = binding.dataVenda.text.toString()

        // 1. Valida Rebanho
        if (rebanho.isBlank()) {
            binding.layoutRebanhosEnvolvidos.error = "Selecione um rebanho"
            isValid = false
        } else {
            binding.layoutRebanhosEnvolvidos.error = null
        }

        // 2. Valida Data
        if (data.isBlank()) {
            binding.layoutDataVenda.error = "Selecione a data"
            isValid = false
        } else {
            binding.layoutDataVenda.error = null
        }

        // 3. Valida Valor
        if (valor == null || valor <= 0) {
            binding.layoutValorTotal.error = "Valor inválido"
            isValid = false
        } else {
            binding.layoutValorTotal.error = null
        }

        // 4. Valida Quantidade (Lógica Crítica)
        if (qtdVenda == null || qtdVenda <= 0) {
            binding.layoutQuantidadeAnimais.error = "Qtd inválida"
            isValid = false
        } else if (qtdVenda > quantidadeDisponivelNoRebanho) {
            // AQUI ESTÁ A TRAVA DE ESTOQUE NEGATIVO
            binding.layoutQuantidadeAnimais.error = "Estoque insuficiente (Atual: $quantidadeDisponivelNoRebanho)"
            isValid = false
        } else {
            binding.layoutQuantidadeAnimais.error = null
        }

        return isValid
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

    private fun exibirDialogoConfirmacao() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Venda")
            .setMessage("Deseja registrar esta venda?")
            .setPositiveButton("Confirmar") { _, _ ->
                salvarVenda()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarVenda() {
        val email = auth.currentUser?.email ?: return
        val nomeRebanho = binding.spinnerRebanhosEnvolvidos.text.toString()
        val quantidadeVendida = binding.etQuantidadeAnimais.text.toString().toIntOrNull() ?: 0

        // 1. Criar o objeto da Venda
        val novaVenda = VendaDataClass(
            rebanhoEnvolvido = nomeRebanho,
            dataVenda = binding.dataVenda.text.toString(),
            valorTotal = binding.etValorTotal.text.toString().toDoubleOrNull() ?: 0.0,
            comprador = binding.autoComprador.text.toString(),
            metodoPagamento = binding.autoMetodoDePagamento.text.toString(),
            quantidadeAnimais = quantidadeVendida,
            baixaAutomatica = binding.cbBaixaAutomatica.isChecked
        )

        // 2. Salvar o documento da venda na subcoleção correta
        db.collection("Usuarios").document(email)
            .collection("Rebanhos").document(nomeRebanho)
            .collection("Vendas").add(novaVenda) // .add() cria um ID automático para a venda
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Venda registrada com sucesso!", Toast.LENGTH_SHORT).show()
                // 3. Se a baixa automática estiver marcada, atualiza a quantidade do rebanho
                if (binding.cbBaixaAutomatica.isChecked) {
                    darBaixaNoRebanho(email, nomeRebanho, quantidadeVendida)
                }
                limparCampos()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Falha ao registrar venda: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun darBaixaNoRebanho(email: String, nomeRebanho: String, quantidadeVendida: Int) {
        val rebanhoRef = db.collection("Usuarios").document(email)
            .collection("Rebanhos").document(nomeRebanho)

        // Usa FieldValue.increment com um valor negativo para subtrair
        rebanhoRef.update("quantidadeInicial", FieldValue.increment(-quantidadeVendida.toLong()))
            .addOnSuccessListener {
                Log.d("Vendas", "Baixa de $quantidadeVendida animais realizada no rebanho $nomeRebanho.")
            }
            .addOnFailureListener { e ->
                Log.e("Vendas", "Falha ao dar baixa no rebanho: ${e.message}")
                Toast.makeText(requireContext(), "A venda foi salva, mas falhou ao dar baixa no rebanho.", Toast.LENGTH_LONG).show()
            }
    }

    private fun limparCampos() {
        binding.spinnerRebanhosEnvolvidos.setText("", false)
        binding.layoutRebanhosEnvolvidos.error = null

        binding.dataVenda.text?.clear()
        binding.layoutDataVenda.error = null

        binding.etValorTotal.text?.clear()
        binding.layoutValorTotal.error = null

        binding.autoComprador.setText("", false)
        binding.layoutComprador.error = null

        binding.autoMetodoDePagamento.setText("", false)
        binding.layoutMetodoPagamento.error = null

        binding.etQuantidadeAnimais.text?.clear()
        binding.layoutQuantidadeAnimais.error = null

        binding.cbBaixaAutomatica.isChecked = false

        quantidadeDisponivelNoRebanho = 0 // Reseta o contador interno
    }

    private fun showDatePickerDialog() {
        val calendario = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, ano, mes, dia ->
                val dataSelecionada = Calendar.getInstance()
                dataSelecionada.set(ano, mes, dia)
                val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.dataVenda.setText(formatoData.format(dataSelecionada.time))
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
