package com.github.sellersj.flickr.image.tagger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

public class TrainingUtilTest {

    private TrainingUtil trainingUtil = new TrainingUtil();

    @Test
    public void getTrainingEnteries() {
        List<FlickrTrainingEntry> enteries = trainingUtil.getTrainingEnteries();
        assertNotNull(enteries, "shouldn't be null");
        assertFalse(enteries.isEmpty(), "shouldn't be empty");

        HashSet<Long> idsWeHaveSeen = new HashSet<>();
        TreeSet<Long> dupIds = new TreeSet<>();

        for (FlickrTrainingEntry entry : enteries) {
            assertNotNull(entry.getTagName(), "tag name shouldn't be null for " + entry);
            assertFalse(entry.getTagName().isEmpty(), "tag name shouldn't be null for " + entry);
            assertFalse(entry.getPhotoIds().isEmpty(), "photo id's shouldn't be empty for " + entry);

            assertFalse(entry.getCachedPhotoPath().isEmpty(), "photo file paths shouldn't be empty for " + entry);
            // TODO check if the file paths actually exist

            for (Long id : entry.getPhotoIds()) {

                // keep track of the dup ids and print them all at once at the end
                if (idsWeHaveSeen.contains(id)) {
                    dupIds.add(id);
                }

                // now make sure we add it to track we've seen it
                idsWeHaveSeen.add(id);
            }
        }

        assertFalse(idsWeHaveSeen.isEmpty(), "we should have been adding id's");
        assertTrue(dupIds.isEmpty(), "Found " + dupIds.size() + " duplicate id's " + dupIds);
    }

}
