# 🌿 PixelRotina — Organizador de Rotina Diária, Semanal & Bem-Estar em Pixel Art

**PixelRotina** é um aplicativo Android nativo e *offline-first* desenvolvido em **Kotlin** e **Jetpack Compose** para organizar sua rotina diária e semanal com foco em bem-estar, constância leve e estética retrô em **Pixel Art**.

Diferente de aplicativos corporativos ou excessivamente gamificados, o PixelRotina foi projetado para ajudar você a lembrar e manter hábitos reais no seu próprio ritmo — funcionando com o app fechado e persistindo mesmo após o reinício do dispositivo.

---

## ✨ Principais Funcionalidades

### 1. 💧 Avisos de Intervalo (OK Direto na Notificação)
- Ideal para hábitos recorrentes ao longo do dia (ex: beber água, descansar a vista, alongar os ombros).
- Configure o **intervalo em minutos** (ex: a cada 45 min ou 60 min), a **janela de horário ativa** (ex: das 08h às 21h) e a meta diária de confirmações.
- Confirme com **1 toque no botão `"✓ OK! Feito"` diretamente na barra de notificações do Android**, sem precisar abrir o aplicativo.

### 2. 🔔 Horários Específicos & Alarme em Tela Cheia com Slide para Concluir
- Agende lembretes para horários fixos e escolha os dias da semana ativos (Segunda a Domingo).
- Dois modos de alerta fora do app:
  - **📩 Notificação Padrão**: Notificação pontual com ações rápidas de *OK! Feito* e *Adiar*.
  - **🔔 Alarme Persistente em Tela Cheia**: Toca e vibra continuamente (via `ForegroundService` e `AlarmManager`) até ser desligado, abrindo em **Tela Cheia** (mesmo sobre a tela de bloqueio) com a **Arte em Pixel Art do Momento do Dia** e uma barra **Deslize para Concluir (`Slide to Complete`)**, além de botão de Soneca.

### 3. 🍅 Cronômetro Pomodoro Embutido com Lembretes Visuais em Pixel Art
- Embutido na tela **Hoje** (recolhido por padrão para manter a interface limpa).
- Modos de **Foco Profundo (25m)**, **Pausa Curta (5m)** e **Pausa Longa (15m)**, além de durações customizáveis.
- Permite **vincular qualquer tarefa do dia** ao bloco de foco.
- Ao final de cada intervalo, exibe um **Lembrete Visual em Pixel Art animado** (lembrando de hidratar, descansar os olhos por 20 segundos ou iniciar o próximo bloco) com botão para concluir a tarefa vinculada e iniciar a pausa com 1 toque.

### 4. 🌤️ Reagendamento Suave (Sem Culpa)
- Passou do horário de uma rotina? O painel de **Reagendamento Suave** permite adiar em **+15 min** ou **+1 hora** com 1 toque ou marcar como feito sem pressão.

### 5. 📅 Calendário Mensal & Integração com o Calendário do Android
- Navegue pelos meses do ano na aba **Calendário**, visualize o número de tarefas concluídas em cada dia e filtre os compromissos da data selecionada.
- Integração nativa com o provedor `CalendarContract` do Android para ler os eventos da sua agenda e exportar suas rotinas para o Calendário do sistema.

### 6. 📊 Relatórios Semanal & Mensal de Bem-Estar
- **Relatório Semanal (Últimos 7 Dias)**: Gráfico de barras em blocos 8-bit comparando rotinas concluídas e reagendamentos suaves, melhor período do dia e equilíbrio por categoria.
- **Relatório Mensal (Mês Atual)**: Total concluído no mês, percentual de dias ativos, evolução por semana do mês (`SEM 1` a `SEM 5`), **Mapa de Constância Mensal (Heatmap em Pixel Art)** dia a dia e acumulado mensal por área de bem-estar.

### 7. 🎨 10 Temas Pixel Art & 7 Cenários Ilustrados do Dia
- **6 Temas Escuros Aconchegantes**: *Catppuccin Mocha*, *Catppuccin Macchiato*, *Noite Aconchegante*, *Meia-Noite Cyberpunk*, *Floresta de Pinheiros* e *Café Expresso Lo-Fi*.
- **4 Temas Claros Retrô**: *Catppuccin Latte*, *Pastel Retrô*, *Natureza Lo-Fi* e *Pôr do Sol Quente*.
- **Transições Suaves & Botões Táteis**: Animações físicas nos botões (que afundam na direção da sombra ao toque com retorno tátil), transições fluidas de tela e interpolação suave de cores ao trocar de tema.
- **7 Artes Dinâmicas no Card do Dia**: *Manhã Ensolarada*, *Manhã de Chuva*, *Tarde Lo-Fi*, *Jardim à Tarde*, *Pôr do Sol*, *Noite Estrelada* e *Noite Catppuccin*, acompanhadas de partículas ambientes 8-bit (gotas de chuva, poeira solar ou estrelas cintilantes).

### 8. 💾 Offline-First, Save Local (`.json`) & Backup no Google Drive
- Todos os dados ficam salvos localmente no dispositivo via **Room Database** e **Preferences DataStore** (o app inicia 100% limpo para novos usuários, sem dados falsos).
- Exporte e importe seu arquivo de save completo (`.json`) a qualquer momento ou envie diretamente para o **Google Drive** via `FileProvider`.

---

## 🛠️ Arquitetura & Tecnologias

