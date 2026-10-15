from pathlib import Path
import hashlib, json, urllib.request, urllib.error
from playwright.sync_api import sync_playwright

OUT = Path(__file__).resolve().parent
def env(path):
    result = {}
    for line in Path(path).read_text(encoding='utf-8').splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            key, value = line.split('=', 1)
            result[key.strip()] = value.strip().strip('"').strip("'")
    return result

values = env('C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer/.env.production')
checks = {
    'exactProd': values.get('ACCOUNT_SERVER_URL', '').rstrip('/') == 'https://tlsuxbghuhlqntkgtrxt.supabase.co',
    'envProd': values.get('ACCOUNT_SERVER_ENV') == 'prod',
    'adminKeyPresent': bool(values.get('ACCOUNT_SERVER_ADMIN_KEY')),
    'publishableKeyPresent': bool(values.get('ACCOUNT_SERVER_PUBLISHABLE_KEY')),
}
print(json.dumps({'cachedConfig': checks}), flush=True)
with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    response = page.goto('https://sm-server-view.vercel.app/login', wait_until='networkidle', timeout=45000)
    checks['browserStatus'] = response.status
    checks['loginText'] = page.locator('body').inner_text()[:1800]
    checks['inputs'] = page.locator('input').evaluate_all('(xs) => xs.map(x => ({type:x.type,id:x.id,name:x.name,placeholder:x.placeholder}))')
    page.screenshot(path=str(OUT/'production-login.png'))
    browser.close()
(OUT/'preflight.json').write_text(json.dumps(checks, indent=2)+'\n', encoding='utf-8')
print(json.dumps(checks, indent=2))
