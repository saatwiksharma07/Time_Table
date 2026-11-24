import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Lecture {
    private final String studentGroup, teacher, subject, classroom, day;
    private final LocalTime startTime, endTime;

    public Lecture(String studentGroup, String teacher, String subject,
                   String classroom, String day, LocalTime startTime, LocalTime endTime) {
        this.studentGroup = required(studentGroup);
        this.teacher = required(teacher);
        this.subject = required(subject);
        this.classroom = required(classroom);
        this.day = TimetableManager.normalizeDay(required(day));
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)
                || startTime.isBefore(TimetableManager.DAY_START) || endTime.isAfter(TimetableManager.DAY_END)) {
            throw new IllegalArgumentException("Lecture must have a valid time between 09:00 and 17:00.");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    private static String required(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("All fields are required.");
        return value.trim();
    }

    public String getStudentGroup() { return studentGroup; }
    public String getTeacher() { return teacher; }
    public String getSubject() { return subject; }
    public String getClassroom() { return classroom; }
    public String getDay() { return day; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }

    public boolean overlaps(Lecture other) {
        return day.equalsIgnoreCase(other.day)
                && startTime.isBefore(other.endTime)
                && other.startTime.isBefore(endTime);
    }

    /** Returns every reason this lecture clashes with another one (empty = no clash). */
    public List<String> conflictsWith(Lecture other) {
        List<String> reasons = new ArrayList<>();
        if (!overlaps(other)) return reasons;
        if (teacher.equalsIgnoreCase(other.teacher)) reasons.add("TEACHER");
        if (classroom.equalsIgnoreCase(other.classroom)) reasons.add("CLASSROOM");
        if (studentGroup.equalsIgnoreCase(other.studentGroup)) reasons.add("STUDENT GROUP");
        return reasons;
    }

    @Override
    public String toString() {
        return String.format("%-12s %-18s %-18s %-12s %-10s %s - %s",
                studentGroup, teacher, subject, classroom, day, startTime, endTime);
    }
}
