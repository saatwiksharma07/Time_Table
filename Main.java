import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static final TimetableManager manager = new TimetableManager();

    public static void main(String[] args) throws java.io.IOException {
        if (args.length > 0 && args[0].equals("--web")) { WebServer.start(); return; }
        addSampleLectures(manager);
        int choice;
        do {
            showMenu();
            System.out.print("Enter your choice: ");
            if (!sc.hasNextLine()) break;
            try { choice = Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { choice = 0; }

            switch (choice) {
                case 1: addLecture(); break;
                case 2: manager.showAllLectures(); break;
                case 3: System.out.print("Student group (e.g. CSE-A): "); manager.showTimetableBy("group", sc.nextLine().trim()); break;
                case 4: System.out.print("Teacher name: "); manager.showTimetableBy("teacher", sc.nextLine().trim()); break;
                case 5: System.out.print("Subject name: "); manager.showTimetableBy("subject", sc.nextLine().trim()); break;
                case 6: manager.detectConflicts(); break;
                case 7: showFreeSlotMenu(); break;
                case 8: addTeacher(); break;
                case 9: manager.showTeachers(); break;
                case 10: removeLecture(); break;
                case 11: System.out.println("Thank you for using the Timetable Conflict Detector!"); break;
                default: System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 11);
        sc.close();
    }

    private static void showMenu() {
        System.out.println("\n==============================================");
        System.out.println("     AUTOMATED TIMETABLE CONFLICT DETECTOR");
        System.out.println("==============================================");
        System.out.println(" 1. Add a lecture");
        System.out.println(" 2. View complete timetable");
        System.out.println(" 3. View student/group timetable");
        System.out.println(" 4. View teacher timetable");
        System.out.println(" 5. View subject timetable");
        System.out.println(" 6. Detect timetable conflicts");
        System.out.println(" 7. Find free time slots");
        System.out.println(" 8. Add a teacher");
        System.out.println(" 9. View teachers");
        System.out.println("10. Delete a lecture");
        System.out.println("11. Exit");
        System.out.println("----------------------------------------------");
    }

    private static void addTeacher() {
        System.out.print("Teacher name: ");
        String name = sc.nextLine().trim();
        System.out.println(manager.addTeacher(name) ? "Teacher added." : "Name is empty or teacher already exists.");
    }

    private static void removeLecture() {
        manager.showAllLectures();
        System.out.print("Lecture number to delete: ");
        if (!sc.hasNextInt()) { sc.nextLine(); System.out.println("Invalid number."); return; }
        int n = sc.nextInt(); sc.nextLine();
        System.out.println(manager.removeLecture(n) ? "Lecture deleted." : "No lecture with that number.");
    }

    private static void addLecture() {
        System.out.println("\n--- Add New Lecture ---");
        System.out.print("Student group/class: ");   String group = sc.nextLine().trim();
        System.out.print("Teacher name: ");          String teacher = sc.nextLine().trim();
        System.out.print("Subject name: ");          String subject = sc.nextLine().trim();
        System.out.print("Classroom: ");             String room = sc.nextLine().trim();
        System.out.print("Day (Monday-Friday): ");   String day = sc.nextLine().trim();

        if (group.isEmpty() || teacher.isEmpty() || subject.isEmpty() || room.isEmpty()) {
            System.out.println("All fields are required."); return;
        }
        String validDay = normalizeDay(day);
        if (validDay == null) { System.out.println("Invalid day. Use Monday to Friday."); return; }

        try {
            System.out.print("Start time (HH:mm): ");
            LocalTime start = TimetableManager.readTime(sc.nextLine().trim());
            System.out.print("End time (HH:mm): ");
            LocalTime end = TimetableManager.readTime(sc.nextLine().trim());
            if (!start.isBefore(end)) { System.out.println("End time must be later than start time."); return; }

            List<String> problems = manager.addLectureChecked(
                    new Lecture(group, teacher, subject, room, validDay, start, end));
            if (problems.isEmpty()) {
                System.out.println("Lecture added successfully.");
            } else {
                System.out.println("Lecture NOT added because of conflicts:");
                for (String p : problems) System.out.println("  - " + p);
            }
        } catch (DateTimeParseException e) {
            System.out.println("Invalid time format. Use HH:mm, for example 09:30.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void showFreeSlotMenu() {
        System.out.println("\n1. Teacher free slots\n2. Classroom free slots");
        System.out.print("Choose an option: ");
        if (!sc.hasNextInt()) { sc.nextLine(); System.out.println("Invalid choice."); return; }
        int option = sc.nextInt(); sc.nextLine();
        if (option == 1) { System.out.print("Teacher name: "); manager.showFreeSlots("teacher", sc.nextLine().trim()); }
        else if (option == 2) { System.out.print("Classroom name: "); manager.showFreeSlots("classroom", sc.nextLine().trim()); }
        else System.out.println("Invalid choice.");
    }

    private static String normalizeDay(String day) {
        for (String d : TimetableManager.DAYS) if (d.equalsIgnoreCase(day)) return d;
        return null;
    }

    static void addSampleLectures(TimetableManager manager) {
        manager.addLecture(new Lecture("CSE-A", "Mr. Sharma", "Java", "Room 101", "Monday", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.addLecture(new Lecture("CSE-B", "Mrs. Verma", "DBMS", "Room 102", "Monday", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.addLecture(new Lecture("CSE-A", "Mr. Sharma", "Mathematics", "Room 103", "Monday", LocalTime.of(10, 0), LocalTime.of(11, 0)));
        manager.addLecture(new Lecture("CSE-B", "Mr. Singh", "Java", "Room 101", "Monday", LocalTime.of(10, 0), LocalTime.of(11, 0)));
        manager.addLecture(new Lecture("CSE-A", "Mrs. Verma", "DBMS", "Room 101", "Tuesday", LocalTime.of(11, 0), LocalTime.of(12, 0)));
    }
}
