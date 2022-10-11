package xyz.doikki.videoplayer.exo;

import com.google.android.exoplayer2.Tracks;

import tv.danmaku.ijk.media.player.misc.IMediaFormat;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;

public class ExoTrackInfo implements ITrackInfo {

    private int trackType = 0;
    private int trackIndex = -1;
    private String language = null;
    private String infoInline = null;
    private boolean isSelected = false;
    private Tracks.Group trackGroup;
    private int exoTrackType;

    public ExoTrackInfo(int index, int trackType, String language, String infoInline, boolean isSelected, Tracks.Group trackGroup) {
        this.trackIndex = index;
        this.trackType = trackType;
        if(trackType == ITrackInfo.MEDIA_TRACK_TYPE_VIDEO)
            exoTrackType = 2;
        else if(trackType == ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
            exoTrackType = 1;
        else if(trackType == ITrackInfo.MEDIA_TRACK_TYPE_SUBTITLE)
            exoTrackType = 3;
        else
            exoTrackType = -1;
        this.language = language;
        this.infoInline = infoInline;
        this.isSelected = isSelected;
        this.trackGroup = trackGroup;
    }

    @Override
    public int getTrackIndex() {
        return this.trackIndex;
    }

    @Override
    public IMediaFormat getFormat() {
        return null;
    }

    @Override
    public String getLanguage() {
        return this.language;
    }

    @Override
    public int getTrackType() {
        return trackType;
    }

    @Override
    public String getInfoInline() {
        return this.infoInline;
    }

    @Override
    public boolean isSelected() {
        return this.isSelected;
    }

    public Tracks.Group getTrackGroup() {
        return trackGroup;
    }

    public int getExoTrackType() {
        return exoTrackType;
    }
}
