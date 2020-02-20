import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ImageAnalysisGUI extends JFrame implements ActionListener, PropertyChangeListener {

    private JPanel mainPanel;
    private JMenuBar menuBar = new JMenuBar();
    private JMenu fileMenu = new JMenu("File");
    private JMenuItem fileMenuItem = new JMenuItem("Choose File");
    private JMenuItem saveFileMenuItem = new JMenuItem("Save data file");
    private JMenuItem testMethodMenuItem = new JMenuItem("Test Method");
    private JTextField urlTextField;
    private JButton browseButton;
    private JButton analyzeButton;
    private JCheckBox labelsCheckBox;
    private JCheckBox landmarksCheckBox;
    private JCheckBox objectsCheckBox;
    private JCheckBox textCheckBox;
    private JButton loadButton;
    private JProgressBar progressBar;
    private String FILE_IN_URL;
    private boolean FILE_OUT_FLAG = false; //TODO MAKE THIS A CHECKBOX IN THE GUI.
    private boolean SQL_CONNECTION_STATUS = false;
    private int MAX_NUMBER_OF_RESULTS_PER_IMAGE = 10;
    private JSONObject jsonObj = new JSONObject();
    private JSONArray imageData = new JSONArray();
    private JSONObject innerObject = new JSONObject();

    public static void main(String[] args) {
        JFrame frame = new ImageAnalysisGUI("Google Vision Image Analyzer");
        frame.setVisible(true);
    }

    public ImageAnalysisGUI(String title) {
        super(title);

        this.menuBar.add(fileMenu);
        this.fileMenu.add(fileMenuItem);
        this.fileMenu.add(saveFileMenuItem);
        this.fileMenu.add(testMethodMenuItem);

        this.setJMenuBar(menuBar);
        this.analyzeButton.setEnabled(false);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setContentPane(mainPanel);
        this.setResizable(false);
        this.setPreferredSize(new Dimension(800, 175));
        this.pack();

        analyzeButton.addActionListener(new ActionListener() {
            /**
             * Invoked when an action occurs.
             *
             * @param e the event to be processed
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                progressBar.setValue(0);
                //analyzeButton.setEnabled(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (labelsCheckBox.isSelected()) {
                                for (int i = 1; i <= 24; i++) {
                                    detectLabels(FILE_IN_URL + "Cropped" + i + ".jpg");
                                }
                            }
                            progressBar.setValue(25);


                            if (objectsCheckBox.isSelected()) {
                                for (int i = 1; i <= 24; i++) {
                                    detectLocalizedObjects(FILE_IN_URL + "Cropped" + i + ".jpg");
                                }
                            }
                            progressBar.setValue(50);

                            if (landmarksCheckBox.isSelected()) {
                                for (int i = 1; i <= 24; i++) {
                                    detectLandmarks(FILE_IN_URL + "Cropped" + i + ".jpg");
                                }
                            }
                            progressBar.setValue(75);

                            if (textCheckBox.isSelected()) {
                                for (int i = 1; i <= 24; i++) {
                                    detectText(FILE_IN_URL + "Cropped" + i + ".jpg");
                                }
                            }

                            if (!labelsCheckBox.isSelected() && !objectsCheckBox.isSelected() && !landmarksCheckBox.isSelected() && !textCheckBox.isSelected()) {
                                JOptionPane.showMessageDialog(null, "Nothing detectors selected, please select at least one detection and press analyze.");
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        progressBar.setValue(100);
                        loadButton.setEnabled(true);
                        urlTextField.setText("");
                    }
                }).start();
                /*try {
                    if (labelsCheckBox.isSelected()) {
                        for (int i = 1; i <= 24; i++) {
                            detectLabels(FILE_IN_URL + "Cropped" + i + ".jpg");
                        }
                    }
                    if (objectsCheckBox.isSelected()) {
                        for (int i = 1; i <= 24; i++) {
                            detectLocalizedObjects(FILE_IN_URL + "Cropped" + i + ".jpg");
                        }
                    }
                    if (landmarksCheckBox.isSelected()) {
                        for (int i = 1; i <= 24; i++) {
                            detectLandmarks(FILE_IN_URL + "Cropped" + i + ".jpg");
                        }
                    }
                    if (textCheckBox.isSelected()) {
                        for (int i = 1; i <= 24; i++) {
                            detectText(FILE_IN_URL + "Cropped" + i + ".jpg");
                        }
                    }
                    if (!labelsCheckBox.isSelected() && !objectsCheckBox.isSelected() && !landmarksCheckBox.isSelected() && !textCheckBox.isSelected()) {
                        JOptionPane.showMessageDialog(null, "Nothing detectors selected, please select at least one detection and press analyze.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                loadButton.setEnabled(true);
                urlTextField.setText("");*/
            }
        });
        testMethodMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        saveFileMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try (FileWriter file = new FileWriter("masterfile.json")) {

                    file.write(jsonObj.toString());
                    file.flush();

                } catch (IOException err) {
                    err.printStackTrace();
                }
            }
        });
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        loadButton.setEnabled(false);

                        jsonObj.put("imageData", (Object) imageData);
                        imageData.put(innerObject);

                        String JSON_DATA = sqlConnection(urlTextField.getText());
                        int listingID = 0;
                        String urls = "";
                        String screenshotURL = "";

                        final JSONObject obj = new JSONObject(JSON_DATA);
                        final JSONArray listings = obj.getJSONArray("listings");
                        final int n = listings.length();

                        for (int i = 0; i < n; ++i) { //SHOULD ALWAYS BE n = 1
                            final JSONObject person = listings.getJSONObject(i);
                            listingID = person.getInt("listing_id");
                            urls = person.getString("urls");
                            screenshotURL = person.getString("screenshot_url");
                        }

                        try {
                            prepareImageForAnalysis(listingID, urls, screenshotURL);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }).start();
                /*loadButton.setEnabled(false);

                jsonObj.put("imageData", (Object) imageData);
                imageData.put(innerObject);

                String JSON_DATA = sqlConnection(urlTextField.getText());
                int listingID = 0;
                String urls = "";
                String screenshotURL = "";

                final JSONObject obj = new JSONObject(JSON_DATA);
                final JSONArray listings = obj.getJSONArray("listings");
                final int n = listings.length();

                for (int i = 0; i < n; ++i) { //SHOULD ALWAYS BE n = 1
                    final JSONObject person = listings.getJSONObject(i);
                    listingID = person.getInt("listing_id");
                    urls = person.getString("urls");
                    screenshotURL = person.getString("screenshot_url");
                }

                try {
                    prepareImageForAnalysis(listingID, urls, screenshotURL);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }*/

                /*String fullPathToImage = "";
                BufferedImage bufferedImage = null;
                try {
                    URL url = new URL(FILE_IN_URL);
                    // read the url
                    bufferedImage = ImageIO.read(url);

                    String path = "src/main/resources/amothersedit/amothersedit";
                    String fileType = ".jpg";

                    ImageIO.write(bufferedImage, "jpg", new File(path + fileType));

                    fullPathToImage = path + fileType;
                    //FILE_IN_URL = path;

                } catch (IOException error) {
                    error.printStackTrace();
                }

                try {
                    prepareImageForAnalysis(fullPathToImage);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }*/
            }
        });
    }

    public void prepareImageForAnalysis(int theID, String theURLS, String theScreenshotURL) throws IOException {


        /*
        EACH LARGE SCREENSHOT IMAGE IS 1920px x 3352px
        EACH INDIVIDUAL IMAGE IS 293px X 293px
        EACH IMAGE IS SEPARATED BY 28px HORIZONTALLY & VERTICALLY

        IMAGE 1 LOCATION IS X:493 Y:590
        IMAGE 2 LOCATION IS X:814 Y:590
        IMAGE 3 LOCATION IS X:1135 Y:590


        WITH THAT WE CAN CROP EACH IMAGE OUT OF THE LARGER ONE AND SEND OFF THE CROPPED VERSION FOR MORE
        ACCURITE DATA FOR THE WHOLE PROFILE.

         */
        String formattedURL = theURLS.replace("instagram.com/", "");
        String path = "src/main/resources/";
        new File("src/main/resources/" + formattedURL + "").mkdirs();
        FILE_IN_URL = path + formattedURL + "/" + formattedURL;

        BufferedImage bufferedImage = null;
        try {

            URL url = new URL(theScreenshotURL);
            // read the url
            bufferedImage = ImageIO.read(url);
            ImageIO.write(bufferedImage, "jpg", new File(path + formattedURL + "/" + formattedURL + ".jpg"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        File imageFile = new File(path + formattedURL + "/" + formattedURL + ".jpg");
        bufferedImage = ImageIO.read(imageFile);



        /*BufferedImage img = cropImage(bufferedImage, 814, 2837, 293, 293);
        File pathFile = new File("src/main/resources/amotherseditCropped.jpg");
        ImageIO.write(img, "jpg", pathFile);*/


        int row = 8;
        int column = 3;
        int numberOfImages = 1;

        int imageDimention = 293; //Since the image a square its both the width and height.

        // We will create a cropped image of the top of the instagram profile specifically for running text detection
        // on it because we can pull maybe some contextual information about where they are from or language they speak.
        int profileHeaderX = 935;
        int profileHeaderY = 589;
        BufferedImage newHeaderImage = cropImage(bufferedImage, 493, 0, profileHeaderX, profileHeaderY);
        File pathToCroppedHeaderFile = new File("src/main/resources/" + formattedURL + "/" + formattedURL + "" + "CroppedHeader.jpg");
        ImageIO.write(newHeaderImage, "jpg", pathToCroppedHeaderFile);


        int x = 0;
        int y = 0;


        for (int i = 1; i <= row; i++) {

            if (y == 0) {
                y = 590;
            } else {
                y = y + 321;
            }

            for (int j = 1; j <= column; j++) {

                if (j == 1) {
                    x = 493;
                }
                if (j == 2) {
                    x = 814;
                }
                if (j == 3) {
                    x = 1135;
                }

                BufferedImage newImage = cropImage(bufferedImage, x, y, imageDimention, imageDimention);
                File newPathFile = new File("src/main/resources/" + formattedURL + "/" + formattedURL + "Cropped" + numberOfImages + ".jpg");
                numberOfImages++;
                ImageIO.write(newImage, "jpg", newPathFile);
                //System.out.println("Coord: " + x + "," + y);

            }
        }
        analyzeButton.setEnabled(true);
    }

    public void addToMasterFile(String theDetectionType, JSONArray theJSONArray) {
        //JSONObject obj = new JSONObject();
        switch (theDetectionType) {
            case "labelAnnotations":
                for (int j = 0; j < theJSONArray.length(); j++) {
                    JSONObject labelObject = theJSONArray.getJSONObject(j);
                    innerObject.append("labelAnnotations", labelObject);
                }
                break;
            case "textAnnotations":
                for (int j = 0; j < theJSONArray.length(); j++) {
                    JSONObject textObject = theJSONArray.getJSONObject(j);
                    innerObject.append("textAnnotations", textObject);
                }
                break;
            case "landmarkAnnotations":

                for (int j = 0; j < theJSONArray.length(); j++) {
                    JSONObject landmarkObject = theJSONArray.getJSONObject(j);
                    innerObject.append("landmarkAnnotations", landmarkObject);
                }
                break;
            case "localizedObjectAnnotations":
                for (int j = 0; j < theJSONArray.length(); j++) {
                    JSONObject localizedObject = theJSONArray.getJSONObject(j);
                    innerObject.append("localizedObjectAnnotations", localizedObject);
                }
                break;
        }

    }

    public void detectLabels(String filePath) throws Exception {

        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {

            // Reads the image file into memory
            Path path = Paths.get(filePath);
            byte[] data = Files.readAllBytes(path);
            ByteString imgBytes = ByteString.copyFrom(data);

            // Builds the image annotation request
            java.util.List<AnnotateImageRequest> requests = new ArrayList<>();
            com.google.cloud.vision.v1.Image img = com.google.cloud.vision.v1.Image.newBuilder()
                    .setContent(imgBytes)
                    .build();
            Feature feat = Feature.newBuilder()
                    .setType(Feature.Type.LABEL_DETECTION)
                    .setMaxResults(MAX_NUMBER_OF_RESULTS_PER_IMAGE) // This will change the number of results return,
                    // however it will only return as many as it can find. So results could be less than this number.
                    .build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .build();
            requests.add(request);

            // Performs label detection on the image file
            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);

            // I found this on stackoverflow and seems to be the only way I can get the response to be in JSON correctly.
            // https://stackoverflow.com/questions/47368685/android-google-cloud-vision-api-get-json-and-use-json-to-go-to-another-acitvity
            String theData = com.google.protobuf.util.JsonFormat.printer().print(response);


            if (!FILE_OUT_FLAG) {
                final JSONObject obj = new JSONObject(theData);
                final JSONArray imageData = obj.getJSONArray("responses");
                final int n = imageData.length();

                for (int i = 0; i < n; ++i) {
                    final JSONObject person = imageData.getJSONObject(i);
                    if (person.isEmpty()) {
                        //NO DATA HERE
                    } else {
                        JSONArray labelArray = person.getJSONArray("labelAnnotations");
                        addToMasterFile("labelAnnotations", labelArray);
                    }
                }
            } else {

                BufferedWriter writer = new BufferedWriter(new FileWriter("labels.json"));
                writer.write(theData);
                writer.close();
            }
        }
    }

    public void detectText(String filePath) throws Exception {

        java.util.List<AnnotateImageRequest> requests = new ArrayList<>();

        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

        com.google.cloud.vision.v1.Image img = com.google.cloud.vision.v1.Image.newBuilder()
                .setContent(imgBytes)
                .build();
        Feature feat = Feature.newBuilder()
                .setType(Feature.Type.TEXT_DETECTION)
                .setMaxResults(MAX_NUMBER_OF_RESULTS_PER_IMAGE)// This will change the number of results return,
                // however it will only return as many as it can find. So results could be less than this number.
                .build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);

            // I found this on stackoverflow and seems to be the only way I can get the response to be in JSON correctly.
            // https://stackoverflow.com/questions/47368685/android-google-cloud-vision-api-get-json-and-use-json-to-go-to-another-acitvity
            String theData = com.google.protobuf.util.JsonFormat.printer().print(response);
            if (!FILE_OUT_FLAG) {
                final JSONObject obj = new JSONObject(theData);
                final JSONArray imageData = obj.getJSONArray("responses");
                final int n = imageData.length();
                for (int i = 0; i < n; ++i) {
                    final JSONObject person = imageData.getJSONObject(i);
                    if (person.isEmpty()) {
                        //NO DATA HERE
                    } else {
                        JSONArray textArray = person.getJSONArray("textAnnotations");
                        addToMasterFile("textAnnotations", textArray);
                    }
                }
            } else {
                BufferedWriter writer = new BufferedWriter(new FileWriter("labels.json"));
                writer.write(theData);
                writer.close();
            }
        }
    }

    public void detectLandmarks(String filePath) throws Exception {
        java.util.List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

        com.google.cloud.vision.v1.Image img = com.google.cloud.vision.v1.Image.newBuilder()
                .setContent(imgBytes)
                .build();
        Feature feat = Feature.newBuilder()
                .setType(Feature.Type.LANDMARK_DETECTION)
                .setMaxResults(MAX_NUMBER_OF_RESULTS_PER_IMAGE)// This will change the number of results return,
                // however it will only return as many as it can find. So results could be less than this number.
                .build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);

            // I found this on stackoverflow and seems to be the only way I can get the response to be in JSON correctly.
            // https://stackoverflow.com/questions/47368685/android-google-cloud-vision-api-get-json-and-use-json-to-go-to-another-acitvity
            String theData = com.google.protobuf.util.JsonFormat.printer().print(response);
            if (!FILE_OUT_FLAG) {
                final JSONObject obj = new JSONObject(theData);
                final JSONArray imageData = obj.getJSONArray("responses");
                final int n = imageData.length();
                for (int i = 0; i < n; ++i) {
                    final JSONObject person = imageData.getJSONObject(i);
                    if (person.isEmpty()) {
                        //NO RESULTS HERE
                    } else {
                        JSONArray landmarkArray = person.getJSONArray("landmarkAnnotations");
                        addToMasterFile("landmarkAnnotations", landmarkArray);
                    }
                }
            } else {
                BufferedWriter writer = new BufferedWriter(new FileWriter("labels.json"));
                writer.write(theData);
                writer.close();
            }
        }
    }

    public void detectLocalizedObjects(String filePath) throws Exception {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

        com.google.cloud.vision.v1.Image img = Image.newBuilder().setContent(imgBytes).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder()
                        .addFeatures(Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION))
                        .setImage(img)
                        .build();
        requests.add(request);

        // Perform the request
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);

            String theData = com.google.protobuf.util.JsonFormat.printer().print(response);
            if (!FILE_OUT_FLAG) {
                final JSONObject obj = new JSONObject(theData);
                final JSONArray imageData = obj.getJSONArray("responses");
                final int n = imageData.length();

                for (int i = 0; i < n; ++i) {
                    final JSONObject person = imageData.getJSONObject(i);
                    if (person.isEmpty()) {
                        //NO DATA HERE
                    } else {
                        JSONArray localizedArray = person.getJSONArray("localizedObjectAnnotations");
                        addToMasterFile("localizedObjectAnnotations", localizedArray);
                    }

                    /*int labelArrayLength = labelArray.length();
                    for (int j=0; j<labelArrayLength; j++) {
                        System.out.println(labelArray);
                        addToMasterFile("labelAnnotations", labelArray);
                    }*/
                }
            } else {

                BufferedWriter writer = new BufferedWriter(new FileWriter("labels.json"));
                writer.write(theData);
                writer.close();
            }
        }
    }

    /**
     * Crops an image to the specified region
     *
     * @param bufferedImage the image that will be crop
     * @param x             the upper left x coordinate that this region will start
     * @param y             the upper left y coordinate that this region will start
     * @param width         the width of the region that will be crop
     * @param height        the height of the region that will be crop
     * @return the image that was cropped.
     */
    public static BufferedImage cropImage(BufferedImage bufferedImage, int x, int y, int width, int height) {
        BufferedImage croppedImage = bufferedImage.getSubimage(x, y, width, height);
        return croppedImage;
    }

    public String sqlConnection(String theURL) {
        // Create a variable for the connection string.
        //tcp:73.118.249.57,1433\SQLEXPRESS
        String output = "";
        String connectionUrl = "jdbc:sqlserver://73.118.249.57:1433;" +
                "instanceName=SQLEXPRESS;" +
                "databaseName=Influencer;" + //TODO CREATE DATABASE FOR GOOGLE IMAGE RESULTS
                "user=JamesNMartin;" +
                "password=nothingtoseehere";

        try (Connection con = DriverManager.getConnection(connectionUrl); Statement stmt = con.createStatement();) {
            // \/ Second select here to remove random text form end of the query
            String SQLJSON = "SELECT (SELECT listing_id, urls, screenshot_url FROM TestData WHERE urls LIKE '" + theURL + "' FOR JSON AUTO, ROOT('listings'))";

            //System.out.println(SQLJSON);
            ResultSet rs = stmt.executeQuery(SQLJSON);
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();

            SQL_CONNECTION_STATUS = true;
            //analyzeButton.setEnabled(true);

            // Iterate through the data in the result set and display it.
            while (rs.next()) {
                for (int i = 1; i <= columnsNumber; i++) {
                    //if (i > 1) System.out.print(",  ");
                    String columnValue = rs.getString(i);

                    output = columnValue + " " + rsmd.getColumnName(i);
                }
            }
        }
        // Handle any errors that may have occurred.
        catch (SQLException e) {
            SQL_CONNECTION_STATUS = false;
            e.printStackTrace();
        }
        return output;
    }

    //This was more of a test in parsing the file we will create that has all the data from google vision.
    public void parseJSON() {

        String JSON_DATA = "{\n" +
                "\t\"imageData\": [{\n" +
                "\t\t\"labelAnnotations\": [{\n" +
                "\t\t\t\"mid\": \"/m/09j2d\",\n" +
                "\t\t\t\"description\": \"Clothing\",\n" +
                "\t\t\t\"score\": 0.979938,\n" +
                "\t\t\t\"topicality\": 0.979938\n" +
                "\t\t}],\n" +
                "\t\t\"textAnnotations\": [{\n" +
                "\t\t\t\"locale\": \"en\",\n" +
                "\t\t\t\"description\": \"lourm\"\n" +
                "\t\t}],\n" +
                "\t\t\"landmarkAnnotations\": [{\n" +
                "\t\t\t\"mid\": \"/m/014lft\",\n" +
                "\t\t\t\"description\": \"Saint Basil's Cathedral\",\n" +
                "\t\t\t\"score\": 0.89872646,\n" +
                "\t\t\t\"boundingPoly\": {\n" +
                "\t\t\t\t\"vertices\": [\n" +
                "\t\t\t\t\t{\n" +
                "\t\t\t\t\t\t\"x\": 780,\n" +
                "\t\t\t\t\t\t\"y\": 780\n" +
                "\t\t\t\t\t},\n" +
                "\t\t\t\t\t{\n" +
                "\t\t\t\t\t\t\"x\": 2129,\n" +
                "\t\t\t\t\t\t\"y\": 780\n" +
                "\t\t\t\t\t},\n" +
                "\t\t\t\t\t{\n" +
                "\t\t\t\t\t\t\"x\": 2129,\n" +
                "\t\t\t\t\t\t\"y\": 2208\n" +
                "\t\t\t\t\t},\n" +
                "\t\t\t\t\t{\n" +
                "\t\t\t\t\t\t\"x\": 780,\n" +
                "\t\t\t\t\t\t\"y\": 2208\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t]\n" +
                "\t\t\t},\n" +
                "\t\t\t\"locations\": [\n" +
                "\t\t\t\t{\n" +
                "\t\t\t\t\t\"latLng\": {\n" +
                "\t\t\t\t\t\t\"latitude\": 55.752912,\n" +
                "\t\t\t\t\t\t\"longitude\": 37.622315883636475\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t}\n" +
                "\t\t\t]\n" +
                "\t\t}],\n" +
                "\t\t\"localizedObjectAnnotations\": [{\n" +
                "\t\t\t\"mid\": \"/m/01g317\",\n" +
                "\t\t\t\"name\": \"Person\",\n" +
                "\t\t\t\"score\": 0.6834264,\n" +
                "\t\t\t\"boundingPoly\": {\n" +
                "\t\t\t\t\"normalizedVertices\": [{\n" +
                "\t\t\t\t\t\"x\": 0.42230424,\n" +
                "\t\t\t\t\t\"y\": 0.17196484\n" +
                "\t\t\t\t},\n" +
                "\t\t\t\t\t{\n" +
                "\t\t\t\t\t\t\"x\": 0.5733198,\n" +
                "\t\t\t\t\t\t\"y\": 0.17196484\n" +
                "\t\t\t\t\t},\n" +
                "\t\t\t\t\t{\n" +
                "\t\t\t\t\t\t\"x\": 0.5733198,\n" +
                "\t\t\t\t\t\t\"y\": 0.26854423\n" +
                "\t\t\t\t\t},\n" +
                "\t\t\t\t\t{\n" +
                "\t\t\t\t\t\t\"x\": 0.42230424,\n" +
                "\t\t\t\t\t\t\"y\": 0.26854423\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t]\n" +
                "\t\t\t}\n" +
                "\t\t}]\n" +
                "\t}]\n" +
                "}";

        final JSONObject obj = new JSONObject(JSON_DATA);
        final JSONArray imageData = obj.getJSONArray("imageData");
        final int n = imageData.length();

        for (int i = 0; i < n; ++i) {
            final JSONObject person = imageData.getJSONObject(i);

            JSONArray labelArray = person.getJSONArray("labelAnnotations");
            int labelArrayLength = labelArray.length();

            JSONArray textArray = person.getJSONArray("textAnnotations");
            int textArrayLength = textArray.length();

            JSONArray landmarkArray = person.getJSONArray("landmarkAnnotations");
            int landmarkArrayLength = landmarkArray.length();

            JSONArray objectsArray = person.getJSONArray("localizedObjectAnnotations");
            int objectArrayLength = objectsArray.length();


            for (int j = 0; j < labelArrayLength; j++) {
                JSONObject labelObject = labelArray.getJSONObject(j);

                String labelTest = labelObject.getString("description");
                System.out.println(labelTest);
            }
            for (int k = 0; k < textArrayLength; k++) {
                JSONObject textObject = textArray.getJSONObject(k);

                String textTest = textObject.getString("description");
                System.out.println(textTest);
            }
            for (int l = 0; l < landmarkArrayLength; l++) {
                JSONObject landmarkObject = landmarkArray.getJSONObject(l);

                String landmarkTest = landmarkObject.getString("description");
                System.out.println(landmarkTest);
            }
            for (int m = 0; m < objectArrayLength; m++) {
                JSONObject localizedObject = objectsArray.getJSONObject(m);

                String objectTest = localizedObject.getString("name");
                System.out.println(objectTest);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
    }
}
