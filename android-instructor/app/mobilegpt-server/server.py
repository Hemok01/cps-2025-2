from flask import Flask, request, jsonify
import os, json, time
from datetime import datetime

from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()
client = OpenAI()

app = Flask(__name__)

# ---------------------------
# 경로 설정 (상대 경로 사용)
# ---------------------------
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
SAVE_DIR = os.path.join(BASE_DIR, "sessions")
CURRICULUM_DIR = os.path.join(BASE_DIR, "curriculum")

os.makedirs(SAVE_DIR, exist_ok=True)
os.makedirs(CURRICULUM_DIR, exist_ok=True)

recorded_events = []

# 필요한 필드
ESSENTIAL_FIELDS = [
    "time",
    "eventType",
    "package",
    "className",
    "text",
    "contentDescription",
    "viewId",
    "bounds"
]

# ================================================================
# 1) 이벤트 수신
# ================================================================
@app.route("/api/record_event", methods=["POST"])
def record_event():
    try:
        event = request.get_json()
        if not event:
            return jsonify({"error": "no event"}), 400

        event["server_time"] = int(time.time() * 1000)
        recorded_events.append(event)

        print(f"🟢 이벤트 수신됨 {len(recorded_events)}개")
        return jsonify({"status": "ok"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ================================================================
# 2) 세션 저장
# ================================================================
@app.route("/api/save_session", methods=["POST"])
def save_session():
    try:
        if not recorded_events:
            return jsonify({"error": "no recorded events"}), 400

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"session_{timestamp}.json"
        path = os.path.join(SAVE_DIR, filename)

        with open(path, "w", encoding="utf-8") as f:
            json.dump(recorded_events, f, ensure_ascii=False, indent=2)

        print(f"💾 세션 저장 완료 → {path}")

        recorded_events.clear()

        return jsonify({"status": "saved", "file": filename})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ================================================================
# 3) GPT 분석 → Step JSON 생성
# ================================================================
@app.route("/api/analyze_session", methods=["POST"])
def analyze_session():
    try:
        req = request.get_json()
        filename = req.get("file")

        if not filename:
            return jsonify({"error": "filename missing"}), 400

        file_path = os.path.join(SAVE_DIR, filename)
        if not os.path.exists(file_path):
            return jsonify({"error": "file not found"}), 404

        with open(file_path, "r", encoding="utf-8") as f:
            full_events = json.load(f)

        minimized = [
            {k: ev.get(k) for k in ESSENTIAL_FIELDS}
            for ev in full_events
        ]

        prompt = f"""
너는 반드시 JSON 배열만 출력해야 한다.

각 JSON은 다음 필드를 포함해야 한다:
step, title, description, time, eventType, package, className, text, contentDescription, viewId, bounds

아래 이벤트 로그를 보고 단계를 생성하라:
{json.dumps(minimized, ensure_ascii=False, indent=2)}
"""

        gpt_res = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.0
        )

        text = gpt_res.choices[0].message.content.strip()

        if text.startswith("```"):
            text = text.replace("```json", "").replace("```", "").strip()

        steps = json.loads(text)

        session_id = filename.replace(".json", "")
        out_dir = os.path.join(CURRICULUM_DIR, session_id)
        os.makedirs(out_dir, exist_ok=True)

        saved = []

        for s in steps:
            fp = os.path.join(out_dir, f"step{s['step']}.json")
            with open(fp, "w", encoding="utf-8") as f:
                json.dump(s, f, ensure_ascii=False, indent=2)
            saved.append(fp)

        return jsonify({"status": "ok", "files": saved})

    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ================================================================
# 4) 세션 목록 조회
# ================================================================
@app.route("/api/list_sessions", methods=["GET"])
def list_sessions():
    try:
        sessions = [
            name for name in os.listdir(CURRICULUM_DIR)
            if os.path.isdir(os.path.join(CURRICULUM_DIR, name))
        ]
        return jsonify({"sessions": sessions})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ================================================================
# 5) step.json 목록 불러오기
# ================================================================
@app.route("/api/get_steps/<session_id>", methods=["GET"])
def get_steps(session_id):
    try:
        target = os.path.join(CURRICULUM_DIR, session_id)
        if not os.path.exists(target):
            return jsonify({"error": "session not found"}), 404

        steps = []
        for file in sorted(os.listdir(target)):
            if file.endswith(".json"):
                with open(os.path.join(target, file), "r", encoding="utf-8") as f:
                    steps.append(json.load(f))

        return jsonify({"steps": steps})

    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ================================================================
# 6) Step 전체 업데이트
# ================================================================
@app.route("/api/update_steps", methods=["POST"])
def update_steps():
    try:
        data = request.get_json()

        session_id = data.get("session_id")
        steps = data.get("steps")

        if not session_id or steps is None:
            return jsonify({"error": "missing fields"}), 400

        target = os.path.join(CURRICULUM_DIR, session_id)
        if not os.path.exists(target):
            return jsonify({"error": "session not found"}), 404

        # 기존 step 파일 삭제
        for file in os.listdir(target):
            if file.endswith(".json"):
                os.remove(os.path.join(target, file))

        # 새로 저장
        for i, step in enumerate(steps, start=1):
            step["step"] = i
            fp = os.path.join(target, f"step{i}.json")
            with open(fp, "w", encoding="utf-8") as f:
                json.dump(step, f, ensure_ascii=False, indent=2)

        return jsonify({"status": "updated"})

    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ================================================================
# 7) Step 개별 수정 API
# ================================================================
@app.route("/api/update_step", methods=["POST"])
def update_step():
    try:
        body = request.get_json()
        session_id = body.get("session_id")
        index = body.get("step_index")
        title = body.get("title")
        description = body.get("description")
        text = body.get("text")

        session_dir = os.path.join(CURRICULUM_DIR, session_id)
        if not os.path.exists(session_dir):
            return jsonify({"status": "error", "error": "session not found"}), 404

        files = sorted([f for f in os.listdir(session_dir) if f.endswith(".json")])

        if index < 0 or index >= len(files):
            return jsonify({"status": "error", "error": "invalid index"}), 400

        target_file = os.path.join(session_dir, files[index])

        with open(target_file, "r", encoding="utf-8") as f:
            step_data = json.load(f)

        step_data["title"] = title
        step_data["description"] = description
        step_data["text"] = text

        with open(target_file, "w", encoding="utf-8") as f:
            json.dump(step_data, f, ensure_ascii=False, indent=2)

        return jsonify({"status": "ok", "error": None})

    except Exception as e:
        return jsonify({"status": "error", "error": str(e)}), 500


# ================================================================
# 8) Step 삭제 + 재정렬 API
# ================================================================
@app.route("/api/delete_step", methods=["POST"])
def delete_step():
    try:
        body = request.get_json()
        session_id = body.get("session_id")
        index = body.get("step_index")

        session_dir = os.path.join(CURRICULUM_DIR, session_id)
        if not os.path.exists(session_dir):
            return jsonify({"status": "error", "error": "session not found"}), 404

        files = sorted([f for f in os.listdir(session_dir) if f.endswith(".json")])

        if index < 0 or index >= len(files):
            return jsonify({"status": "error", "error": "invalid index"}), 400

        # 삭제
        os.remove(os.path.join(session_dir, files[index]))

        # 재정렬
        files = sorted([f for f in os.listdir(session_dir) if f.endswith(".json")])

        for i, old_name in enumerate(files, start=1):
            old_path = os.path.join(session_dir, old_name)
            new_path = os.path.join(session_dir, f"step{i}.json")

            if old_path != new_path:
                os.rename(old_path, new_path)

                with open(new_path, "r", encoding="utf-8") as f:
                    step_data = json.load(f)
                step_data["step"] = i

                with open(new_path, "w", encoding="utf-8") as f:
                    json.dump(step_data, f, ensure_ascii=False, indent=2)

        return jsonify({"status": "ok", "error": None})

    except Exception as e:
        return jsonify({"status": "error", "error": str(e)}), 500


# ================================================================
# Server run
# ================================================================
if __name__ == "__main__":
    print("🚀 MobileGPT Server Starting...")
    print("📍 Server running at: http://0.0.0.0:5001")
    print(f"💾 Sessions saved to: {SAVE_DIR}")
    print(f"📚 Curriculum saved to: {CURRICULUM_DIR}")
    app.run(host="0.0.0.0", port=5001, debug=True)
