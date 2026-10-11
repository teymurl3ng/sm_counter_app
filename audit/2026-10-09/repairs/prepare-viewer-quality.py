from pathlib import Path
root=Path('C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer')
patch=['*** Begin Patch']
patch_index = 0
def edit(name, transform):
    global patch_index
    file=root/name
    old=file.read_text(encoding='utf-8')
    new=transform(old)
    assert old!=new, name
    import difflib
    hunk = list(difflib.unified_diff(old.splitlines(), new.splitlines(), lineterm=''))[2:]
    hunk = ['@@' if line.startswith('@@') else line for line in hunk]
    part = ['*** Begin Patch', '*** Update File: '+file.as_posix(), *hunk, '*** End Patch']
    Path(__file__).with_name(f'viewer-quality-{patch_index:02}.patch.txt').write_text('\n'.join(part)+'\n',encoding='utf-8')
    patch_index += 1

def cache(text):
    text=text.replace('type CacheEntry = {','type CacheEntry = {\n  url: string;')
    text=text.replace('for (const key of store.keys()) {\n    const path = key.slice(key.indexOf("::") + 2);', 'for (const [key, entry] of store) {\n    const path = entry.url;')
    return text.replace('store.set(k, {\n      status:', 'store.set(k, {\n      url,\n      status:')
edit('src/lib/viewer-cache.ts',cache)

panels=['activity/activity-list-panel.tsx','overview/overview-panel.tsx','shops/[shopId]/page.tsx','shops/shops-list-panel.tsx','staff/[staffId]/page.tsx','staff/staff-list-panel.tsx','system/system-panel.tsx']
def panel(text):
    text=text.replace('const load = useCallback(async () => {', 'const load = useCallback(async (isCurrent: () => boolean = () => true) => {')
    # Ignore late results from a previous route/query or unmounted component.
    import re
    text=re.sub(r'(const apply = \(status: number, json: \w+\) => \{)',r'\1\n      if (!isCurrent()) return;',text)
    text=text.replace('    } catch {\n      setData(', '    } catch {\n      if (!isCurrent()) return;\n      setData(')
    text=text.replace('    void load();\n  }, [load', '''    let disposed = false;
    // Subscribe after commit; cleanup fences cache and network completions alike.
    queueMicrotask(() => { if (!disposed) void load(() => !disposed); });
    return () => { disposed = true; };
  }, [load''')
    text=text.replace('  useEffect(() => {\n    setSearchInput(urlSearch);\n  }, [urlSearch]);', '''  const [previousSearch, setPreviousSearch] = useState(urlSearch);
  if (previousSearch !== urlSearch) {
    setPreviousSearch(urlSearch);
    setSearchInput(urlSearch);
  }''')
    text=text.replace('''  useEffect(() => {
    setDraftTarget(target);
    setDraftFrom(from);
    setDraftTo(to);
  }, [target, from, to]);''', '''  const filterIdentity = `${target}|${from}|${to}`;
  const [previousFilters, setPreviousFilters] = useState(filterIdentity);
  if (previousFilters !== filterIdentity) {
    setPreviousFilters(filterIdentity);
    setDraftTarget(target);
    setDraftFrom(from);
    setDraftTo(to);
  }''')
    text=text.replace('catch (e)', 'catch')
    if '(s: any)' in text:
        text=text.replace('const json = await res.json();\n      if (!json.ok || !json.shops)', 'const json: { ok?: boolean; shops?: { shop_id: string; name: string }[] } = await res.json();\n      if (!json.ok || !json.shops)')
        text=text.replace('(s: any)', '(s)').replace('(assigned: any)', '(assigned)').replace('(s: any, i: number)', '(s, i)')
    return text
for name in panels: edit('src/app/(app)/'+name,panel)
edit('src/app/(app)/staff/create/page.tsx',lambda text:text.replace('catch (e)','catch'))
edit('src/lib/admin-mutation.ts',lambda text:text.replace('getAccountServerConfig, getAdminKeyConfig','getAccountServerConfig'))
edit('src/lib/perf.ts',lambda text:text.replace('// intentionally empty','void _marks; // intentionally empty'))
def theme(text):
    text=text.replace('useEffect, useState','useEffect, useState, useSyncExternalStore')
    text=text.replace('export function ThemeToggle', '''const subscribeMounted = () => () => {};
const clientMounted = () => true;
const serverMounted = () => false;

export function ThemeToggle''')
    text=text.replace('''  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setTheme(getInitialTheme());
    setMounted(true);
  }, []);''', '''  const mounted = useSyncExternalStore(subscribeMounted, clientMounted, serverMounted);
  const [initialized, setInitialized] = useState(false);
  if (mounted && !initialized) {
    setInitialized(true);
    setTheme(getInitialTheme());
  }''')
    return text
edit('src/app/theme-toggle.tsx',theme)
patch.append('*** End Patch')
Path(__file__).with_name('viewer-quality.patch.txt').write_text('\n'.join(patch)+'\n',encoding='utf-8')
