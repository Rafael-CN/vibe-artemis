#!/usr/bin/env node
/**
 * Sobe o emulador, instala o app e captura a tela de verdade.
 *
 *   node tools/emu.mjs              build, instala, abre a home e captura
 *   node tools/emu.mjs --boot       só garante o emulador de pé
 *   node tools/emu.mjs --shot nome  captura o que estiver na tela agora
 *
 * Complementa `tools/screens.mjs`. O harness do Robolectric é o loop rápido para
 * composição e tipografia; isto aqui é o que pega o que ele não vê — crash de cast na
 * inflação, comportamento que depende de serviço, e a tela como o Android realmente a
 * compõe. Foi assim que apareceu o ClassCastException que deixava a home preta.
 */
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const SDK = join(process.env.LOCALAPPDATA || process.env.HOME, 'Android', 'Sdk');
const ADB = join(SDK, 'platform-tools', process.platform === 'win32' ? 'adb.exe' : 'adb');
const EMULATOR = join(SDK, 'emulator', process.platform === 'win32' ? 'emulator.exe' : 'emulator');
const OUT = join(ROOT, 'build', 'emu');
const PKG = 'com.limelight.noirdebug';
const AVD = 'vidro';

const args = process.argv.slice(2);
const sh = (cmd, a, opts = {}) =>
  spawnSync(cmd, a, { encoding: 'utf8', ...opts });

const adb = (...a) => sh(ADB, a).stdout || '';

function booted() {
  return adb('shell', 'getprop', 'sys.boot_completed').trim() === '1';
}

function boot() {
  if (booted()) return console.log('emulador já de pé');

  console.log('subindo o emulador…');
  // Sem janela: só precisamos do screencap, e com janela ele briga por GPU com o build.
  spawnSync(EMULATOR, ['-avd', AVD, '-no-window', '-no-audio', '-no-boot-anim',
    '-gpu', 'swiftshader_indirect', '-no-snapshot'], { detached: true, stdio: 'ignore' });

  const deadline = Date.now() + 240000;
  while (Date.now() < deadline) {
    if (booted()) return console.log('emulador pronto');
    sh(process.platform === 'win32' ? 'cmd' : 'sh',
       process.platform === 'win32' ? ['/c', 'timeout', '/t', '3', '/nobreak'] : ['-c', 'sleep 3'],
       { stdio: 'ignore' });
  }
  console.error('emulador não subiu em 4 min');
  process.exit(1);
}

function shot(name) {
  mkdirSync(OUT, { recursive: true });
  adb('shell', 'screencap', '-p', '/sdcard/_v.png');
  adb('pull', '/sdcard/_v.png', join(OUT, `${name}.png`));
  console.log(`  ${join(OUT, `${name}.png`)}`);
}

boot();
if (args.includes('--boot')) process.exit(0);

const shotIdx = args.indexOf('--shot');
if (shotIdx >= 0) {
  shot(args[shotIdx + 1] || 'tela');
  process.exit(0);
}

console.log('compilando…');
const gradlew = join(ROOT, process.platform === 'win32' ? 'gradlew.bat' : 'gradlew');
const build = sh(gradlew, ['assembleNonRoot_gameDebug', '--console=plain', '-q'],
  { cwd: ROOT, shell: true });
if (build.status !== 0) {
  console.error((build.stdout || '') + (build.stderr || ''));
  process.exit(1);
}

// x86_64: é o ABI do emulador, não o do aparelho.
const apk = join(ROOT, 'app', 'build', 'outputs', 'apk', 'nonRoot_game', 'debug',
  'app-nonRoot_game-x86_64-debug.apk');
if (!existsSync(apk)) {
  console.error('APK x86_64 não encontrado em ' + apk);
  process.exit(1);
}

console.log('instalando…');
console.log('  ' + adb('install', '-r', '-t', apk).trim().split('\n').pop());

adb('logcat', '-c');
adb('shell', 'am', 'start', '-n', `${PKG}/com.limelight.PcView`);

sh(process.platform === 'win32' ? 'cmd' : 'sh',
   process.platform === 'win32' ? ['/c', 'timeout', '/t', '7', '/nobreak'] : ['-c', 'sleep 7'],
   { stdio: 'ignore' });

// Um crash aqui é a coisa mais valiosa que este script produz — nunca deixá-lo passar
// como "capturou e pronto".
const log = adb('logcat', '-d', '-t', '400');
const fatal = log.split('\n').filter((l) =>
  /FATAL|AndroidRuntime: |ClassCastException|InflateException|Caused by/.test(l));

console.log('\ncapturando…');
shot('home-real');

if (fatal.length) {
  console.error('\nCRASH:\n');
  console.error(fatal.slice(0, 20).join('\n'));
  process.exitCode = 1;
}
else {
  console.log('\nsem crash');
}
