# AgroTrack 🐄

**Sistema de Gestão Pecuária para Android**

AgroTrack é um aplicativo Android desenvolvido para auxiliar produtores rurais no gerenciamento e controle de rebanhos bovinos, oferecendo funcionalidades completas para cadastro, monitoramento de custos, vendas e relatórios financeiros.

## 📱 Funcionalidades

### 🏠 **Tela Inicial (Dashboard)**

- Resumo financeiro com ganhos totais
- Custos do mês atual
- Total de animais no rebanho
- Saudação personalizada ao usuário

### 🐂 **Gestão de Rebanhos**

- **Cadastro de Rebanhos**: Registro completo com nome, origem, quantidade, tipo de gado e data de compra
- **Edição de Rebanhos**: Modificação de dados existentes
- **Exclusão de Rebanhos**: Remoção segura com confirmação
- **Tipos de Gado Suportados**: Cria, Recria, Engorda, Matrizes, Leiteiro, Misto

### 💰 **Controle Financeiro**

#### Vendas e Receitas

- Registro de vendas com dados completos
- Compradores: Frigorífico, Leilão
- Métodos de pagamento: PIX, Cheque, Boleto, Dinheiro
- Baixa automática no estoque do rebanho
- Controle de quantidade de animais vendidos

#### Custos e Despesas

- **Categorias de Custos**:
  - Alimentação (Ração, Sal Mineral, Silagem)
  - Medicamentos (Vacinas, Vermífugos, Antibióticos)
  - Mão de Obra (Salário Fixo, Diarista, Terceirizado)
  - Manutenção (Cercas, Maquinário, Instalações)
  - Impostos
  - Outros custos genéricos

### 📊 **Relatórios**

- Análise financeira detalhada
- Gráficos de desempenho
- Histórico de transações

### 🔐 **Autenticação**

- Sistema de login seguro
- Registro de novos usuários
- Integração com Firebase Authentication

## 🛠️ Tecnologias Utilizadas

### **Frontend**

- **Kotlin** - Linguagem principal
- **Android SDK** (API 24-36)
- **View Binding** - Vinculação de views
- **Navigation Component** - Navegação entre fragments
- **Material Design** - Interface moderna

### **Backend & Banco de Dados**

- **Firebase Firestore** - Banco de dados NoSQL
- **Firebase Authentication** - Autenticação de usuários
- **Google Services** - Integração com serviços Google

### **Bibliotecas Principais**

- **MPAndroidChart v3.1.0** - Gráficos e visualizações de dados
- **RecyclerView 1.4.0** - Listas dinâmicas e adaptadores
- **Kotlin Coroutines 1.7.3** - Programação assíncrona e operações em background
- **Credentials API 1.6.0-beta03** - Gerenciamento seguro de credenciais
- **Google Identity 1.1.1** - Autenticação com Google
- **Firebase BOM 33.1.2** - Gerenciamento de versões Firebase
- **Firebase Auth KTX** - Autenticação de usuários
- **Firebase Firestore KTX 26.0.2** - Banco de dados em tempo real

## 📁 Estrutura do Projeto

```
app/src/main/java/com/example/agrotrack/
├── ui/
│   ├── cadastro_rebanho/       # Cadastro de rebanhos
│   │   ├── CadastroFragment.kt
│   │   └── RebanhoDataClass.kt
│   ├── custos/                 # Gestão de custos e despesas
│   │   ├── CustoDataClass.kt
│   │   └── Custos_e_DespesasFragment.kt
│   ├── editar/                 # Edição de rebanhos
│   │   ├── EditarRebanhoActivity.kt
│   │   ├── RebanhoAdapter.kt
│   │   ├── RebanhoDataClass.kt
│   │   └── SelecaoRebanhoFragment.kt
│   ├── excluir/                # Exclusão de rebanhos
│   │   └── ExcluirFragment.kt
│   ├── home/                   # Tela inicial/dashboard
│   │   └── HomeFragment.kt
│   ├── login/                  # Autenticação
│   │   └── LoginActivity.kt
│   ├── registro/               # Registro de usuários
│   │   └── RegistrarUsuarioActivity.kt
│   ├── relatorio/              # Relatórios e análises
│   │   └── RelatorioFragment.kt
│   └── vendas/                 # Vendas e receitas
│       ├── VendaDataClass.kt
│       └── Vendas_e_ReceitasFragment.kt
└── MainActivity.kt             # Atividade principal
```

