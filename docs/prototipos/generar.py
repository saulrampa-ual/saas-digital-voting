# Genera los prototipos visuales estáticos de docs/prototipos (python generar.py → out/; copiar los .html aquí).
# Generates self-contained static mockups of VeriVoto (no JS logic, synthetic data).
import os, html
OUT = "out"
os.makedirs(OUT, exist_ok=True)

CSS = r"""
:root{--primary:#14365c;--primary-2:#1d4a7a;--secondary:#1f7a4d;--danger:#b3261e;--warn-bg:#fff4dc;--warn:#7a4b00;
--text:#1b2733;--muted:#5b6876;--border:#d6dde5;--bg:#f2f4f7;--card:#fff;--radius:12px;--radius-c:10px}
*{box-sizing:border-box}html,body{margin:0}
body{font-family:Inter,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;color:var(--text);background:var(--bg);font-size:15px;line-height:1.45}
a{color:var(--primary)}
.banner{background:#fff8e1;border-bottom:1px solid #f0d58a;color:#6b4e00;font-size:13px;padding:6px 16px;text-align:center}
.banner b{font-weight:700}
/* management shell */
.shell>div{min-width:0}.shell{display:grid;grid-template-columns:64px 1fr;min-height:calc(100vh - 31px)}
.rail{background:#fff;border-right:1px solid var(--border);display:flex;flex-direction:column;align-items:center;gap:6px;padding-top:0}
.rail .menu{background:var(--primary);color:#fff;width:100%;height:56px;display:flex;align-items:center;justify-content:center}
.rail .ic{width:44px;height:44px;border-radius:10px;display:flex;align-items:center;justify-content:center;color:var(--primary)}
.rail .ic.on{background:#e3e9f1}
.rail .spacer{flex:1}
.top{background:var(--primary);color:#fff;height:56px;display:flex;align-items:center;gap:12px;padding:0 16px;position:relative}
.top h1{font-size:18px;margin:0;font-weight:700}
.top .logo{position:absolute;left:50%;transform:translateX(-50%);display:flex;gap:8px;align-items:center;font-weight:700;letter-spacing:.2px}
.top .ses{margin-left:auto;text-align:right;font-size:13px;line-height:1.2}
.top .ses small{opacity:.85;display:block}
.langs{display:flex;gap:4px}
.lang{display:inline-flex;align-items:center;gap:6px;min-height:40px;padding:0 10px;border-radius:8px;border:2px solid transparent;background:rgba(255,255,255,.12);font-weight:700;font-size:13px}
.lang.on{border-color:currentColor;background:transparent}
.flag{width:22px;height:15px;border-radius:2px;display:inline-block}
.f-es{background:linear-gradient(#c60b1e 0 25%,#ffc400 25% 75%,#c60b1e 75%)}
.f-en{background:#012169;position:relative;overflow:hidden}
.f-en:before,.f-en:after{content:"";position:absolute;background:#c8102e;box-shadow:0 0 0 2px #fff}
.f-en:before{left:9px;top:0;width:4px;height:15px}.f-en:after{top:5.5px;left:0;height:4px;width:22px}
main{padding:20px;max-width:1100px;margin:0 auto;width:100%}
.intro{color:var(--muted);margin:0 0 16px}
.card{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:20px}
.card+.card{margin-top:16px}
h2{font-size:22px;margin:0 0 12px}
h3{font-size:17px;margin:0 0 8px}
.label{font-weight:600;margin:18px 0 8px;font-size:15px}
.help{color:var(--muted);font-size:14px;margin:6px 0 12px}
.field{display:flex;flex-direction:column;gap:6px;margin-bottom:14px}
.field label{font-size:14px;font-weight:600}
.input{border:1px solid #9aa6b2;border-radius:var(--radius-c);padding:12px 14px;min-height:48px;background:#fff;font:inherit;color:var(--text)}
.input.ph{color:#8a96a3}
.counter{align-self:flex-end;font-size:12px;color:var(--muted)}
.seg{display:inline-grid;grid-template-columns:1fr 1fr;gap:4px;padding:3px;border:1px solid var(--border);border-radius:var(--radius-c);background:#f7f9fb}
.seg span{padding:10px 22px;border-radius:8px;font-weight:600;color:var(--muted);text-align:center}
.seg span.on{background:#fff;box-shadow:inset 0 0 0 1.5px var(--primary);color:var(--primary)}
.choice{border:1px solid var(--border);border-radius:var(--radius);padding:12px 14px;display:flex;gap:12px;align-items:flex-start;background:#fff}
.choice.on{border:2px solid var(--primary);background:rgba(20,54,92,.04)}
.choice small{display:block;color:var(--muted)}
.radio{width:20px;height:20px;border-radius:50%;border:2px solid #7b8794;flex:none;margin-top:2px}
.on .radio{border-color:var(--primary);box-shadow:inset 0 0 0 4px #fff;background:var(--primary)}
.grid2{display:grid;gap:12px;grid-template-columns:1fr 1fr}
.btn{display:inline-flex;align-items:center;justify-content:center;gap:8px;min-height:48px;padding:0 20px;border-radius:var(--radius-c);font-weight:600;border:1px solid var(--primary);color:var(--primary);background:#fff}
.btn.primary{background:var(--primary);color:#fff}
.btn.ok{background:var(--secondary);border-color:var(--secondary);color:#fff}
.btn.block{width:100%}
.actions{display:flex;gap:12px;align-items:center;border-top:1px solid var(--border);margin:20px -20px -20px;padding:16px 20px;background:#fbfcfd;border-radius:0 0 var(--radius) var(--radius)}
.actions .sp{flex:1}
.hint{color:var(--muted);font-size:14px}
.steps{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin-top:8px}
.steps div{display:flex;flex-direction:column;gap:6px;font-size:13px;color:var(--muted)}
.bar{height:6px;border-radius:3px;background:var(--border)}
.steps .done .bar{background:var(--primary)}.steps .cur .bar{background:var(--secondary)}
.steps .cur{color:var(--text);font-weight:700}.steps .done{color:var(--text)}
.progress{margin:0;color:var(--muted)}.progress b{color:var(--text)}
.toggle{color:var(--primary);font-weight:600;display:inline-flex;gap:8px;align-items:center;margin:4px 0 10px}
.caret{width:8px;height:8px;border-right:2px solid currentColor;border-bottom:2px solid currentColor;transform:rotate(45deg);margin-bottom:3px}
.chip{font-size:12px;font-weight:600;color:var(--warn);background:var(--warn-bg);border:1px solid var(--warn);border-radius:999px;padding:0 8px}
.info svg{flex:none;margin-top:2px}.info{display:flex;gap:10px;background:#e8eef6;color:var(--primary);border-radius:var(--radius-c);padding:12px 14px;margin:12px 0}
.opt{display:grid;grid-template-columns:24px 1fr 48px;gap:8px;align-items:center;margin-bottom:8px}
.x{width:48px;height:48px;border:1px solid #d9a3a0;color:var(--danger);border-radius:var(--radius-c);display:flex;align-items:center;justify-content:center}
.check{display:flex;gap:10px;align-items:center;margin-top:12px}
.box{width:20px;height:20px;border-radius:4px;background:var(--primary);color:#fff;font-size:14px;display:flex;align-items:center;justify-content:center}
.badge{display:inline-block;font-size:12px;font-weight:700;padding:2px 10px;border-radius:999px;border:1px solid}
.b-sch{color:#14365c;background:#e8eef6;border-color:#14365c}.b-open{color:#1f7a4d;background:#e5f4ec;border-color:#1f7a4d}
.b-tal{color:#5b2a86;background:#f1eaf8;border-color:#5b2a86}.b-draft{color:#5b6876;background:#f2f4f7;border-color:#5b6876}
.kpis{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}
.kpi{background:#fff;border:1px solid var(--border);border-radius:var(--radius);padding:14px 16px}
.kpi b{display:block;font-size:26px}.kpi span{color:var(--muted);font-size:14px}
.chart{display:flex;align-items:flex-end;gap:10px;height:180px;padding:10px 4px 0;border-bottom:1px solid var(--border)}
.chart div{flex:1;background:var(--primary);border-radius:4px 4px 0 0;position:relative}
.chart div.fut{background:repeating-linear-gradient(45deg,#e3e9f1 0 6px,#f2f4f7 6px 12px)}
.chart div em{position:absolute;top:-20px;left:0;right:0;text-align:center;font-style:normal;font-size:12px;color:var(--text)}
.axis{display:flex;gap:10px;padding:6px 4px 0}.axis span{flex:1;text-align:center;font-size:12px;color:var(--muted)}
table{border-collapse:collapse;width:100%}th,td{text-align:left;padding:10px 8px;border-bottom:1px solid var(--border);font-size:14px}
th{color:var(--muted);font-weight:600}
.res{display:grid;grid-template-columns:140px 1fr 90px;gap:10px;align-items:center;margin:10px 0}
.res.sm{grid-template-columns:1fr auto;gap:4px 10px}.res.sm .track{grid-column:1/-1;grid-row:2}
.res .track{height:22px;background:#eef1f5;border-radius:6px;overflow:hidden}.res .track i{display:block;height:100%;background:var(--primary)}
.hash{font-family:ui-monospace,Consolas,monospace;font-size:13px;background:#f2f4f7;padding:4px 8px;border-radius:6px;word-break:break-all}
/* voter */
.vtop{background:var(--primary);color:#fff;height:56px;display:flex;align-items:center;padding:0 16px;gap:10px}
.vtop .brand{font-weight:700;display:flex;gap:8px;align-items:center}
.vtop .langs{margin-left:auto}
.vmain{max-width:560px;margin:0 auto;padding:20px 16px}
.vsteps{display:flex;gap:6px;margin:4px 0 6px}.vsteps i{flex:1;height:6px;border-radius:3px;background:var(--border)}
.vsteps i.d{background:var(--primary)}.vsteps i.c{background:var(--secondary)}
.status{display:flex;gap:8px;align-items:center;font-size:14px;color:var(--muted);margin:0 0 14px}
.ballot .choice{min-height:56px;align-items:center;margin-bottom:10px}
.receipt{border:2px dashed var(--primary);border-radius:var(--radius);padding:16px;text-align:center}
.receipt .code{font-family:ui-monospace,Consolas,monospace;font-size:20px;font-weight:700;letter-spacing:2px;margin:8px 0}
/* login */
.login-wrap{min-height:calc(100vh - 31px);display:grid;place-items:center;padding:24px;background:linear-gradient(180deg,#14365c 0 42%,var(--bg) 42%)}
.login{width:100%;max-width:420px;background:#fff;border-radius:16px;padding:28px;box-shadow:0 10px 30px rgba(20,54,92,.18)}
.login .brand{display:flex;gap:10px;align-items:center;font-weight:800;font-size:20px;color:var(--primary);margin-bottom:6px}
.otp{display:grid;grid-template-columns:repeat(6,1fr);gap:8px;margin:10px 0 4px}
.otp span{height:52px;border:1px solid #9aa6b2;border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:22px;font-weight:700}
/* gallery */
.gallery{display:grid;gap:18px;grid-template-columns:repeat(auto-fill,minmax(300px,1fr))}
.screen-title{font-size:13px;font-weight:700;color:var(--muted);text-transform:uppercase;letter-spacing:.6px;margin:28px 0 10px}
.phone{width:375px;max-width:100%;border:10px solid #1b2733;border-radius:32px;overflow:hidden;background:var(--bg)}
.pair{display:flex;flex-wrap:wrap;gap:24px;align-items:flex-start}.pair>div{max-width:100%;min-width:0}
.frame .shell,.frame .login-wrap,.phone .shell{min-height:0}.frame .login-wrap{padding:40px 24px}
.frame{flex:1 1 560px;border:1px solid var(--border);border-radius:12px;overflow:hidden;background:var(--bg);box-shadow:0 6px 18px rgba(0,0,0,.06)}
@media (max-width:700px){.grid2,.kpis{grid-template-columns:1fr}.top .logo,.top .ses{display:none}.shell{grid-template-columns:52px 1fr}.shell>div{min-width:0}main{padding:14px}.top{padding:0 10px}.top h1{font-size:16px}.top .lang{padding:0 7px}.top .lang .flag{display:none}.steps div{font-size:0}.steps div .bar{font-size:13px}.actions .hint{display:none}.actions{display:grid;grid-template-columns:1fr 2fr}.actions .sp{display:none}.opt{grid-template-columns:20px 1fr 48px}}
"""

