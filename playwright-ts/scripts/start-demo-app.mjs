// Starts Sandbox Shop for Playwright's `webServer` (or manually: `npm run app`).
// Builds demo-app with Maven on the first run, then runs it with plain `java`.
import { spawn, spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const target = path.join(root, 'demo-app', 'target');
const port = process.env.APP_PORT ?? '4321';

if (!existsSync(path.join(target, 'lib'))) {
  const mvnw = process.platform === 'win32' ? 'mvnw.cmd' : './mvnw';
  const build = spawnSync(mvnw, ['-q', '-pl', 'demo-app', 'package', '-DskipTests'], {
    cwd: root, stdio: 'inherit', shell: true,
  });
  if (build.status !== 0) process.exit(build.status ?? 1);
}

const classpath = [path.join(target, 'classes'), path.join(target, 'lib', '*')].join(path.delimiter);
const app = spawn('java', ['-cp', classpath, 'com.sandbox.app.DemoApp', port], { stdio: 'inherit' });

for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => app.kill());
app.on('exit', code => process.exit(code ?? 0));
