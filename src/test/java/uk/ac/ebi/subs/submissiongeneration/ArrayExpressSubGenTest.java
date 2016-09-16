package uk.ac.ebi.subs.submissiongeneration;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SubGenTestApp.class)
public class ArrayExpressSubGenTest {

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    ArrayExpressSubmissionGenerationService submissionGenerationService;

    @Test
    public void testDateRange() throws IOException, ParseException {


        Path dir = Files.createTempDirectory("aeTesting");

        // date range sufficient to get E-MTAB-4517 only
        submissionGenerationService.writeSubmissionsFromRange(sdf.parse("2016-09-10"),sdf.parse("2016-09-10"),dir);

        List<Path> jsonFilePath = Files.walk(dir).filter(p -> p.toFile().isFile()).collect(Collectors.toList());

        assertThat("Files made", jsonFilePath.size(), equalTo(1));
        assertThat("File name", jsonFilePath.get(0).toFile().getName(),equalTo("E-MTAB-4517.json"));

        if (true) {
            Files.walkFileTree(dir, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    System.out.println("deleted " + file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException exc) throws IOException {
                    Files.delete(path);
                    System.out.println("deleted " + path);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
