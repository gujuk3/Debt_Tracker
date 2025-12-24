package com.example.debttracker.database;

public class Debt {
    private int id;
    private String personName;
    private double amount;
    private String type;
    private String description;
    private long date;
    private boolean isPaid;
    private Long dueDate;
    private Long reminderTime;
    private String calendarEventId;

    public Debt() {}

    public Debt(String personName, double amount, String type, String description, long date) {
        this.personName = personName;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.date = date;
        this.isPaid = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }

    public Long getDueDate() { return dueDate; }
    public void setDueDate(Long dueDate) { this.dueDate = dueDate; }

    public Long getReminderTime() { return reminderTime; }
    public void setReminderTime(Long reminderTime) { this.reminderTime = reminderTime; }

    public String getCalendarEventId() { return calendarEventId; }
    public void setCalendarEventId(String calendarEventId) { this.calendarEventId = calendarEventId; }
}