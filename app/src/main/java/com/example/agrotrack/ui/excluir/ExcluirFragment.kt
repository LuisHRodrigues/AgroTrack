package com.example.agrotrack.ui.excluir

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agrotrack.R
import com.example.agrotrack.databinding.FragmentExcluirRebanhoBinding
import com.example.agrotrack.ui.editar.RebanhoAdapter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ExcluirFragment: Fragment() {

    private var _binding: FragmentExcluirRebanhoBinding? = null
    private val binding get() = _binding!!
    private lateinit var recyclerViewRebanhos: RecyclerView
    private lateinit var rebanhoAdapter: RebanhoAdapter
    private val db = Firebase.firestore

    private lateinit var auth: FirebaseAuth

    private val listaRebanhosFirebase = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Usa o mesmo layout do fragment de seleção, o que é ótimo para reutilização.
        return inflater.inflate(R.layout.fragment_excluir_rebanho, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewRebanhos = view.findViewById(R.id.recyclerViewRebanhos)
        setupRecyclerView()

        auth = Firebase.auth

        // Chama a função para buscar os dados do Firebase assim que a view é criada
        buscarRebanhosDoUsuario()


    }
    // --- LÓGICA DE EXCLUSÃO EM CASCATA ---
    private fun executarExclusaoEmCascata(nomeRebanho: String) {
        val email = auth.currentUser?.email ?: return

        // Bloqueia a UI para o usuário não clicar de novo
        //binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val rebanhoRef = db.collection("Usuarios").document(email)
                    .collection("Rebanhos").document(nomeRebanho)

                // 1. Buscar e Deletar todas as VENDAS
                val vendasSnapshot = rebanhoRef.collection("Vendas").get().await()
                for (doc in vendasSnapshot) {
                    doc.reference.delete().await()
                }

                // 2. Buscar e Deletar todos os CUSTOS
                val custosSnapshot = rebanhoRef.collection("Custos").get().await()
                for (doc in custosSnapshot) {
                    doc.reference.delete().await()
                }

                // 4. Finalmente, deleta o documento do REBANHO
                rebanhoRef.delete().await()

                // Sucesso: Volta para a thread principal para atualizar a tela
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Rebanho e dados vinculados excluídos.", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ExcluirRebanho", "Erro ao excluir: ", e)
                    Toast.makeText(requireContext(), "Erro ao excluir: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun setupRecyclerView() {
        // O adapter é inicializado com a lista e uma ação de clique (lambda)
        rebanhoAdapter = RebanhoAdapter(listaRebanhosFirebase) { rebanhoSelecionado ->
            // A ação de clique agora recebe o nome do rebanho e chama a função onRebanhoClick
            onRebanhoClick(rebanhoSelecionado)
        }

        recyclerViewRebanhos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rebanhoAdapter
        }
    }

    //A função onRebanhoClick recebe o nome do rebanho como parametro
    private fun onRebanhoClick(nomeRebanho: String) {
        // Exibe um diálogo de confirmação antes de excluir
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir o rebanho '$nomeRebanho'? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { dialog, _ ->
                // Se o usuário confirmar, chama a função para excluir o rebanho
                executarExclusaoEmCascata(nomeRebanho)
                excluirRebanho(nomeRebanho)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun buscarRebanhosDoUsuario() {
        val usuarioAtual = auth.currentUser
        if (usuarioAtual != null && usuarioAtual.email != null) {
            pegarRebanhosFirebase(usuarioAtual.email!!)
        } else {
            Log.w("Firestore", "Usuário não logado ou sem e-mail.")
            Toast.makeText(requireContext(), "Erro: Usuário não autenticado.", Toast.LENGTH_LONG).show()
        }
    }

    private fun pegarRebanhosFirebase (email: String){
        db.collection("Usuarios")
            .document(email)
            .collection("Rebanhos")
            .get()
            .addOnSuccessListener { documents ->
                listaRebanhosFirebase.clear() // Limpa a lista antes de adicionar os novos dados
                if (documents.isEmpty) {
                    Log.d("Firestore", "Usuário não tem rebanhos cadastrados.")
                    Toast.makeText(requireContext(), "Nenhum rebanho encontrado.", Toast.LENGTH_SHORT).show()
                } else {
                    for (document in documents) {
                        listaRebanhosFirebase.add(document.id)
                        Log.d("Firestore", "Rebanho encontrado: ${document.id}")
                    }
                }
                // Notifica o adapter que os dados foram atualizados, mesmo se a lista estiver vazia
                rebanhoAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Erro ao buscar rebanhos", exception)
                Toast.makeText(requireContext(), "Falha ao carregar rebanhos.", Toast.LENGTH_SHORT).show()
            }
    }

    // A função excluirRebanho recebe o nome do rebanho a ser excluído
    private fun excluirRebanho(nomeRebanho: String) {
        val emailUsuario = auth.currentUser?.email
        if (emailUsuario == null) {
            Toast.makeText(requireContext(), "Erro de autenticação.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("Usuarios").document(emailUsuario)
            .collection("Rebanhos").document(nomeRebanho)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "'$nomeRebanho' excluído com sucesso!", Toast.LENGTH_SHORT).show()
                //Remove o item da lista local e notifica o adapter para atualizar a UI instantaneamente
                listaRebanhosFirebase.remove(nomeRebanho)
                rebanhoAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Falha ao excluir: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
