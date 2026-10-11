package com.smartstart.portal.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartstart.portal.attendance.dto.AttendanceDtos.AttendanceGridResponse;
import com.smartstart.portal.attendance.service.AttendanceService;
import com.smartstart.portal.chat.dto.ChatDtos.ChatSource;
import com.smartstart.portal.classgroup.dto.ClassGroupDtos.ClassGroupResponse;
import com.smartstart.portal.classgroup.service.ClassGroupService;
import com.smartstart.portal.common.dto.PageResponse;
import com.smartstart.portal.common.util.AppTime;
import com.smartstart.portal.dashboard.dto.DashboardDtos.MonthlySummaryResponse;
import com.smartstart.portal.dashboard.service.DashboardService;
import com.smartstart.portal.fee.dto.FeeDtos.FeeListResponse;
import com.smartstart.portal.fee.dto.FeeDtos.FeeRowResponse;
import com.smartstart.portal.fee.entity.FeeStatus;
import com.smartstart.portal.fee.service.FeeService;
import com.smartstart.portal.feedback.dto.FeedbackDtos.FeedbackRowResponse;
import com.smartstart.portal.feedback.service.FeedbackService;
import com.smartstart.portal.schedule.dto.ScheduleDtos.SlotResponse;
import com.smartstart.portal.schedule.service.ScheduleSlotService;
import com.smartstart.portal.schedule.service.TermService;
import com.smartstart.portal.student.dto.StudentDtos.StudentFilter;
import com.smartstart.portal.student.dto.StudentDtos.StudentResponse;
import com.smartstart.portal.student.entity.StudentStatus;
import com.smartstart.portal.student.service.StudentService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The only way the chatbot can see data: read-only lookups over the same services the screens use, so balances,
 * statuses and totals are the app's own numbers rather than something the model computed. Outputs include
 * pre-computed counts and lists because small local models are unreliable at counting.
 */
@Component
public class ChatTools {

    /** A tool's result plus the screen it corresponds to. */
    public record ToolResult(Object data, ChatSource source) {
    }

    private record Tool(String name, String description, ObjectNode parameters, Function<JsonNode, ToolResult> run) {
    }

    private final ObjectMapper mapper;
    private final FeeService fees;
    private final AttendanceService attendance;
    private final FeedbackService feedback;
    private final StudentService students;
    private final ClassGroupService groups;
    private final DashboardService dashboard;
    private final TermService terms;
    private final ScheduleSlotService slots;
    private final Clock clock;
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ChatTools(ObjectMapper mapper, FeeService fees, AttendanceService attendance, FeedbackService feedback,
                     StudentService students, ClassGroupService groups, DashboardService dashboard, TermService terms,
                     ScheduleSlotService slots, Clock clock) {
        this.mapper = mapper;
        this.fees = fees;
        this.attendance = attendance;
        this.feedback = feedback;
        this.students = students;
        this.groups = groups;
        this.dashboard = dashboard;
        this.terms = terms;
        this.slots = slots;
        this.clock = clock;
        register();
    }

    /** Tool definitions in Ollama's (OpenAI-style) function format. */
    public ArrayNode definitions() {
        ArrayNode out = mapper.createArrayNode();
        tools.values().forEach(t -> {
            ObjectNode fn = out.addObject().put("type", "function").putObject("function");
            fn.put("name", t.name()).put("description", t.description()).set("parameters", t.parameters());
        });
        return out;
    }

    /** Runs a tool; bad arguments come back as an error the model can read and correct. */
    public ToolResult run(String name, JsonNode args) {
        Tool tool = tools.get(name);
        if (tool == null) {
            return new ToolResult(Map.of("error", "Unknown tool " + name + ". Available: " + String.join(", ", tools.keySet())), null);
        }
        try {
            return tool.run().apply(args == null || args.isNull() ? mapper.createObjectNode() : args);
        } catch (IllegalArgumentException e) {
            return new ToolResult(Map.of("error", e.getMessage()), null);
        }
    }

    // ------------------------------------------------------------------ tools