ICONS = {
 'menu':'<path d="M4 7h16M4 12h16M4 17h16"/>',
 'home':'<path d="M4 11l8-7 8 7v9h-5v-6H9v6H4z"/>',
 'people':'<circle cx="9" cy="8" r="3"/><circle cx="17" cy="9" r="2.5"/><path d="M3 20c0-3.5 3-6 6-6s6 2.5 6 6M14 15c3 0 6 2 6 5"/>',
 'cal':'<rect x="4" y="5" width="16" height="15" rx="2"/><path d="M4 10h16M9 3v4M15 3v4"/>',
 'chart':'<path d="M5 20V10M10 20V5M15 20v-8M20 20v-5M3 20h18"/>',
 'check':'<rect x="4" y="4" width="16" height="16" rx="2"/><path d="M8 12l3 3 5-6"/>',
 'list':'<path d="M9 7h11M9 12h11M9 17h11M4 7h1M4 12h1M4 17h1"/>',
 'lock':'<rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 018 0v3"/>',
 'key':'<circle cx="8" cy="15" r="4"/><path d="M11 12l8-8M16 7l3 3"/>',
 'out':'<path d="M14 4h5v16h-5M10 8l-4 4 4 4M6 12h11"/>',
 'shield':'<path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/><path d="M8.5 12l2.5 2.5 4.5-5"/>',
 'info':'<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7.5v.5"/>',
 'clock':'<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
 'ok':'<circle cx="12" cy="12" r="9"/><path d="M8 12l3 3 5-6"/>',
}
def ic(name, size=22, sw=1.8):
    return f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">{ICONS[name]}</svg>'

