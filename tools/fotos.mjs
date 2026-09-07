#!/usr/bin/env node
/**
 * Sobe um AVD, instala o APK de depuração e fotografa as telas da B ("Console") sem
 * host nenhum — usando os moques `MoqueDeCasca`, `MoqueDeApps` e `MoqueDeStream`
 * (debug-only, ver PcView/AppView/Game).
 *
 *   node tools/fotos.mjs --avd s22u
 *   node tools/fotos.mjs --avd tabs8u
 *   node tools/fotos.mjs --avd s22u --port 5556 --rotas computadores,apps
 *   node tools/fotos.mjs --avd s22u --janela
 *   node tools/fotos.mjs --selftest      # só o codificador de PNG, sem AVD nem adb
 *
 * Flags:
 *   --avd <nome>     obrigatório. Nome do AVD (`s22u`, `tabs8u`, ou qualquer outro já
 *                    criado em %USERPROFILE%\.android\avd\<nome>.avd).
 *   --port <n>       porta do emulador (padrão 5554). O serial do adb vira
 *                    `emulator-<n>`. Portas fixas por fatia evitam dois times de
 *                    fotos brigando pela mesma instância (ver docs/planos/B-CONSOLE.md).
 *   --rotas <lista>  subconjunto de rotas, separadas por vírgula (padrão: as 8 abaixo).
 *   --janela         sobe o emulador com janela em vez de `-no-window`. Sem isto o
 *                    emulador não disputa GPU com o build e é o modo padrão do CI.
 *
 * Rotas (nesta ordem por padrão):
 *
 *   computadores    PcView com MoqueDeCasca (ARTEMIS on-line, NOTEBOOK fora do ar)
 *   apps            AppView com MoqueDeApps (6 apps com capa gerada)
 *   ajustes         StreamSettings, sem moque — é a árvore de preferências de hoje
 *   adicionar       AddComputerManually, sem moque
 *   cenas           ProfilesActivity, sem moque
 *   sessao              Game com MoqueDeStream (superfície preta, sem host) — a aba
 *                       (console_aba.xml) em repouso no centro da borda superior
 *   sessao-conectando   idem, com MoqueEstado=conectando — trava ANTES de
 *                       connectionStarted(), então vidro_connecting.xml (Plano B / F5)
 *                       fica de pé em vez de sumir na hora
 *   sessao-queda        idem, com MoqueEstado=queda — conecta normalmente e mostra
 *                       vidro_lost.xml por cima (Plano B / F5); não reintroduz a queda
 *                       de rede real, só a pele
 *   sessao-menu         idem, depois de KEYCODE_BACK — enableBackMenu é true por
 *                       padrão; abre a guia (console_guia.xml, Plano B / F2), não mais
 *                       a lista de antes
 *   sessao-guia         idem, depois de um toque na aba (o gatilho de verdade da guia)
 *   sessao-guia-teclas  idem, depois de abrir a guia e navegar por D-pad (3×
 *                       DPAD_RIGHT + ENTER) até o bloco "Teclas especiais", que troca
 *                       a fileira pelas teclas Esc·Tab·Win·Alt+Tab·Ctrl+Alt+Del
 *   sessao-guia-fecha   idem, depois de abrir a guia e fechar com KEYCODE_ESCAPE
 *   sessao-texto        idem, depois de abrir a guia e tocar no bloco "Barra de texto"
 *                       (abre a barra de texto do E01 — openComposeBar)
 *   sessao-retrato      idem, sem girar pra paisagem (G2, defeito 5: "no modo vertical
 *                       a stream n se adapta e fica cortada") — mostra a superfície
 *                       do moque em pé, pra conferir que ela cabe em letterbox e não
 *                       corta nem estica
 *
 * As rotas de sessão (exceto `sessao-retrato`) giram o `s22u` (celular) para paisagem
 * antes de abrir — sem isto `Game` renderiza na orientação corrente do aparelho
 * (autoOrientation), que no celular é retrato. O `tabs8u` já nasce em paisagem (é a
 * forma do tablet) e não é tocado. As demais rotas ficam em retrato no `s22u`; a
 * rotação volta a 0 no fim.
 *
 * Cada rota: `am force-stop` (estado limpo, sem singleTask/onNewIntent para gerenciar),
 * `am start -n <pacote>/<Activity> [extras]`, espera a tela assentar (uiautomator dump
 * estável entre duas leituras, ou no máximo ~1,5 s), a ação pós-abertura quando houver
 * (tecla ou toque), espera de novo, `screencap`. Logcat é limpo antes e conferido depois
 * — um crash vira falha da rota, não uma foto de tela preta relatada como sucesso.
 *
 * Saída: `app/build/fotos/<avd>/<rota>.png` por rota, mais `app/build/fotos/<avd>/folha.png`
 * — uma folha de contato com miniatura e rótulo de cada rota, montada por um codificador
 * de PNG mínimo escrito aqui (só `node:zlib` — sem dependência nova no repositório).
 *
 * O APK instalado é sempre o x86_64 de depuração (`nonRoot_game`/`debug`) — é o ABI do
 * emulador, não o do aparelho. Constrói com `gradlew` só se o APK ainda não existir.
 */
