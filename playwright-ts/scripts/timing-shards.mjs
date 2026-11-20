// Sharding by TIME instead of by count - free, works in any CI. Playwright's own --shard=i/N cuts the test list
// into N parts with the same NUMBER of tests; slow tests can pile up in one shard and the whole run waits for it.
// Here every test goes to the shard that is "lightest" so far, longest tests first (greedy LPT), using the
// durations of an earlier run. Each shard then runs exactly its tests: npx playwright test --test-list <file>.
//
//   node scripts/timing-shards.mjs save [--from test-results/junit.xml]   junit.xml of a full run -> test-timings.json
//   node scripts/timing-shards.mjs plan --total 3                          compare --shard and the time split
//   node scripts/timing-shards.mjs list --index 2 --total 3 --out shard.txt   tests of shard 2 of 3 for --test-list
//
// Test ids are "file › describe › title" - the format --test-list accepts (file relative to testDir).
// The file part always uses "/": Playwright writes "examples\x.spec.ts" on Windows and "examples/x.spec.ts" on
// Linux, so timings saved on a CI runner would not match a local list (and the other way round).
import { spawnSync } from 'node:child_process';
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const TIMINGS = path.join(root, 'test-timings.json');
const [command, ...rest] = process.argv.slice(2);
const opt = name => {
  const i = rest.indexOf(`--${name}`);
  return i >= 0 ? rest[i + 1] : undefined;
};

// ------------------------------------------------------------------ helpers

const slashes = file => file.replace(/\\/g, '/');

/** All tests Playwright would run now (same config, same filters): ids + count-based shard if asked. */
function listTests(shard) {
  const cli = path.join(root, 'node_modules', 'playwright', 'cli.js');
  const args = [cli, 'test', '--list', '--reporter=json', ...(shard ? [`--shard=${shard}`] : [])];
  const result = spawnSync(process.execPath, args, { cwd: root, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
  if (result.status !== 0) throw new Error(`playwright --list failed:\n${result.stderr}`);
  const ids = new Set();
  const walk = (suite, titles) => {
    for (const spec of suite.specs ?? []) ids.add([...titles, spec.title].join(' › '));
    for (const child of suite.suites ?? []) walk(child, [...titles, child.title]);
  };
  for (const fileSuite of JSON.parse(result.stdout).suites) walk(fileSuite, [slashes(fileSuite.title)]);
  return [...ids];
}

const decode = s =>
  s.replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&');

/** junit.xml of Playwright: classname = file, name = "describe › title", time = seconds. */
function timingsFromJunit(file) {
  const timings = {};
  const re = /<testcase name="([^"]*)" classname="([^"]*)" time="([\d.]+)"/g;
  for (const [, name, classname, time] of readFileSync(file, 'utf8').matchAll(re)) {
    timings[`${slashes(decode(classname))} › ${decode(name)}`] = Number(time);
  }
  return timings;
}

function loadTimings() {
  if (!existsSync(TIMINGS)) throw new Error(`${TIMINGS} not found - run a full test run, then: node scripts/timing-shards.mjs save`);
  return JSON.parse(readFileSync(TIMINGS, 'utf8'));
}

/** Duration of every test; a test without history (new one) gets the average. */
function durations(ids, timings) {
  const known = Object.values(timings);
  const average = known.length ? known.reduce((a, b) => a + b, 0) / known.length : 1;
  return Object.fromEntries(ids.map(id => [id, timings[id] ?? average]));
}

/** Greedy LPT: longest test first, always into the shard with the smallest total so far. */
function splitByTime(ids, duration, total) {
  const shards = Array.from({ length: total }, () => ({ tests: [], seconds: 0 }));
  const sorted = [...ids].sort((a, b) => duration[b] - duration[a] || a.localeCompare(b));
  for (const id of sorted) {
    const lightest = shards.reduce((min, s) => (s.seconds < min.seconds ? s : min));
    lightest.tests.push(id);
    lightest.seconds += duration[id];
  }
  return shards;
}

const totalArg = () => {
  const total = Number(opt('total'));
  if (!Number.isInteger(total) || total < 1) throw new Error('--total <number of shards> is required');
  return total;
};

// ------------------------------------------------------------------ commands

if (command === 'save') {
  const from = path.resolve(root, opt('from') ?? 'test-results/junit.xml');
  const timings = timingsFromJunit(from);
  const sorted = Object.fromEntries(Object.entries(timings).sort(([a], [b]) => a.localeCompare(b)));
  writeFileSync(TIMINGS, JSON.stringify(sorted, null, 2) + '\n');
  console.log(`[timings] ${Object.keys(sorted).length} tests from ${path.relative(root, from)} -> test-timings.json`);
} else if (command === 'plan') {
  const total = totalArg();
  const ids = listTests();
  const duration = durations(ids, loadTimings());
  const sum = list => list.reduce((a, id) => a + duration[id], 0);
  const byTime = splitByTime(ids, duration, total);
  console.log(`${ids.length} tests, ${sum(ids).toFixed(1)} s in total (sum of single test durations)\n`);
  console.log('shard |  --shard (by count)  |  by time');
  for (let i = 1; i <= total; i++) {
    const count = listTests(`${i}/${total}`);
    const t = byTime[i - 1];
    console.log(`  ${i}   | ${String(count.length).padStart(3)} tests ${sum(count).toFixed(1).padStart(6)} s  | ` +
      `${String(t.tests.length).padStart(3)} tests ${t.seconds.toFixed(1).padStart(6)} s`);
  }
  const worstCount = Math.max(...Array.from({ length: total }, (_, i) => sum(listTests(`${i + 1}/${total}`))));
  const worstTime = Math.max(...byTime.map(s => s.seconds));
  console.log(`\nslowest shard (= the whole run waits for it): by count ${worstCount.toFixed(1)} s, by time ${worstTime.toFixed(1)} s`);
} else if (command === 'list') {
  const total = totalArg();
  const index = Number(opt('index'));
  const out = path.resolve(root, opt('out') ?? 'shard-tests.txt');
  if (!Number.isInteger(index) || index < 1 || index > total) throw new Error(`--index must be 1..${total}`);
  const ids = listTests();
  const shard = splitByTime(ids, durations(ids, loadTimings()), total)[index - 1];
  writeFileSync(out, shard.tests.join('\n') + '\n');
  console.log(`[timings] shard ${index}/${total}: ${shard.tests.length} tests, ~${shard.seconds.toFixed(1)} s -> ${path.relative(root, out)}`);
} else {
  console.error('Usage: node scripts/timing-shards.mjs save [--from junit.xml] | plan --total N | list --index I --total N [--out file]');
  process.exit(1);
}