LANGS = '<div class="langs"><span class="lang on"><i class="flag f-es"></i>ES</span><span class="lang"><i class="flag f-en"></i>EN</span></div>'
BANNER = '<div class="banner"><b>Prototipo visual · no funcional.</b> VeriVoto (TFM, UAL). Datos ficticios.</div>'

def rail(active):
    items = ['home','people','cal','chart','check','list','lock','key']
    return ('<nav class="rail" aria-label="Menú"><div class="menu">'+ic('menu')+'</div>' +
            ''.join(f'<span class="ic{" on" if i==active else ""}">{ic(i)}</span>' for i in items) +
            '<span class="spacer"></span><span class="ic" style="margin-bottom:10px">'+ic('out')+'</span></nav>')

def admin(title, active, body):
    return (f'<div class="shell">{rail(active)}<div><header class="top"><h1>{title}</h1>'
            f'<span class="logo">{ic("shield",22,2)} VeriVoto</span>'
            f'<span class="ses">Laura Gómez<small>Administración · UAL-DEMO</small></span>{LANGS}</header>'
            f'<main>{body}</main></div></div>')

def voter(body):
    return (f'<header class="vtop"><span class="brand">{ic("shield",22,2)} VeriVoto</span>{LANGS}</header>'
            f'<div class="vmain">{body}</div>')

