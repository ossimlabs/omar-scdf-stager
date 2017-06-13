package io.ossim.omar.scdf.stager

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.SendTo
import joms.oms.Init
import joms.oms.ImageStager

/**
 * Created by slallier on 6/8/2017
 *
 * The OmarScdfStagerApplication is a purpose built image stager for integration with a full SCDF stack.
 */
@SpringBootApplication
@EnableBinding(Processor.class)
@Slf4j
class OmarScdfStagerApplication {

    /**
     * The application logger
     */
    //private final Logger logger = LoggerFactory.getLogger(this.getClass())


     /**
     * Constructor
     */
    OmarScdfStagerApplication()
    {
        Init.instance().initialize()
    }

    /**
     * The main entry point of the SCDF Sqs application.
     * @param args
     */
    static void main(String[] args)
    {
        SpringApplication.run OmarScdfStagerApplication, args
    }

    /**
     * The method that handles the stage request when a filename is received
     * @param message the message containing the image filename
     */
    @StreamListener(Processor.INPUT)
    @SendTo(Processor.OUTPUT)
    final String handleStageRequest(final Message<?> message)
    {
        log.debug("Received message ${message} containing the name of a file to stage")

        if (null != message.payload)
        {
            HashMap params = [
                    filename : "",
                    buildHistograms: true,
                    buildOverviews: true,
                    useFastHistograms: false,
                    overviewCompressionType: "none",
                    overviewType:"ossim_tiff_box"
            ]
            // Parse filename from message
            final def parsedJson = new JsonSlurper().parseText(message.payload)
            log.debug("parsedJson : ${parsedJson}")
            final String filename = parsedJson.filename
            log.debug("filename: ${filename}")

            // build histograms and overviews, stage image
            log.debug("Building histograms and overviews for ${filename}")

            params.filename = filename

            log.debug("Stager params:\n ${params}")

            boolean stagedSuccessfully = stageImage(params)

            // Return filename and result of staging request
            JsonBuilder stagedFile = new JsonBuilder()
            String status = stagedSuccessfully ? "succeeded" : "failed"
            stagedFile(
                    filename : filename,
                    status : status
            )

            log.debug("Sending result to output stream -- ${stagedFile.toString()}")
            return stagedFile.toString()
        }
        else
        {
            log.warn("Received null payload for message: ${message}")
            return null
        }
    }

    /**
    * Method to stage image using the params Map
    * @return boolean stating whether the image was staged successfully or not
    */
    private boolean stageImage(HashMap params)
    {
        boolean successfullyStaged = true
        def imageStager = new ImageStager()
        try{

            if (imageStager.open(params.filename))
            {
                URI uri = new URI(params.filename)
                String scheme = uri.scheme
                if (!scheme) scheme = "file"
                if (scheme != "file")
                {
                    params.buildHistograms = false
                    params.buildOverviews = false
                }
                 Integer nEntries = imageStager.getNumberOfEntries()
                for(Integer idx = 0; ((idx < nEntries)&&successfullyStaged);++idx)
                {
                    imageStager.setEntry(it)
                    imageStager.setDefaults()
                    imageStager.setUseFastHistogramStagingFlag(params.useFastHistograms)
                    imageStager.setHistogramStagingFlag(params.buildHistograms)
                    imageStager.setOverviewStagingFlag(params.buildOverviews)
                    imageStager.setCompressionType(params.overviewCompressionType)
                    imageStager.setOverviewType(params.overviewType)
                    successfullyStaged = imageStager.stage()
                }
            }
        }
        catch(e)
        {
           log.error(e.toString())
           successfullyStaged = false 
        }
        finally{
            imageStager.delete()
            imageStager = null                
        }
        return successfullyStaged
    }
}
