// Offline reproduction of identity-dr.yml's checkout sequence; no remote access.
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const base = fs.realpathSync(os.tmpdir());
const root = fs.mkdtempSync(path.join(base, 'saaf-audit-dr-'));
function git(...args) {
  const result = spawnSync('git', args, { cwd: root, encoding: 'utf8' });
  return { status: result.status, output: result.stdout + result.stderr };
}
function must(...args) {
  const result = git(...args);
  if (result.status !== 0) throw new Error(result.output);
}
try {
  must('init', '-b', 'main');
  must('config', 'user.name', 'Offline Audit');
  must('config', 'user.email', 'audit@example.invalid');
  fs.writeFileSync(path.join(root, 'source.txt'), 'fixture only\n');
  must('add', '-A'); must('commit', '-m', 'fixture');
  must('switch', '-c', 'identity-backups');
  fs.writeFileSync(path.join(root, '.snapshot_dir'), '2026-10-09\n');
  must('add', '-A'); must('commit', '-m', 'first snapshot');
  must('switch', 'main');
  fs.writeFileSync(path.join(root, '.snapshot_dir'), '2026-10-16\n');
  const result = git('checkout', '--detach', 'identity-backups');
  console.log(JSON.stringify({ probe: 'second-scheduled-snapshot-checkout', ...result, pass: result.status === 0 }, null, 2));
  process.exitCode = result.status === 0 ? 0 : 1;
} finally {
  // Delete only the exact temporary fixture created above, after containment check.
  const resolved = fs.realpathSync(root);
  if (path.dirname(resolved) !== base || !path.basename(resolved).startsWith('saaf-audit-dr-')) {
    throw new Error('Unsafe fixture cleanup path');
  }
  fs.rmSync(resolved, { recursive: true, force: true });
}