def page(fname, title, content, extra_css=""):
    doc = f"""<!DOCTYPE html>
<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(title)} · Prototipo VeriVoto</title><style>{CSS}{extra_css}</style></head>
<body>{BANNER}{content}</body></html>"""
    open(os.path.join(OUT, fname), "w", encoding="utf-8").write(doc)

def section(t): return f'<p class="screen-title">{t}</p>'
def frame(inner): return f'<div class="frame">{inner}</div>'
def phone(inner): return f'<div class="phone">{inner}</div>'
def wrap(inner): return f'<div style="max-width:1200px;margin:0 auto;padding:8px 20px 40px">{inner}</div>'

# ---------- 1. Login ----------
login_mgmt = f'''<div class="login-wrap"><div class="login">
<div class="brand">{ic("shield",28,2)} VeriVoto</div>
<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px"><h2 style="font-size:20px;margin:0">Inicia sesión</h2>
<div class="langs" style="color:var(--primary)"><span class="lang on" style="background:none"><i class="flag f-es"></i>ES</span><span class="lang" style="background:#eef1f5"><i class="flag f-en"></i>EN</span></div></div>
<p class="help">Consola de gestión para organizaciones.</p>
<div class="field"><label>Correo electrónico</label><div class="input">laura.gomez@ual-demo.test</div></div>
<div class="field"><label>Contraseña</label><div class="input">••••••••••••</div></div>
<span class="btn primary block">Entrar</span>
<p class="help" style="text-align:center;margin-top:14px"><a href="#">¿Has olvidado la contraseña?</a></p></div></div>'''
login_code = f'''<div class="login-wrap"><div class="login">
<div class="brand">{ic("shield",28,2)} VeriVoto</div>
<h2 style="font-size:20px">Código de acceso</h2>
<p class="help">Te hemos enviado un código de 6 cifras a <b>laura.gomez@ual-demo.test</b>. Caduca en 10 minutos.</p>
<div class="otp"><span>4</span><span>8</span><span>1</span><span>2</span><span></span><span></span></div>
<p class="help">¿No te ha llegado? <a href="#">Reenviar código</a> (disponible en 60 s)</p>
<span class="btn primary block">Verificar y entrar</span></div></div>'''
v_access = voter(f'''<div class="vsteps"><i class="c"></i><i></i><i></i><i></i></div>
<p class="hint" style="margin:0 0 6px"><b style="color:var(--text)">Paso 1 de 4</b> · Acceso</p>
<p class="status">{ic("clock",18)} Abierto · cierra el 20/11/2026 a las 18:00</p>
<div class="card"><h2>Elección de delegación de curso 2026</h2>
<p class="help">Para votar te enviamos un código a tu correo. Así comprobamos que estás en el censo sin guardar tu identidad junto al voto.</p>
<div class="field"><label>Correo del censo</label><div class="input">a••••@ual-demo.test</div></div>
<span class="btn primary block">Enviarme el código</span>
<div class="info">{ic("info",20)}<span>Tu voto es secreto: la urna no guarda quién eres ni a qué hora votaste.</span></div></div>''')
page("01-login.html", "Login", wrap(
    section("Gestión · usuario y contraseña (escritorio)") + frame(login_mgmt) +
    section("Gestión · segundo paso: código por correo") + frame(login_code) +
    section("Votante · acceso con enlace personal y código (móvil)") + phone(v_access)))

