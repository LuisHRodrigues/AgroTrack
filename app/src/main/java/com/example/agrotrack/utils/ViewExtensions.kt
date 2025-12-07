package com.example.agrotrack.utils // Use o seu nome de pacote

import android.os.Build
import android.view.View
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Função de extensão para a classe Window que habilita o modo de tela cheia,
 * escondendo tanto a barra de status (topo) quanto a barra de navegação (inferior).
 *
 * @param isSticky Define se o modo imersivo será "pegajoso". Se true, as barras
 * reaparecem de forma translúcida ao deslizar das bordas e somem novamente
 * após alguns segundos.
 */
fun Window.hideSystemUI(isSticky: Boolean = true) {
    // 1. Permite que o layout da Activity se estenda por trás das barras do sistema.
    WindowCompat.setDecorFitsSystemWindows(this, false)

    // 2. Obtém o controlador das barras do sistema.
    val controller = WindowInsetsControllerCompat(this, this.decorView)

    // 3. Esconde as barras do sistema (status bar e navigation bar).
    controller.hide(WindowInsetsCompat.Type.systemBars())

    // 4. Define o comportamento das barras quando o usuário interage.
    if (isSticky) {
        // Modo "pegajoso": as barras reaparecem temporariamente e somem de novo.
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        // Modo não-pegajoso: o usuário precisa deslizar da borda para as barras aparecerem,
        // e elas permanecem visíveis até que a função hideSystemUI seja chamada novamente.
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
    }
}
