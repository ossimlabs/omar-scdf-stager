# omar-scdf-stager
The Stager is a Spring Cloud Data Flow (SCDF) Processor.
This means it:
1. Receives a message on a Spring Cloud input stream using Kafka.
2. Performs an operation on the data.
3. Sends the result on a Spring Cloud output stream using Kafka to a listening SCDF Processor or SCDF Sink.

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
