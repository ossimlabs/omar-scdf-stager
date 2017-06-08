package io.ossim.omar.scdf.stager

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink

/**
 * Created by slallier on 6/8/2017
 *
 * The OmarScdfStagerApplication is a purpose built image stager for integration with a full SCDF stack.
 */
@SpringBootApplication
@EnableBinding(Sink.class)
final class OmarScdfStagerApplication {

    /**
     * The application logger
     */
    private final Logger logger = LoggerFactory.getLogger(this.getClass())

    /**
     * The filename of the image to stage
     */
    private String imageFilename

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
    @StreamListener(Sink.INPUT)
    public void receiveFilenameToStage(String filename) {
        imageFilename = filename
        logger.info("Received file ${imageFilename} to stage")
    }
}
