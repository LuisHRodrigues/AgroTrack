# AgroTrack 🐄

**Sistema de Gestão Pecuária para Android**

AgroTrack é um aplicativo Android nativo desenvolvido em Kotlin para auxiliar produtores rurais no gerenciamento e controle de rebanhos bovinos. Oferece funcionalidades completas para cadastro, monitoramento de custos, vendas e relatórios financeiros, com sincronização em tempo real via Firebase.

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

### **Bibliotecas e Dependências**

#### Core Android
- **AndroidX Core KTX 1.17.0** - Extensões Kotlin para Android
- **AppCompat 1.7.1** - Compatibilidade com versões antigas
- **Material Design 1.13.0** - Componentes Material Design
- **ConstraintLayout 2.2.1** - Layouts responsivos
- **RecyclerView 1.4.0** - Listas dinâmicas e adaptadores

#### Arquitetura e Navegação
- **Lifecycle LiveData KTX 2.9.4** - Observação de dados reativos
- **Lifecycle ViewModel KTX 2.9.4** - Gerenciamento de estado da UI
- **Navigation Fragment KTX 2.9.5** - Navegação entre fragments
- **Navigation UI KTX 2.9.5** - Integração com componentes de UI

#### Firebase
- **Firebase BOM 33.1.2** - Gerenciamento centralizado de versões Firebase
- **Firebase Auth KTX** - Autenticação de usuários
- **Firebase Firestore KTX 26.0.2** - Banco de dados NoSQL em tempo real

#### Autenticação e Credenciais
- **Credentials API 1.6.0-beta03** - Gerenciamento seguro de credenciais
- **Credentials Play Services Auth 1.6.0-beta03** - Integração com Google Play Services
- **Google Identity 1.1.1** - Autenticação com Google

#### Programação Assíncrona
- **Kotlin Coroutines Play Services 1.7.3** - Integração de Coroutines com Firebase

#### Visualização de Dados
- **MPAndroidChart v3.1.0** - Gráficos e visualizações de dados

#### Testes
- **JUnit 4.13.2** - Testes unitários
- **AndroidX JUnit 1.3.0** - Testes instrumentados
- **Espresso Core 3.7.0** - Testes de UI

## 📁 Estrutura do Projeto

