// Builds an Allure report from the results of every framework that has run - with Allure 2 or Allure 3.
// Both read the SAME allure-results folders (the result format did not change), only the report UI differs:
//   Allure 2 (allure-commandline, Java): the classic report - Overview, Categories, Suites, Graphs, Timeline,
//            Behaviors, Packages. The one the Jenkins Allure plugin shows.
//   Allure 3 (allure, Node.js): the new "awesome" report, configured in allurerc.mjs.
//
//   node scripts/allure-report.mjs <2|3> generate   -> allure-report-2/index.html or allure-report-3/index.html
//   node scripts/allure-report.mjs <2|3> open       -> generate + open it in the default browser
// Both reports are SINGLE-FILE: one index.html with all data and attachments inside, so it works as file://
// (double click, e-mail, CI artifact) without a web server. A regular multi-file Allure report loads every test
// and tab with fetch() and shows "Failed to fetch" unless a server (allure open) keeps running.
// Only some frameworks:  node scripts/allure-report.mjs 2 open selenide-testng   (selenide | selenide-testng | playwright-ts)
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, readdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const [version, command = 'generate', ...only] = process.argv.slice(2);
const reportName = 'Sandbox Shop: Selenide (JUnit 5 / TestNG) vs Playwright TS';

if (!['2', '3'].includes(version) || !['generate', 'open'].includes(command)) {
  console.error('Usage: node scripts/allure-report.mjs <2|3> <generate|open> [selenide] [selenide-testng] [playwright-ts]');
  process.exit(1);
}

const results = {
  selenide: 'selenide/target/allure-results',
  'selenide-testng': 'selenide-testng/target/allure-results',
  'playwright-ts': 'playwright-ts/allure-results',
};
const unknown = only.filter(name => !(name in results));
if (unknown.length > 0) {
  console.error(`Unknown framework: ${unknown.join(', ')}. Use: ${Object.keys(results).join(', ')}`);
  process.exit(1);
}
const dirs = (only.length > 0 ? only : Object.keys(results))
  .map(name => path.resolve(root, results[name]))
  .filter(dir => existsSync(dir) && readdirSync(dir).length > 0);

if (dirs.length === 0) {
  console.error('No allure-results found - run some tests first.');
  process.exit(1);
}
const output = path.join(root, `allure-report-${version}`);
console.log(`[allure ${version}] results from:\n  ` + dirs.map(d => path.relative(root, d)).join('\n  '));

const notInstalled = file => {
  if (!existsSync(file)) {
    console.error('Allure is not installed - run `npm ci` in the repository root first.');
    process.exit(1);
  }
};
// Allure 2 is Java. JDK 25 on Windows: NIO creates an AF_UNIX socket in %TEMP%, and an 8.3 short name there
// (C:\Users\JOHNDO~1) breaks it - "Unable to establish loopback connection" (same fix as in pom.xml, see README).
const allure2Env = () => {
  const tmp = path.join(root, 'node_modules', '.allure-tmp');
  mkdirSync(tmp, { recursive: true });
  return { ...process.env, ALLURE_OPTS: `${process.env.ALLURE_OPTS ?? ''} -Djdk.net.unixdomain.tmpdir="${tmp}"`.trim() };
};
const run = (exe, args) => {
  // allure.bat is a batch file: Windows runs it only through cmd.exe. Every part is quoted (paths with spaces),
  // and /s + outer quotes keep cmd from re-parsing the line.
  const result = exe.endsWith('.bat')
    ? spawnSync('cmd.exe', ['/d', '/s', '/c', `"${[exe, ...args].map(a => `"${a}"`).join(' ')}"`], {
        cwd: root,
        stdio: 'inherit',
        windowsVerbatimArguments: true,
        env: allure2Env(),
      })
    : spawnSync(exe, args, { cwd: root, stdio: 'inherit', env: allure2Env() });
  if (result.status !== 0) process.exit(result.status ?? 1);
};

if (version === '3') {
  const cli = path.join(root, 'node_modules', 'allure', 'cli.js');
  notInstalled(cli);
  // single file: singleFile: true in allurerc.mjs
  run(process.execPath, [cli, 'generate', ...dirs, '--config', path.join(root, 'allurerc.mjs'), '--output', output, '--clean']);
} else {
  const bin = path.join(root, 'node_modules', 'allure-commandline', 'dist', 'bin', process.platform === 'win32' ? 'allure.bat' : 'allure');
  notInstalled(bin);
  run(bin, ['generate', ...dirs, '--output', output, '--clean', '--single-file', '--name', reportName]);
}

const index = path.join(output, 'index.html');
console.log(`[allure ${version}] report: ${path.relative(root, index)}`);
if (command === 'open') {
  // default browser; "start" is a cmd built-in, the empty "" is its window title
  if (process.platform === 'win32') spawnSync('cmd.exe', ['/d', '/c', 'start', '""', index], { stdio: 'ignore' });
  else spawnSync(process.platform === 'darwin' ? 'open' : 'xdg-open', [index], { stdio: 'ignore' });
}