| Camada | Tecnologia |
| :--- | :--- |
| **Linguagem** | Kotlin 2.x |
| **Interface (UI)** | Jetpack Compose + Material Design 3 + Custom 8-Bit Canvas Components |
| **Tipografia Pixel** | Fontes locais Google Fonts (`Press Start 2P`, `Silkscreen`, `VT323`) |
| **Banco de Dados Local** | AndroidX Room (`RoutineEntity`, `CheckInLogEntity`) |
| **Preferências** | AndroidX DataStore Preferences (`UserPreferencesRepository`) |
| **Alarmes & Segundo Plano** | `AlarmManager` (`setAlarmClock` / `setExactAndAllowWhileIdle`), `BroadcastReceiver` (`BOOT_COMPLETED`), `ForegroundService` (`AlarmRingingService`) |
| **Calendário Nativo** | Android `CalendarContract` (`Events` & `Instances`) |
| **Backup & Nuvem** | Serialização JSON nativa + `FileProvider` para compartilhamento com Google Drive |

### Estrutura de Diretórios

```text
app/src/main/java/com/example/
├── MainActivity.kt                  # Entrada principal, navegação com transições e TopBar
├── alarm/
│   ├── AlarmRingingService.kt       # Serviço em 1º plano para som e vibração contínua do alarme
│   ├── AlarmScheduler.kt            # Agendamento exato no AlarmManager e cálculo de gatilhos
│   ├── BootCompletedReceiver.kt     # Reagendamento automático após reinício do Android
│   ├── FullScreenAlarmActivity.kt   # Tela cheia do alarme com Foto do Momento e Slide para Concluir
│   ├── NotificationHelper.kt        # Canais de notificação e ações rápidas (OK! Feito / Soneca)
│   └── ReminderBroadcastReceiver.kt # Processamento de alarmes, check-ins externos e sonecas
├── data/
│   ├── AppDatabase.kt               # Configuração do banco de dados Room
│   ├── BackupManager.kt             # Exportação/Importação JSON e envio ao Google Drive
│   ├── CalendarSyncHelper.kt        # Leitura e inserção de eventos no Calendário do Android
│   ├── RoutineDao.kt                # Consultas reativas (Flow) de rotinas e histórico
│   ├── RoutineModels.kt             # Entidades, enums de frequência, alertas e utilitários de data
│   ├── RoutineRepository.kt         # Repositório unificado de rotinas, alarmes e backup
│   └── UserPreferencesRepository.kt # Temas, arte do dia, onboarding e configurações no DataStore
└── ui/
    ├── RoutineViewModel.kt          # Estado reativo (UI State), Pomodoro e Relatórios Semanal/Mensal
    ├── components/
    │   ├── PixelArtComponents.kt    # Botões físicos táteis, cards, sprites 8-bit e confete pixel
    │   ├── PomodoroPixelWidget.kt   # Cronômetro Pomodoro embutido e lembretes visuais em pixel art
    │   └── RoutineEditorDialog.kt   # Modal completo de criação e edição de rotinas
    ├── screens/
    │   ├── OnboardingScreen.kt      # Boas-vindas interativas em 3 passos para novos usuários
    │   ├── SettingsThemeBackupScreen.kt # Seleção de temas, arte do card, permissões e backup
    │   ├── TodayScreen.kt           # Tela principal com card dinâmico, Pomodoro e tarefas do dia
    │   ├── WeeklyCalendarScreen.kt  # Calendário mensal interativo e sincronização com a agenda
    │   └── WeeklyReportScreen.kt    # Relatórios Semanal e Mensal com gráficos e heatmap pixel
    └── theme/
        ├── Color.kt                 # Definição das 10 paletas Pixel Art (incluindo Catppuccin)
        ├── Theme.kt                 # Interpolação animada de cores de tema
        └── Type.kt                  # Configuração tipográfica retrô e legível
```

---

## 🚀 Como Compilar Localmente

1. **Pré-requisitos**:
   - JDK 21 (Temurin / OpenJDK 21)
   - Gradle 9.3.1
   - Android SDK (API 36)
2. **Clonar e preparar ambiente**:
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
   O APK gerado estará disponível em `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📦 GitHub Actions (Geração Automática de APK e Release)

O projeto já inclui o workflow `.github/workflows/android-release.yml` configurado para:
1. Executar os testes unitários (`gradle :app:testDebugUnitTest`) em cada *push* ou *Pull Request*.
2. Compilar automaticamente os pacotes **`PixelRotina-debug.apk`** e **`PixelRotina-release.apk`** e disponibilizá-los como *Artifacts* na aba **Actions**.
3. Publicar automaticamente uma **GitHub Release** com os APKs anexados sempre que uma tag `v*.*.*` (ex: `v1.0.0`) for enviada ao repositório ou quando acionado manualmente via `workflow_dispatch`.

### Assinatura Customizada Opcional no GitHub
Caso deseje assinar o APK de Release com sua própria chave privada no GitHub, basta cadastrar os seguintes *Repository Secrets* (se não forem informados, o workflow gera uma chave instalável automaticamente):
- `KEYSTORE_BASE64`: Conteúdo do seu arquivo `.jks` codificado em Base64 (`base64 -w 0 my-upload-key.jks`).
- `STORE_PASSWORD`: Senha da keystore.
- `KEY_PASSWORD`: Senha do alias `upload`.
