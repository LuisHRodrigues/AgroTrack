package com.example.agrotrack.ui.cadastro_rebanho

import android.app.DatePickerDialog
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.agrotrack.R
import androidx.lifecycle.ViewModelProvider
import com.example.agrotrack.databinding.FragmentCadastroBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.appcompat.app.AlertDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore


class CadastroFragment: Fragment() {

    private var _binding: FragmentCadastroBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadastroBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Inicialize o auth
        auth = Firebase.auth

        setupBtnCadastrar()
        setupSpinners()
        setupData()
    }

    private fun exibirDialogoDeConfirmacao() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Cadastro")
            .setMessage("Confirmar conclusão de cadastro de rebanho?")
            .setPositiveButton("Confirmar") { dialog, which ->
                // 5. Chame a função para salvar no Firebase
                salvarRebanhoNoFirebase()
            }
            .setNegativeButton("Cancelar") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    // Função principal para salvar os dados
    private fun salvarRebanhoNoFirebase() {
        val usuario = auth.currentUser
        val emailUsuario = usuario?.email

        // Garante que temos um e-mail para usar como ID do documento
        if (emailUsuario == null) {
            Toast.makeText(requireContext(), "Erro: Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        // Coleta os dados dos campos
        val nomeRebanho = binding.editTextNomeRebanho.text.toString()
        val origemRebanho = binding.spinnerOrigemLote.text.toString()
        val quantidadeRebanho = binding.editTextQuantidadeInicial.text.toString().toIntOrNull() ?: 0
        val tipoRebanho = binding.spinnerTipoDeGado.text.toString()
        val dataCompraRebanho = binding.dataCompra.text.toString()

        // LÓGICA NOVA: Captura o valor apenas se o campo estiver visível
        val valorCompra = if (binding.layoutValorCompra.visibility == View.VISIBLE) {
            binding.etValorCompra.text.toString().toDoubleOrNull()
        } else {
            null // Salva como null se não for compra
        }

        // Cria o objeto Rebanho com os dados do formulário
        val novoRebanho = RebanhoDataClass(
            nome = nomeRebanho,
            origem = origemRebanho,
            quantidadeInicial = quantidadeRebanho,
            tipo = tipoRebanho,
            dataCompra = dataCompraRebanho,
            valorCompra = valorCompra
        )

        // Salva o documento no Firestore
        // Estrutura: Usuarios/{email_do_usuario}/Rebanhos/{nome_do_rebanho}
        db.collection("Usuarios").document(emailUsuario)
            .collection("Rebanhos").document(nomeRebanho)
            .set(novoRebanho) // .set() cria ou sobrescreve o documento
            .addOnSuccessListener {
                // Sucesso! Limpa o formulário e avisa o usuário.
                Toast.makeText(requireContext(), "Rebanho cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                limparCampos()
            }
            .addOnFailureListener { e ->
                // Falha. Mostra o erro no log e avisa o usuário.
                Toast.makeText(requireContext(), "Falha ao cadastrar rebanho: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    //Função auxiliar para limpar os campos após o sucesso
    private fun limparCampos() {
        binding.editTextNomeRebanho.text?.clear()
        binding.layoutNomeRebanho.error = null // Limpa erro visual

        binding.spinnerOrigemLote.setText("", false)
        binding.layoutOrigemLote.error = null

        binding.editTextQuantidadeInicial.text?.clear()
        binding.layoutQuantidade.error = null

        binding.spinnerTipoDeGado.setText("", false)
        binding.layoutTipoGado.error = null

        binding.dataCompra.text?.clear()
        binding.layoutDataCompra.error = null

        // Limpa o campo e reseta visibilidade
        binding.etValorCompra.text?.clear()
        binding.layoutValorCompra.visibility = View.GONE
        binding.layoutValorCompra.error = null

        binding.editTextNomeRebanho.requestFocus()
    }
    private fun setupBtnCadastrar() {
        binding.buttonCadastrar.setOnClickListener {
            // Primeiro, valida os campos. Se estiverem ok, mostra o diálogo.
            if (validarCampos()) {
                exibirDialogoDeConfirmacao()
            }
        }
    }

    // Função para validar se os campos principais estão preenchidos
    private fun validarCampos(): Boolean {
        var isValid = true

        // Valida Nome
        if (binding.editTextNomeRebanho.text.isNullOrBlank()) {
            binding.layoutNomeRebanho.error = "Digite o nome do rebanho"
            isValid = false
        } else {
            binding.layoutNomeRebanho.error = null
        }

        // Valida Quantidade (Não vazio e maior que zero)
        val qtd = binding.editTextQuantidadeInicial.text.toString().toIntOrNull()
        if (qtd == null || qtd <= 0) {
            binding.layoutQuantidade.error = "Qtd deve ser maior que 0"
            isValid = false
        } else {
            binding.layoutQuantidade.error = null
        }

        // Valida Data
        if (binding.dataCompra.text.isNullOrBlank()) {
            binding.layoutDataCompra.error = "Selecione a data"
            isValid = false
        } else {
            binding.layoutDataCompra.error = null
        }

        // Valida Spinners (Obrigatório selecionar)
        if (binding.spinnerOrigemLote.text.toString().isBlank()) {
            binding.layoutOrigemLote.error = "Selecione a origem"
            isValid = false
        } else {
            binding.layoutOrigemLote.error = null
        }

        if (binding.spinnerTipoDeGado.text.toString().isBlank()) {
            binding.layoutTipoGado.error = "Selecione o tipo"
            isValid = false
        } else {
            binding.layoutTipoGado.error = null
        }

        //Se for compra, o valor é obrigatório
        if (binding.layoutValorCompra.visibility == View.VISIBLE) {
            val valor = binding.etValorCompra.text.toString().toDoubleOrNull()
            if (valor == null || valor <= 0) {
                binding.layoutValorCompra.error = "Informe o valor da compra"
                isValid = false
            } else {
                binding.layoutValorCompra.error = null
            }
        }
        return isValid
    }
    private fun setupSpinners() {
        // Pega o array de strings do strings.xml
        val origen = resources.getStringArray(R.array.origem_rebanho)
        // Cria um ArrayAdapter
        val origenAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, origen)
        // Conecta o adapter ao AutoCompleteTextView
        binding.spinnerOrigemLote.setAdapter(origenAdapter)

        // Pega o array de strings do strings.xml
        val tiposGado = resources.getStringArray(R.array.tipo_gado)
        // Cria um ArrayAdapter
        val tipoGadoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tiposGado)
        // Conecta o adapter ao AutoCompleteTextView
        binding.spinnerTipoDeGado.setAdapter(tipoGadoAdapter)

        binding.spinnerOrigemLote.setAdapter(origenAdapter)

        //Listener para mostrar/esconder campo de valor
        binding.spinnerOrigemLote.setOnItemClickListener { parent, _, position, _ ->
            val selecionado = parent.getItemAtPosition(position).toString()

            // Verifica se a opção selecionada é "Compra" (ignora maiúsculas/minúsculas)
            if (selecionado.equals("Compra", ignoreCase = true)) {
                binding.layoutValorCompra.visibility = View.VISIBLE
            } else {
                binding.layoutValorCompra.visibility = View.GONE
                binding.etValorCompra.text?.clear() // Limpa o valor se ocultar
                binding.layoutValorCompra.error = null // Limpa erro visual
            }
            binding.layoutOrigemLote.error = null
        }
    }

    private fun setupData() {
        //Pega o icon e o campo de texto do layout
        val dateField = binding.dataCompra
        val dateLayout = binding.layoutDataCompra

        //Define a ação de click do icon
        dateLayout.setEndIconOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        // Pega a instância atual do calendário
        val calendario = Calendar.getInstance()
        val ano = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)

        // Cria o DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, anoSelecionado, mesSelecionado, diaSelecionado ->
                // Quando o usuário seleciona uma data, atualiza o campo de texto
                val dataSelecionada = Calendar.getInstance()
                dataSelecionada.set(anoSelecionado, mesSelecionado, diaSelecionado)

                // Formata a data para "dd/MM/yyyy"
                val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val formattedDate = formatoData.format(dataSelecionada.time)

                // Define a data formatada no campo de texto
                binding.dataCompra.setText(formattedDate)
            },
            ano,
            mes,
            dia
        )
        // Exibe o diálogo
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
