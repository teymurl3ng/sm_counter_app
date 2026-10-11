import subprocess, pathlib, queue, threading, time, json
adb=r"C:\Users\Teymur\AppData\Local\android\platform-tools\adb.exe"
serial="R8VXA02SNKH"
out=pathlib.Path(__file__).resolve().parent
remote="/sdcard/Download/phase14-native-evidence/native-motion-a06"
proc=subprocess.Popen([adb,"-s",serial,"logcat","-T","1","-v","time","-s","TestRunner:I"],stdout=subprocess.PIPE,text=True,encoding="utf-8",errors="replace")
q=queue.Queue()
def reader():
 for line in proc.stdout:q.put(line)
threading.Thread(target=reader,daemon=True).start()
deadline=time.monotonic()+2100
try:
 while time.monotonic()<deadline:
  try:line=q.get(timeout=10)
  except queue.Empty:continue
  if "started: uxRepeatedOwnerEntriesKeepChromeAndCaptureScreens" in line:
   print("Capturing known ReceiveMilk/Deliveries re-entry route.",flush=True)
   (out/"motion-trigger.txt").write_text(line,encoding="utf-8")
   proc.terminate()
   subprocess.run([adb,"-s",serial,"shell","mkdir","-p",remote],check=True)
   subprocess.run([adb,"-s",serial,"shell","screenrecord","--time-limit","60","--bit-rate","2000000",remote+"/known-reentry-route.mp4"],check=True)
   subprocess.run([adb,"-s",serial,"pull",remote+"/known-reentry-route.mp4",str(out/"known-reentry-route.mp4")],check=True)
   (out/"known-reentry-route.review.json").write_text(json.dumps({"scenario":"known-reentry-route","reviewStatus":"UNREVIEWED","device":serial,"note":"Current-run A06 route recording, triggered at exact test start; inspect before motion acceptance."},indent=2),encoding="utf-8")
   print("Finalized route recording retained.",flush=True)
   break
 else:print("No matching route observed within bounded deadline.",flush=True)
finally:
 if proc.poll() is None:proc.terminate()
