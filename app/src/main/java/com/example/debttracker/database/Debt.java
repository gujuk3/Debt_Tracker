package com.example.debttracker.database;

public class Debt {
    private int id;
    private String personName;
    private double amount;
    private String type;
    private String description;
    private long date;
    private boolean isPaid;
    private long dueDate;              // Vade tarihi
    private boolean notificationEnabled; // Bildirim açık mı
    private String recurringType;        // NONE, WEEKLY, MONTHLY

    public Debt() {}

    public Debt(String personName, double amount, String type, String description, long date) {
        this.personName = personName;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.date = date;
        this.isPaid = false;
        this.dueDate = 0;
        this.notificationEnabled = false;
        this.recurringType = "NONE";
    }

    public Debt(String personName, double amount, String type, String description, long date, long dueDate, boolean notificationEnabled) {
        this.personName = personName;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.date = date;
        this.isPaid = false;
        this.dueDate = dueDate;
        this.notificationEnabled = notificationEnabled;
        this.recurringType = "NONE";
    }

    public Debt(String personName, double amount, String type, String description, long date, long dueDate, boolean notificationEnabled, String recurringType) {
        this.personName = personName;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.date = date;
        this.isPaid = false;
        this.dueDate = dueDate;
        this.notificationEnabled = notificationEnabled;
        this.recurringType = recurringType != null ? recurringType : "NONE";
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

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }

    public boolean isNotificationEnabled() { return notificationEnabled; }
    public void setNotificationEnabled(boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }

    public String getRecurringType() { return recurringType != null ? recurringType : "NONE"; }
    public void setRecurringType(String recurringType) { this.recurringType = recurringType != null ? recurringType : "NONE"; }
}