```
AgroTrack/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/agrotrack/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── base/
│   │   │   │   │   │   └── BaseActivity.kt           # Activity base com modo imersivo
│   │   │   │   │   ├── cadastro_rebanho/
│   │   │   │   │   │   ├── CadastroFragment.kt       # Cadastro de rebanhos
│   │   │   │   │   │   └── RebanhoDataClass.kt       # Modelo de dados de rebanho
│   │   │   │   │   ├── custos/
│   │   │   │   │   │   ├── CustoDataClass.kt         # Modelo de dados de custo
│   │   │   │   │   │   └── Custos_e_DespesasFragment.kt
│   │   │   │   │   ├── editar/
│   │   │   │   │   │   ├── EditarRebanhoActivity.kt  # Edição de rebanhos
│   │   │   │   │   │   ├── RebanhoAdapter.kt         # Adapter para RecyclerView
│   │   │   │   │   │   ├── RebanhoDataClass.kt
│   │   │   │   │   │   └── SelecaoRebanhoFragment.kt
│   │   │   │   │   ├── excluir/
│   │   │   │   │   │   └── ExcluirFragment.kt        # Exclusão de rebanhos
│   │   │   │   │   ├── home/
│   │   │   │   │   │   └── HomeFragment.kt           # Dashboard principal
│   │   │   │   │   ├── login/
│   │   │   │   │   │   └── LoginActivity.kt          # Tela de autenticação
│   │   │   │   │   ├── registro/
│   │   │   │   │   │   └── RegistrarUsuarioActivity.kt
│   │   │   │   │   ├── relatorio/
│   │   │   │   │   │   └── RelatorioFragment.kt      # Relatórios e gráficos
│   │   │   │   │   └── vendas/
│   │   │   │   │       ├── VendaDataClass.kt         # Modelo de dados de venda
│   │   │   │   │       └── Vendas_e_ReceitasFragment.kt
│   │   │   │   ├── utils/
│   │   │   │   │   └── ViewExtensions.kt             # Extensões para modo imersivo
│   │   │   │   └── MainActivity.kt                   # Activity principal com Navigation Drawer
│   │   │   ├── res/
│   │   │   │   ├── drawable/                         # Ícones e backgrounds
│   │   │   │   ├── layout/                           # Layouts XML
│   │   │   │   ├── menu/                             # Menus de navegação
│   │   │   │   ├── navigation/                       # Navigation graph
│   │   │   │   └── values/                           # Strings, cores, temas
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/                              # Testes instrumentados
│   │   └── test/                                     # Testes unitários
│   ├── build.gradle.kts                              # Configurações do módulo app
│   ├── google-services.json                          # Configuração Firebase
│   └── proguard-rules.pro
├── gradle/
│   ├── wrapper/
│   └── libs.versions.toml                            # Catálogo de versões centralizado
├── build.gradle.kts                                  # Configurações do projeto
├── settings.gradle.kts
├── local.properties                                  # Propriedades locais (API keys)
└── README.md
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
   - Adicione suas API keys (se necessário):

   ```properties
   API_KEY="sua_api_key_aqui"
   ```
   
   - As API keys são injetadas automaticamente no BuildConfig via Gradle
   - Disponíveis em `BuildConfig.API_KEY` para builds debug e release
   - O arquivo `local.properties` está no `.gitignore` para segurança

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

#### Funcionalidades de Negócio
- **Baixa Automática**: Reduz automaticamente o estoque ao registrar vendas
- **Filtros por Data**: Custos filtrados por mês atual no dashboard
- **Cálculos em Tempo Real**: Somatórios automáticos de receitas e custos usando Coroutines
- **Formatação de Moeda**: Valores exibidos em formato brasileiro (R$)
- **Sincronização em Tempo Real**: Dados sincronizados automaticamente com Firebase Firestore

#### Interface e Experiência do Usuário
- **Modo Imersivo**: Tela cheia com barras do sistema ocultas (BaseActivity + ViewExtensions)
- **Navigation Drawer**: Menu lateral com navegação fluida entre módulos
- **View Binding**: Acesso seguro e eficiente às views
- **Material Design**: Interface moderna e intuitiva
- **Saudação Personalizada**: Exibe nome do usuário no dashboard

#### Segurança e Validação
- **Validação de Campos**: Verificação de dados obrigatórios
- **Diálogos de Confirmação**: Confirmação para ações críticas (exclusão)
- **Logout Seguro**: Limpeza de sessão e redirecionamento para login
- **Autenticação Firebase**: Sistema robusto de autenticação
- **API Keys Seguras**: Gerenciamento via local.properties e BuildConfig

## 📱 Requisitos do Sistema

### **Dispositivo**
- **Android**: 7.0 Nougat (API 24) ou superior
- **RAM**: Mínimo 2GB recomendado
- **Armazenamento**: 50MB livres
- **Conexão**: Internet obrigatória para sincronização com Firebase

### **Desenvolvimento**
- **Android Studio**: Hedgehog (2023.1.1) ou superior
- **JDK**: 11 ou superior
- **Gradle**: 8.13 (via wrapper)
- **Conta Firebase**: Projeto configurado com Authentication e Firestore

## 🔧 Configurações de Build

### **Versões**
- **Compile SDK**: 36
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 36
- **Version Code**: 1
- **Version Name**: 1.0

### **Ferramentas de Build**
- **Gradle**: 8.13
- **Android Gradle Plugin**: 8.12.3
- **Kotlin**: 2.0.21
- **Google Services Plugin**: 4.4.4

### **Configurações Java/Kotlin**
- **Java Source Compatibility**: 11
- **Java Target Compatibility**: 11
- **Kotlin JVM Target**: 11

### **Build Features Habilitados**
- **View Binding**: Vinculação segura de views sem findViewById
- **BuildConfig**: Geração de constantes de configuração (API keys)

### **Repositórios**
- Google Maven Repository
- Maven Central
- JitPack (para MPAndroidChart)

## 🎨 Padrões e Arquitetura

### **Padrões Utilizados**
- **Repository Pattern**: Acesso centralizado aos dados do Firebase
- **Data Classes**: Modelos imutáveis para representação de dados
- **View Binding**: Substituição segura do findViewById
- **Extension Functions**: Funções de extensão para código reutilizável (ViewExtensions)
- **Coroutines**: Programação assíncrona estruturada

### **Organização do Código**
- **Pacote ui**: Separação por feature (home, cadastro, vendas, custos, etc.)
- **Pacote utils**: Utilitários e extensões compartilhadas
- **Pacote base**: Classes base reutilizáveis (BaseActivity)
- **Data Classes**: Modelos de dados próximos às suas features

## 🔒 Segurança

- **Firebase Authentication**: Autenticação segura de usuários
- **Firestore Security Rules**: Controle de acesso aos dados
- **API Keys**: Armazenadas em `local.properties` (não versionado)
- **ProGuard**: Configurado para ofuscação em builds de release
- **HTTPS**: Todas as comunicações com Firebase são criptografadas

## 🚧 Melhorias Futuras

- [ ] Implementar testes unitários e instrumentados
- [ ] Adicionar suporte offline com cache local
- [ ] Implementar exportação de relatórios em PDF
- [ ] Adicionar notificações push para lembretes
- [ ] Implementar backup automático de dados
- [ ] Adicionar suporte a múltiplos idiomas
- [ ] Implementar modo escuro completo
- [ ] Adicionar gráficos mais detalhados no módulo de relatórios

## 📄 Licença

Este projeto é de uso educacional e demonstrativo.

## 👨‍💻 Desenvolvedor

Desenvolvido com ❤️ para auxiliar produtores rurais na gestão de seus rebanhos.

---

**AgroTrack** - Transformando a gestão pecuária através da tecnologia 🚀
