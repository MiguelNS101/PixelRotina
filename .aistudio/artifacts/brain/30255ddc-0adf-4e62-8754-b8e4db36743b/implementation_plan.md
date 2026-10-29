# PixelRotina — Organizador de Rotina e Bem-Estar em Pixel Art

Aplicativo Android *offline-first* para organizar rotinas diárias e semanais com leveza e foco no bem-estar, combinando estética **Pixel Art aconchegante multi-tema**, avisos rápidos de frequência customizável com botão "OK" direto na notificação, alarmes sonoros/vibratórios persistentes, sincronização com o calendário do Android e backup fácil para o Google Drive.

### User Review & Critical Decisions

> [!IMPORTANT]
> Abaixo estão as decisões de produto confirmadas a partir das suas respostas e como elas guiarão a construção do aplicativo:

- **Decisão Confirmada 1 — Integração Direta com o Calendário do Android**: O app lerá os compromissos do calendário do aparelho (`CalendarContract`) para exibi-los junto à sua rotina diária/semanal e permitirá exportar ou criar novos blocos de rotina diretamente na agenda nativa do dispositivo.
- **Decisão Confirmada 2 — Offline-First com Save Local e Envio Fácil ao Google Drive**: Todos os dados ficam salvos localmente no aparelho em banco SQLite (Room). Você poderá exportar/importar um arquivo de save completo (`.json`) para qualquer pasta do celular ou enviá-lo com um toque direto para o **Google Drive** via compartilhamento nativo seguro.
- **Decisão Confirmada 3 — Lembretes Inteligentes Gentis (Soneca e Reagendamento Suave)**: As notificações terão botões de ação direta (**"OK / Feito"** e **"Soneca +15 min"**) sem precisar abrir o app. Caso uma rotina atrase no dia, o app sugere um reagendamento suave para mais tarde, sem pressão e **sem mecânicas de gamificação**.
- **Decisão Confirmada 4 — Sistema Multi-Tema Pixel Art Vibrante e Relaxante**: O app contará com **5 temas completos em Pixel Art** selecionáveis a qualquer momento:
  1. **Pastel Retrô** (Lavanda, Menta, Pêssego e Creme)
  2. **Natureza Lo-Fi** (Verde Folha, Azul Céu, Amarelo Sol e Terracota)
  3. **Noite Aconchegante** (Tons escuros suaves com detalhes em Neon Calmo)
  4. **Pôr do Sol 16-Bit** (Coral suave, Dourado quente, Ameixa e Areia)
  5. **Oceano Pixel** (Turquesa relaxante, Azul Brisa, Espuma do Mar e Coral claro)

---

### 1. Overview & Core Concept

- **O Que o App Faz**: Ajuda a construir e manter uma rotina diária e semanal saudável, lembrando de hábitos recorrentes (como beber água ou alongar com apenas um "OK" na notificação), compromissos em horários específicos e tarefas críticas que exigem um alarme real vibrando e tocando até ser desligado.
- **Público e Contexto de Uso**: Pessoas que buscam organizar o dia a dia e cuidar do bem-estar com uma interface divertida, acolhedora e sem o peso corporativo de ferramentas tradicionais ou a pressão de sistemas gamificados.
- **Valor Principal**: Confiabilidade fora do app (funcionando com o app fechado e após reiniciar o celular), flexibilidade entre lembretes rápidos e alarmes reais, visão integrada com o calendário e acompanhamento gentil por meio de um relatório semanal visual.

---

### 2. User Experience & Visual Design

