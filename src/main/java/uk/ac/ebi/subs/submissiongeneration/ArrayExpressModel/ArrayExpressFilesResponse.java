package uk.ac.ebi.subs.submissiongeneration.ArrayExpressModel;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public class ArrayExpressFilesResponse {
    AeFilesWrapper files = new AeFilesWrapper();

    public AeFilesWrapper getFiles() {
        return files;
    }

    public void setFiles(AeFilesWrapper files) {
        this.files = files;
    }


    /**
     * Get URL for IDF file from response
     * <p>
     * takes accession, as files endpoint sometimes includes extra experiments (e.g. as links)
     *
     * @param accession
     * @return
     * @throws MalformedURLException
     */
    public URL idfUrl(String accession) throws MalformedURLException {
        Optional<String> url = files.experiment.stream()
                .filter(e -> e.getAccession().equals(accession))
                .flatMap(e -> e.getFile().stream())
                .filter(f -> f.getKind() != null)
                .filter(f -> f.getKind().contains("idf"))
                .map(f -> f.getUrl())
                .findFirst();


        return new URL(url.get());
    }

}
