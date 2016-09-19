package uk.ac.ebi.subs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.ac.ebi.subs.submissiongeneration.ArrayExpressSubmissionGenerationService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Application to get Direct ArrayExpress submissions released within a date range
 * and convert them to USI Subs object model json file, place in the target directory
 *
 * Create the jar with the jar and bootRepackage then run it with the command line like this:
 *
 * java -jar subs/ae-subs-generator/build/libs/ae-subs-generator-1.0.0.jar
 *      --targetDir=ArrayExpressSubs/
 *      --startDate=2015-01-01
 *      --endDate=2015-12-31
 *
 * Would retreive all submissions from 2015
 */
@SpringBootApplication
public class ArrayExpressSubGenApp implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ArrayExpressSubGenApp.class);

    @Autowired
    ArrayExpressSubmissionGenerationService submissionGenerationService;

    @Value("${targetDir:.}")
    String targetDir;

    @Value("${startDate:.}")
    String startDate;

    @Value("${endDate:.}")
    String endDate;

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void run(String... args) {

        Date startDateObj = null;
        Date endDateObj = null;

        try {
            if (this.startDate != null && !this.startDate.equals(".")) {
                startDateObj = simpleDateFormat.parse(this.startDate);
            }
            if (this.endDate != null && !this.startDate.equals(".")) {
                endDateObj = simpleDateFormat.parse(this.endDate);
            }
        } catch (ParseException e) {
            System.err.println("Cannot parse date: " + e.getMessage());
            System.exit(1);
        }

        if (targetDir == null || targetDir.equals(".")) {
            targetDir = System.getProperty("user.dir"); //current working directory
        }

        Path targetDirPath = Paths.get(targetDir);

        if (!targetDirPath.toFile().isDirectory()) {
            System.err.println("targetDir is must be a directory");
            System.exit(1);
        }
        if (!targetDirPath.toFile().canWrite()) {
            System.err.println("targetDir is must be writeable");
            System.exit(1);
        }

        if (startDateObj != null && endDateObj == null) {
            endDateObj = new Date(); //make end date now
        }

        if (startDateObj != null && endDateObj != null && startDateObj.getTime() > endDateObj.getTime()) {
            System.err.println("startDate must be before endDate");
            System.exit(1);
        }

        if (startDateObj == null || endDateObj == null) {
            System.out.println("Starting submission generation from ArrayExpress, writing to " + targetDirPath.toAbsolutePath());

            //submissionGenerationService.writeSubmissions(targetDirPath);
        }
        else {
            System.out.println(
                    String.join(" ",
                            "Starting submission generation from ArrayExpress, using submissions between",
                            simpleDateFormat.format(startDateObj),
                            "and ",
                            simpleDateFormat.format(endDateObj),
                            " writing to ",
                            targetDirPath.toAbsolutePath().toString()
                    )
            );

            submissionGenerationService.writeSubmissionsFromRange(startDateObj, endDateObj, targetDirPath);
        }

    }

    public static void main(String[] args) {
        SpringApplication.run(ArrayExpressSubGenApp.class, args);
    }
}