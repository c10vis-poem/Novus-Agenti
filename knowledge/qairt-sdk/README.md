# knowledge/qairt-sdk/ — Qualcomm AI Runtime (QAIRT) SDK reference

Text extracted from the operator's `#QAIRT/` Google Drive capture of the
official **Qualcomm AI Runtime (QAIRT) SDK** manual (saved doc pages, `.mht`).
This is Qualcomm's public documentation — the readable text was pulled out of
each captured page and site nav/chrome stripped; a little boilerplate may
remain. Raw `.mht`/`.pdf` captures stay in Drive.

**Why it's here:** QAIRT is **GenieX's NPU backend** (the Qualcomm AI Engine
Direct path). These chapters are the integration reference for the
GenieX-on-HTP-SDK runtime — see `wiki/GENIEX-DAEMON-PLAN.md`.

| File | Chapter |
|---|---|
| `overview.md` | QAIRT SDK overview + API map |
| `context.md`  | QairtContext — context creation/config |
| `backend.md`  | QairtBackend — backend registration/config |
| `api.md`      | QAIRT API surface |
| `graph.md`    | QairtGraph — graph build/execute |
| `tensor.md`   | QairtTensor — tensor types/layout |
| `htp.md`      | **HTP** — Hexagon backend: op packages, HTP API usage, optimization utils (largest; the Hexagon-specific reference) |

Extraction is lossy vs. the original rendered pages (tables/images dropped to
text). For anything ambiguous, the source `.mht`/`.pdf` in Drive `#QAIRT/` is
ground truth.
