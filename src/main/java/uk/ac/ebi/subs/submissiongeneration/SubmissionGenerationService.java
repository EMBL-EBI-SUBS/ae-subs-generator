package uk.ac.ebi.subs.submissiongeneration;


import java.nio.file.Path;
import java.util.Date;

public interface SubmissionGenerationService {

    void writeSubmissions(Path targetDir);

    void writeSubmissionsFromRange(Date start, Date end, Path targetDir);
}
