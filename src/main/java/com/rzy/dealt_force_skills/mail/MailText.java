package com.rzy.dealt_force_skills.mail;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Only native visual formatting is accepted; mail cannot inject click events or commands. */
public final class MailText {
    private MailText() { }

    public static Component parse(String input) {
        MutableComponent result=Component.empty();
        Style style=Style.EMPTY;
        StringBuilder part=new StringBuilder();
        for(int i=0;i<input.length();i++) {
            char c=input.charAt(i);
            if((c=='&'||c=='\u00a7')&&i+1<input.length()) {
                char next=input.charAt(i+1);
                if(c=='&'&&next=='&'){part.append('&');i++;continue;}
                ChatFormatting format=ChatFormatting.getByCode(Character.toLowerCase(next));
                if(format!=null) {
                    if(part.length()>0)result.append(Component.literal(part.toString()).setStyle(style));
                    part.setLength(0);
                    style=format==ChatFormatting.RESET?Style.EMPTY:style.applyLegacyFormat(format);
                    i++;continue;
                }
            }
            part.append(c);
        }
        return part.length()>0?result.append(Component.literal(part.toString()).setStyle(style)):result;
    }
}