import { spawn, spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { deflateSync, inflateSync } from 'node:zlib';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const SDK = join(process.env.LOCALAPPDATA || process.env.HOME, 'Android', 'Sdk');
const isWin = process.platform === 'win32';
const ADB = join(SDK, 'platform-tools', isWin ? 'adb.exe' : 'adb');
const EMULATOR = join(SDK, 'emulator', isWin ? 'emulator.exe' : 'emulator');
const APK = join(ROOT, 'app', 'build', 'outputs', 'apk', 'nonRoot_game', 'debug',
  'app-nonRoot_game-x86_64-debug.apk');
const PKG = 'com.limelight.noirdebug';

// ---------------------------------------------------------------------------------
// CLI
// ---------------------------------------------------------------------------------

function lerArgs(argv) {
  const args = { port: 5554, janela: false, rotas: null };
  for (let i = 0; i < argv.length; i++) {
    switch (argv[i]) {
      case '--avd': args.avd = argv[++i]; break;
      case '--port': args.port = parseInt(argv[++i], 10); break;
      case '--rotas': args.rotas = argv[++i].split(',').map((s) => s.trim()).filter(Boolean); break;
      case '--janela': args.janela = true; break;
      default:
        console.error(`argumento desconhecido: ${argv[i]}`);
        process.exit(1);
    }
  }
  if (!args.avd) {
    console.error('uso: node tools/fotos.mjs --avd <s22u|tabs8u> [--port 5554] [--rotas a,b] [--janela]');
    process.exit(1);
  }
  return args;
}

// ---------------------------------------------------------------------------------
// adb / emulador
// ---------------------------------------------------------------------------------

const sh = (cmd, cmdArgs, opts = {}) => spawnSync(cmd, cmdArgs, { encoding: 'utf8', ...opts });

function adb(serial, ...args) {
  return sh(ADB, ['-s', serial, ...args]);
}

function adbOut(serial, ...args) {
  return (adb(serial, ...args).stdout || '').trim();
}

function serialPresente(serial) {
  return sh(ADB, ['-s', serial, 'get-state']).stdout?.trim() === 'device';
}

function booted(serial) {
  return adbOut(serial, 'shell', 'getprop', 'sys.boot_completed') === '1';
}

function esperarMs(ms) {
  // `cmd /c timeout` sai na hora com "Input redirection is not supported" quando o
  // stdin não é um console de verdade — que é exatamente o caso aqui (stdio: ignore).
  // O sintoma era silencioso: nenhum erro visível, só um script que não esperava nada
  // e corria na frente do próprio emulador. Start-Sleep não tem essa checagem.
  sh(isWin ? 'powershell' : 'sh',
    isWin ? ['-NoProfile', '-Command', `Start-Sleep -Milliseconds ${ms}`] : ['-c', `sleep ${ms / 1000}`],
    { stdio: 'ignore' });
}

/** Sobe o AVD se a porta pedida ainda não tiver um emulador de pé; senão, reusa. */
function garantirEmulador(avd, port, janela) {
  const serial = `emulator-${port}`;
  const jaEstavaDePe = serialPresente(serial);

  if (jaEstavaDePe) {
    console.log(`${serial} já está de pé, reusando…`);
  } else {
    console.log(`subindo ${avd} em ${serial}…`);
    const flags = ['-avd', avd, '-port', String(port), '-no-audio', '-no-boot-anim',
      '-gpu', 'swiftshader_indirect', '-no-snapshot'];
    if (!janela) flags.push('-no-window');
    // spawnSync bloquearia este script até o emulador FECHAR — é um processo de vida
    // longa, não um comando que termina. spawn (assíncrono) + unref larga o processo
    // solto e deixa o script seguir pro polling de boot que vem a seguir.
    const filho = spawn(EMULATOR, flags, { detached: true, stdio: 'ignore' });
    filho.unref();

    const prazo = Date.now() + 240000;
    while (Date.now() < prazo) {
      if (serialPresente(serial)) break;
      esperarMs(2000);
    }
    if (!serialPresente(serial)) {
      console.error(`${serial} não apareceu em 4 min`);
      process.exit(1);
    }
  }

  const prazoBoot = Date.now() + 240000;
  while (Date.now() < prazoBoot) {
    if (booted(serial)) break;
    esperarMs(3000);
  }
  if (!booted(serial)) {
    console.error(`${serial} não terminou de dar boot em 4 min`);
    process.exit(1);
  }

  if (!jaEstavaDePe) {
    // sys.boot_completed=1 chega um pouco antes do launcher e do package manager
    // estarem realmente prontos pra instalar e abrir Activity — só importa na
    // primeira subida; um emulador reusado já passou disso há muito tempo.
    esperarMs(5000);
  }

  // Confere que a porta é mesmo o AVD pedido — evita fotografar o emulador errado
  // quando a porta já estava ocupada por outra fatia.
  const nomeReal = adbOut(serial, 'emu', 'avd', 'name').split('\n')[0].trim();
  if (nomeReal && nomeReal.toLowerCase() !== avd.toLowerCase()) {
    console.error(`${serial} está rodando "${nomeReal}", não "${avd}" — porta errada?`);
    process.exit(1);
  }

  console.log(`${serial} pronto (${avd})`);
  return serial;
}

/**
 * Mesma lógica de tools/validate.mjs: o AGP 8.13 quer um JDK 17-21, e o JBR que vem
 * com o Android Studio costuma ser mais novo que isso. Sem JAVA_HOME no ambiente,
 * procura um JDK instalado antes de deixar o Gradle escolher errado.
 */
function resolveJavaHome() {
  if (process.env.JAVA_HOME && existsSync(process.env.JAVA_HOME)) return process.env.JAVA_HOME;

  const candidates = [];
  const programFiles = process.env.ProgramFiles || 'C:\\Program Files';
  for (const vendor of ['Java', 'Eclipse Adoptium', 'Microsoft', 'Amazon Corretto']) {
    const dir = join(programFiles, vendor);
    if (!existsSync(dir)) continue;
    try {
      for (const entry of readdirSync(dir)) {
        const m = entry.match(/-(\d+)/);
        const major = m ? parseInt(m[1], 10) : null;
        if (major && major >= 17 && major <= 21) candidates.push({ path: join(dir, entry), major });
      }
    } catch { /* diretório ilegível, segue */ }
  }
  candidates.sort((a, b) => b.major - a.major);
  return candidates.length ? candidates[0].path : null;
}

function garantirApk() {
  if (existsSync(APK)) return;

  console.log('APK x86_64 de depuração não existe, compilando…');
  const gradlew = join(ROOT, isWin ? 'gradlew.bat' : 'gradlew');
  const javaHome = resolveJavaHome();
  const r = sh(gradlew, ['assembleNonRoot_gameDebug', '--console=plain', '-q'], {
    cwd: ROOT,
    shell: true,
    env: javaHome ? { ...process.env, JAVA_HOME: javaHome } : process.env,
  });
  if (r.status !== 0 || !existsSync(APK)) {
    console.error((r.stdout || '') + (r.stderr || ''));
    console.error(`build não produziu ${APK}`);
    process.exit(1);
  }
}

function instalarApk(serial) {
  console.log('instalando…');
  const r = adb(serial, 'install', '-r', '-t', APK);
  console.log('  ' + (r.stdout || '').trim().split('\n').pop());
  if (r.status !== 0) {
    console.error(r.stderr || '');
    process.exit(1);
  }
}

// ---------------------------------------------------------------------------------
// UI: assentar, tocar, girar
// ---------------------------------------------------------------------------------

function dumpUi(serial) {
  return adbOut(serial, 'shell', 'uiautomator dump /sdcard/_fotos_dump.xml >/dev/null 2>&1 && cat /sdcard/_fotos_dump.xml');
}

/**
 * uiautomator dump estável entre duas leituras, ou no máximo ~3 s.
 *
 * O piso de 900 ms antes da primeira leitura existe por causa da PRIMEIRA vez que
 * PcView roda num AVD recém-instalado: sem GLRenderer em cache, o onCreate sobe uma
 * GLSurfaceView e só chama completeOnCreate() no callback assíncrono dela — a tela
 * fica "parada" (vazia) por um instante que passaria no teste de estabilidade sem
 * este piso, e a foto saía sem os PCs de mentira. Rodas seguintes no mesmo AVD já têm
 * o fingerprint em cache e são rápidas; o piso só custa tempo, não corretude.
 */
function aguardarAssentar(serial) {
  esperarMs(900);
  const prazo = Date.now() + 3000;
  let anterior = dumpUi(serial);
  esperarMs(400);
  while (Date.now() < prazo) {
    const atual = dumpUi(serial);
    if (atual && atual === anterior) return;
    anterior = atual;
    esperarMs(400);
  }
}

/** Centro do primeiro nó cujo resource-id termina com o sufixo dado, ou null. */
function encontrarCentro(xml, sufixoId) {
  for (const trecho of xml.split('<node ')) {
    const idMatch = trecho.match(/resource-id="([^"]*)"/);
    if (!idMatch || !idMatch[1].endsWith(sufixoId)) continue;
    const boundsMatch = trecho.match(/bounds="\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]"/);
    if (!boundsMatch) continue;
    const [, x1, y1, x2, y2] = boundsMatch.map(Number);
    return { x: Math.round((x1 + x2) / 2), y: Math.round((y1 + y2) / 2) };
  }
  return null;
}

