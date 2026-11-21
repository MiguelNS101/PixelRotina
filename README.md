# 🌿 PixelRotina — Rotina Diária, Diário de Humor & Bem-Estar em Pixel Art

**PixelRotina** é um aplicativo Android nativo e **100% offline-first** desenvolvido em **Kotlin** e **Jetpack Compose** para organizar sua rotina diária e semanal, acompanhar seu **humor e energia ao longo do dia** e manter constância leve com estética retrô em **Pixel Art**.

Projetado para funcionar no seu próprio ritmo — com o aplicativo fechado, após o reinício do celular e sem depender de nuvem obrigatória — o PixelRotina combina lembretes inteligentes, alarme em tela cheia, Pomodoro, medalhas de sequência, diário emocional, gráficos nativos em `Canvas` e integração opcional com **Google Gemini**.

---

## ✨ Principais Funcionalidades

### 1. 💧 Avisos de Intervalo & Check-in Direto na Notificação (com Humor e Energia)
- Ideal para hábitos recorrentes ao longo do dia (beber água, descansar a vista, alongar, postura).
- Configure **intervalo em minutos**, **janela de horário ativa** e **meta diária** de confirmações.
- Cada notificação exibe o **ícone pixel art da própria atividade** e oferece 3 ações rápidas direto na barra do Android:
  - **`✓ OK! Feito`**: conclui a rotina instantaneamente sem abrir o app.
  - **`😊 OK + Humor`**: conclui a rotina e transforma silenciosamente a mesma notificação em um fluxo interativo de **2 passos** — **Passo 1/2 (Humor: Baixo / Neutro / Alto)** → **Passo 2/2 (Energia: Baixa / Média / Alta)** — salvando direto na sua média diária.
  - **`⏰ Adiar`**: soneca rápida configurável (+10m, +15m, +30m ou +60m).

### 2. 🔔 Horários Específicos & Alarme em Tela Cheia com Slide para Concluir
- Agende lembretes para horários fixos escolhendo os dias da semana ativos (Segunda a Domingo).
- Dois modos de alerta fora do app:
  - **📩 Notificação Padrão**: lembrete pontual com `OK! Feito`, `OK + Humor` e `Adiar`.
  - **🔔 Alarme Persistente em Tela Cheia**: toca e vibra continuamente (`ForegroundService` + `AlarmManager`) mesmo com a tela bloqueada, exibindo a **Arte em Pixel Art do Momento do Dia** e uma barra tátil **Deslize para Concluir (`Slide to Complete`)**, além de Soneca.

### 3. 🏅 Sistema de Streaks (Sequências) & Medalhas em Pixel Art
- Incentiva a constância diária com 3 níveis de medalhas desenhadas em pixel art:
  - **🥉 Bronze**: 50% das rotinas do dia concluídas.
  - **🥈 Prata**: 75% das rotinas do dia concluídas.
  - **🥇 Ouro**: 100% das rotinas do dia concluídas.
- Acompanha **sequência atual de dias**, **sequência Ouro (100%)** e **recorde de consistência**, com explosão de confetes 8-bit ao concluir tarefas.

### 4. 📖 Módulo de Diário, Humor & Energia (Média Diária + Notificação no Fim do Dia)
- **Check-in Rápido na Aba Hoje + Média Diária ao Vivo**: registre Humor (1–5) e Energia (1–5) em 2 toques várias vezes ao dia e acompanhe a **Média de Humor e Energia do dia** (`Nx HOJE`).
- **Humor & Energia ao Concluir Rotinas**: dentro do app, um modal leve e opcional (ligável/desligável em Ajustes) permite registrar como você está logo após concluir qualquer rotina.
- **Notificação Configurável de Fim do Dia**: configure o horário desejado (padrão `21:00`, com ajuste fino de horas/minutos, presets e botão de teste imediato) para receber o check-in interativo em 2 passos na barra de notificações — ou toque na notificação para abrir direto na aba **Diário**.
- **Tipos de Registro**: `Check-in de Humor`, `Micro-log`, `Reflexão` e **`Cápsula do Tempo`** (mensagem selada com cadeado que só é desbloqueada na data futura escolhida).
- **Tags de Contexto & Snapshot Automático**: associe tags (`Sono ruim`, `Treino`, `Foco`, `Estudo`, `Social`, `Sobrecarga`, `Café`) e salve automaticamente o % de rotinas, medalha e ciclos Pomodoro daquele momento.
- **Privacidade (Modo Furtivo & PIN)**: oculte textos do diário com 1 toque e proteja a revelação com **PIN de 4 dígitos** local.
- **Memória "Há 30 dias"**: relembre automaticamente o que você registrou exatamente 30 dias atrás.

