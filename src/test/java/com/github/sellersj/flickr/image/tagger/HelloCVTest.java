package com.github.sellersj.flickr.image.tagger;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HelloCVTest {

    private final String flickrCacheDir = "/Users/sellersj/Downloads/flickr_frame/";

    private final HelloCV helloCV = new HelloCV();

    @BeforeEach
    public void loadTraining() {
        helloCV.train();
    }

    @Test
    public void allOfCachedFlickr() {
        File dir = new File(flickrCacheDir + "vert/");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".jpg"));
        for (File file : files) {
            helloCV.processImage(file.getAbsolutePath());
        }
    }

    @Test
    public void subsetOfCachedFlickr() {
        String flickrCacheDir = "/Users/sellersj/Downloads/flickr_frame/";
        List<String> flickrImages = Arrays.asList("vert/50315994748.jpg", //
            "vert/50316010018.jpg", //
            "vert/50316826751.jpg");

        for (String string : flickrImages) {
            helloCV.processImage(flickrCacheDir + string);
        }
    }

    @Test
    public void testImages() {
        String flickrCacheDir = "/Users/sellersj/Downloads/image-rec-test/data/images/";
        List<String> flickrImages = Arrays.asList("IMG_E4366.JPG", "IMG_E4388.JPG", "IMG_E4394.jpg", "IMG_E4478.JPG");

        for (String string : flickrImages) {
            helloCV.processImage(flickrCacheDir + string);
        }
    }
}
