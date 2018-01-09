# Android-ENSG-2016
An Android course given at ENSG Paris Marne la Vallée

# Ressources
## HelloSpacialite example
The example project:
https://github.com/kristina-hager/HelloToSpatialite

The Spatialite compiled library
https://github.com/kristina-hager/Spatialite-Database-Driver

## Spacialite cookbook
https://www.gaia-gis.it/spatialite-3.0.0-BETA/spatialite-cookbook-fr/index.html#family

## spacialite db
https://www.gaia-gis.it/spatialite-2.3.1/resources.html

## JSON & XML
Jackson library:
https://github.com/FasterXML/jackson

# Tools
## GPS visualizer
http://www.mygpsfiles.com/app/
http://www.gpsvisualizer.com/

## Shapefiles
http://www.apur.org/article/donnees-disponibles-open-data

## GPX files
http://www.visugpx.com/
http://www.tracegps.com/fr/reccircuit.htm

## Webservices
http://www.geonames.org/export/ws-overview.html
http://api.geonames.org/findNearByWeatherJSON?lat=43&lng=-2&username=demo

Reference:
https://github.com/typicode/json-server

# Misc
Android file system // TODO
http://android.stackexchange.com/questions/46926/android-folder-hierarchy/46934#46934
and
http://stackoverflow.com/questions/1998400/data-directory-has-no-read-write-permission-in-android

get a file
http://stackoverflow.com/questions/21062187/get-sqlite-database-from-android-app

material design
http://developer.android.com/design/downloads/index.html

apk mirror (to update google play services)
http://www.apkmirror.com/


# ENSG Configuration
Ajouter le path
PATH="$PATH:/home/prof/Android/Sdk/platform-tools"

Autoriser le Proxy
Dans gradle.properties, ajouter 

systemProp.https.proxyHost=10.0.4.2
systemProp.https.proxyPort=3128
