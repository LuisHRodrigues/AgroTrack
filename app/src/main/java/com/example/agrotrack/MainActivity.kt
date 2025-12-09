package com.example.agrotrack

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.agrotrack.databinding.ActivityMainBinding
import com.example.agrotrack.ui.base.BaseActivity
import com.example.agrotrack.ui.login.LoginActivity
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

// MainActivity é a tela principal do aplicativo que contém a navegação (menu lateral e fragmentos).
class MainActivity : BaseActivity() {

    // Configuração da barra de aplicativos para gerenciar o botão de navegação (voltar).
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    // Instância do Firebase Auth para gerenciar a autenticação do usuário.
    private lateinit var auth: FirebaseAuth

    // onCreate é chamado quando a activity é criada pela primeira vez. É aqui que a configuração inicial é feita.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Infla o layout da activity usando ViewBinding, preparando-o para ser exibido.
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Define o layout inflado como o conteúdo da activity.
        setContentView(binding.root)

        // Esconde o botão de ação flutuante (FAB) por padrão ao iniciar a tela.
        binding.appBarMain.fab.visibility = View.GONE

        // Define a toolbar personalizada como a barra de ação (ActionBar) da activity.
        setSupportActionBar(binding.appBarMain.toolbar)
        // Referências para o layout do menu lateral (Drawer) e a view de navegação.
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        // Controlador de navegação que gerencia a troca de fragmentos dentro do host (nav_host_fragment_content_main).
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Define os destinos de nível superior. Nessas telas, o ícone do menu (hambúrguer) será mostrado.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_tela_inicial, R.id.nav_cadastroRebanho, R.id.nav_editarRebanho, R.id.nav_relatorio, R.id.nav_excluirRebanho,R.id.nav_custosDespesas, R.id.nav_vendasReceitas
            ), drawerLayout
        )
        // Conecta a barra de ação com o controlador de navegação para atualizar o título e o ícone de navegação.
        setupActionBarWithNavController(navController, appBarConfiguration)
        // Conecta a view de navegação (menu lateral) com o controlador de navegação para que os cliques nos itens do menu funcionem.
        navView.setupWithNavController(navController)

        // Inicializa o serviço de autenticação do Firebase.
        auth = FirebaseAuth.getInstance()

        // Obtém o usuário atualmente logado no Firebase.
        val currentUser: FirebaseUser? = auth.currentUser

        // Se houver um usuário logado, atualiza o cabeçalho do menu lateral com suas informações.
        if (currentUser != null) {
            // Pega a view do cabeçalho do menu lateral (geralmente o primeiro item).
            val headerView = navView.getHeaderView(0)

            // Encontra os TextViews para nome e email dentro do cabeçalho.
            val nomeUsuarioTextView = headerView.findViewById<TextView>(R.id.tvNomeUsuario)
            val emailUsuarioTextView = headerView.findViewById<TextView>(R.id.tvEmailUsuario)

            // Define o nome e o email do usuário nos TextViews.
            nomeUsuarioTextView.text = currentUser.displayName ?: "Nome não disponível"
            emailUsuarioTextView.text = currentUser.email ?: "Email não disponível"
        }
    }

    // onCreateOptionsMenu é chamado para criar o menu de opções na barra de ação (geralmente os 3 pontinhos).
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Infla o layout do menu (R.menu.main) na barra de ação, adicionando os itens definidos no XML.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    // onOptionsItemSelected é chamado quando um item do menu de opções da barra de ação é selecionado.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Usa um "when" para tratar o clique baseado no ID do item selecionado.
        return when (item.itemId) {
            // Caso o item de logout (sair) seja selecionado.
            R.id.setting_logout -> {
                // Desloga o usuário da sessão atual do Firebase.
                FirebaseAuth.getInstance().signOut()
                // Cria uma intenção para navegar de volta para a tela de Login.
                val intent = Intent(this, LoginActivity::class.java)
                // Limpa o histórico de telas para que o usuário não possa voltar à tela principal pressionando "voltar".
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // Inicia a navegação para a tela de Login.
                startActivity(intent)
                // Finaliza a MainActivity para removê-la da pilha de atividades.
                finish()
                true // Retorna true para indicar que o evento foi tratado.
            }
            // Para qualquer outro item, delega o tratamento para a implementação padrão da superclasse.
            else -> super.onOptionsItemSelected(item)
        }
    }

    // onSupportNavigateUp é chamado quando o usuário clica no botão "voltar" ou no ícone do menu na barra de ação.
    override fun onSupportNavigateUp(): Boolean {
        // Pede ao controlador de navegação para lidar com a navegação "para cima" (ou voltar/abrir o menu).
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}