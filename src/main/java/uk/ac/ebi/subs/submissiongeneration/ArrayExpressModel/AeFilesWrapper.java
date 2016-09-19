package uk.ac.ebi.subs.submissiongeneration.ArrayExpressModel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.ac.ebi.subs.submissiongeneration.OptionalArrayDeserializer;

import java.util.ArrayList;
import java.util.List;

public class AeFilesWrapper {

    @JsonDeserialize(using = AeExperimentListDeserializer.class)
    List<AeExperiment> experiment;

    public List<AeExperiment> getExperiment() {
        return experiment;
    }

    public void setExperiment(List<AeExperiment> experiment) {
        this.experiment = experiment;
    }

    List<String> kind = new ArrayList<>();

    public static class AeExperimentListDeserializer extends OptionalArrayDeserializer<AeExperiment> {
        protected AeExperimentListDeserializer() {
            super(AeExperiment.class);
        }
    }
}
