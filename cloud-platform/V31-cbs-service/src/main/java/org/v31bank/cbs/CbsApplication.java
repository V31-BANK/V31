package org.v31bank.cbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the core banking service.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@SpringBootApplication
public class CbsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CbsApplication.class, args);
    }

}
