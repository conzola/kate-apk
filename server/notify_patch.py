# Add to ESP/on-pc/server.py (FastAPI bridge). Names marked ADAPT depend on how server.py
# already queues timer/automation announcements for /announce - I only know it from my notes.
from pydantic import BaseModel

class Note(BaseModel):
    app: str
    title: str = ""
    text: str = ""

@app.post("/notify")
async def notify(n: Note):
    line = f"{n.app}. {n.title}. {n.text}".strip()[:240]
    announce_queue.put_nowait(line)      # ADAPT: whatever feeds /announce (synthesised by Kokoro)
    return {"ok": True}
