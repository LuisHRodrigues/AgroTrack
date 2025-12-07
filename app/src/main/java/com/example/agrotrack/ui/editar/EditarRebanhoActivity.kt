package com.example.agrotrack.ui.editar

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.agrotrack.R
import com.example.agrotrack.databinding.ActivityEditarRebanhoBinding
import com.example.agrotrack.ui.base.BaseActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class EditarRebanhoActivity : BaseActivity() {

    // 2. Configurar ViewBinding e variáveis do Firebase
    private lateinit var binding: ActivityEditarRebanhoBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var nomeRebanhoAtual: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarRebanhoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        auth = Firebase.auth
        db = Firebase.firestore

        // Receber o nome do rebanho da Intent
        nomeRebanhoAtual = intent.getStringExtra("NOME_REBANHO")

        if (nomeRebanhoAtual == null) {
            Toast.makeText(this, "Erro: Nome do rebanho não encontrado.", Toast.LENGTH_LONG).show()
            finish() // Fecha a activity se não houver nome de rebanho
            return
        }

        configurarSpinners()
        buscarDadosDoRebanho()
        configurarBotoes()
        setupData()
    }

    private fun buscarDadosDoRebanho() {
        val emailUsuario = auth.currentUser?.email
        if (emailUsuario == null || nomeRebanhoAtual == null) {
            Toast.makeText(this, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("Usuarios").document(emailUsuario)
            .collection("Rebanhos").document(nomeRebanhoAtual!!)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Converte o documento do Firestore para o nosso objeto Rebanho
                    val rebanho = document.toObject(RebanhoDataClass::class.java)
                    if (rebanho != null) {
                        popularCampos(rebanho)
                    }
                } else {
                    Toast.makeText(this, "Rebanho não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao buscar dados: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun popularCampos(rebanho: RebanhoDataClass) {

        binding.spinnerOrigemLote.setText(rebanho.origem, false)
        binding.editTextQuantidadeInicial.setText(rebanho.quantidadeInicial.toString())
        binding.spinnerTipoDeGado.setText(rebanho.tipo, false)
        binding.dataCompra.setText(rebanho.dataCompra)
    }

    private fun configurarBotoes() {
        binding.buttonConcluir.setOnClickListener {
            exibirDialogoConfirmacao("Salvar Alterações", "Deseja salvar as alterações neste rebanho?") {
                salvarAlteracoes()
            }
        }

        binding.buttonVoltar.setOnClickListener {
            finish()
        }

    }

    private fun salvarAlteracoes() {
        val emailUsuario = auth.currentUser?.email
        if (emailUsuario == null || nomeRebanhoAtual == null) return

        // Cria um mapa apenas com os campos que podem ser atualizados
        val dadosAtualizados = mapOf(
            "origem" to binding.spinnerOrigemLote.text.toString(),
            "quantidadeInicial" to (binding.editTextQuantidadeInicial.text.toString().toIntOrNull() ?: 0),
            "tipo" to binding.spinnerTipoDeGado.text.toString(),
            "dataCompra" to binding.dataCompra.text.toString()
        )

        db.collection("Usuarios").document(emailUsuario)
            .collection("Rebanhos").document(nomeRebanhoAtual!!)
            .update(dadosAtualizados) // Usa .update() para modificar campos existentes
            .addOnSuccessListener {
                Toast.makeText(this, "Rebanho atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                finish() // Fecha a activity e volta para a lista
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao atualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Função genérica para exibir diálogos de confirmação
    private fun exibirDialogoConfirmacao(titulo: String, mensagem: String, acaoPositiva: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensagem)
            .setPositiveButton("Confirmar") { dialog, _ ->
                acaoPositiva()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun configurarSpinners() {
        val origens = resources.getStringArray(R.array.origem_rebanho)
        val origemAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, origens)
        binding.spinnerOrigemLote.setAdapter(origemAdapter)

        val tiposGado = resources.getStringArray(R.array.tipo_gado)
        val tipoGadoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tiposGado)
        binding.spinnerTipoDeGado.setAdapter(tipoGadoAdapter)
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
            this,
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
}
