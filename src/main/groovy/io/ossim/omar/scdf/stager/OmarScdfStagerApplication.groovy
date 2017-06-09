package io.ossim.omar.scdf.stager

import groovy.json.JsonBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.messaging.handler.annotation.SendTo

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
     * Constructor
     */
    OmarScdfStagerApplication() {

    }

    /**
     * The main entry point of the SCDF Sqs application.
     * @param args
     */
    static void main(String[] args) {
        SpringApplication.run OmarScdfStagerApplication, args
    }

    /**
     * The callback for when a filename is received
     * @param message the body of the SQS message from the queue
     */
    @StreamListener(Processor.INPUT)
    @SendTo(Processor.OUTPUT)
    String stageImage(String filename) {
        JsonBuilder stagedFile
        logger.debug("Test")
        logger.info("Received file ${filename} to stage")
        // add raster, etc

        stagedFile = new JsonBuilder()
        stagedFile(filename : filename, stagedSuccessfully : "true")
        stagedFile.toPrettyString()
    }
}
