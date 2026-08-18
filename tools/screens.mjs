#!/usr/bin/env node
/**
 * Captura as telas do app em PNG sem emulador e sem APK.
 *
 * Roda o VidroScreenshotTest com o Robolectric em modo de gráficos NATIVO: o layout é
 * rasterizado de verdade na JVM, com as fontes empacotadas e os drawables reais. O ciclo
 * fica em segundos em vez dos minutos de gerar, instalar e olhar um APK.
 *
 *   node tools/screens.mjs             captura tudo
 *   node tools/screens.mjs --open      captura e abre a pasta
 *
 * O que ele NÃO mostra: vídeo (SurfaceView é camada de hardware e não existe aqui),
 * animação, gesto e qualquer dado que só chegue pela rede. Serve para composição,
 * medida, cor, contraste e tipografia.
 */
import { spawnSync } from 'node:child_process';
import { existsSync, readdirSync, rmSync, statSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const OUT = join(ROOT, 'app', 'build', 'vidro-screens');
const ALT = join(ROOT, 'build', 'vidro-screens');

const args = process.argv.slice(2);
const open = args.includes('--open');

// Começar limpo, senão uma tela que parou de ser gerada fica na pasta parecendo atual.
for (const dir of [OUT, ALT]) {
  if (existsSync(dir)) rmSync(dir, { recursive: true, force: true });
}

const gradlew = join(ROOT, process.platform === 'win32' ? 'gradlew.bat' : 'gradlew');

console.log('capturando as telas…\n');

const run = spawnSync(
  gradlew,
  [
    ':app:testNonRoot_gameDebugUnitTest',
    '--tests', 'com.limelight.VidroScreenshotTest',
    '-Dvidro.screens=1',
    '--console=plain',
    '-q',
  ],
  { cwd: ROOT, stdio: ['ignore', 'pipe', 'pipe'], encoding: 'utf8', shell: true },
);

const output = (run.stdout || '') + (run.stderr || '');

// O Gradle engole System.out do teste por padrão; as linhas que interessam são as que o
// harness imprime. Se não vier nenhuma, mostramos o erro cru em vez de fingir sucesso.
const shot = output.split('\n').filter((l) => l.includes('[vidro-screens]'));

const dir = existsSync(OUT) ? OUT : existsSync(ALT) ? ALT : null;

if (!dir) {
  console.error('nenhuma imagem foi escrita.\n');
  console.error(output.split('\n').filter((l) => /error|FAIL|Exception|Caused by/i.test(l))
    .slice(0, 25).join('\n') || output.slice(-3000));
  process.exit(1);
}

const files = readdirSync(dir).filter((f) => f.endsWith('.png')).sort();

console.log(`${files.length} tela(s) em ${dir}\n`);
for (const f of files) {
  const kb = (statSync(join(dir, f)).size / 1024).toFixed(0);
  console.log(`  ${f.padEnd(26)} ${kb.padStart(5)} KB`);
}

// Uma Activity que não sobe some da lista de PNG sem deixar rastro; sem isto o
// resultado parece completo e não está.
const broke = shot.filter((l) => l.includes('!!'));
if (broke.length) {
  console.log('\nActivity que NAO subiu:\n');
  for (const l of broke) console.log('  ' + l.replace('[vidro-screens] !! ', ''));
}

const ok = shot.filter((l) => !l.includes('!!'));
if (ok.length) {
  console.log();
  for (const l of ok) console.log('  ' + l.replace('[vidro-screens] ', ''));
}

if (broke.length) process.exitCode = 1;

if (open) {
  const opener = process.platform === 'win32' ? 'explorer'
    : process.platform === 'darwin' ? 'open' : 'xdg-open';
  spawnSync(opener, [dir], { shell: true });
}
