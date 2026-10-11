// Offline audit probes: load the real Viewer module, with a fake network.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const viewer = process.argv[2];
if (!viewer) throw new Error('Pass the Admin Viewer checkout path');
const ts = require(path.join(viewer, 'node_modules/typescript'));
const source = fs.readFileSync(path.join(viewer, 'src/lib/viewer-cache.ts'), 'utf8');
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText;
let calls = 0;
let version = 'before';
const exportsObject = {};
const context = { exports: exportsObject, Date, Map, Promise, fetch: async () => {
  calls++;
  return { status: 200, json: async () => ({ version }) };
}};
vm.runInNewContext(compiled, context, { filename: 'viewer-cache.ts' });
(async () => {
  const api = exportsObject;
  api.setViewerCacheSession('audit-user');
  const request = { key: '/api/staff', url: '/api/staff', ttlMs: 10000 };
  await api.viewerCachedGet(request);
  version = 'after';
  api.viewerCacheInvalidate(['/api/staff']);
  const result = await api.viewerCachedGet(request);
  console.log(JSON.stringify({ probe: 'invalidate-after-mutation', calls, actual: result, expectedVersion: 'after', pass: result.body.version === 'after' }, null, 2));
  process.exitCode = result.body.version === 'after' ? 0 : 1;
})().catch(error => { console.error(error); process.exitCode = 2; });
