package com.smartstart.portal.notification.service;

import com.smartstart.portal.notification.entity.NotificationType;

/**
 * Names of the templates used by automated flows, plus fallback content used when the admin
 * has not created (or has deleted) a template with that name.
 */
public enum BuiltInTemplates {

    WEEKLY_SCHEDULE("Weekly Schedule", NotificationType.SCHEDULE,
            "{{className}} — weekly class schedule",
            """
                    Dear {{guardianName}},

                    Here is the weekly timetable for {{className}}:

                    {{scheduleTable}}

                    Please contact us if you have any questions.

                    {{centreName}}"""),
    MONTHLY_SCHEDULE("Monthly Schedule", NotificationType.SCHEDULE,
            "{{studentName}} — {{className}} class schedule for {{month}}",
            """
                    Dear {{guardianName}},

                    Here are {{studentName}}'s {{className}} class dates for {{month}}:

                    {{scheduleTable}}

                    Please let us know in advance if your child will miss a class.

                    {{centreName}}"""),
    MONTHLY_SCHEDULE_SMS("Monthly Schedule SMS", NotificationType.SCHEDULE,
            "SMS schedule: {{className}} — {{month}}",
            "{{centreName}}: {{studentName}}'s {{className}} classes in {{month}}: {{classDates}}. Please tell us in advance if {{studentName}} will miss a class."),
    CLASS_REMINDER("Class Reminder", NotificationType.SCHEDULE,
            "Reminder: {{studentName}} has class on {{date}}",
            """
                    Dear {{guardianName}},

                    This is a reminder that {{studentName}} has {{className}} class on {{date}}:

                    {{scheduleTable}}

                    See you there!

                    {{centreName}}"""),
    CLASS_CHANGE("Class Change", NotificationType.SCHEDULE_CHANGE,
            "{{className}} class update for {{studentName}}",
            """
                    Dear {{guardianName}},

                    Please note a change to {{studentName}}'s {{className}} classes:

                    {{change}}

                    Please contact us if you have any questions.

                    {{centreName}}"""),
    CLASS_CHANGE_SMS("Class Change SMS", NotificationType.SCHEDULE_CHANGE,
            "SMS class update: {{className}} — {{studentName}}",
            "{{centreName}}: {{studentName}}'s {{className}} class update: {{change}}"),
    FEE_REMINDER("Fee Reminder", NotificationType.FEE_REMINDER,
            "Fee reminder for {{studentName}} — {{month}}",
            """
                    Dear {{guardianName}},

                    This is a friendly reminder that the tuition fee for {{studentName}} for {{month}} is outstanding.

                    Amount due: {{amountDue}}
                    Balance remaining: {{balance}}

                    If you have already paid, please disregard this message.

                    {{centreName}}"""),
    FEEDBACK("Monthly Feedback", NotificationType.FEEDBACK,
            "{{studentName}} — monthly feedback for {{month}}",
            """
                    Dear {{guardianName}},

                    Here is {{studentName}}'s progress report for {{month}}.

                    {{feedback}}

                    {{centreName}}"""),

    // SMS versions of the automated reminders: short, no timetable. The subject is only the title in the history list.
    CLASS_REMINDER_SMS("Class Reminder SMS", NotificationType.SCHEDULE,
            "SMS reminder: {{className}} class on {{date}}",
            "{{centreName}}: reminder, {{studentName}} has class on {{date}}, {{time}} at {{location}}. See you there!"),
    FEE_REMINDER_SMS("Fee Reminder SMS", NotificationType.FEE_REMINDER,
            "SMS fee reminder for {{studentName}} — {{month}}",
            "{{centreName}}: friendly reminder, {{studentName}}'s fee for {{month}} has {{balance}} outstanding. "
                    + "Please ignore if already paid.");

    private final String templateName;
    private final NotificationType type;
    private final String subject;
    private final String body;

    BuiltInTemplates(String templateName, NotificationType type, String subject, String body) {
        this.templateName = templateName;
        this.type = type;
        this.subject = subject;
        this.body = body;
    }

    public String templateName() {
        return templateName;
    }

    public NotificationType type() {
        return type;
    }

    public String subject() {
        return subject;
    }

    public String body() {
        return body;
    }
}
