package uk.ac.ebi.subs.submissiongeneration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.ena.taxonomy.client.TaxonomyClient;
import uk.ac.ebi.ena.taxonomy.client.TaxonomyClientCurrentImpl;
import uk.ac.ebi.ena.taxonomy.client.TaxonomyException;
import uk.ac.ebi.ena.taxonomy.client.model.Taxon;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class TaxonomyClientConfig {


    @Value("${taxonomyServiceUrl:https://www.ebi.ac.uk/ena/data/taxonomy}")
    String taxonomyServiceUrl;

    static Logger logger = LoggerFactory.getLogger(TaxonomyClientConfig.class);

    @Bean
    public TaxonomyClient taxonomyClient(){
        return new CachingTaxonomyClientCurrentImpl(taxonomyServiceUrl);
    }

    public class CachingTaxonomyClientCurrentImpl extends TaxonomyClientCurrentImpl{

        public CachingTaxonomyClientCurrentImpl(String url){
            super(url);
        }

        private Map<String,List<Taxon>> cheesyCache = new LinkedHashMap(){
            private static final int MAX_ENTRIES = 500;

            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_ENTRIES;
            }
        };

        @Override
        public List<Taxon> getTaxonByScientificName(String scientificName) throws TaxonomyException {
            if (cheesyCache.containsKey(scientificName)){
                return cheesyCache.get(scientificName);
            }
            else {
                List<Taxon> taxa = super.getTaxonByScientificName(scientificName);
                cheesyCache.put(scientificName,taxa);
                return taxa;
            }
        }


    }



}