function tocarEm(serial, x, y) {
  adb(serial, 'shell', 'input', 'tap', String(x), String(y));
}

/** O nó com resource-id terminando no sufixo dado está com focused="true"? */
function estaFocado(xml, sufixoId) {
  for (const trecho of xml.split('<node ')) {
    const idMatch = trecho.match(/resource-id="([^"]*)"/);
    if (!idMatch || !idMatch[1].endsWith(sufixoId)) continue;
    return /focused="true"/.test(trecho);
  }
  return false;
}

/**
 * Navega por D-pad até o bloco focado ser o sufixo pedido, conferindo o foco de
 * verdade a cada passo em vez de contar toques às cegas — mandar vários
 * `input keyevent` em sequência rápida perdia evento (o foco só andava 2 blocos
 * em vez de 3, achado fotografando `sessao-guia-teclas`). Devolve true se chegou.
 */
function navegarAteFoco(serial, keycode, sufixoAlvo, maxPassos) {
  for (let passo = 0; passo < maxPassos; passo++) {
    if (estaFocado(dumpUi(serial), sufixoAlvo)) return true;
    adb(serial, 'shell', 'input', 'keyevent', String(keycode));
    esperarMs(400);
  }
  return estaFocado(dumpUi(serial), sufixoAlvo);
}

function definirRotacao(serial, paisagem) {
  adb(serial, 'shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0');
  adb(serial, 'shell', 'settings', 'put', 'system', 'user_rotation', paisagem ? '1' : '0');
}

/**
 * `settings put system user_rotation` só pede a rotação; quem decide se ela É USADA é
 * a Activity em primeiro plano (`Game` roda em modo `FULL_USER` de propósito — "sessão
 * livre pra girar", `Game.java:1535` — que RESPEITA essa trava, mas só depois que o
 * WindowManager termina de aplicá-la). Sob `swiftshader_indirect` esse relayout às
 * vezes passa de 1 s; um `am start` disparado antes disso faz a `Game` nascer lendo a
 * orientação antiga (achado fotografando `sessao`/`sessao-texto` em retrato do nada).
 * Espera o `dumpsys` confirmar a rotação de verdade em vez de um tempo fixo torcido.
 */
function esperarRotacaoAplicada(serial, paisagem) {
  const prazo = Date.now() + 4000;
  while (Date.now() < prazo) {
    const saida = adbOut(serial, 'shell', 'dumpsys', 'window', 'displays');
    const landscape = /mDisplayRotation=ROTATION_(90|270)/.test(saida);
    if (landscape === paisagem) return;
    esperarMs(150);
  }
}

// ---------------------------------------------------------------------------------
// Rotas
// ---------------------------------------------------------------------------------