## 🚀 Como Executar

### **Pré-requisitos**

- Android Studio Hedgehog ou superior
- JDK 11 ou superior
- SDK Android (API 24-36)
- Gradle 8.13
- Conta no Firebase (Authentication + Firestore habilitados)

### **Configuração**

1. **Clone o repositório**

```bash
git clone [URL_DO_REPOSITORIO]
cd AgroTrack
```

2. **Configuração do Firebase**

   - Crie um projeto no [Firebase Console](https://console.firebase.google.com/)
   - Adicione o arquivo `google-services.json` em `app/`
   - Configure Authentication e Firestore

3. **Configuração de API Keys**

   - O arquivo `local.properties` já existe na raiz do projeto
   - Adicione sua API key (se necessário):

   ```properties
   API_KEY="sua_api_key_aqui"
   ```
   
   - A API key é injetada automaticamente no BuildConfig para builds debug e release

4. **Build e Execução**
   - Abra o projeto no Android Studio
   - Sincronize o projeto (Sync Project)
   - Execute no emulador ou dispositivo físico

## 📊 Estrutura do Banco de Dados (Firestore)

```
Usuarios/
└── {email_usuario}/
    └── Rebanhos/
        └── {nome_rebanho}/
            ├── dados_rebanho
            ├── Vendas/
            │   └── {venda_id}
            └── Custos/
                └── {custo_id}
```

## 🎯 Funcionalidades Principais

### **Data Classes**

#### RebanhoDataClass
```kotlin
data class RebanhoDataClass(
    val nome: String,
    val origem: String,
    val quantidadeInicial: Int,
    val tipo: String,
    val dataCompra: String,
    val valorCompra: Double?
)
```

#### VendaDataClass
```kotlin
data class VendaDataClass(
    val rebanhoEnvolvido: String,
    val dataVenda: String,
    val valorTotal: Double,
    val comprador: String,
    val metodoPagamento: String,
    val quantidadeAnimais: Int,
    val baixaAutomatica: Boolean
)
```

#### CustoDataClass
```kotlin
data class CustoDataClass(
    val rebanhoAssociado: String,
    val tipoCusto: String,
    val subcategoria: String,
    val descricao: String,
    val dataCusto: String,
    val valorTotal: Double
)
```

### **Recursos Especiais**

- **Baixa Automática**: Reduz automaticamente o estoque ao registrar vendas
- **Filtros por Data**: Custos filtrados por mês atual no dashboard
- **Validação de Campos**: Verificação de dados obrigatórios
- **Diálogos de Confirmação**: Confirmação para ações críticas
- **Navegação por Drawer**: Menu lateral com navegação entre módulos
- **Logout Seguro**: Limpeza de sessão e redirecionamento para login
- **Cálculos em Tempo Real**: Somatórios automáticos de receitas e custos
- **Formatação de Moeda**: Valores exibidos em formato brasileiro (R$)

## 📱 Requisitos do Sistema

- **Android**: 7.0 (API 24) ou superior
- **RAM**: Mínimo 2GB recomendado
- **Armazenamento**: 50MB livres
- **Conexão**: Internet para sincronização com Firebase

## 🔧 Configurações de Build

- **Compile SDK**: 36
- **Min SDK**: 24
- **Target SDK**: 36
- **Version Code**: 1
- **Version Name**: 1.0
- **Gradle**: 8.13
- **Kotlin**: 2.0.21
- **Android Gradle Plugin**: 8.12.3
- **Java Version**: 11 (sourceCompatibility & targetCompatibility)
- **JVM Target**: 11

### **Build Features Habilitados**
- View Binding
- BuildConfig

**AgroTrack** - Transformando a gestão pecuária através da tecnologia 🚀