### 5. 🤖 Assistente Opcional de IA (Google Gemini SDK — 100% Opt-in)
- Sem chave configurada ou sem internet, todo o aplicativo funciona **100% offline** sem erros.
- Em **Ajustes → Inteligência Artificial (Gemini)**, você pode informar opcionalmente sua própria chave da API Gemini (salva apenas localmente no DataStore, nunca exportada em backups) e alternar entre **`gemini-2.5-flash`** e **`gemini-2.5-pro`**.
- **Patch Notes da Semana (7D)**: gera um resumo retrô no topo do Diário nos blocos **`▲ Buffs`**, **`▼ Nerfs / Drenos`** e **`★ Próxima Quest`**.
- **Reações Sob Demanda nas Notas**: `[Resumir]`, `[2 Perguntas]` (reflexão socrática) e `[Extrair Ação]`.
- **Transformar em Rotina**: quando a IA sugere um hábito prático estruturado, um botão abre o editor de rotinas já pré-preenchido para você revisar e salvar.

### 6. 🍅 Cronômetro Pomodoro Embutido com Lembretes Visuais em Pixel Art
- Recolhido por padrão na aba **Hoje** para manter a tela limpa.
- Modos **Foco Profundo (25m)**, **Pausa Curta (5m)**, **Pausa Longa (15m)** e durações customizáveis.
- Vincule qualquer rotina ativa ao cronômetro e receba um **lembrete visual animado em Pixel Art** ao final de cada ciclo.

### 7. 📅 Calendário Mensal Integrado (Rotinas + Diário + Agenda Android)
- Grade mensal de 42 células com contagem de check-ins, **indicador discreto de humor/energia** no canto de cada dia e card resumo do dia selecionado com atalho direto **`VER NO DIÁRIO`**.
- Integração nativa via `CalendarContract` para visualizar eventos da agenda do aparelho e sincronizar suas rotinas.

### 8. 📊 Relatórios Semanal, Mensal & Correlação 100% Nativos em Canvas
- **Correlação 7 Dias (`Rotina % × Humor × Energia`)**: gráfico nativo em `Canvas` unindo barras de conclusão das rotinas, linha pixelada de humor e linha tracejada de energia.
- **Impacto das Tags de Contexto (30 Dias)**: ranking offline das 3 tags mais frequentes com médias de humor/energia e barras de progresso 8-bit.
- **Relatório Semanal & Mensal**: gráfico de barras 8-bit, melhor período do dia, **Heatmap Mensal de Constância em Pixel Art** e distribuição nas **12 categorias customizadas** (`Hidratação`, `Corpo & Mente`, `Treino & Movimento`, `Meditação & Calma`, `Vitaminas & Saúde`, `Nutrição & Café`, `Foco & Estudos`, `Música & Arte`, `Casa & Ordem`, `Pets & Passeio`, `Autocuidado & Hobby`, `Sono & Descanso`).

### 9. 🎨 10 Temas Pixel Art & 11 Cenários Ilustrados do Dia
- **6 Temas Escuros** (*Catppuccin Mocha*, *Catppuccin Macchiato*, *Noite Aconchegante*, *Meia-Noite Cyberpunk*, *Floresta de Pinheiros*, *Café Expresso Lo-Fi*) e **4 Temas Claros Retrô** (*Catppuccin Latte*, *Pastel Retrô*, *Natureza Lo-Fi*, *Pôr do Sol Quente*), com cards de seleção minimizáveis em Ajustes.
- **11 Artes Dinâmicas do Momento do Dia** com partículas ambientes 8-bit (gotas de chuva, poeira solar ou estrelas cintilantes).

### 10. 💾 Offline-First, Backup JSON v3 & Google Drive
- Banco local **Room v4** com migrações explícitas (`v2 → v3 → v4`) preservando todos os dados do usuário.
- Exportação e importação de arquivo **`.json` (versão 3)** incluindo rotinas, histórico de check-ins, notas do diário e análises de IA — com retrocompatibilidade total para backups **v1** e **v2** — e compartilhamento direto para o **Google Drive** via `FileProvider`.

---

## 🛠️ Arquitetura & Tecnologias

