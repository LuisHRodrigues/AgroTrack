package com.example.agrotrack.ui.registro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.agrotrack.BuildConfig
import com.example.agrotrack.MainActivity
import com.example.agrotrack.R
import com.example.agrotrack.databinding.ActivityRegistrarUsuarioBinding
import com.example.agrotrack.ui.base.BaseActivity
import com.example.agrotrack.utils.hideSystemUI
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegistrarUsuarioActivity : BaseActivity() {

    // Gerencia as views do layout (substitui o findViewById)
    private lateinit var binding: ActivityRegistrarUsuarioBinding
    // Ponto de entrada para o Firebase Authentication (Auth)
    private lateinit var auth: FirebaseAuth
    // Novo gerenciador de credenciais do Android (para Google Sign-In)
    private lateinit var credentialManager: CredentialManager

    val apiKey = BuildConfig.API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Define o layout inicial (necessário antes do binding)
        setContentView(R.layout.activity_registrar_usuario)

        window.hideSystemUI()

        // Configura o layout para ocupar a tela inteira (edge-to-edge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registrarUsuario)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Infla o layout usando ViewBinding
        binding = ActivityRegistrarUsuarioBinding.inflate(layoutInflater)
        // Define o layout principal da Activity como a raiz do binding
        setContentView(binding.root)

        // Inicializa os serviços
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        // Configura os listeners de clique
        setupListeners()
        voltar()
    }
    

    /**
     * Centraliza a configuração dos listeners de clique da UI.
     */
    private fun setupListeners() {
        // Listener para o botão de cadastro com Google
        binding.btnGoogle.setOnClickListener {
            iniciarLoginComGoogle()
        }

        // Listener para o botão de cadastro com Email/Senha
        binding.btnCadastrar.setOnClickListener {
            validarERegistrarComEmail()
        }
    }

    // --- FLUXO 1: REGISTRO COM EMAIL E SENHA ---

    /**
     * Valida os campos de entrada do formulário de email.
     */
    private fun validarERegistrarComEmail() {
        // 1. Coleta os dados dos campos
        val nomeCompleto = binding.etNomeCompleto.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val telefone = binding.etTelefone.text.toString().trim()
        val cpf = binding.etCpf.text.toString().trim()
        val senha = binding.etSenha.text.toString()
        val confirmarSenha = binding.etConfirmarSenha.text.toString()
        val termos = binding.cbTermos.isChecked

        // 2. Executa as validações
        if (nomeCompleto.isEmpty() || email.isEmpty() || telefone.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
            Snackbar.make(binding.root, "Preencha todos os campos", Snackbar.LENGTH_SHORT).show()
        } else if (senha != confirmarSenha) {
            Snackbar.make(binding.root, "As senhas não coincidem", Snackbar.LENGTH_SHORT).show()
        } else if (senha.length <= 6) { // Firebase exige 6 caracteres no mínimo
            Snackbar.make(binding.root, "A senha deve ter no mínimo 6 caracteres", Snackbar.LENGTH_SHORT).show()
        } else if (!termos) {
            Snackbar.make(binding.root, "Aceite os termos e condições", Snackbar.LENGTH_SHORT).show()
        } else {
            // 3. Se tudo for válido, inicia o registro no Firebase Auth
            registrarUsuarioComEmail(email, senha, nomeCompleto, telefone, cpf)
        }
    }

    /**
     * Cria o usuário no Firebase Auth e atualiza o perfil.
     */
    private fun registrarUsuarioComEmail(email: String, senha: String, nomeCompleto: String, telefone: String, cpf: String) {
        // Cria o usuário no Firebase Authentication
        auth.createUserWithEmailAndPassword(email, senha).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Se criou com sucesso, pega o usuário atual
                val usuario = auth.currentUser
                // Prepara a atualização do perfil para incluir o nome
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(nomeCompleto)
                    .build()

                // Executa a atualização do perfil
                usuario?.updateProfile(profileUpdates)?.addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        // Se o perfil foi atualizado, salva os dados no Firestore
                        salvarUsuarioNoFirestore(usuario.uid, nomeCompleto, email, telefone, cpf)
                    } else {
                        // Falha ao ATUALIZAR o perfil (usuário já foi criado no Auth)
                        Snackbar.make(binding.root, "Falha ao atualizar perfil: ${updateTask.exception?.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Falha ao CRIAR o usuário (ex: email já existe, senha fraca)
                Snackbar.make(binding.root, "Erro ao registrar: ${task.exception?.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // --- FLUXO 2: REGISTRO COM GOOGLE ---

    /**
     * Inicia o fluxo de login/registro com Google usando o Credential Manager.
     */
    private fun iniciarLoginComGoogle() {
        // Configura a opção de login do Google, pedindo o ID do cliente web
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(apiKey)
            .build()

        // Cria a requisição de credencial
        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // Inicia uma Coroutine (Main thread) pois o 'getCredential' é uma 'suspend function'
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Solicita a credencial ao Google (abre o pop-up "Sign in with Google")
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@RegistrarUsuarioActivity,
                )
                // Se o usuário selecionar uma conta, processa o resultado
                handleSignInWithGoogle(result.credential)
            } catch (e: GetCredentialException) {
                // Captura exceções (ex: usuário fechou o pop-up, falha de rede)
                Log.e("GoogleSignIn", "Falha ao obter credencial: ${e.message}")
                Snackbar.make(binding.root, "Falha no login com Google: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Processa a credencial do Google e autentica no Firebase.
     */
    private fun handleSignInWithGoogle(credential: Credential) {
        // Extrai o token de ID do Google da credencial recebida
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val googleIdToken = googleIdTokenCredential.idToken

        if (googleIdToken != null) {
            // Converte o token do Google em uma credencial do Firebase
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)

            // Faz o login (ou registro) no Firebase com essa credencial
            auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login/Registro no Firebase foi bem-sucedido
                    val usuario = auth.currentUser
                    if (usuario != null) {
                        // Salva/Atualiza os dados no Firestore
                        salvarUsuarioNoFirestore(
                            usuario.uid,
                            usuario.displayName ?: "Usuário Google", // Pega o nome da conta Google
                            usuario.email ?: "", // Pega o email da conta Google
                            "", // Não tem telefone no Google
                            "" // Não tem CPF no Google
                        )
                    } else {
                        Snackbar.make(binding.root, "Erro ao obter dados do usuário.", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    // Falha ao autenticar no Firebase com a credencial Google
                    Snackbar.make(binding.root, "Falha na autenticação com Firebase: ${task.exception?.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        } else {
            // Falha grave: Token do Google veio nulo
            Snackbar.make(binding.root, "Erro: Token do Google nulo.", Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * Salva (ou atualiza) os dados do usuário no Firestore.
     * Esta função é chamada tanto pelo fluxo de email quanto pelo fluxo do Google.
     */
    private fun salvarUsuarioNoFirestore(uid: String, nomeCompleto: String, email: String, telefone: String, cpf: String) {
        // Pega a instância do Firestore
        val db = Firebase.firestore

        // Cria um mapa (HashMap) com os dados do usuário
        val dadosUsuario = hashMapOf(
            "nomeCompleto" to nomeCompleto,
            "email" to email,
            "telefone" to telefone,
            "cpf" to cpf
        )

        // Salva os dados na coleção "Usuarios"
        // Usar .document(email).set() é a chave:
        // 2. Se o documento já existe (ex: usuário do Google), ele apenas atualiza os dados.
        db.collection("Usuarios")
            .document(email)
            .set(dadosUsuario)

            .addOnSuccessListener {
                // Sucesso: avisa o usuário e navega para a tela principal
                Snackbar.make(binding.root, "Cadastro realizado com sucesso!", Snackbar.LENGTH_SHORT).show()

                // Navega para a MainActivity
                val intent = Intent(this, MainActivity::class.java)

                // Limpa a pilha de atividades para que o usuário não possa "voltar" para a tela de registro
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)

                finish() // Fecha a tela de registro
            }
            .addOnFailureListener { e ->
                // Falha ao salvar no BANCO DE DADOS (autenticação já foi feita)
                Snackbar.make(binding.root, "Erro ao salvar dados: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    fun voltar() {
        binding.tvFazerLogin.setOnClickListener {
            finish()
        }
    }
}