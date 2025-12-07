package com.example.agrotrack.ui.cadastro_rebanho

data class RebanhoDataClass(
    val nome: String = "",
    val origem: String = "",
    val quantidadeInicial: Int = 0,
    val tipo: String = "",
    val dataCompra: String = "",
    val valorCompra: Double? = null
)