| Camada | Tecnologia |
| :--- | :--- |
| **Linguagem** | Kotlin `2.2.10` |
| **Build System** | AGP `9.1.1` • Gradle `9.3.1` • KSP `2.3.5` • API `36.1` (`minSdk 24`) |
| **Interface (UI)** | Jetpack Compose + Material Design 3 + Componentes e Gráficos Nativos em `Canvas` |
| **Tipografia** | Google Fonts locais em `res/font/` (`Silkscreen`, `Plus Jakarta Sans`) |
| **Banco de Dados Local** | AndroidX Room `2.7.0` (`routines`, `check_in_logs`, `journal_entries` — v4) |
| **Preferências** | AndroidX DataStore Preferences `1.1.7` (`UserPreferencesRepository`) |
| **IA Opcional** | Google Generative AI SDK (`com.google.ai.client.generativeai:generativeai:0.9.0`) |
| **Alarmes & Segundo Plano** | `AlarmManager`, `BroadcastReceiver` (`BOOT_COMPLETED` + ações 2-passos), `ForegroundService` |
| **Calendário Nativo** | Android `CalendarContract` (`Events` & `Instances`) |
| **Testes** | JUnit 4 + Robolectric `4.16.1` (12 testes unitários/integrados em JVM) |

### Estrutura de Diretórios

```text
.
├── PixelRotina_Build_Colab.ipynb       # Notebook pronto para compilar APKs no Google Colab
├── scripts/
│   └── build_colab.sh                  # Script automatizado (JDK 21 + Gradle 9.3.1 + Android SDK 36.1)
├── .github/workflows/
│   └── android-release.yml             # CI/CD GitHub Actions para testes e releases de APK
└── app/src/main/java/com/example/
    ├── MainActivity.kt                 # Entrada principal, navegação em 5 abas e deep-link de notificações
    ├── alarm/
    │   ├── AlarmRingingService.kt      # ForegroundService para som e vibração contínua do alarme
    │   ├── AlarmScheduler.kt           # AlarmManager para rotinas e check-in diário configurável
    │   ├── BootCompletedReceiver.kt    # Reagendamento de rotinas e check-in diário após boot
    │   ├── FullScreenAlarmActivity.kt  # Alarme em tela cheia com arte do momento e Slide to Complete
    │   ├── NotificationHelper.kt       # Canais, ícones de atividade e fluxo interativo Humor -> Energia
    │   └── ReminderBroadcastReceiver.kt# Processamento de OK, OK + Humor, 2 passos, Soneca e Check-in Diário
    ├── data/
    │   ├── AppDatabase.kt              # Room Database v4 com MIGRATION_2_3 e MIGRATION_3_4
    │   ├── BackupManager.kt            # Backup JSON v3 (retrocompatível v1/v2) + FileProvider Drive
    │   ├── CalendarSyncHelper.kt       # Leitura e sincronização com o Calendário do Android
    │   ├── JournalAiAssistant.kt       # Integração opcional sob demanda com Google Gemini SDK
    │   ├── JournalCorrelationCalculator.kt # Cálculos 100% offline de médias, 7d e top tags 30d
    │   ├── RoutineDao.kt               # Queries reativas (Flow) de rotinas, check-ins e diário
    │   ├── RoutineModels.kt            # Entidades, 12 categorias, enums de diário/IA e DateUtils
    │   ├── RoutineRepository.kt        # Repositório unificado de dados, alarmes e backup
    │   └── UserPreferencesRepository.kt# Temas, arte, PIN, Modo Furtivo, Gemini e horário do check-in
    └── ui/
        ├── RoutineViewModel.kt         # Estado reativo, Streaks/Medalhas, Pomodoro, Diário e IA
        ├── components/
        │   ├── PixelArtComponents.kt   # Botões táteis, cards, 12+ sprites 8-bit, ícones de humor/energia/medalhas
        │   ├── PomodoroPixelWidget.kt  # Cronômetro Pomodoro embutido e lembretes visuais 8-bit
        │   └── RoutineEditorDialog.kt  # Modal completo de criação e edição de rotinas
        ├── screens/
        │   ├── JournalScreen.kt        # Aba Diário (check-in, micro-log, cápsula, stealth/PIN, Patch Notes IA)
        │   ├── OnboardingScreen.kt     # Boas-vindas interativas em 3 passos para novos usuários
        │   ├── SettingsThemeBackupScreen.kt # Temas, artes, check-in diário, PIN, Gemini e backup
        │   ├── TodayScreen.kt          # Aba Hoje (hero dinâmico, média de humor, Pomodoro, rotinas e modal pós-rotina)
        │   ├── WeeklyCalendarScreen.kt # Calendário mensal com indicador de humor e resumo do dia
        │   └── WeeklyReportScreen.kt   # Relatórios Semanal, Mensal, Heatmap e Correlação em Canvas
        └── theme/
            ├── Color.kt                # 10 paletas Pixel Art (incluindo família Catppuccin)
            ├── Theme.kt                # Interpolação animada de cores de tema
            └── Type.kt                 # Tipografia Silkscreen + Plus Jakarta Sans
```

