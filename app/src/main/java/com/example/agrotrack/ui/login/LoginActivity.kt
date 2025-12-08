package com.example.agrotrack.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.text
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.agrotrack.MainActivity
import com.example.agrotrack.databinding.ActivityLoginBinding
import com.example.agrotrack.ui.registro.RegistrarUsuarioActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.example.agrotrack.BuildConfig
import com.example.agrotrack.R
import com.example.agrotrack.ui.base.BaseActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private val db = Firebase.firestore

    //call BuildConfig to access its API_KEY Constant
    val apiKey = BuildConfig.API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Firebase Auth e o CredentialManager
        auth = Firebase.auth
        credentialManager = CredentialManager.create(this)

        // Configura os listeners de clique
        setupListeners()

        binding.tvEsqueceuSenha.setOnClickListener {
            mostrarDialogoEsqueciSenha()
        }

    }

    public override fun onStart() {
        super.onStart()
        // Verifica se o usuário já está logado ao iniciar a activity
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Se já está logado, vai direto para a MainActivity
            Log.d("LoginActivity", "Usuário já logado, redirecionando para MainActivity.")
            updateUI(currentUser)
        }
        // Se não estiver logado, permanece nesta tela.
    }

    /**
     * Centraliza a configuração dos listeners de clique da UI.
     */
    private fun setupListeners() {
        // Listener para o botão de login com Google (usa Credential Manager)
        binding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        }

        // Listener para o texto "Cadastre-se"
        binding.tvCadastrese.setOnClickListener {
            val intent = Intent(this, RegistrarUsuarioActivity::class.java)
            startActivity(intent)
        }

        // Listener para o botão de login com E-mail e Senha
        binding.btnEntrar.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etSenha.text.toString()

            // Validação simples de campos
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Para a execução aqui
            }

            // Chama a função de login com e-mail/senha
            loginComEmailESenha(email, password)
        }
    }

    // LOGIN COM EMAIL E SENHA

    /**
     * Autentica o usuário no Firebase usando E-mail e Senha.
     */
    private fun loginComEmailESenha(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sucesso no login
                    Log.d("LoginActivity", "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user) // Navega para a próxima tela
                } else {
                    // Falha no login
                    Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Falha na autenticação.", Toast.LENGTH_SHORT).show()
                    updateUI(null) // Garante que o usuário não seja redirecionado
                }
            }
    }

    // LOGIN COM CREDENTIAL MANAGER (GOOGLE, PASSKEYS, SENHAS SALVAS)

    /**
     * Inicia o fluxo de login unificado via CredentialManager.
     * Isso vai abrir o pop-up "Fazer login com..." do Google.
     */
    private fun signInWithGoogle() {
        // Configuração para o pedido de credencial do Google
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            // false: sempre mostra a tela de seleção de contas.
            // true: tenta usar a conta já autorizada automaticamente (login com "um toque").
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(apiKey) // ID do cliente web
            .build()

        // Monta o pedido para o CredentialManager, incluindo a opção do Google
        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // A chamada ao CredentialManager (getCredential) é uma 'suspend function'
        // Por isso, precisa ser chamada dentro de uma Coroutine (lifecycleScope)
        lifecycleScope.launch {
            try {
                // Solicita a credencial (exibe o pop-up do Google)
                val result = credentialManager.getCredential(this@LoginActivity, request)
                // Se o usuário escolher uma opção, processa o resultado
                handleSignInResult(result)
            } catch (e: GetCredentialException) {
                // Trata exceções comuns do CredentialManager
                when (e) {
                    is NoCredentialException -> {
                        // Usuário fechou o pop-up ou não tem credenciais
                        Log.d("LoginActivity", "Nenhuma credencial encontrada ou usuário cancelou.")
                        Toast.makeText(this@LoginActivity, "Nenhuma conta selecionada.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        // Outro erro (rede, configuração, etc.)
                        Log.e("LoginActivity", "GetCredential falhou", e)
                        Toast.makeText(this@LoginActivity, "Falha no login: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * Processa a credencial recebida do CredentialManager.
     * O CredentialManager pode retornar diferentes tipos de credenciais.
     */
    private fun handleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential

        when (credential) {
            // CASO 1: Login padrão com "Conta Google".
            is GoogleIdTokenCredential -> {
                Log.d("LoginActivity", "Credencial recebida: GoogleIdTokenCredential")
                val googleIdToken = credential.idToken
                // Converte o token do Google em uma credencial do Firebase
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                // Autentica no Firebase
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            updateUI(auth.currentUser)
                        } else {
                            Toast.makeText(this, "Falha na autenticação com Firebase.", Toast.LENGTH_SHORT).show()
                            updateUI(null)
                        }
                    }
            }

            // CASO 2: Tipo genérico de credencial.
            is CustomCredential -> {
                Log.d("LoginActivity", "Credencial recebida: CustomCredential (tipo: ${credential.type})")
                // Verifica se é o tipo específico do Google (redundante, mas seguro)
                if (credential.type == "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL") {
                    try {
                        // O fluxo é o mesmo: extrair o token e autenticar no Firebase
                        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    updateUI(auth.currentUser)
                                } else {
                                    Toast.makeText(this, "Falha na autenticação (custom) com Firebase.", Toast.LENGTH_SHORT).show()
                                    updateUI(null)
                                }
                            }
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Falha ao processar CustomCredential do Google", e)
                        Toast.makeText(this, "Erro ao processar credencial do Google.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("LoginActivity", "Tipo de CustomCredential não suportado: ${credential.type}")
                    Toast.makeText(this, "Este tipo de login personalizado não é suportado.", Toast.LENGTH_SHORT).show()
                }
            }

            // CASO 3: Tipo desconhecido (tratamento de erro).
            else -> {
                Log.e("LoginActivity", "Tipo de credencial inesperado: ${credential.javaClass.name}")
                Toast.makeText(this, "Tipo de credencial não suportado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Verifica se o usuário autenticado existe no Firestore antes de navegar.
     */
    private fun updateUI(user: FirebaseUser?) {
        if (user?.email == null) {
            // Se o usuário for nulo ou não tiver e-mail, não faz nada.
            // Isso também trata falhas de login.
            return
        }

        val email = user.email!!
        Log.d("LoginActivity", "Usuário autenticado com e-mail: $email. Verificando no Firestore.")

        // Consulta o Firestore para ver se o documento do usuário existe
        db.collection("Usuarios").document(email).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // << SUCESSO: O usuário existe no Auth E no Firestore >>
                    Log.d("LoginActivity", "Documento encontrado no Firestore. Navegando para MainActivity.")
                    // Redireciona para a tela principal
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // << FALHA: O usuário existe no Auth, MAS NÃO no Firestore >>
                    Log.w("LoginActivity", "Usuário autenticado, mas não encontrado no Firestore. Negando acesso.")
                    Toast.makeText(this, "Este usuário não está cadastrado em nosso sistema.", Toast.LENGTH_LONG).show()

                    // Importante: Desloga o usuário do Firebase Auth para forçar um novo login ou registro.
                    auth.signOut()
                }
            }
            .addOnFailureListener { exception ->
                // Trata erros de conexão com o Firestore
                Log.e("LoginActivity", "Erro ao verificar usuário no Firestore", exception)
                Toast.makeText(this, "Erro ao conectar com o banco de dados.", Toast.LENGTH_SHORT).show()
                auth.signOut()
            }
    }

    /**
     * Exibe um pop-up para o usuário digitar o email para redefinição de senha.
     */
    private fun mostrarDialogoEsqueciSenha() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_esqueci_senha, null)
        val campoEmail = dialogView.findViewById<EditText>(R.id.etEmailRedefinicao)

        builder.setView(dialogView)
            .setPositiveButton("Enviar") { dialog, _ ->
                val email = campoEmail.text.toString().trim()

                // Valida o campo de email antes de enviar
                if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    enviarEmailRedefinicao(email)
                } else {
                    Toast.makeText(this, "Por favor, insira um email válido", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Usa o Firebase Auth para enviar o email de redefinição.
     */
    private fun enviarEmailRedefinicao(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Usa Snackbar para uma mensagem mais visível e elegante
                    Snackbar.make(binding.root, "Email de redefinição enviado para $email",
                        Snackbar.LENGTH_LONG).show()
                } else {
                 Snackbar.make(binding.root, "Falha ao enviar email: ${task.exception?.message}", Snackbar.LENGTH_LONG).show()
                }
            }
    }
}