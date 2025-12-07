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

class MainActivity : BaseActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appBarMain.fab.visibility = View.GONE

        setSupportActionBar(binding.appBarMain.toolbar)
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_tela_inicial, R.id.nav_cadastroRebanho, R.id.nav_editarRebanho, R.id.nav_relatorio, R.id.nav_excluirRebanho,R.id.nav_custosDespesas, R.id.nav_vendasReceitas
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Inicializa o Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Pega o usuário logado atualmente
        val currentUser: FirebaseUser? = auth.currentUser

        // Verifica se há um usuário logado
        if (currentUser != null) {
            // Acessa a view do cabeçalho do NavigationView (ele está no índice 0)
            val headerView = navView.getHeaderView(0)

            // Encontra os TextViews dentro da view do cabeçalho
            val nomeUsuarioTextView = headerView.findViewById<TextView>(R.id.tvNomeUsuario)
            val emailUsuarioTextView = headerView.findViewById<TextView>(R.id.tvEmailUsuario)

            // Define o nome e o email nos TextViews
            // Usa o DisplayName se disponível, caso contrário, mostra um texto padrão
            nomeUsuarioTextView.text = currentUser.displayName ?: "Nome não disponível"
            emailUsuarioTextView.text = currentUser.email ?: "Email não disponível"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        return when (item.itemId) {
            R.id.setting_logout -> {
                // User chose the "logout" item, sign them out.
                FirebaseAuth.getInstance().signOut()
                // Create an Intent to go to the LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                // Add flags to clear the back stack and start a new task.
                // This prevents the user from navigating back to MainActivity after logging out.
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                // Finish the current activity
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
