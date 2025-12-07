package com.example.agrotrack.ui.vendas

data class VendaDataClass(

    val rebanhoEnvolvido: String = "",
    val dataVenda: String = "",
    val valorTotal: Double = 0.0,
    val comprador: String = "",
    val metodoPagamento: String = "",
    val quantidadeAnimais: Int = 0,
    val baixaAutomatica: Boolean = false
)
