# /// script
# dependencies = ["qai-hub"]
# ///
import os, pathlib, glob

tok = os.environ["QAI_HUB_API_TOKEN"]
cfg = pathlib.Path.home() / ".qai_hub"
cfg.mkdir(exist_ok=True)
(cfg / "client.ini").write_text(
    f"[api]\napi_token = {tok}\napi_url = https://app.aihub.qualcomm.com\n"
    "web_url = https://app.aihub.qualcomm.com\nverbose = False\n"
)

import qai_hub as hub

for jid in ["jpeloqlog", "jp0v4vm2g"]:
    print(f"\n{'='*70}\nJOB {jid}\n{'='*70}")
    j = hub.get_job(jid)
    s = j.get_status()
    print("state:", s.code)
    print("failure_reason:", s.message)
    try:
        out = f"/tmp/logs_{jid}"
        os.makedirs(out, exist_ok=True)
        p = j.download_job_logs(out)
        print("log file:", p)
        for f in glob.glob(out + "/**/*", recursive=True):
            if os.path.isfile(f):
                txt = open(f, errors="replace").read()
                print(f"\n----- {f} (last 8000 chars) -----")
                print(txt[-8000:])
    except Exception as e:
        print("log download failed:", repr(e))
