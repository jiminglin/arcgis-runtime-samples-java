#Map Image Layer#
Demonstrates how to display an ArcGISMapImageLayer on a ArcGISMap. Typically this type of content is known as operational data, an example would be business data that changes frequently, such as displaying a fleet of vehicles as they make deliveries.

![](MapImageLayer.png)

##How it works##
To add an `ArcGISMapImageLayer` to your `ArcGISMap` using its URL:

1. Create an ArcGIS map image layer from its URL.
2. Add it to `ArcGISMap.getOperationalLayers().add()`.
3. Display the ArcGIS map by adding it to the `MapView`.

##Features##
- ArcGISMapImageLayer
- ArcGISMap
- MapView