const ROTAS = {
  computadores: {
    componente: 'com.limelight.PcView',
    extras: ['--ez', 'MoqueDeCasca', 'true'],
    paisagem: false,
  },
  apps: {
    componente: 'com.limelight.AppView',
    extras: ['--ez', 'MoqueDeApps', 'true'],
    paisagem: false,
  },
  'apps-foco': {
    // Mesma rota, com o foco movido pro terceiro item da grade de 2 colunas
    // (Corrida Noturna, índice 2) por D-pad — B-CONSOLE.md §F3, "Pronto quando".
    // Um DPAD_DOWN a partir do primeiro item desce uma linha inteira numa GridView
    // de 2 colunas e pousa direto no índice 2, sem depender de como DPAD_RIGHT
    // resolve o fim de uma linha.
    componente: 'com.limelight.AppView',
    extras: ['--ez', 'MoqueDeApps', 'true'],
    paisagem: false,
    depois: (serial) => adb(serial, 'shell', 'input', 'keyevent', '20'),
  },
  // Início (tablet) — Plano B / F3. PcView com os dois moques juntos: a Início
  // mostra "Continuar" (a primeira capa de MoqueDeApps é a "em execução") e a
  // fileira "Apps" das outras cinco.
  inicio: {
    componente: 'com.limelight.PcView',
    extras: ['--ez', 'MoqueDeCasca', 'true', '--ez', 'MoqueDeApps', 'true'],
    paisagem: false,
  },
  'inicio-foco': {
    componente: 'com.limelight.PcView',
    extras: ['--ez', 'MoqueDeCasca', 'true', '--ez', 'MoqueDeApps', 'true'],
    paisagem: false,
    // ←/→ movem o foco na fileira de capas (KEYCODE_DPAD_RIGHT = 22), duas vezes.
    depois: (serial) => {
      adb(serial, 'shell', 'input', 'keyevent', '22');
      adb(serial, 'shell', 'input', 'keyevent', '22');
    },
  },
  'inicio-dialogo': {
    componente: 'com.limelight.PcView',
    extras: ['--ez', 'MoqueDeCasca', 'true', '--ez', 'MoqueDeApps', 'true'],
    paisagem: false,
    // Toque no chip do PC atual abre o diálogo simples (NOTA.md §4).
    depois: (serial) => {
      const xml = dumpUi(serial);
      const centro = encontrarCentro(xml, ':id/consolePcChipAtual');
      if (!centro) {
        console.warn('  aviso: consolePcChipAtual não encontrado — diálogo não foi aberto');
        return;
      }
      tocarEm(serial, centro.x, centro.y);
    },
  },
  // PcView sem moque nenhum: nenhum PC cadastrado, nenhuma rede — o estado "nenhum
  // PC" de verdade (não um moque fingindo vazio).
  vazio: {
    componente: 'com.limelight.PcView',
    extras: [],
    paisagem: false,
  },
  ajustes: {
    componente: 'com.limelight.preferences.StreamSettings',
    extras: [],
    paisagem: false,
  },
  // Plano B / F4. A porta de entrada nova — casca sobre as mesmas chaves de sempre.
  'ajustes-basico': {
    componente: 'com.limelight.preferences.StreamSettingsBasico',
    extras: [],
    paisagem: false,
  },
  'ajustes-avancado': {
    // Toca no card "Avançado", que abre com um Intent (nova Activity) — a árvore
    // inteira de preferences.xml, intocada ("esconder pode, apagar não").
    componente: 'com.limelight.preferences.StreamSettingsBasico',
    extras: [],
    paisagem: false,
    depois: (serial) => {
      const xml = dumpUi(serial);
      const centro = encontrarCentro(xml, ':id/consoleAvancadoCard');
      if (!centro) {
        console.warn('  aviso: consoleAvancadoCard não encontrado — Avançado não foi aberto');
        return;
      }
      tocarEm(serial, centro.x, centro.y);
    },
  },
  // Prova de persistência do "Pronto quando" da F4: muda um controle de cada família
  // (segmentado, seekbar por toque não dá pra arrastar por coordenada fixa com
  // confiança então troca de segmento é quem prova o mecanismo, switch) e reabre a
  // Activity do zero (force-stop + am start de novo) — a foto final é do que
  // SharedPreferences guardou, não do que ficou na tela por acaso.
  'ajustes-persistencia': {
    componente: 'com.limelight.preferences.StreamSettingsBasico',
    extras: [],
    paisagem: false,
    depois: (serial) => {
      const tocarPorId = (sufixo) => {
        const xml = dumpUi(serial);
        const centro = encontrarCentro(xml, sufixo);
        if (!centro) {
          console.warn(`  aviso: ${sufixo} não encontrado — controle não foi tocado`);
          return;
        }
        tocarEm(serial, centro.x, centro.y);
        aguardarAssentar(serial);
      };

      // Achado tocando esta rota: o PRIMEIRO toque numa TextView de segmento (nunca
      // tocada antes nesta instância da Activity) não dispara OnClickListener nenhum
      // — só o SEGUNDO toque no mesmo lugar dispara. Reproduz até sem uiautomator
      // dump nenhum no meio, então não é sobre o dump; e não acontece nos
      // SwitchCompat (tocarPorId de um toque só funciona neles), então não é sobre
      // "primeiro toque depois que a janela ganha foco" em geral — é específico da
      // TextView clicável comum. Por isso os segmentos usam dois toques e os
      // switches, um: um segundo toque num switch já alternado o alternaria de volta.
      const tocarSegmentoPorId = (sufixo) => {
        tocarPorId(sufixo);
        tocarPorId(sufixo);
      };

      // Resolução fica de fora de propósito: o valor padrão (1280x720) não é um dos
      // três segmentos oferecidos, então a linha nasce em modo mono (nenhum
      // consoleSeg1080p/1440p/Nativa existe na árvore agora) — provar que ela
      // continua em mono, sem forçar pro segmento mais próximo, depois do relaunch
      // é o próprio "Pronto quando" da regra, não algo pra este toque mudar.
      tocarSegmentoPorId(':id/consoleSeg90');
      tocarSegmentoPorId(':id/consoleSegCodecHevc');
      tocarSegmentoPorId(':id/consoleSegTouchpad');
      // HDR pode estar desabilitado (sem decoder HEVC/HDR10 no emulador) — o toque
      // não faz nada nesse caso, e é o comportamento certo, não uma falha da rota.
      tocarPorId(':id/consoleHdrSwitch');
      tocarPorId(':id/consoleBarraTextoSwitch');
      tocarPorId(':id/consoleControlesTelaSwitch');

      adb(serial, 'shell', 'am', 'force-stop', PKG);
      adb(serial, 'shell', 'am', 'start', '-n',
          `${PKG}/com.limelight.preferences.StreamSettingsBasico`);
      aguardarAssentar(serial);
    },
  },
  adicionar: {
    componente: 'com.limelight.preferences.AddComputerManually',
    extras: [],
    paisagem: false,
  },
  cenas: {
    componente: 'com.limelight.ProfilesActivity',
    extras: [],
    paisagem: false,
  },
  sessao: {
    componente: 'com.limelight.Game',
    extras: ['--ez', 'MoqueDeStream', 'true'],
    paisagem: true,
  },
  'sessao-retrato': {
    componente: 'com.limelight.Game',
    // Sem paisagem: true — fica na orientação de retrato do s22u (G2, defeito 5).
    // allowPortraitSession é true por padrão (PreferenceConfiguration), então Game
    // roda em SCREEN_ORIENTATION_FULL_USER e deveria caber a superfície do moque em
    // letterbox em vez de cortar ou esticar.
    extras: ['--ez', 'MoqueDeStream', 'true'],
    paisagem: false,
  },
  'sessao-conectando': {
    componente: 'com.limelight.Game',
    // MoqueEstado=conectando trava o moque ANTES de connectionStarted(): a tela de
    // conectar (vidro_connecting.xml, mostrada desde onCreate) fica de pé para sempre
    // em vez de sumir no instante em que a superfície falsa aparece (B-CONSOLE.md §F5).
    extras: ['--ez', 'MoqueDeStream', 'true', '--es', 'MoqueEstado', 'conectando'],
    paisagem: true,
  },
  'sessao-queda': {
    componente: 'com.limelight.Game',
    // MoqueEstado=queda deixa o moque conectar normalmente e, no mesmo instante,
    // mostra vidro_lost.xml por cima — o par exato do que uma queda de rede real
    // mostraria depois que o vídeo já estava de pé. Não é a queda de verdade (fora do
    // escopo desta fatia, "não mexa em rede") — só a pele fotografada sob demanda.
    extras: ['--ez', 'MoqueDeStream', 'true', '--es', 'MoqueEstado', 'queda'],
    paisagem: true,
  },
  'sessao-menu': {
    componente: 'com.limelight.Game',
    extras: ['--ez', 'MoqueDeStream', 'true'],
    paisagem: true,
    // enableBackMenu é true por padrão — é o caminho de hoje pra abrir a guia sem
    // tocar em nada na tela. Ver Game.onBackPressed()/GameMenu (Plano B / F2).
    depois: (serial) => adb(serial, 'shell', 'input', 'keyevent', '4'),
  },
  'sessao-guia': {
    componente: 'com.limelight.Game',
    extras: ['--ez', 'MoqueDeStream', 'true'],
    paisagem: true,
    // A aba (console_aba.xml) é o gatilho de verdade da guia — sempre presa ao
    // centro da borda superior do vídeo desde que connectionStarted() a mostra.
    depois: (serial) => abrirGuiaPorToque(serial),
  },
  'sessao-guia-teclas': {
    componente: 'com.limelight.Game',
    extras: ['--ez', 'MoqueDeStream', 'true'],
    paisagem: true,
    // 3× DPAD_RIGHT a partir do foco inicial (Teclado) chega em "Teclas especiais"
    // (Teclado · Barra de texto · Modo do toque · Teclas especiais); ENTER troca a
    // fileira pelas teclas especiais em vez de fechar a guia.
    depois: (serial) => {
      if (!abrirGuiaPorToque(serial)) return;
      const chegou = navegarAteFoco(serial, 22 /* DPAD_RIGHT */, ':id/consoleBlocoTeclasEspeciais', 6);
      if (!chegou) {
        console.warn('  aviso: D-pad não chegou em "Teclas especiais" — foto mostra o foco onde parou');
      }
      adb(serial, 'shell', 'input', 'keyevent', '66'); // ENTER
      esperarMs(300);
    },
  },
  'sessao-guia-fecha': {
    componente: 'com.limelight.Game',
    extras: ['--ez', 'MoqueDeStream', 'true'],
    paisagem: true,
    // KEYCODE_ESCAPE fecha a guia (GameMenu#showMenu — setOnKeyListener).
    depois: (serial) => {
      if (!abrirGuiaPorToque(serial)) return;
      adb(serial, 'shell', 'input', 'keyevent', '111'); // ESCAPE
    },
  },
  'sessao-texto': {
    componente: 'com.limelight.Game',
    extras: ['--ez', 'MoqueDeStream', 'true'],
    paisagem: true,
    // A barra de texto vive atrás da guia agora (ilha e botão flutuante foram
    // aposentados na F2): abre a guia pela aba e toca no bloco "Barra de texto".
    depois: (serial) => {
      if (!abrirGuiaPorToque(serial)) return;
      const xml = dumpUi(serial);
      const centro = encontrarCentro(xml, ':id/consoleBlocoBarraTexto');
      if (!centro) {
        console.warn('  aviso: bloco "Barra de texto" não encontrado na guia — barra de texto não foi aberta');
        return;
      }
      tocarEm(serial, centro.x, centro.y);
    },
  },
};

