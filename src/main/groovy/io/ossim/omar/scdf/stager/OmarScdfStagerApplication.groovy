package io.ossim.omar.scdf.stager

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.SendTo
import joms.oms.Init
import joms.oms.ImageStager

import java.nio.file.Paths

/**
 * Created by slallier on 6/8/2017
 *
 * The OmarScdfStagerApplication is a purpose built image stager for integration with a full SCDF stack.
 */
@SpringBootApplication
@EnableBinding(Processor.class)
@Slf4j
class OmarScdfStagerApplication implements CommandLineRunner
{
    /**************************************************
     * mediaTypeList contains the media types supported
     * by this class. The media types must be in the
     * format recognized by Apache Tika.
     * https://tika.apache.org/1.15/formats.html
     ***************************************************/
    final public static String[] MEDIA_TYPE_LIST = ['image/jpeg','image/tiff','image/nitf']

    // OSSIM Environment variables

    @Value('${ossim.prefs.file:/usr/share/ossim/ossim-site-preferences}')
    private String ossimPrefsFile

    // Stager settings, such as whether or not to build histograms and overviews

    @Value('${ossim.data:/data}')
    private String ossimData

    @Value('${stager.build.histograms:true}')
    private boolean buildHistograms

    @Value('${stager.build.overviews:true}')
    private boolean buildOverviews

    @Value('${stager.use.fast.histograms:false}')
    private boolean useFastHistograms

    @Value('${stager.overview.compression.type:none}')
    private String overviewCompressionType

    @Value('${stager.overview.type:ossim_tiff_box}')
    private String overviewType

    /**
     * The main entry point of the SCDF Sqs application.
     * @param args
     */
    static final void main(String[] args)
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
        boolean stagedSuccessfully = false

        if (null != message.payload)
        {
            // Parse filename from message
            //final def parsedJson = new JsonSlurper().parseText(message.headers)

            final String filename = message.headers.file_originalFile

            // build histograms and overviews, stage image
            log.debug("Building histograms and overviews for ${filename}")

            HashMap params = [
                    filename                     : filename,
                    buildHistograms              : buildHistograms,
                    buildOverviews               : buildOverviews,
                    useFastHistograms            : useFastHistograms,
                    overviewCompressionType      : overviewCompressionType,
                    overviewType                 : overviewType
            ]

            log.debug("Stager params:\n ${params}")

            // Only stage if the file is a valid image type
            final String mediatype = new Tika().detect(Paths.get(filename))

            if (MEDIA_TYPE_LIST.contains(mediatype))
            {
                stagedSuccessfully = stageImage(params)
            }

            // Return filename and result of staging request
            JsonBuilder stagedFile = new JsonBuilder()
            stagedFile(
                    filename : filename,
                    stagedSuccessfully : stagedSuccessfully
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
    final private boolean stageImage(HashMap params)
    {
        boolean successfullyStaged = true
        def imageStager = new ImageStager()

        try
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

                Integer entries = imageStager.getNumberOfEntries()
                for (Integer i = 0; ((i < entries) && successfullyStaged); i++)
                {
                    Println("--------ENTRIES---------=${entries}")
                    imageStager.setEntry(i)
                    imageStager.setDefaults()
                    imageStager.setUseFastHistogramStagingFlag(params.useFastHistograms)
                    imageStager.setHistogramStagingFlag(params.buildHistograms)
                    imageStager.setOverviewStagingFlag(params.buildOverviews)
                    imageStager.setCompressionType(params.overviewCompressionType)
                    imageStager.setOverviewType(params.overviewType)
                    successfullyStaged = imageStager.stage()
                }
            }
            else
            {
               successfullyStaged = false
            }

            Println("--------STAGED?---------=${successfullyStaged}")
        }
        catch (e)
        {
           log.error(e.toString())
           successfullyStaged = false
        }
        finally
        {
            imageStager.delete()
        }
        return successfullyStaged
    }

    @Override
    void run(String... args) throws Exception {
        log.debug("OSSIM_PREFS_FILE: ${ossimPrefsFile}")
        log.debug("OSSIM_DATA: ${ossimData}")

        String[] newArgs = ["dummy",
                            "--env",
                            "OSSIM_PREFS_FILE=${ossimPrefsFile}",
                            "--env",
                            "OSSIM_DATA=${ossimData}"]

        log.debug("JNI Init arguments remaining: ${Init.instance().initialize(newArgs.size(), newArgs)}")
    }
}
