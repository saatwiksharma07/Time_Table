# Timetable Conflict Detector

Java project for detecting overlapping lectures for a teacher, classroom or student group. Includes a console menu and browser interface backed by the same Java model.

## Run (JDK 17 or newer)

From the repository directory:

```sh
javac *.java
java Main
```

For the browser interface:

```sh
java Main --web
```

Open http://127.0.0.1:8765 in a browser. Keep the Java process running. Stop it with Ctrl+C. Do not open index.html directly.

The application uses sample lectures on startup, Monday–Friday, 09:00–17:00. Data is held in memory until the Java process stops; browser refresh does not reset it. No database or external dependencies are required.

## Files

- `Main.java`: console entry point and sample data.
- `Lecture.java`: lecture model, validation and overlap detection.
- `TimetableManager.java`: scheduling, teachers, conflict checks and free slots.
- `WebServer.java`: local HTTP interface to Java logic.
- `index.html`, `style.css`, `script.js`: browser user interface.

## Public demo deployment (Render)

1. Create a Web Service in https://dashboard.render.com/ and connect this GitHub repository, branch main.
2. Select Docker as the runtime, root directory blank, Dockerfile path `./Dockerfile`, and the Free instance type if available.
3. Deploy and open the generated onrender.com URL. The server binds to 0.0.0.0 using the host's PORT environment variable; local runs still use 127.0.0.1:8765.

This is a public demonstration: all visitors share the same in-memory timetable and can add, remove or reset entries without signing in. Use sample data only. Changes are lost when the server restarts, including free-tier sleep or redeployment. Add authentication and persistent storage before using real institutional timetables.
