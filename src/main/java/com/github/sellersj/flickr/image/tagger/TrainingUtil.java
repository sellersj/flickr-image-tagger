package com.github.sellersj.flickr.image.tagger;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TrainingUtil {

    // TODO remove this later
    private static String FLICKR_CACHE_DIR = System.getProperty("user.home") + "/Downloads/flickr_frame/";

    public List<FlickrTrainingEntry> getTrainingEnteries() {

        ObjectMapper objectMapper = new ObjectMapper();
        // TODO to get this off the classpath
        String fileLocation = "src/main/resources/training-name-file.json";
        try {
            List<FlickrTrainingEntry> enteries = objectMapper.readValue(new File(fileLocation),
                new TypeReference<List<FlickrTrainingEntry>>() {
                });

            // TODO this would probably be where we download the photos and we also assume the file
            // is already cached
            for (FlickrTrainingEntry entry : enteries) {
                for (Long photoId : entry.getPhotoIds()) {
                    entry.getCachedPhotoPath().add(FLICKR_CACHE_DIR + "vert/" + photoId + ".jpg");
                }
            }

            return enteries;
        } catch (IOException e) {
            throw new RuntimeException("Could not read the file " + fileLocation, e);
        }
    }

}
