package com.example.agrotrack.ui.base // Use o seu nome de pacote

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.agrotrack.utils.hideSystemUI // Importe a função que criamos antes

/**
 * Uma Activity base que aplica configurações comuns a todas as Activities do app.
 * Neste caso, ela aplica o modo de tela cheia (imersivo).
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Aplica o modo de tela cheia para qualquer Activity que herdar desta.
        window.hideSystemUI()
    }
}
