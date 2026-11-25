package com.personal.reflection;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.time.*;
import java.util.*;

final class Reminders {
    static final String ACTION = "com.personal.reflection.DUE";
    static final String CHANNEL = "due_reports";
    private static final int ALARM_ID = 91, NOTIFICATION_ID = 92;
    static void channel(Context context) {
        NotificationManager nm=context.getSystemService(NotificationManager.class);
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Report reminders",NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("A single reminder at 9 pm when reports are due.");
        nm.createNotificationChannel(channel);
    }
    static boolean notificationsEnabled(Context context) {
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager nm=context.getSystemService(NotificationManager.class);
        NotificationChannel c=nm.getNotificationChannel(CHANNEL);
        return nm.areNotificationsEnabled() && (c==null || c.getImportance()!=NotificationManager.IMPORTANCE_NONE);
    }
    static boolean exactEnabled(Context context) {
        return Build.VERSION.SDK_INT<31 || context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }
    private static PendingIntent alarmIntent(Context c, LocalDate date) {
        Intent i=new Intent(c,ReminderReceiver.class).setAction(ACTION);
        if(date!=null) i.putExtra("date",date.toString());
        return PendingIntent.getBroadcast(c,ALARM_ID,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void schedule(Context context) {
        channel(context);
        AlarmManager manager=context.getSystemService(AlarmManager.class);
        manager.cancel(alarmIntent(context,null));
        Store store=new Store(context);
        ZonedDateTime now=ZonedDateTime.now(); LocalDate today=now.toLocalDate();
        LocalDate from=now.toLocalTime().isBefore(Schedule.DUE_TIME)?today:today.plusDays(1);
        LocalDate earliest=null;
        for(org.json.JSONObject c:store.allCategories()) for(org.json.JSONObject r:store.reports(c)) {
            LocalDate date=store.nextDue(c,r,from);
            if(earliest==null || date.isBefore(earliest)) earliest=date;
        }
        if(earliest==null) return;
        PendingIntent pi=alarmIntent(context,earliest);
        long millis=Schedule.dueMillis(earliest,now.getZone());
        if(exactEnabled(context)) {
            try { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,millis,pi); return; }
            catch(SecurityException ignored) { /* Permission can change between check and call. */ }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,millis,pi);
    }
    static void deliver(Context context, LocalDate date) {
        channel(context);
        Store store=new Store(context);
        List<Store.Due> due=store.dueOn(date,null,true);
        if(due.isEmpty() || !notificationsEnabled(context)) return;
        String title=due.size()==1?"1 report is due":""+due.size()+" reports are due";
        if(date.isBefore(LocalDate.now())) title=due.size()==1?"1 report was due":""+due.size()+" reports were due";
        StringBuilder detail=new StringBuilder();
        for(Store.Due item:due) {
            if(detail.length()>0) detail.append("\n");
            detail.append(item.report.optString("name"));
        }
        Intent open=new Intent(context,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        // Keep private reflection titles out of the lock-screen preview.
        Notification publicVersion=new Notification.Builder(context,CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Reflection reminder").setContentText("Open the app to review your reports.").build();
        Notification notification=new Notification.Builder(context,CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title)
            .setContentText("Open Reflection to complete your reports.")
            .setStyle(new Notification.BigTextStyle().bigText(detail.toString()))
            .setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(publicVersion)
            .setContentIntent(content).setAutoCancel(true).build();
        try { context.getSystemService(NotificationManager.class).notify(NOTIFICATION_ID,notification); }
        catch(SecurityException ignored) { }
    }
}