    private void register() {
        add("get_fees", """
                        Fees for ONE month: for every billed student the amount due, amount paid, balance (positive = still owes, \
                        negative = credit/paid extra) and status (PAID, PARTIAL, UNPAID, CREDIT). Also month totals, how many \
                        students are in each status, and ready-made lists of who still owes and who has a credit. Use for any \
                        question about fees, payments, dues, balances or collections in a month.""",
                params(month(true), batch()),
                a -> feesFor(month(a), batchId(a)));
        add("get_monthly_fee_summary", """
                        Billed, collected and outstanding totals for every month of a year, plus the year total. Use to compare \
                        months or for yearly totals.""",
                params(prop("year", "integer", "Year, e.g. 2026")),
                a -> {
                    int year = a.hasNonNull("year") ? a.get("year").asInt() : YearMonth.now(clock).getYear();
                    MonthlySummaryResponse s = dashboard.monthlySummary(year);
                    return new ToolResult(s, new ChatSource("Dashboard · " + year, "/dashboard"));
                });
        add("get_student_counts", """
                        How many students there are: active and inactive totals, and active students per batch (class group) \
                        and per grade. Use for any "how many students" question.""",
                params(),
                a -> studentCounts());
        add("list_students", """
                        List students (name, student code, grade, batch, status, guardian). Optional filters: status, batch, grade.""",
                params(prop("status", "string", "ACTIVE or INACTIVE (default: both)"), batch(), prop("grade", "string", "e.g. Grade 3, SK, Coding")),
                this::listStudents);
        add("get_student", """
                        Everything about ONE student by name: profile (grade, batch, status, guardian, contacts, school), fees and \
                        balance for each month, attendance per month, and the monthly feedback reports.""",
                params(req(prop("name", "string", "Student name or part of it, e.g. Kaya")), "name"),
                this::student);
        add("get_attendance", """
                        Attendance for ONE month: per student classes present, absent, held and attendance %, plus overall \
                        totals. Optional batch filter. Use for attendance, absences or "who missed classes" questions.""",
                params(month(true), batch()),
                a -> attendanceFor(month(a), batchId(a)));
        add("get_feedback", """
                        Monthly feedback reports for ONE month (batch, topics taught, areas to improve, achievements) for each \
                        student who has one. Optional student name filter.""",
                params(month(true), prop("name", "string", "Optional student name filter")),
                this::feedbackFor);
        add("get_schedule", """
                        The active term (start/end dates) and the weekly class timetable (batch, day, time, subject, location, teacher).""",
                params(),
                a -> schedule());
    }

    private ToolResult feesFor(YearMonth month, Long batchId) {
        FeeListResponse list = fees.list(month, null, batchId);
        Map<FeeStatus, Long> byStatus = list.rows().stream().collect(Collectors.groupingBy(FeeRowResponse::status, TreeMap::new, Collectors.counting()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("month", AppTime.monthLabel(month));
        out.put("studentsBilled", list.rows().size());
        out.put("totals", Map.of("amountDue", list.totals().amountDue(), "amountPaid", list.totals().amountPaid(),
                "balance", list.totals().balance()));
        out.put("studentsPerStatus", byStatus);
        out.put("studentsWhoPaidSomething", list.rows().stream().filter(r -> r.amountPaid().signum() > 0).count());
        out.put("stillOwing", list.rows().stream().filter(r -> r.balance().signum() > 0)
                .map(r -> Map.of("name", r.fullName(), "balance", r.balance(), "status", r.status())).toList());
        out.put("withCredit", list.rows().stream().filter(r -> r.balance().signum() < 0)
                .map(r -> Map.of("name", r.fullName(), "credit", r.balance().negate())).toList());
        out.put("rows", list.rows().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", r.fullName());
            m.put("batch", r.classGroupName());
            m.put("grade", r.grade());
            m.put("classesHeld", r.classesHeld());
            m.put("rate", r.rate());
            m.put("amountDue", r.amountDue());
            m.put("amountPaid", r.amountPaid());
            m.put("balance", r.balance());
            m.put("status", r.status());
            m.put("lastPaymentDate", r.lastPaymentDate());
            return m;
        }).toList());
        if (list.rows().isEmpty()) {
            out.put("note", "No fees exist for this month yet (they are created with Generate fees on the Fees page).");
        }
        return new ToolResult(out, new ChatSource("Fees · " + AppTime.monthLabel(month), "/fees?month=" + month));
    }

    private ToolResult studentCounts() {
        List<StudentResponse> all = allStudents(new StudentFilter(null, null, null, null));
        List<StudentResponse> active = all.stream().filter(s -> s.status() == StudentStatus.ACTIVE).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("active", active.size());
        out.put("inactive", all.size() - active.size());
        out.put("total", all.size());
        out.put("activePerBatch", active.stream().collect(Collectors.groupingBy(s -> s.classGroupName() == null ? "No batch" : s.classGroupName(), TreeMap::new, Collectors.counting())));
        out.put("activePerGrade", active.stream().collect(Collectors.groupingBy(StudentResponse::grade, TreeMap::new, Collectors.counting())));
        return new ToolResult(out, new ChatSource("Students", "/students"));
    }

