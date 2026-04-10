package cn.fred.mytv;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChannelInfo {
private String tvgName;

private String tvgLogo;

private String group;

private String name;

private String url;

}
