package com.lifengqiang.videoeditor.data;

import android.util.Range;

public class MediaSection {
    public static final int MEDIA_TYPE_AUDIO = 0;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /*** 路径 */
    private String path;
    /*** 类型 */
    private int type = 0;
    /*** 媒体文件总时长 如果是图片则为0 */
    private int durationMS = 0;
    /**
     * 选择的媒体区间,用于在编辑时确定所占时长,单位毫秒<br>
     * 视频和音频的范围在[0,durationMS]
     * 图片的范围[500,1000*10]
     */
    private Range<Integer> partRangeMS;
}
