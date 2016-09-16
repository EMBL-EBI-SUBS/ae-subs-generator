package uk.ac.ebi.subs.submissiongeneration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.any23.encoding.TikaEncodingDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.IDF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.*;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.ArrayDesignAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.subs.data.component.*;
import uk.ac.ebi.subs.data.submittable.*;

import java.io.*;
import java.io.File;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

@Service
public class ArrayExpressSubmissionGenerationService implements SubmissionGenerationService {

    @Override
    public void writeSubmissions(Path targetDir) {
        try {
            streamTsv().forEach(p -> {
                try {
                    processAccDate(p.getFirst(), p.getSecond(), targetDir);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (uk.ac.ebi.arrayexpress2.magetab.exception.ParseException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeSubmissionsFromRange(Date start, Date end, Path targetDir) {
        try {
            streamTsv()
                    .filter(stringDatePair -> {
                        Date d = stringDatePair.getSecond();
                        return (d.before(end) || d.equals(end)) && (d.after(start) || d.equals(start));
                    }).forEach(p -> {
                try {
                    processAccDate(p.getFirst(), p.getSecond(), targetDir);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (uk.ac.ebi.arrayexpress2.magetab.exception.ParseException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Value("${arrayExpressTsvUrl:https://www.ebi.ac.uk/arrayexpress/ArrayExpress-Experiments.txt?keywords=&organism=&exptype%5B%5D=&exptype%5B%5D=&array=&directsub=on}")
    URL arrayExpressTsvUrl;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    SimpleDateFormat weekInYearSdf = new SimpleDateFormat("ww");
    SimpleDateFormat yearSdf = new SimpleDateFormat("yyyy");

    RestTemplate restTemplate = new RestTemplate();

    @Autowired
    ObjectMapper objectMapper;

    public void processAccDate(String accession, Date releaseDate, Path targetDir) throws IOException, uk.ac.ebi.arrayexpress2.magetab.exception.ParseException, ParseException {
        System.out.println(String.join("\t", accession, releaseDate.toString(), targetDir.toString()));

        String url = "http://www.ebi.ac.uk/arrayexpress/json/v2/files/" + accession;
        System.out.println(url);
        ArrayExpressFilesResponse response = restTemplate.getForObject(url, ArrayExpressFilesResponse.class);

        MAGETABParser parser = new MAGETABParser();

        MAGETABInvestigation investigation = parser.parse(response.idfUrl());

        Submission submission = createSubmission(investigation);

        writeSubmission(submission, accession, targetDir, releaseDate);
    }

    public void writeSubmission(Submission submission, String accession, Path rootTargetDir, Date releaseDate) throws IOException {

        String year = yearSdf.format(releaseDate);
        String week = weekInYearSdf.format(releaseDate);

        rootTargetDir.toFile();

        String dirName = String.join(File.separator, rootTargetDir.toString(), year, week);

        Files.createDirectories(Paths.get(dirName));

        String fileName = dirName + File.separator + accession + ".json";
        File outputFile = new File(fileName);

        objectMapper.writeValue(outputFile, submission);
        System.out.println(outputFile);
    }

    public Submission createSubmission(MAGETABInvestigation investigation) throws ParseException {
        Submission submission = new Submission();

        Study study = createStudy(investigation.IDF);

        Map<String,String> protocolTypes = new HashMap<>();
        for (Protocol protocol : study.getProtocols()){
            protocolTypes.put(protocol.getName(),protocol.getType());
        }

        Optional<Contact> firstContact = study.getContacts().stream().filter(c -> c.getEmail() != null).findFirst();
        if (firstContact.isPresent()) {
            submission.getDomain().setName(firstContact.get().getEmail());
            submission.getSubmitter().setEmail(firstContact.get().getEmail());
            study.setDomain(submission.getDomain());
        }

        submission.getStudies().add(study);

        convertSdrf(investigation.SDRF,submission,study.asLink(),protocolTypes);

        return submission;
    }

    public void convertSdrf(SDRF sdrf,Submission submission,SubsLink<Study> studyRef,Map<String,String> protocolTypes){



        Collection<SourceNode> rootNodes = (Collection<SourceNode>) sdrf.getRootNodes();


        for (SourceNode node : rootNodes){
            Sample sample = sampleFromNode(node,studyRef);
            submission.getSamples().add(sample);

            for (Node childNode : node.getChildNodes()){
                Assay assay = new Assay();
                assay.setDomain(new Domain());
                assay.getDomain().setName(studyRef.getDomain());
                assay.setAlias(studyRef.getAlias()+'~'+node.getNodeName());
                assay.setStudyRef(studyRef);
                assay.setSampleRef(sample.asLink());

                submission.getAssays().add(assay);

                Collection<AssayData> assayData = new ArrayList<>();
                traverseAssayNodes(assay,childNode,assayData,0,protocolTypes);

                submission.getAssayData().addAll(assayData);
            }
        }
    }

    public void traverseAssayNodes(Assay assay, Node node, Collection<AssayData> assayData, int depth, Map<String,String> protocolTypes){

        System.out.println(assay.getAlias());
        System.out.println(depth);
        System.out.println(node);

        Set<String> protocolTypesTriggeringAssayData = new HashSet<>(
                Arrays.asList("nucleic acid sequencing protocol",
                "array scanning and feature extraction protocol",
                "normalization data transformation protocol")
        );

        boolean childNodesProcessed = false;

        switch (node.getNodeType()){
            case "protocolref":
                ProtocolApplicationNode protocolApplicationNode = (ProtocolApplicationNode)node;
                String type = protocolTypes.get(protocolApplicationNode.protocol);

                if (protocolTypesTriggeringAssayData.contains(type)){
                    childNodesProcessed = true;
                    AssayData ad = assayData(assay,assayData);
                    traverseAssayDataNodes(ad,node,depth);
                }
                else {
                    buildSingleAttribute("protocol ref", protocolApplicationNode.protocol, assay);
                    protocolTypes.get(protocolApplicationNode.protocol);
                }
                //TODO values?? protocolApplicationNode.values(); protocolApplicationNode.parameterValues;
                break;
            case "extractname":
                ExtractNode extractNode = (ExtractNode)node;
                buildSingleAttribute("extract name",extractNode.getNodeName(),assay);
                break;
            case "labeledextractname":
                LabeledExtractNode labeledExtractNode = (LabeledExtractNode)node;
                buildSingleAttribute("labeled extract name",labeledExtractNode.getNodeName(),assay);
                buildSingleAttribute("label",labeledExtractNode.label.getAttributeValue(),assay);
                break;
            case "assayname":
                AssayNode assayNode = (AssayNode)node;
                buildSingleAttribute("assay name",assayNode.getNodeName(),assay);

                for (ArrayDesignAttribute arrayDesignAttribute : assayNode.arrayDesigns){
                    buildSingleAttribute("array design",arrayDesignAttribute.getNodeName(),assay);
                }

                for (FactorValueAttribute factorValue : assayNode.factorValues){
                    buildSingleAttribute("factor value["+factorValue.type+"]", factorValue.getNodeName(),assay);
                }
                break;
           /* case "derivedarraydatafile":
                AssayData ad1 = assayData(assay,assayData);
                traverseAssayDataNodes(ad1,node,depth);
                childNodesProcessed = true;
                break;
            case "arraydatafile":
                AssayData ad2 = assayData(assay,assayData);
                traverseAssayDataNodes(ad2,node,depth);
                childNodesProcessed = true;
                break; */
            default:
                System.out.println("no assay handler for "+node.getNodeType());

        }


        if (!childNodesProcessed) {
            for (Node childNode : node.getChildNodes()) {
                traverseAssayNodes(assay, childNode, assayData, depth + 1,protocolTypes);
            }
        }


    }

    public void traverseAssayDataNodes(AssayData assayData,Node node, int depth){
        System.out.println(assayData.getAlias());
        System.out.println(depth);
        System.out.println(node);

        switch (node.getNodeType()){
            case "protocolref":
                ProtocolApplicationNode protocolApplicationNode = (ProtocolApplicationNode)node;
                buildSingleAttribute("protocol ref", protocolApplicationNode.protocol, assayData);
                break;
            case "derivedarraydatafile":
                DerivedArrayDataNode derivedArrayDataNode = (DerivedArrayDataNode)node;
                uk.ac.ebi.subs.data.component.File derivedFile = new uk.ac.ebi.subs.data.component.File();
                derivedFile.setName(derivedArrayDataNode.getNodeName());
                derivedFile.setType("derived array data");
                assayData.getFiles().add(derivedFile);


                break;
            case "arraydatafile":
                ArrayDataNode arrayDataNode = (ArrayDataNode)node;
                uk.ac.ebi.subs.data.component.File arrayFile = new uk.ac.ebi.subs.data.component.File();
                arrayFile.setName(arrayDataNode.getNodeName());
                arrayFile.setType("array data");
                assayData.getFiles().add(arrayFile);
                break;
            default:
                System.out.println("no assay data handler for "+node.getNodeType());
        }
    }

    public AssayData assayData(Assay assay,Collection<AssayData> assayData){
        AssayData ad = new AssayData();

        ad.setDomain(assay.getDomain());
        ad.setArchive(Archive.ArrayExpress);
        ad.setAssayRef(assay.asLink());
        ad.setAlias( assay.getAlias() + '~' + assayData.size()+1);

        assayData.add(ad);
        return ad;
    }


    public Sample sampleFromNode(SourceNode sourceNode,SubsLink<Study> studyRef){
        Sample sample = new Sample();
        sample.setDomain(new Domain());
        sample.getDomain().setName(studyRef.getDomain());
        sample.setArchive(Archive.Usi);

        String nodeName = sourceNode.getNodeName();
        sample.setTitle(nodeName);
        sample.setAlias(studyRef.getAlias()+'~'+nodeName);

        for (CharacteristicsAttribute characteristic : sourceNode.characteristics){

            if (characteristic.getAttributeType().equals("organism")){
                sample.setTaxon(characteristic.getAttributeValue());
                //TODO get taxonID
            }
            else {
                buildSingleAttribute(characteristic,sample);
            }
        }

        if (sourceNode.materialType != null){
            Attribute a = new Attribute();

            a.setName("material type");
            a.setValue(sourceNode.materialType.getAttributeValue());

            if (sourceNode.materialType.termAccessionNumber != null){
                Term t = new Term();
                t.setTermID(sourceNode.materialType.termAccessionNumber);
                t.setSourceName(sourceNode.materialType.termSourceREF);
                a.setTerm(t);
            }

            sample.getAttributes().add(a);
        }

        return sample;
    }

    public void buildSingleAttribute(CharacteristicsAttribute characteristic, Attributes attributes){
        Attribute a = new Attribute();

        a.setName(characteristic.type);
        a.setValue(characteristic.getAttributeValue());

        if (characteristic.unit != null){
            a.setUnits(characteristic.unit.getAttributeValue());
        }

        if (characteristic.termAccessionNumber != null){
            Term t = new Term();
            t.setTermID(characteristic.termAccessionNumber);
            t.setSourceName(characteristic.termSourceREF);
            a.setTerm(t);
        }

        attributes.getAttributes().add(a);
    }

    public void buildAttributes(String attributeName, List<String> values, List<String> termIds, List<String> termRefs, Attributes submittable) {
        for (int i = 0; i < values.size(); i++) {
            Attribute a = new Attribute();
            a.setName(attributeName);
            a.setValue(values.get(i));

            Term t = new Term();

            t.setSourceName(stringPresent(termRefs, i));
            t.setTermID(stringPresent(termIds, i));


            if (t.getSourceName() != null || t.getTermID() != null) {
                a.setTerm(t);
            }

            submittable.getAttributes().add(a);
        }
    }


    public void buildContacts(IDF idf, Contacts contacts) {
        for (int i = 0; i < idf.personEmail.size(); i++) {
            Contact contact = new Contact();

            contact.setLastName(stringPresent(idf.personLastName, i));
            contact.setFirstName(stringPresent(idf.personFirstName, i));
            contact.setMiddleInitials(stringPresent(idf.personMidInitials, i));
            contact.setEmail(stringPresent(idf.personEmail, i));
            contact.setPhone(stringPresent(idf.personPhone, i));
            contact.setAddress(stringPresent(idf.personAddress, i));
            contact.setAffiliation(stringPresent(idf.personAffiliation, i));

            String roles = stringPresent(idf.personRoles, i);
            if (roles != null) {
                contact.getRoles().addAll(Arrays.asList(roles.split(";")));
            }

            contacts.getContacts().add(contact);
        }
    }

    public void buildSingleAttribute(String name, String value, Attributes attributes) {
        if (value == null) {
            return;
        }
        Attribute a = new Attribute();
        a.setName(name);
        a.setValue(value);
        attributes.getAttributes().add(a);
    }

    public void buildProtocols(IDF idf, Protocols protocols) {

        for (int i = 0; i < idf.protocolName.size(); i++) {
            Protocol p = new Protocol();

            p.setName(stringPresent(idf.protocolName, i));
            p.setType(stringPresent(idf.protocolType, i));
            p.setDescription(stringPresent(idf.protocolDescription, i));

            buildSingleAttribute("hardware", stringPresent(idf.protocolHardware, i), p);
            buildSingleAttribute("software", stringPresent(idf.protocolSoftware, i), p);
            buildSingleAttribute("parameters", stringPresent(idf.protocolParameters, i), p);
            buildSingleAttribute("contact", stringPresent(idf.protocolContact, i), p);

            protocols.getProtocols().add(p);
        }
    }

    public void buildPublications(IDF idf, Publications publications) {
        for (int i = 0; i < idf.publicationTitle.size(); i++) {
            Publication p = new Publication();

            p.setPubmedId(stringPresent(idf.pubMedId, i));
            p.setDoi(stringPresent(idf.publicationDOI, i));

            publications.getPublications().add(p);
        }
    }

    public String stringPresent(List<String> list, int index) {
        if (index < list.size() && list.get(index) != null && !list.get(index).isEmpty()) {
            return list.get(index);
        } else {
            return null;
        }
    }


    public Study createStudy(IDF idf) throws ParseException {
        Study study = new Study();

        study.setArchive(Archive.ArrayExpress);
        study.setAlias(idf.accession);

        study.setTitle(idf.investigationTitle);
        study.setDescription(idf.experimentDescription);

        study.setReleaseDate(sdf.parse(idf.publicReleaseDate));

        buildPublications(idf, study);

        buildProtocols(idf, study);

        buildContacts(idf, study);

        buildAttributes("experimental design", idf.experimentalDesign, idf.experimentalDesignTermAccession, idf.experimentalDesignTermSourceREF, study);

        buildAttributes("experimental factor", idf.experimentalFactorName, idf.experimentalFactorTermAccession, idf.experimentalFactorTermSourceREF, study);

        buildAttributes("normalization type", idf.normalizationType, idf.normalizationTermAccession, idf.normalizationTermSourceREF, study);

        buildAttributes("quality control type", idf.qualityControlType, idf.qualityControlTermAccession, idf.qualityControlTermSourceREF, study);

        buildAttributes("replicate type", idf.replicateType, idf.replicateTermAccession, idf.replicateTermSourceREF, study);

        buildSingleAttribute("date of experiment", idf.dateOfExperiment, study);


        return study;
    }


    public Stream<Pair<String, Date>> streamTsv() throws IOException {
        File tsvFile = downloadTsv();
        return streamTsvLines(tsvFile)
                .filter(l -> !l.startsWith("Accession"))
                .map(line -> {
                    String[] row = line.split("\t");
                    return row;
                })
                .filter(row -> row.length >= 6)
                .map(row -> {
                    String[] accDate = {row[0], row[5]};
                    return accDate;
                })
                .filter(accDate -> accDate[0].matches("E-\\w+-\\d+"))
                .map(accDate -> {
                    Date date = null;
                    try {
                        date = sdf.parse(accDate[1]);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Pair<String, Date> pair = Pair.of(accDate[0], date);
                    return pair;
                });
    }


    public File downloadTsv() throws IOException {
        File tmpFile = File.createTempFile("ae-experiments", "tsv");
        tmpFile.deleteOnExit();

        ReadableByteChannel rbc = Channels.newChannel(arrayExpressTsvUrl.openStream());
        FileOutputStream fos = new FileOutputStream(tmpFile);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();

        return tmpFile;
    }

    public Stream<String> streamTsvLines(File tsvFile) throws IOException {
        Path p = tsvFile.toPath();
        return Files.lines(p, guessCharset(tsvFile));
    }

    public Charset guessCharset(File file) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        Charset cs = Charset.forName(new TikaEncodingDetector().guessEncoding(is));
        is.close();
        System.out.println(cs);
        return cs;
    }
}