# ---------- 2. Creación de proceso (4 pasos) ----------
def wizard(n, name, nextname, body, last=False):
    names = ["Datos básicos","Calendario","Censo","Papeleta"]
    steps = ''.join(f'<div class="{"done" if i<n-1 else "cur" if i==n-1 else ""}"><span class="bar"></span>{i+1} · {s}</div>' for i,s in enumerate(names))
    act = ('<span class="btn">Atrás</span><span class="sp"></span>' +
           (f'<span class="hint">Siguiente: {nextname}</span><span class="btn primary">Siguiente →</span>' if not last else '<span class="btn ok">Crear proceso</span>'))
    return admin("Nuevo proceso", "cal", f'''<p class="intro">El proceso se guarda como Borrador hasta que lo programes.</p>
<div class="card" style="margin-bottom:16px"><p class="progress"><b>Paso {n} de 4</b> · {name}</p><div class="steps">{steps}</div></div>
<div class="card">{body}<div class="actions">{act}</div></div>''')
s1 = wizard(1, "Datos básicos", "Calendario", '''<h2>Datos básicos</h2>
<p class="label">Idioma en el que escribes los textos del proceso</p><div class="seg"><span class="on">Español</span><span>Inglés</span></div>
<p class="help">Escribe cada texto una sola vez. Al salir de cada campo se traduce solo al otro idioma; si quieres, puedes revisar la traducción.</p>
<span class="toggle"><span class="caret"></span>Revisar la traducción al inglés <span class="chip">se traducirá</span></span>
<div class="field"><label>Título *</label><div class="input">Elección de delegación de curso 2026</div><span class="counter">36 / 150</span></div>
<div class="field"><label>Descripción</label><div class="input" style="min-height:84px">Elige a la persona que representará al curso durante 2026-2027.</div></div>
<p class="label">Cuando termine la votación, ¿quién podrá ver el resultado?</p>
<div class="grid2"><div class="choice on"><span class="radio"></span><span><b>Solo las personas del censo</b><small>Con su enlace personal.</small></span></div>
<div class="choice"><span class="radio"></span><span><b>Cualquiera con el enlace público</b><small>Útil para publicarlo en la web.</small></span></div></div>''')
s2 = wizard(2, "Calendario", "Censo", f'''<h2>Calendario</h2>
<p class="help">Por ejemplo: abre el lunes a las 9:00 y cierra el viernes a las 14:00.</p>
<div class="grid2"><div class="field"><label>Apertura *</label><div class="input">16/11/2026 10:00</div></div>
<div class="field"><label>Cierre *</label><div class="input">20/11/2026 18:00</div></div></div>
<p style="font-weight:600;margin:0">La votación estará abierta 4 d 8 h</p>
<div class="info">{ic("clock",20)}<span>El proceso se abre y se cierra solo a esas horas. Hasta que lo programes puedes cambiar las fechas.</span></div>''')
s3 = wizard(3, "Censo", "Papeleta", '''<h2>Censo</h2>
<p class="help">Es el único censo de tu organización: lo hemos seleccionado por ti.</p>
<div class="choice on"><span class="radio"></span><span><b>Estudiantes de 4.º curso</b><small>128 personas</small></span></div>
<p class="label">¿Cómo accederán las personas votantes?</p>
<div class="grid2"><div class="choice on"><span class="radio"></span><span><b>Código por correo</b><small>Reciben un código de 6 cifras con su enlace.</small></span></div>
<div class="choice"><span class="radio"></span><span><b>Cuenta institucional</b><small>Entran con la cuenta de la universidad.</small></span></div></div>''')
s4 = wizard(4, "Papeleta", "", f'''<h2>Papeleta</h2>
<p class="label">Tipo de papeleta</p>
<div class="grid2"><div class="choice on"><span class="radio"></span><span><b>Opción única</b><small>Se elige una candidatura u opción.</small></span></div>
<div class="choice"><span class="radio"></span><span><b>Sí / No / Abstención</b><small>Para consultas.</small></span></div></div>
<span class="toggle" style="margin-top:14px"><span class="caret"></span>Revisar la traducción al inglés <span class="chip" style="color:#14365c;background:#e8eef6;border-color:#14365c">traducción automática</span></span>
<div class="field"><label>Pregunta *</label><div class="input">¿A quién eliges como delegado o delegada?</div></div>
<p class="label">Opciones de la papeleta</p>
<div class="opt"><b>1</b><div class="input">Ana Martín</div><span class="x">✕</span></div>
<div class="opt"><b>2</b><div class="input">Luis Ortega</div><span class="x">✕</span></div>
<div class="opt"><b>3</b><div class="input">Sara Núñez</div><span class="x">✕</span></div>
<span class="btn">+ Añadir opción</span>
<div class="check"><span class="box">✓</span>Habilitar voto en blanco</div>''', last=True)
done = admin("Nuevo proceso", "cal", f'''<div class="card" style="text-align:center;padding:40px 20px"><span style="color:var(--secondary)">{ic("ok",64,1.6)}</span>
<h2>Proceso creado</h2><p class="help">«Elección de delegación de curso 2026» está en Borrador. Revísalo y prográmalo para que se abra en la fecha prevista.</p>
<span class="btn primary">Ver el proceso</span> <span class="btn">Ir a la lista</span></div>''')
page("02-crear-proceso.html", "Creación de proceso", wrap(
    section("Paso 1 de 4 · Datos básicos") + frame(s1) + section("Paso 2 de 4 · Calendario") + frame(s2) +
    section("Paso 3 de 4 · Censo") + frame(s3) + section("Paso 4 de 4 · Papeleta") + frame(s4) +
    section("Resultado · proceso creado (después se programa desde su ficha)") + frame(done)))