/** Toca na aba (console_aba.xml) pra abrir a guia; devolve false com aviso se não
 *  achar (a aba só aparece depois de connectionStarted() — deveria estar sempre lá
 *  no moque de stream, ver Game.setConsoleGuiaOpen/setupConsoleAba). */
function abrirGuiaPorToque(serial) {
  const xml = dumpUi(serial);
  const centro = encontrarCentro(xml, ':id/consoleAba');
  if (!centro) {
    console.warn('  aviso: aba (consoleAba) não encontrada — guia não foi aberta');
    return false;
  }
  tocarEm(serial, centro.x, centro.y);
  aguardarAssentar(serial);
  return true;
}

const ORDEM_PADRAO = Object.keys(ROTAS);

function padroesDeFalha(logcat) {
  // Cada rota faz am force-stop antes de abrir — a morte do processo anterior grava
  // "AndroidRuntime: Shutting down VM" no logcat, uma linha benigna que bateria com o
  // padrão largo do tools/emu.mjs. E o logcat aqui não é filtrado por processo, então
  // "Caused by" de qualquer serviço do sistema (não é raro no emulador) também bateria.
  // Fica só o que é mesmo indício de crash: cabeçalho de FATAL EXCEPTION, os dois tipos
  // de exceção de inflação que já morderam este projeto, e sinal fatal nativo.
  return logcat.split('\n').filter((l) =>
    /FATAL EXCEPTION|ClassCastException|InflateException|Fatal signal/.test(l));
}

/** Só o IHDR (largura/altura), sem inflar o IDAT — rápido o bastante pra chamar a
 * cada tentativa de captura. Mesma suposição do resto do arquivo (screencap do adb:
 * assinatura + IHDR logo em seguida, sem chunk extra antes). */
function dimensoesPng(caminho) {
  const buf = readFileSync(caminho);
  return { largura: buf.readUInt32BE(16), altura: buf.readUInt32BE(20) };
}

function tentarFotografarRota(serial, avd, nome, rota, destino) {
  // force-stop ANTES de mexer na rotação, não depois: a Game da rota anterior (se
  // for o caso) pede paisagem por conta própria (setRequestedOrientation), e isso
  // TEM PRIORIDADE sobre o user_rotation do sistema enquanto ela continuar viva —
  // settings put não convence uma Activity que já travou a própria orientação. Só
  // depois que o processo morre é que o sistema volta a obedecer o user_rotation.
  adb(serial, 'shell', 'am', 'force-stop', PKG);

  // s22u: sessão gira pra paisagem, o resto fica em retrato. tabs8u já nasce em
  // paisagem (é a forma do aparelho) e não é tocado.
  if (avd === 's22u') {
    definirRotacao(serial, rota.paisagem);
    esperarRotacaoAplicada(serial, rota.paisagem);
  }

  adb(serial, 'logcat', '-c');

  const r = adb(serial, 'shell', 'am', 'start', '-n', `${PKG}/${rota.componente}`, ...rota.extras);
  const saida = (r.stdout || '') + (r.stderr || '');
  if (/^Error|Exception|Permission Denial/m.test(saida)) {
    console.error(`  FALHA ao abrir ${nome}:\n${saida.trim()}`);
    return false;
  }

  aguardarAssentar(serial);

  if (rota.depois) {
    rota.depois(serial);
    aguardarAssentar(serial);
  }

  const log = adb(serial, 'logcat', '-d', '-t', '400').stdout || '';
  const falhas = padroesDeFalha(log);
  if (falhas.length) {
    console.error(`  CRASH em ${nome}:\n${falhas.slice(0, 15).join('\n')}`);
    return false;
  }

  adb(serial, 'shell', 'screencap', '-p', '/sdcard/_fotos_shot.png');
  adb(serial, 'pull', '/sdcard/_fotos_shot.png', destino);
  if (!existsSync(destino)) {
    console.error(`  screencap não produziu ${destino}`);
    return false;
  }

  return true;
}

