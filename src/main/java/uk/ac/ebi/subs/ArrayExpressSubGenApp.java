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

@SpringBootApplication
public class ArrayExpressSubGenApp implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ArrayExpressSubGenApp.class);

    @Autowired
    ArrayExpressSubmissionGenerationService submissionGenerationService;

    @Value("${targetDir:.}")
    String targetDir;

    @Value("${startDate:empty}")
    String startDateText;

    @Value("${endDate:empty}")
    String endDateText;

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat();

    @Override
    public void run(String... args) {

        Date startDate = null;
        Date endDate = null;

        try {
            if (startDateText != null && !startDateText.equals("empty")) {
                startDate = simpleDateFormat.parse(startDateText);
            }
            if (endDateText != null && !startDateText.equals("empty")) {
                endDate = simpleDateFormat.parse(endDateText);
            }
        }
        catch (ParseException e){
            System.err.println("Cannot parse date: "+e.getMessage());
            System.exit(1);
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

        if (startDate != null && endDate == null) {
            endDate = new Date(); //make end date now
        }

        if (startDate != null && endDate != null && startDate.getTime() >= endDate.getTime()) {
            System.err.println("startDate must be before endDate");
            System.exit(1);
        }

        if (startDate == null || endDate == null) {
            logger.info("Starting submission generation from ArrayExpress, writing to {}", targetDirPath.toAbsolutePath());

            submissionGenerationService.writeSubmissions(targetDirPath);
        } else {
            logger.info(
                    "Starting submission generation from ArrayExpress, using submissions between {} and {}writing to {}"
                    , startDate, endDate, targetDirPath.toAbsolutePath());

            submissionGenerationService.writeSubmissionsFromRange(startDate,endDate,targetDirPath);
        }

    }

    public static void main(String[] args) {
        SpringApplication.run(ArrayExpressSubGenApp.class, args);
    }
}