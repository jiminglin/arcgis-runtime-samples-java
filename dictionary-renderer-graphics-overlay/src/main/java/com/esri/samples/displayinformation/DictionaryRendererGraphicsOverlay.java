/*
 * Copyright 2015 Esri.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.esri.samples.displayinformation;

import static org.joox.JOOX.$;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LayerViewStatus;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.DictionaryRenderer;
import com.esri.arcgisruntime.symbology.SymbolDictionary;

public class DictionaryRendererGraphicsOverlay extends Application {

  private MapView mapView;
  private GraphicsOverlay graphicsOverlay;

  @Override
  public void start(Stage stage) throws Exception {
    mapView = new MapView();
    StackPane appWindow = new StackPane(mapView);
    Scene scene = new Scene(appWindow);

    // set title, size, and add scene to stage
    stage.setTitle("Dictionary Renderer Sample");
    stage.setWidth(800);
    stage.setHeight(700);
    stage.setScene(scene);
    stage.show();

    ArcGISMap map = new ArcGISMap(Basemap.createTopographic());
    mapView.setMap(map);

    graphicsOverlay = new GraphicsOverlay();
    mapView.getGraphicsOverlays().add(graphicsOverlay);

    // set specification for symbol dictionary using local resource path
    File specificationFile = new File(getClass().getResource("/mil2525d.stylx").getPath());
    SymbolDictionary symbolDictionary = new SymbolDictionary("mil2525d", specificationFile.getAbsolutePath());

    // tells graphics overlay how to render graphics with symbol dictionary attributes set
    DictionaryRenderer renderer = new DictionaryRenderer(symbolDictionary);
    graphicsOverlay.setRenderer(renderer);

    // parse graphic attributes from xml file
    List<Map<String, Object>> messages = parseMessages();

    // create graphics with attributes and add to graphics overlay
    messages.stream().map(DictionaryRendererGraphicsOverlay::createGraphic).collect(Collectors.toCollection(() ->
        graphicsOverlay.getGraphics()));
    
    mapView.addLayerViewStateChangedListener(layer -> {
      // waiting for graphics to load to the view
      if(layer.getLayerViewStatus().iterator().next() == LayerViewStatus.ACTIVE){
        // setting viewpoint to graphics in overlay
        mapView.setViewpointGeometryAsync(graphicsOverlay.getExtent());
        // graphics no longer show after this scale
        graphicsOverlay.setMinScale(1000000);
      }
    });
  }

  /**
   * Parses through a xml file and creates a graphic for each block of attributes found. Each block of attributes is then
   * assigned to that graphic.
   */
  private List<Map<String, Object>> parseMessages() throws Exception {

    final List<Map<String, Object>> messages = new ArrayList<>();
    File symbolData = new File(getClass().getResource("/Mil2525DMessages.xml").getPath());
    // $ reads the file
    $(symbolData).find("message").each().forEach(message -> {
      Map<String, Object> attributes = new HashMap<>();
      message.children().forEach(attr -> attributes.put(attr.getNodeName(), attr.getTextContent()));
      messages.add(attributes);
    });

    return messages;
  }

  /**
   * Creates a graphic using a symbol dictionary and the attributes that were passed.
   * 
   * @param attributes tells symbol dictionary what symbol to apply to graphic
   */
  private static Graphic createGraphic(Map<String, Object> attributes) {

    // get spatial reference
    int wkid = Integer.parseInt((String) attributes.get("_wkid"));
    SpatialReference sr = SpatialReference.create(wkid);

    // get points from coordinates' string
    PointCollection points = new PointCollection(sr);
    String[] coordinates = ((String) attributes.get("_control_points")).split(";");
    Stream.of(coordinates)
        .map(cs -> cs.split(","))
        .map(c -> new Point(Double.valueOf(c[0]), Double.valueOf(c[1]), sr))
        .collect(Collectors.toCollection(() -> points));

    // determine type of geometry and return a graphic
    Graphic graphic;
    if (points.size() == 1) {
      // point
      graphic = new Graphic(points.get(0), attributes);
    } else if (points.size() > 3 && points.get(0).equals(points.get(points.size() - 1))) {
      // polygon
      graphic = new Graphic(new Polygon(points), attributes);
    } else {
      // polyline
      graphic = new Graphic(new Polyline(points), attributes);
    }
    return graphic;
  }

  /**
   * Stops and releases all resources used in application.
   */
  @Override
  public void stop() {

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