- **Fluxos Principais do Usuário**:
  1. **Painel Hoje & Linha do Tempo Diária**: Exibe a saudação do dia com arte pixelada, contadores rápidos de hábitos de intervalo (ex.: copos de água com botão `+1` rápido), compromissos sincronizados do Calendário do Android, lembretes que atrasaram (com botão de *Reagendar Suavemente*) e a lista cronológica das rotinas do dia.
  2. **Criador de Rotina e Lembretes Customizáveis**: Modal/tela intuitiva para configurar:
     - **Tipo de Alerta**:
       - *Aviso Rápido ("OK" na Notificação)*: Ideal para hidratação, postura ou pausas. Permite escolher o intervalo em minutos/horas (ex.: a cada 45 min, 1h, 2h) e a janela de horário ativo (ex.: das 08:00 às 21:00).
       - *Horário Específico (Notificação Padrão)*: Dispara no horário exato nos dias da semana escolhidos, com ações de "Concluir" ou "Adiar".
       - *Modo Alarme Persistente*: Toca som de alarme contínuo, vibra sem parar e abre a tela de despertar sobre a tela de bloqueio até você tocar em "Desligar Alarme / Feito" ou "Soneca".
     - **Sincronização Opcional**: Chave para adicionar automaticamente o horário ao Calendário do Android.
  3. **Visão Semanal & Calendário Integrado**: Grade semanal mostrando a distribuição das rotinas nos 7 dias da semana lado a lado com os eventos reais lidos do calendário do aparelho, permitindo criar ou ajustar horários rapidamente.
  4. **Relatório Semanal de Bem-Estar**: Painel analítico gentil com gráfico de barras em blocos pixelados dos últimos 7 dias, taxa de constância por categoria (Hidratação, Corpo & Mente, Descanso, Rotina Diária), períodos do dia em que você esteve mais ativo e histórico detalhado de check-ins.
  5. **Temas Pixel Art, Permissões & Backup (Save Local / Google Drive)**: Central para trocar entre os 5 temas retrô em tempo real, testar notificações/alarmes, verificar permissões do sistema e exportar/importar o arquivo de save local ou enviá-lo direto ao Google Drive.

- **Identidade Visual & Sistema Multi-Tema**:
  - **Direção Estética**: *Cozy Pixel Art & Lo-Fi Wellness* — cartões com bordas duplas estilo 8-bit/16-bit, sombras deslocadas nítidas (estilo retrô), ícones pixelados desenhados em Canvas/SVG e ilustrações em pixel art para o cabeçalho do dia.
  - **Paletas Multi-Tema (`MaterialTheme.colorScheme` + Tokens Customizados)**:
    - *Pastel Retrô*: Fundo Creme (`#FFFDF7`), Superfície Pêssego Suave (`#FFF3E6`), Primária Lavanda Vibrante (`#8B6CEF`), Secundária Menta (`#4ECDC4`), Acento Coral (`#FF7E67`).
    - *Natureza Lo-Fi*: Fundo Areia Clara (`#F6F8F0`), Superfície Folha Suave (`#E8EFE0`), Primária Verde Musgo Vibrante (`#3D8B5E`), Secundária Azul Céu (`#4A90E2`), Acento Amarelo Sol (`#F5A623`).
    - *Noite Aconchegante*: Fundo Azul Noturno (`#141829`), Superfície Indigo Escuro (`#1E243B`), Primária Ciano Calmo (`#5CE1E6`), Secundária Lavanda Neon (`#B388FF`), Acento Pêssego Noturno (`#FF9E80`).
    - *Pôr do Sol 16-Bit*: Fundo Areia Dourada (`#FFF8F0`), Superfície Rosa Pêssego (`#FDE8E0`), Primária Coral Intenso (`#E85D56`), Secundária Roxo Ameixa (`#6C4A7E`), Acento Âmbar (`#F4A261`).
    - *Oceano Pixel*: Fundo Espuma Clara (`#F0FAFC`), Superfície Azul Gelo (`#E0F4F7`), Primária Turquesa (`#008B9E`), Secundária Verde Água (`#2EC4B6`), Acento Coral Marinho (`#FF6B6B`).
  - **Tipografia & Hierarquia**:
    - Títulos, cabeçalhos e selos numéricos com fonte display estilo Pixel Art instalada localmente (`Silkscreen` / `Press Start 2P` via `font-util`).
    - Textos de leitura, descrições e horários longos com fonte arredondada e super legível (`Plus Jakarta Sans` / `Nunito` via `font-util`).

