package com.personal.reflection;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;

/** One local store. Import changes definitions, never historical submissions. */
final class Store {
    private final SharedPreferences prefs;
    private JSONObject data;
    Store(Context context) {
        prefs = context.getSharedPreferences("reflection", Context.MODE_PRIVATE);
        try {
            String raw = prefs.getString("state", null);
            if (raw == null) {
                data = new JSONObject();
                data.put("categories", new JSONObject());
                data.put("submissions", new JSONObject());
                data.put("starts", new JSONObject());
            } else data = new JSONObject(raw);
        } catch (JSONException e) { throw new IllegalStateException("Cannot read local data", e); }
    }
    private JSONObject categories() { return data.optJSONObject("categories"); }
    private JSONObject submissions() { return data.optJSONObject("submissions"); }
    private JSONObject starts() { return data.optJSONObject("starts"); }
    private void commit(JSONObject next) throws JSONException {
        if (!prefs.edit().putString("state", next.toString()).commit())
            throw new JSONException("Unable to save local data");
        data = next;
    }
    private JSONObject copy() throws JSONException { return new JSONObject(data.toString()); }
    static List<JSONObject> objects(JSONArray a) {
        List<JSONObject> result = new ArrayList<>();
        if (a != null) for (int i = 0; i < a.length(); i++) {
            JSONObject item = a.optJSONObject(i); if (item != null) result.add(item);
        }
        return result;
    }
    List<JSONObject> allCategories() {
        List<JSONObject> result = new ArrayList<>();
        Iterator<String> it = categories().keys();
        while (it.hasNext()) result.add(categories().optJSONObject(it.next()));
        result.sort(Comparator.comparing(c -> c.optString("name"), String.CASE_INSENSITIVE_ORDER));
        return result;
    }
    JSONObject category(String id) { return categories().optJSONObject(id); }
    List<JSONObject> reports(JSONObject c) { return objects(c.optJSONArray("reports")); }
    JSONObject report(String cid, String rid) {
        JSONObject c = category(cid);
        if (c != null) for (JSONObject r : reports(c)) if (rid.equals(r.optString("id"))) return r;
        return null;
    }
    static String key(String cid, String rid) { return cid + "/" + rid; }
    static String entryKey(String cid, String rid, LocalDate due) { return key(cid, rid) + "/" + due; }
    LocalDate start(String cid, String rid) {
        return LocalDate.parse(starts().optString(key(cid, rid), LocalDate.now().toString()));
    }
    JSONObject submission(String cid, String rid, LocalDate due) { return submissions().optJSONObject(entryKey(cid,rid,due)); }
    boolean completed(String cid, String rid, LocalDate due) { return submission(cid,rid,due) != null; }
    static final class Due {
        final JSONObject category, report;
        final LocalDate date;
        Due(JSONObject c, JSONObject r, LocalDate d) { category=c; report=r; date=d; }
        String cid() { return category.optString("id"); }
        String rid() { return report.optString("id"); }
    }
    List<Due> dueOn(LocalDate date, String cadence, boolean pendingOnly) {
        List<Due> result = new ArrayList<>();
        for (JSONObject c : allCategories()) for (JSONObject r : reports(c)) {
            String cid=c.optString("id"), rid=r.optString("id"), freq=r.optString("cadence");
            if (cadence != null && !cadence.equals(freq)) continue;
            if (date.isBefore(start(cid,rid)) || !Schedule.isDue(freq,date)) continue;
            if (!pendingOnly || !completed(cid,rid,date)) result.add(new Due(c,r,date));
        }
        return result;
    }
    /** At most one late period per active report; do not manufacture a backlog. */
    LocalDate overdue(JSONObject c, JSONObject r, LocalDate today) {
        String cid=c.optString("id"), rid=r.optString("id"), freq=r.optString("cadence");
        LocalDate date=Schedule.previousBefore(freq,today);
        if (date.isBefore(start(cid,rid)) || completed(cid,rid,date)) return null;
        return date;
    }
    List<Due> overdue(String cadence, LocalDate today) {
        List<Due> result = new ArrayList<>();
        for (JSONObject c : allCategories()) for (JSONObject r : reports(c)) {
            if (!cadence.equals(r.optString("cadence"))) continue;
            LocalDate date=overdue(c,r,today);
            if (date != null) result.add(new Due(c,r,date));
        }
        return result;
    }
    LocalDate nextDue(JSONObject c, JSONObject r, LocalDate today) {
        String cid=c.optString("id"), rid=r.optString("id"), cadence=r.optString("cadence");
        LocalDate date=Schedule.nextOnOrAfter(cadence,today.isBefore(start(cid,rid))?start(cid,rid):today);
        while (completed(cid,rid,date)) date=Schedule.nextOnOrAfter(cadence,date.plusDays(1));
        return date;
    }
    void save(JSONObject c, JSONObject r, LocalDate due, JSONObject answers) throws JSONException {
        String cid=c.optString("id"), rid=r.optString("id");
        if (due.isAfter(LocalDate.now()) || due.isBefore(start(cid,rid)) || !Schedule.isDue(r.optString("cadence"),due))
            throw new JSONException("This report is not open for that date.");
        JSONObject next=copy();
        JSONObject snapshot=new JSONObject();
        snapshot.put("categoryId",cid).put("categoryName",c.optString("name"))
            .put("reportId",rid).put("reportName",r.optString("name"))
            .put("cadence",r.optString("cadence")).put("dueDate",due.toString())
            .put("submittedAt",Instant.now().toString()).put("questions",r.getJSONArray("questions")).put("answers",answers);
        next.getJSONObject("submissions").put(entryKey(cid,rid,due),snapshot);
        commit(next);
    }
    List<JSONObject> history(String cid, String rid) {
        List<JSONObject> result=new ArrayList<>();
        Iterator<String> it=submissions().keys();
        while(it.hasNext()) {
            JSONObject s=submissions().optJSONObject(it.next());
            if(cid.equals(s.optString("categoryId")) && (rid==null || rid.equals(s.optString("reportId")))) result.add(s);
        }
        result.sort(Comparator.comparing((JSONObject s)->s.optString("dueDate")).reversed().thenComparing(s->s.optString("reportName")));
        return result;
    }
    void importCategory(JSONObject c) throws JSONException {
        validate(c);
        String cid=c.getString("id"); JSONObject previous=category(cid);
        JSONObject next=copy(); JSONObject newStarts=next.getJSONObject("starts");
        for (JSONObject r: reports(c)) {
            String rid=r.getString("id"), k=key(cid,rid);
            JSONObject old=previous==null?null:report(cid,rid);
            if(old!=null && !old.getString("cadence").equals(r.getString("cadence")))
                throw new JSONException("Report '"+rid+"' changed cadence. Give it a new ID.");
            if(old==null) {
                for(JSONObject past:history(cid,rid)) if(!past.optString("cadence").equals(r.getString("cadence")))
                    throw new JSONException("Report '"+rid+"' has history under a different cadence. Use a new ID.");
                newStarts.put(k,LocalDate.now().toString());
            }
        }
        next.getJSONObject("categories").put(cid,c);
        commit(next);
    }
    void archive(String cid) throws JSONException {
        JSONObject next=copy(); next.getJSONObject("categories").remove(cid); commit(next);
    }
    private static String requiredString(JSONObject o,String field,int max) throws JSONException {
        Object v=o.opt(field);
        if(!(v instanceof String) || ((String)v).trim().isEmpty() || ((String)v).length()>max)
            throw new JSONException("Invalid or missing '"+field+"' (max "+max+" characters)");
        return (String)v;
    }
    private static String id(JSONObject o) throws JSONException {
        String value=requiredString(o,"id",64);
        if(!value.matches("[a-zA-Z0-9][a-zA-Z0-9_-]*")) throw new JSONException("Invalid ID: "+value);
        return value;
    }
    private static JSONArray array(JSONObject o,String field,int max) throws JSONException {
        JSONArray a=o.optJSONArray(field);
        if(a==null || a.length()>max) throw new JSONException("'"+field+"' must be an array with at most "+max+" items");
        return a;
    }
    private static int integer(JSONObject o,String field) throws JSONException {
        Object value=o.opt(field);
        if(!(value instanceof Number)) throw new JSONException("'"+field+"' must be an integer");
        double n=((Number)value).doubleValue();
        if(n!=Math.rint(n) || n<Integer.MIN_VALUE || n>Integer.MAX_VALUE) throw new JSONException("'"+field+"' must be an integer");
        return (int)n;
    }
    static void validate(JSONObject c) throws JSONException {
        if(integer(c,"schemaVersion")!=1) throw new JSONException("Only schemaVersion 1 is supported");
        id(c); requiredString(c,"name",100);
        if(c.has("description")) requiredString(c,"description",3000);
        JSONArray review=array(c,"review",100);
        for(int i=0;i<review.length();i++) {
            if(!(review.get(i) instanceof String) || review.getString(i).trim().isEmpty() || review.getString(i).length()>2000)
                throw new JSONException("Invalid review item "+(i+1));
        }
        JSONArray reports=array(c,"reports",60); Set<String> rids=new HashSet<>();
        for(int i=0;i<reports.length();i++) {
            JSONObject r=reports.getJSONObject(i); String rid=id(r);
            if(!rids.add(rid)) throw new JSONException("Duplicate report ID: "+rid);
            requiredString(r,"name",100);
            if(!Arrays.asList(Schedule.CADENCES).contains(requiredString(r,"cadence",20))) throw new JSONException("Unknown cadence in "+rid);
            if(r.has("description")) requiredString(r,"description",3000);
            JSONArray questions=array(r,"questions",50); Set<String> qids=new HashSet<>();
            if(questions.length()==0) throw new JSONException("Report needs at least one question: "+rid);
            for(int j=0;j<questions.length();j++) {
                JSONObject q=questions.getJSONObject(j); String qid=id(q);
                if(!qids.add(qid)) throw new JSONException("Duplicate question ID: "+qid);
                requiredString(q,"label",500);
                String type=requiredString(q,"type",20);
                if(q.has("required") && !(q.opt("required") instanceof Boolean)) throw new JSONException("'required' must be true or false in "+qid);
                if(q.has("hint")) requiredString(q,"hint",1000);
                switch(type) {
                    case "text": case "boolean": break;
                    case "choice": case "multiChoice": {
                        JSONArray options=array(q,"options",30); Set<String> unique=new HashSet<>();
                        if(options.length()<2) throw new JSONException("At least two options required in "+qid);
                        for(int k=0;k<options.length();k++) {
                            Object v=options.get(k);
                            if(!(v instanceof String) || ((String)v).trim().isEmpty() || ((String)v).length()>200 || !unique.add((String)v))
                                throw new JSONException("Invalid or duplicate option in "+qid);
                        }
                        break;
                    }
                    case "scale": {
                        int min=integer(q,"min"), max=integer(q,"max");
                        if(min>=max || min< -1000 || max>1000 || max-min>100) throw new JSONException("Invalid scale range in "+qid);
                        if(q.has("labels")) {
                            JSONArray labels=array(q,"labels",101); Set<String> unique=new HashSet<>();
                            if(labels.length()!=max-min+1) throw new JSONException("One label per scale value required in "+qid);
                            for(int k=0;k<labels.length();k++) {
                                Object v=labels.get(k);
                                if(!(v instanceof String) || ((String)v).trim().isEmpty() || ((String)v).length()>200 || !unique.add((String)v) || (!q.optBoolean("required",true) && "Not applicable (skip)".equals(v)))
                                    throw new JSONException("Invalid or duplicate scale label in "+qid);
                            }
                        }
                        break;
                    }
                    default: throw new JSONException("Unknown question type: "+type);
                }
            }
        }
    }
}
