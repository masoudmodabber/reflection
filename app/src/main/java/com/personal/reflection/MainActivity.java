package com.personal.reflection;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int IMPORT=100, NOTIFY=101;
    private static final int BG=0xfff7f8f5, PAPER=Color.WHITE, INK=0xff202923, MUTED=0xff586760, ACCENT=0xff24675b, LINE=0xffdbe3dc;
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("EEE, d MMM uuuu",Locale.ENGLISH);
    private Store store;
    private LinearLayout body;
    private Runnable back;
    private boolean homeVisible;
    private boolean editing;
    private boolean dirty;
    private boolean permissionReturn;
    private interface ReadAnswer { Object get(); }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        if(Build.VERSION.SDK_INT>=30) getWindow().setDecorFitsSystemWindows(false);
        store=new Store(this);
        Reminders.channel(this); Reminders.schedule(this);
        home();
    }
    @Override protected void onResume() {
        super.onResume();
        if(store!=null) {
            Reminders.schedule(this);
            if(homeVisible || permissionReturn) { permissionReturn=false; home(); }
        }
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); confirmLeave(this::home);
    }
    @Override public void onBackPressed() {
        if(back==null) { super.onBackPressed(); return; }
        confirmLeave(back);
    }
    private int dp(float x) { return (int)(x*getResources().getDisplayMetrics().density+.5f); }
    private TextView text(String value,int size,int color) {
        TextView v=new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setLineSpacing(dp(2),1f); return v;
    }
    private void add(View view) { body.addView(view,new LinearLayout.LayoutParams(-1,-2)); }
    private void space(int amount) { View v=new View(this); body.addView(v,new LinearLayout.LayoutParams(1,dp(amount))); }
    private void paragraph(String value) { add(text(value,15,MUTED)); space(12); }
    private void heading(String value) { TextView t=text(value,18,INK); t.setTypeface(null,Typeface.BOLD); add(t); space(8); }
    private GradientDrawable background(int color,int radius,int border) {
        GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); if(border!=0) g.setStroke(dp(1),border); return g;
    }
    private void page(String title,String subtitle,Runnable goBack) {
        back=goBack; homeVisible=false; editing=false; dirty=false;
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        // Explicitly fit status/navigation bars, including mandatory edge-to-edge on API 35.
        root.setOnApplyWindowInsetsListener((v,insets)-> {
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());
                v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout bar=new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(12),dp(5),dp(12),dp(5));
        if(goBack!=null) {
            Button b=new Button(this,null,android.R.attr.borderlessButtonStyle); b.setText("‹ Back"); b.setAllCaps(false); b.setTextColor(ACCENT); b.setMinHeight(dp(48));
            b.setOnClickListener(v->confirmLeave(goBack)); bar.addView(b,new LinearLayout.LayoutParams(-2,-2));
        } else {
            TextView brand=text("REFLECTION",13,ACCENT); brand.setTypeface(null,Typeface.BOLD); brand.setLetterSpacing(.12f); bar.addView(brand,new LinearLayout.LayoutParams(0,dp(44),1)); brand.setGravity(Gravity.CENTER_VERTICAL);
            Button menu=new Button(this,null,android.R.attr.borderlessButtonStyle); menu.setText("Help"); menu.setAllCaps(false); menu.setTextColor(ACCENT); menu.setOnClickListener(v->help()); bar.addView(menu,new LinearLayout.LayoutParams(-2,-2));
        }
        root.addView(bar,new LinearLayout.LayoutParams(-1,-2));
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(22),dp(8),dp(22),dp(32)); body.setFocusableInTouchMode(true);
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2)); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); root.requestApplyInsets();
        TextView t=text(title,30,INK); t.setTypeface(null,Typeface.BOLD); add(t); space(6);
        if(subtitle!=null&&!subtitle.isEmpty()) paragraph(subtitle);
        space(8);
    }
    private void confirmLeave(Runnable action) {
        if(editing&&dirty) new AlertDialog.Builder(this).setTitle("Discard unsaved answers?")
            .setMessage("Your completed reports are safe. Answers on this screen have not been submitted.")
            .setNegativeButton("Keep editing",null).setPositiveButton("Discard",(d,w)->action.run()).show();
        else action.run();
    }
    private void row(String title,String subtitle,Runnable action) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.HORIZONTAL); box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(15),dp(12),dp(15),dp(12)); box.setMinimumHeight(dp(58)); box.setBackground(background(PAPER,12,LINE));
        LinearLayout words=new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL);
        TextView name=text(title,16,INK); name.setTypeface(null,Typeface.BOLD); words.addView(name);
        if(subtitle!=null&&!subtitle.isEmpty()) { TextView desc=text(subtitle,13,MUTED); desc.setPadding(0,dp(4),0,0); words.addView(desc); }
        box.addView(words,new LinearLayout.LayoutParams(0,-2,1)); TextView chevron=text("›",26,ACCENT); box.addView(chevron);
        android.util.TypedValue ripple=new android.util.TypedValue(); getTheme().resolveAttribute(android.R.attr.selectableItemBackground,ripple,true); box.setForeground(getDrawable(ripple.resourceId));
        box.setClickable(true); box.setFocusable(true); box.setOnClickListener(v->action.run()); add(box); space(8);
    }
    private void button(String label,boolean primary,Runnable action) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(15); b.setMinHeight(dp(48));
        b.setTextColor(primary?Color.WHITE:ACCENT); b.setBackgroundTintList(ColorStateList.valueOf(primary?ACCENT:0xffe8f0eb));
        b.setOnClickListener(v->action.run()); add(b); space(8);
    }
    private void home() {
        store=new Store(this); LocalDate today=LocalDate.now();
        page("Your practice",DATE.format(today),null); homeVisible=true;
        heading("Categories");
        if(store.allCategories().isEmpty()) paragraph("No categories yet. Import a category JSON to get started.");
        for(JSONObject c:store.allCategories()) {
            String cid=c.optString("id"); row(c.optString("name"),null,()->category(cid));
        }
        space(10); heading("Reports due today");
        for(String cadence:Schedule.CADENCES) {
            int pending=store.dueOn(today,cadence,true).size(), total=store.dueOn(today,cadence,false).size(), late=store.overdue(cadence,today).size();
            String subtitle=total==0?"None scheduled today":pending+" remaining of "+total;
            if(late>0) subtitle+=" · "+late+" missed";
            row(Schedule.label(cadence)+" reports",subtitle,()->dueList(cadence));
        }
        space(12); heading("Manage");
        row("Import category JSON","Add a category or update its definition",this::importFile);
        row("Reminder settings",reminderSummary(),this::reminderSettings);
        row("How the app works","Instructions and JSON format",this::help);
    }
    private String reminderSummary() {
        if(!Reminders.notificationsEnabled(this)) return "Notifications disabled";
        return Reminders.exactEnabled(this)?"9 pm · exact alarms enabled":"9 pm · Android may delay reminders";
    }
    private void category(String cid) {
        JSONObject c=store.category(cid); if(c==null) { home(); return; }
        page(c.optString("name"),c.optString("description"),this::home);
        row("Practice & reminders",c.optJSONArray("review").length()+" things to revisit",()->review(cid));
        for(String cadence:Schedule.CADENCES) {
            space(8); heading(Schedule.label(cadence)+" reports"); boolean found=false;
            for(JSONObject r:store.reports(c)) if(cadence.equals(r.optString("cadence"))) {
                found=true; String rid=r.optString("id"); LocalDate next=store.nextDue(c,r,LocalDate.now());
                LocalDate late=store.overdue(c,r,LocalDate.now());
                row(r.optString("name")+" ("+DATE.format(next)+")",late==null?null:"Missed: "+DATE.format(late),()->reportOverview(cid,rid));
            }
            if(!found) paragraph("No reports configured.");
        }
        space(12); row("Report history",null,()->history(cid,null,()->category(cid)));
        button("Archive category",false,()->new AlertDialog.Builder(this).setTitle("Archive this category?").setMessage("It will no longer appear or send reminders. Completed reports remain stored and can be viewed if you import the same category again.")
            .setNegativeButton("Cancel",null).setPositiveButton("Archive",(d,w)->{try{store.archive(cid); Reminders.schedule(this); home();}catch(JSONException e){error(e.getMessage());}}).show());
    }
    private void review(String cid) {
        JSONObject c=store.category(cid); if(c==null){home();return;}
        page("Practice & reminders",c.optString("name"),()->category(cid));
        paragraph("Revisit these regularly. They are ongoing practices, not a permanent completion checklist.");
        JSONArray items=c.optJSONArray("review");
        if(items.length()==0) paragraph("No practice items configured.");
        for(int i=0;i<items.length();i++) {
            TextView t=text((i+1)+".  "+items.optString(i),17,INK); t.setPadding(dp(16),dp(14),dp(16),dp(14)); t.setBackground(background(PAPER,12,LINE)); add(t);space(10);
        }
    }
    private void dueList(String cadence) {
        LocalDate today=LocalDate.now(); page(Schedule.label(cadence)+" reports",DATE.format(today)+" · due 9 pm",this::home);
        List<Store.Due> all=store.dueOn(today,cadence,false); heading("Today");
        if(all.isEmpty()) paragraph("No reports due today.");
        for(Store.Due item:all) {
            boolean completed=store.completed(item.cid(),item.rid(),today);
            row(item.report.optString("name"),item.category.optString("name")+" · "+(completed?"Completed": "Due today"),
                ()->{if(completed) viewSubmission(store.submission(item.cid(),item.rid(),today),()->dueList(cadence)); else form(item.cid(),item.rid(),today,()->dueList(cadence));});
        }
        List<Store.Due> late=store.overdue(cadence,today);
        if(!late.isEmpty()) { space(12); heading("Most recent missed reports"); paragraph("Optional catch-up. Older missed periods are not queued.");
            for(Store.Due item:late) row(item.report.optString("name"),item.category.optString("name")+" · "+DATE.format(item.date),()->form(item.cid(),item.rid(),item.date,()->dueList(cadence)));
        }
    }
    private void reportOverview(String cid,String rid) {
        JSONObject c=store.category(cid), r=store.report(cid,rid); if(r==null){category(cid);return;}
        LocalDate today=LocalDate.now(), next=store.nextDue(c,r,today), late=store.overdue(c,r,today);
        page(r.optString("name"),c.optString("name")+" · "+Schedule.label(r.optString("cadence")),()->category(cid));
        if(r.has("description")) paragraph(r.optString("description"));
        heading("Next due: "+DATE.format(next)+", 9 pm");
        if(next.equals(today)) button("Complete today's report",true,()->form(cid,rid,today,()->reportOverview(cid,rid)));
        else { paragraph("The next questionnaire opens on its due date.");
            if(store.completed(cid,rid,today)) row("Today's completed report",null,()->viewSubmission(store.submission(cid,rid,today),()->reportOverview(cid,rid)));
        }
        if(late!=null) row("Complete most recent missed report",DATE.format(late),()->form(cid,rid,late,()->reportOverview(cid,rid)));
        row("Previous submissions",null,()->history(cid,rid,()->reportOverview(cid,rid)));
    }
    private void form(String cid,String rid,LocalDate due,Runnable returnTo) {
        JSONObject c=store.category(cid),r=store.report(cid,rid);
        if(r==null){returnTo.run();return;}
        JSONObject existing=store.submission(cid,rid,due);
        if(existing!=null){viewSubmission(existing,returnTo);return;}
        if(due.isAfter(LocalDate.now())){reportOverview(cid,rid);return;}
        page(r.optString("name"),DATE.format(due)+" · 9 pm",returnTo); editing=true;
        if(r.has("description")) paragraph(r.optString("description"));
        List<JSONObject> questions=Store.objects(r.optJSONArray("questions")); List<ReadAnswer> readers=new ArrayList<>();
        List<TextView> labels=new ArrayList<>();
        for(JSONObject q:questions) {
            boolean required=q.optBoolean("required",true);
            TextView label=text(q.optString("label")+(required?" *":" (optional)"),16,INK); label.setTypeface(null,Typeface.BOLD); add(label); labels.add(label);
            if(q.has("hint")){TextView hint=text(q.optString("hint"),13,MUTED);add(hint);}
            space(4);
            switch(q.optString("type")) {
                case "text": {
                    EditText edit=new EditText(this); edit.setTextSize(16); edit.setMinLines(2); edit.setMaxLines(7); edit.setGravity(Gravity.TOP); edit.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                    edit.setPadding(dp(10),dp(10),dp(10),dp(10)); edit.setBackground(background(PAPER,8,LINE)); edit.addTextChangedListener(new android.text.TextWatcher(){ public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){dirty=true;} public void afterTextChanged(android.text.Editable e){} }); add(edit);
                    readers.add(()->{String s=edit.getText().toString().trim();return s.isEmpty()?null:s;});break;
                }
                case "boolean": case "choice": case "scale": {
                    List<String> options=new ArrayList<>(); String type=q.optString("type");
                    if(type.equals("boolean")) {options.add("Yes");options.add("No");}
                    else if(type.equals("scale")) {
                        JSONArray ratingLabels=q.optJSONArray("labels");
                        if(ratingLabels==null) {for(int i=q.optInt("min");i<=q.optInt("max");i++) options.add(String.valueOf(i));}
                        else {for(int i=0;i<ratingLabels.length();i++) options.add(ratingLabels.optString(i)); Collections.reverse(options);}
                    }
                    else {JSONArray a=q.optJSONArray("options");for(int i=0;i<a.length();i++)options.add(a.optString(i));}
                    if(type.equals("scale") && !q.has("labels")) {
                        // A placeholder forces an intentional answer rather than silently recording a midpoint.
                        List<String> displayed=new ArrayList<>(); displayed.add("Select a rating"); displayed.addAll(options);
                        Spinner spin=new Spinner(this); ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,displayed);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spin.setAdapter(adapter);spin.setMinimumHeight(dp(48)); add(spin);
                        spin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(pos>0)dirty=true;}public void onNothingSelected(AdapterView<?> p){}});
                        readers.add(()->spin.getSelectedItemPosition()<=0?null:Integer.valueOf(options.get(spin.getSelectedItemPosition()-1)));
                    } else {
                        RadioGroup group=new RadioGroup(this);group.setOrientation(RadioGroup.VERTICAL);
                        if(type.equals("scale") && !required) options.add("Not applicable (skip)");
                        for(String option:options){RadioButton rb=new RadioButton(this);rb.setId(View.generateViewId());rb.setText(option);rb.setTextColor(INK);rb.setTextSize(15);rb.setMinHeight(dp(42));group.addView(rb);}
                        group.setOnCheckedChangeListener((g,id)->dirty=true); add(group);
                        readers.add(()->{int id=group.getCheckedRadioButtonId();if(id==-1)return null;RadioButton selected=group.findViewById(id);
                            if(type.equals("scale")) {
                                int index=group.indexOfChild(selected);
                                int count=q.optInt("max")-q.optInt("min")+1;
                                return index<0||index>=count?null:Integer.valueOf(q.optInt("max")-index);
                            }
                            String value=selected.getText().toString();return type.equals("boolean")?Boolean.valueOf(value.equals("Yes")):value;});
                    } break;
                }
                case "multiChoice": {
                    JSONArray a=q.optJSONArray("options");List<CheckBox> boxes=new ArrayList<>();
                    for(int i=0;i<a.length();i++){CheckBox cb=new CheckBox(this);cb.setText(a.optString(i));cb.setTextColor(INK);cb.setTextSize(15);cb.setMinHeight(dp(42));cb.setOnCheckedChangeListener((v,checked)->dirty=true);boxes.add(cb);add(cb);}
                    readers.add(()->{JSONArray selected=new JSONArray();for(CheckBox cb:boxes)if(cb.isChecked())selected.put(cb.getText().toString());return selected.length()==0?null:selected;});break;
                }
            }
            space(17);
        }
        button("Submit report",true,()->{
            try {
                JSONObject answers=new JSONObject();
                for(int i=0;i<questions.size();i++) {
                    JSONObject q=questions.get(i);Object value=readers.get(i).get();
                    if(value==null&&q.optBoolean("required",true)){labels.get(i).setError("Please answer this question");labels.get(i).requestFocus();error("Please answer: "+q.optString("label"));return;}
                    answers.put(q.optString("id"),value==null?JSONObject.NULL:value);
                }
                store.save(c,r,due,answers); dirty=false; editing=false; Reminders.schedule(this);
                Toast.makeText(this,"Report saved",Toast.LENGTH_SHORT).show(); returnTo.run();
            }catch(JSONException e){error(e.getMessage());}
        });
    }
    private void history(String cid,String rid,Runnable returnTo) {
        page("Report history",null,returnTo); List<JSONObject> entries=store.history(cid,rid);
        if(entries.isEmpty())paragraph("No completed reports yet.");
        for(JSONObject s:entries)row(s.optString("reportName"),DATE.format(LocalDate.parse(s.optString("dueDate"))),()->viewSubmission(s,()->history(cid,rid,returnTo)));
    }
    private void viewSubmission(JSONObject s,Runnable returnTo) {
        if(s==null){returnTo.run();return;}
        page(s.optString("reportName"),"Completed · "+DATE.format(LocalDate.parse(s.optString("dueDate"))),returnTo);
        JSONObject answers=s.optJSONObject("answers");
        for(JSONObject q:Store.objects(s.optJSONArray("questions"))) {
            heading(q.optString("label"));Object value=answers.opt(q.optString("id"));
            String display=value==null||value==JSONObject.NULL?"Not answered":value instanceof Boolean?((Boolean)value?"Yes":"No"):String.valueOf(value);
            if(value instanceof Number && "scale".equals(q.optString("type")) && q.has("labels")) {
                JSONArray ratingLabels=q.optJSONArray("labels");
                int index=((Number)value).intValue()-q.optInt("min");
                if(ratingLabels!=null && index>=0 && index<ratingLabels.length()) display=ratingLabels.optString(index,display);
            }
            if(value instanceof JSONArray){JSONArray a=(JSONArray)value;List<String> items=new ArrayList<>();for(int i=0;i<a.length();i++)items.add(a.optString(i));display=String.join(", ",items);}
            paragraph(display);space(5);
        }
    }
    private void importFile() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");
        startActivityForResult(i,IMPORT);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=IMPORT||result!=RESULT_OK||data==null||data.getData()==null)return;
        try {
            JSONObject c=new JSONObject(read(getContentResolver().openInputStream(data.getData()),1024*1024)); Store.validate(c);
            boolean update=store.category(c.getString("id"))!=null;
            new AlertDialog.Builder(this).setTitle(update?"Update category?":"Import category?")
                .setMessage(c.getString("name")+"\n\n"+(update?"Existing completed reports will be preserved.":"The first reporting period starts from today."))
                .setNegativeButton("Cancel",null).setPositiveButton(update?"Update":"Import",(d,w)->{try{store.importCategory(c);Reminders.schedule(this);home();Toast.makeText(this,"Category saved",Toast.LENGTH_SHORT).show();}catch(JSONException e){error(e.getMessage());}}).show();
        } catch(Exception e){error("Could not import JSON: "+e.getMessage());}
    }
    private static String read(InputStream stream,int limit)throws IOException {
        if(stream==null)throw new IOException("File cannot be opened");
        try(InputStream in=stream; ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192]; int n;while((n=in.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("File exceeds 1 MB");out.write(buffer,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8).replaceFirst("^\uFEFF","");
        }
    }
    private void reminderSettings() {
        page("Reminder settings","Reports are due at 9 pm in your device's local timezone.",this::home);
        heading("Notifications"); paragraph(Reminders.notificationsEnabled(this)?"Enabled":"Disabled. Allow notifications to receive reminders.");
        if(!Reminders.notificationsEnabled(this))button("Enable notifications",true,()->{
            if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED&&shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFY);
            else if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED&&!getPreferences(MODE_PRIVATE).getBoolean("notificationAsked",false)){getPreferences(MODE_PRIVATE).edit().putBoolean("notificationAsked",true).apply();requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFY);}
            else {permissionReturn=true;startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));}
        });
        heading("Exact alarm timing"); paragraph(Reminders.exactEnabled(this)?"Enabled. Android will aim to deliver at 9 pm.":"Not enabled. Reminders are still scheduled for 9 pm but Android may deliver them later.");
        if(!Reminders.exactEnabled(this)&&Build.VERSION.SDK_INT>=31)button("Allow alarms & reminders",true,()->{permissionReturn=true;try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(ActivityNotFoundException e){error("Open Android Settings > Apps > Special app access > Alarms & reminders.");}});
        paragraph("Daily: every day. Weekly: Sunday. Monthly: the last Sunday of each month. Completed reports do not generate a reminder. Android can delay or prevent notifications due to system or manufacturer settings, force stop, or disabled permissions.");
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==NOTIFY){Reminders.schedule(this);reminderSettings();}}
    private void help() {
        page("How Reflection works","App guide · JSON schema v1",this::home);
        try { String guide=read(getAssets().open("guide.txt"),1024*1024); TextView t=text(guide,15,INK);t.setTextIsSelectable(true);add(t); }
        catch(IOException e){paragraph("Guide could not be opened.");}
    }
    private void error(String message){new AlertDialog.Builder(this).setTitle("Reflection").setMessage(message==null?"Something went wrong.":message).setPositiveButton("OK",null).show();}
}