function fotografarRota(serial, avd, nome, destino) {
  const rota = ROTAS[nome];
  if (!rota) {
    console.error(`rota desconhecida: ${nome} (válidas: ${ORDEM_PADRAO.join(', ')})`);
    process.exit(1);
  }

  // `Game` roda em modo FULL_USER de propósito (sessão livre pra girar) e por isso
  // só reflete a rotação de sistema que o WindowManager já tiver de fato aplicado no
  // instante do `am start` — e isso pode ficar assíncrono mesmo depois de
  // esperarRotacaoAplicada() confirmar via dumpsys (a janela de corrida é entre esse
  // dumpsys e o am start em si, não antes dele). Em vez de adivinhar mais um tempo de
  // espera, confere o resultado de verdade — a foto pulada — e tenta de novo se saiu
  // com a orientação errada. Só se aplica a s22u; tabs8u nunca gira.
  const tentativas = avd === 's22u' ? 3 : 1;
  for (let tentativa = 1; tentativa <= tentativas; tentativa++) {
    if (!tentarFotografarRota(serial, avd, nome, rota, destino)) return false;

    if (avd === 's22u') {
      const { largura, altura } = dimensoesPng(destino);
      const saiuPaisagem = largura > altura;
      if (saiuPaisagem !== rota.paisagem && tentativa < tentativas) {
        console.warn(`  aviso: ${nome} saiu em ${saiuPaisagem ? 'paisagem' : 'retrato'} (esperava ${rota.paisagem ? 'paisagem' : 'retrato'}) — repetindo (${tentativa}/${tentativas})`);
        continue;
      }
    }

    console.log(`  ${destino}`);
    return true;
  }
  return true;
}

// ---------------------------------------------------------------------------------
// PNG mínimo (decode/encode 8-bit RGB/RGBA, sem interlace) — só o suficiente pra
// montar a folha de contato a partir de screencaps do adb. A parte difícil (inflate/
// deflate do IDAT) já vem de node:zlib; o resto é chunk e o filtro de linha do PNG
// (RFC 2083 §6). Zero dependência nova.
// ---------------------------------------------------------------------------------

const ASSINATURA_PNG = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

const TABELA_CRC = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
    t[n] = c >>> 0;
  }
  return t;
})();

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = TABELA_CRC[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function lerChunks(buf) {
  const chunks = [];
  let pos = 8;
  while (pos + 8 <= buf.length) {
    const len = buf.readUInt32BE(pos);
    const tipo = buf.toString('ascii', pos + 4, pos + 8);
    chunks.push({ tipo, dados: buf.subarray(pos + 8, pos + 8 + len) });
    pos += 12 + len; // len + tipo(4) + dados + crc(4)
  }
  return chunks;
}

function paeth(a, b, c) {
  const p = a + b - c;
  const pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
  if (pa <= pb && pa <= pc) return a;
  if (pb <= pc) return b;
  return c;
}

/** Só o que o screencap do adb produz: 8-bit, sem interlace, RGB ou RGBA. */
function decodificarPng(buf) {
  if (!buf.subarray(0, 8).equals(ASSINATURA_PNG)) throw new Error('não é PNG');

  const chunks = lerChunks(buf);
  const ihdr = chunks.find((c) => c.tipo === 'IHDR').dados;
  const largura = ihdr.readUInt32BE(0);
  const altura = ihdr.readUInt32BE(4);
  const bitDepth = ihdr[8];
  const corTipo = ihdr[9];
  const interlace = ihdr[12];

  if (bitDepth !== 8 || interlace !== 0 || (corTipo !== 2 && corTipo !== 6)) {
    throw new Error(
      `PNG fora do que este codificador mínimo lê (bitDepth=${bitDepth} corTipo=${corTipo} ` +
      `interlace=${interlace}) — o screencap do adb sempre gera 8-bit RGB/RGBA sem interlace; ` +
      'se isto disparou, o formato do screencap mudou.');
  }

  const canais = corTipo === 6 ? 4 : 3;
  const idat = Buffer.concat(chunks.filter((c) => c.tipo === 'IDAT').map((c) => c.dados));
  const bruto = inflateSync(idat);

  const stride = largura * canais;
  const pixels = Buffer.alloc(altura * stride);
  let posEntrada = 0;

  for (let y = 0; y < altura; y++) {
    const filtro = bruto[posEntrada++];
    const linha = bruto.subarray(posEntrada, posEntrada + stride);
    posEntrada += stride;
    const saida = pixels.subarray(y * stride, y * stride + stride);

    for (let x = 0; x < stride; x++) {
      const a = x >= canais ? saida[x - canais] : 0;
      const b = y > 0 ? pixels[(y - 1) * stride + x] : 0;
      const c = (y > 0 && x >= canais) ? pixels[(y - 1) * stride + x - canais] : 0;
      let valor = linha[x];
      switch (filtro) {
        case 0: break;
        case 1: valor = (valor + a) & 0xff; break;
        case 2: valor = (valor + b) & 0xff; break;
        case 3: valor = (valor + ((a + b) >> 1)) & 0xff; break;
        case 4: valor = (valor + paeth(a, b, c)) & 0xff; break;
        default: throw new Error('filtro PNG desconhecido: ' + filtro);
      }
      saida[x] = valor;
    }
  }

  return { largura, altura, canais, pixels };
}

function montarChunk(tipo, dados) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(dados.length, 0);
  const corpo = Buffer.concat([Buffer.from(tipo, 'ascii'), dados]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(corpo), 0);
  return Buffer.concat([len, corpo, crc]);
}

/** Sempre codifica como RGBA — a folha não precisa economizar byte. */
function codificarPng(largura, altura, pixelsRGBA) {
  const stride = largura * 4;
  const comFiltro = Buffer.alloc(altura * (stride + 1));
  for (let y = 0; y < altura; y++) {
    comFiltro[y * (stride + 1)] = 0; // filtro None
    pixelsRGBA.copy(comFiltro, y * (stride + 1) + 1, y * stride, y * stride + stride);
  }

  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(largura, 0);
  ihdr.writeUInt32BE(altura, 4);
  ihdr[8] = 8;  // bit depth
  ihdr[9] = 6;  // RGBA
  // 10-12 ficam 0: compressão/filtro/interlace padrão

  return Buffer.concat([
    ASSINATURA_PNG,
    montarChunk('IHDR', ihdr),
    montarChunk('IDAT', deflateSync(comFiltro)),
    montarChunk('IEND', Buffer.alloc(0)),
  ]);
}

