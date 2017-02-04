package org.experiment;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.utils.CSVUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by sonic on 17/01/17.
 */
public class Visualizer extends Application {

    public static void main(String[] arg) {
        //launch(arg);
        String evalDir = "test-data/Entity";
        String regEx = "MAP:";
//        String regEx = "P@1:";
//        String regEx = "P@10:";


        try (Stream<Path> paths = Files.walk(Paths.get(evalDir))) {
            CSVUtils csvUtils = new CSVUtils("test-data/" + regEx + ".csv");
            paths.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    if (filePath.toString().endsWith("-report3.out")) {
                        List <String> mapList = new ArrayList<>();
                        mapList.add(filePath.toString());
                        try {
                            String raw = FileUtils.readFileToString(filePath.toFile(), "UTF-8");
                            System.out.println(filePath);
                            for (String line : raw.split("\n")) {
                                if (line.contains(regEx)) {
                                    String map = line.split(regEx)[1];
                                    mapList.add(Double.toString(Double.parseDouble(map.trim())));
                                    System.out.println("map: " + map);
                                }
                            }
                            csvUtils.writeLine(mapList);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            csvUtils.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void start(Stage stage) throws Exception {

        String regex = "=========================";
        String evalDir = "test-data/Entity";

        //Defining the axes
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setCategories(FXCollections.<String> observableArrayList(Arrays.asList("Map")));
        xAxis.setLabel("model");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("score");

        //Creating the Bar chart
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Comparison between various benchmark");

        //Prepare XYChart.Series objects by setting data
        try (Stream<Path> paths = Files.walk(Paths.get(evalDir))) {
            paths.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    if (filePath.toString().endsWith("-report3.out")) {

                        try {
                            String raw = FileUtils.readFileToString(filePath.toFile(), "UTF-8");
//                            System.out.println(filePath + "\n" + raw.split(regex)[0]);
                            String overal = raw.split(regex)[0].split("\n")[1];
                            String map = overal.split("MAP :")[1];
                            System.out.println("map: " + map);
                            XYChart.Series<String, Number> series1 = new XYChart.Series<>();
                            series1.setName(filePath.toString());
                            series1.getData().add(new XYChart.Data<>("Map", Double.parseDouble(map)));
                            //Setting the data to bar chart
                            barChart.getData().addAll(series1);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }


        //Creating a Group object
        Group root = new Group(barChart);

        //Creating a scene object
        Scene scene = new Scene(root, 1200, 600);
        //Setting title to the Stage
        stage.setTitle("Bar Chart");
        //Adding scene to the stage
        stage.setScene(scene);
        //Displaying the contents of the stage
        stage.show();
    }


    public void parse(String data) {

    }


    /**
     *
     *  MAP:  0.2588
     *  P@1:  0.0000
     *  P@5:  0.2000
     *  P@10: 0.5000
     *  P@20: 0.6000
     */
    public class QualityResultData {
        double map;
        double pAt1;
        double pAt5;
        double pAt10;
        double pAt20;

    }

}
