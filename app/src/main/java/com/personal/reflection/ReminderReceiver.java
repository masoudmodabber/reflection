package com.personal.reflection;

import android.content.*;
import java.time.LocalDate;

public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if(Reminders.ACTION.equals(intent.getAction())) {
            String date=intent.getStringExtra("date");
            if(date!=null) Reminders.deliver(context,LocalDate.parse(date));
        }
        Reminders.schedule(context);
    }
}
