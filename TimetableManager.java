import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TimetableManager {
    public static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    public static final LocalTime DAY_START = LocalTime.of(9, 0), DAY_END = LocalTime.of(17, 0);

    public static String normalizeDay(String day) {
        for (String d : DAYS) if (d.equalsIgnoreCase(day)) return d;
        throw new IllegalArgumentException("Invalid day. Use Monday to Friday.");
    }

    private final ArrayList<Lecture> lectures = new ArrayList<>();
    private final ArrayList<String> teachers = new ArrayList<>();

    // ---------- Teachers ----------
    public boolean addTeacher(String name) {
        if (name == null || name.trim().isEmpty() || hasTeacher(name.trim())) return false;
        name = name.trim();
        teachers.add(name);
        return true;
    }

    public boolean hasTeacher(String name) {
        for (String t : teachers) if (t.equalsIgnoreCase(name)) return true;
        return false;
    }

    /** Removes the teacher and all of their lectures. */
    public boolean removeTeacher(String name) {
        boolean removed = teachers.removeIf(t -> t.equalsIgnoreCase(name));
        lectures.removeIf(l -> l.getTeacher().equalsIgnoreCase(name));
        return removed;
    }

    public void showTeachers() {
        if (teachers.isEmpty()) { System.out.println("No teachers registered yet."); return; }
        System.out.println("\nRegistered teachers:");
        for (int i = 0; i < teachers.size(); i++) System.out.println("  " + (i + 1) + ". " + teachers.get(i));
    }

    // ---------- Lectures ----------
    /** Adds the lecture only if it creates no conflict. Returns the list of clashes found. */
    public List<String> addLectureChecked(Lecture lecture) {
        List<String> problems = new ArrayList<>();
        for (Lecture existing : lectures) {
            for (String reason : lecture.conflictsWith(existing)) {
                problems.add(reason + " conflict with: " + existing);
            }
        }
        if (problems.isEmpty()) {
            lectures.add(lecture);
            addTeacher(lecture.getTeacher());
        }
        return problems;
    }

    public void addLecture(Lecture lecture) {
        List<String> problems = addLectureChecked(lecture);
        if (!problems.isEmpty()) throw new IllegalArgumentException(String.join("; ", problems));
    }

    public boolean removeLecture(int number) {
        if (number < 1 || number > lectures.size()) return false;
        lectures.remove(number - 1);
        return true;
    }

    public void showAllLectures() {
        if (lectures.isEmpty()) { System.out.println("No lectures have been added yet."); return; }
        printHeading(true);
        for (int i = 0; i < lectures.size(); i++) System.out.println(String.format("%-4d", i + 1) + lectures.get(i));
    }

    public void showTimetableBy(String field, String value) {
        boolean found = false;
        System.out.println("\nTimetable for " + field + ": " + value);
        printHeading(false);
        for (Lecture l : lectures) {
            String actual;
            switch (field) {
                case "group":   actual = l.getStudentGroup(); break;
                case "teacher": actual = l.getTeacher(); break;
                case "classroom": actual = l.getClassroom(); break;
                default:        actual = l.getSubject();
            }
            if (actual.equalsIgnoreCase(value)) { System.out.println(l); found = true; }
        }
        if (!found) System.out.println("Nothing found for this " + field + ".");
    }

    public void detectConflicts() {
        boolean found = false;
        System.out.println("\n========== TIMETABLE CONFLICT REPORT ==========");
        for (int i = 0; i < lectures.size(); i++) {
            for (int j = i + 1; j < lectures.size(); j++) {
                Lecture a = lectures.get(i), b = lectures.get(j);
                for (String reason : a.conflictsWith(b)) {
                    System.out.println("\n" + reason + " CONFLICT:");
                    System.out.println(a);
                    System.out.println(b);
                    found = true;
                }
            }
        }
        if (!found) System.out.println("No conflicts detected. Timetable is clear!");
        System.out.println("================================================");
    }

    public void showFreeSlots(String type, String name) {
        LocalTime start = DAY_START, end = DAY_END;
        System.out.println("\nFree one-hour slots for " + type + ": " + name);
        for (String day : DAYS) {
            System.out.println("\n" + day + ":");
            boolean freeFound = false;
            for (LocalTime s = start; s.isBefore(end); s = s.plusHours(1)) {
                LocalTime e = s.plusHours(1);
                boolean occupied = false;
                for (Lecture l : lectures) {
                    String resource = type.equalsIgnoreCase("teacher") ? l.getTeacher() : l.getClassroom();
                    if (resource.equalsIgnoreCase(name) && l.getDay().equalsIgnoreCase(day)
                            && l.getStartTime().isBefore(e) && s.isBefore(l.getEndTime())) {
                        occupied = true;
                        break;
                    }
                }
                if (!occupied) { System.out.println("  " + s + " - " + e); freeFound = true; }
            }
            if (!freeFound) System.out.println("  No free slots.");
        }
    }

    public static LocalTime readTime(String value) throws DateTimeParseException {
        if (value == null || !value.matches("[0-9]{2}:[0-9]{2}"))
            throw new DateTimeParseException("Expected HH:mm", value == null ? "" : value, 0);
        return LocalTime.parse(value);
    }

    public List<Lecture> getLectures() { return new ArrayList<>(lectures); }
    public List<String> getTeachers() { return new ArrayList<>(teachers); }

    public boolean isFree(String type, String name, String day, LocalTime start, LocalTime end) {
        for (Lecture l : lectures) {
            String resource = type.equals("teacher") ? l.getTeacher() : l.getClassroom();
            if (resource.equalsIgnoreCase(name) && l.getDay().equalsIgnoreCase(day)
                    && l.getStartTime().isBefore(end) && start.isBefore(l.getEndTime())) return false;
        }
        return true;
    }

    private void printHeading(boolean numbered) {
        System.out.printf("%s%-12s %-18s %-18s %-12s %-10s %s%n", numbered ? "#   " : "",
                "GROUP", "TEACHER", "SUBJECT", "ROOM", "DAY", "TIME");
        System.out.println("------------------------------------------------------------------------------------------");
    }
}
