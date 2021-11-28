package com.github.sellersj.flickr.image.tagger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

public class HelloCV {

    static {
        Loader.load(opencv_java.class);
    }

    private static Map<Integer, List<String>> KNOWN_PEOPLE = null;

    private final EigenFaceRecognizer faceRecognizer = EigenFaceRecognizer.create();
    // private final FisherFaceRecognizer faceRecognizer = FisherFaceRecognizer.create();

    public void initKnownPeople() {
        KNOWN_PEOPLE = new TreeMap<>();
        String flickrCacheDir = "/Users/sellersj/Downloads/flickr_frame/";

        KNOWN_PEOPLE.put(0, // "laura"
            Arrays.asList(//
                flickrCacheDir + "vert/89816602.jpg", //
                flickrCacheDir + "vert/332317382.jpg", //
                flickrCacheDir + "vert/397135131.jpg", //
                flickrCacheDir + "vert/639320620.jpg", //
                // flickrCacheDir + "vert/639320620.jpg", //
                // flickrCacheDir + "vert/2304638646.jpg", //
                flickrCacheDir + "vert/2304671662.jpg"));

        KNOWN_PEOPLE.put(1, // "jim",
            Arrays.asList(//
                flickrCacheDir + "vert/743749434.jpg", //
                flickrCacheDir + "vert/2302719514.jpg", //
                flickrCacheDir + "vert/2481226701.jpg", //
                flickrCacheDir + "vert/2671955983.jpg", //
                // flickrCacheDir + "vert/639305244.jpg", //
                // flickrCacheDir + "vert/3109722534.jpg", //
                flickrCacheDir + "vert/2982279905.jpg"));
    }

    public void train() {
        long t1 = System.currentTimeMillis();

        // figure out total number of images we're doing
        int imageCount = 0;
        for (List<String> values : KNOWN_PEOPLE.values()) {
            imageCount += values.size();
        }

        int[] labels = new int[imageCount];
        List<Mat> list = new ArrayList<Mat>(imageCount);

        int counter = 0;
        for (Entry<Integer, List<String>> entry : KNOWN_PEOPLE.entrySet()) {
            for (String filename : entry.getValue()) {

                // try to create a training image here
                Mat trainingImage = createTrainingImage(new File(filename));
                // TODO does this make sense to return null? Now we have gaps in the array?
                if (null != trainingImage) {

                    // Mat img = loadImage(filename);
                    Mat grayImg = new Mat();
                    Imgproc.cvtColor(trainingImage, grayImg, Imgproc.COLOR_BGR2GRAY);
                    list.add(grayImg);

                    labels[counter] = entry.getKey();

                    counter++;
                }
            }
        }

        MatOfInt labels1 = new MatOfInt();
        labels1.fromArray(labels);
        // FaceRecognizer faceRecognizer = EigenFaceRecognizer.create();
        faceRecognizer.train(list, labels1);

        long t2 = System.currentTimeMillis();
        System.out.println("Done training. It took " + (t2 - t1) + " milliseconds");
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
            throw new IllegalArgumentException("We need to detect one and only 1 face in " + toTrain.getAbsolutePath());
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
