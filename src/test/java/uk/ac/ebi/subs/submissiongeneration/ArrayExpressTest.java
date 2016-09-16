package uk.ac.ebi.subs.submissiongeneration;


import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SubGenApp.class)
public class ArrayExpressTest {

    @Autowired
    ArrayExpressSubmissionGenerationService submissionGenerationService;


    @Test
    public void testDateRange() throws IOException, ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Path dir = Files.createTempDirectory("aeTesting");

        // date range sufficient to get E-MTAB-4517 only
        submissionGenerationService.writeSubmissionsFromRange(sdf.parse("2016-09-10"),sdf.parse("2016-09-10"),dir);

        List<Path> jsonFilePath = Files.walk(dir).filter(p -> p.toFile().isFile()).collect(Collectors.toList());

        assertThat("Files made", jsonFilePath.size(), equalTo(1));

        for (Path p : Lists.reverse(Files.walk(dir).collect(Collectors.toList()))){
            Files.deleteIfExists(p);
            System.out.println("deleted "+p);
        }


    }



}
