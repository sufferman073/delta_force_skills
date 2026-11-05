package com.rzy.dealt_force_skills.client.screen;
import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;
/** Named client presets contain the existing validated plan format. */
final class WorkbenchPresets {
    private static Path path(){return net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("config/dealt_workbench_presets.json");}
    static Map<String,String> read() {
        Map<String,String> out=new LinkedHashMap<>();if(!Files.isRegularFile(path()))return out;
        try(var reader=Files.newBufferedReader(path(),StandardCharsets.UTF_8)) {
            var object=new JsonParser().parse(reader).getAsJsonObject();
            for(var entry:object.entrySet())if(entry.getValue().isJsonPrimitive()&&entry.getKey().length()<=64)out.put(entry.getKey(),entry.getValue().getAsString());
        } catch(java.io.IOException|RuntimeException failure){com.mojang.logging.LogUtils.getLogger().warn("Cannot read workbench presets {}",path(),failure);}
        return out;
    }
    static void save(Map<String,String> plans) throws java.io.IOException {
        var object=new JsonObject();plans.forEach(object::addProperty);Files.createDirectories(path().getParent());
        Path temporary=path().resolveSibling("dealt_workbench_presets.json.tmp");
        Files.writeString(temporary,new GsonBuilder().setPrettyPrinting().create().toJson(object),StandardCharsets.UTF_8);
        Files.move(temporary,path(),StandardCopyOption.REPLACE_EXISTING);
    }
}
