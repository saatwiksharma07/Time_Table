/* Browser interface for the Java timetable server. */
const DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"];
const DAY_START = 9, DAY_END = 17;
let state = { teachers: [], lectures: [], conflicts: [] };
const same = (a, b) => a.trim().toLowerCase() === b.trim().toLowerCase();
const toMin = t => { const [h, m] = t.split(":").map(Number); return h * 60 + m; };
const pad = n => String(n).padStart(2, "0");
async function api(path, data) {
  const options = data === undefined ? {} : { method: "POST", body: new URLSearchParams(data) };
  const response = await fetch("/api/" + path, options);
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || "Request failed.");
  return result;
}
async function change(path, data, message) {
  try { state = await api(path, data); renderAll(); toast(message); return true; }
  catch (error) { toast(error.message); return false; }
}
const unique = key => [...new Map(state.lectures.map(l => [l[key].toLowerCase(), l[key]])).values()].sort();
const esc = s => String(s).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

/* ---------- helpers ---------- */
const $ = id => document.getElementById(id);
function toast(msg) {
  const t = $("toast");
  t.textContent = msg; t.classList.add("show");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => t.classList.remove("show"), 2200);
}
function fillSelect(sel, values, keep) {
  const prev = keep ? sel.value : "";
  sel.innerHTML = values.map(v => `<option>${esc(v)}</option>`).join("");
  if (prev && values.includes(prev)) sel.value = prev;
}

/* ---------- rendering ---------- */
function renderAll() {
  const conflicts = state.conflicts;
  $("summary").textContent = `${state.lectures.length} lectures · ${state.teachers.length} teachers`;
  const badge = $("conflictBadge");
  badge.textContent = conflicts.length;
  badge.classList.toggle("bad", conflicts.length > 0);

  renderViewFilter();
  renderGrid(conflicts);
  renderFormLists();
  renderTeachers();
  renderConflicts(conflicts);
  renderFree();
}

function renderViewFilter() {
  const type = $("viewType").value;
  $("viewValueWrap").hidden = type === "all";
  if (type !== "all") fillSelect($("viewValue"), type === "teacher" ? state.teachers : unique(type), true);
}

function renderGrid(conflicts) {
  const type = $("viewType").value, value = $("viewValue").value;
  const clashing = new Set(conflicts.flatMap(c => [c.a.id, c.b.id]));
  const shown = state.lectures.filter(l => type === "all" || same(l[type], value));

  let html = "<thead><tr><th class='time'></th>" + DAYS.map(d => `<th>${d}</th>`).join("") + "</tr></thead><tbody>";
  for (let h = DAY_START; h < DAY_END; h++) {
    html += `<tr><th class="time">${pad(h)}:00</th>`;
    for (const day of DAYS) {
      const cards = shown.filter(l => same(l.day, day) && toMin(l.start) < (h + 1) * 60 && h * 60 < toMin(l.end));
      html += "<td>" + cards.map(l => `
        <div class="lec${clashing.has(l.id) ? " clash" : ""}">
          <button class="x" data-del="${l.id}" title="Delete lecture" aria-label="Delete ${esc(l.subject)} lecture">×</button>
          <b>${esc(l.subject)}</b>${esc(l.studentGroup)} · ${esc(l.classroom)}
          <small>${esc(l.teacher)} · ${esc(l.start)}–${esc(l.end)}</small>
        </div>`).join("") + "</td>";
    }
    html += "</tr>";
  }
  $("grid").innerHTML = html + "</tbody>";
}

function renderFormLists() {
  fillSelect($("fTeacher"), state.teachers, true);
  fillSelect($("fDay"), DAYS, true);
  $("groupList").innerHTML = unique("studentGroup").map(v => `<option value="${esc(v)}">`).join("");
  $("subjectList").innerHTML = unique("subject").map(v => `<option value="${esc(v)}">`).join("");
  $("roomList").innerHTML = unique("classroom").map(v => `<option value="${esc(v)}">`).join("");
}

