package com.github.sellersj.flickr.image.tagger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.FaceMatch;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;
import software.amazon.awssdk.services.rekognition.model.SearchFacesByImageRequest;
import software.amazon.awssdk.services.rekognition.model.SearchFacesByImageResponse;

public class SearchFaceMatchingImageCollection {

    public static void main(String[] args) {

        final String USAGE = "\n"
            + "SearchFaceMatchingImageCollection - searches for matching faces in a collection\n\n"
            + "Usage: SearchFaceMatchingImageCollection <collectionName><path>\n\n" + "Where:\n"
            + "  collectionName - the name of the collection  \n"
            + "  path - the path to the image (i.e., C:\\AWS\\pic1.png ) \n\n";

        if (args.length < 2) {
            System.out.println(USAGE);
            System.exit(1);
        }

        String collectionId = args[0];
        String sourceImage = args[1];

        Region region = Region.US_EAST_2;
        RekognitionClient rekClient = RekognitionClient.builder().region(region).build();

        System.out.println("Searching for a face in a collection");
        searchFaceInCollection(rekClient, collectionId, sourceImage);
    }

    // snippet-start:[rekognition.java2.search_faces_collection.main]
    public static void searchFaceInCollection(RekognitionClient rekClient, String collectionId, String sourceImage) {

        try {

            InputStream sourceStream = new FileInputStream(new File(sourceImage));
            SdkBytes sourceBytes = SdkBytes.fromInputStream(sourceStream);

            // Create an Image object for the source image
            Image souImage = Image.builder().bytes(sourceBytes).build();

            SearchFacesByImageRequest facesByImageRequest = SearchFacesByImageRequest.builder().image(souImage)
                .maxFaces(10).faceMatchThreshold(70F).collectionId(collectionId).build();

            // Invoke the searchFacesByImage method
            SearchFacesByImageResponse imageResponse = rekClient.searchFacesByImage(facesByImageRequest);

            // Display the results
            System.out.println("Faces matching in the collection");
            List<FaceMatch> faceImageMatches = imageResponse.faceMatches();
            for (FaceMatch face : faceImageMatches) {
                System.out.println("The similarity level  " + face.similarity());
                System.out.println();
            }
        } catch (RekognitionException | FileNotFoundException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        // snippet-end:[rekognition.java2.search_faces_collection.main]
    }
}
