package uk.ac.ebi.subs.submissiongeneration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.subs.data.submittable.Submission;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SubGenApp.class)
public class AeMageTabConverterTest {

    @Autowired
    AeMageTabConverter aeMageTabConverter;

    @Test
    public void test() throws IOException, ParseException, uk.ac.ebi.arrayexpress2.magetab.exception.ParseException {

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("E-MTAB-4222.idf.txt").getFile());
        System.out.println(file.getAbsolutePath());


        Submission sub = aeMageTabConverter.mageTabToSubmission(file.toURI().toURL());

        assertThat("Studies expected", sub.getStudies().size(), equalTo(1));
        assertThat("Samples expected", sub.getSamples().size(), equalTo(72));
        assertThat("Assays expected", sub.getAssays().size(), equalTo(72));
        assertThat("AssayData expected", sub.getAssayData().size(), equalTo(144));



    }
}
