from pathlib import Path
import subprocess,os
out=Path(__file__).resolve().parent
root=out.parents[2]
env=dict(os.environ,JAVA_HOME="C:/Program Files/Android/Android Studio/jbr")
env["PATH"]=env["JAVA_HOME"]+"/bin;"+env["PATH"]
with (out/"qa-build-console.txt").open("w",encoding="utf-8") as log:
 result=subprocess.run([str(root/"phase18/gradlew.bat"),":app:assembleDebug",":app:assembleDebugAndroidTest","-Psaaf.instrumentationSandbox=true","--console=plain","--no-daemon","-Pkotlin.compiler.execution.strategy=in-process"],cwd=root/"phase18",env=env,stdout=log,stderr=subprocess.STDOUT)
print("Isolated QA build exit:",result.returncode)
raise SystemExit(result.returncode)
