package de.shellfire.vpn.android.utils;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import de.shellfire.vpn.android.Country;
import de.shellfire.vpn.android.CountryMap;
import de.shellfire.vpn.android.R;

public class CountryUtils {
    private static final Map<String, Integer> COUNTRY_FLAG_MAP = new HashMap<>();
    private static final String TAG = "CountryUtils";

    static {
        COUNTRY_FLAG_MAP.put("ad", R.drawable.ad);
        COUNTRY_FLAG_MAP.put("ae", R.drawable.ae);
        COUNTRY_FLAG_MAP.put("af", R.drawable.af);
        COUNTRY_FLAG_MAP.put("ag", R.drawable.ag);
        COUNTRY_FLAG_MAP.put("ai", R.drawable.ai);
        COUNTRY_FLAG_MAP.put("al", R.drawable.al);
        COUNTRY_FLAG_MAP.put("am", R.drawable.am);
        COUNTRY_FLAG_MAP.put("an", R.drawable.an);
        COUNTRY_FLAG_MAP.put("ao", R.drawable.ao);
        COUNTRY_FLAG_MAP.put("aq", R.drawable.aq);
        COUNTRY_FLAG_MAP.put("ar", R.drawable.ar);
        COUNTRY_FLAG_MAP.put("as", R.drawable.as);
        COUNTRY_FLAG_MAP.put("at", R.drawable.at);
        COUNTRY_FLAG_MAP.put("au", R.drawable.au);
        COUNTRY_FLAG_MAP.put("aw", R.drawable.aw);
        COUNTRY_FLAG_MAP.put("ax", R.drawable.ax);
        COUNTRY_FLAG_MAP.put("az", R.drawable.az);
        COUNTRY_FLAG_MAP.put("ba", R.drawable.ba);
        COUNTRY_FLAG_MAP.put("bb", R.drawable.bb);
        COUNTRY_FLAG_MAP.put("bd", R.drawable.bd);
        COUNTRY_FLAG_MAP.put("be", R.drawable.be);
        COUNTRY_FLAG_MAP.put("bf", R.drawable.bf);
        COUNTRY_FLAG_MAP.put("bg", R.drawable.bg);
        COUNTRY_FLAG_MAP.put("bh", R.drawable.bh);
        COUNTRY_FLAG_MAP.put("bi", R.drawable.bi);
        COUNTRY_FLAG_MAP.put("bj", R.drawable.bj);
        COUNTRY_FLAG_MAP.put("bl", R.drawable.bl);
        COUNTRY_FLAG_MAP.put("bm", R.drawable.bm);
        COUNTRY_FLAG_MAP.put("bn", R.drawable.bn);
        COUNTRY_FLAG_MAP.put("bo", R.drawable.bo);
        COUNTRY_FLAG_MAP.put("bq", R.drawable.bq);
        COUNTRY_FLAG_MAP.put("br", R.drawable.br);
        COUNTRY_FLAG_MAP.put("bs", R.drawable.bs);
        COUNTRY_FLAG_MAP.put("bt", R.drawable.bt);
        COUNTRY_FLAG_MAP.put("bv", R.drawable.bv);
        COUNTRY_FLAG_MAP.put("bw", R.drawable.bw);
        COUNTRY_FLAG_MAP.put("by", R.drawable.by);
        COUNTRY_FLAG_MAP.put("bz", R.drawable.bz);
        COUNTRY_FLAG_MAP.put("ca", R.drawable.ca);
        COUNTRY_FLAG_MAP.put("cc", R.drawable.cc);
        COUNTRY_FLAG_MAP.put("cd", R.drawable.cd);
        COUNTRY_FLAG_MAP.put("cf", R.drawable.cf);
        COUNTRY_FLAG_MAP.put("cg", R.drawable.cg);
        COUNTRY_FLAG_MAP.put("ch", R.drawable.ch);
        COUNTRY_FLAG_MAP.put("ci", R.drawable.ci);
        COUNTRY_FLAG_MAP.put("ck", R.drawable.ck);
        COUNTRY_FLAG_MAP.put("cl", R.drawable.cl);
        COUNTRY_FLAG_MAP.put("cm", R.drawable.cm);
        COUNTRY_FLAG_MAP.put("cn", R.drawable.cn);
        COUNTRY_FLAG_MAP.put("co", R.drawable.co);
        COUNTRY_FLAG_MAP.put("cr", R.drawable.cr);
        COUNTRY_FLAG_MAP.put("cu", R.drawable.cu);
        COUNTRY_FLAG_MAP.put("cv", R.drawable.cv);
        COUNTRY_FLAG_MAP.put("cw", R.drawable.cw);
        COUNTRY_FLAG_MAP.put("cx", R.drawable.cx);
        COUNTRY_FLAG_MAP.put("cy", R.drawable.cy);
        COUNTRY_FLAG_MAP.put("cz", R.drawable.cz);
        COUNTRY_FLAG_MAP.put("de", R.drawable.de);
        COUNTRY_FLAG_MAP.put("dj", R.drawable.dj);
        COUNTRY_FLAG_MAP.put("dk", R.drawable.dk);
        COUNTRY_FLAG_MAP.put("dm", R.drawable.dm);
        COUNTRY_FLAG_MAP.put("dz", R.drawable.dz);
        COUNTRY_FLAG_MAP.put("ec", R.drawable.ec);
        COUNTRY_FLAG_MAP.put("ee", R.drawable.ee);
        COUNTRY_FLAG_MAP.put("eg", R.drawable.eg);
        COUNTRY_FLAG_MAP.put("eh", R.drawable.eh);
        COUNTRY_FLAG_MAP.put("er", R.drawable.er);
        COUNTRY_FLAG_MAP.put("es", R.drawable.es);
        COUNTRY_FLAG_MAP.put("et", R.drawable.et);
        COUNTRY_FLAG_MAP.put("fi", R.drawable.fi);
        COUNTRY_FLAG_MAP.put("fj", R.drawable.fj);
        COUNTRY_FLAG_MAP.put("fk", R.drawable.fk);
        COUNTRY_FLAG_MAP.put("fm", R.drawable.fm);
        COUNTRY_FLAG_MAP.put("fo", R.drawable.fo);
        COUNTRY_FLAG_MAP.put("fr", R.drawable.fr);
        COUNTRY_FLAG_MAP.put("ga", R.drawable.ga);
        COUNTRY_FLAG_MAP.put("gb", R.drawable.gb);
        COUNTRY_FLAG_MAP.put("gd", R.drawable.gd);
        COUNTRY_FLAG_MAP.put("ge", R.drawable.ge);
        COUNTRY_FLAG_MAP.put("gf", R.drawable.gf);
        COUNTRY_FLAG_MAP.put("gg", R.drawable.gg);
        COUNTRY_FLAG_MAP.put("gh", R.drawable.gh);
        COUNTRY_FLAG_MAP.put("gi", R.drawable.gi);
        COUNTRY_FLAG_MAP.put("gl", R.drawable.gl);
        COUNTRY_FLAG_MAP.put("gm", R.drawable.gm);
        COUNTRY_FLAG_MAP.put("gn", R.drawable.gn);
        COUNTRY_FLAG_MAP.put("gp", R.drawable.gp);
        COUNTRY_FLAG_MAP.put("gq", R.drawable.gq);
        COUNTRY_FLAG_MAP.put("gr", R.drawable.gr);
        COUNTRY_FLAG_MAP.put("gs", R.drawable.gs);
        COUNTRY_FLAG_MAP.put("gt", R.drawable.gt);
        COUNTRY_FLAG_MAP.put("gu", R.drawable.gu);
        COUNTRY_FLAG_MAP.put("gw", R.drawable.gw);
        COUNTRY_FLAG_MAP.put("gy", R.drawable.gy);
        COUNTRY_FLAG_MAP.put("hk", R.drawable.hk);
        COUNTRY_FLAG_MAP.put("hm", R.drawable.hm);
        COUNTRY_FLAG_MAP.put("hn", R.drawable.hn);
        COUNTRY_FLAG_MAP.put("hr", R.drawable.hr);
        COUNTRY_FLAG_MAP.put("ht", R.drawable.ht);
        COUNTRY_FLAG_MAP.put("hu", R.drawable.hu);
        COUNTRY_FLAG_MAP.put("id", R.drawable.id);
        COUNTRY_FLAG_MAP.put("ie", R.drawable.ie);
        COUNTRY_FLAG_MAP.put("il", R.drawable.il);
        COUNTRY_FLAG_MAP.put("im", R.drawable.im);
        COUNTRY_FLAG_MAP.put("in", R.drawable.in);
        COUNTRY_FLAG_MAP.put("io", R.drawable.io);
        COUNTRY_FLAG_MAP.put("iq", R.drawable.iq);
        COUNTRY_FLAG_MAP.put("ir", R.drawable.ir);
        COUNTRY_FLAG_MAP.put("is", R.drawable.is);
        COUNTRY_FLAG_MAP.put("it", R.drawable.it);
        COUNTRY_FLAG_MAP.put("je", R.drawable.je);
        COUNTRY_FLAG_MAP.put("jm", R.drawable.jm);
        COUNTRY_FLAG_MAP.put("jo", R.drawable.jo);
        COUNTRY_FLAG_MAP.put("jp", R.drawable.jp);
        COUNTRY_FLAG_MAP.put("ke", R.drawable.ke);
        COUNTRY_FLAG_MAP.put("kg", R.drawable.kg);
        COUNTRY_FLAG_MAP.put("kh", R.drawable.kh);
        COUNTRY_FLAG_MAP.put("ki", R.drawable.ki);
        COUNTRY_FLAG_MAP.put("km", R.drawable.km);
        COUNTRY_FLAG_MAP.put("kn", R.drawable.kn);
        COUNTRY_FLAG_MAP.put("kp", R.drawable.kp);
        COUNTRY_FLAG_MAP.put("kr", R.drawable.kr);
        COUNTRY_FLAG_MAP.put("kw", R.drawable.kw);
        COUNTRY_FLAG_MAP.put("ky", R.drawable.ky);
        COUNTRY_FLAG_MAP.put("kz", R.drawable.kz);
        COUNTRY_FLAG_MAP.put("la", R.drawable.la);
        COUNTRY_FLAG_MAP.put("lb", R.drawable.lb);
        COUNTRY_FLAG_MAP.put("lc", R.drawable.lc);
        COUNTRY_FLAG_MAP.put("li", R.drawable.li);
        COUNTRY_FLAG_MAP.put("lk", R.drawable.lk);
        COUNTRY_FLAG_MAP.put("lr", R.drawable.lr);
        COUNTRY_FLAG_MAP.put("ls", R.drawable.ls);
        COUNTRY_FLAG_MAP.put("lt", R.drawable.lt);
        COUNTRY_FLAG_MAP.put("lu", R.drawable.lu);
        COUNTRY_FLAG_MAP.put("lv", R.drawable.lv);
        COUNTRY_FLAG_MAP.put("ly", R.drawable.ly);
        COUNTRY_FLAG_MAP.put("ma", R.drawable.ma);
        COUNTRY_FLAG_MAP.put("mc", R.drawable.mc);
        COUNTRY_FLAG_MAP.put("md", R.drawable.md);
        COUNTRY_FLAG_MAP.put("me", R.drawable.me);
        COUNTRY_FLAG_MAP.put("mf", R.drawable.mf);
        COUNTRY_FLAG_MAP.put("mg", R.drawable.mg);
        COUNTRY_FLAG_MAP.put("mh", R.drawable.mh);
        COUNTRY_FLAG_MAP.put("mk", R.drawable.mk);
        COUNTRY_FLAG_MAP.put("ml", R.drawable.ml);
        COUNTRY_FLAG_MAP.put("mm", R.drawable.mm);
        COUNTRY_FLAG_MAP.put("mn", R.drawable.mn);
        COUNTRY_FLAG_MAP.put("mo", R.drawable.mo);
        COUNTRY_FLAG_MAP.put("mp", R.drawable.mp);
        COUNTRY_FLAG_MAP.put("mq", R.drawable.mq);
        COUNTRY_FLAG_MAP.put("mr", R.drawable.mr);
        COUNTRY_FLAG_MAP.put("ms", R.drawable.ms);
        COUNTRY_FLAG_MAP.put("mt", R.drawable.mt);
        COUNTRY_FLAG_MAP.put("mu", R.drawable.mu);
        COUNTRY_FLAG_MAP.put("mv", R.drawable.mv);
        COUNTRY_FLAG_MAP.put("mw", R.drawable.mw);
        COUNTRY_FLAG_MAP.put("mx", R.drawable.mx);
        COUNTRY_FLAG_MAP.put("my", R.drawable.my);
        COUNTRY_FLAG_MAP.put("mz", R.drawable.mz);
        COUNTRY_FLAG_MAP.put("na", R.drawable.na);
        COUNTRY_FLAG_MAP.put("nc", R.drawable.nc);
        COUNTRY_FLAG_MAP.put("ne", R.drawable.ne);
        COUNTRY_FLAG_MAP.put("nf", R.drawable.nf);
        COUNTRY_FLAG_MAP.put("ng", R.drawable.ng);
        COUNTRY_FLAG_MAP.put("ni", R.drawable.ni);
        COUNTRY_FLAG_MAP.put("nl", R.drawable.nl);
        COUNTRY_FLAG_MAP.put("no", R.drawable.no);
        COUNTRY_FLAG_MAP.put("np", R.drawable.np);
        COUNTRY_FLAG_MAP.put("nr", R.drawable.nr);
        COUNTRY_FLAG_MAP.put("nu", R.drawable.nu);
        COUNTRY_FLAG_MAP.put("nz", R.drawable.nz);
        COUNTRY_FLAG_MAP.put("om", R.drawable.om);
        COUNTRY_FLAG_MAP.put("pa", R.drawable.pa);
        COUNTRY_FLAG_MAP.put("pe", R.drawable.pe);
        COUNTRY_FLAG_MAP.put("pf", R.drawable.pf);
        COUNTRY_FLAG_MAP.put("pg", R.drawable.pg);
        COUNTRY_FLAG_MAP.put("ph", R.drawable.ph);
        COUNTRY_FLAG_MAP.put("pk", R.drawable.pk);
        COUNTRY_FLAG_MAP.put("pl", R.drawable.pl);
        COUNTRY_FLAG_MAP.put("pm", R.drawable.pm);
        COUNTRY_FLAG_MAP.put("pn", R.drawable.pn);
        COUNTRY_FLAG_MAP.put("pr", R.drawable.pr);
        COUNTRY_FLAG_MAP.put("ps", R.drawable.ps);
        COUNTRY_FLAG_MAP.put("pt", R.drawable.pt);
        COUNTRY_FLAG_MAP.put("pw", R.drawable.pw);
        COUNTRY_FLAG_MAP.put("py", R.drawable.py);
        COUNTRY_FLAG_MAP.put("qa", R.drawable.qa);
        COUNTRY_FLAG_MAP.put("re", R.drawable.re);
        COUNTRY_FLAG_MAP.put("rn", R.drawable.rn);
        COUNTRY_FLAG_MAP.put("ro", R.drawable.ro);
        COUNTRY_FLAG_MAP.put("rs", R.drawable.rs);
        COUNTRY_FLAG_MAP.put("ru", R.drawable.ru);
        COUNTRY_FLAG_MAP.put("rw", R.drawable.rw);
        COUNTRY_FLAG_MAP.put("sa", R.drawable.sa);
        COUNTRY_FLAG_MAP.put("sb", R.drawable.sb);
        COUNTRY_FLAG_MAP.put("sc", R.drawable.sc);
        COUNTRY_FLAG_MAP.put("sd", R.drawable.sd);
        COUNTRY_FLAG_MAP.put("se", R.drawable.se);
        COUNTRY_FLAG_MAP.put("sg", R.drawable.sg);
        COUNTRY_FLAG_MAP.put("sh", R.drawable.sh);
        COUNTRY_FLAG_MAP.put("si", R.drawable.si);
        COUNTRY_FLAG_MAP.put("sj", R.drawable.sj);
        COUNTRY_FLAG_MAP.put("sk", R.drawable.sk);
        COUNTRY_FLAG_MAP.put("sl", R.drawable.sl);
        COUNTRY_FLAG_MAP.put("sm", R.drawable.sm);
        COUNTRY_FLAG_MAP.put("sn", R.drawable.sn);
        COUNTRY_FLAG_MAP.put("so", R.drawable.so);
        COUNTRY_FLAG_MAP.put("sr", R.drawable.sr);
        COUNTRY_FLAG_MAP.put("ss", R.drawable.ss);
        COUNTRY_FLAG_MAP.put("st", R.drawable.st);
        COUNTRY_FLAG_MAP.put("sv", R.drawable.sv);
        COUNTRY_FLAG_MAP.put("sx", R.drawable.sx);
        COUNTRY_FLAG_MAP.put("sy", R.drawable.sy);
        COUNTRY_FLAG_MAP.put("sz", R.drawable.sz);
        COUNTRY_FLAG_MAP.put("tc", R.drawable.tc);
        COUNTRY_FLAG_MAP.put("td", R.drawable.td);
        COUNTRY_FLAG_MAP.put("tf", R.drawable.tf);
        COUNTRY_FLAG_MAP.put("tg", R.drawable.tg);
        COUNTRY_FLAG_MAP.put("th", R.drawable.th);
        COUNTRY_FLAG_MAP.put("tj", R.drawable.tj);
        COUNTRY_FLAG_MAP.put("tk", R.drawable.tk);
        COUNTRY_FLAG_MAP.put("tl", R.drawable.tl);
        COUNTRY_FLAG_MAP.put("tm", R.drawable.tm);
        COUNTRY_FLAG_MAP.put("to", R.drawable.to);
        COUNTRY_FLAG_MAP.put("tr", R.drawable.tr);
        COUNTRY_FLAG_MAP.put("tt", R.drawable.tt);
        COUNTRY_FLAG_MAP.put("tv", R.drawable.tv);
        COUNTRY_FLAG_MAP.put("tw", R.drawable.tw);
        COUNTRY_FLAG_MAP.put("tz", R.drawable.tz);
        COUNTRY_FLAG_MAP.put("ua", R.drawable.ua);
        COUNTRY_FLAG_MAP.put("ug", R.drawable.ug);
        COUNTRY_FLAG_MAP.put("um", R.drawable.um);
        COUNTRY_FLAG_MAP.put("us", R.drawable.us);
        COUNTRY_FLAG_MAP.put("uy", R.drawable.uy);
        COUNTRY_FLAG_MAP.put("uz", R.drawable.uz);
        COUNTRY_FLAG_MAP.put("va", R.drawable.va);
        COUNTRY_FLAG_MAP.put("vc", R.drawable.vc);
        COUNTRY_FLAG_MAP.put("ve", R.drawable.ve);
        COUNTRY_FLAG_MAP.put("vg", R.drawable.vg);
        COUNTRY_FLAG_MAP.put("vi", R.drawable.vi);
        COUNTRY_FLAG_MAP.put("vn", R.drawable.vn);
        COUNTRY_FLAG_MAP.put("vu", R.drawable.vu);
        COUNTRY_FLAG_MAP.put("wf", R.drawable.wf);
        COUNTRY_FLAG_MAP.put("ws", R.drawable.ws);
        COUNTRY_FLAG_MAP.put("ye", R.drawable.ye);
        COUNTRY_FLAG_MAP.put("yt", R.drawable.yt);
        COUNTRY_FLAG_MAP.put("za", R.drawable.za);
        COUNTRY_FLAG_MAP.put("zm", R.drawable.zm);
        COUNTRY_FLAG_MAP.put("zw", R.drawable.zw);
    }

    public static int getCountryFlagImageResId(String isoCode) {
        Integer resId = COUNTRY_FLAG_MAP.get(isoCode.toLowerCase());
        if (resId != null) {
            return resId;
        } else {
            Log.e(TAG, "No flag found for ISO code: " + isoCode);
            return 0; // or a default flag resource ID
        }
    }

    public static int getCountryFlagImageResId(Country country) {
        String isoCode = CountryMap.get(country);
        if (isoCode != null) {
            isoCode = isoCode.toLowerCase();
        }
        return getCountryFlagImageResId(isoCode);
    }

}
