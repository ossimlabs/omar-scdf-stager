package io.ossim.omar.scdf.stager

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
class OmarScdfStagerApplication {

    /**
     * The application logger
     */
    private final Logger logger = LoggerFactory.getLogger(this.getClass())

    /**
     * Default parameters with example filename
     */
    private HashMap params = [
            filename : "/data/stereo/15JUL19101909-P1BS_R1C1-056263760010_01_P001.NTF",
            buildHistograms: true,
            buildOverviews: true,
            useFastHistograms: false,
            overviewCompressionType: "none",
            overviewType:"ossim_tiff_box"
    ]

    /**
     * ImageStager member variable
     */
    private ImageStager imageStager

    /**
     * Constructor
     */
    OmarScdfStagerApplication()
    {
        Init.instance().initialize()
        imageStager = new ImageStager()
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
        logger.debug("Received message ${message} containing the name of a file to stage")

        if (null != message.payload)
        {
            // Parse filename from message
            final def parsedJson = new JsonSlurper().parseText(message.payload)
            logger.debug("parsedJson : ${parsedJson}")
            final String filename = parsedJson.filename
            logger.debug("filename: ${filename}")

            // build histograms and overviews, stage image
            logger.debug("Building histograms and overviews for ${filename}")

            params.filename = filename

            logger.debug("Stager params:\n ${params}")

            boolean stagedSuccessfully = stageImage()

            // Return filename and result of staging request
            JsonBuilder stagedFile = new JsonBuilder()
            String status = stagedSuccessfully ? "succeeded" : "failed"
            stagedFile(
                    filename : filename,
                    status : status
            )

            logger.debug("Sending result to output stream -- ${stagedFile.toString()}")
            return stagedFile.toString()
        }
        else
        {
            logger.warn("Received null payload for message: ${message}")
            return null
        }
    }

    /**
    *
    */
    boolean stageImage()
    {
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

          boolean successfullyStaged = true
          Integer entries = imageStager.getNumberOfEntries()

          (0..< entries).each
          {
              imageStager.setEntry(it)
              imageStager.setDefaults()
              imageStager.setUseFastHistogramStagingFlag(params.useFastHistograms)
              imageStager.setHistogramStagingFlag(params.buildHistograms)
              imageStager.setOverviewStagingFlag(params.buildOverviews)
              imageStager.setCompressionType(params.overviewCompressionType)
              imageStager.setOverviewType(params.overviewType)
              boolean result = imageStager.stage()

              if (!result) successfullyStaged = false
          }

          return successfullyStaged
      }
      else
      {
        return false
      }
    }
}