function redimensionar(origem, novaLargura, novaAltura) {
  const { largura, altura, canais, pixels } = origem;
  const destino = Buffer.alloc(novaLargura * novaAltura * 4);
  for (let y = 0; y < novaAltura; y++) {
    const sy = Math.min(altura - 1, Math.floor((y * altura) / novaAltura));
    for (let x = 0; x < novaLargura; x++) {
      const sx = Math.min(largura - 1, Math.floor((x * largura) / novaLargura));
      const oi = (sy * largura + sx) * canais;
      const di = (y * novaLargura + x) * 4;
      destino[di] = pixels[oi];
      destino[di + 1] = pixels[oi + 1];
      destino[di + 2] = pixels[oi + 2];
      destino[di + 3] = canais === 4 ? pixels[oi + 3] : 255;
    }
  }
  return destino;
}

function colar(destino, largDestino, altDestino, origem, w, h, offX, offY) {
  for (let y = 0; y < h; y++) {
    const dy = offY + y;
    if (dy < 0 || dy >= altDestino) continue;
    for (let x = 0; x < w; x++) {
      const dx = offX + x;
      if (dx < 0 || dx >= largDestino) continue;
      const si = (y * w + x) * 4;
      const di = (dy * largDestino + dx) * 4;
      destino[di] = origem[si];
      destino[di + 1] = origem[si + 1];
      destino[di + 2] = origem[si + 2];
      destino[di + 3] = 255;
    }
  }
}

// Fonte de bloco, só com o que os nomes de rota usam (maiúsculas + hífen). Cada glifo
// é uma grade de linhas '#'/'.'; a largura pode variar por glifo.
const FONTE = {
  A: ['.##.', '#..#', '####', '#..#', '#..#'],
  C: ['.###', '#...', '#...', '#...', '.###'],
  D: ['###.', '#..#', '#..#', '#..#', '###.'],
  E: ['####', '#...', '###.', '#...', '####'],
  I: ['###', '.#.', '.#.', '.#.', '###'],
  J: ['..##', '...#', '...#', '#..#', '.##.'],
  M: ['#...#', '##.##', '#.#.#', '#...#', '#...#'],
  N: ['#...#', '##..#', '#.#.#', '#..##', '#...#'],
  O: ['.##.', '#..#', '#..#', '#..#', '.##.'],
  P: ['###.', '#..#', '###.', '#...', '#...'],
  R: ['###.', '#..#', '###.', '#.#.', '#..#'],
  S: ['.###', '#...', '.##.', '...#', '###.'],
  T: ['###', '.#.', '.#.', '.#.', '.#.'],
  U: ['#..#', '#..#', '#..#', '#..#', '.##.'],
  X: ['#...#', '.#.#.', '..#..', '.#.#.', '#...#'],
  '-': ['....', '....', '####', '....', '....'],
};
const ESCALA_FONTE = 3;
const ESPACO_GLIFO = 1;

function desenharTexto(tela, largTela, altTela, texto, x, y, larguraDisponivel) {
  const glifos = [...texto.toUpperCase()].map((ch) => FONTE[ch]).filter(Boolean);
  if (!glifos.length) return;

  const larguraTotal = glifos.reduce(
    (acc, g) => acc + (g[0].length + ESPACO_GLIFO) * ESCALA_FONTE, 0);
  let cursorX = x + Math.max(0, Math.floor((larguraDisponivel - larguraTotal) / 2));

  for (const glifo of glifos) {
    for (let ly = 0; ly < glifo.length; ly++) {
      for (let lx = 0; lx < glifo[ly].length; lx++) {
        if (glifo[ly][lx] !== '#') continue;
        for (let sy = 0; sy < ESCALA_FONTE; sy++) {
          for (let sx = 0; sx < ESCALA_FONTE; sx++) {
            const px = cursorX + lx * ESCALA_FONTE + sx;
            const py = y + ly * ESCALA_FONTE + sy;
            if (px < 0 || px >= largTela || py < 0 || py >= altTela) continue;
            const di = (py * largTela + px) * 4;
            tela[di] = 0xf2; tela[di + 1] = 0xf2; tela[di + 2] = 0xf4; tela[di + 3] = 0xff;
          }
        }
      }
    }
    cursorX += (glifo[0].length + ESPACO_GLIFO) * ESCALA_FONTE;
  }
}

function montarFolhaDeContato(fotos) {
  const COLS = 4;
  const CELL_W = 260;
  const CELL_H = 260;
  const LABEL_H = 22;
  const PAD = 16;
  const rows = Math.ceil(fotos.length / COLS);
  const largura = PAD + COLS * (CELL_W + PAD);
  const altura = PAD + rows * (CELL_H + LABEL_H + PAD);

  const tela = Buffer.alloc(largura * altura * 4);
  for (let i = 0; i < tela.length; i += 4) {
    tela[i] = 0x18; tela[i + 1] = 0x18; tela[i + 2] = 0x1c; tela[i + 3] = 0xff;
  }

  fotos.forEach((foto, idx) => {
    const col = idx % COLS;
    const row = Math.floor(idx / COLS);
    const cellX = PAD + col * (CELL_W + PAD);
    const cellY = PAD + row * (CELL_H + LABEL_H + PAD);

    const escala = Math.min(CELL_W / foto.largura, CELL_H / foto.altura);
    const w = Math.max(1, Math.round(foto.largura * escala));
    const h = Math.max(1, Math.round(foto.altura * escala));
    const mini = redimensionar(foto, w, h);

    const offX = cellX + Math.floor((CELL_W - w) / 2);
    const offY = cellY + Math.floor((CELL_H - h) / 2);
    colar(tela, largura, altura, mini, w, h, offX, offY);

    desenharTexto(tela, largura, altura, foto.rota, cellX, cellY + CELL_H + 4, CELL_W);
  });

  return codificarPng(largura, altura, tela);
}

// ---------------------------------------------------------------------------------
// Principal
// ---------------------------------------------------------------------------------

/**
 * Ida e volta do codificador de PNG contra um buffer sintético — sem emulador, sem
 * adb. `node tools/fotos.mjs --selftest`. Existe porque este codificador é código
 * novo e sem teste automatizado (não há harness de unidade pra tools/*.mjs no
 * repositório); rodar contra um PNG de verdade só no fim, depois do build e do
 * emulador de pé, seria tarde demais pra um bug de chunk ou de filtro de linha.
 */