- **Feedback Interativo & Movimento**:
  - Transições suaves com física de mola (`spring()`) ao marcar uma tarefa como concluída, animações em blocos pixelados nas barras de progresso do dia e feedback tátil sutil ao confirmar check-ins.

---

### 3. Key Product Decisions & Trade-Offs

- **Decisão 1 — Motor Híbrido de Notificações e Alarmes Reais em Segundo Plano**
  - *Abordagem Escolhida*: Uso de `AlarmManager.setAlarmClock` / `setExactAndAllowWhileIdle` combinado com `BroadcastReceiver`, `BootCompletedReceiver` (`RECEIVE_BOOT_COMPLETED`) e um `ForegroundService` dedicado com `MediaPlayer` + `Vibrator` + `FullScreenIntent` para o modo Alarme.
  - *Por Quê*: Garante que tanto os avisos de intervalo ("Beber água") quanto os alarmes sonoros toquem pontualmente mesmo com o app fechado, em modo de economia de bateria (Doze) ou após reiniciar o dispositivo.
  - *Alternativas Consideradas*: `WorkManager` sozinho foi descartado para os alarmes porque o sistema operacional pode atrasar sua execução em vários minutos, o que comprometeria lembretes com horário exato.

- **Decisão 2 — Confirmação com 1 Toque Fora do App**
  - *Abordagem Escolhida*: `NotificationCompat.Action` conectado a um `ReminderActionReceiver` que grava a conclusão no banco Room em segundo plano e já agenda automaticamente o próximo ciclo de intervalo (ou soneca de +15 min) sem abrir a interface do app.
  - *Por Quê*: Atende diretamente à necessidade de dar apenas um "OK" rápido em lembretes recorrentes do dia a dia com zero fricção.

- **Decisão 3 — Persistência Offline-First com Backup Portátil JSON + Google Drive**
  - *Abordagem Escolhida*: Banco de dados Room local como fonte única da verdade, aliado a um serializador JSON estruturado com suporte ao seletor de arquivos do sistema (Storage Access Framework) e `FileProvider` para envio direto ao Google Drive.
  - *Por Quê*: Funciona 100% sem internet no dia a dia, preserva sua privacidade e permite salvar cópias seguras na nuvem sempre que desejar sem exigir login obrigatório para usar o app.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Arquitetura & Diagrama de Componentes**:

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         CAMADA DE INTERFACE (Compose)                     │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐  │
│  │ Tela Hoje &  │ │ Visão Semanal│ │  Relatório   │ │  Temas, Ajustes  │  │
│  │ Check-in Ráp.│ │ & Calendário │ │   Semanal    │ │ & Backup GDrive  │  │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └────────┬─────────┘  │
│         └────────────────┴────────┬───────┴──────────────────┘            │
│                                   ▼                                       │
│                   ┌───────────────────────────────┐                       │
│                   │  RoutineViewModel (StateFlow) │                       │
│                   └───────┬───────────────┬───────┘                       │
└───────────────────────────┼───────────────┼───────────────────────────────┘
                            │               │
        ┌───────────────────▼───┐       ┌───▼───────────────────────────┐
        │ CAMADA DE DADOS LOCAL │       │ SERVIÇOS DE SISTEMA & ANDROID │
        │ ┌───────────────────┐ │       │ ┌───────────────────────────┐ │
        │ │ RoutineRepository │ │       │ │ AlarmScheduler            │ │
        │ └─────────┬─────────┘ │       │ │ (AlarmManager Exato)      │ │
        │           ▼           │       │ ├───────────────────────────┤ │
        │ ┌───────────────────┐ │       │ │ AlarmRingingService +     │ │
        │ │   Room Database   │ │       │ │ FullScreenAlarmActivity   │ │
        │ │ • RoutineEntity   │ │       │ │ (Som + Vibração Contínua) │ │
        │ │ • CheckInLogEntity│ │       │ ├───────────────────────────┤ │
        │ └─────────┬─────────┘ │       │ │ ReminderActionReceiver &  │ │
        │           ▼           │       │ │ BootCompletedReceiver     │ │
        │ ┌───────────────────┐ │       │ ├───────────────────────────┤ │
        │ │ BackupManager     │ │       │ │ CalendarSyncHelper        │ │
        │ │ (JSON + GDrive)   │ │       │ │ (CalendarContract Nativo) │ │
        │ └───────────────────┘ │       │ └───────────────────────────┘ │
        └───────────────────────┘       └───────────────────────────────┘
