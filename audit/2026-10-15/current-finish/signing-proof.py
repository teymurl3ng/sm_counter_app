from pathlib import Path
import json, subprocess
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
java='C:/Program Files/Android/Android Studio/jbr/bin/java.exe'
result=subprocess.run([java,str(OUT/'SigningProof.java'),str(ROOT/'phase18')],capture_output=True,text=True,timeout=45)
assert result.returncode==0,'Protected signing-key proof failed; secret-bearing output withheld'
record=json.loads(result.stdout)
assert record['signingChallengeVerified'] and record['certificateSha256']=='9c9e03d7969d79018e2880b79b8d9ec46c43ab50eaf30512ee3fb989bc6abfa1'
(OUT/'signing-proof.json').write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps(record,indent=2))
