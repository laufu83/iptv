package cn.fred.mytv;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChannelApi {
    public static List<ChannelInfo> parse(List<String> list) {
        List<ChannelInfo> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String line = list.get(i);
            if (i == 0) {
                if (!line.contains("#EXTM3U")) {
                    Log.e("parseTV", "解析失败,首行不是#EXTM3U");
                    break;
                }
            } else {
                // 正则表达式，tvg-logo部分是可选的(?:...)?
                String regex = "#EXTINF:[^,]*" +
                        "\\s+tvg-name=\"([^\"]+)\"" +
                        "(?:\\s+tvg-logo=\"([^\"]+)\")?" +  // 可选部分
                        "\\s+group-title=\"([^\"]+)\"" +
                        "\\s*,(.+)";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    if (i >= list.size() - 1) {
                        break;
                    }
                    String url = list.get(++i);
                    String tvgName = matcher.group(1);
                    String tvgLogo = matcher.group(2);
                    String groupTitle = matcher.group(3);
                    String channelName = matcher.group(4);
                    ChannelInfo channelInfo = new ChannelInfo(tvgName, tvgLogo, groupTitle, channelName, url);
                    items.add(channelInfo);
                }

            }

        }
        return items;
    }
}
