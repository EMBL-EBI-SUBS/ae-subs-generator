package uk.ac.ebi.subs.submissiongeneration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.subs.data.submittable.Sample;
import uk.ac.ebi.subs.processing.SubmissionEnvelope;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SubGenTestApp.class)
public class AeMageTabConverterTest {

    @Autowired
    AeMageTabConverter aeMageTabConverter;

    @Test
    public void test() throws IOException, ParseException, uk.ac.ebi.arrayexpress2.magetab.exception.ParseException {

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("E-MTAB-4222.idf.txt").getFile());

        SubmissionEnvelope submissionEnvelope = aeMageTabConverter.mageTabToSubmissionEnvelope(file.toURI().toURL());

        assertThat("Studies expected", submissionEnvelope.getStudies().size(), equalTo(1));
        assertThat("Samples expected", submissionEnvelope.getSamples().size(), equalTo(72));
        assertThat("Assays expected", submissionEnvelope.getAssays().size(), equalTo(72));
        assertThat("AssayData expected", submissionEnvelope.getAssayData().size(), equalTo(72));

        for (Sample s : submissionEnvelope.getSamples()){
            assertThat("Taxon name",s.getTaxon(),equalTo("Triticum aestivum"));
            assertThat("TaxonId",s.getTaxonId(),equalTo(4565L));
        }

    }
}
