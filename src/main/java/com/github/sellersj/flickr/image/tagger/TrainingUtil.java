package com.github.sellersj.flickr.image.tagger;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TrainingUtil {

    // TODO Redesign:
    // This should be changed so that it has a list of "tags of interest".
    // it should query flickr for all the images that have these tags
    // it can either download them, or look for them in a cache dir.
    // for images that have ONLY ONE tag of interest, it should be considered a training image
    // if it has 1 face in it, we could guess that it matches the tag
    // We might have to have some other tag that says "do not use this image" or something

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