```

- **Modelo de Dados & Estado**:
  - **`RoutineEntity` (Rotina / Hábito)**:
    - Identificador, título, descrição/nota de bem-estar, categoria (`HIDRATACAO`, `CORPO_MENTE`, `DESCANSO`, `ALIMENTACAO`, `FOCO`, `LAZER`), ícone pixel art.
    - **Modo de Disparo (`AlertMode`)**: `QUICK_OK_NOTIFICATION` (notificação com botão OK), `SCHEDULED_NOTIFICATION` (lembrete em horário fixo) ou `PERSISTENT_ALARM` (alarme sonoro e vibratório contínuo até desligar).
    - **Frequência (`FrequencyType`)**: `SPECIFIC_TIME` (horário exato `HH:mm` + dias da semana `1..7`) ou `INTERVAL` (intervalo em minutos, ex.: a cada `45` min, dentro da janela `startHour..endHour` nos dias selecionados).
    - Configurações extras: `syncToCalendar` (booleano + `calendarEventId` opcional), `isEnabled` (ativa/pausada), `nextTriggerMillis` (próximo disparo calculado).
  - **`CheckInLogEntity` (Histórico de Acompanhamento)**:
    - Identificador, `routineId`, data de referência (`YYYY-MM-DD`), timestamp real da ação, status (`COMPLETED`, `SNOOZED`, `SKIPPED`), origem (`IN_APP`, `NOTIFICATION_OK`, `ALARM_DISMISSED`) e nota opcional.
  - **`AppPreferences` (Preferências e Tema)**:
    - Tema ativo (`PASTEL_RETRO`, `NATUREZA_LOFI`, `NOITE_ACONCHEGANTE`, `POR_DO_SOL_16BIT`, `OCEANO_PIXEL`), janela padrão de silêncio noturno e preferências de som/vibração.

- **Mapeamento de Componentes Interativos e Transições de Estado**:
  - **Check-in Rápido na Tela Hoje ou na Notificação**: Ao tocar em "OK" (no cartão ou na notificação externa), insere um `CheckInLogEntity(status = COMPLETED)`, atualiza o contador diário daquela rotina e, se for de intervalo, calcula e agenda imediatamente o próximo disparo dentro da janela ativa.
  - **Soneca Rápida & Reagendamento Suave**: Ao tocar em "Adiar +15m" na notificação ou no banner de rotina atrasada no app, reagenda o `AlarmManager` para `agora + 15 min` (ou para o novo horário escolhido pelo usuário) e registra o ajuste sem penalizar o histórico semanal.
  - **Despertador Persistente (`FullScreenAlarmActivity` + `AlarmRingingService`)**: Quando uma rotina em modo `PERSISTENT_ALARM` dispara, inicia o serviço em primeiro plano com áudio de alarme em loop e padrão de vibração contínua, abrindo a interface em pixel art com os botões **"Desligar Alarme e Concluir"** e **"Soneca 10 min"**.
  - **Sincronização com Calendário do Android**: Consulta `CalendarContract.Instances` para listar os eventos reais da semana do usuário e usa `ContentResolver.insert(CalendarContract.Events.CONTENT_URI, ...)` (com fallback seguro para `Intent.ACTION_INSERT`) ao salvar rotinas marcadas com "Sincronizar com Calendário".
  - **Importação, Exportação e Envio ao Google Drive**: Empacota todas as rotinas, registros de check-in e tema selecionado em um arquivo `.json` versionado; oferece botão para salvar no armazenamento local, botão para restaurar save (mesclando ou substituindo dados e reagendando os alarmes) e botão **"Enviar para o Google Drive"** que compartilha o arquivo pronto via `FileProvider`.
