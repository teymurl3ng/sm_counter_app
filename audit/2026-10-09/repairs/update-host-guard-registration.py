from pathlib import Path
root=Path(__file__).resolve().parents[3]
for role,parameter in [('owner','workDestination'),('admin','destination'),('manager','destination'),('operations','destination')]:
    file=root/f'phase18/app/src/main/java/com/saaf/milkshop/nativeui/{role}/{role.title()}PreviewHost.kt'
    text=file.read_text(encoding='utf-8')
    marker=f'        ) {{ {parameter} ->\n        when'
    assert marker in text
    text=text.replace(marker,f'        ) {{ {parameter} ->\n        val registerPaneGuard = com.saaf.milkshop.nativeui.common.rememberPaneGuardRegistration(activeEntryGuard) {{ activeEntryGuard = it }}\n        when',1)
    text=text.replace('onRegisterGuard = { activeEntryGuard = it }','onRegisterGuard = registerPaneGuard')
    file.write_text(text,encoding='utf-8')
