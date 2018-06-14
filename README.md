# humanci
Website depicting current NWS weather alerts and Apache Storm pipeline REST service for processing tweets relevant to selected weather event(s).

## Building the PipelineService

1. Build and install GeoLocator 2.0 (https://github.com/geoparser/geolocator-2.0)
2. Register a Twitter application (https://apps.twitter.com/)
3. Copy your credentials for this application to the file: PipelineService/src/main/java/humanci/TwitterWeatherStorm.java
4. Install ElasticSearch, Kibana, Redis
5. Modify PipelineService/src/main/java/humanci/GeoLocatorBolt.java to set the path to the Gazetter and GeoNames files 
   produced by building GeoLocator:

        GlobalParam.setGazIndex("path to GazIndex");
        GlobalParam.setGeoNames("path to GeoNames");

6. Add the necessary jars to PipelineService/lib using the libs.txt listing.
7. Build the gradle application

## Website

1. The website invokes the Pipeline REST API in several places; replace HOSTNAME with the host where the pipeline is running.
2. The websiter also accesses both ElasticSearch and Kibana; replace ELASTICSEARCH_HOST and KIBANA_HOST as appropriate.

