package com.example.agrotrack.ui.editar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agrotrack.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class SelecaoRebanhoFragment : Fragment() {

    private lateinit var recyclerViewRebanhos: RecyclerView
    private lateinit var rebanhoAdapter: RebanhoAdapter
    val db = Firebase.firestore

    private lateinit var auth: FirebaseAuth

    // Lista de rebanhos que será atualizada com os dados do Firebase
    private val listaRebanhosFirebase = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_selecao_editar_rebanho, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewRebanhos = view.findViewById(R.id.recyclerViewRebanhos)
        setupRecyclerView()

        //Inicialize o auth
        auth = Firebase.auth

        //chama a função para buscar os dados do Firebase
        val usuarioAtual = auth.currentUser
        if (usuarioAtual != null && usuarioAtual.email != null) {
            pegarRebanhosFirebase(usuarioAtual.email!!)
        } else {
            // Caso o usuário não esteja logado ou não tenha e-mail
            Log.w("Firestore", "Usuário não logado ou sem e-mail.")
            Toast.makeText(requireContext(), "Erro: Usuário não autenticado.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecyclerView() {
        //O adapter é inicializado com a lista (inicialmente vazia)
        rebanhoAdapter = RebanhoAdapter(listaRebanhosFirebase) { rebanhoSelecionado ->
            onRebanhoClick(rebanhoSelecionado)
        }

        recyclerViewRebanhos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rebanhoAdapter
        }
    }

    private fun onRebanhoClick(nomeRebanho: String) {
        Toast.makeText(requireContext(), "Selecionado: $nomeRebanho",
            Toast.LENGTH_SHORT).show()

        // Envia o nome do rebanho para a próxima tela
        val intent = Intent(requireContext(), EditarRebanhoActivity::class.java).apply {
            putExtra("NOME_REBANHO", nomeRebanho)
        }
        startActivity(intent)
    }

    //Metodo para pegar os rebanhos cadastrados do usuario logado
    fun pegarRebanhosFirebase (email: String){
        db.collection("Usuarios")
            .document(email)
            .collection("Rebanhos")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("Firestore", "Usuário não tem rebanhos cadastrados.")
                    Toast.makeText(requireContext(), "Nenhum rebanho encontrado.", Toast.LENGTH_SHORT).show()

                } else {
                    // Limpa a lista local antes de adicionar os novos dados
                    listaRebanhosFirebase.clear()

                    // Adiciona o nome de cada rebanho (que é o ID do documento) à lista
                    for (document in documents) {
                        listaRebanhosFirebase.add(document.id)
                        Log.d("Firestore", "Rebanho encontrado: ${document.id} => ${document.data}")
                    }

                    // Notifica o adapter que os dados foram atualizados
                    // Isso fará com que o RecyclerView seja redesenhado
                    rebanhoAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Erro ao buscar rebanhos", exception)
                Toast.makeText(requireContext(), "Falha ao carregar rebanhos.", Toast.LENGTH_SHORT).show()
            }
    }
}