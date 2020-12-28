package io.github.trojan_gfw.igniter;

import android.util.Log;

import com.google.gson.Gson;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TrojanURLHelper {
    private static String TAG="TAG";

    public static String GenerateTrojanURL(TrojanConfig trojanConfig) {

        URI trojanUri;
        try {
            String serverRemark = trojanConfig.getRemoteServerRemark();
            trojanUri = new URI("trojan",
                    trojanConfig.getPassword(),
                    trojanConfig.getRemoteAddr(),
                    trojanConfig.getRemotePort(),
                    null, null,
                    (serverRemark == null || serverRemark.length() <= 0) ? null : serverRemark);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return trojanUri.toASCIIString();
    }

    public static TrojanURLParseResult ParseTrojanURL(String trojanURLStr) {
        URI trojanUri;
        try {
            trojanUri = new URI(trojanURLStr);
        } catch (java.net.URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        String scheme = trojanUri.getScheme();
        if (scheme == null) {
            return null;
        }
        if (!scheme.equals("trojan"))
            return null;
        String host = trojanUri.getHost();
        int port = trojanUri.getPort();
        String userInfo = trojanUri.getUserInfo();
        String serverRemark = trojanUri.getFragment();
        serverRemark = TrojanHelper.RemoveAllEmoji(serverRemark);

        TrojanURLParseResult retConfig = new TrojanURLParseResult();
        retConfig.host = host;
        retConfig.port = port;
        retConfig.password = userInfo;
        retConfig.serverRemark = serverRemark;
        return retConfig;
    }

    public static List<TrojanURLParseResult> ParseMultipleTrojanURL(String inputStr,String split) {
        ArrayList<TrojanURLParseResult> ret = new ArrayList<TrojanURLParseResult>(5);
        if (split==null||split.equals("")) split="\\R+";
        String[] trojanURLLines = inputStr.split(split);
        Log.d(TAG, "ParseMultipleTrojanURL: "+split+ Arrays.toString(trojanURLLines));

        for (String trojanURLLine : trojanURLLines) {
            TrojanURLParseResult parseResult = TrojanURLHelper.ParseTrojanURL(trojanURLLine);
            if (parseResult != null) {
                ret.add(parseResult);
            }
        }
        return ret;
    }

    public static List<TrojanConfig> ParseTrojanConfigsFromContent(String content,String split) {
        Log.d(TAG, "ParseTrojanConfigsFromContent() called with: content = [" + content + "], split = [" + split + "]");
        ArrayList<TrojanConfig> ret = new ArrayList<TrojanConfig>(5);
        List<TrojanURLParseResult> parseResults = ParseMultipleTrojanURL(content,split);
        for (TrojanURLParseResult singleParseResult : parseResults) {
            TrojanConfig newConfig = CombineTrojanURLParseResultToTrojanConfig(singleParseResult, Globals.getTrojanConfigInstance());
            ret.add(newConfig);
        }
        return ret;
    }

    /**
     * Merge Trojan URL parse result and Trojan config instance
     *
     * @param srcParseResult parsed Trojan URL
     * @param srcConfig      the source Trojan config instance
     * @return A deep clone of the source Trojan config instance
     */
    public static TrojanConfig CombineTrojanURLParseResultToTrojanConfig(TrojanURLParseResult srcParseResult,
                                                                         TrojanConfig srcConfig) {
        if (srcConfig == null || srcParseResult == null) {
            return null;
        }
        Gson gson = new Gson();

        TrojanConfig dstConfig = gson.fromJson(gson.toJson(srcConfig), TrojanConfig.class);
        TrojanURLParseResult dstParseResult = gson.fromJson(gson.toJson(srcParseResult), TrojanURLParseResult.class);
        dstConfig.setRemoteAddr(dstParseResult.host);
        dstConfig.setRemotePort(dstParseResult.port);
        dstConfig.setPassword(dstParseResult.password);
        if (dstParseResult.serverRemark != null && dstParseResult.serverRemark.length() > 0)
            dstConfig.setRemoteServerRemark(dstParseResult.serverRemark);
        return dstConfig;
    }
}
