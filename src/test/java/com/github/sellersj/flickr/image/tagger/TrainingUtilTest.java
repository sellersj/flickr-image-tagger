package com.github.sellersj.flickr.image.tagger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

public class TrainingUtilTest {

    private TrainingUtil trainingUtil = new TrainingUtil();

    @Test
    public void getTrainingEnteries() {
        List<FlickrTrainingEntry> enteries = trainingUtil.getTrainingEnteries();
        assertNotNull(enteries, "shouldn't be null");
        assertFalse(enteries.isEmpty(), "shouldn't be empty");

        for (FlickrTrainingEntry entry : enteries) {
            assertNotNull(entry.getTagName(), "tag name shouldn't be null for " + entry);
            assertFalse(entry.getTagName().isEmpty(), "tag name shouldn't be null for " + entry);
            assertFalse(entry.getPhotoIds().isEmpty(), "photo id's shouldn't be empty for " + entry);

            assertFalse(entry.getCachedPhotoPath().isEmpty(), "photo file paths shouldn't be empty for " + entry);
            // TODO check if the file paths actually exist
        }
    }

}
