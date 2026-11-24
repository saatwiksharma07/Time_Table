import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;

/** Browser interface. All scheduling decisions are made by the Java model. */
public class WebServer {
    private static TimetableManager manager = new TimetableManager();
    private static final Map<Integer, Lecture> ids = new LinkedHashMap<>();
    private static int nextId = 1;

    public static void start() throws IOException {
        reset();
        // Keep local development private; cloud hosts provide PORT.
        String cloudPort = System.getenv("PORT");
        int port = cloudPort == null ? 8765 : Integer.parseInt(cloudPort);
        String host = cloudPort == null ? "127.0.0.1" : "0.0.0.0";
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/", WebServer::handle);
        server.start();
        System.out.println("Server listening on " + host + ":" + port + ". Ctrl+C to stop.");
    }

    private static void reset() {
        manager = new TimetableManager();
        Main.addSampleLectures(manager);
        ids.clear();
        // Never reuse identifiers after reset, so stale pages cannot delete a new lecture.
        for (Lecture l : manager.getLectures()) ids.put(nextId++, l);
    }

    private static synchronized void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath(), method = ex.getRequestMethod();
            if (path.equals("/api/state") && method.equals("GET")) { json(ex, 200, state()); return; }
            if (path.equals("/api/free") && method.equals("GET")) {
                Map<String, String> p = params(ex.getRequestURI().getRawQuery());
                String type = p.get("type"), name = p.get("name");
                if (!("teacher".equals(type) || "classroom".equals(type)) || name == null || name.isBlank())
                    throw new IllegalArgumentException("Choose a teacher or classroom.");
                List<String> days = new ArrayList<>();
                for (String day : TimetableManager.DAYS) {
                    List<String> slots = new ArrayList<>();
                    for (LocalTime t = TimetableManager.DAY_START; t.isBefore(TimetableManager.DAY_END); t = t.plusHours(1))
                        if (manager.isFree(type, name, day, t, t.plusHours(1))) slots.add(q(t + "–" + t.plusHours(1)));
                    days.add("{\"day\":" + q(day) + ",\"slots\":[" + String.join(",", slots) + "]}");
                }
                json(ex, 200, "[" + String.join(",", days) + "]"); return;
            }
            if (path.startsWith("/api/") && method.equals("POST")) {
                byte[] body = ex.getRequestBody().readNBytes(16385);
                if (body.length > 16384) throw new IllegalArgumentException("Request too large.");
                Map<String, String> p = params(new String(body, StandardCharsets.UTF_8));
                switch (path) {
                    case "/api/lecture": {
                        Lecture l = new Lecture(p.get("studentGroup"), p.get("teacher"), p.get("subject"), p.get("classroom"), p.get("day"),
                                TimetableManager.readTime(p.get("start")), TimetableManager.readTime(p.get("end")));
                        if (!manager.hasTeacher(l.getTeacher())) throw new IllegalArgumentException("Add this teacher first.");
                        List<String> problems = manager.addLectureChecked(l);
                        if (!problems.isEmpty()) { json(ex, 409, "{\"error\":" + q(String.join("\n", problems)) + "}"); return; }
                        ids.put(nextId++, l); break;
                    }
                    case "/api/delete": {
                        int id = Integer.parseInt(p.get("id"));
                        Lecture l = ids.get(id);
                        if (l == null) throw new IllegalArgumentException("Lecture not found.");
                        manager.removeLecture(manager.getLectures().indexOf(l) + 1);
                        ids.remove(id); break;
                    }
                    case "/api/teacher":
                        if (!manager.addTeacher(p.get("name"))) throw new IllegalArgumentException("Name is empty or teacher already exists.");
                        break;
                    case "/api/remove-teacher": {
                        String name = p.get("name");
                        if (name == null || !manager.removeTeacher(name)) throw new IllegalArgumentException("Teacher not found.");
                        ids.values().removeIf(l -> l.getTeacher().equalsIgnoreCase(name)); break;
                    }
                    case "/api/reset": reset(); break;
                    default: json(ex, 404, "{\"error\":\"Not found\"}"); return;
                }
                json(ex, 200, state()); return;
            }
            if (method.equals("GET")) {
                String file = path.equals("/") ? "index.html" : path.substring(1);
                String mime = switch (file) { case "index.html" -> "text/html"; case "script.js" -> "text/javascript"; case "style.css" -> "text/css"; default -> null; };
                if (mime != null) { send(ex, 200, mime, Files.readAllBytes(Path.of(file))); return; }
            }
            json(ex, 404, "{\"error\":\"Not found\"}");
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            json(ex, 400, "{\"error\":" + q(e.getMessage() == null ? "Invalid input" : e.getMessage()) + "}");
        } finally { ex.close(); }
    }

    private static Map<String, String> params(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw != null) for (String part : raw.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2) map.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
        }
        return map;
    }

    private static String state() {
        List<String> lectures = new ArrayList<>(), teachers = new ArrayList<>(), conflicts = new ArrayList<>();
        for (String t : manager.getTeachers()) teachers.add(q(t));
        for (var entry : ids.entrySet()) lectures.add(lecture(entry.getKey(), entry.getValue()));
        for (var a : ids.entrySet()) for (var b : ids.entrySet()) if (a.getKey() < b.getKey())
            for (String reason : a.getValue().conflictsWith(b.getValue())) conflicts.add("{\"reason\":" + q(reason) + ",\"a\":" + lecture(a.getKey(), a.getValue()) + ",\"b\":" + lecture(b.getKey(), b.getValue()) + "}");
        return "{\"teachers\":[" + String.join(",", teachers) + "],\"lectures\":[" + String.join(",", lectures) + "],\"conflicts\":[" + String.join(",", conflicts) + "]}";
    }

    private static String lecture(int id, Lecture l) {
        return "{\"id\":" + id + ",\"studentGroup\":" + q(l.getStudentGroup()) + ",\"teacher\":" + q(l.getTeacher()) + ",\"subject\":" + q(l.getSubject()) + ",\"classroom\":" + q(l.getClassroom()) + ",\"day\":" + q(l.getDay()) + ",\"start\":" + q(l.getStartTime().toString()) + ",\"end\":" + q(l.getEndTime().toString()) + "}";
    }

    private static String q(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 32) b.append(String.format("\\u%04x", (int)c));
            else b.append(c);
        }
        return b.append('"').toString();
    }
    private static void json(HttpExchange ex, int status, String value) throws IOException {
        send(ex, status, "application/json", value.getBytes(StandardCharsets.UTF_8));
    }
    private static void send(HttpExchange ex, int status, String mime, byte[] bytes) throws IOException {
        ex.getResponseHeaders().set("Content-Type", mime + "; charset=UTF-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
    }
}
