package uk.ac.ebi.subs.submissiongeneration.ArrayExpressModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by davidr on 12/09/2016.
 */
public class AeExperiment {
    String accession;
    List<AeFile> file = new ArrayList<>();

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public List<AeFile> getFile() {
        return file;
    }

    public void setFile(List<AeFile> file) {
        this.file = file;
    }
}
