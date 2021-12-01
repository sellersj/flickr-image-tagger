package com.github.sellersj.flickr.image.tagger;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class FlickrTrainingEntry {

    /** The tag that we want to apply to the photo. */
    private String tagName;

    /** The flickr photo id's where the person is the only face in the photo. */
    private List<Long> photoIds;

    /** Path to photos that we have cached for training. */
    private List<String> cachedPhotoPath = new ArrayList<>();

    /** This will need to be saved somewhere so we can match the face with the label. */
    private int opencvLabelId;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * @return the tagName
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * @param tagName the tagName to set
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    /**
     * @return the photoIds
     */
    public List<Long> getPhotoIds() {
        return photoIds;
    }

    /**
     * @param photoIds the photoIds to set
     */
    public void setPhotoIds(List<Long> photoIds) {
        this.photoIds = photoIds;
    }

    /**
     * @return the cachedPhotoPath
     */
    public List<String> getCachedPhotoPath() {
        return cachedPhotoPath;
    }

    /**
     * @param cachedPhotoPath the cachedPhotoPath to set
     */
    public void setCachedPhotoPath(List<String> cachedPhotoPath) {
        this.cachedPhotoPath = cachedPhotoPath;
    }

    
    /**
     * @return the opencvLabelId
     */
    public int getOpencvLabelId() {
        return opencvLabelId;
    }

    
    /**
     * @param opencvLabelId the opencvLabelId to set
     */
    public void setOpencvLabelId(int opencvLabelId) {
        this.opencvLabelId = opencvLabelId;
    }

}
