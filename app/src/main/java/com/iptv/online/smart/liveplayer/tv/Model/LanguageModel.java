package com.iptv.online.smart.liveplayer.tv.Model;

public class LanguageModel {

    public String s_lan_code;
    public String s_lan_name;
    public boolean select;

    public LanguageModel(String str, String str2, boolean z) {
        s_lan_code = str;
        s_lan_name = str2;
        select = z;

    }

    public void setSelect(boolean z) {
        select = z;
    }

}
