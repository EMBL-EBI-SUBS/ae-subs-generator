package uk.ac.ebi.subs.submissiongeneration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Stream;

public class ArrayExpressFilesResponse {
    AeFilesWrapper files = new AeFilesWrapper();

    public AeFilesWrapper getFiles() {
        return files;
    }

    public void setFiles(AeFilesWrapper files) {
        this.files = files;
    }

    Stream<AeFile> fileStream() {
        return files.experiment.file.
                stream().filter(f -> f.getKind() != null);
    }

    URL idfUrl() throws MalformedURLException {
        return new URL(fileStream().filter(f -> f.getKind().contains("idf")).map(f -> f.getUrl()).findFirst().get());
    }

}
