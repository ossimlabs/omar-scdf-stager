# omar-scdf-stager

## Description
The OMAR SCDF stager application is Spring Cloud Data Flow processor service involved in ingesting imagery. It receives a image's message data, creates histograms and overviews from that message data, and forwards the message down the stream.

## Purpose
The Stager receives a JSON message from the Extractor containing the filename and path of an extracted image. The Stager then attempts to create a histogram and overview for the image. Finally, the Stager passes the message along to the Indexer, which indexes the image information in OMAR.

## JSON Input Example (from the Extractor)
```json
{
   "filename":"/data/2017/06/22/09/933657b1-6752-42dc-98d8-73ef95a5e780/12345/SCDFTestImages/tiff/14SEP12113301-M1BS-053951940020_01_P001.TIF"
}
```

## JSON Output Example (to the Indexer)
```json
{
   "filename":"/data/2017/06/22/09/933657b1-6752-42dc-98d8-73ef95a5e780/12345/SCDFTestImages/tiff/14SEP12113301-M1BS-053951940020_01_P001.TIF",
   "stagedSuccessfully":true
}
```