# ---------- 3. Votación ----------
def vstep(n, name):
    bars = ''.join(f'<i class="{"d" if i<n-1 else "c" if i==n-1 else ""}"></i>' for i in range(4))
    return f'<div class="vsteps">{bars}</div><p class="hint" style="margin:0 0 6px"><b style="color:var(--text)">Paso {n} de 4</b> · {name}</p>'
status = f'<p class="status">{ic("clock",18)} Abierto · cierra el 20/11/2026 a las 18:00</p>'
ballot = voter(vstep(2,"Papeleta") + status + '''<div class="card ballot"><h2>¿A quién eliges como delegado o delegada?</h2>
<p class="help">Elige una opción. Podrás revisarla antes de confirmar.</p>
<div class="choice"><span class="radio"></span><b>Ana Martín</b></div>
<div class="choice on"><span class="radio"></span><b>Luis Ortega</b></div>
<div class="choice"><span class="radio"></span><b>Sara Núñez</b></div>
<div class="choice"><span class="radio"></span><span><b>Voto en blanco</b><small>No eliges a ninguna opción.</small></span></div>
<span class="btn primary block" style="margin-top:8px">Revisar mi voto</span></div>''')
confirm = voter(vstep(3,"Confirmación") + status + f'''<div class="card"><h2>Revisa tu voto</h2>
<p class="help">Vas a votar a:</p><div class="choice on" style="min-height:60px;align-items:center"><span class="radio"></span><b style="font-size:18px">Luis Ortega</b></div>
<div class="info" style="background:#fff4dc;color:#6b4e00">{ic("info",20)}<span>Una vez confirmado <b>no podrás cambiarlo</b>.</span></div>
<span class="btn ok block" style="min-height:52px">Confirmar y votar</span>
<span class="btn block" style="margin-top:10px">Volver y cambiar</span></div>''')
receipt = voter(vstep(4,"Recibo") + f'''<div class="card" style="text-align:center"><span style="color:var(--secondary)">{ic("ok",56,1.6)}</span>
<h2>Tu voto se ha registrado</h2><p class="help">Guarda este recibo. Cuando se publique el resultado podrás comprobar que tu voto consta en la urna.</p>
<div class="receipt"><span class="hint">Código de recibo</span><div class="code">7KQ4-M2XD-9PRA</div><span class="btn">Copiar</span> <span class="btn">Descargar</span></div>
<p class="help" style="margin-top:14px">El recibo demuestra que tu voto está en la urna; no muestra a quién votaste.</p></div>''')
page("03-votacion.html", "Votación", wrap(
    '<div class="pair">' + '<div>'+section("Papeleta")+phone(ballot)+'</div>' + '<div>'+section("Confirmación")+phone(confirm)+'</div>' +
    '<div>'+section("Recibo")+phone(receipt)+'</div>' + '</div>'))

