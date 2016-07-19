/*
 * Copyright 2015 Esri. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.esri.samples.editing;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.datasource.Feature;
import com.esri.arcgisruntime.datasource.arcgis.FeatureEditResult;
import com.esri.arcgisruntime.datasource.arcgis.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.MapView;

public class AddFeatures extends Application {

  private MapView mapView;

  private ServiceFeatureTable featureTable;

  private static final String SERVICE_LAYER_URL =
      "http://sampleserver6.arcgisonline.com/arcgis/rest/services/DamageAssessment/FeatureServer/0";

  @Override
  public void start(Stage stage) throws Exception {

    try {
      // create stack pane and application scene
      StackPane stackPane = new StackPane();
      Scene scene = new Scene(stackPane);

      // set title, size, and add scene to stage
      stage.setTitle("Add Features Sample");
      stage.setWidth(800);
      stage.setHeight(700);
      stage.setScene(scene);
      stage.show();

      // create spatial reference for point
      SpatialReference spatialReference = SpatialReferences.getWebMercator();

      // create a ArcGISMap with streets basemap
      ArcGISMap map = new ArcGISMap(Basemap.createStreets());

      // set viewpoint of the ArcGISMap
      Point pointLondon = new Point(-16773, 6710477, spatialReference);
      Viewpoint viewpoint = new Viewpoint(pointLondon, 200000);
      map.setInitialViewpoint(viewpoint);

      // create a view for this ArcGISMap
      mapView = new MapView();

      // create service feature table from URL
      featureTable = new ServiceFeatureTable(SERVICE_LAYER_URL);

      // create a feature layer from table
      FeatureLayer featureLayer = new FeatureLayer(featureTable);

      // add the layer to the ArcGISMap
      map.getOperationalLayers().add(featureLayer);

      mapView.setOnMouseClicked(event -> {
        // check that the primary mouse button was clicked
        if (event.isStillSincePress() && event.getButton() == MouseButton.PRIMARY) {
          // create a point from where the user clicked
          Point2D point = new Point2D(event.getX(), event.getY());

          // create a map point from a point
          Point mapPoint = mapView.screenToLocation(point);

          // for a wrapped around map, the point coordinates include the wrapped
          // around value
          // for a service in projected coordinate system, this wrapped around
          // value has to be normalized
          Point normalizedMapPoint = (Point) GeometryEngine.normalizeCentralMeridian(mapPoint);

          // add a new feature to the service feature table
          try {
            addFeature(normalizedMapPoint, featureTable);
          } catch (Exception e) {
            displayMessage("Cannot add feature", e.getCause().getMessage());
          }
        }
      });

      // set ArcGISMap to be displayed in map view
      mapView.setMap(map);

      // add the map view to stack pane
      stackPane.getChildren().addAll(mapView);

    } catch (Exception e) {
      // on any error, display the stack trace
      e.printStackTrace();
    }
  }

  /**
   * Adds a new Feature to a ServiceFeatureTable and applies the changes to the
   * server.
   * 
   * @param mapPoint location to add feature
   * @param featureTable service feature table to add feature
   */
  private void addFeature(Point mapPoint, ServiceFeatureTable featureTable) {

    // create default attributes for the feature
    java.util.Map<String, Object> attributes = new HashMap<>();
    attributes.put("typdamage", "Destroyed");
    attributes.put("primcause", "Earthquake");

    // creates a new feature using default attributes and point
    Feature feature = featureTable.createFeature(attributes, mapPoint);

    // check if feature can be added to feature table
    ArcGISRuntimeException exception = featureTable.canAdd(feature);
    if (exception == null) {
      // add the new feature to the feature table
      ListenableFuture<Void> addResult = featureTable.addFeatureAsync(feature);
      addResult.addDoneListener(() -> applyEdits(featureTable));
    } else {
      // throw a runtime exception if feature cannot be added
      throw exception;
    }
  }

  /**
   * Sends any edits on the ServiceFeatureTable to the server.
   *
   * @param featureTable service feature table
   */
  private void applyEdits(ServiceFeatureTable featureTable) {

    // apply the changes to the server
    ListenableFuture<List<FeatureEditResult>> editResult = featureTable.applyEditsAsync();
    editResult.addDoneListener(() -> {
      try {
        List<FeatureEditResult> edits = editResult.get();
        // check if the server edit was successful
        if (edits != null && edits.size() > 0 && edits.get(0).hasCompletedWithErrors()) {
          throw edits.get(0).getError();
        }
      } catch (InterruptedException | ExecutionException e) {
        displayMessage("Exception applying edits on server", e.getCause().getMessage());
      }
    });
  }

  /**
   * Shows a message in an alert dialog.
   *
   * @param title title of alert
   * @param message message to display
   */
  private void displayMessage(String title, String message) {

    Platform.runLater(() -> {
      Alert dialog = new Alert(AlertType.INFORMATION);
      dialog.setHeaderText(title);
      dialog.setContentText(message);
      dialog.showAndWait();
    });
  }

  /**
   * Stops and releases all resources used in application.
   */
  @Override
  public void stop() throws Exception {

    if (mapView != null) {
      mapView.dispose();
    }
  }

  /**
   * Opens and runs application.
   * 
   * @param args arguments passed to this application
   */
  public static void main(String[] args) {

    Application.launch(args);
  }

}