function renderTeachers() {
  const list = $("teacherList");
  if (!state.teachers.length) { list.innerHTML = "<li class='empty'>No teachers yet. Add one above.</li>"; return; }
  list.innerHTML = state.teachers.map(t => {
    const n = state.lectures.filter(l => same(l.teacher, t)).length;
    return `<li><span>${esc(t)}<small>${n} lecture${n === 1 ? "" : "s"}</small></span>
      <button class="danger" data-rm-teacher="${esc(t)}">Remove</button></li>`;
  }).join("");
}

function renderConflicts(conflicts) {
  const box = $("conflictList");
  if (!conflicts.length) { box.innerHTML = "<div class='all-clear'>No conflicts detected. Timetable is clear!</div>"; return; }
  const line = l => `<p><b>${esc(l.subject)}</b> · ${esc(l.studentGroup)} · ${esc(l.teacher)} · ${esc(l.classroom)} · ${esc(l.day)} ${esc(l.start)}–${esc(l.end)}</p>`;
  box.innerHTML = conflicts.map(c => `<div class="conflict"><h3>${esc(c.reason)} conflict</h3>${line(c.a)}${line(c.b)}</div>`).join("");
}

let freeRequest = 0;
async function renderFree() {
  const request = ++freeRequest;
  const type = $("freeType").value;
  fillSelect($("freeName"), type === "teacher" ? state.teachers : unique("classroom"), true);
  const name = $("freeName").value;
  if (!name) { $("freeResult").innerHTML = "<p class='empty'>Nothing to show yet.</p>"; return; }
  try {
    const days = await api("free?" + new URLSearchParams({ type, name }));
    if (request !== freeRequest) return;
    $("freeResult").innerHTML = days.map(d => `<div class="free-day"><h3>${esc(d.day)}</h3>${d.slots.map(slot => `<span class="slot">${esc(slot)}</span>`).join("") || "<span class='empty'>No free slots</span>"}</div>`).join("");
  } catch (error) { if (request === freeRequest) $("freeResult").textContent = error.message; }
}

/* ---------- events ---------- */
$("tabs").addEventListener("click", e => {
  const tab = e.target.closest("button")?.dataset.tab;
  if (!tab) return;
  document.querySelectorAll("nav button").forEach(b => b.classList.toggle("active", b.dataset.tab === tab));
  document.querySelectorAll(".panel").forEach(p => p.classList.toggle("active", p.id === tab));
});

["viewType", "viewValue", "freeType", "freeName"].forEach(id => $(id).addEventListener("change", renderAll));

$("grid").addEventListener("click", e => {
  const id = e.target.dataset.del;
  if (id && confirm("Delete this lecture?")) change("delete", { id }, "Lecture deleted");
});
$("resetBtn").addEventListener("click", () => {
  if (confirm("Replace everything with the sample timetable?")) change("reset", {}, "Sample data restored");
});
$("teacherForm").addEventListener("submit", async e => {
  e.preventDefault();
  if (await change("teacher", { name: $("tName").value.trim() }, "Teacher added")) $("tName").value = "";
});
$("teacherList").addEventListener("click", e => {
  const name = e.target.dataset.rmTeacher;
  if (name && confirm(`Remove ${name} and their lectures?`)) change("remove-teacher", { name }, "Teacher removed");
});
$("lectureForm").addEventListener("submit", async e => {
  e.preventDefault();
  const msg = $("formMsg"), button = e.target.querySelector("button[type=submit]");
  const show = (text, ok) => { msg.textContent = text; msg.className = "form-msg " + (ok ? "ok" : "err"); };
  const data = { studentGroup: $("fGroup").value, teacher: $("fTeacher").value, subject: $("fSubject").value,
    classroom: $("fRoom").value, day: $("fDay").value, start: $("fStart").value, end: $("fEnd").value };
  button.disabled = true;
  try { state = await api("lecture", data); renderAll(); show("Lecture added.", true); toast("Lecture added"); }
  catch (error) { show(error.message, false); }
  finally { button.disabled = false; }
});
api("state").then(data => { state = data; renderAll(); }).catch(() => toast("Start the Java server with java Main --web, then open http://127.0.0.1:8765"));