# ---------- 4. Resultados ----------
rows = [("Luis Ortega",52,"41,3 %"),("Ana Martín",41,"32,5 %"),("Sara Núñez",27,"21,4 %"),("En blanco",6,"4,8 %")]
bars = ''.join(f'<div class="res"><b>{n}</b><div class="track"><i style="width:{v/52*100:.0f}%"></i></div><span>{v} · {p}</span></div>' for n,v,p in rows)
results_admin = admin("Resultados", "check", f'''<div class="card"><div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:8px;align-items:center">
<h2 style="margin:0">Elección de delegación de curso 2026</h2><span class="badge b-tal">Escrutado</span></div>
<p class="help">Cerrado el 20/11/2026 a las 18:00 · escrutinio automático tras verificar la cadena de la urna.</p>
<div class="kpis" style="margin:12px 0 16px"><div class="kpi"><b>126</b><span>votos en la urna</span></div><div class="kpi"><b>128</b><span>personas en el censo</span></div><div class="kpi"><b>98,4 %</b><span>participación</span></div></div>
<h3>Resultado</h3>{bars}
<div class="info">{ic("shield",20)}<span><b>Cadena de la urna íntegra.</b> Huella final del registro: <span class="hash">9f3a…c27e</span></span></div>
<div style="display:flex;gap:10px;flex-wrap:wrap"><span class="btn primary">Descargar acta</span><span class="btn">Exportar CSV</span><span class="btn">Ver registro de auditoría</span></div></div>''')
results_voter = voter(f'''<div class="card"><h2>Resultado</h2><p class="help">Elección de delegación de curso 2026 · publicado el 20/11/2026</p>{bars.replace('class="res"','class="res sm"')}
<h3 style="margin-top:16px">Comprueba tu recibo</h3><div class="field"><label>Código de recibo</label><div class="input">7KQ4-M2XD-9PRA</div></div>
<span class="btn primary block">Comprobar</span>
<div class="info" style="background:#e5f4ec;color:#1f7a4d">{ic("ok",20)}<span><b>Tu voto consta en la urna</b> y se ha contado.</span></div></div>''')
page("04-resultados.html", "Resultados", wrap(
    section("Administración · resultado, integridad y acta") + frame(results_admin) +
    section("Votante · resultado y comprobación del recibo (móvil)") + phone(results_voter)))

