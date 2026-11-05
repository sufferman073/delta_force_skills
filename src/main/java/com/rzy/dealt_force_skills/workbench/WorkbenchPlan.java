package com.rzy.dealt_force_skills.workbench;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.StringReader;
import java.io.IOException;
import java.util.HashSet;

/** Portable allocation only. Import never creates items, attachments, or arbitrary item metadata. */
public final class WorkbenchPlan {
    private WorkbenchPlan() { }
    public static String export(String item,int[] values) {
        if(WorkbenchData.balance(values)<0)throw new IllegalArgumentException("Unbalanced plan");
        var root=new JsonObject();root.addProperty("version",1);root.addProperty("item",item);var steps=new JsonObject();
        for(var stat:WorkbenchStat.values())if(values[stat.ordinal()]!=0)steps.addProperty(stat.key(),values[stat.ordinal()]);
        root.add("steps",steps);return root.toString();
    }
    public static int[] read(String text,String item) {
        if(text==null||text.length()>8192)throw new IllegalArgumentException("Plan too large");
        try(var reader=new JsonReader(new StringReader(text))) {
            reader.setLenient(false);
            var fields=new HashSet<String>();var out=new int[WorkbenchStat.values().length];
            reader.beginObject();
            while(reader.hasNext()) {
                String field=reader.nextName();
                if(!fields.add(field))throw new IllegalArgumentException("Duplicate field");
                switch(field) {
                    case "version" -> {
                        if(reader.peek()!=JsonToken.NUMBER||!reader.nextString().equals("1"))throw new IllegalArgumentException("Unknown format");
                    }
                    case "item" -> {
                        if(reader.peek()!=JsonToken.STRING||!reader.nextString().equals(item))throw new IllegalArgumentException("Different item");
                    }
                    case "steps" -> {
                        var seen=new HashSet<String>();reader.beginObject();
                        while(reader.hasNext()) {
                            String key=reader.nextName();WorkbenchStat found=null;
                            for(var stat:WorkbenchStat.values())if(stat.key().equals(key)){found=stat;break;}
                            if(found==null||!seen.add(key)||reader.peek()!=JsonToken.NUMBER)throw new IllegalArgumentException("Unknown or invalid stat");
                            String value=reader.nextString();
                            if(!value.matches("-?[0-9]{1,4}"))throw new IllegalArgumentException("Invalid step");
                            out[found.ordinal()]=Integer.parseInt(value);
                        }
                        reader.endObject();
                    }
                    default -> throw new IllegalArgumentException("Unknown field");
                }
            }
            reader.endObject();
            if(fields.size()!=3||reader.peek()!=JsonToken.END_DOCUMENT)throw new IllegalArgumentException("Incomplete plan");
            if(WorkbenchData.balance(out)<0)throw new IllegalArgumentException("Unbalanced plan");return out;
        } catch(IOException|RuntimeException ex) {throw new IllegalArgumentException("Invalid workbench plan",ex);}
    }
}
