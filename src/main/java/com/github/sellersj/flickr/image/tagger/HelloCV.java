package com.github.sellersj.flickr.image.tagger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.face.EigenFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HelloCV {

    /**
     * Checks the system variable if we should retrain the model, otherwise it will load what is
     * has.
     */
    private static Boolean RETRAIN = Boolean.getBoolean("retrain");

    static {
        Loader.load(opencv_java.class);
    }

    private final EigenFaceRecognizer faceRecognizer = EigenFaceRecognizer.create();
    // private final FisherFaceRecognizer faceRecognizer = FisherFaceRecognizer.create();

    public void train() {
        // TODO make a decision here about retraining vs loading the data
        if (RETRAIN) {
            List<FlickrTrainingEntry> enteries = retrain();

            // make a map of the tag names and the label
            Map<String, Integer> mapping = new TreeMap<>();
            for (FlickrTrainingEntry entry : enteries) {
                mapping.put(entry.getTagName(), entry.getOpencvLabelId());
            }

            // TODO this should be changed quite a bit... maybe a db ?
            // write the file
            ObjectMapper objectMapper = new ObjectMapper();
            String fileLocation = System.getProperty("user.home") + "/Downloads/flickr-existing-training-mapping.json";
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(fileLocation), mapping);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't write " + fileLocation, e);
            }
        } else {
            loadExistingModel();
        }
    }

    public void loadExistingModel() {
        long t1 = System.currentTimeMillis();

        String cachedModel = System.getProperty("user.home") + "/Downloads/flickr-existing-training.yaml";
        System.out.println("Loading the already trained model from " + cachedModel);
        faceRecognizer.read(cachedModel);

        // TODO read in the id mapping file

        long t2 = System.currentTimeMillis();
        System.out.println("Done loading existing model. It took " + (t2 - t1) + " milliseconds");
    }

    public List<FlickrTrainingEntry> retrain() {
        long t1 = System.currentTimeMillis();

        TrainingUtil trainingUtil = new TrainingUtil();
        List<FlickrTrainingEntry> trainingEnteries = trainingUtil.getTrainingEnteries();

        // figure out total number of images we're doing
        int imageCount = 0;
        for (FlickrTrainingEntry entry : trainingEnteries) {
            imageCount += entry.getCachedPhotoPath().size();
        }

        int[] labels = new int[imageCount];
        List<Mat> list = new ArrayList<Mat>(imageCount);

        int counter = 0;
        for (FlickrTrainingEntry entry : trainingEnteries) {
            for (String filename : entry.getCachedPhotoPath()) {

                // try to create a training image here
                Mat trainingImage = createTrainingImage(new File(filename));
                // TODO does this make sense to return null? Now we have gaps in the array?
                if (null != trainingImage) {

                    // Mat img = loadImage(filename);
                    Mat grayImg = new Mat();
                    Imgproc.cvtColor(trainingImage, grayImg, Imgproc.COLOR_BGR2GRAY);
                    list.add(grayImg);

                    // TODO do we need to match from one run to the next the id of the internal
                    // array against the flickr tag?
                    labels[counter] = counter;
                    entry.setOpencvLabelId(counter);
                }
            }

            // only increment the counter for the person, not the photo we are looking at
            counter++;

            // triggering the gc so we don't use tons of memory
            System.gc();
        }

        MatOfInt labels1 = new MatOfInt();
        labels1.fromArray(labels);
        // FaceRecognizer faceRecognizer = EigenFaceRecognizer.create();
        faceRecognizer.train(list, labels1);

        // write the retraining file to the
        String target = "target/retraining.yaml";
        System.out.println("Writing the retraing data to " + target);
        faceRecognizer.write(target);

        long t2 = System.currentTimeMillis();
        System.out.println("Done training. It took " + (t2 - t1) + " milliseconds");

        return trainingEnteries;
    }

    public void processImage(String filename) {

        String targetImagePath = "target/output-images/"
            + filename.substring(filename.indexOf(File.separator) + File.separator.length());
        System.out.println("Going to write file to " + targetImagePath);

        Mat loadedImage = loadImage(filename);
        System.out.println("done loaded image");

        MatOfRect facesDetected = new MatOfRect();

        CascadeClassifier cascadeClassifier = new CascadeClassifier();
        int minFaceSize = Math.round(loadedImage.rows() * 0.1f);
        cascadeClassifier.load("./src/main/resources/haarcascades/haarcascade_frontalface_alt.xml");
        // cascadeClassifier.load("./src/main/resources/haarcascades/haarcascade_eye_tree_eyeglasses.xml");
        cascadeClassifier.detectMultiScale(loadedImage, facesDetected, 1.1, 3, Objdetect.CASCADE_SCALE_IMAGE,
            new Size(minFaceSize, minFaceSize), new Size());

        // save the image
        Rect[] facesArray = facesDetected.toArray();
        if (0 != facesArray.length) {

            // TODO fix this so that we only do it if it's not already part of the training set
            // try to identify the photos with only 1 found face
            if (1 == facesArray.length) {
                System.out.println("Only 1 face found for " + targetImagePath);
            }

            for (Rect face : facesArray) {
                Imgproc.rectangle(loadedImage, face.tl(), face.br(), new Scalar(0, 0, 255), 3);

                {
                    Imgproc.rectangle(loadedImage, new Point(face.x, face.y),
                        new Point(face.x + face.width, face.y + face.height), new Scalar(0, 255, 0));
                    Rect rectCrop = new Rect(face.x, face.y, face.width, face.height);
                    Mat image_roi = new Mat(loadedImage, rectCrop);

                    // // Resize the face to 300x300
                    // Mat detection = image_roi.clone();
                    Mat gray = new Mat();
                    Imgproc.cvtColor(image_roi, gray, Imgproc.COLOR_BGR2GRAY);
                    Mat detection = new Mat();
                    Imgproc.resize(gray, detection, new Size(300, 300), 0.0, 0.0, Imgproc.INTER_LANCZOS4);

                    // creating arrays so that the values can be passed back
                    int[] label = new int[1];
                    double[] confidence = new double[1];
                    faceRecognizer.predict(detection, label, confidence);
                    System.out.println("confidence: " + Arrays.toString(confidence));
                    System.out.println("prediction is (0 is Laura, 1 for Jim): " + label[0] + " at " + confidence[0]);
                }
            }
            saveImage(loadedImage, targetImagePath);

            System.out.println("done for " + filename);
        } else {
            System.out.println("No faces detected on " + filename);
        }

        // release it to stop a memory leak
        loadedImage.release();

        // total hack to see if this helps with the memory leaking
        // the system does not seem to "see" the memory that the C part of the program has taken so
        // if we don't use this it will leak gigs and gigs of RAM before it crashes. Having this
        // year will clear up the memory and the program will run at a stable amount of ram.
        System.gc();
    }

    public Mat createTrainingImage(File toTrain) {
        Mat loadedImage = loadImage(toTrain.getAbsolutePath());

        MatOfRect facesDetected = new MatOfRect();
        CascadeClassifier cascadeClassifier = new CascadeClassifier();
        int minFaceSize = Math.round(loadedImage.rows() * 0.1f);
        cascadeClassifier.load("./src/main/resources/haarcascades/haarcascade_frontalface_alt.xml");
        // cascadeClassifier.load("./src/main/resources/haarcascades/haarcascade_eye_tree_eyeglasses.xml");
        cascadeClassifier.detectMultiScale(loadedImage, facesDetected, 1.1, 3, Objdetect.CASCADE_SCALE_IMAGE,
            new Size(minFaceSize, minFaceSize), new Size());

        // save the image
        Rect[] facesArray = facesDetected.toArray();
        if (1 != facesArray.length) {
            throw new IllegalArgumentException("We need to detect one and only 1 face in " + toTrain.getAbsolutePath()
                + " but we found " + facesArray.length);
        } else {
            for (Rect face : facesArray) {
                // Mat detection = loadedImage.clone();
                // Imgproc.rectangle(detection, face, new Scalar(0, 255, 0));
                //
                // // Crop the image
                // Mat3b face(img(face));
                //
                // // Resize the face to 300x300
                // // Mat3b resized;
                // Imgproc.resize(face, resized, new Size(300, 300), 0.0, 0.0,
                // Imgproc.INTER_LANCZOS4);

                // MatOfRect justTheFace = new MatOfRect();
                // Size size = new Size(face.tl());
                // Imgproc.resize(loadedImage, justTheFace, size);
                // Imgproc.rectangle(loadedImage, face.tl(), face.br(), new Scalar(0, 0, 255), 3);
                // face_crop.append(gray_image[y:y+h, x:x+w])

                Imgproc.rectangle(loadedImage, new Point(face.x, face.y),
                    new Point(face.x + face.width, face.y + face.height), new Scalar(0, 255, 0));
                Rect rectCrop = new Rect(face.x, face.y, face.width, face.height);
                Mat image_roi = new Mat(loadedImage, rectCrop);

                // Resize the face to 300x300
                Mat gray = new Mat();
                Imgproc.cvtColor(image_roi, gray, Imgproc.COLOR_BGR2GRAY);
                Mat detection = new Mat();
                Imgproc.resize(image_roi, detection, new Size(300, 300), 0.0, 0.0, Imgproc.INTER_LANCZOS4);

                saveImage(detection, "target/training-day/" + toTrain.getName());

                return detection;
            }
            // don't save file for now
        }

        return null;
    }

    public Mat loadImage(String imagePath) {
        if (!(new File(imagePath)).exists()) {
            throw new RuntimeException("The source file doesn't exist: " + imagePath);
        }
        return Imgcodecs.imread(imagePath);
    }

    public void saveImage(Mat imageMatrix, String targetPath) {
        File outputFile = new File(targetPath);
        if (!outputFile.getParentFile().exists()) {
            // create the parent dir if it doesn't exist
            outputFile.getParentFile().mkdirs();
        }

        Imgcodecs.imwrite(targetPath, imageMatrix);
    }
}