    private ToolResult listStudents(JsonNode a) {
        StudentStatus status = null;
        if (a.hasNonNull("status") && !a.get("status").asText().isBlank()) {
            try {
                status = StudentStatus.valueOf(a.get("status").asText().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("status must be ACTIVE or INACTIVE");
            }
        }
        String grade = a.hasNonNull("grade") && !a.get("grade").asText().isBlank() ? a.get("grade").asText().trim() : null;
        List<StudentResponse> list = allStudents(new StudentFilter(null, status, grade, batchId(a)));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("count", list.size());
        out.put("students", list.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", s.fullName());
            m.put("code", s.studentCode());
            m.put("grade", s.grade());
            m.put("batch", s.classGroupName());
            m.put("status", s.status());
            m.put("guardian", s.guardianName());
            return m;
        }).toList());
        return new ToolResult(out, new ChatSource("Students", "/students"));
    }

    private ToolResult student(JsonNode a) {
        String name = a.path("name").asText("").trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        List<StudentResponse> matches = allStudents(new StudentFilter(name, null, null, null));
        if (matches.isEmpty()) {
            matches = allStudents(new StudentFilter(name.split("\\s+")[0], null, null, null));
        }
        if (matches.isEmpty()) {
            return new ToolResult(Map.of("error", "No student matches '" + name + "'."), null);
        }
        if (matches.size() > 1) {
            List<StudentResponse> exact = matches.stream().filter(s -> s.fullName().equalsIgnoreCase(name)
                    || s.fullName().split("\\s+")[0].equalsIgnoreCase(name)).toList();
            if (exact.size() != 1) {
                return new ToolResult(Map.of("error", "Several students match '" + name + "'; ask which one.",
                        "candidates", matches.stream().map(StudentResponse::fullName).toList()), null);
            }
            matches = exact;
        }
        StudentResponse s = matches.getFirst();
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("name", s.fullName());
        profile.put("code", s.studentCode());
        profile.put("status", s.status());
        profile.put("grade", s.grade());
        profile.put("batch", s.classGroupName());
        profile.put("school", s.school());
        profile.put("subjects", s.subjects());
        profile.put("dateOfBirth", s.dateOfBirth());
        profile.put("guardian", s.guardianName());
        profile.put("primaryContact", s.primaryContact());
        profile.put("alternateContact", s.alternateContact());
        profile.put("email", s.email());
        profile.put("perClassRate", s.perClassRate());
        profile.put("enrolled", s.enrollmentDate());
        profile.put("notes", s.notes());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("profile", profile);
        out.put("feesByMonth", fees.studentHistory(s.id()).stream().map(r -> Map.of("month", r.month(), "amountDue", r.amountDue(),
                "amountPaid", r.amountPaid(), "balance", r.balance(), "status", r.status())).toList());
        out.put("attendanceByMonth", attendance.history(s.id()).months());
        out.put("feedbackByMonth", feedback.studentHistory(s.id()).stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", f.month());
            m.put("topicsTaught", f.topicsTaught());
            m.put("improvementAreas", f.improvementAreas());
            m.put("achievements", f.achievements());
            return m;
        }).toList());
        return new ToolResult(out, new ChatSource(s.fullName(), "/students/" + s.id()));
    }

    private ToolResult attendanceFor(YearMonth month, Long batchId) {
        AttendanceGridResponse grid = attendance.grid(month, batchId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("month", AppTime.monthLabel(month));
        out.put("totals", grid.totals());
        out.put("students", grid.rows().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", r.fullName());
            m.put("batch", r.classGroupName());
            m.put("present", r.present());
            m.put("absent", r.absent());
            m.put("held", r.held());
            m.put("attendancePct", r.attendancePct());
            return m;
        }).toList());
        out.put("studentsWithAbsences", grid.rows().stream().filter(r -> r.absent() > 0)
                .map(r -> Map.of("name", r.fullName(), "absent", r.absent())).toList());
        return new ToolResult(out, new ChatSource("Attendance · " + AppTime.monthLabel(month), "/attendance?month=" + month));
    }

    private ToolResult feedbackFor(JsonNode a) {
        YearMonth month = month(a);
        String name = a.path("name").asText("").trim().toLowerCase(Locale.ROOT);
        List<FeedbackRowResponse> rows = feedback.list(month, null).stream()
                .filter(r -> r.id() != null)
                .filter(r -> name.isEmpty() || r.fullName().toLowerCase(Locale.ROOT).contains(name))
                .toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("month", AppTime.monthLabel(month));
        out.put("reports", rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", r.fullName());
            m.put("batch", r.classGroupName());
            m.put("topicsTaught", r.topicsTaught());
            m.put("improvementAreas", r.improvementAreas());
            m.put("achievements", r.achievements());
            return m;
        }).toList());
        return new ToolResult(out, new ChatSource("Feedback · " + AppTime.monthLabel(month), "/feedback?month=" + month));
    }

    private ToolResult schedule() {
        Map<String, Object> out = new LinkedHashMap<>();
        terms.active().ifPresentOrElse(t -> {
            out.put("term", Map.of("name", t.name(), "start", t.startDate(), "end", t.endDate()));
            List<SlotResponse> list = slots.list(t.id());
            out.put("classes", list.stream().map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("batch", s.classGroupName());
                m.put("day", s.dayOfWeek());
                m.put("time", s.startTime() + "–" + s.endTime());
                m.put("subject", s.subject());
                m.put("location", s.location());
                m.put("teacher", s.teacher());
                return m;
            }).toList());
            if (list.isEmpty()) {
                out.put("note", "No weekly classes have been added to the schedule yet.");
            }
        }, () -> out.put("note", "No active term is set up."));
        return new ToolResult(out, new ChatSource("Schedule", "/schedule"));
    }

    // ------------------------------------------------------------------ helpers

    private List<StudentResponse> allStudents(StudentFilter filter) {
        PageResponse<StudentResponse> page = students.list(filter, PageRequest.of(0, 200, Sort.by("studentCode")));
        return new ArrayList<>(page.content());
    }

    /** Accepts 2026-10, "October 2026", "Oct 2026" or a bare month name (current year). */
    YearMonth month(JsonNode a) {
        String raw = a.path("month").asText("").trim();
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("month is required, as YYYY-MM (e.g. " + YearMonth.now(clock) + ")");
        }
        try {
            return YearMonth.parse(raw.length() > 7 && raw.charAt(4) == '-' ? raw.substring(0, 7) : raw);
        } catch (RuntimeException ignored) {
            // try names below
        }
        String[] parts = raw.replace(",", " ").trim().split("\\s+");
        for (Month m : Month.values()) {
            String full = m.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            if (full.equalsIgnoreCase(parts[0]) || full.substring(0, 3).equalsIgnoreCase(parts[0])) {
                int year = parts.length > 1 && parts[1].matches("\\d{4}") ? Integer.parseInt(parts[1]) : YearMonth.now(clock).getYear();
                return YearMonth.of(year, m);
            }
        }
        throw new IllegalArgumentException("month must be YYYY-MM, e.g. " + YearMonth.now(clock) + "; got '" + raw + "'");
    }

    /** Batch by name ("Batch3", "batch 3" or just "3"); null when not given. */
    private Long batchId(JsonNode a) {
        String raw = a.path("batch").asText("").trim();
        if (raw.isEmpty() || raw.equalsIgnoreCase("all")) {
            return null;
        }
        String key = raw.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        if (key.matches("\\d+")) {
            key = "batch" + key;
        }
        List<ClassGroupResponse> all = groups.list();
        for (ClassGroupResponse g : all) {
            if (g.name().replaceAll("\\s+", "").equalsIgnoreCase(key)) {
                return g.id();
            }
        }
        throw new IllegalArgumentException("Unknown batch '" + raw + "'. Batches: " + all.stream().map(ClassGroupResponse::name).toList());
    }

    private void add(String name, String description, ObjectNode parameters, Function<JsonNode, ToolResult> run) {
        tools.put(name, new Tool(name, description, parameters, run));
    }

    private ObjectNode params(ObjectNode... props) {
        return params(props, new String[0]);
    }

    private ObjectNode params(ObjectNode prop, String required) {
        return params(new ObjectNode[]{prop}, new String[]{required});
    }

    private ObjectNode params(ObjectNode[] props, String[] required) {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        List<String> req = new ArrayList<>(List.of(required));
        for (ObjectNode p : props) {
            String name = p.remove("_name").asText();
            if (p.remove("_required") != null) {
                req.add(name);
            }
            properties.set(name, p);
        }
        ArrayNode r = schema.putArray("required");
        req.stream().distinct().forEach(r::add);
        return schema;
    }

    private ObjectNode prop(String name, String type, String description) {
        return mapper.createObjectNode().put("_name", name).put("type", type).put("description", description);
    }

    private static ObjectNode req(ObjectNode prop) {
        return prop.put("_required", true);
    }

    private ObjectNode month(boolean required) {
        ObjectNode p = prop("month", "string", "Month as YYYY-MM, e.g. " + YearMonth.now(clock));
        return required ? req(p) : p;
    }

    private ObjectNode batch() {
        return prop("batch", "string", "Optional class group, e.g. Batch3");
    }
}