/**
 * O autoteste principal só passa pelos meus dois lados (codifico e decodifico), e eu
 * sempre escrevo o filtro None — então nunca exercitaria a reconstrução de Sub/Up/
 * Average/Paeth se um PNG de verdade (que escolhe o filtro por linha) tivesse um bug
 * bem ali. Isto monta os cinco filtros à mão, do lado de fora do meu próprio código,
 * e confere se decodificarPng devolve os pixels originais pra cada um.
 */
function autoTestarFiltros() {
  const largura = 3, altura = 2, canais = 4;
  const stride = largura * canais;
  // Não-monótono de propósito, pra Sub/Average/Paeth terem o que fazer.
  const bruto = Buffer.from([
    10, 200, 30, 255, 250, 5, 210, 255, 40, 40, 40, 255,
    5, 6, 7, 255, 100, 150, 200, 255, 0, 255, 128, 255,
  ]);

  for (let tipo = 0; tipo <= 4; tipo++) {
    const comFiltro = Buffer.alloc(altura * (stride + 1));
    for (let y = 0; y < altura; y++) {
      comFiltro[y * (stride + 1)] = tipo;
      for (let x = 0; x < stride; x++) {
        const orig = bruto[y * stride + x];
        const a = x >= canais ? bruto[y * stride + x - canais] : 0;
        const b = y > 0 ? bruto[(y - 1) * stride + x] : 0;
        const c = (y > 0 && x >= canais) ? bruto[(y - 1) * stride + x - canais] : 0;
        let valor;
        switch (tipo) {
          case 0: valor = orig; break;
          case 1: valor = (orig - a) & 0xff; break;
          case 2: valor = (orig - b) & 0xff; break;
          case 3: valor = (orig - Math.floor((a + b) / 2)) & 0xff; break;
          case 4: valor = (orig - paeth(a, b, c)) & 0xff; break;
        }
        comFiltro[y * (stride + 1) + 1 + x] = valor;
      }
    }

    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(largura, 0);
    ihdr.writeUInt32BE(altura, 4);
    ihdr[8] = 8;
    ihdr[9] = 6;
    const png = Buffer.concat([
      ASSINATURA_PNG,
      montarChunk('IHDR', ihdr),
      montarChunk('IDAT', deflateSync(comFiltro)),
      montarChunk('IEND', Buffer.alloc(0)),
    ]);

    const decodificado = decodificarPng(png);
    if (!decodificado.pixels.equals(bruto)) {
      console.error(`FALHA: filtro tipo ${tipo} não reconstruiu os pixels originais`);
      console.error('  esperado: ' + bruto.join(','));
      console.error('  obtido:   ' + decodificado.pixels.join(','));
      process.exit(1);
    }
  }

  console.log('autoteste dos 5 filtros de linha do PNG: OK');
}

function autoTeste() {
  const w = 37, h = 29; // dimensões ímpares, de propósito: pega erro de stride
  const original = Buffer.alloc(w * h * 4);
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const i = (y * w + x) * 4;
      original[i] = (x * 7) & 0xff;
      original[i + 1] = (y * 11) & 0xff;
      original[i + 2] = ((x + y) * 5) & 0xff;
      original[i + 3] = 255;
    }
  }

  const png = codificarPng(w, h, original);
  const decodificado = decodificarPng(png);

  if (decodificado.largura !== w || decodificado.altura !== h) {
    console.error(`FALHA: dimensão veio ${decodificado.largura}x${decodificado.altura}, esperava ${w}x${h}`);
    process.exit(1);
  }
  if (!decodificado.pixels.equals(original)) {
    let primeiraDiferenca = -1;
    for (let i = 0; i < original.length; i++) {
      if (decodificado.pixels[i] !== original[i]) { primeiraDiferenca = i; break; }
    }
    console.error(`FALHA: pixels não batem (primeira diferença no byte ${primeiraDiferenca})`);
    process.exit(1);
  }

  // A folha de contato completa, com uma foto sintética só — cobre redimensionar,
  // colar e desenharTexto, não só o par codificar/decodificar.
  const folha = montarFolhaDeContato([{ rota: 'sessao-menu', largura: w, altura: h, canais: 4, pixels: original }]);
  const relida = decodificarPng(folha);
  if (relida.largura <= 0 || relida.altura <= 0) {
    console.error('FALHA: folha de contato saiu com dimensão inválida');
    process.exit(1);
  }

  console.log(`autoteste do PNG: OK (${w}x${h} ida e volta, folha ${relida.largura}x${relida.altura})`);
}

function main() {
  if (process.argv.includes('--selftest')) {
    autoTestarFiltros();
    autoTeste();
    return;
  }

  const args = lerArgs(process.argv.slice(2));

  const rotas = args.rotas || ORDEM_PADRAO;
  for (const r of rotas) {
    if (!ROTAS[r]) {
      console.error(`rota desconhecida: ${r} (válidas: ${ORDEM_PADRAO.join(', ')})`);
      process.exit(1);
    }
  }

  const destDir = join(ROOT, 'app', 'build', 'fotos', args.avd);
  mkdirSync(destDir, { recursive: true });

  garantirApk();
  const serial = garantirEmulador(args.avd, args.port, args.janela);
  instalarApk(serial);

  console.log(`\nfotografando ${rotas.length} rota(s) em ${args.avd}…\n`);

  const falharam = [];
  const capturadas = [];
  for (const nome of rotas) {
    console.log(`- ${nome}`);
    const destino = join(destDir, `${nome}.png`);
    const ok = fotografarRota(serial, args.avd, nome, destino);
    if (ok) capturadas.push({ rota: nome, destino });
    else falharam.push(nome);
  }

  // Volta o s22u pro estado de repouso (retrato) — o AVD é reusado entre execuções.
  // force-stop primeiro: se a última rota foi de sessão, a Game ainda viva está
  // travada em paisagem por conta própria e nenhum settings put desfaz isso sozinho
  // (mesmo motivo do começo de fotografarRota).
  if (args.avd === 's22u') {
    adb(serial, 'shell', 'am', 'force-stop', PKG);
    definirRotacao(serial, false);
  }

  if (capturadas.length) {
    console.log('\nmontando folha de contato…');
    const fotos = capturadas.map(({ rota, destino }) => {
      const decodificada = decodificarPng(readFileSync(destino));
      return { rota, ...decodificada };
    });
    const folha = montarFolhaDeContato(fotos);
    const folhaPath = join(destDir, 'folha.png');
    writeFileSync(folhaPath, folha);
    console.log(`  ${folhaPath}`);
  }

  console.log(`\n${capturadas.length}/${rotas.length} rota(s) capturada(s) em ${destDir}`);

  if (falharam.length) {
    console.error(`\nfalharam: ${falharam.join(', ')}`);
    process.exitCode = 1;
  }
}

main();