---

## ☁️ Como Compilar o APK pelo Google Colab (Sem Instalar Nada no PC)

O repositório já vem pronto para compilar tanto o **`PixelRotina-debug.apk`** quanto o **`PixelRotina-release.apk`** gratuitamente na nuvem usando o **Google Colab**:

### Opção 1 — Usando o Notebook `PixelRotina_Build_Colab.ipynb` (Recomendado)
1. Abra o [Google Colab](https://colab.research.google.com/).
2. Vá em **Arquivo → Abrir notebook → GitHub** (informe seu repositório e selecione `PixelRotina_Build_Colab.ipynb`) **ou** faça upload direto do arquivo `PixelRotina_Build_Colab.ipynb`.
3. Na **Etapa 1**, escolha a origem do código:
   - **`github`**: informe a URL do seu repositório (ex.: `https://github.com/SEU_USUARIO/PixelRotina.git`) e a branch (`main`).
   - **`zip_upload`**: envie o arquivo `.zip` do projeto exportado do AI Studio.
   - **`google_drive`**: carregue a pasta do projeto direto do seu Google Drive.
4. Execute a **Etapa 2**: o script `scripts/build_colab.sh` instala automaticamente **OpenJDK 21**, **Gradle 9.3.1** e **Android SDK (API 36 / 36.1)**, prepara `.env` e as keystores, executa os testes Robolectric e compila os APKs em `dist/`.
5. Execute a **Etapa 3** para baixar `PixelRotina-release.apk` ou `PixelRotina-debug.apk` direto para o seu dispositivo (ou a **Etapa 4** para salvar na pasta `PixelRotina_APKs` do Google Drive).

### Opção 2 — Comando Único em Qualquer Célula do Google Colab
Caso prefira rodar tudo em uma única célula de um notebook vazio do Colab:

```bash
!git clone --depth 1 https://github.com/SEU_USUARIO/PixelRotina.git /content/PixelRotina
%cd /content/PixelRotina
!chmod +x scripts/build_colab.sh
!bash scripts/build_colab.sh both true
```

Os APKs prontos ficarão em `/content/PixelRotina/dist/PixelRotina-debug.apk` e `/content/PixelRotina/dist/PixelRotina-release.apk`.

---

## 💻 Como Compilar Localmente

1. **Pré-requisitos**:
   - JDK 21 (Temurin / OpenJDK 21)
   - Gradle `9.3.1`
   - Android SDK (`platforms;android-36`, `platforms;android-36.1` e `build-tools;36.0.0`)
2. **Preparar ambiente**:
   ```bash
   cp .env.example .env
   ```
3. **Executar testes unitários (Robolectric)**:
   ```bash
   gradle :app:testDebugUnitTest
   ```
4. **Gerar o APK de Debug**:
   ```bash
   gradle assembleDebug
   ```
   O APK gerado estará em `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📦 GitHub Actions (Build Automático e Releases)

O workflow `.github/workflows/android-release.yml` executa automaticamente em cada *push* ou *Pull Request*:
1. Testes unitários com Robolectric (`gradle :app:testDebugUnitTest`).
2. Compilação dos pacotes **`PixelRotina-debug.apk`** e **`PixelRotina-release.apk`** publicados como *Artifacts*.
3. Publicação automática de **GitHub Release** com os APKs anexados ao enviar tags `v*.*.*` (ex.: `v1.0.0`) ou via execução manual (`workflow_dispatch`).

### Assinatura Customizada Opcional (GitHub Secrets)
Se desejar assinar o APK de Release com sua própria keystore no GitHub Actions, adicione os *Repository Secrets* abaixo (caso não sejam informados, uma chave instalável é gerada automaticamente):
- `KEYSTORE_BASE64`: arquivo `.jks` em Base64 (`base64 -w 0 my-upload-key.jks`)
- `STORE_PASSWORD`: senha da keystore
- `KEY_PASSWORD`: senha do alias `upload`
