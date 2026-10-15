"""Loopback-only design study; serve only the preview and three existing bundled fonts."""
from pathlib import Path
from http.server import BaseHTTPRequestHandler, HTTPServer
import json, os
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
allowed={'/':(HERE/'UI_PREVIEW.html','text/html; charset=utf-8'),'/UI_PREVIEW.html':(HERE/'UI_PREVIEW.html','text/html; charset=utf-8')}
for weight in ['regular','semibold','bold']:
    name='manrope_'+weight+'.ttf'
    allowed['/phase18/app/src/main/res/font/'+name]=(ROOT/'phase18/app/src/main/res/font'/name,'font/ttf')
class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        item=allowed.get(self.path.split('?')[0])
        if item is None:
            self.send_error(404); return
        body=item[0].read_bytes()
        self.send_response(200)
        self.send_header('Content-Type',item[1]);self.send_header('Content-Length',str(len(body)))
        self.send_header('Cache-Control','no-store');self.end_headers();self.wfile.write(body)
    def log_message(self,*args): pass
server=HTTPServer(('127.0.0.1',0),Handler)
(HERE/'preview-server-local.json').write_text(json.dumps({'port':server.server_port,'pid':os.getpid()}))
server.serve_forever()
