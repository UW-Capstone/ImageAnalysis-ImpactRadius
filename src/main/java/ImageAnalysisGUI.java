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
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ImageAnalysisGUI extends JFrame {

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
    private JPanel resultsPanel;
    private JScrollPane resultsScrollPane;
    private JTextPane resultsTextPane;
    private JLabel localeLabel;
    private String FILE_IN_URL;
    private boolean FILE_OUT_FLAG = false; //TODO MAKE THIS A CHECKBOX IN THE GUI.
    private boolean SQL_CONNECTION_STATUS = false;
    private int MAX_NUMBER_OF_RESULTS_PER_IMAGE = 10;
    List<String> resultsList = new ArrayList<String>();
    List<String> bodyPartsList = new ArrayList<String>();
    Map<String, Integer> sortedMap;

    static DecisionTree apparelTree;
    static DecisionTree artsTree;

    static BufferedReader keyboardInput = new
            BufferedReader(new InputStreamReader(System.in));

    int CURRENT_PROFILE_ID = 0;
    String CURRENT_PROFILE_URLS = "";
    String CURRENT_PROFILE_SCREENSHOT_URL = "";
    String CURRENT_PROFILE_LOCALE = "";


    public static void main(String[] args) {
        JFrame frame = new ImageAnalysisGUI("Google Vision Image Analyzer");
        frame.setVisible(true);
    }

    /**
     * @param title The window title
     */
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
        this.setPreferredSize(new Dimension(800, 500));
        this.pack();

        analyzeButton.addActionListener(new ActionListener() {
            /**
             * Invoked when an action occurs.
             *
             * @param e the event to be processed
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                resultsTextPane.setText("Please wait while we run image analysis...");
                progressBar.setValue(0);
                analyzeButton.setEnabled(false);
                new Thread(() -> {
                    try {
                        if (labelsCheckBox.isSelected()) {
                            for (int i = 1; i <= 24; i++) {
                                detectLabels(FILE_IN_URL + "Cropped" + i + ".jpg");
                            }
                        }
                        progressBar.setValue(50);


                        if (objectsCheckBox.isSelected()) {
                            for (int i = 1; i <= 24; i++) {
                                detectLocalizedObjects(FILE_IN_URL + "Cropped" + i + ".jpg");
                            }
                        }
                        progressBar.setValue(100);

                        /*if (landmarksCheckBox.isSelected()) {
                            for (int i = 1; i <= 24; i++) {
                                detectLandmarks(FILE_IN_URL + "Cropped" + i + ".jpg");
                            }
                        }*/
                        //progressBar.setValue(75);

                        /*
                        if (textCheckBox.isSelected()) {
                            for (int i = 1; i <= 24; i++) {
                                detectText(FILE_IN_URL + "Cropped" + i + ".jpg");
                            }
                        }*/
                        detectText(FILE_IN_URL + "CroppedHeader" + ".jpg");

                        if (!labelsCheckBox.isSelected() && !objectsCheckBox.isSelected() && !landmarksCheckBox.isSelected() && !textCheckBox.isSelected()) {
                            JOptionPane.showMessageDialog(null, "Nothing detectors selected, please select at least one detection and press analyze.");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    loadButton.setEnabled(true);
                    urlTextField.setText("");
                    countResults();

                }).start();
            }
        });
        testMethodMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {


                /*apparelTree = new DecisionTree();
                artsTree = new DecisionTree();
                // Generate tree
                generateTrees();

                // Output tree
                //System.out.println("\nOUTPUT DECISION TREE");
                //System.out.println("====================");
                //apparelTree.outputBinTree();

                // Query tree
                try {
                    queryTree();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }*/
            }
        });
        saveFileMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /*try (FileWriter file = new FileWriter("masterfile.json")) {

                    file.write(jsonObj.toString());
                    file.flush();

                } catch (IOException err) {
                    err.printStackTrace();
                }*/
            }
        });
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                localeLabel.setText("Profile locale: --");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        loadButton.setEnabled(false);

                        /*jsonObj.put("imageData", (Object) imageData);
                        imageData.put(innerObject);*/

                        String JSON_DATA = sqlConnection(urlTextField.getText());
                        /*int listingID = 0;
                        String urls = "";
                        String screenshotURL = "";*/

                        final JSONObject obj = new JSONObject(JSON_DATA);
                        final JSONArray listings = obj.getJSONArray("listings");
                        final int n = listings.length();

                        for (int i = 0; i < n; ++i) { //SHOULD ALWAYS BE n = 1
                            final JSONObject person = listings.getJSONObject(i);
                            CURRENT_PROFILE_ID = person.getInt("listing_id");
                            CURRENT_PROFILE_URLS = person.getString("urls");
                            CURRENT_PROFILE_SCREENSHOT_URL = person.getString("screenshot_url");
                        }

                        try {
                            prepareImageForAnalysis(CURRENT_PROFILE_ID, CURRENT_PROFILE_URLS, CURRENT_PROFILE_SCREENSHOT_URL);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }

    /**
     * Downloads full screenshot and crops all images within it including the profile header
     *
     * @param theID            The listing_id column from the database we query to get the profile information (Ex. 2,3,4,....)
     * @param theURLS          The urls column from the database we query to get the profile name. We use is later for image and directory names (Ex. instagram.com/amothersedit)
     * @param theScreenshotURL The direct url to the instagram full profile screenshot. Used to download and crop each individual picture out.
     * @throws IOException
     */
    public void prepareImageForAnalysis(int theID, String theURLS, String theScreenshotURL) throws IOException {


        String[] bodyPartsArray = new String[]{"Person", "Head", "Shoulders", "Shoulder", "Thigh", "Joint", "Nose",
                "Chin", "Cheek", "Eyebrow", "Face", "Forehead", "Leg", "Lip", "Neck", "Skin", "Arm","Finger", "Eye", "Human", "Waist"};

        for (int i = 0; i<bodyPartsArray.length; i++){
            bodyPartsList.add(bodyPartsArray[i]);
        }

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
        int imageHeight = 0;
        int imageWidth = 0;

        String formattedURL = theURLS.replace("instagram.com/", "");
        String path = "src/main/resources/";
        new File("src/main/resources/" + formattedURL + "").mkdirs();
        FILE_IN_URL = path + formattedURL + "/" + formattedURL;

        BufferedImage bufferedImage = null;
        try {

            URL url = new URL(theScreenshotURL);
            // read the url
            bufferedImage = ImageIO.read(url);
            imageHeight = bufferedImage.getHeight();
            imageWidth = bufferedImage.getWidth();

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

    /**
     * @param theDetectionType Pass in a string that tells the detection type so it can add it to the master file in the correct spot (Ex. labelAnnotations)
     * @param theJSONArray     Pass in the JSONArray that contains the data so that we can add it to the master file
     */
    /*public void addToMasterFile(String theDetectionType, JSONArray theJSONArray) {
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
    }*/

    /**
     * @param filePath Takes in local disk filepath to image to be scanned with label detection by google
     * @throws Exception
     */
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
                        int labelArrayLength = labelArray.length();
                        for (int l = 0; l < labelArrayLength; l++) {
                            JSONObject labelObject = labelArray.getJSONObject(l);
                            String labelTest = labelObject.getString("description");
                            resultsList.add(labelTest);
                        }
                        /*JSONArray labelArray = person.getJSONArray("labelAnnotations");
                        addToMasterFile("labelAnnotations", labelArray);*/
                    }
                }
            } else {

                BufferedWriter writer = new BufferedWriter(new FileWriter("labels.json"));
                writer.write(theData);
                writer.close();
            }
        }
    }

    /**
     * @param filePath Takes in local disk filepath to image to be scanned with text detection by google
     * @throws Exception
     */
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

            System.out.println("#####################################");
            System.out.println(theData);

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
                        int textArrayLength = textArray.length();
                        //for (int l = 0; l < textArrayLength; l++) {
                            JSONObject textObject = textArray.getJSONObject(0);
                            CURRENT_PROFILE_LOCALE = textObject.getString("locale");
                            localeLabel.setText("Profile locale: " + CURRENT_PROFILE_LOCALE);
                            //resultsList.add(textTest);
                            //System.out.println(landmarkTest);


                        //}
                        /*JSONArray textArray = person.getJSONArray("textAnnotations");
                        addToMasterFile("textAnnotations", textArray);*/
                    }
                }
            } else {
                BufferedWriter writer = new BufferedWriter(new FileWriter("labels.json"));
                writer.write(theData);
                writer.close();
            }
        }
    }

    /**
     * Detects landmarks found in an image
     *
     * @param filePath Takes in local disk filepath to image to be scanned with landmark detection by google
     * @throws Exception
     */
    /*public void detectLandmarks(String filePath) throws Exception {
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
                        //TODO You have to place just empty data because it will crash when parsing. Update for all other methods too
                    } else {
                        JSONArray landmarkArray = person.getJSONArray("landmarkAnnotations");
                        int landmarkArrayLength = landmarkArray.length();
                        for (int l = 0; l < landmarkArrayLength; l++) {
                            JSONObject landmarkObject = landmarkArray.getJSONObject(l);
                            String landmarkTest = landmarkObject.getString("description");
                            resultsList.add(landmarkTest);
                            //System.out.println(landmarkTest);


                        }
                        //addToMasterFile("landmarkAnnotations", landmarkArray);
                    }
                }
            } else {
                BufferedWriter writer = new BufferedWriter(new FileWriter("labels.json"));
                writer.write(theData);
                writer.close();
            }
        }
    }*/

    /**
     * Detects objects that are found in an image
     *
     * @param filePath Takes in local disk filepath to image to be scanned with localized objects detection by google
     * @throws Exception
     */
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
                        int localizedArrayLength = localizedArray.length();
                        for (int l = 0; l < localizedArrayLength; l++) {
                            JSONObject localizedObject = localizedArray.getJSONObject(l);
                            String localizedTest = localizedObject.getString("name");
                            resultsList.add(localizedTest);



                        }
                        /*JSONArray localizedArray = person.getJSONArray("localizedObjectAnnotations");
                        addToMasterFile("localizedObjectAnnotations", localizedArray);*/
                    }
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

    /**
     * Connects to a database and queries a specific url
     *
     * @param theURL the url that needs to be downloaded
     * @return
     */
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

    public void sqlInsert(int theID, String theTag) {
        String connectionUrl = "jdbc:sqlserver://73.118.249.57:1433;" +
                "instanceName=SQLEXPRESS;" +
                "databaseName=Influencer;" +
                "user=JamesNMartin;" +
                "password=nothingtoseehere";

        try (Connection con = DriverManager.getConnection(connectionUrl); Statement stmt = con.createStatement();) {
            // \/ Second select here to remove random text form end of the query
            String SQLJSON = "INSERT INTO genTags (RID, tag) VALUES ('" + theID + "', '" + theTag + "')";
            stmt.executeUpdate(SQLJSON);//TODO fix this because im not doing the right command for SQL insert.
        }
        // Handle any errors that may have occurred.
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Found this method here: https://stackoverflow.com/a/13913206/4657273
    public void countResults() {
        // Creating a HashMap containing string
        // as a key and occurrences as  a value
        HashMap<String, Integer> stringIntegerHashMap = new HashMap<>();
        resultsList.removeAll(new HashSet(bodyPartsList));

        // Converting given string to string array
        String[] strArray = new String[resultsList.size()];

        //resultsList = new ArrayList(Arrays.asList(strArray));

        for (int i = 0; i < resultsList.size(); i++) strArray[i] = resultsList.get(i);
        //for (String x : strArray) System.out.print(x + " ");


        // checking each string of strArray
        for (String c : strArray) {

            if (stringIntegerHashMap.containsKey(c)) {


                // If string is present in stringIntegerHashMap,
                // incrementing it's count by 1
                stringIntegerHashMap.put(c, stringIntegerHashMap.get(c) + 1);
            } else {

                // If string is not present in stringIntegerHashMap,
                // putting this string to stringIntegerHashMap with 1 as it's value
                stringIntegerHashMap.put(c, 1);
            }
        }
        sortedMap = sortByValue(stringIntegerHashMap, false);
        resultsTextPane.setText(sortedMap.toString());
        //sendMapToSQL();//TODO Turn me back on to insert data to database
    }

    public void sendMapToSQL() {
        for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
            Integer hitCount = entry.getValue();
            String s = entry.getKey();
            if (hitCount >= 3) {
                sqlInsert(CURRENT_PROFILE_ID, s);
            }
        }
        cleanUp();
    }

    private void cleanUp() {
        progressBar.setValue(0);
        resultsList.clear();
        sortedMap.clear();
        loadButton.setEnabled(true);
    }

    public void tagsToCategories(Map<String, Integer> theMap) throws IOException {

    }

    //Found this method here: https://stackoverflow.com/a/13913206/4657273
    public static Map<String, Integer> sortByValue(Map<String, Integer> unsortMap, final boolean order) {
        List<Map.Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
                ? o1.getKey().compareTo(o2.getKey()) : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
                ? o2.getKey().compareTo(o1.getKey()) : o2.getValue().compareTo(o1.getValue()));
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }

    //Found this method here: https://stackoverflow.com/a/13913206/4657273
    public static void printMap(Map<String, Integer> theMap) {
        theMap.forEach((key, value) -> System.out.println(key + " " + value));

    }
}