# ---------- 5. Participación ----------
days = [("Lun 16",31),("Mar 17",44),("Mié 18",22),("Jue 19",0),("Vie 20",0)]
mx = 44
chart = ''.join(f'<div class="{"fut" if v==0 else ""}" style="height:{max(v/mx*100,6):.0f}%">{"<em>"+str(v)+"</em>" if v else ""}</div>' for _,v in days)
axis = ''.join(f'<span>{d}</span>' for d,_ in days)
part = admin("Participación", "chart", f'''<div class="card"><div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:8px;align-items:center">
<h2 style="margin:0">Elección de delegación de curso 2026</h2><span class="badge b-open">Abierto</span></div>
<p class="help">Abierto del 16/11 10:00 al 20/11 18:00 · datos actualizados a las 12:40. Solo se muestra cuántas personas han votado cada día, nunca qué han votado.</p>
<div class="kpis" style="margin:12px 0 18px"><div class="kpi"><b>97</b><span>han votado</span></div><div class="kpi"><b>31</b><span>pendientes</span></div><div class="kpi"><b>75,8 %</b><span>participación</span></div></div>
<h3>Votos por día</h3><div class="chart">{chart}</div><div class="axis">{axis}</div>
<p class="help">Los días futuros aparecen rayados.</p></div>
<div class="card"><h3>Recordatorios</h3><p class="help">Avisos por correo enviados a las personas del censo. El correo nunca incluye información sobre el voto.</p>
<table><thead><tr><th>Envío</th><th>Destinatarios</th><th>Estado</th></tr></thead><tbody>
<tr><td>Apertura · 16/11 10:00</td><td>128</td><td>Enviado</td></tr><tr><td>Recordatorio · 18/11 09:00</td><td>128</td><td>Enviado</td></tr></tbody></table>
<div style="margin-top:12px"><span class="btn primary">Enviar recordatorio ahora</span></div></div>''')
page("05-participacion.html", "Participación", wrap(section("Administración · panel de participación (escritorio)") + frame(part)))

# ---------- index ----------
cards = [("01-login.html","Login","Gestión (contraseña y código) y acceso del votante."),
         ("02-crear-proceso.html","Creación de proceso","Asistente de 4 pasos y confirmación."),
         ("03-votacion.html","Votación","Papeleta, confirmación y recibo (móvil)."),
         ("04-resultados.html","Resultados","Vista de administración y del votante."),
         ("05-participacion.html","Participación","Panel de seguimiento de la administración.")]
idx = wrap('<h1 style="margin:24px 0 4px">VeriVoto · prototipos de pantallas</h1>'
  '<p class="intro">Trabajo Fin de Máster · Saul David Ramos Pacheco · Universidad de Almería. Mockups estáticos, sin lógica; los datos son ficticios. Cada fichero se abre directamente en el navegador.</p>'
  '<div class="gallery">' + ''.join(f'<a class="card" style="text-decoration:none;color:inherit" href="{f}"><h3>{t}</h3><p class="help" style="margin:0">{d}</p></a>' for f,t,d in cards) + '</div>')
page("index.html", "Índice", idx)
print("ok", sorted(os.listdir(OUT)))
